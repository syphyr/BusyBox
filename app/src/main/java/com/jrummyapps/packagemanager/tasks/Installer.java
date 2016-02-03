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

import com.jrummyapps.android.app.App;
import com.jrummyapps.android.eventbus.Events;
import com.jrummyapps.android.io.Storage;
import com.jrummyapps.android.io.external.ExternalStorageHelper;
import com.jrummyapps.android.roottools.RootTools;
import com.jrummyapps.android.roottools.box.BusyBox;
import com.jrummyapps.android.roottools.files.AFile;
import com.jrummyapps.android.roottools.files.FileLister;
import com.jrummyapps.android.roottools.shell.stericson.Shell;
import com.jrummyapps.android.roottools.utils.Assets;
import com.jrummyapps.android.roottools.utils.Mount;

import java.io.File;
import java.util.List;

import static com.jrummyapps.android.io.PermissionsHelper.S_IRGRP;
import static com.jrummyapps.android.io.PermissionsHelper.S_IROTH;
import static com.jrummyapps.android.io.PermissionsHelper.S_IRUSR;
import static com.jrummyapps.android.io.PermissionsHelper.S_IWUSR;
import static com.jrummyapps.android.io.PermissionsHelper.S_IXGRP;
import static com.jrummyapps.android.io.PermissionsHelper.S_IXOTH;
import static com.jrummyapps.android.io.PermissionsHelper.S_IXUSR;

public class Installer implements Runnable {

  private static final int MODE_EXECUTABLE = S_IRUSR | S_IWUSR | S_IXUSR | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH;

  public final AFile binary;
  public final String asset;
  public final String path;
  public final String filename;
  public final boolean symlink;
  public final boolean overwrite;

  private boolean running;

  private Installer(Builder builder) {
    binary = builder.binary;
    asset = builder.asset;
    path = builder.path;
    filename = builder.filename;
    symlink = builder.symlink;
    overwrite = builder.overwrite;
  }

  @Override public void run() {
    running = true;

    Events.post(this);

    AFile srFile;
    if (asset != null) {
      Assets.transferAsset(App.getContext(), asset, filename, MODE_EXECUTABLE);
      srFile = new AFile(App.getContext().getFilesDir(), filename);
    } else {
      srFile = binary;
    }

    Mount mount = Mount.getMount(path);
    if (mount == null) {
      throw new RuntimeException("Error getting mount point for " + path);
    }

    boolean mountedReadWrite = mount.isMountedReadWrite();

    if (!mount.remountReadWrite()) {
      throw new RuntimeException("Failed mounting " + mount.mountPoint + " read/write");
    }

    AFile dtFile = new AFile(path, filename);
    AFile parent = dtFile.getParentFile();

    if (parent != null && !parent.isDirectory()) {
      if (parent.isOnRemovableStorage()) {
        ExternalStorageHelper.mkdir(parent);
      } else if (parent.isOnExternalStorage()) {
        //noinspection ResultOfMethodCallIgnored
        parent.mkdirs(); // TODO: add storage permission
      } else {
        RootTools.mkdir(parent);
        RootTools.chmod("0755", parent);
        RootTools.chown("root", "shell", parent);
      }
    }

    if (dtFile.path.equals("/sbin/busybox")) {
      dtFile.setFileInfo(FileLister.getFileInfo(dtFile.path));
    }

    if (dtFile.exists()) {
      Uninstaller.uninstall(dtFile);
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

    if (overwrite && symlink && Storage.isSystemFile(dtFile)) {
      mount.remountReadWrite();
      List<String> applets = busyBox.getApplets();
      for (String applet : applets) {
        File file = new File(path, applet);
        if (file.exists()) {
          RootTools.rm(file);
        }
      }
    }

    if (symlink && Storage.isSystemFile(dtFile)) {
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

    running = false;

    Events.post(this);
  }

  public boolean isRunning() {
    return running;
  }

  public static final class Builder {

    private AFile binary;
    private String asset;
    private String path;
    private String filename;
    private boolean symlink;
    private boolean overwrite;

    public Installer create() {
      return new Installer(this);
    }

    public Builder setBinary(AFile binary) {
      this.binary = binary;
      return this;
    }

    public Builder setAsset(String asset) {
      this.asset = asset;
      return this;
    }

    public Builder setPath(String path) {
      this.path = path;
      return this;
    }

    public Builder setFilename(String filename) {
      this.filename = filename;
      return this;
    }

    public Builder setSymlink(boolean symlink) {
      this.symlink = symlink;
      return this;
    }

    public Builder setOverwrite(boolean overwrite) {
      this.overwrite = overwrite;
      return this;
    }

  }

}
