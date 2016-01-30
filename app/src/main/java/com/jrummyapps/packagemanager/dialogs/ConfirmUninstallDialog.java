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

package com.jrummyapps.packagemanager.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.jrummyapps.android.eventbus.Events;
import com.jrummyapps.android.roottools.files.AFile;
import com.jrummyapps.packagemanager.R;
import com.jrummyapps.packagemanager.events.RequestUninstallBinaryEvent;

public class ConfirmUninstallDialog extends DialogFragment {

  public static void show(Activity activity, AFile file) {
    ConfirmUninstallDialog dialog = new ConfirmUninstallDialog();
    Bundle args = new Bundle();
    args.putParcelable("file", file);
    dialog.setArguments(args);
    dialog.show(activity.getFragmentManager(), "ConfirmUninstallDialog");
  }

  @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    final AFile file = getArguments().getParcelable("file");
    return new AlertDialog.Builder(getActivity())
        .setTitle(R.string.confirm_before_deleting)
        .setMessage(getString(R.string.are_you_sure_you_want_to_uninstall_s, file.filename))
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

          @Override public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            Events.post(new RequestUninstallBinaryEvent(file));
          }
        })
        .create();
  }

}
