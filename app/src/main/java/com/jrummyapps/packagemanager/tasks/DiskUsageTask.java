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

package com.jrummyapps.packagemanager.tasks;

import android.os.AsyncTask;
import android.os.StatFs;

import com.jrummyapps.android.eventbus.Events;
import com.jrummyapps.android.io.Storage;
import com.jrummyapps.android.roottools.utils.Mount;
import com.jrummyapps.packagemanager.models.BinaryInfo;

public class DiskUsageTask extends AsyncTask<Void, Void, Long[]> {

  private final BinaryInfo binaryInfo;
  private final String path;

  public DiskUsageTask(BinaryInfo binaryInfo, String path) {
    this.binaryInfo = binaryInfo;
    this.path = path;
  }

  @Override protected Long[] doInBackground(Void... params) {
    Mount mount = Mount.getMount(path);
    long total, free;

    String fileSystemPath;
    if (mount == null || mount.mountPoint.equals("/")) {
      fileSystemPath = Storage.ANDROID_ROOT.getAbsolutePath();
    } else {
      fileSystemPath = mount.mountPoint;
    }

    StatFs statFs = new StatFs(fileSystemPath);

    total = Storage.getTotalSpace(statFs);
    free = Storage.getFreeSpace(statFs);

    if (total == 0) {
      statFs.restat(Storage.ANDROID_ROOT.getAbsolutePath());
      total = Storage.getTotalSpace(statFs);
      free = Storage.getFreeSpace(statFs);
    }

    return new Long[]{total, free};
  }

  @Override protected void onPostExecute(Long[] sizes) {
    Events.post(new BusyBoxDiskUsageEvent(binaryInfo, path, sizes[0], sizes[1]));
  }

  public static final class BusyBoxDiskUsageEvent {

    public final BinaryInfo binaryInfo;
    public final String path;
    public final long total;
    public final long free;

    public BusyBoxDiskUsageEvent(BinaryInfo binaryInfo, String path, long total, long free) {
      this.binaryInfo = binaryInfo;
      this.path = path;
      this.total = total;
      this.free = free;
    }

  }

}
