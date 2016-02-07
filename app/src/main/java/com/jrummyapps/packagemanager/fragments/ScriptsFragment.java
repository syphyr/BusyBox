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
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.jrummyapps.android.app.App;
import com.jrummyapps.android.base.BaseFragment;
import com.jrummyapps.android.colors.Color;
import com.jrummyapps.android.eventbus.EventBusHook;
import com.jrummyapps.android.eventbus.Events;
import com.jrummyapps.android.io.FileHelper;
import com.jrummyapps.android.io.FileUtils;
import com.jrummyapps.android.preferences.activities.MainPreferenceActivity;
import com.jrummyapps.android.prefs.Prefs;
import com.jrummyapps.android.roottools.utils.Assets;
import com.jrummyapps.android.theme.ColorScheme;
import com.jrummyapps.android.theme.Themes;
import com.jrummyapps.android.view.ViewHolder;
import com.jrummyapps.android.widget.jazzylistview.JazzyListView;
import com.jrummyapps.packagemanager.R;
import com.jrummyapps.packagemanager.activities.CreateScriptActivity;
import com.jrummyapps.packagemanager.database.Database;
import com.jrummyapps.packagemanager.database.ShellScriptTable;
import com.jrummyapps.packagemanager.dialogs.CreateScriptDialog;
import com.jrummyapps.packagemanager.scripts.ShellScript;
import com.jrummyapps.packagemanager.transitions.FabDialogMorphSetup;
import com.jrummyapps.packagemanager.utils.Utils;
import com.jrummyapps.texteditor.activities.TextEditorActivity;
import com.jrummyapps.texteditor.shell.activities.ScriptExecutorActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScriptsFragment extends BaseFragment {

  private static final String LOAD_SCRIPTS_FROM_ASSETS = "load_scripts_from_assets";

  public static final int REQUEST_CREATE_SCRIPT = 27;

  public static List<ShellScript> getShellScripts() {
    ShellScriptTable table = Database.getInstance().getTable(ShellScriptTable.NAME);
    List<ShellScript> scripts = new ArrayList<>();

    if (Prefs.getInstance().get(LOAD_SCRIPTS_FROM_ASSETS, true)) {
      String json = Utils.readRaw(R.raw.scripts);
      try {
        JSONArray jsonArray = new JSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {
          JSONObject jsonObject = jsonArray.getJSONObject(i);
          String name = jsonObject.getString("name");
          String filename = jsonObject.getString("filename");
          String info = jsonObject.getString("info");
          String asset = "scripts/" + filename;
          //noinspection OctalInteger
          Assets.transferAsset(App.getContext(), asset, asset, 0755);
          File file = new File(App.getContext().getFilesDir(), asset);
          ShellScript script = new ShellScript(name, file.getAbsolutePath()).setInfo(info);
          table.insert(script);
        }
        Prefs.getInstance().save(LOAD_SCRIPTS_FROM_ASSETS, false);
      } catch (JSONException e) {
        Crashlytics.logException(e);
      }
    }

    scripts.addAll(table.select());

    return scripts;
  }

  private Adapter adapter;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    Events.register(this);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    Events.unregister(this);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_scripts, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    new AsyncTask<Void, Void, List<ShellScript>>() {

      @Override protected List<ShellScript> doInBackground(Void... params) {
        return getShellScripts();
      }

      @Override protected void onPostExecute(List<ShellScript> scripts) {
        JazzyListView listView = findById(android.R.id.list);
        adapter = new Adapter(scripts);
        listView.setAdapter(adapter);
      }

    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    findById(R.id.fab).setOnClickListener(new View.OnClickListener() {

      @Override public void onClick(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          Intent intent = new Intent(getActivity(), CreateScriptActivity.class);
          intent.putExtra(FabDialogMorphSetup.EXTRA_SHARED_ELEMENT_START_COLOR, ColorScheme.getAccent());
          ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
              getActivity(), v, getString(R.string.dialog_transition));
          startActivityForResult(intent, REQUEST_CREATE_SCRIPT, options.toBundle());
        } else {
          // normal dialog with no fancy animations :-(
          new CreateScriptDialog().show(getFragmentManager(), "CreateScriptDialog");
        }
      }
    });

  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    menu.add(0, R.id.action_settings, 0, R.string.settings)
        .setIcon(R.drawable.ic_settings_white_24dp)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    ColorScheme.newMenuTint(menu).forceIcons().apply(getActivity());
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_settings:
        startActivity(new Intent(getActivity(), MainPreferenceActivity.class));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CREATE_SCRIPT && resultCode == Activity.RESULT_OK) {
      String name = data.getStringExtra(CreateScriptActivity.EXTRA_SCRIPT_NAME);
      String filename = data.getStringExtra(CreateScriptActivity.EXTRA_FILE_NAME);
      createScript(name, filename);
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @EventBusHook public void onEvent(CreateScriptDialog.CreateScriptEvent event) {
    createScript(event.name, event.filename);
  }

  private void createScript(String name, String filename) {
    File file = new File(getActivity().getFilesDir(), "scripts/" + filename);
    ShellScript script = new ShellScript(name, file.getAbsolutePath());
    int errorMessage = 0;

    for (ShellScript shellScript : adapter.scripts) {
      if (shellScript.path.equals(script.path) || shellScript.name.equals(script.name)) {
        errorMessage = R.string.a_script_with_that_name_already_exists;
        break;
      }
    }

    try {
      FileUtils.touch(file);
    } catch (IOException e) {
      errorMessage = R.string.an_error_occurred_while_creating_the_file;
      Crashlytics.logException(e);
    }

    if (errorMessage != 0) {
      Snackbar snackbar = Snackbar.make(findById(R.id.fab), errorMessage, Snackbar.LENGTH_LONG);
      View view = snackbar.getView();
      TextView messageText = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
      if (Themes.isDark()) {
        messageText.setTextColor(ColorScheme.getPrimaryText(getActivity()));
        view.setBackgroundColor(ColorScheme.getBackgroundDark(getActivity()));
      } else {
        messageText.setTextColor(Color.WHITE);
      }
      snackbar.show();
      return;
    }

    ShellScriptTable table = Database.getInstance().getTable(ShellScriptTable.NAME);
    table.insert(script);
    adapter.scripts.add(script);
    adapter.notifyDataSetChanged();

    Intent intent = new Intent(getActivity(), TextEditorActivity.class);
    intent.putExtra(FileHelper.INTENT_EXTRA_PATH, script.path);
    startActivity(intent);
  }

  private final class Adapter extends BaseAdapter {

    private final List<ShellScript> scripts;

    public Adapter(List<ShellScript> scripts) {
      this.scripts = scripts;
    }

    @Override public int getCount() {
      return scripts.size();
    }

    @Override public ShellScript getItem(int position) {
      return scripts.get(position);
    }

    @Override public long getItemId(int position) {
      return position;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
      final ViewHolder holder;

      if (convertView == null) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        convertView = inflater.inflate(R.layout.item_script, parent, false);
        holder = new ViewHolder(convertView);

        ImageView imageView = holder.find(R.id.icon);
        imageView.setColorFilter(ColorScheme.getAccent());
        imageView.setImageResource(R.drawable.ic_code_array_white_24dp);
      } else {
        holder = (ViewHolder) convertView.getTag();
      }

      final ShellScript script = getItem(position);
      holder.setText(R.id.text, script.name);

      holder.find(R.id.list_item).setOnClickListener(new View.OnClickListener() {

        @Override public void onClick(View v) {
          PopupMenu popupMenu = new PopupMenu(getActivity(), v);

          popupMenu.getMenu().add(0, 1, 0, R.string.run).setIcon(R.drawable.ic_play_white_24dp);
          popupMenu.getMenu().add(0, 2, 0, R.string.edit).setIcon(R.drawable.ic_edit_white_24dp);
          popupMenu.getMenu().add(0, 3, 0, R.string.info).setIcon(R.drawable.ic_information_white_24dp);
          popupMenu.getMenu().add(0, 4, 0, R.string.delete).setIcon(R.drawable.ic_delete_white_24dp);

          if (TextUtils.isEmpty(script.info)) {
            popupMenu.getMenu().findItem(3).setVisible(false);
          }

          ColorScheme.newMenuTint(popupMenu.getMenu()).forceIcons().apply(getActivity());

          popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override public boolean onMenuItemClick(MenuItem item) {
              switch (item.getItemId()) {
                case 1: { // run
                  Intent intent = new Intent(getActivity(), ScriptExecutorActivity.class);
                  intent.putExtra(FileHelper.INTENT_EXTRA_PATH, script.path);
                  startActivity(intent);
                  return true;
                }
                case 2: { // edit
                  Intent intent = new Intent(getActivity(), TextEditorActivity.class);
                  intent.putExtra(FileHelper.INTENT_EXTRA_PATH, script.path);
                  startActivity(intent);
                  return true;
                }
                case 3: { // info
                  new AlertDialog.Builder(getActivity())
                      .setTitle(script.name)
                      .setMessage(script.info)
                      .setPositiveButton(android.R.string.ok, null)
                      .show();
                  return true;
                }
                case 4: { // delete
                  ShellScriptTable table = Database.getInstance().getTable(ShellScriptTable.NAME);
                  boolean deleted = table.delete(script) != 0;
                  if (deleted) {
                    scripts.remove(script);
                    notifyDataSetChanged();
                    new File(script.path).delete();
                  }
                  return true;
                }
                default:
                  return false;
              }
            }
          });

          popupMenu.show();

        }
      });

      return convertView;
    }

  }

}
