package com.gome.launcher;

/**
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;

import com.gome.launcher.util.DLog;

import java.util.ArrayList;
import java.util.Iterator;

public class FolderIconPagedView extends PagedView {

    private static final String TAG = "FolderPagedView";

    private static final boolean ALLOW_FOLDER_SCROLL = true;

    private final IconCache mIconCache;

    private final int mMaxCountX;
    private final int mMaxCountY;
    private final int mMaxItemsPerPage;

    private int mGridCountX;
    private int mGridCountY;

    //delete by liuning for they are unused on 2017/7/15 start
//    private Folder mFolder;
//    private DeviceProfile mDeviceProfile;

//    private final ArrayList<View> views = new ArrayList<View>();
    //delete by liuning for they are unused on 2017/7/15 end

    private Launcher mLauncher;

    private boolean mIsMoveFolderIcon = false;

    public FolderIconPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLauncher = (Launcher)context;
        LauncherAppState app = LauncherAppState.getInstance();

        InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
        //zhaosuzhou Modify the maximum number of rows
        // and columns below the folder on each page.
        mMaxCountX = profile.numFolderColumns;
        mMaxCountY = profile.numFolderRows;

        //support rebound effect
        mHasReboundEffect = false;

        mMaxItemsPerPage = mMaxCountX * mMaxCountY;

        mIconCache = app.getIconCache();

    }

    /**
     * Sets up the grid size such that {@param count} items can fit in the grid.
     * The grid size is calculated such that countY <= countX and countX = ceil(sqrt(count)) while
     * maintaining the restrictions of {@link #mMaxCountX} &amp; {@link #mMaxCountY}.
     */
    private void setupContentDimensions(int count) {
        //zhaosuzhou  Modify the folder every page is displayed squares start 2016/4/6
        mGridCountX = mMaxCountX;
        mGridCountY = mMaxCountY;
        //zhaosuzhou  Modify the folder every page is displayed squares end 2016/4/6
        // Update grid sizes
        for (int i = getPageCount() - 1; i >= 0; i--) {
            getPageAt(i).setGridSize(mGridCountX, mGridCountY);
        }
    }

    /**
     * Binds items to the layout.
     * @return list of items that could not be bound, probably because we hit the max size limit.
     */
    public ArrayList<ShortcutInfo> bindItems(ArrayList<ShortcutInfo> items) {
        ArrayList<View> icons = new ArrayList<View>();
        ArrayList<ShortcutInfo> extra = new ArrayList<ShortcutInfo>();
        for (ShortcutInfo item : items) {
            if (!ALLOW_FOLDER_SCROLL && icons.size() >= mMaxItemsPerPage) {
                extra.add(item);
            } else {
                icons.add(createNewView(item));
            }
        }
        arrangeChildren(icons, icons.size());
        return extra;
    }



    @SuppressLint("InflateParams")
    public View createNewView(ShortcutInfo item) {

        final FolderIconImageView imageview = new FolderIconImageView(getContext());
        Bitmap b = item.getIcon(mIconCache);
        FastBitmapDrawable iconDrawable = mLauncher.createIconDrawable(b);
        imageview.setDrawable(iconDrawable , mLauncher.getDeviceProfile(),isMoveFolderIcon());

        if (item.contentDescription != null) {
            imageview.setContentDescription(item.contentDescription);
        }

        imageview.setVisibility(View.VISIBLE);
        imageview.setTag(item);

        CellLayout.LayoutParams lp = new CellLayout.LayoutParams(
                item.cellX, item.cellY, item.spanX, item.spanY);

        imageview.setLayoutParams(lp);

        return imageview;
    }

    @Override
    public CellLayout getPageAt(int index) {
        return (CellLayout) getChildAt(index);
    }

    public CellLayout createAndAddNewPage() {

        CellLayout page = new CellLayout(getContext());
        //page.setCellDimensions(mDeviceProfile.folderIconCellWidth, mDeviceProfile.folderIconCellHeight);//zhaosuzhou effect
        page.getShortcutsAndWidgets().setMotionEventSplittingEnabled(false);
        page.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        page.setInvertIfRtl(true);
        page.setGridSize(mGridCountX, mGridCountY);
        page.getShortcutsAndWidgets().setCustomEnable(true);

        addView(page, -1, generateDefaultLayoutParams());
        return page;
    }

    @Override
    protected int getChildGap() {
        return 0;//getPaddingLeft() + getPaddingRight();
    }

    public void setFixedSize(int width, int height) {
        width -= (getPaddingLeft() + getPaddingRight());
        height -= (getPaddingTop() + getPaddingBottom());
        for (int i = getChildCount() - 1; i >= 0; i --) {
            ((CellLayout) getChildAt(i)).setFixedSize(width, height);
        }
    }

    @SuppressLint("RtlHardcoded")
    public void arrangeChildren(ArrayList<View> list, int itemCount) {
        ArrayList<CellLayout> pages = new ArrayList<CellLayout>();
        for (int i = 0; i < getChildCount(); i++) {
            CellLayout page = (CellLayout) getChildAt(i);
            page.removeAllViews();
            pages.add(page);
        }
        setupContentDimensions(itemCount);

        Iterator<CellLayout> pageItr = pages.iterator();
        CellLayout currentPage = null;

        int position = 0;
        int newX, newY, rank;

        DLog.e(TAG,"arrangeChildren --> pages = "+pages.size() + " itemCount = "+itemCount
                + " , getChildCount() " + getChildCount());

        rank = 0;
        for (int i = 0; i < itemCount; i++) {
            View v = list.size() > i ? list.get(i) : null;

            DLog.e(TAG,"arrangeChildren --> i = "+i + "  position = "+position
                    + "   itemCount = "+itemCount + " ,mMaxItemsPerPage : " + mMaxItemsPerPage);

            if (currentPage == null || position >= mMaxItemsPerPage) {
                // Next page
                if (pageItr.hasNext()) {
                    currentPage = pageItr.next();
                } else {
                    currentPage = createAndAddNewPage();
                }
                position = 0;
            } else if(position == mMaxItemsPerPage - 1 && itemCount > 0 && itemCount % mMaxItemsPerPage == 0){
                createAndAddNewPage();
            }

            if (v != null) {
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
                newX = position % mGridCountX;
                newY = position / mGridCountX;
                ItemInfo info = (ItemInfo) v.getTag();
                if (info.cellX != newX || info.cellY != newY || info.rank != rank) {
                    info.cellX = newX;
                    info.cellY = newY;
                    info.rank = rank;
                }
                lp.cellX = info.cellX;
                lp.cellY = info.cellY;
                currentPage.addViewToCellLayout(
                        v, -1, mLauncher.getViewIdForItem(info), lp, true);
            }

            rank ++;
            position++;
        }

        // Remove extra views.
        boolean removed = false;
        while (pageItr.hasNext()) {
            removeView(pageItr.next());
            removed = true;
        }
        if (removed) {
            setCurrentPage(0);
        }

        setEnableOverscroll(getPageCount() > 1);
    }

    @Override
    protected void notifyPageSwitchListener() {
        super.notifyPageSwitchListener();
    }

    @Override
    protected void onPageBeginMoving() {
        super.onPageBeginMoving();
        //getVisiblePages(sTempPosArray);
    }

    @Override
    protected void getEdgeVerticalPostion(int[] pos) {
        pos[0] = 0;
        pos[1] = getViewportHeight();
    }
	
	//added by liuning for multi apps move on 2017/7/18 start
    public boolean isMoveFolderIcon() {
        return mIsMoveFolderIcon;
    }

    public void setMoveFolderIcon(boolean isMoveFolderIcon) {
        mIsMoveFolderIcon = isMoveFolderIcon;
    }
	//added by liuning for multi apps move on 2017/7/3 end
}

