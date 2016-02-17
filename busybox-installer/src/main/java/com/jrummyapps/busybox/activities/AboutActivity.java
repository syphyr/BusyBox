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

import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.transition.TransitionInflater;
import android.view.View;
import android.widget.TextView;

import com.commit451.elasticdragdismisslayout.ElasticDragDismissFrameLayout;
import com.commit451.elasticdragdismisslayout.ElasticDragDismissListener;
import com.jrummyapps.android.animations.Technique;
import com.jrummyapps.android.base.BaseCompatActivity;
import com.jrummyapps.android.html.HtmlBuilder;
import com.jrummyapps.android.theme.ColorScheme;
import com.jrummyapps.android.theme.Themes;
import com.jrummyapps.android.widget.svg.SvgOutlineView;
import com.jrummyapps.busybox.R;
import com.jrummyapps.busybox.design.SvgIcons;

public class AboutActivity extends BaseCompatActivity implements ElasticDragDismissListener {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_about_busybox);

    final ElasticDragDismissFrameLayout draggableLayout = findById(R.id.draggable_frame);
    final View backgroundView = findById(R.id.about_background);
    final SvgOutlineView svgView = findById(R.id.svg);
    final TextView aboutText = findById(R.id.about_text);
    final TextView creditsText = findById(R.id.credits_text);

    backgroundView.setBackgroundColor(ColorScheme.getBackgroundLight(this));
    svgView.setSvgData(SvgIcons.LOGO.getSvgData());
    svgView.adjustDuration(1.25f);
    svgView.setSvgColorsAsTrace(true);
    svgView.start();

    svgView.setOnLongClickListener(new View.OnLongClickListener() {

      @Override public boolean onLongClick(View v) {
        Technique.ROTATE.playOn(v);
        return true;
      }
    });

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
