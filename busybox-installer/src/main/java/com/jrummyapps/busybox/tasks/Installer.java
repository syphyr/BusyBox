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
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.AlertDialog;

import com.crashlytics.android.Crashlytics;
import com.jrummyapps.android.app.App;
import com.jrummyapps.android.eventbus.Events;
import com.jrummyapps.android.io.FileUtils;
import com.jrummyapps.android.io.Storage;
import com.jrummyapps.android.io.external.ExternalStorageHelper;
import com.jrummyapps.android.prefs.Prefs;
import com.jrummyapps.android.roottools.RootTools;
import com.jrummyapps.android.roottools.box.BusyBox;
import com.jrummyapps.android.roottools.check.RootCheck;
import com.jrummyapps.android.roottools.files.AFile;
import com.jrummyapps.android.roottools.files.FileLister;
import com.jrummyapps.android.roottools.shell.stericson.Shell;
import com.jrummyapps.android.roottools.utils.Assets;
import com.jrummyapps.android.roottools.utils.Mount;
import com.jrummyapps.android.roottools.utils.Reboot;
import com.jrummyapps.busybox.R;
import com.jrummyapps.busybox.utils.BusyBoxZipHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.jrummyapps.android.io.PermissionsHelper.S_IRGRP;
import static com.jrummyapps.android.io.PermissionsHelper.S_IROTH;
import static com.jrummyapps.android.io.PermissionsHelper.S_IRUSR;
import static com.jrummyapps.android.io.PermissionsHelper.S_IWUSR;
import static com.jrummyapps.android.io.PermissionsHelper.S_IXGRP;
import static com.jrummyapps.android.io.PermissionsHelper.S_IXOTH;
import static com.jrummyapps.android.io.PermissionsHelper.S_IXUSR;

public class Installer implements Runnable {

  private static final int MODE_EXECUTABLE = S_IRUSR | S_IWUSR | S_IXUSR | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH;

  public static final String ERROR_NOT_ROOTED = "Root is required to install busybox";

  public static Builder newBusyboxInstaller() {
    return new Builder();
  }

  public final AFile binary;
  public final String asset;
  public final String path;
  public final String filename;
  public final boolean symlink;
  public final boolean overwrite;
  public final boolean recovery;

  private Installer(Builder builder) {
    binary = builder.binary;
    asset = builder.asset;
    path = builder.path;
    filename = builder.filename;
    symlink = builder.symlink;
    overwrite = builder.overwrite;
    recovery = builder.recovery;
  }

  @Override public void run() {
    Events.post(new StartEvent(this));

    if (!RootCheck.getInstance().accessGranted) {
      Events.post(new ErrorEvent(this, ERROR_NOT_ROOTED));
      return;
    }

    AFile dtFile = new AFile(path, filename);
    AFile parent = dtFile.getParentFile();
    AFile srFile;

    if (asset != null) {
      Assets.transferAsset(App.getContext(), asset, filename, MODE_EXECUTABLE);
      srFile = new AFile(App.getContext().getFilesDir(), filename);
    } else {
      srFile = binary;
    }

    if (recovery) {
      File updateZip = new File(App.getContext().getFilesDir(), "update.zip");
      BusyBox busybox = BusyBox.from(srFile.path);
      try {
        BusyBoxZipHelper.createBusyboxRecoveryZip(busybox, dtFile.getAbsolutePath(), updateZip);

        File commandTemp = new File(App.getContext().getFilesDir(), "command");
        FileUtils.writeNewFile(commandTemp, "--update_package=CACHE:busybox.zip'");
        RootTools.cp(commandTemp, new File("/cache/recovery/command"));
        RootTools.chmod("755", "/cache/recovery/command");
        RootTools.cp(updateZip, new File("/cache/busybox.zip"));
        RootTools.chmod("755", "/cache/busybox.zip");

        // TWRP reportedly does not find the update package
        FileUtils.writeNewFile(commandTemp, "install /cache/busybox.zip");
        RootTools.cp(commandTemp, new File("/cache/recovery/openrecoveryscript"));
        RootTools.chmod("755", "/cache/recovery/openrecoveryscript");

        commandTemp.delete();
        updateZip.delete();

        Reboot.RECOVERY.execute();

        Events.post(new FinishedEvent(this));
      } catch (IOException e) {
        Events.post(new ErrorEvent(this, "Error creating installable zip"));
        Crashlytics.logException(e);
      }
    } else {
      Mount mount = Mount.getMount(path);
      if (mount == null) {
        Events.post(new ErrorEvent(this, "Error getting mount point for " + path));
        return;
      }

      boolean mountedReadWrite = mount.isMountedReadWrite();

      if (!mount.remountReadWrite()) {
        Events.post(new ErrorEvent(this, "Error mounting " + mount.mountPoint + " read/write"));
        return;
      }

      if (parent != null && !parent.isDirectory()) {
        if (parent.isOnRemovableStorage()) {
          ExternalStorageHelper.mkdir(parent);
        } else if (parent.isOnExternalStorage()) {
          //noinspection ResultOfMethodCallIgnored
          parent.mkdirs();
        } else {
          RootTools.mkdir(parent);
          RootTools.chmod("0755", parent);
          RootTools.chown("root", "shell", parent);
        }
      }

      if (dtFile.path.equals("/sbin/busybox")) {
        dtFile.setFileInfo(FileLister.getFileInfo(dtFile.path));
      }

      if (dtFile.exists()) {
        Uninstaller.uninstall(dtFile);
      }

      if (!RootTools.cp(srFile, dtFile)) {
        Events.post(new ErrorEvent(this, "Failed copying " + srFile + " to " + dtFile));
        return;
      }

      RootTools.chmod("0755", dtFile);
      RootTools.chown("root", "root", dtFile);

      BusyBox busyBox = BusyBox.from(dtFile.getAbsolutePath());

      if (overwrite && symlink && Storage.isSystemFile(dtFile)) {
        mount.remountReadWrite();
        List<String> applets = busyBox.getApplets();
        for (String applet : applets) {
          File file = new File(path, applet);
          if (file.exists()) {
            RootTools.rm(file);
          }
        }
      }

      if (symlink && Storage.isSystemFile(dtFile)) {
        mount.remountReadWrite();
        if (!Shell.SU.run("\"" + busyBox.path + "\" --install -s \"" + path + "\"").success()) {
          // "--install" is not a command, symlink applets one by one.
          List<String> applets = busyBox.getApplets();
          for (String applet : applets) {
            AFile file = new AFile(path, applet);
            RootTools.ln(busyBox, file);
          }
        }
      }

      if (!mountedReadWrite) {
        mount.remountReadOnly();
      }

      Events.post(new FinishedEvent(this));
    }
  }

  public static final class StartEvent {

    public final Installer installer;

    public StartEvent(Installer installer) {
      this.installer = installer;
    }

  }

  public static final class FinishedEvent {

    public final Installer installer;

    public FinishedEvent(Installer installer) {
      this.installer = installer;
    }

  }

  public static final class ErrorEvent {

    public final Installer installer;
    public final String error;

    public ErrorEvent(Installer installer, String error) {
      this.installer = installer;
      this.error = error;
    }

  }

  public static final class Builder implements Parcelable {

    private AFile binary;
    private String asset;
    private String path;
    private String filename;
    private boolean symlink;
    private boolean overwrite;
    private boolean recovery;

    private Builder() {
      symlink = Prefs.getInstance().get("symlink_busybox_applets", true);
      overwrite = Prefs.getInstance().get("replace_with_busybox_applets", false);
      recovery = Prefs.getInstance().get("install_busybox_in_recovery", false);
    }

    public Installer create() {
      return new Installer(this);
    }

    public void confirm(Activity activity) {
      ConfirmInstallDialog dialog = new ConfirmInstallDialog();
      Bundle args = new Bundle();
      args.putParcelable("builder", this);
      dialog.setArguments(args);
      // Fix “Error Can not perform this action after onSaveInstanceState”
      // http://stackoverflow.com/a/12464899/1048340
      FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
      transaction.add(dialog, "ConfirmInstallDialog").commitAllowingStateLoss();
      //dialog.show(activity.getFragmentManager(), "ConfirmInstallDialog");
    }

    public Builder setBinary(AFile binary) {
      this.binary = binary;
      return this;
    }

    public Builder setAsset(String asset) {
      this.asset = asset;
      return this;
    }

    public Builder setPath(String path) {
      this.path = path;
      return this;
    }

    public Builder setFilename(String filename) {
      this.filename = filename;
      return this;
    }

    public Builder setSymlink(boolean symlink) {
      this.symlink = symlink;
      return this;
    }

    public Builder setOverwrite(boolean overwrite) {
      this.overwrite = overwrite;
      return this;
    }

    public Builder setRecovery(boolean recovery) {
      this.recovery = recovery;
      return this;
    }

    @Override public int describeContents() {
      return 0;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
      dest.writeParcelable(this.binary, 0);
      dest.writeString(this.asset);
      dest.writeString(this.path);
      dest.writeString(this.filename);
      dest.writeByte(symlink ? (byte) 1 : (byte) 0);
      dest.writeByte(overwrite ? (byte) 1 : (byte) 0);
    }

    protected Builder(Parcel in) {
      this.binary = in.readParcelable(AFile.class.getClassLoader());
      this.asset = in.readString();
      this.path = in.readString();
      this.filename = in.readString();
      this.symlink = in.readByte() != 0;
      this.overwrite = in.readByte() != 0;
    }

    public static final Parcelable.Creator<Builder> CREATOR = new Parcelable.Creator<Builder>() {

      public Builder createFromParcel(Parcel source) {
        return new Builder(source);
      }

      public Builder[] newArray(int size) {
        return new Builder[size];
      }
    };

  }

  public static class ConfirmInstallDialog extends DialogFragment {

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
      final Builder builder = getArguments().getParcelable("builder");
      return new AlertDialog.Builder(getActivity())
          .setTitle(R.string.confirm_before_install)
          .setMessage(getString(R.string.are_you_sure_you_want_to_install_s_to_s, builder.filename, builder.path))
          .setNegativeButton(android.R.string.cancel, null)
          .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            @Override public void onClick(DialogInterface dialog, int which) {
              new Thread(new Installer(builder)).start();
              dialog.dismiss();
            }
          })
          .create();
    }

  }

}
