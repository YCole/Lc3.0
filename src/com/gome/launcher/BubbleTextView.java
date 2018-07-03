/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.TextView;

import com.gome.drawbitmaplib.BitmapInfo;
import com.gome.drawbitmaplib.BitmapStateFactory;
import com.gome.drawbitmaplib.DecodeBitmap;
import com.gome.drawbitmaplib.IDownLoadCallback;
import com.gome.drawbitmaplib.animation.AnimatorListener;
import com.gome.drawbitmaplib.animation.DownloadAnimation;
import com.gome.drawbitmaplib.bitmapState.BaseBitmapState;
import com.gome.launcher.model.PackageItemInfo;
import com.gome.launcher.unread.BadgeUnreadLoader;
import com.gome.launcher.util.DisplayMetricsUtils;

import java.text.NumberFormat;


/**
 * TextView that draws a bubble behind the text. We cannot use a LineBackgroundSpan
 * because we want to make the bubble taller than the text and TextView's clip is
 * too aggressive.
 */
public class BubbleTextView extends TextView
        implements BaseRecyclerViewFastScrollBar.FastScrollFocusableView, IDownLoadCallback {

    private static SparseArray<Theme> sPreloaderThemes = new SparseArray<Theme>(2);

    private static final float SHADOW_LARGE_RADIUS = 4.0f;
    private static final float SHADOW_SMALL_RADIUS = 1.75f;
    private static final float SHADOW_Y_OFFSET = 2.0f;
    private static final int SHADOW_LARGE_COLOUR = 0xDD000000;
    private static final int SHADOW_SMALL_COLOUR = 0xCC000000;

    private static final int DISPLAY_WORKSPACE = 0;
    private static final int DISPLAY_ALL_APPS = 1;
    //add by liuning for icon size in the moveRestContainer on 2017/7/18 start
    private static final int DISPLAY_MOVE_CONTAINER = 3;
    //add by liuning for icon size in the moveRestContainer on 2017/7/18 end

    private final Launcher mLauncher;
    private Drawable mIcon;
    private final Drawable mBackground;
    private final CheckLongPressHelper mLongPressHelper;
    private final HolographicOutlineHelper mOutlineHelper;
    private final StylusEventHelper mStylusEventHelper;

    private boolean mBackgroundSizeChanged;

    private Bitmap mPressedBackground;

    private float mSlop;

    private final boolean mDeferShadowGenerationOnTouch;
    private final boolean mCustomShadowsEnabled;
    private final boolean mLayoutHorizontal;
    private final int mIconSize;
    private int mTextColor;

    private boolean mStayPressed;
    private boolean mIgnorePressedStateChange;
    private boolean mDisableRelayout = false;

    private IconCache.IconLoadRequest mIconLoadRequest;
    //add for dynamic clock by louhan 20170713 start
    public IconScript mScript;
    //add for dynamic clock by louhan 20170713 end
    //add for dynamic download icon by louhan & weijiaqi 20170804
    static final String TAG = "BubbleTextView";
    public Handler mHandler = new Handler();
    private Rect mDrawableTopRect = new Rect();
    //add for dynamic download icon by louhan & weijiaqi 20170804
    public BubbleTextView(Context context) {
        this(context, null, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLauncher = (Launcher) context;
        DeviceProfile grid = mLauncher.getDeviceProfile();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.BubbleTextView, defStyle, 0);
        mCustomShadowsEnabled = a.getBoolean(R.styleable.BubbleTextView_customShadows, true);
        mLayoutHorizontal = a.getBoolean(R.styleable.BubbleTextView_layoutHorizontal, false);
        mDeferShadowGenerationOnTouch =
                a.getBoolean(R.styleable.BubbleTextView_deferShadowGeneration, false);

        int display = a.getInteger(R.styleable.BubbleTextView_iconDisplay, DISPLAY_WORKSPACE);
        int defaultIconSize = grid.iconSizePx;
        if (display == DISPLAY_WORKSPACE) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, grid.iconTextSizePx);
        } else if (display == DISPLAY_ALL_APPS) {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, grid.allAppsIconTextSizeSp);
            defaultIconSize = grid.allAppsIconSizePx;
        }
        //add by liuning for icon size in the moveRestContainer on 2017/7/18 start
        else if (display == DISPLAY_MOVE_CONTAINER) {
            defaultIconSize = grid.moveIconSizePx;
        }
        //add by liuning for icon size in the moveRestContainer on 2017/7/18 end

        mIconSize = a.getDimensionPixelSize(R.styleable.BubbleTextView_iconSizeOverride,
                defaultIconSize);

        a.recycle();

        if (mCustomShadowsEnabled) {
            // Draw the background itself as the parent is drawn twice.
            mBackground = getBackground();
            setBackground(null);
        } else {
            mBackground = null;
        }

        mLongPressHelper = new CheckLongPressHelper(this);
        mStylusEventHelper = new StylusEventHelper(this);

        mOutlineHelper = HolographicOutlineHelper.obtain(getContext());
        if (mCustomShadowsEnabled) {
            setShadowLayer(SHADOW_LARGE_RADIUS, 0.0f, SHADOW_Y_OFFSET, SHADOW_LARGE_COLOUR);
        }

        setAccessibilityDelegate(LauncherAppState.getInstance().getAccessibilityDelegate());
        //add for dynamic download icon by louhan & weijiaqi 20170804
        mBitmapInfo = new BitmapInfo(new BitmapInfo.StatusChangeListener() {
            @Override
            public void onChangeListener(int status) {
                switch (status) {
                    case BitmapInfo.ICON_READY:
                    case BitmapInfo.PRE_DOWNLOAD:
                    case BitmapInfo.DOWNLOADING:
                        setDownLoadStatusText(R.string.app_loading);
                        break;

                    case BitmapInfo.DOWNLOAD_PAUSE:
                        setDownLoadStatusText(R.string.app_pause);
                        break;

                    case BitmapInfo.INSTALLING:
                        setDownLoadStatusText(R.string.app_installing);
                        break;

                    case BitmapInfo.PRE_INSTALL:
                        setDownLoadStatusText(R.string.app_pre_install);
                        break;

                    case BitmapInfo.DOWNLOAD_ERROR:
                        setDownLoadStatusText(R.string.app_download_error);
                        break;

                    case BitmapInfo.INSTALL_ERROR:
                        setDownLoadStatusText(R.string.app_install_error);
                        break;

                    default:
                        break;
                }
            }
        });
        mBitmapInfo.setStatus(BitmapInfo.NONE);
        //add for dynamic download icon by louhan & weijiaqi 20170804
    }


    public void setDownLoadStatusText(final int stringId) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                setText(stringId);
            }
        };
        mLauncher.runOnUiThread(r);

    }

    public void applyFromShortcutInfo(ShortcutInfo info, IconCache iconCache) {
        applyFromShortcutInfo(info, iconCache, false);
    }

    public void applyFromShortcutInfo(ShortcutInfo info, IconCache iconCache,
            boolean promiseStateChanged) {
        //add for dynamic clock by louhan 20170713 start
        mScript = info.getScript(iconCache);
        //add for dynamic clock by louhan 20170713 end
        Bitmap b = info.getIcon(iconCache);

        DeviceProfile grid = ((Launcher) getContext()).getDeviceProfile();
        setCompoundDrawablePadding(grid.iconDrawablePaddingPx);

        if (info.itemType == LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT) {
            //add for PRODUCTION-4118 by louhan 20171025
            //Drawable srcDrawable = new BitmapDrawable(getContext().getResources(), b);
            //Bitmap srcBitmap = Utilities.createIconBitmap(srcDrawable, getContext(), DisplayMetricsUtils.dip2px(ThemeHelper.WHOLE_SIZE));
            if(info.getIntent().getPackage() == null) {
                b = Utilities.createGomeIconBitmap(getContext(), b);
            }
        }
        FastBitmapDrawable iconDrawable = mLauncher.createIconDrawable(b);
        if (info.isDisabled()) {
            iconDrawable.setState(FastBitmapDrawable.State.DISABLED);
        }
        setIcon(iconDrawable, mIconSize);
        if (info.contentDescription != null) {
            setContentDescription(info.contentDescription);
        }


        setText(info.title);
        setTag(info);

        if (promiseStateChanged || info.isPromise()) {
            applyState(promiseStateChanged);
        }
        if (info.isQueryFromDB() && (info.getDownLoadStatus() == BitmapInfo.DOWNLOAD_ERROR || info.getDownLoadStatus() == BitmapInfo.INSTALL_ERROR)) {
            return;
        } else if (info.isQueryFromDB() && info.getDownLoadStatus() >= BitmapInfo.NONE
                && info.getDownLoadStatus() != BitmapInfo.DOWNLOAD_PAUSE
                && info.getDownLoadStatus() < BitmapInfo.PRE_INSTALL) {
            info.setDownLoadStatus(BitmapInfo.DOWNLOAD_PAUSE);
            mBitmapInfo.setStatus(BitmapInfo.DOWNLOAD_PAUSE);
            mLauncher.updateDownloadItemByPkgName(info.getPkgName(), BitmapInfo.DOWNLOAD_PAUSE, -1, null, null);
        } else if (info.isQueryFromDB() && info.getDownLoadStatus() == BitmapInfo.INSTALLING) {
            mBitmapInfo.setStatus(BitmapInfo.PRE_INSTALL);
            info.setDownLoadStatus(BitmapInfo.PRE_INSTALL);
            String packageName = "";
            if (info.getIntent() != null && info.getIntent().getPackage() != null) {
                packageName = info.getIntent().getPackage();
            }
            if (packageName == null) {
                packageName = info.getPkgName();
            }
            mLauncher.updateDownloadItemByPkgName(packageName, BitmapInfo.PRE_INSTALL, -1, null, null);

        }
    }

    public void applyFromApplicationInfo(AppInfo info) {
        FastBitmapDrawable iconDrawable = mLauncher.createIconDrawable(info.iconBitmap);
        if (info.isDisabled()) {
            iconDrawable.setState(FastBitmapDrawable.State.DISABLED);
        }
        setIcon(iconDrawable, mIconSize);
        setText(info.title);
        if (info.contentDescription != null) {
            setContentDescription(info.contentDescription);
        }
        // We don't need to check the info since it's not a ShortcutInfo
        super.setTag(info);

        // Verify high res immediately
        verifyHighRes();
    }

    public void applyFromPackageItemInfo(PackageItemInfo info) {
        setIcon(mLauncher.createIconDrawable(info.iconBitmap), mIconSize);
        setText(info.title);
        if (info.contentDescription != null) {
            setContentDescription(info.contentDescription);
        }
        // We don't need to check the info since it's not a ShortcutInfo
        super.setTag(info);

        // Verify high res immediately
        verifyHighRes();
    }

    /**
     * Used for measurement only, sets some dummy values on this view.
     */
    public void applyDummyInfo() {
        ColorDrawable d = new ColorDrawable();
        setIcon(mLauncher.resizeIconDrawable(d), mIconSize);
        setText("");
    }

    /**
     * 增加“+”符号
     *add by huanghaihao in 2017-7-11 for adding more app in folder
     */
    public void addPlusIcon() {
        Bitmap bitmap = ThemeHelper.getInstance(getContext()).getAddIcon();
        Drawable drawable = new BitmapDrawable(bitmap);
        setIcon(drawable, mIconSize);
        setText("");
    }

    public void stopDynamic() {
        if (mScript != null) {
            mScript.onStop();
        }
    }

    public void startDynamic() {
        if (mScript != null) {
            if (!mScript.isRuning) {
                mScript.run(this);
            }
        }
    }

    /**
     * Overrides the default long press timeout.
     */
    public void setLongPressTimeout(int longPressTimeout) {
        mLongPressHelper.setLongPressTimeout(longPressTimeout);
    }

    @Override
    protected boolean setFrame(int left, int top, int right, int bottom) {
        if (getLeft() != left || getRight() != right || getTop() != top || getBottom() != bottom) {
            mBackgroundSizeChanged = true;
        }
        return super.setFrame(left, top, right, bottom);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mBackground || super.verifyDrawable(who);
    }

    @Override
    public void setTag(Object tag) {
        if (tag != null) {
            LauncherModel.checkItemInfo((ItemInfo) tag);
        }
        super.setTag(tag);
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);

        if (!mIgnorePressedStateChange) {
            updateIconState();
        }
    }

    /** Returns the icon for this view. */
    public Drawable getIcon() {
        return mIcon;
    }

    /** Returns whether the layout is horizontal. */
    public boolean isLayoutHorizontal() {
        return mLayoutHorizontal;
    }

    private void updateIconState() {
        if (mIcon instanceof FastBitmapDrawable) {
            FastBitmapDrawable d = (FastBitmapDrawable) mIcon;
            if (getTag() instanceof ItemInfo
                    && ((ItemInfo) getTag()).isDisabled()) {
                d.animateState(FastBitmapDrawable.State.DISABLED);
            } else if (isPressed() || mStayPressed) {
                d.animateState(FastBitmapDrawable.State.PRESSED);
            } else {
                d.animateState(FastBitmapDrawable.State.NORMAL);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Call the superclass onTouchEvent first, because sometimes it changes the state to
        // isPressed() on an ACTION_UP
        boolean result = super.onTouchEvent(event);

        // Check for a stylus button press, if it occurs cancel any long press checks.
        if (mStylusEventHelper.checkAndPerformStylusEvent(event)) {
            mLongPressHelper.cancelLongPress();
            result = true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // So that the pressed outline is visible immediately on setStayPressed(),
                // we pre-create it on ACTION_DOWN (it takes a small but perceptible amount of time
                // to create it)
                if (!mDeferShadowGenerationOnTouch && mPressedBackground == null) {
                    mPressedBackground = mOutlineHelper.createMediumDropShadow(this);
                }

                // If we're in a stylus button press, don't check for long press.
                if (!mStylusEventHelper.inStylusButtonPressed()) {
                    mLongPressHelper.postCheckForLongPress();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // If we've touched down and up on an item, and it's still not "pressed", then
                // destroy the pressed outline
                if (!isPressed()) {
                    mPressedBackground = null;
                }

                mLongPressHelper.cancelLongPress();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!Utilities.pointInView(this, event.getX(), event.getY(), mSlop)) {
                    mLongPressHelper.cancelLongPress();
                }
                break;
        }
        return result;
    }

    void setStayPressed(boolean stayPressed) {
        mStayPressed = stayPressed;
        if (!stayPressed) {
            mPressedBackground = null;
        } else {
            if (mPressedBackground == null) {
                mPressedBackground = mOutlineHelper.createMediumDropShadow(this);
            }
        }

        // Only show the shadow effect when persistent pressed state is set.
        ViewParent parent = getParent();
        if (parent != null && parent.getParent() instanceof BubbleTextShadowHandler) {
            ((BubbleTextShadowHandler) parent.getParent()).setPressedIcon(
                    this, mPressedBackground);
        }

        updateIconState();
    }

    void clearPressedBackground() {
        setPressed(false);
        setStayPressed(false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (super.onKeyDown(keyCode, event)) {
            // Pre-create shadow so show immediately on click.
            if (mPressedBackground == null) {
                mPressedBackground = mOutlineHelper.createMediumDropShadow(this);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Unlike touch events, keypress event propagate pressed state change immediately,
        // without waiting for onClickHandler to execute. Disable pressed state changes here
        // to avoid flickering.
        mIgnorePressedStateChange = true;
        boolean result = super.onKeyUp(keyCode, event);

        mPressedBackground = null;
        mIgnorePressedStateChange = false;
        updateIconState();
        return result;
    }


    public void drawDownLoadBitmap(Canvas canvas) {
        //add for dynamic download icon by louhan & weijiaqi 20170804
        if (mBitmapInfo.getStatus() > BitmapInfo.NONE  && mBitmapInfo.getStatus() < BitmapInfo.INSTALLED) {
            mDrawableTopRect.set(getScrollX() + 0 + getPaddingLeft(),
                    getScrollY() + 0 + getPaddingTop(),
                    getScrollX() + getWidth() - getPaddingRight(),
                    getScrollY() + getExtendedPaddingTop() - getCompoundDrawablePadding());

            if (mBitmapInfo.getRendererRect() == null || mBitmapInfo.getMaskBitmap() == null) {
                Rect rendererRect = new Rect(mDrawableTopRect.centerX() - mIconSize / 2,
                        mDrawableTopRect.centerY() - mIconSize / 2,
                        mDrawableTopRect.centerX() + mIconSize / 2,
                        mDrawableTopRect.centerY() + mIconSize / 2);
                mBitmapInfo.setRendererRect(rendererRect);
                //Bitmap maskBitmap = ThemeHelper.getInstance(getContext()).getDownloadMaskIcon();
                Drawable maskDrawable = getResources().getDrawable(R.drawable.hc_app_mask);
                Bitmap maskBitmap =  Utilities.createGomeIconBitmap(getContext().getApplicationContext(),IconScript.drawableToBitmap(maskDrawable));
                mBitmapInfo.setMaskBitmap(maskBitmap);
                Bitmap pauseBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pause);
                mBitmapInfo.setPauseBitmap(pauseBitmap);
            }
            BaseBitmapState bitmapState = BitmapStateFactory.getInstance().createBitmapState(mBitmapInfo);
            DecodeBitmap DecodeBitmap = new DecodeBitmap(bitmapState);
            if (null != DecodeBitmap.decode()) {
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawBitmap(DecodeBitmap.decode(), mDrawableTopRect.centerX() - mIconSize / 2, mDrawableTopRect.centerY() - mIconSize / 2, paint);
            }
//            if (mBitmapInfo.getStatus() > BitmapInfo.NONE && mBitmapInfo.getStatus() < BitmapInfo.INSTALLED) {
//                setText(mBitmapInfo.getDescription() + "");
//            }
        }
        //add for dynamic download icon by louhan & weijiaqi 20170804
    }

    @Override
    public void draw(Canvas canvas) {
        if (!mCustomShadowsEnabled) {
            super.draw(canvas);
            drawDownLoadBitmap(canvas);
            return;
        }

        final Drawable background = mBackground;
        if (background != null) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();

            if (mBackgroundSizeChanged) {
                background.setBounds(0, 0,  getRight() - getLeft(), getBottom() - getTop());
                mBackgroundSizeChanged = false;
            }

            if ((scrollX | scrollY) == 0) {
                background.draw(canvas);
            } else {
                canvas.translate(scrollX, scrollY);
                background.draw(canvas);
                canvas.translate(-scrollX, -scrollY);
            }
        }

        // If text is transparent, don't draw any shadow
        if (getCurrentTextColor() == getResources().getColor(android.R.color.transparent)) {
            getPaint().clearShadowLayer();
            super.draw(canvas);
            /**
             * Added by gaoquan 2017.6.1
             */
            //-------------------------------start--------------///
            ///: Added for unread message feature.@{
            if(!mLauncher.isShowAppSearchWindow) {
                drawUnreadEvent(canvas);
            }
            ///: @}
            //-------------------------------end--------------///


            drawDownLoadBitmap(canvas);

            return;
        }
        // We enhance the shadow by drawing the shadow twice
        getPaint().setShadowLayer(SHADOW_LARGE_RADIUS, 0.0f, SHADOW_Y_OFFSET, SHADOW_LARGE_COLOUR);
        super.draw(canvas);
        canvas.save(Canvas.CLIP_SAVE_FLAG);

        drawDownLoadBitmap(canvas);

        canvas.clipRect(getScrollX(), getScrollY() + getExtendedPaddingTop(),
                getScrollX() + getWidth(),
                getScrollY() + getHeight(), Region.Op.INTERSECT);
        getPaint().setShadowLayer(SHADOW_SMALL_RADIUS, 0.0f, 0.0f, SHADOW_SMALL_COLOUR);
        super.draw(canvas);
        canvas.restore();
        /**
         * Added by gaoquan 2017.6.1
         */
        //-------------------------------start--------------///
        ///: Added for unread message feature.@{
        if(!mLauncher.isShowAppSearchWindow) {
            drawUnreadEvent(canvas);
        }
        ///: @}
        //-------------------------------end--------------///
        /**
         * Added by gaoquan 2017.7.20
         */
        //-------------------------------start--------------///
        mLauncher.getPackageCircle().drawCircle(canvas, this);
        //-------------------------------end--------------///
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mBackground != null) mBackground.setCallback(this);

        if (mIcon instanceof PreloadIconDrawable) {
            ((PreloadIconDrawable) mIcon).applyPreloaderTheme(getPreloaderTheme());
        }
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        if (mScript != null){
            if (!mScript.isRuning) {
                mScript.run(this);
            }
        }
    }

    //add for dynamic clock by louhan 20170713 start
    @Override
    public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom) {
        if (top != null) {
            if (mScript != null) {
                top = mScript;
                FastBitmapDrawable fastBitmapDrawable = new FastBitmapDrawable(ThemeHelper.getInstance(getContext()).getClockBackground());
                //modified by liuning for dynamic clock size in the moveViewRestContainer on 2017/7/25 start
                //mScript.setBounds(fastBitmapDrawable.getBounds());
                mScript.setBounds(0, 0, mIconSize, mIconSize);
                //modified by liuning for dynamic clock size in the moveViewRestContainer on 2017/7/25 end
                mScript.setFastBitmapDrawable(fastBitmapDrawable);
                if (!mScript.isRuning) {
                    mScript.run(this);
                }
            }
        }
        super.setCompoundDrawables(left, top, right, bottom);
    }
    //add for dynamic clock by louhan 20170713 end
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mBackground != null) mBackground.setCallback(null);
        if (mScript != null) {
            mScript.onStop();
//            if (mScript.getFastBitmapDrawable() != null) {
//                mScript.getFastBitmapDrawable().getBitmap().recycle();
//            }
//            mScript.setFastBitmapDrawable(null);
//            mScript = null;
        }
    }

    @Override
    public void setTextColor(int color) {
        mTextColor = color;
        super.setTextColor(color);
    }

    @Override
    public void setTextColor(ColorStateList colors) {
        mTextColor = colors.getDefaultColor();
        super.setTextColor(colors);
    }

    public void setTextVisibility(boolean visible) {
        Resources res = getResources();
        if (visible) {
            super.setTextColor(mTextColor);
        } else {
            super.setTextColor(res.getColor(android.R.color.transparent));
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mLongPressHelper.cancelLongPress();
    }

    public void applyState(boolean promiseStateChanged) {
        if (getTag() instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo) getTag();
            final boolean isPromise = info.isPromise();
            final int progressLevel = isPromise ?
                    ((info.hasStatusFlag(ShortcutInfo.FLAG_INSTALL_SESSION_ACTIVE) ?
                            info.getInstallProgress() : 0)) : 100;

            setContentDescription(progressLevel > 0 ?
                getContext().getString(R.string.app_downloading_title, info.title,
                        NumberFormat.getPercentInstance().format(progressLevel * 0.01)) :
                    getContext().getString(R.string.app_waiting_download_title, info.title));

            if (mIcon != null) {
                final PreloadIconDrawable preloadDrawable;
                if (mIcon instanceof PreloadIconDrawable) {
                    preloadDrawable = (PreloadIconDrawable) mIcon;
                } else {
                    preloadDrawable = new PreloadIconDrawable(mIcon, getPreloaderTheme());
                    setIcon(preloadDrawable, mIconSize);
                }

                preloadDrawable.setLevel(progressLevel);
                if (promiseStateChanged) {
                    preloadDrawable.maybePerformFinishedAnimation();
                }
            }
        }
    }

    private Theme getPreloaderTheme() {
        Object tag = getTag();
        int style = ((tag != null) && (tag instanceof ShortcutInfo) &&
                (((ShortcutInfo) tag).container >= 0)) ? R.style.PreloadIcon_Folder
                        : R.style.PreloadIcon;
        Theme theme = sPreloaderThemes.get(style);
        if (theme == null) {
            theme = getResources().newTheme();
            theme.applyStyle(style, true);
            sPreloaderThemes.put(style, theme);
        }
        return theme;
    }

    /**
     * Sets the icon for this view based on the layout direction.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private Drawable setIcon(Drawable icon, int iconSize) {
        mIcon = icon;
        if (iconSize != -1) {
            mIcon.setBounds(0, 0, iconSize, iconSize);
        }
        if (mLayoutHorizontal) {
            if (Utilities.ATLEAST_JB_MR1) {
                setCompoundDrawablesRelative(mIcon, null, null, null);
            } else {
                setCompoundDrawables(mIcon, null, null, null);
            }
        } else {
            setCompoundDrawables(null, mIcon, null, null);
        }
        return icon;
    }

    @Override
    public void requestLayout() {
        if (!mDisableRelayout) {
            super.requestLayout();
        }
    }

    /**
     * Applies the item info if it is same as what the view is pointing to currently.
     */
    public void reapplyItemInfo(final ItemInfo info) {
        if (getTag() == info) {
            FastBitmapDrawable.State prevState = FastBitmapDrawable.State.NORMAL;
            if (mIcon instanceof FastBitmapDrawable) {
                prevState = ((FastBitmapDrawable) mIcon).getCurrentState();
            }
            mIconLoadRequest = null;
            mDisableRelayout = true;

            if (info instanceof AppInfo) {
                applyFromApplicationInfo((AppInfo) info);
            } else if (info instanceof ShortcutInfo) {
                applyFromShortcutInfo((ShortcutInfo) info,
                        LauncherAppState.getInstance().getIconCache());
                if ((info.rank < FolderIcon.NUM_ITEMS_IN_PREVIEW) && (info.container >= 0)) {
                    View folderIcon =
                            mLauncher.getWorkspace().getHomescreenIconByItemId(info.container);
                    if (folderIcon != null) {
                        folderIcon.invalidate();
                    }
                }
            } else if (info instanceof PackageItemInfo) {
                applyFromPackageItemInfo((PackageItemInfo) info);
            }

            // If we are reapplying over an old icon, then we should update the new icon to the same
            // state as the old icon
            if (mIcon instanceof FastBitmapDrawable) {
                ((FastBitmapDrawable) mIcon).setState(prevState);
            }

            mDisableRelayout = false;
        }
    }

    /**
     * Verifies that the current icon is high-res otherwise posts a request to load the icon.
     */
    public void verifyHighRes() {
        if (mIconLoadRequest != null) {
            mIconLoadRequest.cancel();
            mIconLoadRequest = null;
        }
        if (getTag() instanceof AppInfo) {
            AppInfo info = (AppInfo) getTag();
            if (info.usingLowResIcon) {
                mIconLoadRequest = LauncherAppState.getInstance().getIconCache()
                        .updateIconInBackground(BubbleTextView.this, info);
            }
        } else if (getTag() instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo) getTag();
            if (info.usingLowResIcon) {
                mIconLoadRequest = LauncherAppState.getInstance().getIconCache()
                        .updateIconInBackground(BubbleTextView.this, info);
            }
        } else if (getTag() instanceof PackageItemInfo) {
            PackageItemInfo info = (PackageItemInfo) getTag();
            if (info.usingLowResIcon) {
                mIconLoadRequest = LauncherAppState.getInstance().getIconCache()
                        .updateIconInBackground(BubbleTextView.this, info);
            }
        }
    }

    @Override
    public void setFastScrollFocusState(final FastBitmapDrawable.State focusState, boolean animated) {
        // We can only set the fast scroll focus state on a FastBitmapDrawable
        if (!(mIcon instanceof FastBitmapDrawable)) {
            return;
        }

        FastBitmapDrawable d = (FastBitmapDrawable) mIcon;
        if (animated) {
            FastBitmapDrawable.State prevState = d.getCurrentState();
            if (d.animateState(focusState)) {
                // If the state was updated, then update the view accordingly
                animate().scaleX(focusState.viewScale)
                        .scaleY(focusState.viewScale)
                        .setStartDelay(getStartDelayForStateChange(prevState, focusState))
                        .setDuration(d.getDurationForStateChange(prevState, focusState))
                        .start();
            }
        } else {
            if (d.setState(focusState)) {
                // If the state was updated, then update the view accordingly
                animate().cancel();
                setScaleX(focusState.viewScale);
                setScaleY(focusState.viewScale);
            }
        }
    }

    /**
     * Returns the start delay when animating between certain {@link FastBitmapDrawable} states.
     */
    private static int getStartDelayForStateChange(final FastBitmapDrawable.State fromState,
            final FastBitmapDrawable.State toState) {
        switch (toState) {
            case NORMAL:
                switch (fromState) {
                    case FAST_SCROLL_HIGHLIGHTED:
                        return FastBitmapDrawable.FAST_SCROLL_INACTIVE_DURATION / 4;
                }
        }
        return 0;
    }

    /**
     * Interface to be implemented by the grand parent to allow click shadow effect.
     */
    public interface BubbleTextShadowHandler {
        void setPressedIcon(BubbleTextView icon, Bitmap background);
    }

    /**
     * Added by gaoquan 2017.6.1
     */
    //-------------------------------start--------------///
    ///: Added for unread message feature.@{
    private void drawUnreadEvent(Canvas canvas) {
        if(!isShowIcon){
            return;
        }
        BadgeUnreadLoader.drawUnreadEventIfNeed(canvas, this);
    }

    private boolean isShowIcon = true;

    public void setShowIcon(boolean showIcon) {
        isShowIcon = showIcon;
    }

    ///: @}
    //-------------------------------end--------------///
    /**
     * Added by gaoquan 2017.7.20
     */
    //-------------------------------start--------------///
    public Rect getTextBound(){
        Rect mBound = new Rect();
        getPaint().getTextBounds(getText().toString(), 0, getText().length(), mBound);
        return mBound;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (getTag() instanceof ItemInfo) {
            Resources res = getContext().getResources();
            // added by jubingcheng for specify text padding start on 2017/7/24
            int paddingLeftRight = res.getDimensionPixelSize(R.dimen.dynamic_grid_text_left_right_padding);
            // added by jubingcheng for specify text padding start end 2017/7/24
            if (((ItemInfo) getTag()).isNewApp) {
                DisplayMetrics dm = res.getDisplayMetrics();
                this.setPadding( paddingLeftRight + mLauncher.getPackageCircle().getCircleRadius() * 2
                                + mLauncher.getPackageCircle().getCircleTitleSpacing(), getPaddingTop(),
                        paddingLeftRight + mLauncher.getPackageCircle().getCircleRadius() * 2
                                + mLauncher.getPackageCircle().getCircleTitleSpacing(), getPaddingBottom());
            } else {
                this.setPadding( paddingLeftRight, getPaddingTop(), paddingLeftRight, getPaddingBottom());
            }
        }
    }
    //-------------------------------end--------------///

    //add for dynamic download icon by louhan & weijiaqi 20170804
    public BitmapInfo mBitmapInfo;

    public BitmapInfo getBitmapInfo() {
        return mBitmapInfo;
    }

    public Drawable getDrawable(Bitmap bitmap) {
        //add for PRODUCTION-4118 by louhan 20171025
        //Drawable srcDrawable = new BitmapDrawable(getContext().getResources(), bitmap);
        //Bitmap srcBitmap = Utilities.createIconBitmap(srcDrawable, getContext(), DisplayMetricsUtils.dip2px(ThemeHelper.WHOLE_SIZE));

        Bitmap realBitmap = Utilities.createGomeIconBitmap(LauncherAppState.getInstance().getContext(), bitmap);
        Drawable drawableDefault = new BitmapDrawable(realBitmap);
        drawableDefault.setBounds(0, 0, realBitmap.getWidth(), realBitmap.getHeight());
        return drawableDefault;
    }

    @Override
    public void onIconReady(final Bitmap bitmap) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setIcon(getDrawable(bitmap), mIconSize);
            }
        });
    }

    @Override
    public void onReady() {
        Log.e(TAG, "mBitmapInfo.getStatus() = " + mBitmapInfo.getStatus());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBitmapInfo.setStatus(BitmapInfo.PRE_DOWNLOAD);
                mBitmapInfo.setCircleInterpolator(1f);
                postInvalidate();
            }
        }, 1000);
    }

    @Override
    public void onStart(final String appName,final String pkgName, String iconUrl) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final ContentResolver cr = mLauncher.getContentResolver();
                Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI,
                        new String[]{LauncherSettings.Favorites.STATUS, LauncherSettings.Favorites.PROCESS, LauncherSettings.Favorites.ICON},
                        LauncherSettings.BaseLauncherColumns.INTENT + " LIKE ? ", new String[]{"%" + pkgName + "%"}, null);
                int status = -1;
                int process = -1;
                Bitmap icon = null;
                while (c.moveToNext()) {
                    final int statusIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.STATUS);
                    final int processIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.PROCESS);
                    final int iconIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
                    status = c.getInt(statusIndex);
                    process = c.getInt(processIndex);
                    icon = Utilities.createIconBitmap(c, iconIndex, mLauncher);
                    Log.i(TAG, "pkgName = " + pkgName + "onStart = " + status);

                    // Added by louhan for fix bug GMOS-7669,7938
                    if (icon != null) {
                        final Bitmap finalIcon = icon;
                        mLauncher.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setIcon(getDrawable(finalIcon), mIconSize);
                            }
                        });

                    }
                    if (status == 0) {
                        status = BitmapInfo.ICON_READY;
                    }
                    mBitmapInfo.setStatus(status);
                    mBitmapInfo.setCircleSchedule(process);
                    mBitmapInfo.setTransLateInterpolator(1f);
                    mBitmapInfo.setCircleInterpolator(1f);
                    postInvalidate();
                }
                c.close();
                Log.i(TAG, "appName = " + appName + " | " + "status = " + status + " | " + "process = " + process);
            }
        };

        mLauncher.runOnDownLoadThread(runnable);

    }


    @Override
    public void onPause(String appName, String pkgName) {
        mBitmapInfo.setStatus(BitmapInfo.DOWNLOAD_PAUSE);
        postInvalidate();
    }

    @Override
    public void onResume(String appName, String pkgName) {
        Log.e(TAG, "appName + " + appName);
        mBitmapInfo.setStatus(BitmapInfo.DOWNLOADING);
        postInvalidate();
    }

    @Override
    public void onProcess(String appName, String pkgName, long process) {
        mBitmapInfo.setCircleInterpolator(1f);
        mBitmapInfo.setCircleSchedule(process);
        mBitmapInfo.setStatus(BitmapInfo.DOWNLOADING);
        postInvalidate();
    }

    @Override
    public void onInstalling(String pkgName, float process) {
        if (mBitmapInfo.getInstallProcess() >= process) {
            return;
        }
        mBitmapInfo.setStatus(BitmapInfo.INSTALLING);
        mBitmapInfo.setInstallProcess(process);
        postInvalidate();
    }

    @Override
    public void onInstalled(String pkgName) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBitmapInfo.setStatus(BitmapInfo.INSTALLED);
                mBitmapInfo.setTransLateInterpolator(1f);
                Log.e(TAG, "onInstalled = " + 1f);
                postInvalidate();
            }
        });


    }

    @Override
    public void onCanceled(final String appName, String pkgName) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBitmapInfo.setStatus(BitmapInfo.NORMAL);
                setText(appName);
                postInvalidate();
            }
        });
    }

    @Override
    public void onComplete(String appName, String pkgName) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBitmapInfo.setStatus(BitmapInfo.DOWNLOADED);
                mBitmapInfo.setCircleSchedule(100);
                postInvalidate();
            }
        });
    }

    @Override
    public void onDownloadError(String appName, String pkgName, String err) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBitmapInfo.setStatus(BitmapInfo.DOWNLOAD_ERROR);
                postInvalidate();
            }
        });
    }

    @Override
    public void onInstallError(String appName, String pkgName, String err) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBitmapInfo.setStatus(BitmapInfo.INSTALL_ERROR);
                postInvalidate();
            }
        });
    }

    @Override
    public void onSucceed(final String appName) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setText(appName);
                mBitmapInfo.setStatus(BitmapInfo.INSTALLED);
                mBitmapInfo.setTransLateInterpolator(1f);
                postInvalidate();
            }
        });
    }

    //add for dynamic download icon by louhan & weijiaqi 20170804
}
