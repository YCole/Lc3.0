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

package com.gome.launcher.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.State;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.gome.launcher.BaseContainerView;
import com.gome.launcher.CellLayout;
import com.gome.launcher.DeleteDropTarget;
import com.gome.launcher.DeviceProfile;
import com.gome.launcher.DragController;
import com.gome.launcher.DragSource;
import com.gome.launcher.Folder;
import com.gome.launcher.IconCache;
import com.gome.launcher.ItemInfo;
import com.gome.launcher.Launcher;
import com.gome.launcher.LauncherAnimUtils;
import com.gome.launcher.LauncherAppState;
import com.gome.launcher.LauncherSettings;
import com.gome.launcher.PendingAddItemInfo;
import com.gome.launcher.R;
import com.gome.launcher.SearchDropTargetBar;
import com.gome.launcher.Utilities;
import com.gome.launcher.WidgetPreviewLoader;
import com.gome.launcher.Workspace;
import com.gome.launcher.model.WidgetsModel;
import com.gome.launcher.util.DLog;
import com.gome.launcher.util.Thunk;
import com.gome.launcher.DropTarget;


/**
 * The widgets list view container.
 * move and mod from gm02 launcher by rongwenzhao
 * date: 2017-6-27
 */
public class WidgetsContainerViewGM extends BaseContainerView
        implements View.OnLongClickListener, View.OnClickListener, DragSource {

    private static final String TAG = "WidgetsContainerViewGM";
    private static final boolean DEBUG = false;

    /* Coefficient multiplied to the screen height for preloading widgets. */
    private static final int PRELOAD_SCREEN_HEIGHT_MULTIPLE = 1;

    /* Global instances that are used inside this container. */
    @Thunk
    Launcher mLauncher;
    private DragController mDragController;
    private IconCache mIconCache;

    /* Recycler view related member variables */
    //private View mContent;
    private WidgetsRecyclerView mView;
    private WidgetsListAdapterGM mAdapter;

    /* Touch handling related member variables. */
    private Toast mWidgetInstructionToast;

    /* Rendering related. */
    private WidgetPreviewLoader mWidgetPreviewLoader;

    private Rect mPadding = new Rect();

    public WidgetsContainerViewGM(Context context) {
        this(context, null);
    }

    public WidgetsContainerViewGM(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetsContainerViewGM(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher) context;
        mDragController = mLauncher.getDragController();
        mAdapter = new WidgetsListAdapterGM(context, this, this, mLauncher);
        mIconCache = (LauncherAppState.getInstance()).getIconCache();
        if (DEBUG) {
            DLog.d(TAG, "WidgetsContainerViewGM constructor");
        }
    }

    @Override
    protected void onFinishInflate() {
        DLog.e(TAG,"WidgetContainerView onFinishInflate() begin ====================");
        super.onFinishInflate();

        mView = (WidgetsRecyclerView) findViewById(R.id.widgets_list_view);
        mView.setVisibility(View.VISIBLE);
        mAdapter.setmContentView(getContentView());
        mView.addItemDecoration(new SpaceItemDecoration(getResources().getDimensionPixelSize(R.dimen.widget_cell_margin)));
        mView.setAdapter(mAdapter);

        // This extends the layout space so that preloading happen for the {@link RecyclerView}
        //zhaosuzhou add note start {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext()) {
            @Override
            protected int getExtraLayoutSpace(State state) {
                DeviceProfile grid = mLauncher.getDeviceProfile();
                return super.getExtraLayoutSpace(state)
                        + grid.availableWidthPx * PRELOAD_SCREEN_HEIGHT_MULTIPLE;
            }
        };
        // set item display Orientation
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        layoutManager.setSmoothScrollbarEnabled(false);

        // } end
        mView.setLayoutManager(layoutManager);
        mPadding.set(getPaddingLeft(), getPaddingTop(), getPaddingRight(),
                getPaddingBottom());
        DLog.e(TAG,"WidgetContainerView onFinishInflate() end====================");
    }

    public void scrollToTop() {
        mView.scrollToPosition(0);
    }

    //
    // Touch related handling.
    //
    @Override
    public void onClick(View v) {
        // When we have exited widget tray or are in transition, disregard clicks
        if (((!mLauncher.isWidgetsViewVisible())&& (!mLauncher.isSecondLevelWidgetVisible())) //mod by rongwenzhao
                || mLauncher.getWorkspace().isSwitchingState()
                || !(v instanceof WidgetCell)) return;

        Workspace mWorkSpace = mLauncher.getWorkspace();
        long screenId = mWorkSpace.getScreenIdForPageIndex(mWorkSpace.getCurrentPage());
        CellLayout cellLayout = mWorkSpace.getScreenWithId(screenId);
        PendingAddItemInfo pendingAddItemInfo = (PendingAddItemInfo)v.getTag();
        int[] vacantCell = new int[2];
        boolean vacantCellBoolean;
        vacantCellBoolean = cellLayout.findVacantCell(pendingAddItemInfo.spanX, pendingAddItemInfo.spanY, vacantCell);
        if(vacantCellBoolean){
            mLauncher.addPendingItem(pendingAddItemInfo, LauncherSettings.Favorites.CONTAINER_DESKTOP, screenId, vacantCell,
                    pendingAddItemInfo.spanX, pendingAddItemInfo.spanY);
        }else{
//            if (mWidgetInstructionToast != null) {
//                mWidgetInstructionToast.cancel();
//            }
//            /*mWidgetInstructionToast = Toast.makeText(getContext(),R.string.out_of_space,
//                    Toast.LENGTH_SHORT);*/
//            mWidgetInstructionToast = Utilities.getCustomWidget(getContext(),R.string.out_of_space);
//            mWidgetInstructionToast.show();
            //add by linhai show no space view start 2017/10/17
            SearchDropTargetBar searchDropTargetBar = mLauncher.getSearchDropTargetBar();
            if (searchDropTargetBar != null)
            {
                searchDropTargetBar.toastShow();
            }
            //end linhai show no space view start 2017/10/17
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (DEBUG) {
            DLog.d(TAG, String.format("onLonglick [v=%s]", v));
        }
        // Return early if this is not initiated from a touch
        if (!v.isInTouchMode()) return false;
        // When we have exited all apps or are in transition, disregard long clicks
        if (((!mLauncher.isWidgetsViewVisible())&& (!mLauncher.isSecondLevelWidgetVisible())) //mod by rongwenzhao
                ||mLauncher.getWorkspace().isSwitchingState()) return false;
        // Return if global dragging is not enabled
        DLog.d(TAG, String.format("onLonglick dragging enabled?.", v));
        if (!mLauncher.isDraggingEnabled()) return false;

        boolean status = beginDragging(v);
        if (status && v.getTag() instanceof PendingAddWidgetInfo) {
            WidgetHostViewLoader hostLoader = new WidgetHostViewLoader(mLauncher, v);
            // Added by wjq in 2017-09-27 for fix bug GMOS-8755 start
            PendingAddItemInfo pendingAddItemInfo = (PendingAddItemInfo) v.getTag();
            if (!pendingAddItemInfo.componentName.getPackageName().equals("com.ss.android.article.news")) {
                boolean preloadStatus = hostLoader.preloadWidget();
                if (DEBUG) {
                    DLog.d(TAG, String.format("preloading widget [status=%s]", preloadStatus));
                }
            }
            // Added by wjq in 2017-09-27 for fix bug GMOS-8755 end
            mLauncher.getDragController().addDragListener(hostLoader);
        }

        return status;
    }

    private boolean beginDragging(View v) {
        if (v instanceof WidgetCell) {
            if (!beginDraggingWidget((WidgetCell) v)) {
                return false;
            }
        } else {
            DLog.e(TAG, "Unexpected dragging view: " + v);
        }

        // We don't enter spring-loaded mode if the drag has been cancelled
        if (mLauncher.getDragController().isDragging()) {
            // Go into spring loaded mode (must happen before we startDrag())
            mLauncher.enterSpringLoadedDragMode();
        }

        return true;
    }

    private boolean beginDraggingWidget(WidgetCell v) {
        // Get the widget preview as the drag representation
        WidgetImageView image = (WidgetImageView) v.findViewById(R.id.widget_preview);
        PendingAddItemInfo createItemInfo = (PendingAddItemInfo) v.getTag();

        // If the ImageView doesn't have a drawable yet, the widget preview hasn't been loaded and
        // we abort the drag.
        if (image.getBitmap() == null) {
            return false;
        }

        // Compose the drag image
        Bitmap preview;
        float scale = 1f;
        final Rect bounds = image.getBitmapBounds();

        if (createItemInfo instanceof PendingAddWidgetInfo) {
            // This can happen in some weird cases involving multi-touch. We can't start dragging
            // the widget if this is null, so we break out.

            PendingAddWidgetInfo createWidgetInfo = (PendingAddWidgetInfo) createItemInfo;
            int[] size = mLauncher.getWorkspace().estimateItemSize(createWidgetInfo, true);

            Bitmap icon = image.getBitmap();
            float minScale = 1.25f;
            int maxWidth = Math.min((int) (icon.getWidth() * minScale), size[0]);
            int maxHeight = Math.min((int) (icon.getHeight() * minScale) , size[1]); //add by rongwenzhao 2017-7-7

            int[] previewSizeBeforeScale = new int[2];//mod by rongwenzhao from int[1] to int[2] 2017-7-7
            preview = getWidgetPreviewLoader().generateWidgetPreview(mLauncher,
                    createWidgetInfo.info, maxWidth, maxHeight, null, previewSizeBeforeScale);//mod by rongwenzhao 2017-7-7

            if (previewSizeBeforeScale[0] < icon.getWidth()) {
                // The icon has extra padding around it.
                int padding = (icon.getWidth() - previewSizeBeforeScale[0]) / 2;
                if (icon.getWidth() > image.getWidth()) {
                    padding = padding * image.getWidth() / icon.getWidth();
                }

                bounds.left += padding;
                bounds.right -= padding;
            }
            scale = bounds.width() / (float) preview.getWidth();
        } else {
            PendingAddShortcutInfo createShortcutInfo = (PendingAddShortcutInfo) v.getTag();
            Drawable icon = mIconCache.getFullResIcon(createShortcutInfo.activityInfo);
            preview = Utilities.createIconBitmap(icon, mLauncher);
            createItemInfo.spanX = createItemInfo.spanY = 1;
            scale = ((float) mLauncher.getDeviceProfile().iconSizePx) / preview.getWidth();
        }

        // Don't clip alpha values for the drag outline if we're using the default widget preview
        boolean clipAlpha = !(createItemInfo instanceof PendingAddWidgetInfo &&
                (((PendingAddWidgetInfo) createItemInfo).previewImage == 0));

        // Start the drag
        mLauncher.lockScreenOrientation();
        mLauncher.getWorkspace().onDragStartedWithItem(createItemInfo, preview, clipAlpha);
        mDragController.startDrag(image, preview, this, createItemInfo,
                bounds, DragController.DRAG_ACTION_COPY, scale);

        preview.recycle();
        return true;
    }

    //
    // Drag related handling methods that implement {@link DragSource} interface.
    //

    @Override
    public boolean supportsFlingToDelete() {
        return false;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        return false;
    }

    /*
     * Both this method and {@link #supportsFlingToDelete} has to return {@code false} for the
     * {@link DeleteDropTarget} to be invisible.)
     */
    @Override
    public boolean supportsDeleteDropTarget() {
        return true;
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        return 0;
    }

    @Override
    public void onFlingToDeleteCompleted() {
        // We just dismiss the drag when we fling, so cleanup here
        mLauncher.exitSpringLoadedDragModeDelayed(true,
                Launcher.EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
        mLauncher.unlockScreenOrientation(false);
    }

    @Override
    public void onDropCompleted(View target, DropTarget.DragObject d, boolean isFlingToDelete,
                                boolean success) {
        if (isFlingToDelete || !success || (target != mLauncher.getWorkspace() &&
                !(target instanceof DeleteDropTarget) && !(target instanceof Folder))) {
            // Exit spring loaded mode if we have not successfully dropped or have not handled the
            // drop in Workspace
            mLauncher.exitSpringLoadedDragModeDelayed(true,
                    Launcher.EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
        }
        mLauncher.unlockScreenOrientation(false);

        // Display an error message if the drag failed due to there not being enough space on the
        // target layout we were dropping on.
        if (!success) {
            boolean showOutOfSpaceMessage = false;
            if (target instanceof Workspace) {
                int currentScreen = mLauncher.getCurrentWorkspaceScreen();
                Workspace workspace = (Workspace) target;
                CellLayout layout = (CellLayout) workspace.getChildAt(currentScreen);
                ItemInfo itemInfo = (ItemInfo) d.dragInfo;
                if (layout != null) {
                    showOutOfSpaceMessage =
                            !layout.findCellForSpan(null, itemInfo.spanX, itemInfo.spanY);
                }
            }
            if (showOutOfSpaceMessage) {
                mLauncher.showWidgetOutOfSpaceMessage();
            }
            d.deferDragViewCleanupPostAnimation = false;
        }
    }

    //
    // Container rendering related.
    //
    @Override
    protected void onUpdateBgPadding(Rect padding, Rect bgPadding) {
        if (Utilities.isRtl(getResources())) {
            getContentView().setPadding(0, bgPadding.top,
                    bgPadding.right, bgPadding.bottom);
            mView.updateBackgroundPadding(new Rect(bgPadding.left, 0, 0, 0));
        } else {
            getContentView().setPadding(bgPadding.left, bgPadding.top,
                    0, bgPadding.bottom);
            mView.updateBackgroundPadding(new Rect(0, 0, bgPadding.right, 0));
        }
    }

    /**
     * Initialize the widget data model.
     */
    public void addWidgets(WidgetsModel model) {
        mView.setWidgets(model);
        mAdapter.setWidgetsModel(model);
        mAdapter.notifyDataSetChanged();
        //add by rongwenzhao for bug begin: delete app with widget in widgetlist, when in second widget level , back to widgetlist level
        if(mLauncher.isSecondLevelWidgetVisible()){
            LauncherAnimUtils.fadeAlphaInOrOut(getContentView().findViewById(R.id.widgets_scroll_container),false,300,0);
            LauncherAnimUtils.fadeAlphaInOrOut(getContentView().findViewById(R.id.widgets_list_view),true,300,300);
            LinearLayout widgetListContainer = (LinearLayout)getContentView().findViewById(R.id.widgets_cell_list);
            if(widgetListContainer.getChildCount() > 0){
                widgetListContainer.removeAllViews();
            }
        }
        //add by rongwenzhao for bug end: delete app with widget in widgetlist, when in second widget level , back to widgetlist level
    }

    public boolean isEmpty() {
        return mAdapter.getItemCount() == 0;
    }

    private WidgetPreviewLoader getWidgetPreviewLoader() {
        if (mWidgetPreviewLoader == null) {
            mWidgetPreviewLoader = LauncherAppState.getInstance().getWidgetCache();
        }
        return mWidgetPreviewLoader;
    }

    /**
     * 设置RecyclerView的间距的类
     */
    class SpaceItemDecoration extends RecyclerView.ItemDecoration{
        private int space;

        public SpaceItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            DLog.d(TAG,"SpaceItemDecoration == position = " + parent.getChildPosition(view));
            outRect.right = space;
        }
    }
}