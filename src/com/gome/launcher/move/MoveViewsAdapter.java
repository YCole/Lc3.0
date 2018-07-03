package com.gome.launcher.move;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gome.launcher.BubbleTextView;
import com.gome.launcher.DeviceProfile;
import com.gome.launcher.FolderIcon;
import com.gome.launcher.FolderInfo;
import com.gome.launcher.IconCache;
import com.gome.launcher.ItemInfo;
import com.gome.launcher.Launcher;
import com.gome.launcher.LauncherAppState;
import com.gome.launcher.LauncherModel;
import com.gome.launcher.LauncherSettings;
import com.gome.launcher.R;
import com.gome.launcher.ShortcutInfo;

import java.util.ArrayList;

import static com.gome.launcher.LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
import static com.gome.launcher.LauncherSettings.Favorites.ITEM_TYPE_FOLDER;
import static com.gome.launcher.LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;

/**
 * Created by Liuning on 2017/7/3.
 */

public class MoveViewsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MoveViewListAdapter";

    private Launcher mLauncher;
    private Context mContext;
    private LayoutInflater mLayoutInflater;

    private ArrayList<ItemInfo> mMoveItemInfos =new ArrayList<>();
    private View.OnClickListener mIconClickListener;
    private View.OnLongClickListener mIconLongClickListener;

    private RecyclerView mRecyclerView;
    private IconCache mIconCache;
    private int mItemWidth;
    private int mItemHeight;

    private AdapterItemAddListener mAdapterItemAddListener;


    public MoveViewsAdapter(RecyclerView recyclerView, Context context,
                            View.OnClickListener iconClickListener,
                            View.OnLongClickListener iconLongClickListener,
                            Launcher launcher) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mRecyclerView = recyclerView;
        mIconClickListener = iconClickListener;
        mIconLongClickListener = iconLongClickListener;
        mLauncher = launcher;
        mIconCache = (LauncherAppState.getInstance()).getIconCache();
        mItemWidth = (int) context.getResources().getDimension(R.dimen.move_item_width);
        mItemHeight = (int) context.getResources().getDimension(R.dimen.move_views_container_height);
    }

    public void setAdapterItemAddListener(AdapterItemAddListener adapterItemAddListener) {
        mAdapterItemAddListener = adapterItemAddListener;
    }


    @Override
    public int getItemViewType(int position) {
        ItemInfo info = mMoveItemInfos.get(position);
        return info.itemType;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        RecyclerView.ViewHolder viewHolder = null;
        if (viewType == ITEM_TYPE_FOLDER) {
            view = mLayoutInflater.inflate(R.layout.move_item_folder, parent, false);
            viewHolder = new FolderItemHolder(view);
        } else if (viewType == ITEM_TYPE_SHORTCUT || viewType == ITEM_TYPE_APPLICATION) {
            view = mLayoutInflater.inflate(R.layout.move_item_shortcut, parent, false);
            viewHolder = new ShortcutItemHolder(view);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        DeviceProfile grid = mLauncher.getDeviceProfile();
        int itemType = mMoveItemInfos.get(position).itemType;
        if (itemType == ITEM_TYPE_FOLDER) {
            int folderIconSize = (int) mContext.getResources().getDimension(R.dimen.move_item_icon_size);
            float scale = folderIconSize * 1.0f / grid.folderIconSizePx;
            int folderIconPageViewSize = (int) (grid.folderIconPageViewSizePx * scale);
            FolderInfo folderInfo = (FolderInfo) mMoveItemInfos.get(position);
            FolderIcon restIcon = ((FolderItemHolder) holder).folderIcon;

            restIcon = restIcon.initMoveFolderIcon(restIcon,folderInfo,folderIconPageViewSize,folderIconSize,mLauncher);

            restIcon.setOnClickListener(mIconClickListener);
            restIcon.setOnLongClickListener(mIconLongClickListener);
            restIcon.setFocusable(true);
        } else if (itemType == ITEM_TYPE_SHORTCUT || itemType == ITEM_TYPE_APPLICATION) {
            BubbleTextView restIcon = ((ShortcutItemHolder) holder).bubbleTextView;
            ShortcutInfo shortcutInfo = (ShortcutInfo) mMoveItemInfos.get(position);
            restIcon.applyFromShortcutInfo(shortcutInfo, mIconCache);
            restIcon.setTextVisibility(false);
            int paddingTop = (mItemHeight - grid.moveIconSizePx) / 2;
            restIcon.setTag(shortcutInfo);
            restIcon.setPadding(0, paddingTop, 0, paddingTop);
            restIcon.setOnClickListener(mIconClickListener);
            restIcon.setOnLongClickListener(mIconLongClickListener);
            restIcon.setFocusable(true);
            if (null != mAdapterItemAddListener) {
                mAdapterItemAddListener.onAdded(restIcon);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mMoveItemInfos == null ? -1 : mMoveItemInfos.size();
    }

    class FolderItemHolder extends RecyclerView.ViewHolder {

        FolderIcon folderIcon;

        public FolderItemHolder(View itemView) {
            super(itemView);
            folderIcon = (FolderIcon) itemView.findViewById(R.id.move_item_folder);
        }
    }

    class ShortcutItemHolder extends RecyclerView.ViewHolder {

        BubbleTextView bubbleTextView;

        public ShortcutItemHolder(View itemView) {
            super(itemView);
            bubbleTextView = (BubbleTextView) itemView.findViewById(R.id.move_item_shortcut);
        }
    }

    public boolean isContainItem(ItemInfo info) {
        return mMoveItemInfos.contains(info);
    }

    public ArrayList<ItemInfo> getMoveItemInfos() {
        return mMoveItemInfos;
    }

    public boolean isMoveListEmpty() {
        return mMoveItemInfos.size() == 0 ? true : false;
    }

    public void addItem(ItemInfo info, int position) {
        if (mMoveItemInfos == null) {
            mMoveItemInfos = new ArrayList<>();
        }
        if (info instanceof FolderInfo) {
            ((FolderInfo) info).unbind();
        }
        mMoveItemInfos.add(position, info);

        info.container = LauncherSettings.Favorites.CONTAINER_MOVE;
        info.rank = position;
        LauncherModel.moveItemInDatabase(mLauncher, info, info.container, -1, -1, -1);
        Log.v(TAG, "addItemToMoveContainner " + info.title);

        notifyItemInserted(position);
    }

    public void deleteItem(View v) {
        ItemInfo info = (ItemInfo) v.getTag();
        int pos = getItemPos(info);

        if (mMoveItemInfos.contains(info)) {
            mMoveItemInfos.remove(info);
            notifyItemRemoved(pos);
        }
    }

    public int getItemPos(ItemInfo info) {
        return mMoveItemInfos.indexOf(info);
    }

    public void clearMoveItemInfos() {
        if (mMoveItemInfos != null) {
            mMoveItemInfos.clear();
        }
        notifyDataSetChanged();
    }

    public int getItemWidth() {
        return mItemWidth;
    }

    public int getItemHeight() {
        return mItemHeight;
    }
}
