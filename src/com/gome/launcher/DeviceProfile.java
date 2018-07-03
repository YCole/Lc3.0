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

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class DeviceProfile {

    public final InvariantDeviceProfile inv;

    // Device properties
    public final boolean isTablet;
    public final boolean isLargeTablet;
    public final boolean isPhone;
    public final boolean transposeLayoutWithOrientation;

    // Device properties in current orientation
    public final boolean isLandscape;
    public final int widthPx;
    public final int heightPx;
    public final int availableWidthPx;
    public final int availableHeightPx;
    /**
     * The maximum amount of left/right workspace padding as a percentage of the screen width.
     * To be clear, this means that up to 7% of the screen width can be used as left padding, and
     * 7% of the screen width can be used as right padding.
     */
    private static final float MAX_HORIZONTAL_PADDING_PERCENT = 0.14f;

    // Overview mode
    private final int overviewModeMinIconZoneHeightPx;
    private final int overviewModeMaxIconZoneHeightPx;
    private final int overviewModeBarItemWidthPx;
    private final int overviewModeBarSpacerWidthPx;
    private final float overviewModeIconZoneRatio;

    // Workspace
    private int desiredWorkspaceLeftRightMarginPx;
    public final int edgeMarginPx;
    public final Rect defaultWidgetPadding;
    private final int pageIndicatorHeightPx;
    private final int pageIndicatorMarkPx;
    private final int pageIndicatorOffSetPx;
    private final int defaultPageSpacingPx;
    private float dragViewScale;

    // Workspace icons
    public int iconSizePx;
    public int iconTextSizePx;
    public int iconDrawablePaddingPx;
    public int iconDrawablePaddingOriginalPx;

    public int cellWidthPx;
    public int cellHeightPx;

    // Folder
    public int folderBackgroundOffset;
    public int folderIconSizePx;
    public int folderCellWidthPx;
    public int folderCellHeightPx;

    // FolderIcon
    public int folderIconPageViewSizePx;

    // Hotseat
    public int hotseatCellWidthPx;
    public int hotseatCellHeightPx;
    public int hotseatIconSizePx;
    private int normalHotseatBarHeightPx, shortHotseatBarHeightPx;
    private int hotseatBarHeightPx; // One of the above.

    // All apps
    public int allAppsNumCols;
    public int allAppsNumPredictiveCols;
    public int allAppsButtonVisualSize;
    public final int allAppsIconSizePx;
    public final float allAppsIconTextSizeSp;

    // QSB
    private int searchBarWidgetInternalPaddingTop, searchBarWidgetInternalPaddingBottom;
    private int searchBarTopPaddingPx;
    private int tallSearchBarNegativeTopPaddingPx, normalSearchBarTopExtraPaddingPx;
    private int searchBarTopExtraPaddingPx; // One of the above.
    private int normalSearchBarBottomPaddingPx, tallSearchBarBottomPaddingPx;
    private int searchBarBottomPaddingPx; // One of the above.
    private int normalSearchBarSpaceHeightPx, tallSearchBarSpaceHeightPx;
    private int searchBarSpaceHeightPx; // One of the above.

    //Move icons add by liuning on 2017/7/18
    public int moveIconSizePx;

    //added by liuning for the page shrink factor when darging on 2017/7/25
    public float mDragModeShrinkFactor;
    public int cellLayoutLeftRightPadding;

    //added by liuning for the icon shrink factor when be darged into folder on 2017/8/4
    public float mFolderIconImageShrinkFactor;

    public int mPresetPreviewSize;

    public int mPresetPreviewWidgetWidth;

    public int mPresetPreviewWidgetHight;

    public DeviceProfile(Context context, InvariantDeviceProfile inv,
            Point minSize, Point maxSize,
            int width, int height, boolean isLandscape) {

        this.inv = inv;
        this.isLandscape = isLandscape;

        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        // Constants from resources
        isTablet = res.getBoolean(R.bool.is_tablet);
        isLargeTablet = res.getBoolean(R.bool.is_large_tablet);
        isPhone = !isTablet && !isLargeTablet;

        // Some more constants
        transposeLayoutWithOrientation =
                res.getBoolean(R.bool.hotseat_transpose_layout_with_orientation);

        ComponentName cn = new ComponentName(context.getPackageName(),
                this.getClass().getName());
        defaultWidgetPadding = AppWidgetHostView.getDefaultPaddingForWidget(context, cn, null);
        edgeMarginPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin);
        desiredWorkspaceLeftRightMarginPx = 2 * edgeMarginPx;
        pageIndicatorHeightPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_page_indicator_height);
        //add by chenchao 2017.8.2 start
        pageIndicatorMarkPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_page_indicator_mark);
        //add by chenchao 2017.8.2 end
        pageIndicatorOffSetPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_page_indicator_bottom_offset);
        defaultPageSpacingPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_workspace_page_spacing);
        overviewModeMinIconZoneHeightPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_min_icon_zone_height);
        overviewModeMaxIconZoneHeightPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_max_icon_zone_height);
        overviewModeBarItemWidthPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_bar_item_width);
        overviewModeBarSpacerWidthPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_bar_spacer_width);
        overviewModeIconZoneRatio =
                res.getInteger(R.integer.config_dynamic_grid_overview_icon_zone_percentage) / 100f;
        iconDrawablePaddingOriginalPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_icon_drawable_padding);
        cellLayoutLeftRightPadding =  res.getDimensionPixelSize(R.dimen.celllayout_left_right_padding);
        // AllApps uses the original non-scaled icon text size
        allAppsIconTextSizeSp = inv.iconTextSize;
        mPresetPreviewSize = res.getDimensionPixelSize(R.dimen.preset_Preview_size);
        mPresetPreviewWidgetWidth = res.getDimensionPixelSize(R.dimen.preset_Preview_widget_width);
        mPresetPreviewWidgetHight = res.getDimensionPixelSize(R.dimen.preset_Preview_widget_hight);
        // AllApps uses the original non-scaled icon size
        allAppsIconSizePx = Utilities.pxFromDp(inv.iconSize, dm);

        // Determine sizes.
        widthPx = width;
        heightPx = height;
        if (isLandscape) {
            availableWidthPx = maxSize.x;
            availableHeightPx = minSize.y;
        } else {
            availableWidthPx = minSize.x;
            availableHeightPx = maxSize.y;
        }
        // Calculate the remaining vars
        updateAvailableDimensions(dm, res);
        computeAllAppsButtonSize(context);

        mDragModeShrinkFactor =
                res.getInteger(R.integer.config_workspaceDragModeShrinkPercentage) / 100f;

        mFolderIconImageShrinkFactor = res.getInteger(R.integer.config_folderIconImageShrinkPercentage) / 100f;
    }

    /**
     * Determine the exact visual footprint of the all apps button, taking into account scaling
     * and internal padding of the drawable.
     */
    private void computeAllAppsButtonSize(Context context) {
        Resources res = context.getResources();
        float padding = res.getInteger(R.integer.config_allAppsButtonPaddingPercent) / 100f;
        allAppsButtonVisualSize = (int) (hotseatIconSizePx * (1 - padding)) - context.getResources()
                        .getDimensionPixelSize(R.dimen.all_apps_button_scale_down);
    }

    private void updateAvailableDimensions(DisplayMetrics dm, Resources res) {
        // Check to see if the icons fit in the new available height.  If not, then we need to
        // shrink the icon size.
        float scale = 1f;
        int drawablePadding = iconDrawablePaddingOriginalPx;
        updateIconSize(1f, drawablePadding, res, dm);
        float usedHeight = (cellHeightPx * inv.numRows);

        // We only care about the top and bottom workspace padding, which is not affected by RTL.
        Rect workspacePadding = getWorkspacePadding(false /* isLayoutRtl */);
        int maxHeight = (availableHeightPx - workspacePadding.top - workspacePadding.bottom);
        if (usedHeight > maxHeight) {
            scale = maxHeight / usedHeight;
            drawablePadding = 0;
        }
        updateIconSize(scale, drawablePadding, res, dm);
    }

    private void updateIconSize(float scale, int drawablePadding, Resources res,
                                DisplayMetrics dm) {
        iconSizePx = (int) (Utilities.pxFromDp(inv.iconSize, dm) * scale);
        /**
         * Added by gaoquan 2017.10.13
         * fix:
         * GMOS2.0GMOS-9925【Launcher】长按桌面-设置，没有匹配大字体
         * GMOS2.0GMOS-9932【Launcher】launcher的 TOAST 没有大字体
         */
        //-------------------------------start--------------///
        iconTextSizePx = (int) (Utilities.pxFromDp(inv.iconTextSize, dm) /* * scale*/);
        //-------------------------------end--------------///
        iconDrawablePaddingPx = drawablePadding;
        hotseatIconSizePx = (int) (Utilities.pxFromDp(inv.hotseatIconSize, dm) * scale);

        // Search Bar
        normalSearchBarSpaceHeightPx = res.getDimensionPixelSize(
                R.dimen.dynamic_grid_search_bar_height);
        tallSearchBarSpaceHeightPx = res.getDimensionPixelSize(
                R.dimen.dynamic_grid_search_bar_height_tall);
        searchBarWidgetInternalPaddingTop = res.getDimensionPixelSize(
                R.dimen.qsb_internal_padding_top);
        searchBarWidgetInternalPaddingBottom = res.getDimensionPixelSize(
                R.dimen.qsb_internal_padding_bottom);
        normalSearchBarTopExtraPaddingPx = res.getDimensionPixelSize(
                R.dimen.dynamic_grid_search_bar_extra_top_padding);
        tallSearchBarNegativeTopPaddingPx = res.getDimensionPixelSize(
                R.dimen.dynamic_grid_search_bar_negative_top_padding_short);
        if (isTablet && !isVerticalBarLayout()) {
            searchBarTopPaddingPx = searchBarWidgetInternalPaddingTop;
            normalSearchBarBottomPaddingPx = searchBarWidgetInternalPaddingBottom +
                    res.getDimensionPixelSize(R.dimen.dynamic_grid_search_bar_bottom_padding_tablet);
            tallSearchBarBottomPaddingPx = normalSearchBarBottomPaddingPx;
        } else {
            searchBarTopPaddingPx = searchBarWidgetInternalPaddingTop;
            normalSearchBarBottomPaddingPx = searchBarWidgetInternalPaddingBottom +
                    res.getDimensionPixelSize(R.dimen.dynamic_grid_search_bar_bottom_padding);
            tallSearchBarBottomPaddingPx = searchBarWidgetInternalPaddingBottom
                    + res.getDimensionPixelSize(
                    R.dimen.dynamic_grid_search_bar_bottom_negative_padding_short);
        }

        // Calculate the actual text height
        Paint textPaint = new Paint();
        textPaint.setTextSize(iconTextSizePx);
        FontMetrics fm = textPaint.getFontMetrics();
        cellWidthPx = iconSizePx;
        cellHeightPx = iconSizePx + iconDrawablePaddingPx + (int) Math.ceil(fm.bottom - fm.top);
        final float scaleDps = res.getDimensionPixelSize(R.dimen.dragViewScale);
        dragViewScale = (iconSizePx + scaleDps) / iconSizePx;

        // Hotseat
        //Modify by chenchao 2017/8/2 hotseat height 60dp;
        //Modify by chenchao 2017/12/15 hotseat height 65dp;
//        normalHotseatBarHeightPx = iconSizePx + 4 * edgeMarginPx;
        normalHotseatBarHeightPx = iconSizePx + 3 * edgeMarginPx / 2;
        //Modify by chenchao 2017/12/15 hotseat height 65dp;
        //Modify by chenchao 2017/8/2 hotseat height 60dp;
        shortHotseatBarHeightPx = iconSizePx + 2 * edgeMarginPx;
        hotseatCellWidthPx = iconSizePx;
        hotseatCellHeightPx = iconSizePx;

        /* delete useless code,
           mod width and height of folderCell
           by liuqiushou 20170629 @{
        */
        // Folder
        //int folderCellPadding = isTablet || isLandscape ? 6 * edgeMarginPx : 3 * edgeMarginPx;
        // Don't let the folder get too close to the edges of the screen.

        /* mod for folder style by liuqiushou 20170620 @
        folderCellWidthPx = Math.min(cellWidthPx + folderCellPadding,
                (availableWidthPx - 4 * edgeMarginPx) / inv.numFolderColumns);
        folderCellHeightPx = cellHeightPx + edgeMarginPx;
        folderBackgroundOffset = -edgeMarginPx;
        folderIconSizePx = iconSizePx + 2 * -folderBackgroundOffset;
        */
        //zhaosuzhou modify for Add the gap of item int the CellLauout of FolderPageView
        //modify folderCellWidthPx as 280px,
        //modify folderCellHeightPx as 300px for FolderPagedView height
        // as 331dp (300px * 3 + 93px(padding)) by liuqiushou 20170623 @{

        folderCellWidthPx = (int)(1.0 * res.getDimensionPixelSize(R.dimen.folder_paged_view_width)
                / inv.numFolderColumns);
        folderCellHeightPx = (int)(1.0 * (res.getDimensionPixelSize(R.dimen.folder_paged_view_height)
                - res.getDimensionPixelSize(R.dimen.folder_paged_view_padding_bottom)
                - res.getDimensionPixelSize(R.dimen.folder_paged_view_padding_top)) / inv.numFolderRows);
        //@}
        //@}

        folderBackgroundOffset =0;

        // FolderIcon
        folderIconSizePx = iconSizePx;
        folderIconPageViewSizePx = res.getDimensionPixelSize(R.dimen.folder_icon_page_view_size);

        //MoveIcon add by liuning for multi apps move on 2017/7/18 start
        moveIconSizePx = res.getDimensionPixelSize(R.dimen.move_item_icon_size);
		//add by liuning for multi apps move on 2017/7/18 end
    }

    /**
     * @param recyclerViewWidth the available width of the AllAppsRecyclerView
     */
    public void updateAppsViewNumCols(Resources res, int recyclerViewWidth) {
        int appsViewLeftMarginPx =
                res.getDimensionPixelSize(R.dimen.all_apps_grid_view_start_margin);
        int allAppsCellWidthGap =
                res.getDimensionPixelSize(R.dimen.all_apps_icon_width_gap);
        int availableAppsWidthPx = (recyclerViewWidth > 0) ? recyclerViewWidth : availableWidthPx;
        int numAppsCols = (availableAppsWidthPx + allAppsCellWidthGap - appsViewLeftMarginPx) /
                (allAppsIconSizePx + allAppsCellWidthGap);
        int numPredictiveAppCols = Math.max(inv.minAllAppsPredictionColumns, numAppsCols);
        allAppsNumCols = numAppsCols;
        allAppsNumPredictiveCols = numPredictiveAppCols;
    }

    /** Returns the amount of extra space to allocate to the search bar for vertical padding. */
    private int getSearchBarTotalVerticalPadding() {
        return searchBarTopPaddingPx + searchBarTopExtraPaddingPx + searchBarBottomPaddingPx;
    }

    /** Returns the width and height of the search bar, ignoring any padding. */
    public Point getSearchBarDimensForWidgetOpts(Resources res) {
        Rect searchBarBounds = getSearchBarBounds(Utilities.isRtl(res));
        if (isVerticalBarLayout()) {
            return new Point(searchBarBounds.width(), searchBarBounds.height());
        }
        int widgetInternalPadding = searchBarWidgetInternalPaddingTop +
                searchBarWidgetInternalPaddingBottom;
        return new Point(searchBarBounds.width(), searchBarSpaceHeightPx + widgetInternalPadding);
    }

    /** Returns the search bar bounds in the current orientation */
    public Rect getSearchBarBounds(boolean isLayoutRtl) {
        Rect bounds = new Rect();
        if (isVerticalBarLayout()) {
            if (isLayoutRtl) {
                bounds.set(availableWidthPx - normalSearchBarSpaceHeightPx, edgeMarginPx,
                        availableWidthPx, availableHeightPx - edgeMarginPx);
            } else {
                bounds.set(0, edgeMarginPx, normalSearchBarSpaceHeightPx,
                        availableHeightPx - edgeMarginPx);
            }
        } else {
            int boundsBottom = searchBarSpaceHeightPx + getSearchBarTotalVerticalPadding();
            if (isTablet) {
                // Pad the left and right of the workspace to ensure consistent spacing
                // between all icons
                int width = getCurrentWidth();
                // XXX: If the icon size changes across orientations, we will have to take
                //      that into account here too.
                int gap = (int) ((width - 2 * edgeMarginPx -
                        (inv.numColumns * cellWidthPx)) / (2 * (inv.numColumns + 1)));
                bounds.set(edgeMarginPx + gap, 0,
                        availableWidthPx - (edgeMarginPx + gap), boundsBottom);
            } else {
                bounds.set(desiredWorkspaceLeftRightMarginPx - defaultWidgetPadding.left,
                        0,
                        availableWidthPx - (desiredWorkspaceLeftRightMarginPx -
                        defaultWidgetPadding.right), boundsBottom);
            }
        }
        return bounds;
    }

    /** Returns the workspace padding in the specified orientation */
    Rect getWorkspacePadding(boolean isLayoutRtl) {
        Rect searchBarBounds = getSearchBarBounds(isLayoutRtl);
        Rect padding = new Rect();
        if (isVerticalBarLayout()) {
            // Pad the left and right of the workspace with search/hotseat bar sizes
            if (isLayoutRtl) {
                padding.set(normalHotseatBarHeightPx, edgeMarginPx,
                        searchBarBounds.width(), edgeMarginPx);
            } else {
                padding.set(searchBarBounds.width(), edgeMarginPx,
                        normalHotseatBarHeightPx, edgeMarginPx);
            }
        } else {
            int paddingTop = searchBarBounds.bottom;
            int paddingBottom = hotseatBarHeightPx + pageIndicatorHeightPx;
            if (isTablet) {
                // Pad the left and right of the workspace to ensure consistent spacing
                // between all icons
                float gapScale = 1f + (dragViewScale - 1f) / 2f;
                int width = getCurrentWidth();
                int height = getCurrentHeight();
                // The amount of screen space available for left/right padding.
                int availablePaddingX = Math.max(0, width - (int) ((inv.numColumns * cellWidthPx) +
                        ((inv.numColumns - 1) * gapScale * cellWidthPx)));
                availablePaddingX = (int) Math.min(availablePaddingX,
                            width * MAX_HORIZONTAL_PADDING_PERCENT);
                int availablePaddingY = Math.max(0, height - paddingTop - paddingBottom
                        - (int) (2 * inv.numRows * cellHeightPx));
                padding.set(availablePaddingX / 2, paddingTop + availablePaddingY / 2,
                        availablePaddingX / 2, paddingBottom + availablePaddingY / 2);
            } else {
                // Pad the top and bottom of the workspace with search/hotseat bar sizes
                padding.set(desiredWorkspaceLeftRightMarginPx - defaultWidgetPadding.left,
                        0,
                        desiredWorkspaceLeftRightMarginPx - defaultWidgetPadding.right,
                        paddingBottom);
            }
        }
        return padding;
    }

    public int getWorkspacePageSpacing(boolean isLayoutRtl) {
        if (isVerticalBarLayout() || isLargeTablet) {
            // In landscape mode the page spacing is set to the default.
            return defaultPageSpacingPx;
        } else {
            // In portrait, we want the pages spaced such that there is no
            // overhang of the previous / next page into the current page viewport.
            // We assume symmetrical padding in portrait mode.
            return Math.max(defaultPageSpacingPx, 2 * getWorkspacePadding(isLayoutRtl).left);
        }
    }

    /**
     * added by liuning on 2017/7/25
     * @return
     */
    public int getWorkspaceLargePageSpacing(boolean isLayoutRtl) {
        if (isVerticalBarLayout() || isLargeTablet) {
            // In landscape mode the page spacing is set to the default.
            return defaultPageSpacingPx;
        } else {
            // In portrait, we want the pages spaced such that the previous / next page can not be seen
            float shrinkWidth = (widthPx - getWorkspacePadding(isLayoutRtl).left - getWorkspacePadding(isLayoutRtl).right) * mDragModeShrinkFactor;
            return (int) (widthPx - shrinkWidth) / 2 + edgeMarginPx;
        }
    }

    int getOverviewModeButtonBarHeight() {
        int zoneHeight = (int) (overviewModeIconZoneRatio * availableHeightPx);
        zoneHeight = Math.min(overviewModeMaxIconZoneHeightPx,
                Math.max(overviewModeMinIconZoneHeightPx, zoneHeight));
        return zoneHeight;
    }

    // The rect returned will be extended to below the system ui that covers the workspace
    Rect getHotseatRect() {
        if (isVerticalBarLayout()) {
            return new Rect(availableWidthPx - normalHotseatBarHeightPx, 0,
                    Integer.MAX_VALUE, availableHeightPx);
        } else {
            return new Rect(0, availableHeightPx - hotseatBarHeightPx,
                    availableWidthPx, Integer.MAX_VALUE);
        }
    }

    public static int calculateCellWidth(int width, int countX) {
        return width / countX;
    }
    public static int calculateCellHeight(int height, int countY) {
        return height / countY;
    }

    /**
     * When {@code true}, the device is in landscape mode and the hotseat is on the right column.
     * When {@code false}, either device is in portrait mode or the device is in landscape mode and
     * the hotseat is on the bottom row.
     */
    boolean isVerticalBarLayout() {
        return isLandscape && transposeLayoutWithOrientation;
    }

    public boolean shouldFadeAdjacentWorkspaceScreens() {
        return isVerticalBarLayout() || isLargeTablet;
    }

    private int getVisibleChildCount(ViewGroup parent) {
        int visibleChildren = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i).getVisibility() != View.GONE) {
                visibleChildren++;
            }
        }
        return visibleChildren;
    }

    // TODO(twickham): b/25154513
    public void setSearchBarHeight(int searchBarHeight) {
        if (searchBarHeight == LauncherCallbacks.SEARCH_BAR_HEIGHT_TALL) {
            hotseatBarHeightPx = shortHotseatBarHeightPx;
            searchBarSpaceHeightPx = tallSearchBarSpaceHeightPx;
            searchBarBottomPaddingPx = tallSearchBarBottomPaddingPx;
            searchBarTopExtraPaddingPx = isPhone ? tallSearchBarNegativeTopPaddingPx
                    : normalSearchBarTopExtraPaddingPx;
        } else {
            hotseatBarHeightPx = normalHotseatBarHeightPx;
            searchBarSpaceHeightPx = normalSearchBarSpaceHeightPx;
            searchBarBottomPaddingPx = normalSearchBarBottomPaddingPx;
            searchBarTopExtraPaddingPx = normalSearchBarTopExtraPaddingPx;
        }
    }

    public void layout(Launcher launcher) {
        FrameLayout.LayoutParams lp;
        boolean hasVerticalBarLayout = isVerticalBarLayout();
        final boolean isLayoutRtl = Utilities.isRtl(launcher.getResources());

        // Layout the search bar space
        Rect searchBarBounds = getSearchBarBounds(isLayoutRtl);
        View searchBar = launcher.getSearchDropTargetBar();
        lp = (FrameLayout.LayoutParams) searchBar.getLayoutParams();
        lp.width = searchBarBounds.width();
        lp.height = searchBarBounds.height();
        lp.topMargin = searchBarTopExtraPaddingPx;
        if (hasVerticalBarLayout) {
            // Vertical search bar space -- The search bar is fixed in the layout to be on the left
            //                              of the screen regardless of RTL
            lp.gravity = Gravity.LEFT;

            LinearLayout targets = (LinearLayout) searchBar.findViewById(R.id.drag_target_bar);
            targets.setOrientation(LinearLayout.VERTICAL);
            FrameLayout.LayoutParams targetsLp = (FrameLayout.LayoutParams) targets.getLayoutParams();
            targetsLp.gravity = Gravity.TOP;
            targetsLp.height = LayoutParams.WRAP_CONTENT;

        } else {
            // Horizontal search bar space
            lp.gravity = Gravity.TOP|Gravity.CENTER_HORIZONTAL;
        }
        searchBar.setLayoutParams(lp);

        // Layout the workspace
        PagedView workspace = (PagedView) launcher.findViewById(R.id.workspace);
        lp = (FrameLayout.LayoutParams) workspace.getLayoutParams();
        lp.gravity = Gravity.CENTER;
        Rect padding = getWorkspacePadding(isLayoutRtl);

        workspace.setLayoutParams(lp);
        workspace.setPadding(padding.left, padding.top, padding.right, padding.bottom);
        workspace.setPageSpacing(getWorkspacePageSpacing(isLayoutRtl));

        // Layout the hotseat
        View hotseat = launcher.findViewById(R.id.hotseat);
        lp = (FrameLayout.LayoutParams) hotseat.getLayoutParams();
        // We want the edges of the hotseat to line up with the edges of the workspace, but the
        // icons in the hotseat are a different size, and so don't line up perfectly. To account for
        // this, we pad the left and right of the hotseat with half of the difference of a workspace
        // cell vs a hotseat cell.
        float workspaceCellWidth = (float) getCurrentWidth() / inv.numColumns;
		//linhai update 保持热键盘 item宽度 不会岁坑位变化而变化 start 2017/7/14
        float hotseatCellWidth = (float) getCurrentWidth() / inv.numHotseatIconsMax;
        //linhai end 2017/7/14
        int hotseatAdjustment = Math.round((workspaceCellWidth - hotseatCellWidth) / 2);
        if (hasVerticalBarLayout) {
            // Vertical hotseat -- The hotseat is fixed in the layout to be on the right of the
            //                     screen regardless of RTL
            lp.gravity = Gravity.RIGHT;
            lp.width = normalHotseatBarHeightPx;
            lp.height = LayoutParams.MATCH_PARENT;
            hotseat.findViewById(R.id.layout).setPadding(0, 2 * edgeMarginPx, 0, 2 * edgeMarginPx);
        } else if (isTablet) {
            // Pad the hotseat with the workspace padding calculated above
            lp.gravity = Gravity.BOTTOM;
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = hotseatBarHeightPx;
            hotseat.findViewById(R.id.layout).setPadding(
                    hotseatAdjustment + padding.left + cellLayoutLeftRightPadding, 0,
                    hotseatAdjustment + padding.right + cellLayoutLeftRightPadding, 2 * edgeMarginPx);
        } else {
            // For phones, layout the hotseat without any bottom margin
            // to ensure that we have space for the folders
            lp.gravity = Gravity.BOTTOM;
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = hotseatBarHeightPx;
            //modify by chenchao 2017.8.2 start
            hotseat.findViewById(R.id.layout).setPadding(
                    hotseatAdjustment + padding.left+ cellLayoutLeftRightPadding, 0,
                    hotseatAdjustment + padding.right+cellLayoutLeftRightPadding, 0);
            //modify by chenchao 2017.8.2 end
        }
        hotseat.setLayoutParams(lp);

        // Layout the page indicators
        View pageIndicator = launcher.findViewById(R.id.page_indicator);
        if (pageIndicator != null) {
            if (hasVerticalBarLayout) {
                // Hide the page indicators when we have vertical search/hotseat
                pageIndicator.setVisibility(View.GONE);
            } else {
                // Put the page indicators above the hotseat
                lp = (FrameLayout.LayoutParams) pageIndicator.getLayoutParams();
                lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
                lp.width = LayoutParams.WRAP_CONTENT;
                lp.height = LayoutParams.WRAP_CONTENT;
                //modify by chenchao 2017.8.2 start
                //modify by chenchao from sunyingying 2017.12.15 start
                //下边距8dp，上边距10dp，之前是均分各9dp，因此要往下挪1dp，所以减去了一个6px
                lp.bottomMargin = hotseatBarHeightPx + (pageIndicatorHeightPx - pageIndicatorMarkPx - pageIndicatorOffSetPx) / 2;
                //modify by chenchao 2017.12.15 end
                //modify by chenchao 2017.8.2 end
                pageIndicator.setLayoutParams(lp);
            }
        }

        // Layout the Overview Mode
        ViewGroup overviewMode = launcher.getOverviewPanel();
        if (overviewMode != null) {
            int overviewButtonBarHeight = getOverviewModeButtonBarHeight();
            lp = (FrameLayout.LayoutParams) overviewMode.getLayoutParams();
            lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
            overviewMode.setLayoutParams(lp);
            //linhai 自适应适配  start 2017/6/16
//            int visibleChildCount = getVisibleChildCount(overviewMode);
//            int totalItemWidth = visibleChildCount * overviewModeBarItemWidthPx;
//            int maxWidth = totalItemWidth + (visibleChildCount-1) * overviewModeBarSpacerWidthPx;
//
//            lp.width = Math.min(availableWidthPx, maxWidth);
//            lp.height = overviewButtonBarHeight;

//            if (lp.width > totalItemWidth && visibleChildCount > 1) {
//                // We have enough space. Lets add some margin too.
//                int margin = (lp.width - totalItemWidth) / (visibleChildCount-1);
//                View lastChild = null;
//
//                // Set margin of all visible children except the last visible child
//                for (int i = 0; i < visibleChildCount; i++) {
//                    if (lastChild != null) {
//                        MarginLayoutParams clp = (MarginLayoutParams) lastChild.getLayoutParams();
//                        if (isLayoutRtl) {
//                            clp.leftMargin = margin;
//                        } else {
//                            clp.rightMargin = margin;
//                        }
//                        lastChild.setLayoutParams(clp);
//                        lastChild = null;
//                    }
//                    View thisChild = overviewMode.getChildAt(i);
//                    if (thisChild.getVisibility() != View.GONE) {
//                        lastChild = thisChild;
//                    }
//                }
//            }
          //  by linhai end on 2017/6/19
        }
    }

    private int getCurrentWidth() {
        return isLandscape
                ? Math.max(widthPx, heightPx)
                : Math.min(widthPx, heightPx);
    }

    private int getCurrentHeight() {
        return isLandscape
                ? Math.min(widthPx, heightPx)
                : Math.max(widthPx, heightPx);
    }
}
