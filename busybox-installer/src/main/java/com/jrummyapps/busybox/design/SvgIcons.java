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

package com.jrummyapps.busybox.design;

import android.graphics.PointF;

import com.jrummyapps.android.svg.SVGData;

public enum SvgIcons {
  LOGO(
      new String[]{
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
          "M256.8,275.6l-120.5-64.3l0,0l-54.5,31.3l54.5,30.7l63,35.4L256.8,275.6L256.8,275.6L256.8,275.6L256.8,275.6z"
      },
      new int[]{
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
      },
      512, 512
  );

  final String[] glyphs;
  final int[] colors;
  final int width;
  final int height;

  SvgIcons(String[] glyphs, int[] colors, int width, int height) {
    this.glyphs = glyphs;
    this.colors = colors;
    this.width = width;
    this.height = height;
  }

  public SVGData getSvgData() {
    return new SVGData(new PointF(width, height), glyphs, colors);
  }

}
