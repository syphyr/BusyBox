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

package com.jrummyapps.packagemanager.activities;

import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.transition.TransitionInflater;
import android.view.View;
import android.widget.TextView;

import com.commit451.elasticdragdismisslayout.ElasticDragDismissFrameLayout;
import com.commit451.elasticdragdismisslayout.ElasticDragDismissListener;
import com.jrummyapps.android.base.BaseCompatActivity;
import com.jrummyapps.android.html.HtmlBuilder;
import com.jrummyapps.android.svg.SVGData;
import com.jrummyapps.android.theme.ColorScheme;
import com.jrummyapps.android.theme.Themes;
import com.jrummyapps.android.widget.svg.SvgOutlineView;
import com.jrummyapps.packagemanager.R;

public class AboutActivity extends BaseCompatActivity implements ElasticDragDismissListener {

  private static final SVGData LOGO;

  static {
    String[] glyphs = {
        "M481,452.9c0,15.5-12.6,28.1-28.1,28.1H59.1C43.6,481,31,468.4,31,452.9V59.1C31,43.6,43.6,31,59.1,31h393.8c15.5,0,28.1,12.6,28.1,28.1V452.9z",
        "M256.4,397.8l-0.1-0.1l-119.9-71.4v-115l0,0l120.4,64.2l0,0l0.1,0l0,0V398l0,0l-0.1-0.1L256.4,397.8z",
        "M375.7,211.3l-118.8,64.3V398l118.8-71.9L375.7,211.3L375.7,211.3z",
        "M375.7,211.3v84.2L312.3,328l-55.5-52.4v0L375.7,211.3L375.7,211.3z",
        "M433.3,244.4L375.7,274l-64,32.9l-54.8-31.3v0l118.8-64.3l0,0L433.3,244.4z",
        "M255.2,147l120.5,64.3l0,0l54.5-31.3l-54.5-30.7l-63-35.4L255.2,147L255.2,147L255.2,147L255.2,147z",
        "M78.6,178.3l57.6-29.6l64-32.9l54.8,31.3v0l-118.8,64.3l0,0L78.6,178.3z",
        "M255.1,147l1.8,128.7l-0.1,0l-120.5-64.3L255.1,147z",
        "M256.9,275.7L256.9,275.7L255.1,147l0,0l0.1,0l120.5,64.3L256.9,275.7z",
        "M256.9,275.7V350l-91-48.6l9.6-6.1l23.8,13.4L256.9,275.7L256.9,275.7z",
        "M256.8,275.6L256.8,275.6l-0.1,122.3l0.2,0.1L256.8,275.6L256.8,275.6z",
        "M256.8,275.6l-120.5-64.3l0,0l-54.5,31.3l54.5,30.7l63,35.4L256.8,275.6L256.8,275.6L256.8,275.6L256.8,275.6z",
    };

    int[] colors = {
        0xFF41A4C4,
        0xFFD95545,
        0xFFC54C3F,
        0xFFAA4438,
        0xFFF4F3EE,
        0xFFF4F3EE,
        0xFFF4F3EE,
        0xFFD2D1CC,
        0xFFDCDAD6,
        0xFFC54C3F,
        0xFFD95545,
        0xFFF4F3EE
    };

    PointF viewport = new PointF(512, 512);

    LOGO = new SVGData(viewport, glyphs, colors);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_about_busybox);

    final ElasticDragDismissFrameLayout draggableLayout = findById(R.id.draggable_frame);
    final View backgroundView = findById(R.id.about_background);
    final SvgOutlineView svgView = findById(R.id.svg);
    final TextView aboutText = findById(R.id.about_text);
    final TextView creditsText = findById(R.id.credits_text);

    backgroundView.setBackgroundColor(ColorScheme.getBackgroundLight(this));

    svgView.setSvgData(LOGO);
    svgView.adjustDuration(1.25f);
    svgView.setSvgColorsAsTrace(true);
    svgView.start();

    HtmlBuilder html = new HtmlBuilder();

    html.h3("BusyBox: The Swiss Army Knife of Embedded Linux");

    html.p("BusyBox combines tiny versions of many common UNIX utilities into a single small executable. It " +
        "provides replacements for most of the utilities you usually find in GNU fileutils, shellutils, etc. The " +
        "utilities in BusyBox generally have fewer options than their full-featured GNU cousins; however, the options" +
        " that are included provide the expected functionality and behave very much like their GNU counterparts. " +
        "BusyBox provides a fairly complete environment for any small or embedded system.");

    html.p("BusyBox has been written with size-optimization and limited resources in mind. It is also extremely " +
        "modular so you can easily include or exclude commands (or features) at compile time. This makes it easy to " +
        "customize your embedded systems. To create a working system, just add some device nodes in /dev, a few " +
        "configuration files in /etc, and a Linux kernel.");

    html.p().append("BusyBox is maintained by Denys Vlasenko, and licensed under the ")
        .a("https://busybox.net/license.html", "GNU GENERAL PUBLIC LICENSE")
        .append(" version 2.")
        .close();

    aboutText.setText(html.toSpan());
    aboutText.setLinkTextColor(0xFF41A4C4);
    aboutText.setMovementMethod(LinkMovementMethod.getInstance());

    html = new HtmlBuilder();
    html.p("Developed by Jared Rummler");
    html.a("https://twitter.com/#jrummy16", "Twitter");
    html.append(" | ");
    html.a("https://plus.google.com/+JaredRummler", "Google+");

    creditsText.setText(html.toSpan());
    creditsText.setLinkTextColor(0xFF41A4C4);
    creditsText.setMovementMethod(LinkMovementMethod.getInstance());

    draggableLayout.addListener(this);

  }

  @Override public void applyWindowBackground() {
    // NO-OP
  }

  @Override public int getActivityTheme() {
    switch (Themes.getBaseTheme()) {
      case DARK:
        return R.style.Theme_Dark_NoActionBar_Translucent;
      case LIGHT:
      default:
        return R.style.Theme_Light_NoActionBar_Translucent;
    }
  }

  @Override public void onDrag(float elasticOffset, float elasticOffsetPixels, float rawOffset, float rawOffsetPixels) {

  }

  @Override public void onDragDismissed() {
    final ElasticDragDismissFrameLayout draggableLayout = findById(R.id.draggable_frame);
    // if we drag dismiss downward then the default reversal of the enter
    // transition would slide content upward which looks weird. So reverse it.
    if (draggableLayout.getTranslationY() > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().setReturnTransition(TransitionInflater.from(this).inflateTransition(R.transition.return_downward));
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      finishAfterTransition();
    } else {
      finish();
    }
  }

}
