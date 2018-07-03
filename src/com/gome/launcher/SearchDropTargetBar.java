/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.gome.launcher.util.Thunk;

/*
 * Ths bar will manage the transition between the QSB search bar and the delete drop
 * targets so that each of the individual IconDropTargets don't have to.
 */
public class SearchDropTargetBar extends FrameLayout implements DragController.DragListener {

    private static final TimeInterpolator MOVE_DOWN_INTERPOLATOR = new DecelerateInterpolator(0.6f);
    private static final TimeInterpolator MOVE_UP_INTERPOLATOR = new DecelerateInterpolator(1.5f);
    private static final TimeInterpolator DEFAULT_INTERPOLATOR = new AccelerateInterpolator();

    /** The different states that the search bar space can be in. */
    public enum State {
        INVISIBLE             (0f, 0f, 0f),
        INVISIBLE_TRANSLATED  (0f, 0f, -1f),
        SEARCH_BAR            (1f, 0f, 0f),
        DROP_TARGET           (0f, 1f, 0f);

        private final float mSearchBarAlpha;
        private final float mDropTargetBarAlpha;
        private final float mTranslation;

        State(float sbAlpha, float dtbAlpha, float translation) {
            mSearchBarAlpha = sbAlpha;
            mDropTargetBarAlpha = dtbAlpha;
            mTranslation = translation;
        }

    }

    public static int DEFAULT_DRAG_FADE_DURATION = 175;

    public static int DEFAULT_TOTAL_DURATION = 1000;

    private AnimatorSet mCurrentAnimation;

    private State mState = State.SEARCH_BAR;
    private State toastState = State.SEARCH_BAR;
    @Thunk View mQSB;
    @Thunk View mDropTargetBar;
    private boolean mDeferOnDragEnd = false;
    @Thunk boolean mAccessibilityEnabled = false;

    // Drop targets
    //add by liuning for multi apps move on 2017/7/18 start
    //private ButtonDropTarget mInfoDropTarget;
    private ButtonDropTarget mMoveDropTarget;
	private Launcher mLauncher;
    //add by liuning for multi apps move on 2017/7/18 end
    private ButtonDropTarget mDeleteDropTarget;
    private ButtonDropTarget mUninstallDropTarget;

    private LinearLayout toastLinearLayout;

    private AnimatorSet toastAnimation;

    public SearchDropTargetBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    private Handler handler = new Handler();

    public SearchDropTargetBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setup(Launcher launcher, DragController dragController) {
        dragController.addDragListener(this);
        dragController.setFlingToDeleteDropTarget(mDeleteDropTarget);

        dragController.addDragListener(mMoveDropTarget);
        dragController.addDragListener(mDeleteDropTarget);
        dragController.addDragListener(mUninstallDropTarget);

        dragController.addDropTarget(mMoveDropTarget);
        dragController.addDropTarget(mDeleteDropTarget);
        dragController.addDropTarget(mUninstallDropTarget);

        mMoveDropTarget.setLauncher(launcher);
        mDeleteDropTarget.setLauncher(launcher);
        mUninstallDropTarget.setLauncher(launcher);
        mLauncher = launcher;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Get the individual components
        mDropTargetBar = findViewById(R.id.drag_target_bar);
        mMoveDropTarget = (ButtonDropTarget) mDropTargetBar.findViewById(R.id.move_target_text);
        mDeleteDropTarget = (ButtonDropTarget) mDropTargetBar.findViewById(R.id.delete_target_text);
        mUninstallDropTarget = (ButtonDropTarget) mDropTargetBar.findViewById(R.id.uninstall_target_text);
        toastLinearLayout = (LinearLayout) findViewById(R.id.toast_text_layout);
        mMoveDropTarget.setSearchDropTargetBar(this);
        mDeleteDropTarget.setSearchDropTargetBar(this);
        mUninstallDropTarget.setSearchDropTargetBar(this);
        // Create the various fade animations
        mDropTargetBar.setAlpha(0f);
        toastLinearLayout.setAlpha(0f);
        AlphaUpdateListener.updateVisibility(mDropTargetBar, mAccessibilityEnabled);
    }

    public void setQsbSearchBar(View qsb) {
        mQSB = qsb;
    }

    /**
     * Animates the current search bar state to a new state.  If the {@param duration} is 0, then
     * the state is applied immediately.
     */
    public void animateToState(State newState, int duration) {
        animateToState(newState, duration, null);
    }

    /**
     * add by linhai show no space view show by animation start 2017/10/2017
     * @param newState
     * @param duration
     */
    public void animateToStateForToastLy(State newState, int duration) {
        if (toastState != newState) {
            toastState = newState;
            if (toastAnimation != null) {
                toastAnimation.cancel();
                toastAnimation = null;
            }
            toastAnimation = new AnimatorSet();
            toastAnimation.setDuration(duration);
            AccessibilityManager am = (AccessibilityManager)
                    getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
            mAccessibilityEnabled = am.isEnabled();

            if (duration > 0) {
                toastAnimation = new AnimatorSet();
                toastAnimation.setDuration(duration);
                animateAlpha(toastLinearLayout, toastState.mDropTargetBarAlpha, toastAnimation, DEFAULT_INTERPOLATOR);
            } else {
                toastLinearLayout.setAlpha(toastState.mDropTargetBarAlpha);
                AlphaUpdateListener.updateVisibility(toastLinearLayout, mAccessibilityEnabled);
            }
            if (duration > 0) {
                toastAnimation.start();
            }

        }
    }

    public void animateToState(State newState, int duration, AnimatorSet animation) {
        if (mState != newState) {
            mState = newState;

            // Update the accessibility state
            AccessibilityManager am = (AccessibilityManager)
                    getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
            mAccessibilityEnabled = am.isEnabled();

            if (mCurrentAnimation != null) {
                mCurrentAnimation.cancel();
                mCurrentAnimation = null;
            }
            mCurrentAnimation = null;

            if (duration > 0) {
                mCurrentAnimation = new AnimatorSet();
                mCurrentAnimation.setDuration(duration);

                animateAlpha(mDropTargetBar, mState.mDropTargetBarAlpha, mCurrentAnimation, DEFAULT_INTERPOLATOR);
            } else {
                mDropTargetBar.setAlpha(mState.mDropTargetBarAlpha);
                AlphaUpdateListener.updateVisibility(mDropTargetBar, mAccessibilityEnabled);
            }

            if (mQSB != null) {
                boolean isVertical = ((Launcher) getContext()).getDeviceProfile()
                        .isVerticalBarLayout();
                float translation = isVertical ? 0 : mState.mTranslation * getMeasuredHeight();

                if (duration > 0) {
                    int translationChange = Float.compare(mQSB.getTranslationY(), translation);

                    animateAlpha(mQSB, mState.mSearchBarAlpha, mCurrentAnimation,
                            translationChange == 0 ? DEFAULT_INTERPOLATOR
                                    : (translationChange < 0 ? MOVE_DOWN_INTERPOLATOR
                                    : MOVE_UP_INTERPOLATOR));

                    if (translationChange != 0) {
                        mCurrentAnimation.play(
                                ObjectAnimator.ofFloat(mQSB, View.TRANSLATION_Y, translation));
                    }
                } else {
                    mQSB.setTranslationY(translation);
                    mQSB.setAlpha(mState.mSearchBarAlpha);
                    AlphaUpdateListener.updateVisibility(mQSB, mAccessibilityEnabled);
                }
            }

            // Start the final animation
            if (duration > 0) {
                if (animation != null) {
                    animation.play(mCurrentAnimation);
                } else {
                    mCurrentAnimation.start();
                }
            }
        }
    }

    private void animateAlpha(View v, float alpha, AnimatorSet animatorSet, TimeInterpolator interpolator) {
        if (Float.compare(v.getAlpha(), alpha) != 0) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(v, View.ALPHA, alpha);
            anim.setInterpolator(interpolator);
            anim.addListener(new ViewVisiblilyUpdateHandler(v));
            animatorSet.play(anim);
        }
    }


    /*
     * DragController.DragListener implementation
     */
    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        //modify by liuning for app manage mode on 2017/7/18 start
        if (!mLauncher.getWorkspace().isInAppManageMode()) {
            if (toastLinearLayout != null && toastLinearLayout.isShown()) {
                animateToStateForToastLy(State.INVISIBLE, 0);
            }
            animateToState(State.DROP_TARGET, DEFAULT_DRAG_FADE_DURATION);
        }
        //modify by liuning for app manage mode on 2017/7/18 end
    }

    /**
     * add by linhai show no shace view start 2017/10/17
     */
    public void toastShow() {
        handler.removeCallbacks(hideAction);
        animateToStateForToastLy(State.DROP_TARGET, DEFAULT_DRAG_FADE_DURATION);
        handler.postDelayed(hideAction, DEFAULT_TOTAL_DURATION);
    }

    private Runnable hideAction = new Runnable() {
        @Override
        public void run() {
            if (toastLinearLayout != null && toastLinearLayout.isShown()) {
                animateToStateForToastLy(State.SEARCH_BAR, DEFAULT_DRAG_FADE_DURATION);
            }
        }
    };

    /**
     * This is called to defer hiding the delete drop target until the drop animation has completed,
     * instead of hiding immediately when the drag has ended.
     */
    public void deferOnDragEnd() {
        mDeferOnDragEnd = true;
    }

    @Override
    public void onDragEnd() {
        if (!mDeferOnDragEnd) {
            animateToState(State.SEARCH_BAR, DEFAULT_DRAG_FADE_DURATION);
        } else {
            mDeferOnDragEnd = false;
        }
    }

    /**
     * @return the bounds of the QSB search bar.
     */
    public Rect getSearchBarBounds() {
        if (mQSB != null) {
            final int[] pos = new int[2];
            mQSB.getLocationOnScreen(pos);

            final Rect rect = new Rect();
            rect.left = pos[0];
            rect.top = pos[1];
            rect.right = pos[0] + mQSB.getWidth();
            rect.bottom = pos[1] + mQSB.getHeight();
            return rect;
        } else {
            return null;
        }
    }

    public void enableAccessibleDrag(boolean enable) {
        if (mQSB != null) {
            mQSB.setVisibility(enable ? View.GONE : View.VISIBLE);
        }
        mMoveDropTarget.enableAccessibleDrag(enable);
        mDeleteDropTarget.enableAccessibleDrag(enable);
        mUninstallDropTarget.enableAccessibleDrag(enable);
    }

    private class ViewVisiblilyUpdateHandler extends AnimatorListenerAdapter {
        private final View mView;

        ViewVisiblilyUpdateHandler(View v) {
            mView = v;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            // Ensure that the view is visible for the animation
            mView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animator animation){
            AlphaUpdateListener.updateVisibility(mView, mAccessibilityEnabled);
        }

    }
}
