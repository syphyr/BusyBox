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

public class AssetBinary implements Parcelable {

  public final String name;
  public final String abi;
  public final String path;

  public AssetBinary(String name, String abi, String path) {
    this.name = name;
    this.abi = abi;
    this.path = path;
  }

  @Override public String toString() {
    return name;
  }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.name);
    dest.writeString(this.abi);
    dest.writeString(this.path);
  }

  protected AssetBinary(Parcel in) {
    this.name = in.readString();
    this.abi = in.readString();
    this.path = in.readString();
  }

  public static final Parcelable.Creator<AssetBinary> CREATOR = new Parcelable.Creator<AssetBinary>() {

    public AssetBinary createFromParcel(Parcel source) {
      return new AssetBinary(source);
    }

    public AssetBinary[] newArray(int size) {
      return new AssetBinary[size];
    }
  };

}
