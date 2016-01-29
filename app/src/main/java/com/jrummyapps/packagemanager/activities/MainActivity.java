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

package com.jrummyapps.packagemanager.activities;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.jrummyapps.android.base.BaseDrawerActivity;
import com.jrummyapps.android.directorypicker.DirectoryPickerDialog;
import com.jrummyapps.android.preferences.activities.MainPreferenceActivity;
import com.jrummyapps.android.theme.ColorScheme;
import com.jrummyapps.android.util.ReflectUtils;
import com.jrummyapps.packagemanager.R;
import com.jrummyapps.packagemanager.fragments.BusyBoxInstaller;

import java.io.File;

public class MainActivity extends BaseDrawerActivity implements
    DirectoryPickerDialog.OnDirectorySelectedListener,
    DirectoryPickerDialog.OnDirectoryPickerCancelledListener {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null) {
      getFragmentManager()
          .beginTransaction()
          .add(R.id.content_frame, new BusyBoxInstaller())
          .commit();
    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(0, Menu.FIRST, 0, R.string.settings)
        .setIcon(R.drawable.ic_settings_white_24dp)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    ColorScheme.newMenuTint(menu).forceIcons().apply(this);
    return super.onCreateOptionsMenu(menu);
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

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == Menu.FIRST) {
      startActivity(new Intent(this, MainPreferenceActivity.class));
    } else {
      return super.onOptionsItemSelected(item);
    }
    return true;
  }

  @Override public int getLayoutResId() {
    return R.layout.fullscreen_drawerlayout;
  }

  @Override public void onDirectorySelected(File directory) {
    Fragment fragment = getFragmentManager().findFragmentById(R.id.content_frame);
    if (fragment instanceof DirectoryPickerDialog.OnDirectorySelectedListener) {
      ((DirectoryPickerDialog.OnDirectorySelectedListener) fragment).onDirectorySelected(directory);
    }
  }

  @Override public void onDirectoryPickerCancelledListener() {
    Fragment fragment = getFragmentManager().findFragmentById(R.id.content_frame);
    if (fragment instanceof DirectoryPickerDialog.OnDirectoryPickerCancelledListener) {
      ((DirectoryPickerDialog.OnDirectoryPickerCancelledListener) fragment).onDirectoryPickerCancelledListener();
    }
  }

}
