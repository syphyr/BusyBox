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

package com.jrummy.busybox.installer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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
import com.jrummyapps.android.eventbus.EventBusHook;
import com.jrummyapps.android.eventbus.Events;
import com.jrummyapps.busybox.monetize.Monetize;
import com.jrummyapps.busybox.monetize.OnAdsRemovedEvent;
import com.jrummyapps.busybox.monetize.OnPurchasedPremiumEvent;
import com.jrummyapps.busybox.monetize.RequestPremiumEvent;
import com.jrummyapps.busybox.monetize.RequestRemoveAds;
import com.jrummyapps.busybox.monetize.ShowInterstitalAdEvent;
import com.jrummyapps.busybox.utils.Utils;

public class MainActivity extends com.jrummyapps.busybox.activities.MainActivity
    implements BillingProcessor.IBillingHandler {

  private static final String TAG = "MainActivity";

  private InterstitialAd interstitialAd;
  private BillingProcessor bp;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final AdView adView = (AdView) findViewById(R.id.ad_view);
    bp = new BillingProcessor(this, Monetize.decrypt(Monetize.ENCRYPTED_LICENSE_KEY), this);
    if (!Monetize.isAdsRemoved()) {
      AdRequest adRequest;
      if (App.isDebug()) {
        adRequest = new AdRequest.Builder().addTestDevice(Utils.getDeviceId(this)).build();
      } else {
        adRequest = new AdRequest.Builder().build();
      }
      adView.setAdListener(new AdListener() {

        @Override public void onAdFailedToLoad(int errorCode) {
          if (adView.getVisibility() == View.VISIBLE) {
            Technique.SLIDE_OUT_DOWN.getComposer().hideOnFinished().playOn(adView);
          }
        }

        @Override public void onAdLoaded() {
          adView.setVisibility(View.VISIBLE);
        }
      });
      adView.loadAd(adRequest);
      loadInterstitialAd();
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (bp.handleActivityResult(requestCode, resultCode, data)) {
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
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
    int itemId = item.getItemId();
    if (itemId == R.id.action_remove_ads) {
      onEventMainThread(new RequestRemoveAds());
      return true;
    } else if (itemId == R.id.action_unlock_premium) {
      onEventMainThread(new RequestPremiumEvent());
      return true;
    }
    return super.onOptionsItemSelected(item);
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

}
