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

public class BusyBoxNavigationFragment extends BaseFragment implements NavigationView.OnNavigationItemSelectedListener {

  private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
  private static final int[] DISABLED_STATE_SET = {-android.R.attr.state_enabled};
  static final int[] EMPTY_STATE_SET = new int[0];

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.busybox_navigation_view, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    NavigationView navigationView = findById(R.id.navigation_view);

    navigationView.getMenu().findItem(R.id.action_installer).setChecked(true);
    getActivity().setTitle(navigationView.getMenu().findItem(R.id.action_installer).getTitle());

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

  @Override public boolean onNavigationItemSelected(MenuItem item) {
    if (getActivity() instanceof BaseDrawerActivity) {
      BaseDrawerActivity activity = (BaseDrawerActivity) getActivity();
      activity.toggle();
    }

    if (item.isChecked()) {
      return true;
    }

    item.setChecked(true);

    int itemId = item.getItemId();

    switch (itemId){
      case R.id.action_installer:
        getActivity().setTitle(item.getTitle());
        getActivity().getFragmentManager()
            .beginTransaction()
            .replace(R.id.content_frame, new BusyBoxInstallerFragment())
            .commit();
        return true;
      case R.id.action_applets:
        getActivity().setTitle(item.getTitle());
        getActivity().getFragmentManager()
            .beginTransaction()
            .replace(R.id.content_frame, new BusyBoxAppletsFragment())
            .commit();
        return true;
      case R.id.action_scripts:
        getActivity().setTitle(item.getTitle());
        return true;
      case R.id.action_terminal:
        getActivity().setTitle(item.getTitle());
        return true;
    }

    return true;
  }

}
