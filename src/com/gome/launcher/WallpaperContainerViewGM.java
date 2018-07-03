package com.gome.launcher;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.gallery3d.common.Utils;
import com.gome.launcher.util.WallpaperUtils;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.gome.launcher.WallpaperPickerActivity.IMAGE_PICK;
import static com.gome.launcher.util.WallpaperUtils.SAVED_FLAG;
import static com.gome.launcher.util.WallpaperUtils.SAVED_RES_ID;
import static com.gome.launcher.util.WallpaperUtils.FIRST_LAUNH_FLAG;
/**
 * Created by weijiaqi on 2017/7/1.
 */

public class WallpaperContainerViewGM extends LinearLayout {

    static final String TAG = "WallpaperContainerViewGM";

    private static final int MIN_CLICK_DELAY_TIME = 500;

    private static Toast mToast = null;

    private enum ITEM_TYPE {RESOURCE, GALLERY}

    private RecyclerView mMasterWallpaperList;

    private WallpaperAdapter mWallpaperAdapter;

    private Launcher mLauncher;

    private GalleryLoadListener mGalleryLoadListener;

    private View mSelectedTile;

    private long mLastClickTime = 0;

    private List<WallpaperTileInfo> mWallpapers;


    public WallpaperContainerViewGM(Context context) {
        this(context, null);
    }

    public WallpaperContainerViewGM(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WallpaperContainerViewGM(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (context instanceof Launcher) {
            mLauncher = (Launcher) context;
        } else {
            mLauncher = LauncherApplication.getLauncher();
        }

        mWallpapers = findBundledWallpapers();
        setFirstLaunchedWallpaper();

    }

    private void setFirstLaunchedWallpaper() {
        if (!WallpaperUtils.getBooleanValue(FIRST_LAUNH_FLAG)) {
            if (mWallpapers != null && mWallpapers.size() > 0) {
                //wjq 2017-09-29 [PRODUCTION-196]:【开机向导】开机向导点击完成，先跳出的是原生桌面壁纸 start
                ResourceWallpaperInfo resourceWallpaperInfo = (ResourceWallpaperInfo) mWallpapers.get(0);
                WallpaperUtils.setIntValue(SAVED_RES_ID,resourceWallpaperInfo.mResId);
                WallpaperUtils.setBooleanValue(SAVED_FLAG,true);
                WallpaperUtils.setBooleanValue(FIRST_LAUNH_FLAG,true);
                //wjq 2017-09-29 [PRODUCTION-196]:【开机向导】开机向导点击完成，先跳出的是原生桌面壁纸 end
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();
        bundleData();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (View.VISIBLE == visibility) {
            notifyDataChanged();
        }
    }

    public void notifyDataChanged() {
        mWallpaperAdapter.setSelectResId(WallpaperUtils.getPrefs().getInt(SAVED_RES_ID,-1));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mGalleryLoadListener = null;
        if(mToast != null)
        {
            mToast.cancel();
            mToast = null;
        }
    }

    public void setGalleryLoadListener(GalleryLoadListener galleryLoadListener) {
        mGalleryLoadListener = galleryLoadListener;
    }

    private void initViews() {
        mMasterWallpaperList = (RecyclerView) findViewById(R.id.master_wallpaper_list);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mLauncher);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mMasterWallpaperList.setLayoutManager(linearLayoutManager);

        mWallpaperAdapter = new WallpaperAdapter(mLauncher);
        mMasterWallpaperList.setAdapter(mWallpaperAdapter);
    }

    private void bundleData() {
        mWallpaperAdapter.addList(mWallpapers);
        mWallpaperAdapter.addWallpaperTileInfo(0, new PickImageInfo());
    }

    public void selectTile(View v) {
        if (mSelectedTile != null) {
            mSelectedTile.setSelected(false);
            mSelectedTile = null;
        }
        mSelectedTile = v;
        v.setSelected(true);

        // TODO: Remove this once the accessibility framework and
        // services have better support for selection state.
        v.announceForAccessibility(
                getContext().getString(R.string.announce_selection, v.getContentDescription()));
    }

    private class WallpaperAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<WallpaperTileInfo> mWallpaperTileList;
        private Context mContext;
        private int mCurrentResId = -1;

        public WallpaperAdapter(Context context) {
            mWallpaperTileList = new ArrayList<>();
            mContext = context;
        }


        public void addList(List<WallpaperTileInfo> list) {
            mWallpaperTileList.addAll(list);
            notifyDataSetChanged();
        }

        public void addWallpaperTileInfo(int index, WallpaperTileInfo wallpaperTileInfo) {
            mWallpaperTileList.add(index, wallpaperTileInfo);
            notifyDataSetChanged();
        }

        public void setSelectResId(int resId) {
            mCurrentResId = resId;
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            RecyclerView.ViewHolder holder = null;
            if (viewType == ITEM_TYPE.RESOURCE.ordinal()) {
                holder = new ResourceViewHolder(
                        layoutInflater.inflate(R.layout.wallpaper_picker_item,
                                parent, false));
            } else if (viewType == ITEM_TYPE.GALLERY.ordinal()) {
                holder = new GalleryViewHolder(
                        layoutInflater.inflate(R.layout.wallpaper_picker_image_picker_item,
                                parent, false));
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == ITEM_TYPE.GALLERY.ordinal()) {
                PickImageInfo pickImageInfo =
                        (PickImageInfo) mWallpaperTileList.get(position);
                holder.itemView.setTag(pickImageInfo);
            } else if (getItemViewType(position) == ITEM_TYPE.RESOURCE.ordinal()) {
                ResourceWallpaperInfo resourceWallpaperInfo =
                        ((ResourceWallpaperInfo) mWallpaperTileList.get(position));
                ((ResourceViewHolder) holder).wallpaper_image
                        .setImageDrawable(resourceWallpaperInfo.mThumb);
                holder.itemView.setTag(resourceWallpaperInfo);
                holder.itemView.setSelected(false);
                if (mWallpaperTileList.get(position) instanceof ResourceWallpaperInfo) {
                    int resId =  ((ResourceWallpaperInfo) mWallpaperTileList.get(position)).mResId;
                    if (resId == mCurrentResId) {
                        selectTile(holder.itemView);
                    }
                }
            }
            holder.itemView.setOnClickListener(mThumbnailOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mWallpaperTileList == null ? -1 : mWallpaperTileList.size();
        }

        @Override
        public int getItemViewType(int position) {

            return position == 0 ? ITEM_TYPE.GALLERY.ordinal() : ITEM_TYPE.RESOURCE.ordinal();
        }

        class ResourceViewHolder extends WallpaperViewHolder {


            public ResourceViewHolder(View itemView) {
                super(itemView);
            }
        }

        class GalleryViewHolder extends WallpaperViewHolder {

            public GalleryViewHolder(View itemView) {
                super(itemView);
            }
        }

        abstract class WallpaperViewHolder extends RecyclerView.ViewHolder {
            protected ImageView wallpaper_image;

            public WallpaperViewHolder(View itemView) {
                super(itemView);
                wallpaper_image = (ImageView) itemView.findViewById(R.id.wallpaper_image);
            }
        }
    }

    public static abstract class WallpaperTileInfo {

        public boolean isSelectable() {
            return false;
        }

        public void onClick(WallpaperContainerViewGM view) {
        }
    }

    public static class PickImageInfo extends WallpaperTileInfo {
        @Override
        public void onClick(WallpaperContainerViewGM view) {
            if (null != view.mGalleryLoadListener) {
                view.mGalleryLoadListener.onClickListener();
            }
           // Added by wjq in 2017-8-8 for fix bug GMOS-1494 start
//            Intent intent = new Intent(Intent.ACTION_PICK);
//            intent.setType("image/*");
//            ///M: ALPS02768802 Filter DRM files.
//            intent.putExtra(EXTRA_DRM_LEVEL, LEVEL_FL);

            // Added by wjq in 2017-8-18 for fix bug GMOS-3041 start
            Intent intent = new Intent(Intent.ACTION_PICK);
            // Added by wjq in 2017-12-11for fix bug PRODUCTION-11027 start
            intent.setComponent(new ComponentName("com.android.gallery3d",
                    "com.android.newgallery.NewGallery"));
            // Added by wjq in 2017-12-11for fix bug PRODUCTION-11027 end
            intent.setType("image/*");
            intent.putExtra("crop", "launcher");
            view.startActivityForResultSafely(intent, IMAGE_PICK);
            // Added by wjq in 2017-8-18 for fix bug GMOS-3041 end

        }
    }

    public static class ResourceWallpaperInfo extends WallpaperTileInfo {
        Resources mResources;
        int mResId;
        Drawable mThumb;

        public ResourceWallpaperInfo(Resources res, int resId, Drawable thumb) {
            mResources = res;
            mResId = resId;
            mThumb = thumb;
        }

        @Override
        public boolean isSelectable() {
            return true;
        }

        @Override
        public void onClick(WallpaperContainerViewGM view) {
            view.setWallpaperFromResources(mResources, mResId);
        }
    }

    public OnClickListener mThumbnailOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            long currentTime = Calendar.getInstance().getTimeInMillis();

            if (currentTime - mLastClickTime > MIN_CLICK_DELAY_TIME) {
                mLastClickTime = currentTime;
                WallpaperTileInfo info = (WallpaperTileInfo) view.getTag();

                /// M: ALPS01665621.
                if (info == null) return;

                if (info.isSelectable() && view.getVisibility() == View.VISIBLE) {
                    selectTile(view);
                }

                info.onClick(WallpaperContainerViewGM.this);
            } else {
                showToast(mLauncher.getApplicationContext(),mLauncher.getResources().getString(R.string.user_wait_tips),
                        Toast.LENGTH_SHORT);
            }
        }
    };

    private ArrayList<WallpaperTileInfo> findBundledWallpapers() {
        final ArrayList<WallpaperTileInfo> bundled = new ArrayList<WallpaperTileInfo>(24);
        Pair<ApplicationInfo, Integer> r = getWallpaperArrayResourceId();
        if (r != null) {
            try {
                Resources wallpaperRes = getContext().getPackageManager()
                        .getResourcesForApplication(r.first);
                addWallpapers(bundled, wallpaperRes, r.first.packageName, r.second);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return bundled;
    }

    public void setWallpaperFromGallery(Intent data) {
        if (data != null && data.getData() != null) {
            if (mSelectedTile != null) {
                mSelectedTile.setSelected(false);
            }
            Uri uri = data.getData();
            setWallpaperFromUri(uri);
        }

    }

    public Pair<ApplicationInfo, Integer> getWallpaperArrayResourceId() {
        // Context.getPackageName() may return the "original" package name,
        // com.gome.launcher; Resources needs the real package name,
        // com.gome.launcher. So we ask Resources for what it thinks the
        // package name should be.
        final String packageName = getResources().getResourcePackageName(R.array.wallpapers);
        try {
            ApplicationInfo info = getContext().getPackageManager().getApplicationInfo(packageName, 0);
            return new Pair<ApplicationInfo, Integer>(info, R.array.wallpapers);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }


    private void addWallpapers(ArrayList<WallpaperTileInfo> known, Resources res,
                               String packageName, int listResId) {
        final String[] extras = res.getStringArray(listResId);
        for (String extra : extras) {
            int resId = res.getIdentifier(extra, "drawable", packageName);
            if (resId != 0) {
                final int thumbRes = res.getIdentifier(extra + "_small", "drawable", packageName);
                if (thumbRes != 0) {
                    ResourceWallpaperInfo wallpaperInfo =
                            new ResourceWallpaperInfo(res, resId, res.getDrawable(thumbRes));
                    known.add(wallpaperInfo);
                    // Log.d(TAG, "add: [" + packageName + "]: " + extra + " (" + res + ")");
                }
            } else {
                Log.e(TAG, "Couldn't find wallpaper " + extra);
            }
        }
    }

    protected void setWallpaperFromResources(Resources res, final int resId) {
        // Added by wjq in 2017-10-11 for fix bug PRODUCTION-2289 start
        mWallpaperAdapter.setSelectResId(resId);
        // Added by wjq in 2017-10-11 for fix bug PRODUCTION-2289 end
        WallpaperUtils.setIntValue(SAVED_RES_ID,resId);
        WallpaperUtils.setBooleanValue(SAVED_FLAG,true);
        WallpaperSetTask wallpaperSetTask = new WallpaperSetTask(mLauncher, res, resId);
        wallpaperSetTask.execute(WallpaperManager.FLAG_SYSTEM);
    }

    protected void setWallpaperFromUri(Uri uri) {
        WallpaperSetTask wallpaperSetTask = new WallpaperSetTask(mLauncher, uri);
        wallpaperSetTask.execute(WallpaperManager.FLAG_SYSTEM);
    }

    public void startActivityForResultSafely(Intent intent, int requestCode) {
        Utilities.startActivityForResultSafely(mLauncher, intent, requestCode);
    }

    class WallpaperSetTask extends AsyncTask<Integer, Void, Boolean> {
        Resources mResources;
        Context mContext;
        Uri mInUri;
        int mInResId = 0;
        int mOutWidth;
        int mOutHeight;
        public WallpaperSetTask(Context c, Uri inUri) {
            mContext = c;
            mInUri = inUri;
            init();
        }

        public WallpaperSetTask(Context c, Resources res,int inResId) {
            mResources = res;
            mContext = c;
            mInResId = inResId;
            init();
        }

        private void init() {
            Point outSize = WallpaperUtils.getDefaultWallpaperSize(getResources(),
                    mLauncher.getWindowManager());
            mOutWidth = outSize.x;
            mOutHeight =outSize.y;
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            int whichWallpaper = params.length == 0 ? WallpaperManager.FLAG_SYSTEM : params[0];
            InputStream is = regenerateInputStream();
            try {
                WallpaperManager.getInstance(mContext).suggestDesiredDimensions(mOutWidth, mOutHeight);
                setWallpaper(is, null, whichWallpaper);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.closeSilently(is);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

        }

        // Helper to setup input stream
        private InputStream regenerateInputStream() {
            if (mInResId == 0 && mInUri == null) {
                Log.w(TAG, "cannot read original file, no input URI, resource ID, or " +
                        "image byte array given");
            } else {
                try {
                    if (mInUri != null) {
                        return new BufferedInputStream(
                                mContext.getContentResolver().openInputStream(mInUri));

                    } else if (mInResId != 0) {
                        return new BufferedInputStream(mResources.openRawResource(mInResId));
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        private void setWallpaper(InputStream in, Rect crop, int whichWallpaper) throws IOException {
            if (!Utilities.ATLEAST_N) {
                WallpaperManager.getInstance(mContext.getApplicationContext()).setStream(in);
            } else {
                NycWallpaperUtils.setStream(mContext, in, crop, true, whichWallpaper);
            }
        }
    }

    public static void showToast(Context context, String text, int duration) {
        if (mToast == null) {
            mToast = Toast.makeText(context, text, duration);
        } else {
            mToast.setText(text);
            mToast.setDuration(duration);
        }
        mToast.show();
    }

    interface GalleryLoadListener {
        void onClickListener();
    }
}
