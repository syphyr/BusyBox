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

import com.jrummyapps.android.common.Toasts;
import com.jrummyapps.android.easteregg.EasterEggCallback;
import com.jrummyapps.android.preferences.activities.MainPreferenceActivity;
import com.jrummyapps.busybox.R;
import com.jrummyapps.busybox.fragments.AboutFragment;
import com.jrummyapps.busybox.fragments.SettingsFragment;

import java.util.Random;

public class SettingsActivity extends MainPreferenceActivity implements EasterEggCallback {

  private static final String[] INSULTS = {
      "I'm not saying I hate you, but I would unplug your life support to charge my phone.",
      "I bet your brain feels as good as new, seeing that you never use it.",
      "You bring everyone a lot of joy, when you leave the room.",
      "Two wrongs don't make a right, take your parents as an example.",
      "If laughter is the best medicine, your face must be curing the world.",
      "You're the reason the gene pool needs a lifeguard.",
      "I don't exactly hate you, but if you were on fire and I had water, I'd drink it.",
      "You're as bright as a black hole, and twice as dense.",
      "If you spoke your mind, you'd be speechless.",
      "Don't feel sad, don't feel blue, Frankenstein was ugly too."
  };

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
    // insult our most valued customers for finding our precious easter egg
    Toasts.show(INSULTS[new Random().nextInt(INSULTS.length)]);
  }

}
