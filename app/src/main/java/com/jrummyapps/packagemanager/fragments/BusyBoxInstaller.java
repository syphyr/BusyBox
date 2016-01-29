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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.jaredrummler.materialspinner.MaterialSpinner.OnItemSelectedListener;
import com.jaredrummler.materialspinner.MaterialSpinner.OnNothingSelectedListener;
import com.jrummyapps.android.animations.Technique;
import com.jrummyapps.android.base.BaseFragment;
import com.jrummyapps.android.directorypicker.DirectoryPickerDialog;
import com.jrummyapps.android.io.Storage;
import com.jrummyapps.android.os.ABI;
import com.jrummyapps.packagemanager.R;
import com.jrummyapps.packagemanager.models.AssetBinary;
import com.jrummyapps.packagemanager.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class BusyBoxInstaller extends BaseFragment implements
    DirectoryPickerDialog.OnDirectorySelectedListener,
    DirectoryPickerDialog.OnDirectoryPickerCancelledListener {

  private static final String DEFAULT_INSTALL_PATH = "/system/xbin";

  private ArrayList<AssetBinary> binaries;
  private ArrayList<String> paths;

  private MaterialSpinner binarySpinner;
  private MaterialSpinner directorySpinner;

  private int selectedDirectoryPosition;

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.busybox_installer, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {

    onRestoreInstanceState(savedInstanceState);

    final View backgroundShadow = findById(R.id.background_shadow);
    binarySpinner = findById(R.id.binary_spinner);
    directorySpinner = findById(R.id.directory_spinner);

    final View.OnClickListener onClickListener = new View.OnClickListener() {

      @Override public void onClick(View v) {
        backgroundShadow.setVisibility(View.VISIBLE);
        Technique.FADE_IN.getComposer().duration(500).playOn(backgroundShadow);
      }
    };

    final OnNothingSelectedListener onNothingSelectedListener = new OnNothingSelectedListener() {

      @Override public void onNothingSelected(MaterialSpinner spinner) {
        Technique.FADE_OUT.getComposer().duration(500).hideOnFinished().playOn(backgroundShadow);
      }
    };

    binarySpinner.setItems(binaries);
    binarySpinner.setOnClickListener(onClickListener);
    binarySpinner.setOnNothingSelectedListener(onNothingSelectedListener);

    directorySpinner.setItems(paths);
    directorySpinner.setOnClickListener(onClickListener);
    directorySpinner.setOnNothingSelectedListener(onNothingSelectedListener);
    directorySpinner.setSelectedIndex(selectedDirectoryPosition);

    directorySpinner.setOnItemSelectedListener(new OnItemSelectedListener<String>() {

      @Override public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
        Technique.FADE_OUT.getComposer().duration(500).hideOnFinished().playOn(backgroundShadow);
        if (item.equals(getString(R.string.choose_a_directory))) {
          DirectoryPickerDialog.show(getActivity(), new File("/"));
        } else {
          selectedDirectoryPosition = view.getSelectedIndex();
        }
      }
    });

    binarySpinner.setOnItemSelectedListener(new OnItemSelectedListener<AssetBinary>() {

      @Override public void onItemSelected(MaterialSpinner view, int position, long id, AssetBinary item) {
        Technique.FADE_OUT.getComposer().duration(500).hideOnFinished().playOn(backgroundShadow);
      }
    });
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt("selected_directory_position", selectedDirectoryPosition);
    outState.putStringArrayList("paths", paths);
    outState.putParcelableArrayList("binaries", binaries);
  }

  @Override public void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    if (savedInstanceState != null) {
      selectedDirectoryPosition = savedInstanceState.getInt("selected_directory_position", -1);
      paths = savedInstanceState.getStringArrayList("paths");
      binaries = savedInstanceState.getParcelableArrayList("binaries");
    } else {
      paths = new ArrayList<>();
      paths.addAll(Arrays.asList(Storage.PATH));
      paths.add(getString(R.string.choose_a_directory));

      binaries = Utils.getBinariesFromAssets(ABI.getAbi());
      binaries.add(new AssetBinary("Download...", null, ABI.getAbi().name, "http://jrummyapps.com"));

      selectedDirectoryPosition = 0;
      for (int i = 0; i < paths.size(); i++) {
        if (paths.get(i).equals(DEFAULT_INSTALL_PATH)) {
          selectedDirectoryPosition = i;
          break;
        }
      }
    }
  }

  @Override public void onDirectorySelected(File directory) {
    if (paths.contains(directory.getAbsolutePath())) {
      for (int i = 0; i < paths.size(); i++) {
        if (paths.get(i).equals(directory.getAbsolutePath())) {
          selectedDirectoryPosition = i;
          directorySpinner.setSelectedIndex(selectedDirectoryPosition);
          break;
        }
      }
    } else {
      selectedDirectoryPosition = paths.size() - 1;
      paths.add(selectedDirectoryPosition, directory.getAbsolutePath());
      directorySpinner.setSelectedIndex(selectedDirectoryPosition);
    }
  }

  @Override public void onDirectoryPickerCancelledListener() {
    directorySpinner.setSelectedIndex(selectedDirectoryPosition);
  }

}
