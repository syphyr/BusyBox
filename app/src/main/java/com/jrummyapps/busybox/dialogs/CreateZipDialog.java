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

package com.jrummyapps.busybox.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.jrummyapps.android.dialog.BaseDialogFragment;
import com.jrummyapps.android.eventbus.Events;
import com.jrummyapps.android.util.KeyboardUtils;
import com.jrummyapps.busybox.R;

import java.io.File;

public class CreateZipDialog extends BaseDialogFragment {

  /**
   * Display the dialog
   *
   * @param activity
   *     the current activity
   * @param directory
   *     the directory to save the file in
   * @param filename
   *     the name of the file
   */
  public static void show(Activity activity, File directory, String filename) {
    CreateZipDialog dialog = new CreateZipDialog();
    Bundle args = new Bundle();
    args.putString("path", directory.getAbsolutePath());
    args.putString("filename", filename);
    dialog.setArguments(args);
    dialog.show(activity.getFragmentManager(), "CreateZipDialog");
  }

  private EditText editText;

  @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    @SuppressLint("InflateParams")
    final View view = getActivity().getLayoutInflater().inflate(R.layout.editor__dialog_save_as, null);
    editText = (EditText) view.findViewById(R.id.edittext);

    final String path = getArguments().getString("path");
    final String name = getArguments().getString("filename", "update.zip");
    final int stringRes = new File(path, name).exists() ? R.string.overwrite : R.string.create;
    editText.setHint(name);
    editText.setText(name);

    // TODO: make sure filename is valid. Show error message if invalid.
    editText.addTextChangedListener(new TextWatcher() {

      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override public void afterTextChanged(Editable s) {
        if (getDialog() instanceof AlertDialog) {
          AlertDialog dialog = (AlertDialog) getDialog();
          Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
          String text = s.toString().trim();
          if (text.length() == 0 && button.isEnabled()) {
            button.setEnabled(false);
          } else {
            if (!button.isEnabled()) {
              button.setEnabled(true);
            }
            if (new File(path, text).exists()) {
              button.setText(R.string.overwrite);
            } else {
              button.setText(R.string.save);
            }
          }
        }
      }
    });

    return new AlertDialog.Builder(getActivity())
        .setTitle(path)
        .setView(view)
        .setPositiveButton(stringRes, new DialogInterface.OnClickListener() {

          @Override public void onClick(DialogInterface dialog, int which) {
            KeyboardUtils.hideKeyboard(editText);
            dialog.dismiss();
            String filename = editText.getText().toString().trim();
            File file = new File(path, filename);
            Events.post(new CreateZipEvent(file));
          }
        }).create();
  }

  @Override public void onStart() {
    super.onStart();
    editText.setSelection(0, editText.getText().length());
    KeyboardUtils.showKeyboard(editText, true);
  }

  public static final class CreateZipEvent {

    public final File file;

    public CreateZipEvent(File file) {
      this.file = file;
    }

  }

}
