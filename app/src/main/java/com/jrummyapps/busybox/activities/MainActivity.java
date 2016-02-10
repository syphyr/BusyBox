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
import android.graphics.drawable.Drawable;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.jrummyapps.android.analytics.Analytics;
import com.jrummyapps.android.animations.Technique;
import com.jrummyapps.android.app.App;
import com.jrummyapps.android.base.BaseCompatActivity;
import com.jrummyapps.android.directorypicker.DirectoryPickerDialog;
import com.jrummyapps.android.eventbus.EventBusHook;
import com.jrummyapps.android.eventbus.Events;
import com.jrummyapps.android.exceptions.NotImplementedException;
import com.jrummyapps.android.io.WriteExternalStoragePermissions;
import com.jrummyapps.android.theme.ColorScheme;
import com.jrummyapps.android.theme.Themes;
import com.jrummyapps.android.tinting.EdgeTint;
import com.jrummyapps.android.util.ReflectUtils;
import com.jrummyapps.busybox.R;
import com.jrummyapps.busybox.fragments.AppletsFragment;
import com.jrummyapps.busybox.fragments.InstallerFragment;
import com.jrummyapps.busybox.fragments.ScriptsFragment;
import com.jrummyapps.busybox.monetize.Monetize;
import com.jrummyapps.busybox.monetize.OnAdsRemovedEvent;
import com.jrummyapps.busybox.monetize.OnPurchasedPremiumEvent;
import com.jrummyapps.busybox.monetize.RequestPremiumEvent;
import com.jrummyapps.busybox.monetize.RequestRemoveAds;
import com.jrummyapps.busybox.monetize.ShowInterstitalAdEvent;
import com.jrummyapps.busybox.utils.Utils;

import java.io.File;

import static com.jrummyapps.android.app.App.getContext;
import static com.jrummyapps.android.util.FragmentUtils.getCurrentFragment;

public class MainActivity extends BaseCompatActivity implements
    DirectoryPickerDialog.OnDirectorySelectedListener,
    DirectoryPickerDialog.OnDirectoryPickerCancelledListener, BillingProcessor.IBillingHandler {

  private static final String TAG = "MainActivity";

  private InterstitialAd interstitialAd;
  private BillingProcessor bp;
  private ViewPager viewPager;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    TabLayout tabLayout = findById(R.id.tabs);
    viewPager = findById(R.id.container);
    Toolbar toolbar = findById(R.id.toolbar);
    AdView adView = (AdView) findViewById(R.id.ad_view);

    String[] titles = {getString(R.string.applets), getString(R.string.installer), getString(R.string.scripts)};
    SectionsAdapter pagerAdapter = new SectionsAdapter(getSupportFragmentManager(), titles);

    setSupportActionBar(toolbar);
    viewPager.setOffscreenPageLimit(2);
    viewPager.setAdapter(pagerAdapter);
    tabLayout.setupWithViewPager(viewPager);
    viewPager.setCurrentItem(1);

    bp = new BillingProcessor(this, Monetize.decrypt(Monetize.ENCRYPTED_LICENSE_KEY), this);

    if (!Monetize.isAdsRemoved()) {
      AdRequest adRequest;
      if (App.isDebug()) {
        adRequest = new AdRequest.Builder().addTestDevice(Utils.getDeviceId(this)).build();
      } else {
        adRequest = new AdRequest.Builder().build();
      }
      adView.loadAd(adRequest);
      loadInterstitialAd();
    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.monetize_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.action_remove_ads).setVisible(!Monetize.isAdsRemoved());
    menu.findItem(R.id.action_unlock_premium).setVisible(!Monetize.isProVersion());
    return super.onPrepareOptionsMenu(menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_remove_ads:
        onEventMainThread(new RequestRemoveAds());
        return true;
      case R.id.action_unlock_premium:
        onEventMainThread(new RequestPremiumEvent());
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @TargetApi(Build.VERSION_CODES.M)
  @Override public View onViewCreated(@NonNull View view, @Nullable AttributeSet attrs) {
    // We need to manually set the color scheme on Android 6.0+
    try {
      if (view instanceof TextView) {
        TextView textView = (TextView) view;
        ColorStateList textColors = textView.getTextColors();
        ColorScheme.applyColorScheme(textColors);
        textView.setTextColor(textColors);
        ColorScheme.applyColorScheme(textView.getBackgroundTintList());
        ColorScheme.applyColorScheme(textView.getBackground());
      }
      if (view instanceof ViewGroup) {
        if (view.getClass().getName().startsWith("android.widget")) {
          ColorScheme.applyColorScheme(view.getBackground());
        } else {
          ColorScheme.applyColorScheme((Drawable) ReflectUtils.getFieldValue(view, "mBackground"));
        }
      }
      if (view instanceof ImageButton) {
        ColorScheme.applyColorScheme(view.getBackground());
      }
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
      EdgeTint.setEdgeGlowColor(view, ColorScheme.getPrimary());
    } catch (Exception e) {
      Crashlytics.logException(e);
    }
    return view;
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (bp.handleActivityResult(requestCode, resultCode, data)) {
      return;
    } else if (requestCode == ScriptsFragment.REQUEST_CREATE_SCRIPT) {
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

  @Override public void onDirectorySelected(File directory) {
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

  @EventBusHook public void onEvent(ShowInterstitalAdEvent event) {
    if (interstitialAd != null && interstitialAd.isLoaded()) {
      interstitialAd.show();
    }
  }

  @EventBusHook public void onEventMainThread(OnAdsRemovedEvent event) {
    Technique.SLIDE_OUT_DOWN.getComposer().hideOnFinished().playOn(findViewById(R.id.ad_view));
    interstitialAd = null;
  }

  @EventBusHook public void onEventMainThread(OnPurchasedPremiumEvent event) {

  }

  @EventBusHook public void onEventMainThread(RequestPremiumEvent event) {
    bp.purchase(this, Monetize.decrypt(Monetize.ENCRYPTED_PRO_VERSION_PRODUCT_ID));
  }

  @EventBusHook public void onEventMainThread(RequestRemoveAds event) {
    bp.purchase(this, Monetize.decrypt(Monetize.ENCRYPTED_REMOVE_ADS_PRODUCT_ID));
  }

  private void loadInterstitialAd() {
    if (Monetize.isAdsRemoved()) return;
    if (interstitialAd == null) {
      interstitialAd = new InterstitialAd(this);
    }
    interstitialAd.setAdUnitId(getString(R.string.banner_ad_unit_id));
    if (App.isDebug()) {
      interstitialAd.loadAd(new AdRequest.Builder().addTestDevice(Utils.getDeviceId(this)).build());
    } else {
      interstitialAd.loadAd(new AdRequest.Builder().build());
    }
    interstitialAd.setAdListener(new AdListener() {

      @Override public void onAdClosed() {
        if (App.isDebug()) {
          interstitialAd.loadAd(new AdRequest.Builder().addTestDevice(Utils.getDeviceId(getActivity())).build());
        } else {
          interstitialAd.loadAd(new AdRequest.Builder().build());
        }
      }
    });
  }

  @Override public void onProductPurchased(String productId, TransactionDetails details) {
    // Called when requested PRODUCT ID was successfully purchased
    Analytics.newEvent("Purchased Product")
        .put("product_id", productId)
        .put("order_id", details.orderId)
        .put("token", details.purchaseToken)
        .put("purchase_time", details.purchaseTime)
        .log();

    if (productId.equals(Monetize.decrypt(Monetize.ENCRYPTED_PRO_VERSION_PRODUCT_ID))) {
      Monetize.removeAds();
      Monetize.unlockProVersion();
      Events.post(new OnAdsRemovedEvent());
      Events.post(new OnPurchasedPremiumEvent());
    } else if (productId.equals(Monetize.decrypt(Monetize.ENCRYPTED_REMOVE_ADS_PRODUCT_ID))) {
      Monetize.removeAds();
      Events.post(new OnAdsRemovedEvent());
    }
  }

  @Override public void onPurchaseHistoryRestored() {
    // Called when requested PRODUCT ID was successfully purchased
    Log.i(TAG, "Restored purchases");
  }

  @Override public void onBillingError(int errorCode, Throwable error) {
    // Called when some error occurred. See Constants class for more details
    Analytics.newEvent("Billing error").put("error_code", errorCode).log();
    Crashlytics.logException(error);
  }

  @Override public void onBillingInitialized() {
    // Called when BillingProcessor was initialized and it's ready to purchase
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
