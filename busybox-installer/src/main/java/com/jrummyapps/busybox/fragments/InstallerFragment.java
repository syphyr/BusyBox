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

package com.jrummyapps.busybox.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
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
import com.jrummyapps.android.analytics.Analytics;
import com.jrummyapps.android.animations.Technique;
import com.jrummyapps.android.base.BaseSupportFragment;
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
import com.jrummyapps.android.os.Os;
import com.jrummyapps.android.prefs.Prefs;
import com.jrummyapps.android.roottools.box.BusyBox;
import com.jrummyapps.android.roottools.files.AFile;
import com.jrummyapps.android.roottools.shell.stericson.Shell;
import com.jrummyapps.android.theme.ColorScheme;
import com.jrummyapps.android.theme.Themes;
import com.jrummyapps.android.util.IntentUtils;
import com.jrummyapps.android.util.OrientationUtils;
import com.jrummyapps.android.util.ResUtils;
import com.jrummyapps.busybox.R;
import com.jrummyapps.busybox.activities.AboutActivity;
import com.jrummyapps.busybox.activities.SettingsActivity;
import com.jrummyapps.busybox.dialogs.BusyboxSuccessDialog;
import com.jrummyapps.busybox.dialogs.CreateZipDialog;
import com.jrummyapps.busybox.models.BinaryInfo;
import com.jrummyapps.busybox.monetize.Monetize;
import com.jrummyapps.busybox.tasks.BusyBoxFinder;
import com.jrummyapps.busybox.tasks.BusyBoxMetaTask;
import com.jrummyapps.busybox.tasks.DiskUsageTask;
import com.jrummyapps.busybox.tasks.Installer;
import com.jrummyapps.busybox.tasks.Uninstaller;
import com.jrummyapps.busybox.utils.BusyBoxZipHelper;
import com.jrummyapps.busybox.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import static com.jrummyapps.android.util.IntentUtils.isIntentAvailable;

public class InstallerFragment extends BaseSupportFragment implements
    DirectoryPickerDialog.OnDirectorySelectedListener,
    DirectoryPickerDialog.OnDirectoryPickerCancelledListener,
    View.OnClickListener {

  private static final String DEFAULT_INSTALL_PATH = "/system/xbin";

  private static final int REQUEST_TERM = 56;

  private static final int CMD_INSTALL = 0;
  private static final int CMD_TERMINAL = 1;
  private static final int CMD_CREATE_ZIP = 2;

  private final Object termLock = new Object();

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
  private int downloadCompleteCommand;
  private boolean uninstalling;
  private boolean installing;
  private boolean createArchive;
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
    return inflater.inflate(R.layout.fragment_busybox_installer, container, false);
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
    outState.putInt("download_complete_command", downloadCompleteCommand);
    outState.putBoolean("create_archive", createArchive);
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
      downloadCompleteCommand = savedInstanceState.getInt("download_complete_command", downloadCompleteCommand);
      createArchive = savedInstanceState.getBoolean("create_archive");
      updateDiskUsagePieChart();
      setProperties(properties);
    } else {
      paths = new ArrayList<>();
      paths.addAll(Arrays.asList(Storage.PATH));
      paths.add(getString(R.string.choose_a_directory));
      binaries = Utils.getBinariesFromAssets(ABI.getAbi());
      new BusyBoxFinder().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.busybox_installer_menu, menu);
    progressItem = menu.findItem(R.id.menu_item_progress);
    progressItem.setVisible(uninstalling || installing);
    menu.findItem(R.id.action_terminal).setVisible(isTerminalSupported());
    if (getActivity() != null) {
      ColorScheme.newMenuTint(menu).forceIcons().apply(getActivity());
    }
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.action_settings) {
      startActivity(new Intent(getActivity(), SettingsActivity.class));
      return true;
    } else if (itemId == R.id.action_terminal) {
      openTerminal();
      return true;
    } else if (itemId == R.id.action_zip_archive) {
      if (getSelectedBinary(CMD_CREATE_ZIP) != null) {
        createArchive = true;
        DirectoryPickerDialog.show(getActivity());
      }
      return true;
    } else if (itemId == R.id.action_info) {
      startActivity(new Intent(getActivity(), AboutActivity.class));
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == REQUEST_TERM) {
      OrientationUtils.unlockOrientation(getActivity());
      synchronized (termLock) {
        termLock.notify();
      }
      return;
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override public void onClick(View v) {
    if (v == pathSpinner || v == versionSpinner) {
      backgroundShadow.setVisibility(View.VISIBLE);
      Technique.FADE_IN.getComposer().duration(500).playOn(backgroundShadow);
    } else if (v == uninstallButton) {
      Uninstaller.showConfirmationDialog(getActivity(), busybox);
    } else if (v == installButton) {
      installBusyBox();
    } else if (v == infoButton) {
      Intent intent = new Intent(getActivity(), FilePropertiesActivity.class);
      intent.putExtra(FileHelper.INTENT_EXTRA_FILE, (Parcelable) busybox);
      startActivity(intent);
    }
  }

  @SuppressWarnings("ConstantConditions")
  @EventBusHook public void onEvent(CreateZipDialog.CreateZipEvent event) {
    new AsyncTask<String, Void, Integer>() {

      @Override protected void onPreExecute() {
        progressItem.setVisible(true);
      }

      @Override protected Integer doInBackground(String... params) {
        try {
          BusyBox busybox = BusyBox.from(params[0]);
          String installPath = params[1];
          Analytics.newEvent("Create ZIP")
              .put("busybox", busybox)
              .put("path", installPath)
              .log();
          BusyBoxZipHelper.createBusyboxRecoveryZip(busybox, installPath, new File(params[2]));
          return R.string.created_installable_zip;
        } catch (Exception e) {
          return R.string.could_not_create_the_zip_archive;
        }
      }

      @Override protected void onPostExecute(Integer resid) {
        progressItem.setVisible(false);
        showMessage(resid);
      }

    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
        getSelectedBinary(CMD_CREATE_ZIP).getAbsolutePath(),
        paths.get(pathSpinner.getSelectedIndex()) + "/busybox",
        event.file.getAbsolutePath()
    );
  }

  @Override public void onDirectorySelected(File directory) {
    if (createArchive) {
      BinaryInfo binaryInfo = binaries.get(versionSpinner.getSelectedIndex());
      String filename = binaryInfo.filename + "-" + binaryInfo.abi + ".zip";
      CreateZipDialog.show(getActivity(), directory, filename);
      createArchive = false;
    } else {
      Analytics.newEvent("Selected path").put("path", directory.getPath()).log();
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
  }

  @Override public void onDirectoryPickerCancelledListener() {
    pathSpinner.setSelectedIndex(pathIndex);
  }

  // --------------------------------------------------------------------------------------------

  @EventBusHook public void onEventMainThread(DownloadFinished event) {
    if (download != null && download.getId() == event.download.getId()) {

      Analytics.newEvent("Downloaded binary")
          .put("command", downloadCompleteCommand)
          .put("filename", event.download.getFilename())
          .put("url", event.download.getUrl().toString())
          .log();

      if (downloadCompleteCommand == CMD_TERMINAL) {
        openTerminal();
      } else if (downloadCompleteCommand == CMD_INSTALL) {
        installBusyBox();
      } else if (downloadCompleteCommand == CMD_CREATE_ZIP) {
        createArchive = true;
        DirectoryPickerDialog.show(getActivity());
      }
    }
  }

  @EventBusHook public void onEventMainThread(DownloadError event) {
    if (download != null && download.getId() == event.download.getId()) {
      Analytics.newEvent("Download error")
          .put("error_code", event.download.getError())
          .log();
      showMessage(R.string.download_unsuccessful);
    }
  }

  @EventBusHook public void onEventMainThread(Installer.StartEvent event) {
    installing = true;
    progressItem.setVisible(true);
    uninstallButton.setEnabled(false);
    installButton.setEnabled(false);
  }

  @EventBusHook public void onEventMainThread(Installer.FinishedEvent event) {
    installing = false;
    progressItem.setVisible(false);
    uninstallButton.setEnabled(true);
    installButton.setEnabled(true);
    busybox = BusyBox.from(new AFile(event.installer.path, event.installer.filename).path);

    Analytics.newEvent("Installed BusyBox")
        .put("is_ads_removed", Monetize.isAdsRemoved())
        .put("path", busybox.path)
        .log();

    new BusyBoxMetaTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, busybox);
    if (Monetize.isAdsRemoved()) {
      showMessage(R.string.successfully_installed_s, busybox.filename);
    } else {
      BusyboxSuccessDialog dialog = new BusyboxSuccessDialog();
      dialog.show(getActivity().getFragmentManager(), "BusyboxSuccessDialog");
    }
  }

  @EventBusHook public void onEventMainThread(Installer.ErrorEvent event) {
    installing = false;
    showMessage(R.string.installation_failed);
    progressItem.setVisible(false);
    uninstallButton.setEnabled(busybox != null && busybox.exists());
    installButton.setEnabled(true);

    Analytics.newEvent("Error installing BusyBox")
        .put("error", event.error)
        .log();

    if (TextUtils.equals(event.error, Installer.ERROR_NOT_ROOTED)) {
      RootRequiredDialog dialog = new RootRequiredDialog();
      Bundle args = new Bundle();
      args.putString("filename", event.installer.filename);
      dialog.setArguments(args);
      dialog.show(getActivity().getFragmentManager(), "RootRequiredDialog");
    }
  }

  @EventBusHook public void onEventMainThread(Uninstaller.StartEvent event) {
    if (busybox == null || !busybox.equals(event.file)) {
      return;
    }
    uninstalling = true;
    uninstallButton.setEnabled(false);
    installButton.setEnabled(false);
    progressItem.setVisible(true);
  }

  @EventBusHook public void onEventMainThread(Uninstaller.FinishedEvent event) {
    if (busybox == null || !busybox.equals(event.file)) {
      return;
    }
    uninstalling = false;
    installButton.setEnabled(!installing);
    if (event.success) {
      new BusyBoxFinder().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      progressItem.setVisible(uninstalling || installing);
      showMessage(R.string.uninstalled_s, event.file.filename);
    } else {
      showMessage(R.string.error_uninstalling_s, busybox.filename);
    }
  }

  @EventBusHook public void onEvent(BusyBoxFinder.BusyboxFoundEvent event) {
    busybox = event.busybox;
    String defaultInstallPath = busybox == null ? DEFAULT_INSTALL_PATH : busybox.getParent();
    for (int i = 0; i < Storage.PATH.length; i++) {
      String path = Storage.PATH[i];
      if (path.equals(defaultInstallPath)) {
        pathIndex = i;
        break;
      }
    }

    Analytics.newEvent("BusyBox Found Event")
        .put("default_install_path", defaultInstallPath)
        .put("busybox", busybox == null ? "null" : busybox.path)
        .log();

    pathSpinner.setSelectedIndex(pathIndex);
    uninstallButton.setEnabled(busybox != null && busybox.exists() && !uninstalling && !installing);
    installButton.setEnabled(!uninstalling && !installing);
    new BusyBoxMetaTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, busybox);
    updateDiskUsagePieChart();
  }

  @EventBusHook public void onEvent(BusyBoxMetaTask.BusyBoxPropertiesEvent event) {
    setProperties(event.properties);
  }

  @EventBusHook public void onEvent(DiskUsageTask.BusyBoxDiskUsageEvent event) {
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

  @EventBusHook public void onEvent(OpenTerminalEvent event) {
    openTerminal();
  }

  // --------------------------------------------------------------------------------------------

  private File getSelectedBinary(int command) {
    BinaryInfo binary = binaries.get(versionSpinner.getSelectedIndex());

    File file;
    if (binary.path.startsWith("http")) {
      file = binary.getDownloadDestination();
      if (!file.exists() || file.length() != binary.size) {
        // We need to download busybox before opening terminal
        NetworkInfo networkInfo =
            ((ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
          showMessage(R.string.please_connect_to_a_network_and_try_again);
          return null;
        }
        downloadCompleteCommand = command;
        download = new Download.Builder(binary.path)
            .setDestination(file)
            .setFilename(binary.filename)
            .setShouldRedownload(true)
            .setMd5sum(binary.md5sum)
            .build();
        DownloadRequest request = download.request()
            .setNotificationVisibility(DownloadRequest.VISIBILITY_HIDDEN)
            .build();
        DownloadProgressDialog.show(getActivity(), download);
        request.start(getActivity());
        return null;
      }
    } else {
      file = new File(getActivity().getFilesDir(), binary.filename);
    }

    return file;
  }

  private boolean isTerminalSupported() {
    Context context = getActivity();
    return context != null &&
        isIntentAvailable(context, new Intent("jackpal.androidterm.RUN_SCRIPT")) ||
        isIntentAvailable(context, new Intent("jrummy.androidterm.RUN_SCRIPT")) ||
        isIntentAvailable(context, new Intent(Intent.ACTION_VIEW,
            Uri.parse("market://details?id=jackpal.androidterm")));
  }

  private void openTerminal() {
    File file = getSelectedBinary(CMD_INSTALL);
    if (file == null) {
      return; // downloading or no network connection
    }

    new AsyncTask<File, String, Intent>() {

      private boolean openGooglePlay = true;

      @Override protected Intent doInBackground(File... params) {
        Intent intent;
        String permission;
        if (isIntentAvailable(getActivity(), new Intent("jackpal.androidterm.RUN_SCRIPT"))) {
          intent = new Intent("jackpal.androidterm.RUN_SCRIPT");
          permission = "jackpal.androidterm.permission.RUN_SCRIPT";
        } else if (isIntentAvailable(getActivity(), new Intent("jrummy.androidterm.RUN_SCRIPT"))) {
          intent = new Intent("jrummy.androidterm.RUN_SCRIPT");
          permission = "jrummy.androidterm.permission.RUN_SCRIPT";
        } else {
          return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          if (getActivity().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            OrientationUtils.lockOrientation(getActivity());
            requestPermissions(new String[]{permission}, REQUEST_TERM);
            synchronized (termLock) {
              try {
                termLock.wait();
              } catch (InterruptedException e) {
                openGooglePlay = false;
                return null;
              }
            }
          }
        }

        File bin = new File(getActivity().getFilesDir(), "bin");
        //noinspection ResultOfMethodCallIgnored
        bin.mkdirs();

        File file = params[0];
        //noinspection OctalInteger
        Os.chmod(file.getAbsolutePath(), 0755);
        //noinspection OctalInteger
        Os.chmod(bin.getAbsolutePath(), 0777);
        //noinspection OctalInteger
        Os.chmod(file.getParent(), 0777);
        Shell.SH.run(file + " --install -s " + bin);
        Shell.SH.run(file + " ln -s " + file + " " + bin + "/busybox");

        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra("jackpal.androidterm.iInitialCommand",
            String.format("export PATH=%s:$PATH; busybox", bin.getPath()));
        return intent;
      }

      @Override protected void onPostExecute(Intent intent) {
        if (intent == null && openGooglePlay) {
          intent = IntentUtils.newMarketForAppIntent(getActivity(), "jackpal.androidterm");
        }
        try {
          startActivity(intent);
        } catch (Exception ignored) {
        }
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, file);
  }

  private void installBusyBox() {
    BinaryInfo binary = binaries.get(versionSpinner.getSelectedIndex());
    String path = paths.get(pathSpinner.getSelectedIndex());
    if (binary.path.startsWith("http")) {
      File destination = binary.getDownloadDestination();
      if (destination.exists() && destination.length() == binary.size) {
        Prefs prefs = Prefs.getInstance();
        Installer.newBusyboxInstaller()
            .setFilename(binary.filename)
            .setBinary(new AFile(destination))
            .setPath(path)
            .setSymlink(prefs.get("symlink_busybox_applets", true))
            .setOverwrite(prefs.get("replace_with_busybox_applets", false))
            .confirm(getActivity());
      } else {
        NetworkInfo networkInfo =
            ((ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
          showMessage(R.string.please_connect_to_a_network_and_try_again);
          return;
        }
        downloadCompleteCommand = CMD_INSTALL;
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
      Installer.newBusyboxInstaller()
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
    new DiskUsageTask(binaryInfo, path) {

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

    Analytics analytics = Analytics.newEvent("BusyBox Properties");
    for (FileMeta property : properties) {
      analytics.put(property.name, property.value);
    }
    analytics.log();

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

    final int WORD_LENGTH = getResources().getBoolean(R.bool.isTablet) ? 12 : 8;

    String text = "";
    for (int i = title.length(); i <= WORD_LENGTH; i++) text += 'A';

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
    TextView messageText = (TextView) view.findViewById(R.id.snackbar_text);
    if (Themes.isDark()) {
      messageText.setTextColor(ColorScheme.getPrimaryText(getActivity()));
      view.setBackgroundColor(ColorScheme.getBackgroundDark(getActivity()));
    } else {
      messageText.setTextColor(Color.WHITE);
    }
    snackbar.show();
  }

  public static final class OpenTerminalEvent {

  }

  public static class RootRequiredDialog extends DialogFragment {

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
      return new AlertDialog.Builder(getActivity())
          .setTitle(R.string.root_required)
          .setMessage(getString(R.string.root_access_is_required_to_install_s, getArguments().getString("filename")))
          .setNegativeButton(R.string.close, null)
          .setNeutralButton(R.string.terminal, new DialogInterface.OnClickListener() {

            @Override public void onClick(DialogInterface dialog, int which) {
              Events.post(new OpenTerminalEvent());
            }
          })
          .setPositiveButton(R.string.root_info, new DialogInterface.OnClickListener() {

            @Override public void onClick(DialogInterface dialog, int which) {
              try {
                Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(
                    "com.jrummyapps.rootchecker");
                if (intent == null) {
                  intent = IntentUtils.newGooglePlayIntent(getActivity(), "com.jrummyapps.rootchecker");
                }
                startActivity(intent);
              } catch (ActivityNotFoundException ignored) {
              }
            }
          })
          .create();
    }

  }

}
