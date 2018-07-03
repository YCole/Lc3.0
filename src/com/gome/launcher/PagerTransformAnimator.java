package com.gome.launcher;


import android.view.View;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by weijiaqi on 2017/7/17.
 */

public class PagerTransformAnimator extends ValueAnimator {

    AnimatorSet mAnimatorSet = new AnimatorSet();;


    public PagerTransformAnimator(View view, AnimationType... animationTypes) {
        List<Animator> animators = new ArrayList<>();
        for (AnimationType animationType : animationTypes) {
            ValueAnimator valueAnimator = ObjectAnimator.ofFloat(view,
                    animationType.getPropertyName(),
                    animationType.getStartValue(),
                    animationType.getEndValue());


            valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
            valueAnimator.setRepeatMode(ValueAnimator.REVERSE);
            animators.add(valueAnimator);
        }
        mAnimatorSet.playTogether(animators);
    }

    public void start() {
        if (null != mAnimatorSet) {
            mAnimatorSet.setDuration(1000 * 2).start();
        }
    }

    public void end() {
        if (null != mAnimatorSet && mAnimatorSet.isRunning()) {
            mAnimatorSet.start();
            mAnimatorSet.cancel();
        }
    }



}

enum AnimationType {
    translateX("translationX",0f,0f),
    translateY("translationY",0f,0f),
    scaleX("scaleX",0f,0f),
    scaleY("scaleY",0f,0f),
    alpha("alpha",0f,0f);

    private String propertyName;
    private float startValue;
    private float endValue;

    AnimationType(String propertyName, float startValue, float endValue) {
        this.propertyName = propertyName;
        this.startValue = startValue;
        this.endValue = endValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setStartAndEndValue(float startValue,float endValue) {
         this.startValue = startValue;
        this.endValue = endValue;
    }

    public float getStartValue() {
        return startValue;
    }

    public float getEndValue() {
        return endValue;
    }

}
