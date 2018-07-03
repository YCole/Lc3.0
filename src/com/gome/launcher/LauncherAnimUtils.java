/*
 * Copyright (C) 2012 The Android Open Source Project
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
 */

package com.gome.launcher;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;

import com.gome.launcher.util.UiThreadCircularReveal;
import com.gome.launcher.view.WidgetHorizontalScrollView;
import com.gome.launcher.view.WidgetLinearLayout;

import java.util.HashSet;
import java.util.WeakHashMap;

public class LauncherAnimUtils {
    static WeakHashMap<Animator, Object> sAnimators = new WeakHashMap<Animator, Object>();
    static Animator.AnimatorListener sEndAnimListener = new Animator.AnimatorListener() {
        public void onAnimationStart(Animator animation) {
            sAnimators.put(animation, null);
        }

        public void onAnimationRepeat(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
            sAnimators.remove(animation);
        }

        public void onAnimationCancel(Animator animation) {
            sAnimators.remove(animation);
        }
    };

    public static void cancelOnDestroyActivity(Animator a) {
        a.addListener(sEndAnimListener);
    }

    // Helper method. Assumes a draw is pending, and that if the animation's duration is 0
    // it should be cancelled
    public static void startAnimationAfterNextDraw(final Animator animator, final View view) {
        view.getViewTreeObserver().addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
            private boolean mStarted = false;

            public void onDraw() {
                if (mStarted) return;
                mStarted = true;
                // Use this as a signal that the animation was cancelled
                if (animator.getDuration() == 0) {
                    return;
                }
                animator.start();

                final ViewTreeObserver.OnDrawListener listener = this;
                view.post(new Runnable() {
                    public void run() {
                        view.getViewTreeObserver().removeOnDrawListener(listener);
                    }
                });
            }
        });
    }

    public static void onDestroyActivity() {
        HashSet<Animator> animators = new HashSet<Animator>(sAnimators.keySet());
        for (Animator a : animators) {
            if (a.isRunning()) {
                a.cancel();
            }
            sAnimators.remove(a);
        }
    }

    public static AnimatorSet createAnimatorSet() {
        AnimatorSet anim = new AnimatorSet();
        cancelOnDestroyActivity(anim);
        return anim;
    }

    public static ValueAnimator ofFloat(View target, float... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setFloatValues(values);
        cancelOnDestroyActivity(anim);
        return anim;
    }

    public static ObjectAnimator ofFloat(View target, String propertyName, float... values) {
        ObjectAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setPropertyName(propertyName);
        anim.setFloatValues(values);
        cancelOnDestroyActivity(anim);
        new FirstFrameAnimatorHelper(anim, target);
        return anim;
    }

    public static ObjectAnimator ofPropertyValuesHolder(View target,
            PropertyValuesHolder... values) {
        ObjectAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setValues(values);
        cancelOnDestroyActivity(anim);
        new FirstFrameAnimatorHelper(anim, target);
        return anim;
    }

    public static ObjectAnimator ofPropertyValuesHolder(Object target,
            View view, PropertyValuesHolder... values) {
        ObjectAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setValues(values);
        cancelOnDestroyActivity(anim);
        new FirstFrameAnimatorHelper(anim, view);
        return anim;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static ValueAnimator createCircularReveal(View view, int centerX,
            int centerY, float startRadius, float endRadius) {
        ValueAnimator anim = UiThreadCircularReveal.createCircularReveal(view, centerX,
                centerY, startRadius, endRadius);
        new FirstFrameAnimatorHelper(anim, view);
        return anim;
    }

    //move from gm02 to mtk7.0 by rongwenzhao fade in/out animation 2017-6-27 begin
    public static void fadeAlphaInOrOut(final View view, final boolean isIn, long duration, long startDelay) {
        LauncherViewPropertyAnimator scale = new LauncherViewPropertyAnimator(view);
        if (isIn) {
            view.setAlpha(0f);
        }

        //add by rongwenzhao disable touchable of WidgetLinearLayout during the animation back to the first widget list level 2017-7-7 begin
        if(!isIn && view instanceof WidgetHorizontalScrollView){
            ((WidgetLinearLayout)view.findViewById(R.id.widgets_cell_list)).setChildClickable(false);
        }
        //add by rongwenzhao disable touchable of WidgetLinearLayout during the animation back to the first widget list level 2017-7-7 end

        scale.alpha(isIn ? 1f : 0f)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator(1f));
        if(startDelay > 0) {
            scale.setStartDelay(startDelay);
        }
        scale.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //add by rongwenzhao disable touchable of WidgetLinearLayout during the animation back to the first widget list level 2017-7-7 begin
                if(!isIn && view instanceof WidgetHorizontalScrollView){
                    ((WidgetLinearLayout)view.findViewById(R.id.widgets_cell_list)).setChildClickable(true);
                }
                //add by rongwenzhao disable touchable of WidgetLinearLayout during the animation back to the first widget list level 2017-7-7 end
                if (isIn) {
                    view.setVisibility(View.VISIBLE);
                } else {
                    view.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (isIn) {
                    view.setVisibility(View.VISIBLE);
                } else {
                    view.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        scale.start();
    }
    //move from gm02 to mtk7.0 by rongwenzhao fade in/out animation 2017-6-27 end
}
