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

package com.jrummyapps.busybox.activities;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.jrummyapps.android.base.BaseDrawerActivity;
import com.jrummyapps.android.directorypicker.DirectoryPickerDialog;
import com.jrummyapps.android.io.WriteExternalStoragePermissions;
import com.jrummyapps.android.theme.ColorScheme;
import com.jrummyapps.android.util.ReflectUtils;
import com.jrummyapps.busybox.R;
import com.jrummyapps.busybox.fragments.InstallerFragment;
import com.jrummyapps.busybox.fragments.NavigationFragment;
import com.jrummyapps.busybox.fragments.ScriptsFragment;

import java.io.File;

@Deprecated
public class DrawerActivity extends BaseDrawerActivity implements
    DirectoryPickerDialog.OnDirectorySelectedListener,
    DirectoryPickerDialog.OnDirectoryPickerCancelledListener {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null) {
      getSupportFragmentManager()
          .beginTransaction()
          .add(R.id.content_frame, new InstallerFragment())
          .commit();
      getFragmentManager()
          .beginTransaction().
          add(R.id.navigation_drawer, new NavigationFragment())
          .commit();
    }
  }

  @TargetApi(Build.VERSION_CODES.M)
  @Override public View onViewCreated(@NonNull View view, @Nullable AttributeSet attrs) {
    if (view instanceof Button) {
      Button button = (Button) view;
      ColorStateList textColors = button.getTextColors();
      ColorScheme.applyColorScheme(textColors);
      button.setTextColor(textColors);
      ColorStateList csl = button.getBackgroundTintList();
      if (csl != null) {
        button.setBackgroundTintList(ColorScheme.applyColorScheme(csl));
      }
    } else if (view instanceof TextView) {
      TextView textView = (TextView) view;
      ColorStateList textColors = textView.getTextColors();
      ColorScheme.applyColorScheme(textColors);
      textView.setTextColor(textColors);
      ColorScheme.applyColorScheme(textView.getBackgroundTintList());
      ColorScheme.applyColorScheme(textView.getBackground());
    }
    if (view instanceof MaterialSpinner) {
      MaterialSpinner spinner = (MaterialSpinner) view;
      spinner.setBackgroundColor(ColorScheme.getBackgroundLight(this));
    } else if (view instanceof CardView) {
      CardView cardView = (CardView) view;
      cardView.setCardBackgroundColor(ColorScheme.getBackgroundLight(this));
    } else if (view instanceof Toolbar) {
      Toolbar toolbar = (Toolbar) view;
      toolbar.setBackgroundColor(ColorScheme.getPrimary());
    }
    if (view instanceof TabLayout) {
      TabLayout tabLayout = (TabLayout) view;
      tabLayout.setSelectedTabIndicatorColor(ColorScheme.getAccent());
    }
    if (view instanceof ViewGroup) {
      if (view.getClass().getName().startsWith("android.widget")) {
        ColorScheme.applyColorScheme(view.getBackground());
      } else {
        ColorScheme.applyColorScheme((Drawable) ReflectUtils.getFieldValue(view, "mBackground"));
      }
    }
    return super.onViewCreated(view, attrs);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == ScriptsFragment.REQUEST_CREATE_SCRIPT) {
      Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
      if (fragment instanceof ScriptsFragment) {
        // android.app.support.v4.Fragment doesn't have
        // startActivityForResult(Intent intent, int requestCode, Bundle options)
        // so wee need to pass the result on
        fragment.onActivityResult(requestCode, resultCode, data);
        return;
      }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (WriteExternalStoragePermissions.INSTANCE.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
      return;
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override public int getLayoutResId() {
    return R.layout.fullscreen_drawerlayout;
  }

  @Override public void onDirectorySelected(File directory) {
    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
    if (fragment instanceof DirectoryPickerDialog.OnDirectorySelectedListener) {
      ((DirectoryPickerDialog.OnDirectorySelectedListener) fragment).onDirectorySelected(directory);
    }
  }

  @Override public void onDirectoryPickerCancelledListener() {
    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
    if (fragment instanceof DirectoryPickerDialog.OnDirectoryPickerCancelledListener) {
      ((DirectoryPickerDialog.OnDirectoryPickerCancelledListener) fragment).onDirectoryPickerCancelledListener();
    }
  }

}
