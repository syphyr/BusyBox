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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import com.jrummyapps.android.analytics.Analytics;
import com.jrummyapps.android.roottools.box.BusyBox;
import com.jrummyapps.packagemanager.R;
import com.jrummyapps.packagemanager.utils.Utils;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class BusyBoxAppletDialog extends DialogFragment {

  public static void show(Activity activity, String applet) {
    new AppletHelpTask(activity, applet).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  public static void show(Activity activity, String applet, String help) {
    BusyBoxAppletDialog dialog = new BusyBoxAppletDialog();
    Bundle args = new Bundle();
    args.putString("applet_name", applet);
    args.putString("applet_help", help);
    dialog.setArguments(args);
    dialog.show(activity.getFragmentManager(), "BusyBoxAppletDialog");
    Analytics.newEvent("applet help").put("applet", applet).log();
  }

  @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity())
        .setTitle(getArguments().getString("applet_name"))
        .setMessage(getArguments().getString("applet_help"))
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

          @Override public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
          }
        })
        .create();
  }

  private static class AppletHelpTask extends AsyncTask<Void, Void, String> {

    private static final HashMap<String, String> CACHE = new HashMap<>();

    private static String json;

    private final WeakReference<Activity> activityWeakReference;
    private final String applet;

    private AppletHelpTask(Activity activity, String name) {
      activityWeakReference = new WeakReference<>(activity);
      applet = name;
    }

    @Override protected String doInBackground(Void... params) {
      String help = CACHE.get(applet);
      if (help != null) {
        return help;
      }
      if (BusyBox.getInstance().exists()) {
        try {
          help = BusyBox.getInstance().getHelp(applet);
        } catch (Exception ignored) {
        }
        if (!TextUtils.isEmpty(help)) {
          CACHE.put(applet, help);
          return help;
        }
      }
      try {
        if (json == null) {
          json = Utils.readRaw(R.raw.busybox_applets);
        }
        help = new JSONObject(json).getString(applet);
        if (help != null) {
          CACHE.put(applet, help);
        }
      } catch (Exception ignored) {
      }
      return help;
    }

    @Override protected void onPostExecute(String help) {
      Activity activity = activityWeakReference.get();
      if (activity == null || activity.isFinishing()) {
        return;
      }
      show(activity, applet, help);
    }

  }

}
