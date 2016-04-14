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

import android.content.ActivityNotFoundException;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.transition.Transition;
import android.view.View;
import android.widget.ImageView;

import com.jrummyapps.android.analytics.Analytics;
import com.jrummyapps.android.animations.Rebound;
import com.jrummyapps.android.base.BaseActivity;
import com.jrummyapps.android.constants.Websites;
import com.jrummyapps.android.theme.BaseTheme;
import com.jrummyapps.android.theme.ColorScheme;
import com.jrummyapps.android.theme.Themes;
import com.jrummyapps.android.transitions.FabDialogMorphSetup;
import com.jrummyapps.android.transitions.TransitionUtils;
import com.jrummyapps.android.util.IntentUtils;
import com.jrummyapps.android.util.OrientationUtils;
import com.jrummyapps.android.util.ResUtils;
import com.jrummyapps.android.widget.svg.SvgOutlineView;
import com.jrummyapps.busybox.R;
import com.jrummyapps.busybox.design.SvgIcons;

public class DeveloperProfileActivity extends BaseActivity {

  private SvgOutlineView twitterView;
  private SvgOutlineView googlePlusView;
  private SvgOutlineView githubView;
  private SvgOutlineView linkedinView;

  private final Rebound.SpringyTouchListener touchListener = new Rebound.SpringyTouchListener() {

    @Override public void onClick(View v) {
      if (v == twitterView) {
        try {
          startActivity(IntentUtils.newTwitterIntent(getPackageManager(), Websites.getDeveloperTwitterPage()));
          Analytics.newEvent("developer twitter").log();
        } catch (ActivityNotFoundException ignored) {
        }
      } else if (v == googlePlusView) {
        try {
          startActivity(IntentUtils.newGooglePlusIntent(getPackageManager(), Websites.getDeveloperGooglePlusProfile()));
          Analytics.newEvent("developer google plus").log();
        } catch (ActivityNotFoundException ignored) {
        }
      } else if (v == githubView) {
        try {
          startActivity(IntentUtils.newOpenWebBrowserIntent(getString(R.string.website_developer_github_page)));
          Analytics.newEvent("developer github").log();
        } catch (ActivityNotFoundException ignored) {
        }
      } else if (v == linkedinView) {
        try {
          startActivity(IntentUtils.newOpenWebBrowserIntent(getString(R.string.website_developer_linkedin_page)));
          Analytics.newEvent("developer linkedin").log();
        } catch (ActivityNotFoundException ignored) {
        }
      }
    }

  };

  @Override protected void onCreate(Bundle savedInstanceState) {
    OrientationUtils.lockOrientation(this);

    super.onCreate(savedInstanceState);

    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    setContentView(R.layout.activity_developer_profile);

    twitterView = findById(R.id.twitter);
    googlePlusView = findById(R.id.google_plus);
    githubView = findById(R.id.github);
    linkedinView = findById(R.id.linkedin);

    findViewById(R.id.bottom_container).setBackgroundColor(ColorScheme.getAccent());

    if (VERSION.SDK_INT >= VERSION_CODES.M) {
      // fix color scheme not applying on Android 6.0+
      ImageView profileImageBackground = findById(R.id.profile_background);
      profileImageBackground.setColorFilter(ColorScheme.getAccent());
    }

    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      FabDialogMorphSetup.setupSharedEelementTransitions(this, findViewById(R.id.container), ResUtils.dpToPx(2));
    }

    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP && getWindow().getSharedElementEnterTransition() != null) {
      getWindow().getSharedElementEnterTransition().addListener(new TransitionUtils.TransitionListenerAdapter() {

        @Override public void onTransitionEnd(Transition transition) {
          loadView();
        }
      });
    } else {
      loadView();
    }
  }

  private void loadView() {
    Analytics.newEvent("viewed developer profile").log();

    twitterView.setSvgData(SvgIcons.TWITTER.getSvgData());
    googlePlusView.setSvgData(SvgIcons.GOOGLE_PLUS.getSvgData());
    githubView.setSvgData(SvgIcons.GITHUB.getSvgData());
    linkedinView.setSvgData(SvgIcons.LINKEDIN.getSvgData());

    twitterView.start();
    googlePlusView.start();
    githubView.start();
    linkedinView.start();

    touchListener.setEndValue(-0.3);
    twitterView.setOnTouchListener(touchListener);
    googlePlusView.setOnTouchListener(touchListener);
    githubView.setOnTouchListener(touchListener);
    linkedinView.setOnTouchListener(touchListener);
  }

  @Override public void applyWindowBackground() {
    // NO-OP
  }

  @Override public int getActivityTheme() {
    if (Themes.getBaseTheme() == BaseTheme.DARK) {
      return R.style.Theme_Dark_NoActionBar_MaterialDialog;
    }
    return R.style.Theme_Light_NoActionBar_MaterialDialog;
  }

  @Override public boolean isSystemBarTintEnabled() {
    return false;
  }

  @Override public void onBackPressed() {
    dismiss(null);
  }

  public void dismiss(View view) {
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      finishAfterTransition();
    } else {
      finish();
    }
  }

}
