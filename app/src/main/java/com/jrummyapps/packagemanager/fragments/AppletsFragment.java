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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jrummyapps.android.base.BaseFragment;
import com.jrummyapps.android.theme.ColorScheme;
import com.jrummyapps.packagemanager.R;
import com.jrummyapps.packagemanager.activities.AboutActivity;
import com.jrummyapps.packagemanager.activities.SettingsActivity;
import com.jrummyapps.packagemanager.dialogs.BusyBoxAppletDialog;
import com.jrummyapps.packagemanager.utils.Utils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.List;
import java.util.Locale;

public class AppletsFragment extends BaseFragment {

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_busybox_applets, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    FastScrollRecyclerView recyclerView = findById(R.id.recycler);
    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    recyclerView.setAdapter(new RecyclerAdapter());
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    menu.add(0, R.id.action_settings, 0, R.string.settings)
        .setIcon(R.drawable.ic_settings_white_24dp)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    menu.add(0, R.id.action_info, 0, R.string.about)
        .setIcon(R.drawable.ic_information_white_24dp)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    ColorScheme.newMenuTint(menu).forceIcons().apply(getActivity());
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_settings:
        startActivity(new Intent(getActivity(), SettingsActivity.class));
        return true;
      case R.id.action_info:
        startActivity(new Intent(getActivity(), AboutActivity.class));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private static class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder>
      implements FastScrollRecyclerView.SectionedAdapter {

    private final List<String> applets = Utils.getBusyBoxApplets();

    @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      LayoutInflater inflater = LayoutInflater.from(parent.getContext());
      return new ViewHolder(inflater.inflate(R.layout.item_busybox_applet, parent, false));
    }

    @Override public void onBindViewHolder(ViewHolder holder, int position) {
      holder.text.setText(applets.get(position));
    }

    @Override public int getItemCount() {
      return applets.size();
    }

    @NonNull @Override public String getSectionName(int position) {
      return String.format("%c", applets.get(position).charAt(0)).toUpperCase(Locale.ENGLISH);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

      public TextView text;

      public ViewHolder(View itemView) {
        super(itemView);
        text = (TextView) itemView.findViewById(R.id.text);
        itemView.setOnClickListener(new View.OnClickListener() {

          @Override public void onClick(View v) {
            BusyBoxAppletDialog.show((Activity) v.getContext(), text.getText().toString());
          }
        });
      }
    }

  }

}
