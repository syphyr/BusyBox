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

import android.app.Activity;
import android.app.Fragment;
import android.view.View;
import android.view.ViewGroup;

import com.jrummyapps.android.animations.Technique;
import com.jrummyapps.android.easteregg.EasterEggCallback;
import com.jrummyapps.android.preferences.activities.MainPreferenceActivity;
import com.jrummyapps.android.util.ViewUtils;
import com.jrummyapps.busybox.R;
import com.jrummyapps.busybox.fragments.AboutFragment;
import com.jrummyapps.busybox.fragments.SettingsFragment;

import java.util.Random;

public class SettingsActivity extends MainPreferenceActivity implements EasterEggCallback {

  @Override protected Fragment getFragment(int position) {
    int stringId = getStringId(position);
    if (stringId == R.string.settings) {
      return new SettingsFragment();
    } else if (stringId == R.string.about) {
      return new AboutFragment();
    }
    return super.getFragment(position);
  }

  @Override public void onRequestEgg(Activity activity, int id, int count) {
    Technique[] techniques = {
        Technique.SHAKE,
        Technique.BOUNCE,
        Technique.FLASH,
        Technique.PULSE,
        Technique.ROTATE,
        Technique.SWING,
        Technique.TADA,
        Technique.WAVE,
        Technique.WOBBLE
    };
    Random random = new Random();
    Technique technique = techniques[random.nextInt(techniques.length)];
    ViewGroup viewGroup = ViewUtils.getRootView(activity);
    if (random.nextInt(10) == 0) {
      Technique.ROTATE.playOn(viewGroup);
      return;
    }
    animate(viewGroup, technique);
  }

  private void animate(ViewGroup viewGroup, Technique technique) {
    for (int i = 0, len = viewGroup.getChildCount(); i < len; i++) {
      View child = viewGroup.getChildAt(i);
      if (child instanceof ViewGroup) {
        animate((ViewGroup) child, technique);
      } else {
        technique.playOn(child);
      }
    }
  }

}
