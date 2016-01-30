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

package com.jrummyapps.packagemanager.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.jaredrummler.materialspinner.MaterialSpinner.OnItemSelectedListener;
import com.jaredrummler.materialspinner.MaterialSpinner.OnNothingSelectedListener;
import com.jrummyapps.android.animations.Technique;
import com.jrummyapps.android.base.BaseFragment;
import com.jrummyapps.android.colors.Color;
import com.jrummyapps.android.directorypicker.DirectoryPickerDialog;
import com.jrummyapps.android.drawable.CircleDrawable;
import com.jrummyapps.android.drawable.TextDrawable;
import com.jrummyapps.android.eventbus.EventBusHook;
import com.jrummyapps.android.eventbus.Events;
import com.jrummyapps.android.fileproperties.charts.PieChart;
import com.jrummyapps.android.fileproperties.charts.PieModel;
import com.jrummyapps.android.html.HtmlBuilder;
import com.jrummyapps.android.io.Storage;
import com.jrummyapps.android.os.ABI;
import com.jrummyapps.android.roottools.files.AFile;
import com.jrummyapps.android.roottools.utils.Mount;
import com.jrummyapps.android.theme.ColorScheme;
import com.jrummyapps.android.util.ResUtils;
import com.jrummyapps.packagemanager.R;
import com.jrummyapps.packagemanager.dialogs.ConfirmUninstallDialog;
import com.jrummyapps.packagemanager.events.RequestUninstallBinaryEvent;
import com.jrummyapps.packagemanager.models.BinaryInfo;
import com.jrummyapps.packagemanager.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class BusyBoxInstaller extends BaseFragment implements
    DirectoryPickerDialog.OnDirectorySelectedListener,
    DirectoryPickerDialog.OnDirectoryPickerCancelledListener, View.OnClickListener {

  private static final String TAG = "BusyBoxInstaller";

  private static final String DEFAULT_INSTALL_PATH = "/system/xbin";

  private static final String REPO = ""; // TODO: add URL to install utilities

  private ArrayList<BinaryInfo> binaries;
  private ArrayList<String> paths;

  private MaterialSpinner binarySpinner;
  private MaterialSpinner directorySpinner;
  private PieChart pieChart;
  private Button installButton;
  private Button uninstallButton;
  private View backgroundShadow;

  private int selectedDirectoryPosition;

  private PieModel usedSlice;
  private PieModel freeSlice;
  private PieModel itemSlice;

  private AFile file;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Events.register(this);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    Events.unregister(this);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.busybox_installer, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    backgroundShadow = findById(R.id.background_shadow);
    installButton = findById(R.id.button_install);
    uninstallButton = findById(R.id.button_uninstall);
    pieChart = findById(R.id.piechart);
    binarySpinner = findById(R.id.binary_spinner);
    directorySpinner = findById(R.id.directory_spinner);

    onRestoreInstanceState(savedInstanceState);

    final OnNothingSelectedListener onNothingSelectedListener = new OnNothingSelectedListener() {

      @Override public void onNothingSelected(MaterialSpinner spinner) {
        Technique.FADE_OUT.getComposer().duration(500).hideOnFinished().playOn(backgroundShadow);
      }
    };

    binarySpinner.setItems(binaries);
    binarySpinner.setOnClickListener(this);
    binarySpinner.setOnNothingSelectedListener(onNothingSelectedListener);

    directorySpinner.setItems(paths);
    directorySpinner.setOnClickListener(this);
    directorySpinner.setOnNothingSelectedListener(onNothingSelectedListener);
    directorySpinner.setSelectedIndex(selectedDirectoryPosition);

    directorySpinner.setOnItemSelectedListener(new OnItemSelectedListener<String>() {

      @Override public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
        Technique.FADE_OUT.getComposer().duration(500).hideOnFinished().playOn(backgroundShadow);
        if (item.equals(getString(R.string.choose_a_directory))) {
          DirectoryPickerDialog.show(getActivity(), new File("/"));
        } else {
          selectedDirectoryPosition = view.getSelectedIndex();
          updateDiskUsagePieChart();
        }
      }
    });

    binarySpinner.setOnItemSelectedListener(new OnItemSelectedListener<BinaryInfo>() {

      @Override public void onItemSelected(MaterialSpinner view, int position, long id, BinaryInfo item) {
        Technique.FADE_OUT.getComposer().duration(500).hideOnFinished().playOn(backgroundShadow);
      }
    });

    uninstallButton.setEnabled(file != null && file.exists());
    uninstallButton.setOnClickListener(this);
    installButton.setOnClickListener(this);

    updateDiskUsagePieChart();
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt("selected_directory_position", selectedDirectoryPosition);
    outState.putStringArrayList("paths", paths);
    outState.putParcelableArrayList("binaries", binaries);
    outState.putParcelable("file", file);
  }

  @Override public void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    if (savedInstanceState != null) {
      selectedDirectoryPosition = savedInstanceState.getInt("selected_directory_position", -1);
      paths = savedInstanceState.getStringArrayList("paths");
      binaries = savedInstanceState.getParcelableArrayList("binaries");
      file = savedInstanceState.getParcelable("file");
    } else {
      paths = new ArrayList<>();
      paths.addAll(Arrays.asList(Storage.PATH));
      paths.add(getString(R.string.choose_a_directory));
      binaries = Utils.getBinariesFromAssets(ABI.getAbi());
      binaries.add(new BinaryInfo(getString(R.string.download_), null, ABI.getAbi().base, REPO, 0));
      String filename = binaries.get(0).filename;
      selectedDirectoryPosition = 0;
      for (int i = 0; i < Storage.PATH.length; i++) {
        String path = Storage.PATH[i];
        if (filename != null && new File(path, filename).exists()) {
          file = new AFile(path, filename);
          selectedDirectoryPosition = i;
          break;
        }
        if (path.equals(DEFAULT_INSTALL_PATH)) {
          selectedDirectoryPosition = i;
        }
      }
    }
  }

  @Override public void onClick(View v) {
    if (v == directorySpinner || v == binarySpinner) {
      backgroundShadow.setVisibility(View.VISIBLE);
      Technique.FADE_IN.getComposer().duration(500).playOn(backgroundShadow);
    } else if (v == uninstallButton) {
      ConfirmUninstallDialog.show(getActivity(), file);
    }
  }

  @Override public void onDirectorySelected(File directory) {
    if (paths.contains(directory.getAbsolutePath())) {
      for (int i = 0; i < paths.size(); i++) {
        if (paths.get(i).equals(directory.getAbsolutePath())) {
          selectedDirectoryPosition = i;
          directorySpinner.setSelectedIndex(selectedDirectoryPosition);
          updateDiskUsagePieChart();
          break;
        }
      }
    } else {
      selectedDirectoryPosition = paths.size() - 1;
      paths.add(selectedDirectoryPosition, directory.getAbsolutePath());
      directorySpinner.setSelectedIndex(selectedDirectoryPosition);
      updateDiskUsagePieChart();
    }
  }

  @Override public void onDirectoryPickerCancelledListener() {
    directorySpinner.setSelectedIndex(selectedDirectoryPosition);
  }

  @EventBusHook public void onEvent(RequestUninstallBinaryEvent event) {
    // TODO: uninstall the binary
  }

  private void updateDiskUsagePieChart() {
    BinaryInfo binaryInfo = binaries.get(binarySpinner.getSelectedIndex());
    String path = paths.get(directorySpinner.getSelectedIndex());
    new DiskUsageUpdater(binaryInfo, path).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private final class DiskUsageUpdater extends AsyncTask<Void, Void, Long[]> {

    private final BinaryInfo binaryInfo;
    private final String path;

    public DiskUsageUpdater(BinaryInfo binaryInfo, String path) {
      this.binaryInfo = binaryInfo;
      this.path = path;
    }

    @Override protected void onPreExecute() {
      findById(R.id.progress).setVisibility(View.VISIBLE);
      findById(R.id.text_used).setVisibility(View.GONE);
      findById(R.id.text_free).setVisibility(View.GONE);
      findById(R.id.text_item).setVisibility(View.GONE);
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
      long totalSize = sizes[0];
      long freeSize = sizes[1];
      long usedSize = totalSize - freeSize;

      int color1 = ColorScheme.getAccent();
      int color2 = ColorScheme.getAccentDark();
      int color3 = ColorScheme.getPrimary();
      if (color1 == color3) color3 = Color.invert(color1);

      if (itemSlice == null || freeSlice == null || usedSlice == null) {
        usedSlice = new PieModel(usedSize - binaryInfo.size, color1);
        freeSlice = new PieModel(freeSize, color2);
        itemSlice = new PieModel(binaryInfo.size, color3);
        pieChart.addPieSlice(usedSlice);
        pieChart.addPieSlice(freeSlice);
        pieChart.addPieSlice(itemSlice);
      } else {
        usedSlice.setValue(usedSize - binaryInfo.size);
        freeSlice.setValue(freeSize);
        itemSlice.setValue(binaryInfo.size);
        pieChart.update();
      }

      String item = binaryInfo.filename.length() >= 8 ? "binary" : binaryInfo.filename;

      setLegendText(R.id.text_used, getString(R.string.used).toUpperCase(), usedSize, totalSize, color1);
      setLegendText(R.id.text_free, getString(R.string.free).toUpperCase(), freeSize, totalSize, color2);
      setLegendText(R.id.text_item, item.toUpperCase(), binaryInfo.size, totalSize, color3);

      findById(R.id.text_used).setVisibility(View.VISIBLE);
      findById(R.id.text_free).setVisibility(View.VISIBLE);
      findById(R.id.text_item).setVisibility(View.VISIBLE);
      findById(R.id.progress).setVisibility(View.GONE);
    }

    private void setLegendText(int id, String title, long size, long total, int color) {
      String percent = formatPercent(size, total);
      TextDrawable legendDrawable = new TextDrawable(getActivity(), percent).setBackgroundColor(Color.TRANSPARENT);
      CircleDrawable drawable = new CircleDrawable(legendDrawable, color);
      drawable.setBounds(0, 0, ResUtils.dpToPx(32), ResUtils.dpToPx(32));
      TextView textView = findById(id);

      String text = "";
      for (int i = title.length(); i <= 10; i++) text += 'A';

      new HtmlBuilder()
          .strong()
          .append(title.toUpperCase())
          .font()
          .color(ColorScheme.getBackgroundLight(getActivity()))
          .text(text)
          .close()
          .close()
          .append(Formatter.formatFileSize(getActivity(), size))
          .on(textView);

      textView.setCompoundDrawables(drawable, null, null, null);
      textView.setVisibility(View.VISIBLE);
    }

    private String formatPercent(long n1, long n2) {
      float result;
      if (n2 == 0) {
        return "0%";
      } else {
        result = 1.0f * n1 / n2;
      }
      if (result < 0.01f) {
        return "< 1%";
      }
      return String.format(Locale.ENGLISH, "%d%%", (int) (100.0f * result));
    }

  }

}
