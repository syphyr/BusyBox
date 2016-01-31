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

package com.jrummyapps.packagemanager.utils;

import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.jrummyapps.android.app.App;
import com.jrummyapps.android.io.PermissionsHelper;
import com.jrummyapps.android.io.Storage;
import com.jrummyapps.android.os.ABI;
import com.jrummyapps.android.roottools.RootTools;
import com.jrummyapps.android.roottools.box.BusyBox;
import com.jrummyapps.android.roottools.shell.stericson.Shell;
import com.jrummyapps.android.roottools.utils.Assets;
import com.jrummyapps.android.roottools.utils.Mount;
import com.jrummyapps.packagemanager.R;
import com.jrummyapps.packagemanager.models.BinaryInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Utils {

  /**
   * Get a list of binaries in the assets directory
   *
   * @return a list of binaries from the assets in this APK file.
   */
  public static ArrayList<BinaryInfo> getBinariesFromAssets() {
    ArrayList<BinaryInfo> binaries = new ArrayList<>();
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      InputStream input = App.getContext().getResources().openRawResource(R.raw.binaries);
      byte[] buffer = new byte[4096];
      int n;
      while ((n = input.read(buffer)) != -1) {
        output.write(buffer, 0, n);
      }
      input.close();
      JSONArray jsonArray = new JSONArray(output.toString());
      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject jsonObject = jsonArray.getJSONObject(i);
        String name = jsonObject.getString("name");
        String filename = jsonObject.getString("filename");
        String path = jsonObject.getString("path");
        String abi = jsonObject.getString("abi");
        long size = jsonObject.getLong("size");
        binaries.add(new BinaryInfo(name, filename, abi, path, size));
      }
    } catch (Exception e) {
      Crashlytics.logException(e);
    }
    return binaries;
  }

  /**
   * Get a list of supported binaries for the given ABI.
   *
   * @param abi
   *     the {@link ABI} to filter
   * @return a list of binaries from the assets in this APK file.
   */
  public static ArrayList<BinaryInfo> getBinariesFromAssets(ABI abi) {
    ArrayList<BinaryInfo> binaries = getBinariesFromAssets();
    for (Iterator<BinaryInfo> iterator = binaries.iterator(); iterator.hasNext(); ) {
      if (!TextUtils.equals(iterator.next().abi, abi.base)) {
        iterator.remove();
      }
    }
    return binaries;
  }

  public static final int RWXR_XR_X = PermissionsHelper.S_IRUSR
      | PermissionsHelper.S_IWUSR
      | PermissionsHelper.S_IXUSR
      | PermissionsHelper.S_IRGRP
      | PermissionsHelper.S_IXGRP
      | PermissionsHelper.S_IROTH
      | PermissionsHelper.S_IXOTH;

  public static void installFromAssets(BinaryInfo binaryInfo, String path, boolean symlink, boolean overwrite) {

    // TODO: clean up!

    Assets.transferAsset(App.getContext(), binaryInfo.path, binaryInfo.filename, RWXR_XR_X);

    Mount mount = Mount.getMount(path);
    if (mount == null) {
      throw new RuntimeException("Error getting mount point for " + path);
    }

    boolean mountedReadWrite = mount.isMountedReadWrite();

    if (!mount.remountReadWrite()) {
      throw new RuntimeException("Failed mounting " + mount.mountPoint + " read/write");
    }

    File srFile = new File(App.getContext().getFilesDir(), binaryInfo.filename);
    File dtFile = new File(path, binaryInfo.filename);

    // remove old busybox binaries
    for (String systemPath : Storage.PATH) {
      File file = new File(systemPath, binaryInfo.filename);
      if (file.exists() && file.equals(dtFile) && !systemPath.equals(path)) {
        RootTools.rm(file);
      }
    }

    if (!RootTools.cp(srFile, dtFile)) {
      throw new RuntimeException("Failed copying " + srFile + " to " + dtFile);
    }
    if (!RootTools.chmod("0755", dtFile)) {
      throw new RuntimeException("Failed to give permissions to " + dtFile);
    }
    if (!RootTools.chown("root", "root", dtFile)) {
      throw new RuntimeException("Failed to set user/group to root/root for " + dtFile);
    }

    BusyBox busyBox = BusyBox.from(dtFile.getAbsolutePath());

    if (overwrite && symlink) {
      List<String> applets = busyBox.getApplets();
      for (String applet : applets) {
        File file = new File(path, applet);
        if (file.exists()) {
          RootTools.rm(file);
        }
      }
    }

    if (symlink) {
      if (!Shell.SU.run("\"" + busyBox.path + "\" --install -s \"" + path + "\"").success()) {
        List<String> applets = busyBox.getApplets();
        for (String applet : applets) {
          File file = new File(path, applet);
          RootTools.ln(busyBox, file);
        }
      }
    }

    if (!mountedReadWrite) {
      mount.remountReadOnly();
    }

  }

}
