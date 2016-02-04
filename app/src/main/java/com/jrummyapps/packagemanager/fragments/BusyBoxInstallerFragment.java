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

import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.jaredrummler.materialspinner.MaterialSpinner.OnItemSelectedListener;
import com.jaredrummler.materialspinner.MaterialSpinner.OnNothingSelectedListener;
import com.jrummyapps.android.animations.Technique;
import com.jrummyapps.android.base.BaseFragment;
import com.jrummyapps.android.colors.Color;
import com.jrummyapps.android.directorypicker.DirectoryPickerDialog;
import com.jrummyapps.android.downloader.Download;
import com.jrummyapps.android.downloader.DownloadRequest;
import com.jrummyapps.android.downloader.dialogs.DownloadProgressDialog;
import com.jrummyapps.android.downloader.events.DownloadError;
import com.jrummyapps.android.downloader.events.DownloadFinished;
import com.jrummyapps.android.drawable.CircleDrawable;
import com.jrummyapps.android.drawable.TextDrawable;
import com.jrummyapps.android.eventbus.EventBusHook;
import com.jrummyapps.android.eventbus.Events;
import com.jrummyapps.android.fileproperties.activities.FilePropertiesActivity;
import com.jrummyapps.android.fileproperties.charts.PieChart;
import com.jrummyapps.android.fileproperties.charts.PieModel;
import com.jrummyapps.android.fileproperties.models.FileMeta;
import com.jrummyapps.android.html.HtmlBuilder;
import com.jrummyapps.android.io.FileHelper;
import com.jrummyapps.android.io.Storage;
import com.jrummyapps.android.os.ABI;
import com.jrummyapps.android.prefs.Prefs;
import com.jrummyapps.android.roottools.box.BusyBox;
import com.jrummyapps.android.roottools.files.AFile;
import com.jrummyapps.android.theme.ColorScheme;
import com.jrummyapps.android.theme.Themes;
import com.jrummyapps.android.util.ResUtils;
import com.jrummyapps.packagemanager.R;
import com.jrummyapps.packagemanager.activities.SettingsActivity;
import com.jrummyapps.packagemanager.models.BinaryInfo;
import com.jrummyapps.packagemanager.tasks.BusyBoxDiskUsageTask;
import com.jrummyapps.packagemanager.tasks.BusyBoxInstaller;
import com.jrummyapps.packagemanager.tasks.BusyBoxLocater;
import com.jrummyapps.packagemanager.tasks.BusyBoxMetaTask;
import com.jrummyapps.packagemanager.tasks.BusyBoxUninstaller;
import com.jrummyapps.packagemanager.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class BusyBoxInstallerFragment extends BaseFragment implements
    DirectoryPickerDialog.OnDirectorySelectedListener,
    DirectoryPickerDialog.OnDirectoryPickerCancelledListener,
    View.OnClickListener {

  private static final String DEFAULT_INSTALL_PATH = "/system/xbin";

  private ArrayList<FileMeta> properties;
  private ArrayList<BinaryInfo> binaries;
  private ArrayList<String> paths;
  private MaterialSpinner versionSpinner;
  private MaterialSpinner pathSpinner;
  private PieChart pieChart;
  private CardView propertiesCard;
  private Button installButton;
  private Button uninstallButton;
  private ImageButton infoButton;
  private View backgroundShadow;
  private MenuItem progressItem;
  private PieModel usedSlice;
  private PieModel freeSlice;
  private PieModel itemSlice;
  private BusyBox busybox;
  private int pathIndex;
  private boolean uninstalling;
  private boolean installing;
  private Download download;

  private final OnItemSelectedListener<String> onPathSelectedListener = new OnItemSelectedListener<String>() {

    @Override public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
      Technique.FADE_OUT.getComposer().duration(500).hideOnFinished().playOn(backgroundShadow);
      if (item.equals(getString(R.string.choose_a_directory))) {
        DirectoryPickerDialog.show(getActivity(), new File("/"));
      } else {
        pathIndex = view.getSelectedIndex();
        updateDiskUsagePieChart();
      }
    }
  };

  private final OnItemSelectedListener<BinaryInfo> onBinarySelectedListener = new OnItemSelectedListener<BinaryInfo>() {

    @Override public void onItemSelected(MaterialSpinner view, int position, long id, BinaryInfo item) {
      Technique.FADE_OUT.getComposer().duration(500).hideOnFinished().playOn(backgroundShadow);
    }
  };

  private final OnNothingSelectedListener onNothingSelectedListener = new OnNothingSelectedListener() {

    @Override public void onNothingSelected(MaterialSpinner spinner) {
      Technique.FADE_OUT.getComposer().duration(500).hideOnFinished().playOn(backgroundShadow);
    }
  };

  // --------------------------------------------------------------------------------------------

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Events.register(this);
    setHasOptionsMenu(true);
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
    propertiesCard = findById(R.id.properties_layout);
    versionSpinner = findById(R.id.binary_spinner);
    pathSpinner = findById(R.id.directory_spinner);
    infoButton = findById(R.id.properties_button);
    onRestoreInstanceState(savedInstanceState);
    versionSpinner.setItems(binaries);
    versionSpinner.setOnClickListener(this);
    versionSpinner.setOnNothingSelectedListener(onNothingSelectedListener);
    versionSpinner.setOnItemSelectedListener(onBinarySelectedListener);
    pathSpinner.setItems(paths);
    pathSpinner.setOnClickListener(this);
    pathSpinner.setOnNothingSelectedListener(onNothingSelectedListener);
    pathSpinner.setSelectedIndex(pathIndex);
    pathSpinner.setOnItemSelectedListener(onPathSelectedListener);
    infoButton.setColorFilter(ColorScheme.getSubMenuIcon());
    uninstallButton.setOnClickListener(this);
    installButton.setOnClickListener(this);
    infoButton.setOnClickListener(this);
    uninstallButton.setEnabled(busybox != null && busybox.exists() && !installing && !uninstalling);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt("path_index", pathIndex);
    outState.putStringArrayList("paths", paths);
    outState.putParcelableArrayList("binaries", binaries);
    outState.putParcelable("busybox", busybox);
    outState.putBoolean("uninstalling", uninstalling);
    outState.putBoolean("installing", installing);
    outState.putParcelable("download", download);
    outState.putParcelableArrayList("properties", properties);
  }

  @Override public void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    if (savedInstanceState != null) {
      pathIndex = savedInstanceState.getInt("path_index", -1);
      paths = savedInstanceState.getStringArrayList("paths");
      binaries = savedInstanceState.getParcelableArrayList("binaries");
      busybox = savedInstanceState.getParcelable("busybox");
      uninstalling = savedInstanceState.getBoolean("uninstalling");
      installing = savedInstanceState.getBoolean("installing");
      download = savedInstanceState.getParcelable("download");
      properties = savedInstanceState.getParcelableArrayList("properties");
      updateDiskUsagePieChart();
      setProperties(properties);
    } else {
      paths = new ArrayList<>();
      paths.addAll(Arrays.asList(Storage.PATH));
      paths.add(getString(R.string.choose_a_directory));
      binaries = Utils.getBinariesFromAssets(ABI.getAbi());
      new BusyBoxLocater().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.busybox_installer_menu, menu);
    progressItem = menu.findItem(R.id.menu_item_progress);
    progressItem.setVisible(uninstalling || installing);
    ColorScheme.newMenuTint(menu).forceIcons().apply(getActivity());
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_settings) {
      startActivity(new Intent(getActivity(), SettingsActivity.class));
    } else {
      return super.onOptionsItemSelected(item);
    }
    return true;
  }

  @Override public void onClick(View v) {
    if (v == pathSpinner || v == versionSpinner) {
      backgroundShadow.setVisibility(View.VISIBLE);
      Technique.FADE_IN.getComposer().duration(500).playOn(backgroundShadow);
    } else if (v == uninstallButton) {
      BusyBoxUninstaller.showConfirmationDialog(getActivity(), busybox);
    } else if (v == installButton) {
      installBusyBox();
    } else if (v == infoButton) {
      Intent intent = new Intent(getActivity(), FilePropertiesActivity.class);
      intent.putExtra(FileHelper.INTENT_EXTRA_FILE, (Parcelable) busybox);
      startActivity(intent);
    }
  }

  @Override public void onDirectorySelected(File directory) {
    if (paths.contains(directory.getAbsolutePath())) {
      for (int i = 0; i < paths.size(); i++) {
        if (paths.get(i).equals(directory.getAbsolutePath())) {
          pathIndex = i;
          pathSpinner.setSelectedIndex(pathIndex);
          updateDiskUsagePieChart();
          break;
        }
      }
    } else {
      pathIndex = paths.size() - 1;
      paths.add(pathIndex, directory.getAbsolutePath());
      pathSpinner.setSelectedIndex(pathIndex);
      updateDiskUsagePieChart();
    }
  }

  @Override public void onDirectoryPickerCancelledListener() {
    pathSpinner.setSelectedIndex(pathIndex);
  }

  // --------------------------------------------------------------------------------------------

  @EventBusHook public void onEventMainThread(DownloadFinished event) {
    if (download != null && download.getId() == event.download.getId()) {
      String path = paths.get(pathSpinner.getSelectedIndex());
      Prefs prefs = Prefs.getInstance();
      BusyBoxInstaller.newBusyboxInstaller()
          .setFilename(event.download.getFilename())
          .setBinary(new AFile(event.download.getDestinationFile()))
          .setPath(path)
          .setSymlink(prefs.get("symlink_busybox_applets", true))
          .setOverwrite(prefs.get("replace_with_busybox_applets", false))
          .confirm(getActivity());
    }
  }

  @EventBusHook public void onEventMainThread(DownloadError event) {
    if (download != null && download.getId() == event.download.getId()) {
      showMessage(R.string.download_unsuccessful);
    }
  }

  @EventBusHook public void onEventMainThread(BusyBoxInstaller.StartEvent event) {
    installing = true;
    progressItem.setVisible(true);
    uninstallButton.setEnabled(false);
    installButton.setEnabled(false);
  }

  @EventBusHook public void onEventMainThread(BusyBoxInstaller.FinishedEvent event) {
    installing = false;
    progressItem.setVisible(false);
    uninstallButton.setEnabled(true);
    installButton.setEnabled(true);
    busybox = BusyBox.from(new AFile(event.installer.path, event.installer.filename).path);
    new BusyBoxMetaTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, busybox);
    showMessage(R.string.successfully_installed_s, busybox.filename);
  }

  @EventBusHook public void onEventMainThread(BusyBoxInstaller.ErrorEvent event) {
    installing = false;
    showMessage(R.string.installation_failed);
    progressItem.setVisible(false);
    uninstallButton.setEnabled(busybox != null && busybox.exists());
    installButton.setEnabled(true);
  }

  @EventBusHook public void onEventMainThread(BusyBoxUninstaller.StartEvent event) {
    if (busybox == null || !busybox.equals(event.file)) {
      return;
    }
    uninstalling = true;
    uninstallButton.setEnabled(false);
    installButton.setEnabled(false);
    progressItem.setVisible(true);
  }

  @EventBusHook public void onEventMainThread(BusyBoxUninstaller.FinishedEvent event) {
    if (busybox == null || !busybox.equals(event.file)) {
      return;
    }
    uninstalling = false;
    installButton.setEnabled(!installing);
    if (event.success) {
      new BusyBoxLocater().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      progressItem.setVisible(uninstalling || installing);
      showMessage(R.string.uninstalled_s, event.file.filename);
    } else {
      showMessage(R.string.error_uninstalling_s, busybox.filename);
    }
  }

  @EventBusHook public void onEvent(BusyBoxLocater.BusyboxLocatedEvent event) {
    busybox = event.busybox;
    String defaultInstallPath = busybox == null ? DEFAULT_INSTALL_PATH : busybox.getParent();
    for (int i = 0; i < Storage.PATH.length; i++) {
      String path = Storage.PATH[i];
      if (path.equals(defaultInstallPath)) {
        pathIndex = i;
        break;
      }
    }
    pathSpinner.setSelectedIndex(pathIndex);
    uninstallButton.setEnabled(busybox != null && busybox.exists() && !uninstalling && !installing);
    installButton.setEnabled(!uninstalling && !installing);
    new BusyBoxMetaTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, busybox);
    updateDiskUsagePieChart();
  }

  @EventBusHook public void onEvent(BusyBoxMetaTask.BusyBoxPropertiesEvent event) {
    setProperties(event.properties);
  }

  @EventBusHook public void onEvent(BusyBoxDiskUsageTask.BusyBoxDiskUsageEvent event) {
    long totalSize = event.total;
    long freeSize = event.free;
    long usedSize = totalSize - freeSize;

    int color1 = ColorScheme.getAccent();
    int color2 = ColorScheme.getAccentDark();
    int color3 = ColorScheme.getPrimary();
    if (color1 == color3) color3 = Color.invert(color1);

    if (itemSlice == null || freeSlice == null || usedSlice == null) {
      usedSlice = new PieModel(usedSize - event.binaryInfo.size, color1);
      freeSlice = new PieModel(freeSize, color2);
      itemSlice = new PieModel(event.binaryInfo.size, color3);
      pieChart.addPieSlice(usedSlice);
      pieChart.addPieSlice(freeSlice);
      pieChart.addPieSlice(itemSlice);
    } else {
      usedSlice.setValue(usedSize - event.binaryInfo.size);
      freeSlice.setValue(freeSize);
      itemSlice.setValue(event.binaryInfo.size);
      pieChart.update();
    }

    String item = event.binaryInfo.filename.length() >= 8 ? "binary" : event.binaryInfo.filename;

    setLegendText(R.id.text_used, getString(R.string.used).toUpperCase(), usedSize, totalSize, color1);
    setLegendText(R.id.text_free, getString(R.string.free).toUpperCase(), freeSize, totalSize, color2);
    setLegendText(R.id.text_item, item.toUpperCase(), event.binaryInfo.size, totalSize, color3);

    findById(R.id.text_used).setVisibility(View.VISIBLE);
    findById(R.id.text_free).setVisibility(View.VISIBLE);
    findById(R.id.text_item).setVisibility(View.VISIBLE);
    findById(R.id.progress).setVisibility(View.GONE);
  }

  // --------------------------------------------------------------------------------------------

  private void installBusyBox() {
    BinaryInfo binary = binaries.get(versionSpinner.getSelectedIndex());
    String path = paths.get(pathSpinner.getSelectedIndex());
    if (binary.path.startsWith("http")) {
      File destination = new File(getActivity().getCacheDir(), binary.name + "/" + binary.filename);
      if (destination.exists() && destination.length() == binary.size) {
        Prefs prefs = Prefs.getInstance();
        BusyBoxInstaller.newBusyboxInstaller()
            .setFilename(binary.filename)
            .setBinary(new AFile(destination))
            .setPath(path)
            .setSymlink(prefs.get("symlink_busybox_applets", true))
            .setOverwrite(prefs.get("replace_with_busybox_applets", false))
            .confirm(getActivity());
      } else {
        download = new Download.Builder(binary.path)
            .setDestination(destination)
            .setFilename(binary.filename)
            .setShouldRedownload(true)
            .setMd5sum(binary.md5sum)
            .build();
        DownloadRequest request = download.request()
            .setNotificationVisibility(DownloadRequest.VISIBILITY_HIDDEN)
            .build();
        DownloadProgressDialog.show(getActivity(), download);
        request.start(getActivity());
      }
    } else {
      Prefs prefs = Prefs.getInstance();
      BusyBoxInstaller.newBusyboxInstaller()
          .setAsset(binary.path)
          .setFilename(binary.filename)
          .setPath(path)
          .setSymlink(prefs.get("symlink_busybox_applets", true))
          .setOverwrite(prefs.get("replace_with_busybox_applets", false))
          .confirm(getActivity());
    }
  }

  private void updateDiskUsagePieChart() {
    BinaryInfo binaryInfo = binaries.get(versionSpinner.getSelectedIndex());
    String path = paths.get(pathSpinner.getSelectedIndex());
    new BusyBoxDiskUsageTask(binaryInfo, path) {

      @Override protected void onPreExecute() {
        findById(R.id.progress).setVisibility(View.VISIBLE);
        findById(R.id.text_used).setVisibility(View.GONE);
        findById(R.id.text_free).setVisibility(View.GONE);
        findById(R.id.text_item).setVisibility(View.GONE);
      }

    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private void setProperties(ArrayList<FileMeta> properties) {
    this.properties = properties;

    if (properties == null) {
      propertiesCard.setVisibility(View.GONE);
      return;
    }
    if (propertiesCard.getVisibility() != View.VISIBLE) {
      propertiesCard.setVisibility(View.VISIBLE);
    }

    TableLayout tableLayout = findById(R.id.table_properties);

    if (tableLayout.getChildCount() > 0) {
      tableLayout.removeAllViews();
    }

    int width = ResUtils.dpToPx(128);
    int left = ResUtils.dpToPx(16);
    int top = ResUtils.dpToPx(6);
    int bottom = ResUtils.dpToPx(6);

    int i = 0;
    for (FileMeta meta : properties) {
      TableRow tableRow = new TableRow(getActivity());
      TextView nameText = new TextView(getActivity());
      TextView valueText = new TextView(getActivity());

      if (i % 2 == 0) {
        tableRow.setBackgroundColor(0x0D000000);
      } else {
        tableRow.setBackgroundColor(Color.TRANSPARENT);
      }

      nameText.setLayoutParams(new TableRow.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
      nameText.setPadding(left, top, 0, bottom);
      nameText.setAllCaps(true);
      nameText.setTypeface(Typeface.DEFAULT_BOLD);
      nameText.setText(meta.name);

      valueText.setPadding(left, top, 0, bottom);
      valueText.setText(meta.value);

      tableRow.addView(nameText);
      tableRow.addView(valueText);
      tableLayout.addView(tableRow);

      i++;
    }
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

  private void showMessage(@StringRes int resid, Object... args) {
    showMessage(getString(resid, args));
  }

  private void showMessage(String message) {
    Snackbar snackbar = Snackbar.make(findById(R.id.main), message, Snackbar.LENGTH_LONG);
    View view = snackbar.getView();
    TextView messageText = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
    if (Themes.isDark()) {
      messageText.setTextColor(ColorScheme.getPrimaryText(getActivity()));
      view.setBackgroundColor(ColorScheme.getBackgroundDark(getActivity()));
    } else {
      messageText.setTextColor(Color.WHITE);
    }
    snackbar.show();
  }

}
