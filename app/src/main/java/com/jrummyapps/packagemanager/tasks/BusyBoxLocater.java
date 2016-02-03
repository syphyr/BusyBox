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
import android.support.annotation.Nullable;

import com.jrummyapps.android.eventbus.Events;
import com.jrummyapps.android.io.Storage;
import com.jrummyapps.android.roottools.box.BusyBox;
import com.jrummyapps.android.roottools.files.FileInfo;
import com.jrummyapps.android.roottools.files.FileLister;

import java.io.File;

public class BusyBoxLocater extends AsyncTask<Void, Void, BusyBox> {

  @Override protected BusyBox doInBackground(Void... params) {
    for (String path : Storage.PATH) {
      BusyBox busybox = BusyBox.from(new File(path, "busybox").getAbsolutePath());
      if (path.equals("/sbin")) {
        // /sbin is not readable. Get file info with root
        FileInfo fileInfo = FileLister.getFileInfo("/sbin/busybox");
        if (fileInfo != null) {
          busybox.setFileInfo(fileInfo);
          return busybox;
        }
      }
      if (busybox.exists()) {
        return busybox;
      }
    }
    return null;
  }

  @Override protected void onPostExecute(BusyBox busybox) {
    Events.post(new BusyboxLocatedEvent(busybox));
  }

  public static final class BusyboxLocatedEvent {

    @Nullable public final BusyBox busybox;

    public BusyboxLocatedEvent(BusyBox busybox) {
      this.busybox = busybox;
    }

  }

}
