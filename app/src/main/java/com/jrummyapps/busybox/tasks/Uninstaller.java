/*
 * Copyright (C) 2016 Jared Rummler <jared.rummler@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.jrummyapps.busybox.tasks;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.jrummyapps.android.eventbus.Events;
import com.jrummyapps.android.io.external.ExternalStorageHelper;
import com.jrummyapps.android.roottools.RootTools;
import com.jrummyapps.android.roottools.files.AFile;
import com.jrummyapps.android.roottools.utils.Mount;
import com.jrummyapps.android.roottools.utils.RootUtils;
import com.jrummyapps.busybox.R;

import java.util.ArrayList;
import java.util.List;

public class Uninstaller implements Runnable {

  public static void showConfirmationDialog(Activity activity, AFile file) {
    ConfirmUninstallDialog dialog = new ConfirmUninstallDialog();
    Bundle args = new Bundle();
    args.putParcelable("file", file);
    dialog.setArguments(args);
    dialog.show(activity.getFragmentManager(), "ConfirmUninstallDialog");
  }

  public static boolean uninstall(AFile binary) {
    return deleteFile(binary) && uninstallSymlinks(binary);
  }

  public static boolean uninstallSymlinks(AFile binary) {
    List<AFile> symlinks = getSymlinks(binary);
    if (symlinks.isEmpty()) {
      return true;
    }

    String rm = RootUtils.getUtil("rm");
    if (rm != null) {
      StringBuilder command = new StringBuilder(rm);
      for (AFile symlink : symlinks) {
        command.append(" \"").append(symlink.path).append("\"");
      }
      if (Mount.remountThenRun(binary, command.toString()).success()) {
        return true;
      }
    }

    for (AFile symlink : symlinks) {
      if (!deleteFile(symlink)) {
        return false;
      }
    }

    return true;
  }

  private static List<AFile> getSymlinks(AFile binary) {
    List<AFile> symlinks = new ArrayList<>();
    AFile parent = binary.getParentFile();
    if (parent != null) {
      AFile[] files = parent.listFiles();
      if (files != null) {
        for (AFile file : files) {
          if (file.isSymbolicLink() && file.readlink().equals(binary)) {
            symlinks.add(file);
          }
        }
      }
    }
    return symlinks;
  }

  private static boolean deleteFile(AFile file) {
    return file.isOnRemovableStorage() && ExternalStorageHelper.delete(file) || file.delete() || RootTools.rm(file);
  }

  private final AFile file;

  public Uninstaller(AFile file) {
    this.file = file;
  }

  @Override public void run() {
    Events.post(new StartEvent(file));
    Events.post(new FinishedEvent(file, uninstall(file)));
  }

  public static final class StartEvent {

    public final AFile file;

    public StartEvent(AFile file) {
      this.file = file;
    }

  }

  public static final class FinishedEvent {

    public final AFile file;
    public final boolean success;

    public FinishedEvent(AFile file, boolean success) {
      this.file = file;
      this.success = success;
    }

  }

  public static class ConfirmUninstallDialog extends DialogFragment {

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
      final AFile file = getArguments().getParcelable("file");
      return new AlertDialog.Builder(getActivity())
          .setTitle(R.string.confirm_before_deleting)
          .setMessage(getString(R.string.are_you_sure_you_want_to_uninstall_s, file.filename))
          .setNegativeButton(android.R.string.cancel, null)
          .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            @Override public void onClick(DialogInterface dialog, int which) {
              new Thread(new Uninstaller(file)).start();
              dialog.dismiss();
            }
          })
          .create();
    }

  }

}
