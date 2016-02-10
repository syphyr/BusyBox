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

package com.jrummyapps.busybox.utils;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.jrummyapps.android.io.IOUtils;
import com.jrummyapps.android.os.ABI;
import com.jrummyapps.android.roottools.box.BusyBox;
import com.jrummyapps.busybox.R;
import com.jrummyapps.busybox.models.BinaryInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static com.jrummyapps.android.app.App.getContext;

public class Utils {

  static String deviceId;
  static String androidId;

  public static String getDeviceId(Context context) {
    if (deviceId == null) {
      androidId =
          Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
      try {
        // Create MD5 Hash
        MessageDigest digest = java.security.MessageDigest
            .getInstance("MD5");
        digest.update(androidId.getBytes());
        byte messageDigest[] = digest.digest();
        // Create Hex String
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++) {
          String h = Integer.toHexString(0xFF & messageDigest[i]);
          while (h.length() < 2)
            h = "0" + h;
          hexString.append(h);
        }
        deviceId = hexString.toString().toUpperCase(Locale.ENGLISH);
      } catch (NoSuchAlgorithmException ignored) {
      }
    }
    return deviceId;
  }

  public static List<String> getBusyBoxApplets() {
    BusyBox busyBox = BusyBox.getInstance();
    List<String> applets = busyBox.getApplets();
    if (applets.isEmpty()) {
      String json = readRaw(R.raw.busybox_applets);
      try {
        JSONObject jsonObject=new JSONObject(json);
        for(Iterator<String> iterator = jsonObject.keys(); iterator.hasNext(); ) {
          applets.add(iterator.next());
        }
      } catch (Exception ignored) {
      }
    }
    Collections.sort(applets);
    return applets;
  }

  /**
   * Read a file from /res/raw
   *
   * @param id
   *     The id from R.raw
   * @return The contents of the file or {@code null} if reading failed.
   */
  public static String readRaw(int id) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    InputStream inputStream = getContext().getResources().openRawResource(id);
    try {
      IOUtils.copy(inputStream, outputStream);
    } catch (IOException e) {
      return null;
    } finally {
      IOUtils.closeQuietly(outputStream);
      IOUtils.closeQuietly(inputStream);
    }
    return outputStream.toString();
  }

  /**
   * Get a list of binaries in the assets directory
   *
   * @return a list of binaries from the assets in this APK file.
   */
  public static ArrayList<BinaryInfo> getBinariesFromAssets() {
    ArrayList<BinaryInfo> binaries = new ArrayList<>();
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      InputStream input = getContext().getResources().openRawResource(R.raw.binaries);
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
        String abi = jsonObject.getString("abi");
        String path = jsonObject.getString("path");
        String md5sum = jsonObject.getString("md5sum");
        long size = jsonObject.getLong("size");
        int minSdk = jsonObject.optInt("maxsdk", Build.VERSION.SDK_INT);
        binaries.add(new BinaryInfo(name, filename, abi, path, md5sum, size, minSdk));
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
      BinaryInfo binaryInfo = iterator.next();
      if (!TextUtils.equals(binaryInfo.abi, abi.base) || binaryInfo.maxSdk < Build.VERSION.SDK_INT) {
        iterator.remove();
      }
    }
    return binaries;
  }

}
