package com.gome.launcher.move;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gome.launcher.CellLayout;
import com.gome.launcher.DeviceProfile;
import com.gome.launcher.DragController;
import com.gome.launcher.DragLayer;
import com.gome.launcher.DragSource;
import com.gome.launcher.DragView;
import com.gome.launcher.DropTarget;
import com.gome.launcher.Folder;
import com.gome.launcher.FolderIcon;
import com.gome.launcher.FolderInfo;
import com.gome.launcher.IconCache;
import com.gome.launcher.ItemInfo;
import com.gome.launcher.Launcher;
import com.gome.launcher.LauncherAppState;
import com.gome.launcher.LauncherModel;
import com.gome.launcher.LauncherSettings;
import com.gome.launcher.MoveDropTarget;
import com.gome.launcher.R;
import com.gome.launcher.ShortcutInfo;
import com.gome.launcher.Workspace;
import com.gome.launcher.compat.UserHandleCompat;
import com.gome.launcher.util.DLog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;


/**
 * Created by liuning on 2017/7/1.
 */

public class MoveViewsRestContainer extends FrameLayout implements DragController.DragListener, DropTarget, DragSource,
        View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = "MoveViewRestContainer";

    private static int DRAG_VIEW_DROP_DURATION = 400;
    private Launcher mLauncher;
    private DragController mDragController;
    private MoveViewsAdapter mAdapter;
    private MoveViewsRecyclerView mRecycleView;
    private TextView mHint;
    private IconCache mIconCache;
    private View mCurrentDragView;
    private Rect mOriginalRect = new Rect();
    private int mOriginalPosition;
    private boolean mDragOutFailed = false;


    public MoveViewsRestContainer(Context context) {
        this(context, null);
    }

    public MoveViewsRestContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MoveViewsRestContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher) context;
        mDragController = mLauncher.getDragController();
    }

    public void setAdapterItemAddListener(AdapterItemAddListener adapterItemAddListener) {
        mAdapter.setAdapterItemAddListener(adapterItemAddListener);
    }

    @Override
    protected void onFinishInflate() {
        DLog.e(TAG,"MoveViewRestContainer onFinishInflate() begin ====================");
        super.onFinishInflate();

        mRecycleView = (MoveViewsRecyclerView) findViewById(R.id.move_view_list);
        mHint = (TextView) findViewById(R.id.move_more_app_hint);
        mHint.setVisibility(INVISIBLE);

        mAdapter = new MoveViewsAdapter(mRecycleView, getContext(), this, this,mLauncher);
        mRecycleView.setAdapter(mAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        // set item display Orientation
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        layoutManager.setSmoothScrollbarEnabled(true);
        mRecycleView.setLayoutManager(layoutManager);
        mIconCache = (LauncherAppState.getInstance()).getIconCache();
        DLog.e(TAG,"MoveViewRestContainer onFinishInflate() end====================");
    }

    public void show() {
        setVisibility(VISIBLE);
        mHint.setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(INVISIBLE);
    }

    public void addItemFirst(ItemInfo info) {
        //when add more than one item,the mHint disappear
        if (!mAdapter.isMoveListEmpty()) {
            mHint.setVisibility(INVISIBLE);
        }
        mAdapter.addItem(info, 0);
        mRecycleView.scrollToPosition(0);
    }

    public void addItemPos(ItemInfo info, int positon) {
        //when add more than one item,the mHint disappear
        if (!mAdapter.isMoveListEmpty()) {
            mHint.setVisibility(INVISIBLE);
        }
        mAdapter.addItem(info, positon);
        if (positon == 0) {
            mRecycleView.scrollToPosition(0);
        }

    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
    }

    @Override
    public void onDragEnd() {
        /**
         * Added by gaoquan 2018.03.15
         * fix 	OS2X-13872【Launcher】带有角标的文件夹拖动至移动区域后，有的显示角标，有的不显示
         */
        //-------------------------------start--------------///
        mLauncher.getWorkspace().updateShortcutsAndFoldersUnread();
        //-------------------------------end--------------///
    }


    @Override
    public boolean isDropEnabled() {
        if (!mLauncher.getWorkspace().isInAppManageMode()) {
            return false;
        }
        if (getVisibility() != View.VISIBLE) {
            return false;
        }
        return true;
    }

    @Override
    public void onDragEnter(DragObject dragObject) {
    }

    @Override
    public void onDragOver(DragObject dragObject) {
    }

    @Override
    public void onDragExit(DragObject dragObject) {

    }

    @Override
    public void onFlingToDelete(DragObject dragObject, PointF vec) {
        // Do nothing
    }

    @Override
    public boolean acceptDrop(DragObject dragObject) {
        if (dragObject.dragInfo instanceof ShortcutInfo || dragObject.dragInfo instanceof FolderInfo) {
            return true;
        }
        mLauncher.getWorkspace().onDrop(dragObject);
        return false;
    }

    @Override
    public void prepareAccessibilityDrop() {
        // Do nothing
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        super.getHitRect(outRect);

        int[] coords = new int[2];
        mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, coords);
        outRect.offsetTo(coords[0], coords[1]);
    }

    @Override
    public void getLocationInDragLayer(int[] loc) {
        mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
    }

    @Override
    public boolean supportsFlingToDelete() {
        return false;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        return false;
    }

    @Override
    public boolean supportsDeleteDropTarget() {
        return false;
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        DeviceProfile grid = mLauncher.getDeviceProfile();
        return grid.moveIconSizePx * 1.0f / mAdapter.getItemWidth();
    }

    @Override
    public void onFlingToDeleteCompleted() {
        // Do nothing
    }

    @Override
    public void onDropCompleted(View target, DragObject d, boolean isFlingToDelete, boolean success) {
        if (!success) {
            mDragOutFailed = true;
            onDrop(d);
        }

        if (mAdapter.isMoveListEmpty()) {
            mLauncher.exitAppManageMode();
        }
    }

    @Override
    public void onDrop(DragObject dragObject) {
        DragLayer dragLayer = mLauncher.getDragLayer();
        DragView animateView = dragObject.dragView;
        ItemInfo info = (ItemInfo) dragObject.dragInfo;
        Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(animateView, from);
        if (mDragOutFailed) {
            mAdapter.addItem(info, mOriginalPosition);
            mCurrentDragView.setVisibility(INVISIBLE);
            Rect to = mOriginalRect;
            final float scale = (float) to.width() / from.width();
            int offLeft = (int) (animateView.getWidth() * (-1 + scale) / 2.0f);
            int offTop = (int) (animateView.getHeight() * (-1 + scale) / 2.0f);
            to.offset(offLeft, offTop);
            Runnable onAnimationEndRunnable = new Runnable() {
                @Override
                public void run() {
                    mCurrentDragView.setVisibility(VISIBLE);
                }
            };

            dragLayer.animateView(animateView, from, to, 1,
                    1, 1, scale, scale, DRAG_VIEW_DROP_DURATION,
                    new DecelerateInterpolator(2), new AccelerateInterpolator(2),
                    onAnimationEndRunnable, DragLayer.ANIMATION_END_DISAPPEAR, null);
            mDragOutFailed = false;
        } else {
            float[] mDragViewVisualCenter = new float[2];
            mDragViewVisualCenter = dragObject.getVisualCenter(mDragViewVisualCenter);
            int position = pointToPosition((int) mDragViewVisualCenter[0] + animateView.getWidth() / 2, (int) mDragViewVisualCenter[1]);
            addItemPos(info, position);
            //没做drop animation，应将deferDragViewCleanupPostAnimation设为false，否则不会remove the DragView,也不会调onDragEnd()
            dragObject.deferDragViewCleanupPostAnimation = false;
            dragLayer.removeView(animateView);
            if (dragObject.dragSource instanceof MoveDropTarget.MoveSource) {
                ((MoveDropTarget.MoveSource) dragObject.dragSource).onDropToMoveTarget();
            }
        }

    }

    /*
    * Convert the 2D coordinate xy from the parent View's coordinate space to this MoveViewsRestContainer's
    * coordinate space. The argument xy is modified with the return result.
    */
    void mapPointFromSelfToChild(View v, float[] xy) {
        xy[0] = xy[0] - v.getLeft();
        xy[1] = xy[1] - v.getTop();
    }

    public int pointToPosition(int x, int y) {
        Log.v("ln","liuning "+"pointToPosition  x:  "+x +"y:  "+y);
        int res = mRecycleView.pointToPosition(x, y);
        if (res != MoveViewsRecyclerView.INVALID_POSITION) {
            return res;
        }
        if (x < 0) {
            return mRecycleView.getFirstVisiblePosition();
        }
        return mRecycleView.getLastVisiblePosition() + 1;
    }

    public Rect getIconRectByPosition(int position) {
        Log.v("ln", "liuning " + "getIconRectByPosition  position:  " + position);
        Rect rect = new Rect();
        View v = mRecycleView.getViewByPosition(position);
        if (v != null) {
            mLauncher.getDragLayer().getViewRectRelativeToSelf(v, rect);
            mapRectFromViewToIcon(rect);
        }
        return rect;
    }


    public void mapRectFromViewToIcon(Rect rect) {
        DeviceProfile grid = mLauncher.getDeviceProfile();

        int offLeft = (mAdapter.getItemWidth() - grid.moveIconSizePx) / 2;
        int offRight = (grid.moveIconSizePx - mAdapter.getItemWidth()) / 2;
        int offTop = (mAdapter.getItemHeight() - grid.moveIconSizePx) / 2;
        int offBottom = (grid.moveIconSizePx - mAdapter.getItemHeight()) / 2;
        rect.set(rect.left + offLeft, rect.top + offTop, rect.right + offRight, rect.bottom + offBottom);

    }

    @Override
    public void onClick(View v) {
        ItemInfo info = (ItemInfo) v.getTag();

        if (!mAdapter.isContainItem(info)) {
            return;
        }

        Folder openFolder = mLauncher.getWorkspace() != null ? mLauncher.getWorkspace().getOpenFolder() : null;
        if (openFolder != null) {
            if (info instanceof ShortcutInfo) {
                //update by huanghaihao in 2017-7-24 for adding more app in folder start
                mAdapter.deleteItem(v);
                if (mAdapter.isMoveListEmpty()) {
                    mLauncher.exitAppManageMode();
                }
                openFolder.getFolderIcon().addItem((ShortcutInfo) info);
                //update by huanghaihao in 2017-7-24 for adding more app in folder end
                return;
            } else {
                mLauncher.closeFolder();
            }
        }
        if (addItemToWorkSpace(info, true)) {
            mAdapter.deleteItem(v);
            if (mAdapter.isMoveListEmpty()) {
                mLauncher.exitAppManageMode();
            }
        }
        /**
         * Added by gaoquan 2018.03.15
         * fix 	OS2X-13872【Launcher】带有角标的文件夹拖动至移动区域后，有的显示角标，有的不显示
         */
        //-------------------------------start--------------///
        mLauncher.getWorkspace().updateShortcutsAndFoldersUnread();
        //-------------------------------end--------------///
    }

    @Override
    public boolean onLongClick(View v) {
        Workspace workspace = mLauncher.getWorkspace();
        if (!workspace.isInAppManageMode()) {
            return false;
        }
        // Return if global dragging is not enabled
        if (!mLauncher.isDraggingEnabled()) {
            return true;
        }

        mLauncher.getDragLayer().getViewRectRelativeToSelf(v, mOriginalRect);
        mapRectFromViewToIcon(mOriginalRect);

        return beginDrag(v, false);
    }

    private boolean beginDrag(View v, boolean accessible) {
        if (!v.isInTouchMode()) {
            return false;
        }
        ItemInfo info = (ItemInfo) v.getTag();
        mLauncher.getWorkspace().beginDragShared(v, new Point(), this, accessible);

        Folder openFolder = mLauncher.getWorkspace() != null ? mLauncher.getWorkspace().getOpenFolder() : null;
        if (openFolder != null) {
            if (info instanceof ShortcutInfo) {
                openFolder.beginExternalDrag((ShortcutInfo) (v.getTag()));
            } else {
                mLauncher.closeFolder();
            }
        }

        mCurrentDragView = v;
        mOriginalPosition = mAdapter.getItemPos((ItemInfo) mCurrentDragView.getTag());
        mAdapter.deleteItem(v);

        return true;
    }

    /**
     * Remove item from CellLayout or folder in the WorkSpace
     * @param
     */
    public void removeViewFromWorkSpace(View v) {
        ItemInfo itemInfo = (ItemInfo) v.getTag();
        //mLauncher.removeItem(v, itemInfo, false);

        if (itemInfo instanceof ShortcutInfo) {
            // Remove the shortcut from the folder before removing it from launcher
            FolderInfo folderInfo = mLauncher.getsFolders().get(itemInfo.container);
            if (folderInfo != null) {
                itemInfo.container = LauncherSettings.Favorites.CONTAINER_MOVE;
                folderInfo.remove((ShortcutInfo) itemInfo);
            } else {
                mLauncher.getWorkspace().removeWorkspaceItem(v);
            }

        } else if (itemInfo instanceof FolderInfo) {
            final FolderInfo folderInfo = (FolderInfo) itemInfo;
            mLauncher.unbindFolder(folderInfo);
            mLauncher.getWorkspace().removeWorkspaceItem(v);
        }
    }

    public void deleteItemByComponent(final HashSet<ComponentName> componentNames,
                                      final UserHandleCompat user) {
        Iterator<ItemInfo> iter = mAdapter.getMoveItemInfos().iterator();
        while (iter.hasNext()) {
            ItemInfo info = iter.next();
            if (info instanceof ShortcutInfo) {
                if (componentNames.contains(info.getIntent().getComponent()) && info.user.equals(user)) {
                    int pos = mAdapter.getItemPos(info);
                    iter.remove();
                    mAdapter.notifyItemRemoved(pos);
                    if (mAdapter.isMoveListEmpty()) {
                        mLauncher.exitAppManageMode();
                    }
                }
            } else if (info instanceof FolderInfo) {
                FolderInfo folderInfo = (FolderInfo) info;
                Iterator<ShortcutInfo> iter1 = folderInfo.contents.iterator();
                while (iter1.hasNext()) {
                    ShortcutInfo s = iter1.next();
                    if (componentNames.contains(s.getIntent().getComponent()) && s.user.equals(user)) {
                        iter1.remove();
                        folderInfo.onRemove(s);
                        int pos = mAdapter.getItemPos(folderInfo);
                        if (folderInfo.contents.size() == 1) {
                            ShortcutInfo shortcutInfo = folderInfo.contents.get(0);
                            iter.remove();
                            mAdapter.notifyItemRemoved(pos);
                            mAdapter.addItem(shortcutInfo, pos);
                            LauncherModel.deleteFolderAndContentsFromDatabase(mLauncher, folderInfo);
                        } else {
                            mAdapter.notifyItemChanged(pos);
                        }
                    }
                }
            }
        }
    }

    public boolean isMoveListEmpty() {
        return mAdapter.isMoveListEmpty();
    }

    public ArrayList<ItemInfo> getMoveItemInfos() {
        return mAdapter.getMoveItemInfos();
    }

    public void placeAllItemBack() {
        for (ItemInfo info : mAdapter.getMoveItemInfos()) {
            addItemToWorkSpace(info, false);
        }
        mAdapter.clearMoveItemInfos();
    }

    /**
     * Add Item to CellLayout in WorkSpace
     *
     * @param toCurScreen whether to put the item only in the current screen
     */
    public boolean addItemToWorkSpace(ItemInfo info, boolean toCurScreen) {
        final Workspace workspace = mLauncher.getWorkspace();
        if (toCurScreen) {
            int[] vacantCell = new int[2];
            if (!findVacantCellInCurScreen(vacantCell)) {
                Toast.makeText(getContext(), R.string.out_of_space, Toast.LENGTH_SHORT).show();
                return false;
            } else {
                info.screenId = workspace.getScreenIdForPageIndex(workspace.getCurrentPage());
                info.cellX = vacantCell[0];
                info.cellY = vacantCell[1];
            }
        } else {
            Pair<Long, int[]> coords = findVacantCellFromCurScreen();
            info.screenId = coords.first;
            int[] vacantCell = coords.second;
            info.cellX = vacantCell[0];
            info.cellY = vacantCell[1];
        }
        info.spanX = 1;
        info.spanY = 1;
        info.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
        View view = null;
        CellLayout cellLayout = (CellLayout) workspace.getChildAt(workspace.getPageIndexForScreenId(info.screenId));
        if (info instanceof ShortcutInfo) {
            view = mLauncher.createShortcut(cellLayout, (ShortcutInfo) info);
        } else if (info instanceof FolderInfo) {
            ((FolderInfo) info).unbind();
            view = FolderIcon.fromXml(R.layout.folder_icon, mLauncher, cellLayout,
                    (FolderInfo) info, mIconCache);
        }

        LauncherModel.updateLauncherModelWorkSpaceItemList(info, true);
        LauncherModel.updateLauncherModelBgItemsIdMap(info, false);
        workspace.addInScreenFromBind(view, info.container, info.screenId, info.cellX,
                info.cellY, info.spanX, info.spanY);


        LauncherModel.moveItemInDatabase(mLauncher, info, info.container,info.screenId, info.cellX,
                info.cellY);
        return true;
    }

    /**
     * For a item to find the empty position int current screen
     *
     * @param vacantCell
     * @return
     */
    public boolean findVacantCellInCurScreen(int[] vacantCell) {
        Workspace workspace = mLauncher.getWorkspace();
        int index = workspace.getCurrentPage();
        long screenId = workspace.getScreenIdForPageIndex(index);
        if (screenId == Workspace.EXTRA_EMPTY_SCREEN_ID) {
            screenId = workspace.commitExtraEmptyScreen();
        }
        CellLayout cellLayout = workspace.getScreenWithId(screenId);
        if (cellLayout != null) {
            return (cellLayout.findCellForSpan(vacantCell, 1, 1));
        }
        return false;
    }

    /**
     * For a item to find the empty position from current screen
     *
     * @return
     */
    public Pair<Long, int[]> findVacantCellFromCurScreen() {
        Workspace workspace = mLauncher.getWorkspace();
        int[] vacantCell = new int[2];
        long currentId = 0;
        if (workspace != null) {
            int index = workspace.getCurrentPage();
            CellLayout cellLayout;
            for (int i = index; i < 50; i++) {
                currentId = workspace.getScreenIdForPageIndex(i);
                if (currentId == Workspace.EXTRA_EMPTY_SCREEN_ID) {
                    currentId = workspace.commitExtraEmptyScreen();
                }
                cellLayout = workspace.getScreenWithId(currentId);
                if (cellLayout != null) {
                    if (cellLayout.findVacantCell(1, 1, vacantCell)) {
                        break;
                    }
                } else {
                    workspace.addExtraEmptyScreen();
                    workspace.commitExtraEmptyScreen();
                    --i;
                }
            }
        }
        return Pair.create(currentId, vacantCell);
    }
}
