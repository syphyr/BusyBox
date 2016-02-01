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

import android.os.Build;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.jrummyapps.android.app.App;
import com.jrummyapps.android.os.ABI;
import com.jrummyapps.packagemanager.R;
import com.jrummyapps.packagemanager.models.BinaryInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

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
        int minSdk = jsonObject.optInt("maxsdk", Build.VERSION.SDK_INT);
        binaries.add(new BinaryInfo(name, filename, abi, path, size, minSdk));
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
    for (BinaryInfo binary : binaries) {
      System.out.println(binary.path);
    }
    return binaries;
  }

}
