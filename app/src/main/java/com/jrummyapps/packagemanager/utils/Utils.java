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
import com.jrummyapps.android.io.external.ExternalStorageHelper;
import com.jrummyapps.android.os.ABI;
import com.jrummyapps.android.roottools.RootTools;
import com.jrummyapps.android.roottools.box.BusyBox;
import com.jrummyapps.android.roottools.files.AFile;
import com.jrummyapps.android.roottools.shell.stericson.Shell;
import com.jrummyapps.android.roottools.utils.Assets;
import com.jrummyapps.android.roottools.utils.Mount;
import com.jrummyapps.android.roottools.utils.RootUtils;
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

import static com.jrummyapps.android.io.PermissionsHelper.S_IRGRP;
import static com.jrummyapps.android.io.PermissionsHelper.S_IROTH;
import static com.jrummyapps.android.io.PermissionsHelper.S_IRUSR;
import static com.jrummyapps.android.io.PermissionsHelper.S_IWUSR;
import static com.jrummyapps.android.io.PermissionsHelper.S_IXGRP;
import static com.jrummyapps.android.io.PermissionsHelper.S_IXOTH;
import static com.jrummyapps.android.io.PermissionsHelper.S_IXUSR;

public class Utils {

  private static final String TAG = "Utils";

  /**
   * 0755 (rwxr-xr-x)
   */
  public static final int MODE_EXECUTABLE = S_IRUSR | S_IWUSR | S_IXUSR | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH;

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

  private static boolean deleteFile(AFile file) {
    return file.isOnRemovableStorage() && ExternalStorageHelper.delete(file) || file.delete() || RootTools.rm(file);
  }

  public static List<AFile> getSymlinks(AFile binary) {
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

  public static boolean deleteSymlinks(AFile binary) {
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

  public static boolean uninstallBinary(AFile binary) {
    return deleteFile(binary) && deleteSymlinks(binary);
  }

  public static void installBusyboxFromAsset(BinaryInfo binaryInfo, String path, boolean symlink, boolean overwrite) {

    // TODO: clean up!

    Assets.transferAsset(App.getContext(), binaryInfo.path, binaryInfo.filename, MODE_EXECUTABLE);

    Mount mount = Mount.getMount(path);
    if (mount == null) {
      throw new RuntimeException("Error getting mount point for " + path);
    }

    boolean mountedReadWrite = mount.isMountedReadWrite();

    if (!mount.remountReadWrite()) {
      throw new RuntimeException("Failed mounting " + mount.mountPoint + " read/write");
    }

    AFile srFile = new AFile(App.getContext().getFilesDir(), binaryInfo.filename);
    AFile dtFile = new AFile(path, binaryInfo.filename);

    if (dtFile.exists()) {
      uninstallBinary(dtFile);
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
      mount.remountReadWrite();
      List<String> applets = busyBox.getApplets();
      for (String applet : applets) {
        File file = new File(path, applet);
        if (file.exists()) {
          RootTools.rm(file);
        }
      }
    }

    if (symlink) {
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
  }

}
