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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

import com.crashlytics.android.Crashlytics;
import com.jrummyapps.android.base.BaseCompatActivity;
import com.jrummyapps.android.directorypicker.DirectoryPickerDialog;
import com.jrummyapps.android.exceptions.NotImplementedException;
import com.jrummyapps.android.io.WriteExternalStoragePermissions;
import com.jrummyapps.android.roottools.files.AFile;
import com.jrummyapps.android.theme.ColorScheme;
import com.jrummyapps.android.theme.Themes;
import com.jrummyapps.android.tinting.TintHelper;
import com.jrummyapps.busybox.R;
import com.jrummyapps.busybox.fragments.AppletsFragment;
import com.jrummyapps.busybox.fragments.InstallerFragment;
import com.jrummyapps.busybox.fragments.ScriptsFragment;

import static com.jrummyapps.android.app.App.getContext;
import static com.jrummyapps.android.util.FragmentUtils.getCurrentFragment;

public class MainActivity extends BaseCompatActivity implements
    DirectoryPickerDialog.OnDirectorySelectedListener,
    DirectoryPickerDialog.OnDirectoryPickerCancelledListener {

  private ViewPager viewPager;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    TabLayout tabLayout = findById(R.id.tabs);
    viewPager = findById(R.id.container);
    Toolbar toolbar = findById(R.id.toolbar);
    String[] titles = {getString(R.string.applets), getString(R.string.installer), getString(R.string.scripts)};
    SectionsAdapter pagerAdapter = new SectionsAdapter(getSupportFragmentManager(), titles);
    setSupportActionBar(toolbar);
    viewPager.setOffscreenPageLimit(2);
    viewPager.setAdapter(pagerAdapter);
    tabLayout.setupWithViewPager(viewPager);
    viewPager.setCurrentItem(1);
  }

  @TargetApi(Build.VERSION_CODES.M)
  @Override public View onViewCreated(@NonNull View view, @Nullable AttributeSet attrs) {
    // We need to manually set the color scheme on Android 6.0+
    try {
      if (view instanceof CardView) {
        ((CardView) view).setCardBackgroundColor(ColorScheme.getBackgroundLight(getActivity()));
      }
      if (view instanceof FloatingActionButton) {
        FloatingActionButton fab = (FloatingActionButton) view;
        fab.setBackgroundTintList(ColorStateList.valueOf(ColorScheme.getAccent()));
      }
      if (view instanceof TabLayout) {
        TabLayout tabLayout = (TabLayout) view;
        tabLayout.setSelectedTabIndicatorColor(ColorScheme.getAccent());
      }
    } catch (Exception e) {
      Crashlytics.logException(e);
    }
    return TintHelper.applyColorScheme(view, attrs);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == ScriptsFragment.REQUEST_CREATE_SCRIPT) {
      Fragment fragment = getCurrentFragment(getSupportFragmentManager(), viewPager);
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

  @Override public void onDirectorySelected(AFile directory) {
    Fragment fragment = getCurrentFragment(getSupportFragmentManager(), viewPager);
    if (fragment instanceof DirectoryPickerDialog.OnDirectorySelectedListener) {
      ((DirectoryPickerDialog.OnDirectorySelectedListener) fragment).onDirectorySelected(directory);
    }
  }

  @Override public void onDirectoryPickerCancelledListener() {
    Fragment fragment = getCurrentFragment(getSupportFragmentManager(), viewPager);
    if (fragment instanceof DirectoryPickerDialog.OnDirectoryPickerCancelledListener) {
      ((DirectoryPickerDialog.OnDirectoryPickerCancelledListener) fragment).onDirectoryPickerCancelledListener();
    }
  }

  @Override public int getActivityTheme() {
    return Themes.getNoActionBarTheme();
  }

  public static class SectionsAdapter extends FragmentPagerAdapter {

    private final String[] titles;

    public SectionsAdapter(FragmentManager fm, String[] titles) {
      super(fm);
      this.titles = titles;
    }

    @Override public Fragment getItem(int position) {
      final String title = getPageTitle(position).toString();
      if (title.equals(getContext().getString(R.string.installer))) {
        return new InstallerFragment();
      } else if (title.equals(getContext().getString(R.string.applets))) {
        return new AppletsFragment();
      } else if (title.equals(getContext().getString(R.string.scripts))) {
        return new ScriptsFragment();
      }
      throw new NotImplementedException();
    }

    @Override public int getCount() {
      return titles.length;
    }

    @Override public CharSequence getPageTitle(int position) {
      return titles[position];
    }

  }

}
