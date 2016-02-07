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

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.flaviofaria.kenburnsview.KenBurnsView;
import com.jrummyapps.android.base.BaseDrawerActivity;
import com.jrummyapps.android.base.BaseFragment;
import com.jrummyapps.android.theme.ColorScheme;
import com.jrummyapps.android.util.ResUtils;
import com.jrummyapps.packagemanager.R;

public class NavigationFragment extends BaseFragment implements NavigationView.OnNavigationItemSelectedListener {

  private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
  private static final int[] DISABLED_STATE_SET = {-android.R.attr.state_enabled};
  static final int[] EMPTY_STATE_SET = new int[0];

  private int checkedItemId = R.id.action_installer;

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_navigation_view, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      checkedItemId = savedInstanceState.getInt("checked_item_id", R.id.action_installer);
    }

    NavigationView navigationView = findById(R.id.navigation_view);

    navigationView.getMenu().findItem(checkedItemId).setChecked(true);

    for (int i = 0; i < navigationView.getMenu().size(); i++) {
      if (navigationView.getMenu().getItem(i).isChecked()) {
        System.out.println(navigationView.getMenu().getItem(i).getTitle());
        getActivity().setTitle(navigationView.getMenu().getItem(i).getTitle());
      }
    }

    ImageView headerView = new KenBurnsView(getActivity());
    headerView.setLayoutParams(new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ResUtils.dpToPx(180)));

    navigationView.setItemIconTintList(new ColorStateList(new int[][]{
        DISABLED_STATE_SET,
        CHECKED_STATE_SET,
        EMPTY_STATE_SET
    }, new int[]{
        ColorScheme.getAccent(),
        ColorScheme.getAccent(),
        ColorScheme.getSubMenuIcon()
    }));

    navigationView.setItemTextColor(new ColorStateList(new int[][]{
        DISABLED_STATE_SET,
        CHECKED_STATE_SET,
        EMPTY_STATE_SET
    }, new int[]{
        ColorScheme.getAccent(),
        ColorScheme.getAccent(),
        ColorScheme.getSubMenuIcon()
    }));

    navigationView.setNavigationItemSelectedListener(this);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt("checked_item_id", checkedItemId);
  }

  @Override public boolean onNavigationItemSelected(MenuItem item) {
    if (getActivity() instanceof BaseDrawerActivity) {
      BaseDrawerActivity activity = (BaseDrawerActivity) getActivity();
      activity.toggle();
    }

    if (item.isChecked()) {
      return true;
    }

    int itemId = checkedItemId = item.getItemId();
    getActivity().setTitle(item.getTitle());
    item.setChecked(true);

    switch (itemId) {
      case R.id.action_installer:
        getActivity().getFragmentManager()
            .beginTransaction()
            .replace(R.id.content_frame, new InstallerFragment())
            .commit();
        return true;
      case R.id.action_applets:
        getActivity().getFragmentManager()
            .beginTransaction()
            .replace(R.id.content_frame, new AppletsFragment())
            .commit();
        return true;
      case R.id.action_scripts:
        getActivity().getFragmentManager()
            .beginTransaction()
            .replace(R.id.content_frame, new ScriptsFragment())
            .commit();
        return true;
    }

    return true;
  }

}
