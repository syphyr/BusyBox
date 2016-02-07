/*
 * Copyright 2015 Google Inc.
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

package com.jrummyapps.packagemanager.utils;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.util.ArrayMap;
import android.transition.Transition;
import android.util.Property;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import java.util.ArrayList;

/**
 * Utility methods for working with animations.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class AnimUtils {

  // taken from https://github.com/nickbutcher/plaid

  private AnimUtils() {
  }

  private static Interpolator fastOutSlowIn;
  private static Interpolator fastOutLinearIn;
  private static Interpolator linearOutSlowIn;

  public static Interpolator getFastOutSlowInInterpolator(Context context) {
    if (fastOutSlowIn == null) {
      fastOutSlowIn = AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in);
    }
    return fastOutSlowIn;
  }

  public static Interpolator getFastOutLinearInInterpolator(Context context) {
    if (fastOutLinearIn == null) {
      fastOutLinearIn = AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_linear_in);
    }
    return fastOutLinearIn;
  }

  public static Interpolator getLinearOutSlowInInterpolator(Context context) {
    if (linearOutSlowIn == null) {
      linearOutSlowIn = AnimationUtils.loadInterpolator(context, android.R.interpolator.linear_out_slow_in);
    }
    return linearOutSlowIn;
  }

  /**
   * Linear interpolate between a and b with parameter t.
   */
  public static float lerp(float a, float b, float t) {
    return a + (b - a) * t;
  }

  /**
   * An implementation of {@link android.util.Property} to be used specifically with fields of type
   * <code>float</code>. This type-specific subclass enables performance benefit by allowing
   * calls to a {@link #set(Object, Float) set()} function that takes the primitive
   * <code>float</code> type and avoids autoboxing and other overhead associated with the
   * <code>Float</code> class.
   *
   * @param <T>
   *     The class on which the Property is declared.
   **/
  public static abstract class FloatProperty<T> extends Property<T, Float> {

    public FloatProperty(String name) {
      super(Float.class, name);
    }

    /**
     * A type-specific override of the {@link #set(Object, Float)} that is faster when dealing
     * with fields of type <code>float</code>.
     */
    public abstract void setValue(T object, float value);

    @Override final public void set(T object, Float value) {
      setValue(object, value);
    }
  }

  /**
   * An implementation of {@link android.util.Property} to be used specifically with fields of type
   * <code>int</code>. This type-specific subclass enables performance benefit by allowing
   * calls to a {@link #set(Object, Integer) set()} function that takes the primitive
   * <code>int</code> type and avoids autoboxing and other overhead associated with the
   * <code>Integer</code> class.
   *
   * @param <T>
   *     The class on which the Property is declared.
   */
  public static abstract class IntProperty<T> extends Property<T, Integer> {

    public IntProperty(String name) {
      super(Integer.class, name);
    }

    /**
     * A type-specific override of the {@link #set(Object, Integer)} that is faster when dealing
     * with fields of type <code>int</code>.
     */
    public abstract void setValue(T object, int value);

    @Override final public void set(T object, Integer value) {
      setValue(object, value);
    }

  }

  /**
   * Interrupting Activity transitions can yield an OperationNotSupportedException when the
   * transition tries to pause the animator. Yikes! We can fix this by wrapping the Animator:
   */
  public static class NoPauseAnimator extends Animator {

    private final Animator animator;
    private final ArrayMap<AnimatorListener, AnimatorListener> listeners = new ArrayMap<>();

    public NoPauseAnimator(Animator animator) {
      this.animator = animator;
    }

    @Override public void addListener(AnimatorListener listener) {
      AnimatorListener wrapper = new AnimatorListenerWrapper(this, listener);
      if (!listeners.containsKey(listener)) {
        listeners.put(listener, wrapper);
        animator.addListener(wrapper);
      }
    }

    @Override public void cancel() {
      animator.cancel();
    }

    @Override public void end() {
      animator.end();
    }

    @Override public long getDuration() {
      return animator.getDuration();
    }

    @Override public TimeInterpolator getInterpolator() {
      return animator.getInterpolator();
    }

    @Override public void setInterpolator(TimeInterpolator timeInterpolator) {
      animator.setInterpolator(timeInterpolator);
    }

    @Override public ArrayList<AnimatorListener> getListeners() {
      return new ArrayList<AnimatorListener>(listeners.keySet());
    }

    @Override public long getStartDelay() {
      return animator.getStartDelay();
    }

    @Override public void setStartDelay(long delayMS) {
      animator.setStartDelay(delayMS);
    }

    @Override public boolean isPaused() {
      return animator.isPaused();
    }

    @Override public boolean isRunning() {
      return animator.isRunning();
    }

    @Override public boolean isStarted() {
      return animator.isStarted();
    }

    @Override public void removeAllListeners() {
      listeners.clear();
      animator.removeAllListeners();
    }

    @Override public void removeListener(AnimatorListener listener) {
      AnimatorListener wrapper = listeners.get(listener);
      if (wrapper != null) {
        listeners.remove(listener);
        animator.removeListener(wrapper);
      }
    }

    @Override public Animator setDuration(long durationMS) {
      animator.setDuration(durationMS);
      return this;
    }

    @Override public void setTarget(Object target) {
      animator.setTarget(target);
    }

    @Override public void setupEndValues() {
      animator.setupEndValues();
    }

    @Override public void setupStartValues() {
      animator.setupStartValues();
    }

    @Override public void start() {
      animator.start();
    }
  }

  static class AnimatorListenerWrapper implements Animator.AnimatorListener {

    private final Animator animator;
    private final Animator.AnimatorListener listener;

    public AnimatorListenerWrapper(Animator animator, Animator.AnimatorListener listener) {
      this.animator = animator;
      this.listener = listener;
    }

    @Override public void onAnimationStart(Animator animator) {
      listener.onAnimationStart(this.animator);
    }

    @Override public void onAnimationEnd(Animator animator) {
      listener.onAnimationEnd(this.animator);
    }

    @Override public void onAnimationCancel(Animator animator) {
      listener.onAnimationCancel(this.animator);
    }

    @Override public void onAnimationRepeat(Animator animator) {
      listener.onAnimationRepeat(this.animator);
    }
  }

  public static class TransitionListenerAdapter implements Transition.TransitionListener {

    @Override public void onTransitionStart(Transition transition) {
    }

    @Override public void onTransitionEnd(Transition transition) {
    }

    @Override public void onTransitionCancel(Transition transition) {
    }

    @Override public void onTransitionPause(Transition transition) {
    }

    @Override public void onTransitionResume(Transition transition) {
    }

  }

}