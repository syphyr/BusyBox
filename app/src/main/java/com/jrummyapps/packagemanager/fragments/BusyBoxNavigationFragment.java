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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.jrummyapps.android.fileproperties.wallpaper.NowWallpaper;
import com.jrummyapps.android.util.ResUtils;
import com.jrummyapps.packagemanager.R;
import com.squareup.picasso.Picasso;

import java.util.Random;

public class BusyBoxNavigationFragment extends BaseFragment implements NavigationView.OnNavigationItemSelectedListener {

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return new NavigationView(getActivity());
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    NavigationView navigationView = (NavigationView) view;
    navigationView.getMenu().add(0, 1, 0, "Installer").setIcon(R.drawable.ic_package_down_white_24dp);
    navigationView.getMenu().add(0, 2, 0, "Applets").setIcon(R.drawable.ic_applets_white_24dp);
    navigationView.getMenu().add(0, 3, 0, "Scripts").setIcon(R.drawable.ic_code_array_white_24dp);
    navigationView.getMenu().add(0, 3, 0, "Terminal").setIcon(R.drawable.ic_terminal_white_24dp);

    ImageView headerView = new KenBurnsView(getActivity());
    headerView.setLayoutParams(new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ResUtils.dpToPx(180)));

    NetworkInfo networkInfo =
        ((ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
    if (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
      NowWallpaper[] wallpapers = NowWallpaper.values();
      Random random = new Random();
      NowWallpaper nowWallpaper = wallpapers[random.nextInt(wallpapers.length)];
      int size = Math.min(1440, ResUtils.getScreenWidth());
      String url = nowWallpaper.getUrl(NowWallpaper.getTimeOfDay(), size, size);
      Picasso.with(getActivity())
          .load(url)
          .centerCrop()
          .fit()
          .error(R.drawable.materialviewpagerheader)
          .into(headerView);
    } else {
      headerView.setImageResource(R.drawable.materialviewpagerheader);
    }

    navigationView.addHeaderView(headerView);
    navigationView.setNavigationItemSelectedListener(this);
  }

  @Override public boolean onNavigationItemSelected(MenuItem item) {
    if (getActivity() instanceof BaseDrawerActivity) {
      BaseDrawerActivity activity = (BaseDrawerActivity) getActivity();
      activity.toggle();
    }
    return true;
  }

}
