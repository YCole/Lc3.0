/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;

import com.gome.launcher.settings.SettingsProvider;
import com.gome.launcher.util.Thunk;

import java.util.HashMap;

/**
 * A convenience class to update a view's visibility state after an alpha animation.
 */
class AlphaUpdateListener extends AnimatorListenerAdapter implements ValueAnimator.AnimatorUpdateListener {
    private static final float ALPHA_CUTOFF_THRESHOLD = 0.01f;

    private View mView;
    private boolean mAccessibilityEnabled;

    public AlphaUpdateListener(View v, boolean accessibilityEnabled) {
        mView = v;
        mAccessibilityEnabled = accessibilityEnabled;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator arg0) {
        updateVisibility(mView, mAccessibilityEnabled);
    }

    public static void updateVisibility(View view, boolean accessibilityEnabled) {
        // We want to avoid the extra layout pass by setting the views to GONE unless
        // accessibility is on, in which case not setting them to GONE causes a glitch.
        int invisibleState = accessibilityEnabled ? View.GONE : View.INVISIBLE;
        if (view.getAlpha() < ALPHA_CUTOFF_THRESHOLD && view.getVisibility() != invisibleState) {
            view.setVisibility(invisibleState);
        } else if (view.getAlpha() > ALPHA_CUTOFF_THRESHOLD
                && view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAnimationEnd(Animator arg0) {
        updateVisibility(mView, mAccessibilityEnabled);
    }

    @Override
    public void onAnimationStart(Animator arg0) {
        // We want the views to be visible for animation, so fade-in/out is visible
        mView.setVisibility(View.VISIBLE);
    }
}

/**
 * This interpolator emulates the rate at which the perceived scale of an object changes
 * as its distance from a camera increases. When this interpolator is applied to a scale
 * animation on a view, it evokes the sense that the object is shrinking due to moving away
 * from the camera.
 */
class ZInterpolator implements TimeInterpolator {
    private float focalLength;

    public ZInterpolator(float foc) {
        focalLength = foc;
    }

    public float getInterpolation(float input) {
        return (1.0f - focalLength / (focalLength + input)) /
                (1.0f - focalLength / (focalLength + 1.0f));
    }
}

/**
 * The exact reverse of ZInterpolator.
 */
class InverseZInterpolator implements TimeInterpolator {
    private ZInterpolator zInterpolator;
    public InverseZInterpolator(float foc) {
        zInterpolator = new ZInterpolator(foc);
    }
    public float getInterpolation(float input) {
        return 1 - zInterpolator.getInterpolation(1 - input);
    }
}

/**
 * InverseZInterpolator compounded with an ease-out.
 */
class ZoomInInterpolator implements TimeInterpolator {
    private final InverseZInterpolator inverseZInterpolator = new InverseZInterpolator(0.35f);
    private final DecelerateInterpolator decelerate = new DecelerateInterpolator(3.0f);

    public float getInterpolation(float input) {
        return decelerate.getInterpolation(inverseZInterpolator.getInterpolation(input));
    }
}

/**
 * Stores the transition states for convenience.
 */
class TransitionStates {

    // Raw states
    final boolean oldStateIsNormal;
    final boolean oldStateIsSpringLoaded;
    final boolean oldStateIsNormalHidden;
    final boolean oldStateIsOverviewHidden;
    final boolean oldStateIsOverview;

    final boolean stateIsNormal;
    final boolean stateIsSpringLoaded;
    final boolean stateIsNormalHidden;
    final boolean stateIsOverviewHidden;
    final boolean stateIsOverview;

    // Convenience members
    final boolean workspaceToAllApps;
    final boolean overviewToAllApps;
    final boolean allAppsToWorkspace;
    final boolean workspaceToOverview;
    final boolean overviewToWorkspace;

    public TransitionStates(final Workspace.State fromState, final Workspace.State toState) {
        oldStateIsNormal = (fromState == Workspace.State.NORMAL);
        oldStateIsSpringLoaded = (fromState == Workspace.State.SPRING_LOADED);
        oldStateIsNormalHidden = (fromState == Workspace.State.NORMAL_HIDDEN);
        oldStateIsOverviewHidden = (fromState == Workspace.State.OVERVIEW_HIDDEN);
        oldStateIsOverview = (fromState == Workspace.State.OVERVIEW);

        stateIsNormal = (toState == Workspace.State.NORMAL);
        stateIsSpringLoaded = (toState == Workspace.State.SPRING_LOADED);
        stateIsNormalHidden = (toState == Workspace.State.NORMAL_HIDDEN);
        stateIsOverviewHidden = (toState == Workspace.State.OVERVIEW_HIDDEN);
        stateIsOverview = (toState == Workspace.State.OVERVIEW);

        workspaceToOverview = (oldStateIsNormal && stateIsOverview);
        workspaceToAllApps = (oldStateIsNormal && stateIsNormalHidden);
        overviewToWorkspace = (oldStateIsOverview && stateIsNormal);
        overviewToAllApps = (oldStateIsOverview && stateIsOverviewHidden);
        allAppsToWorkspace = (stateIsNormalHidden && stateIsNormal);
    }
}

/**
 * Manages the animations between each of the workspace states.
 */
public class WorkspaceStateTransitionAnimation {

    public static final String TAG = "WorkspaceStateTransitionAnimation";

    public static final int SCROLL_TO_CURRENT_PAGE = -1;
    static final int BACKGROUND_FADE_OUT_DURATION = 350;

    final Launcher mLauncher;
    final @Thunk
    Workspace mWorkspace;

    @Thunk AnimatorSet mStateAnimator;
    @Thunk float[] mOldBackgroundAlphas;
    @Thunk float[] mOldAlphas;
    @Thunk float[] mNewBackgroundAlphas;
    @Thunk float[] mNewAlphas;
    @Thunk int mLastChildCount = -1;

    @Thunk float mCurrentScale;
    @Thunk float mNewScale;

    @Thunk final ZoomInInterpolator mZoomInInterpolator = new ZoomInInterpolator();

    @Thunk float mSpringLoadedShrinkFactor;
    @Thunk float mOverviewModeShrinkFactor;
    @Thunk float mWorkspaceScrimAlpha;
    @Thunk int mAllAppsTransitionTime;
    @Thunk int mOverviewTransitionTime;
    @Thunk int mOverlayTransitionTime;
    @Thunk boolean mWorkspaceFadeInAdjacentScreens;
    int mHotSeatTransitionY;
    int mPageIndicatorTransitionY;

    public WorkspaceStateTransitionAnimation(Launcher launcher, Workspace workspace) {
        mLauncher = launcher;
        mWorkspace = workspace;

        DeviceProfile grid = mLauncher.getDeviceProfile();
        Resources res = launcher.getResources();
        mAllAppsTransitionTime = res.getInteger(R.integer.config_allAppsTransitionTime);
        mOverviewTransitionTime = res.getInteger(R.integer.config_overviewTransitionTime);
        mOverlayTransitionTime = res.getInteger(R.integer.config_overlayTransitionTime);
        mSpringLoadedShrinkFactor =
                res.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100f;
        mOverviewModeShrinkFactor =
                res.getInteger(R.integer.config_workspaceOverviewShrinkPercentage) / 100f;
        mWorkspaceScrimAlpha = res.getInteger(R.integer.config_workspaceScrimAlpha) / 100f;
        mWorkspaceFadeInAdjacentScreens = grid.shouldFadeAdjacentWorkspaceScreens();
        mHotSeatTransitionY = res.getDimensionPixelSize(R.dimen.hotseat_transionY);
        mPageIndicatorTransitionY = res.getDimensionPixelSize(R.dimen.pageindicator_transionY);

    }

    public AnimatorSet getAnimationToState(Workspace.State fromState, Workspace.State toState,
                                           int toPage, boolean animated, HashMap<View, Integer> layerViews) {
        AccessibilityManager am = (AccessibilityManager)
                mLauncher.getSystemService(Context.ACCESSIBILITY_SERVICE);
        final boolean accessibilityEnabled = am.isEnabled();
        TransitionStates states = new TransitionStates(fromState, toState);
        int workspaceDuration = getAnimationDuration(states);
        animateWorkspace(states, toPage, animated, workspaceDuration, layerViews,
                accessibilityEnabled);
        animateBackgroundGradient(states, animated, BACKGROUND_FADE_OUT_DURATION);
        return mStateAnimator;
    }

    public float getFinalScale() {
        return mNewScale;
    }

    /**
     * Reinitializes the arrays that we need for the animations on each page.
     */
    private void reinitializeAnimationArrays() {
        final int childCount = mWorkspace.getChildCount();
        if (mLastChildCount == childCount) return;

        mOldBackgroundAlphas = new float[childCount];
        mOldAlphas = new float[childCount];
        mNewBackgroundAlphas = new float[childCount];
        mNewAlphas = new float[childCount];
    }

    /**
     * Returns the proper animation duration for a transition.
     */
    private int getAnimationDuration(TransitionStates states) {
        if (states.workspaceToAllApps || states.overviewToAllApps) {
            return mAllAppsTransitionTime;
        } else if (states.workspaceToOverview || states.overviewToWorkspace) {
            return mOverviewTransitionTime;
        } else {
            return mOverlayTransitionTime;
        }
    }

    /**
     * linhai note
     * From WorkSpace to OverView mode or reverse, control the creation, execution, display of all animations
     *
     * Starts a transition animation for the workspace.
     */
    private void animateWorkspace(final TransitionStates states, int toPage, final boolean animated,
                                  final int duration, final HashMap<View, Integer> layerViews,
                                  final boolean accessibilityEnabled) {
        //add by rongwenzhao workspace state from overview to springloaded state 2017-6-27 begin
        if (states.oldStateIsOverview && states.stateIsSpringLoaded) {
            // Reinitialize animation arrays for the current workspace state
            reinitializeAnimationArrays();

            // Cancel existing workspace animations and create a new animator set if requested
            cancelAnimation();
            if (animated) {
                mStateAnimator = LauncherAnimUtils.createAnimatorSet();
            }
            float finalBackgroundAlpha = (states.stateIsSpringLoaded || states.stateIsOverview) ?
                    Workspace.MAX_CELLLAYOUT_BACKGROUND_ALPHA : 0f;

            final int childCount = mWorkspace.getChildCount();
            final int customPageCount = mWorkspace.numCustomPages();

            mNewScale = 1.0f;

            if (states.oldStateIsOverview || states.stateIsOverview) {
                mWorkspace.disableFreeScroll();
            }

            if (!states.stateIsNormal) {
                if (states.stateIsSpringLoaded) {
                    mNewScale = mSpringLoadedShrinkFactor;
                } else if (states.stateIsOverview || states.stateIsOverviewHidden) {
                    mNewScale = mOverviewModeShrinkFactor;
                }
            }
            if (toPage == SCROLL_TO_CURRENT_PAGE) {
		    	//update linhai without CustomPage to try start on 2017/8/2
                toPage = mWorkspace.getPageNearestToCenterOfScreenNoCustomPage();
				//update linhai without CustomPage to try end on 2017/8/2
            }
            if (mLauncher != null && mLauncher.needCustomContentToLeft()) {
                mWorkspace.setCurrentPage(toPage);
            } else {
                //linhai note
                //Long press CellLayout blank, enter the OverView mode,
                // in order to show both sides of the CellLayout
                mWorkspace.snapToPage(toPage, duration, mZoomInInterpolator);
            }

            for (int i = 0; i < childCount; i++) {
                if (i == childCount - 1) {
                    final CellLayout cl = (CellLayout) mWorkspace.getChildAt(i);
                    //boolean isCurrentPage = (i == toPage);
                    float initialAlpha = cl.getShortcutsAndWidgets().getAlpha();
                    float finalAlpha;
                    if (states.stateIsNormalHidden || states.stateIsOverviewHidden) {
                        finalAlpha = 0f;
                    } else if (states.stateIsNormal && mWorkspaceFadeInAdjacentScreens) {
                        finalAlpha = (i == toPage || i < customPageCount) ? 1f : 0f;
                    } else {
                        finalAlpha = 1f;
                    }

                    mOldAlphas[i] = initialAlpha;
                    mNewAlphas[i] = finalAlpha;
                    if (animated) {
                        mOldBackgroundAlphas[i] = cl.getBackgroundAlpha();
                        mNewBackgroundAlphas[i] = finalBackgroundAlpha;
                    } else {
                        cl.setBackgroundAlpha(finalBackgroundAlpha);
                        cl.setShortcutAndWidgetAlpha(finalAlpha);
                    }
                }
            }

            if (animated) {
                for (int index = 0; index < childCount; index++) {
                    if (index == childCount - 1) {
                        final int i = index;
                        final CellLayout cl = (CellLayout) mWorkspace.getChildAt(i);
                        float currentAlpha = cl.getShortcutsAndWidgets().getAlpha();
                        if (mOldAlphas[i] == 0 && mNewAlphas[i] == 0) {
                            cl.setBackgroundAlpha(mNewBackgroundAlphas[i]);
                            cl.setShortcutAndWidgetAlpha(mNewAlphas[i]);
                        } else {
                            if (layerViews != null) {
                                layerViews.put(cl, LauncherStateTransitionAnimation.BUILD_LAYER);
                            }
                            if (mOldAlphas[i] != mNewAlphas[i] || currentAlpha != mNewAlphas[i]) {
                                LauncherViewPropertyAnimator alphaAnim =
                                        new LauncherViewPropertyAnimator(cl.getShortcutsAndWidgets());
                                alphaAnim.alpha(mNewAlphas[i])
                                        .setDuration(duration)
                                        .setInterpolator(mZoomInInterpolator);
                                mStateAnimator.play(alphaAnim);
                            }
                            if (mOldBackgroundAlphas[i] != 0 ||
                                    mNewBackgroundAlphas[i] != 0) {
                                ValueAnimator bgAnim = ObjectAnimator.ofFloat(cl, "backgroundAlpha",
                                        mOldBackgroundAlphas[i], mNewBackgroundAlphas[i]);
                                LauncherAnimUtils.ofFloat(cl, 0f, 1f);
                                bgAnim.setInterpolator(mZoomInInterpolator);
                                bgAnim.setDuration(duration);
                                mStateAnimator.play(bgAnim);
                            }
                        }
                    }
                }
            }
            //add by rongwenzhao workspace state from overview to springloaded state 2017-6-23 2017-6-27 end
        } else {

            // Reinitialize animation arrays for the current workspace state
            reinitializeAnimationArrays();

            // Cancel existing workspace animations and create a new animator set if requested
            cancelAnimation();
            if (animated) {
                mStateAnimator = LauncherAnimUtils.createAnimatorSet();
            }
            //duration = 3000;
            // Update the workspace state
            // linhai from 0.5f to 1.0f
            float finalBackgroundAlpha = (states.stateIsSpringLoaded || states.stateIsOverview) ?
                    Workspace.MAX_CELLLAYOUT_BACKGROUND_ALPHA : 0f;
            float finalHotseatAndPageIndicatorAlpha = (states.stateIsNormal || states.stateIsSpringLoaded) ?
                    1f : 0f;
            float finalOverviewPanelAlpha = states.stateIsOverview ? 1f : 0f;
            float finalWorkspaceTranslationY = states.stateIsOverview || states.stateIsOverviewHidden ?
                    mWorkspace.getOverviewModeTranslationY() : 0;

            final int childCount = mWorkspace.getChildCount();
            final int customPageCount = mWorkspace.numCustomPages();

            mNewScale = 1.0f;

            if (states.oldStateIsOverview || states.stateIsOverview) {
                mWorkspace.disableFreeScroll();
            }

            if (!states.stateIsNormal) {
                if (states.stateIsSpringLoaded) {
                    mNewScale = mSpringLoadedShrinkFactor;
                } else if (states.stateIsOverview || states.stateIsOverviewHidden) {
                    mNewScale = mOverviewModeShrinkFactor;
                }
            }
            if (toPage == SCROLL_TO_CURRENT_PAGE) {

			    //update linhai without CustomPage to try start on 2017/8/2
                toPage = mWorkspace.getPageNearestToCenterOfScreenNoCustomPage();
				//update linhai without CustomPage to try end on 2017/8/2

            }
            //linhai add for when enter into overview mode ,to remove left screen start 2017/6/19
            if (mLauncher != null && mLauncher.needCustomContentToLeft()) {
                if (states.workspaceToOverview) {
//                    mWorkspace.resetPageRunable();
                    mWorkspace.setCurrentPage(toPage);
                } else {
                    //退出编辑模式时，使用immediate==true,快速结束滚动，保证编辑模式缩放动画完成后，添加负一屏的时候，pagedview滚动状态为finish
                    mWorkspace.snapToPage(toPage, duration, true, mZoomInInterpolator);
                }
            } else {
                //linhai note
                //Long press CellLayout blank, enter the OverView mode,
                // in order to show both sides of the CellLayout
                mWorkspace.snapToPage(toPage, duration, mZoomInInterpolator);
            }
            //end linhai 2017/6/19

            //move and mod by rongwenzhao from 6.0 to mtk7.0 2017-7-3 begin
            //zhaosuzhou add for listener Shake the phone
            if(mLauncher != null && states.stateIsOverview){
                if(SettingsProvider.getBooleanCustomDefault(mLauncher,SettingsProvider.SETTINGS_UI_SHAKE_ARRANGE_ICONS,false)
                        && !ShakeListener.isRegister){
                    mLauncher.startShakeListening();
                }
            } else if(mLauncher != null && !states.stateIsOverview){
                mLauncher.stopShakeListening();
            }
            //move and mod by rongwenzhao from 6.0 to mtk7.0 2017-7-3 end

            for (int i = 0; i < childCount; i++) {
                final CellLayout cl = (CellLayout) mWorkspace.getChildAt(i);
                boolean isCurrentPage = (i == toPage);
                float initialAlpha = cl.getShortcutsAndWidgets().getAlpha();
                float finalAlpha;
                if (states.stateIsNormalHidden || states.stateIsOverviewHidden) {
                    finalAlpha = 0f;
                } else if (states.stateIsNormal && mWorkspaceFadeInAdjacentScreens) {
                    finalAlpha = (i == toPage || i < customPageCount) ? 1f : 0f;
                } else {
                    finalAlpha = 1f;
                }

                // If we are animating to/from the small state, then hide the side pages and fade the
                // current page in
                if (!mWorkspace.isSwitchingState()) {
                    if (states.workspaceToAllApps || states.allAppsToWorkspace) {
                        if (states.allAppsToWorkspace && isCurrentPage) {
                            initialAlpha = 0f;
                        } else if (!isCurrentPage) {
                            initialAlpha = finalAlpha = 0f;
                        }
                        cl.setShortcutAndWidgetAlpha(initialAlpha);
                    }
                }

                mOldAlphas[i] = initialAlpha;
                mNewAlphas[i] = finalAlpha;
                if (animated) {
                    mOldBackgroundAlphas[i] = cl.getBackgroundAlpha();
                    mNewBackgroundAlphas[i] = finalBackgroundAlpha;
                } else {
                    cl.setBackgroundAlpha(finalBackgroundAlpha);
                    cl.setShortcutAndWidgetAlpha(finalAlpha);
                }
            }

            final ViewGroup overviewPanel = mLauncher.getOverviewPanel();
            final View hotseat = mLauncher.getHotseat();
            final View pageIndicator = mWorkspace.getPageIndicator();
            //linhai add 获取hotseat、pageIndicator、overviewPanel参数 start  2017/6/19
            final float screenHeight = Utilities.getScreenHeight(mLauncher);

            int[] hotseatps = new int[2];
            hotseat.getLocationOnScreen(hotseatps);

            int[] pageIndicatorps = new int[2];
            pageIndicator.getLocationOnScreen(pageIndicatorps);

            int[] overviewPanelps = new int[2];
            overviewPanel.getLocationOnScreen(overviewPanelps);


            //final float hotseatTransionY = states.stateIsOverview
            //        ?screenHeight-hotseatps[1]:-(screenHeight-hotseatps[1]);
            final float hotseatTransionY = states.stateIsOverview
                    ? mHotSeatTransitionY : 0;
            final float overviewPanelTransionY = states.stateIsOverview ? 0 : (screenHeight - overviewPanelps[1]);

            // Control the distance of the indicator to move up in OverView mode

            final float pageIndicatorTransionY = states.stateIsOverview ? -mPageIndicatorTransitionY : 0;
            float finalPageIndicatorAlpha = (states.stateIsNormal || states.stateIsSpringLoaded || states.stateIsOverview) ?
                    1f : 0f;
            if (states.stateIsOverview) {
                if (states.workspaceToOverview) {
                    overviewPanel.setTranslationY(overviewPanelTransionY);
                } else {
                    overviewPanel.setTranslationY(0);
                }
            }
            // 设置hotseat、PageIndicator、overviewPanel 动画 start 2017/6/19
            if (animated) {
                LauncherViewPropertyAnimator scale = new LauncherViewPropertyAnimator(mWorkspace);
                scale.scaleX(mNewScale)
                        .scaleY(mNewScale)
                        .translationY(finalWorkspaceTranslationY)
                        .setDuration(duration)
                        .setInterpolator(mZoomInInterpolator);
                mStateAnimator.play(scale);


                LauncherViewPropertyAnimator hotseatAlpha = new LauncherViewPropertyAnimator(hotseat)
                        .alpha(finalHotseatAndPageIndicatorAlpha);
                LauncherViewPropertyAnimator pageIndicatorScales = new LauncherViewPropertyAnimator(pageIndicator);
                if (states.stateIsOverview || states.stateIsNormal) {
                    pageIndicatorScales.translationY(pageIndicatorTransionY);
                    hotseatAlpha.translationY(hotseatTransionY);
                }
                pageIndicatorScales.alpha(finalPageIndicatorAlpha);
                pageIndicatorScales.setDuration(duration);
                pageIndicatorScales.setInterpolator(mZoomInInterpolator);

                hotseatAlpha.addListener(new AlphaUpdateListener(hotseat, accessibilityEnabled));
                hotseatAlpha.setInterpolator(mZoomInInterpolator);
                hotseatAlpha.setDuration(duration);


                for (int index = 0; index < childCount; index++) {
                    final int i = index;
                    final CellLayout cl = (CellLayout) mWorkspace.getChildAt(i);
                    float currentAlpha = cl.getShortcutsAndWidgets().getAlpha();
                    if (mOldAlphas[i] == 0 && mNewAlphas[i] == 0) {
                        cl.setBackgroundAlpha(mNewBackgroundAlphas[i]);
                        cl.setShortcutAndWidgetAlpha(mNewAlphas[i]);
                    } else {
                        if (layerViews != null) {
                            layerViews.put(cl, LauncherStateTransitionAnimation.BUILD_LAYER);
                        }
                        if (mOldAlphas[i] != mNewAlphas[i] || currentAlpha != mNewAlphas[i]) {
                            LauncherViewPropertyAnimator alphaAnim =
                                    new LauncherViewPropertyAnimator(cl.getShortcutsAndWidgets());
                            alphaAnim.alpha(mNewAlphas[i])
                                    .setDuration(duration)
                                    .setInterpolator(mZoomInInterpolator);
                            mStateAnimator.play(alphaAnim);
                        }
                        if (mOldBackgroundAlphas[i] != 0 ||
                                mNewBackgroundAlphas[i] != 0) {
                            ValueAnimator bgAnim = ObjectAnimator.ofFloat(cl, "backgroundAlpha",
                                    mOldBackgroundAlphas[i], mNewBackgroundAlphas[i]);
                            LauncherAnimUtils.ofFloat(cl, 0f, 1f);
                            bgAnim.setInterpolator(mZoomInInterpolator);
                            bgAnim.setDuration(duration);
                            mStateAnimator.play(bgAnim);
                        }
                    }
                }

                LauncherViewPropertyAnimator overviewPanelAlpha = new LauncherViewPropertyAnimator(overviewPanel)
                        .translationY(overviewPanelTransionY).alpha(finalOverviewPanelAlpha);
                overviewPanelAlpha.addListener(new AlphaUpdateListener(overviewPanel,
                        accessibilityEnabled));

                // For animation optimations, we may need to provide the Launcher transition
                // with a set of views on which to force build layers in certain scenarios.
                hotseat.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                overviewPanel.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                if (layerViews != null) {
                    // If layerViews is not null, we add these views, and indicate that
                    // the caller can manage layer state.
                    layerViews.put(hotseat, LauncherStateTransitionAnimation.BUILD_AND_SET_LAYER);
                    layerViews.put(overviewPanel, LauncherStateTransitionAnimation.BUILD_AND_SET_LAYER);
                } else {
                    // Otherwise let the animator handle layer management.
                    hotseatAlpha.withLayer();
                    overviewPanelAlpha.withLayer();
                }

                //if (states.workspaceToOverview) {
                overviewPanelAlpha.setInterpolator(mZoomInInterpolator);

                //} else if (states.overviewToWorkspace) {
                //    hotseatAlpha.setInterpolator(null);
                //    overviewPanelAlpha.setInterpolator(null);
                //}

                overviewPanelAlpha.setDuration(duration);
                //Added by huanghaihao in 2017-6-26 for screen setting start
                for (int index = 0; index < childCount; index++) {
                    CellLayout cl = (CellLayout) mWorkspace.getChildAt(index);
                    if (cl != null && mWorkspace.getIdForScreen(cl) != Workspace.CUSTOM_CONTENT_SCREEN_ID) {
                        ViewGroup pageSetting = cl.getPageSetting();
                        LauncherViewPropertyAnimator pageSettingAlpha =
                                new LauncherViewPropertyAnimator(pageSetting).alpha(finalOverviewPanelAlpha);
                        pageSettingAlpha.addListener(new AlphaUpdateListener(pageSetting,
                                accessibilityEnabled));
                        pageSetting.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                        if (layerViews != null) {
                            layerViews.put(pageSetting, LauncherStateTransitionAnimation.BUILD_AND_SET_LAYER);
                        } else {
                            pageSettingAlpha.withLayer();
                        }
                        if (states.workspaceToOverview) {
                            pageSettingAlpha.setInterpolator(null);
                        } else if (states.overviewToWorkspace) {
                            pageSettingAlpha.setInterpolator(new DecelerateInterpolator(2));
                        }
                        pageSettingAlpha.setDuration(duration);
                        mStateAnimator.play(pageSettingAlpha);
                    }
                }
                //Added by huanghaihao in 2017-6-26 for screen setting end
                mStateAnimator.play(overviewPanelAlpha);
                mStateAnimator.play(hotseatAlpha);
                mStateAnimator.play(pageIndicatorScales);
                mStateAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mStateAnimator = null;
                        PagedView.TransitionEffect.setIsEnterToOverviewMode(false);
                        //add linhai 动画执行完 负一平显示 start on 2017/8/2
//                        if (!states.stateIsOverview)
//                        {
//                            CustomPageView customPageView = CustomPageManager.getInstance(null).getCustomPageView();
//                            if (customPageView != null)
//                            {
//                                customPageView.setMapVisable();
//                            }
//
//                        }
                        //add linhai end on 2017/8/2

                        if (accessibilityEnabled && overviewPanel.getVisibility() == View.VISIBLE) {
                            overviewPanel.getChildAt(0).performAccessibilityAction(
                                    AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
                        }
                    }
                });
                // linhai end 2017/6/19
            } else {
                overviewPanel.setAlpha(finalOverviewPanelAlpha);
                overviewPanel.setTranslationY(overviewPanelTransionY);
                AlphaUpdateListener.updateVisibility(overviewPanel, accessibilityEnabled);
                hotseat.setAlpha(finalHotseatAndPageIndicatorAlpha);

                AlphaUpdateListener.updateVisibility(hotseat, accessibilityEnabled);
                if (pageIndicator != null) {
                    pageIndicator.setAlpha(finalHotseatAndPageIndicatorAlpha);
                    AlphaUpdateListener.updateVisibility(pageIndicator, accessibilityEnabled);
                }
                if (states.stateIsOverview || states.stateIsNormal) {
                    hotseat.setTranslationY(hotseatTransionY);
                    if (pageIndicator != null) {
                        pageIndicator.setAlpha(1f);
                        AlphaUpdateListener.updateVisibility(pageIndicator, accessibilityEnabled);
                        pageIndicator.setTranslationY(pageIndicatorTransionY);
                    }
                }
                //Added by huanghaihao in 2017-6-26 for screen setting start
                for (int i = 0; i < mWorkspace.getPageCount(); i++) {
                    CellLayout cl = (CellLayout) mWorkspace.getChildAt(i);
                    if (cl != null && mWorkspace.getIdForScreen(cl) != Workspace.CUSTOM_CONTENT_SCREEN_ID) {
                        ViewGroup viewGroup = cl.getPageSetting();
                        viewGroup.setAlpha(finalOverviewPanelAlpha);
                        AlphaUpdateListener.updateVisibility(viewGroup, accessibilityEnabled);
                    }
                }
                //Added by huanghaihao in 2017-6-26 for screen setting end
                mWorkspace.updateCustomContentVisibility();
                mWorkspace.setScaleX(mNewScale);
                mWorkspace.setScaleY(mNewScale);
                mWorkspace.setTranslationY(finalWorkspaceTranslationY);

                if (accessibilityEnabled && overviewPanel.getVisibility() == View.VISIBLE) {
                    overviewPanel.getChildAt(0).performAccessibilityAction(
                            AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
                }
            }
        }
    }

    /**
     * Animates the background scrim. Add to the state animator to prevent jankiness.
     *
     * @param states the current and final workspace states
     * @param animated whether or not to set the background alpha immediately
     * @duration duration of the animation
     */
    private void animateBackgroundGradient(TransitionStates states,
                                           boolean animated, int duration) {

        final DragLayer dragLayer = mLauncher.getDragLayer();
        final float startAlpha = dragLayer.getBackgroundAlpha();
        float finalAlpha = states.stateIsNormal ? 0 : mWorkspaceScrimAlpha;

        if (finalAlpha != startAlpha) {
            if (animated) {
                // These properties refer to the background protection gradient used for AllApps
                // and Widget tray.
                ValueAnimator bgFadeOutAnimation =
                        LauncherAnimUtils.ofFloat(mWorkspace, startAlpha, finalAlpha);
                bgFadeOutAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        dragLayer.setBackgroundAlpha(
                                ((Float)animation.getAnimatedValue()).floatValue());
                    }
                });
                bgFadeOutAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
                bgFadeOutAnimation.setDuration(duration);
                mStateAnimator.play(bgFadeOutAnimation);
            } else {
                dragLayer.setBackgroundAlpha(finalAlpha);
            }
        }
    }

    /**
     * Cancels the current animation.
     */
    private void cancelAnimation() {
        if (mStateAnimator != null) {
            mStateAnimator.setDuration(0);
            mStateAnimator.cancel();
        }
        mStateAnimator = null;
    }
}
