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

package com.jrummyapps.packagemanager.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.jrummyapps.android.app.App;

import java.io.File;

public class BinaryInfo implements Parcelable {

  public final String name;
  public final String filename;
  public final String abi;
  public final String path;
  public final String md5sum;
  public final long size;
  public final int maxSdk;

  public BinaryInfo(String name, String filename, String abi, String path, String md5sum, long size, int maxSdk) {
    this.name = name;
    this.filename = filename;
    this.abi = abi;
    this.path = path;
    this.md5sum = md5sum;
    this.size = size;
    this.maxSdk = maxSdk;
  }

  public File getDownloadDestination() {
    String folder = name.replaceAll(" ", "_");
    return new File(App.getContext().getCacheDir(), folder + "/" + filename);
  }

  @Override public String toString() {
    return name;
  }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(name);
    dest.writeString(filename);
    dest.writeString(abi);
    dest.writeString(path);
    dest.writeString(md5sum);
    dest.writeLong(size);
    dest.writeInt(maxSdk);
  }

  protected BinaryInfo(Parcel in) {
    name = in.readString();
    filename = in.readString();
    abi = in.readString();
    path = in.readString();
    md5sum = in.readString();
    size = in.readLong();
    maxSdk = in.readInt();
  }

  public static final Parcelable.Creator<BinaryInfo> CREATOR = new Parcelable.Creator<BinaryInfo>() {

    @Override public BinaryInfo createFromParcel(Parcel source) {
      return new BinaryInfo(source);
    }

    @Override public BinaryInfo[] newArray(int size) {
      return new BinaryInfo[size];
    }

  };

}
