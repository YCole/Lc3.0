package com.gome.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gome.launcher.backup.nano.BackupProtos;
import com.gome.launcher.compat.UserHandleCompat;
import com.gome.launcher.util.DisplayMetricsUtils;
import com.gome.launcher.util.LongArrayMap;
import com.gome.launcher.view.WaveSideBar;
import com.gome.launcher.view.WaveSideBar.SideBarUtil;
import com.mediatek.launcher3.LauncherLog;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by huanghaihao on 2017/7/11.
 */

public class AppSearchWindow extends PopupWindow implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener, LauncherProviderChangeListener,
        WaveSideBar.OnSelectIndexItemListener {
    private static final String TAG = "AppSearchWindow";
    public static final  int COLUMN_COUNT = 4;
    private Launcher mLauncher;
    private Folder mFolder;
    private ImageView mAppsClose;
    private TextView mAppsSelect;
    private TextView mAppsAdd;
    private EditText mSearchContent;
    private ImageView mSearchCancel;
    private ViewGroup mAllAppsView;
    private RecyclerView mAppsList;
    private WaveSideBar mSideBar;
    private GridView mSearchAppView;
    private AppsGirdAdapter  mSearchAdapter;
    private RelativeLayout mNoSearchView;

    private IconCache mIconCache;
    private LauncherModel mModel;
    private Map<String, List<ItemInfo>> mAllAppsMap;
    private AllAppsAdapter mAllAppsAdapter;
    private AllAppsList mAllAppsList;
    private AllAppsList mRestoreList;
    private Resources mResources;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case INIT_DATA:
                    LauncherLog.d("chenchao", "init window *******");
                    initWindow();
                    break;
                default:
                    break;
            }
        }
    };

    public AppSearchWindow(View contentView, Folder folder) {
        super(contentView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        contentView.setPadding(0, DisplayMetricsUtils.getStatusBarStableHeight(),
                0, DisplayMetricsUtils.getNavigationBarStableHeight());
        mFolder = folder;
        mLauncher = folder.mLauncher;
        mResources = mLauncher.getResources();
        LauncherAppState appState = LauncherAppState.getInstance();
        mIconCache = appState.getIconCache();
        mModel = appState.getModel();
        mAllAppsMap = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                char key1 = o1.charAt(0);
                char key2 = o2.charAt(0);
                if (Character.isLetter(key1) && Character.isLetter(key2)) {
                    return key1 - key2;
                } else if (Character.isLetter(key1)) {
                    return -1;
                } else if (Character.isLetter(key2)) {
                    return 1;
                }
                return 0;
            }
        });
        mAllAppsAdapter = new AllAppsAdapter(this, mAllAppsMap);
        mAllAppsList = new AllAppsList();
        mRestoreList = new AllAppsList();

        setupViews(contentView);
    }

    private void setupViews(View contentView) {
        mAppsClose = (ImageView) contentView.findViewById(R.id.apps_close);
        mAppsClose.setOnClickListener(this);
        mAppsSelect = (TextView) contentView.findViewById(R.id.apps_size);
        mAppsAdd = (TextView) contentView.findViewById(R.id.apps_add);
        mAppsAdd.setOnClickListener(this);
        mSearchContent = (EditText) contentView.findViewById(R.id.apps_search);
        mSearchContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String searchContent = s.toString();
                if (TextUtils.isEmpty(searchContent)) {
                    mRestoreList.clear();
                    mSearchCancel.setVisibility(View.GONE);
                    mSearchAppView.setVisibility(View.GONE);
                    mAllAppsView.setVisibility(View.VISIBLE);
                    mAppsAdd.setText(R.string.add_app);
                    mAllAppsAdapter.notifyDataSetChanged();
                    mNoSearchView.setVisibility(View.GONE);
                } else {
                    if (!isVisible(mSearchCancel)) {
                        mRestoreList.content.addAll(mAllAppsList.data);
                        mRestoreList.data.addAll(mAllAppsList.data);
                        mSearchCancel.setVisibility(View.VISIBLE);
                        mAllAppsView.setVisibility(View.GONE);
                        mAppsAdd.setText(R.string.sure_app);
                    }
                    List<ItemInfo> searchApps = getSearchApps(searchContent);
                    //added by liuning for GM0S-6194 on 2017/9/19 start
                    if (searchApps.size() < 1) {
                        mSearchAppView.setVisibility(View.GONE);
                        mNoSearchView.setVisibility(View.VISIBLE);
                    } else {
                        mSearchAppView.setVisibility(View.VISIBLE);
                        mNoSearchView.setVisibility(View.GONE);
                    }
                    //added by liuning for GM0S-6194 on 2017/9/19 end

                    mSearchAdapter.setAllApps(searchApps);
                    mSearchAdapter.notifyDataSetChanged();
                }
            }
        });
        mSearchCancel = (ImageView) contentView.findViewById(R.id.apps_search_cancel);
        mSearchCancel.setOnClickListener(this);
        mAllAppsView = (ViewGroup) contentView.findViewById(R.id.all_apps);
        //added by liuning for GM0S-6194 on 2017/9/19 start
        mNoSearchView = (RelativeLayout) contentView.findViewById(R.id.search_item_no_search_layout);
        //added by liuning for GM0S-6194 on 2017/9/19 end
        mAppsList = (RecyclerView) contentView.findViewById(R.id.apps_list);
        mAppsList.setLayoutManager(new LinearLayoutManager(mLauncher));
        mAppsList.setAdapter(mAllAppsAdapter);
        mSideBar = (WaveSideBar) contentView.findViewById(R.id.wave_side_bar);
        mSideBar.setOnSelectIndexItemListener(this);

        mSearchAppView = (GridView) contentView.findViewById(R.id.search_grid);
        mSearchAdapter = new AppsGirdAdapter(AppSearchWindow.this);
        mSearchAppView.setAdapter(mSearchAdapter);
    }

    private void bindFolderContent() {
        mAllAppsList.clear();
        List<ShortcutInfo> items = mFolder.getInfo().contents;
        mAllAppsList.data.addAll(items);
        mAllAppsList.content.addAll(items);
    }

    private void initWindow() {

        //不在每次都将所有app重新getPinYin来生成adapter需要的map，在LauncherModel中已经将map准备好，直接刷新。
        mAllAppsMap.clear();
        synchronized (mModel.sAllAppsMap){
            mAllAppsMap.putAll(mModel.sAllAppsMap);
        }
        mAllAppsAdapter.notifyDataSetChanged();

        if (isVisible(mSearchAppView)) {
            List<ItemInfo> searchApps = getSearchApps(mSearchContent.getText().toString());
            mSearchAdapter.setAllApps(searchApps);
            mSearchAdapter.notifyDataSetChanged();
        }

        int selectSize = mAllAppsList.data.size();
        mAppsSelect.setText(String.format(mLauncher.getString(R.string.select_app_size), selectSize + ""));
        mAppsAdd.setText(R.string.add_app);
    }

    private String getKey(Map<String, List<ItemInfo>> map, int index) {
        int sign = 0;
        for (String key : map.keySet()) {
            if (sign == index) {
                return key;
            }
            sign++;
        }
        return null;
    }

    private int indexOfKey(Map<String, List<ItemInfo>> map, String key) {
        int index = 0;
        for (String item : map.keySet()) {
            if (TextUtils.equals(key, item)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * 获取搜索的结果
     * add  by huanghaihao in 2017-7-13 for adding more app in folder
     *
     * @param searchContent
     * @return
     */
    private List<ItemInfo> getSearchApps(String searchContent) {
            List<ItemInfo> searchApps = new ArrayList<>();
            if (TextUtils.isEmpty(searchContent.trim())) {
                return searchApps;
            }
            String language = Locale.getDefault().getLanguage().toLowerCase();
            boolean isSwitchLanguage;
            switch (language) {
                default:
                    isSwitchLanguage = !isChinese(searchContent.charAt(0));
                    break;
            }
            for (Map.Entry<String, List<ItemInfo>> entry : mAllAppsMap.entrySet()) {
                for (ItemInfo app : entry.getValue()) {
                    if (!TextUtils.isEmpty(app.title)) {
                        String title = app.title.toString();
                        if (isSwitchLanguage) {
                            title = getPinYin(title);
                        }
                        if (title.toUpperCase().contains(searchContent.toUpperCase())) {
                            searchApps.add(app);
                        }
                    }
                }
            }
            return searchApps;
    }

    /**
     * 是否是中文
     * add  by huanghaihao in 2017-7-19 for adding more app in folder
     *
     * @param content
     * @return
     */
    private boolean isChinese(Character content) {
        return Character.toString(content).matches("[\u4e00-\u9fa5]+");
    }

    /**
     * 获取汉语拼音
     * add  by huanghaihao in 2017-7-13 for adding more app in folder
     *
     * @param strs
     * @return
     */
    public static String getPinYin(CharSequence strs) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
        char[] ch = strs.toString().trim().toCharArray();
        StringBuffer buffer = new StringBuffer("");
        try {
            for (int i = 0; i < ch.length; i++) {
                // unicode，bytes应该也可以.
                if (Character.toString(ch[i]).matches("[\u4e00-\u9fa5]+")) {
                    String[] temp = PinyinHelper.toHanyuPinyinStringArray(ch[i], format);
                    buffer.append(temp[0]);
                } else {
                    buffer.append(Character.toString(ch[i]));
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            LauncherLog.e(TAG, e.getMessage(), e);
        }
        return buffer.toString();
    }
    public void show(View parent) {
        DisplayMetricsUtils.showStatusBar();
        setBackgroundDrawable(mLauncher.mFolderBlur.getBackground());
        mSearchContent.setText("");
        bindFolderContent();
        initWindow();
        super.showAtLocation(parent, Gravity.NO_GRAVITY, 0, 0);
        mLauncher.setLauncherProviderChangeListener(this);
    }

    @Override
    public void dismiss() {
        if (isVisible(mSearchCancel) && mFolder.getInfo().opened) {
            for (ItemInfo item : mRestoreList.removed) {
                mAllAppsList.add(item);
            }
            for (ItemInfo item : mRestoreList.added) {
                mAllAppsList.remove(item);
            }
            mSearchContent.setText("");
            mAppsSelect.setText(String.format(mLauncher.getString(
                    R.string.select_app_size), mAllAppsList.data.size() + ""));
        } else {
            DisplayMetricsUtils.hideStatusBar();
            /**
             * Added by gaoquan 2017.7.25
             */
            //-------------------------------start--------------///
            mLauncher.isShowAppSearchWindow = false;
            //-------------------------------end--------------///
            mLauncher.setLauncherProviderChangeListener(null);
            super.dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.apps_search_cancel:
                mSearchContent.setText("");
                break;
            case R.id.apps_add:
                onAdd();
                break;
            case R.id.apps_close:
                dismiss();
                break;
            default:
                break;
        }
    }

    /**
     * 执行添加点击
     */
    private void onAdd() {
        if (isVisible(mSearchCancel)) {
            mSearchContent.setText("");
        } else {
            dismiss();
            mFolder.addApps(mFolder, mAllAppsList.added);
            mFolder.removeApps(mFolder, mAllAppsList.removed);
            mLauncher.removeApps(mFolder, mAllAppsList.added);
            mLauncher.addApps(mFolder, mAllAppsList.removed);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        ItemInfo itemInfo = (ItemInfo) compoundButton.getTag();
        if (null == itemInfo) {
            return;
        }
        if (isChecked) {
            if (isVisible(mSearchCancel)) {
                mRestoreList.add(itemInfo);
            }
            mAllAppsList.add(itemInfo);
        } else {
            if (isVisible(mSearchCancel)) {
                mRestoreList.remove(itemInfo);
            }
            mAllAppsList.remove(itemInfo);
        }
        int contentSize = mAllAppsList.data.size();
        if (0 == contentSize) {
            mAppsAdd.setClickable(false);
            mAppsAdd.setTextColor(mLauncher.getColor(R.color.add_unnormal_color));
        } else if (!mAppsAdd.isClickable()) {
            mAppsAdd.setClickable(true);
            mAppsAdd.setTextColor(mLauncher.getColor(R.color.add_normal_color));
        }
        mAppsSelect.setText(String.format(mLauncher.getString(
                R.string.select_app_size), contentSize + ""));
    }

    /**
     * 是否view可见
     * add  by huanghaihao in 2017-7-13 for adding more app in folder
     *
     * @param view
     * @return
     */
    private boolean isVisible(View view) {
        return view.getVisibility() == View.VISIBLE;
    }

    private static final int INIT_DATA = 0;
    @Override
    public void onLauncherProviderChange(boolean isDownloadingUpdate) {
        //isDownloadingUpdate 用来过滤掉下载应用进度时，数据库更新导致的不必要的刷新。
        if(!isDownloadingUpdate) {
            //如果1000毫秒内频繁来消息，过滤掉。防止频繁刷新
            if(mHandler.hasMessages(INIT_DATA)){
                mHandler.removeMessages(INIT_DATA);
                mHandler.sendEmptyMessageDelayed(INIT_DATA, 1000);
            } else {
                mHandler.sendEmptyMessageDelayed(INIT_DATA, 1000);
            }
        }
    }

    @Override
    public void onSettingsChanged(String settings, boolean value) {
    }

    @Override
    public void onAppWidgetHostReset() {
    }

    @Override
    public void onSelectIndexItem(String item) {
        int index = indexOfKey(mAllAppsMap, item);
        if (index >= 0) {
            mAppsList.smoothScrollToPosition(index);
        }
    }

    /**
     * "添加"监听
     * add  by huanghaihao in 2017-7-13 for adding more app in folder
     */
    public interface FolderAppListener {
        void addApps(Folder folder, List<? extends ItemInfo> apps);

        void removeApps(Folder folder, List<? extends ItemInfo> apps);
    }

    /**
     * 加载allapps适配器
     * add  by huanghaihao in 2017-7-13 for adding more app in folder
     */
    private class AllAppsAdapter extends RecyclerView.Adapter<Holder1> {
        AppSearchWindow mWindow;
        Context mContext;
        Map<String, List<ItemInfo>> mAllApps;

        private AllAppsAdapter(AppSearchWindow window, Map<String, List<ItemInfo>> allApps) {
            mWindow = window;
            mContext = window.getContentView().getContext();
            mAllApps = allApps;
        }


//        @Override
//        public int getCount() {
//            if (!isMapEmpty()) {
//                return mAllApps.size();
//            }
//            return 0;
//        }
//
//        @Override
//        public Object getItem(int position) {
//            if (!isMapEmpty()) {
//                return getKey(mAllApps, position);
//            }
//            return null;
//        }

        @Override
        public Holder1 onCreateViewHolder(ViewGroup parent, int viewType) {
            View convertView = LayoutInflater.from(mContext).inflate(R.layout.folder_all_apps_item, parent,false);
            return new Holder1(convertView);
        }

        @Override
        public void onBindViewHolder(Holder1 holder, int position) {
            String key = getKey(mAllApps, position);
            List<ItemInfo> itemInfos = mAllApps.get(key);
            addCellItem(itemInfos, holder.mCellListView);
            holder.mAppsLetter.setText(key);
            for(int i = 0; i < itemInfos.size(); i++ ) {
                ItemInfo itemInfo =  mAllApps.get(key).get(i);
                RelativeLayout cellLayout = (RelativeLayout) holder.mCellListView.getChildAt(i);
                if (cellLayout == null) {
                    Log.e("wjq","cellLayout = " + itemInfo.title.toString());
                    continue;
                }
                CheckBox checkBox = (CheckBox) cellLayout.findViewById(R.id.app_checkbox);
                BubbleTextView appView = (BubbleTextView) cellLayout.findViewById(R.id.app_icon);
                appView.applyFromShortcutInfo((ShortcutInfo) itemInfo, mIconCache);
                if (mAllAppsList.contain(mAllAppsList.data, itemInfo)) {
                    checkBox.setChecked(true);
                } else {
                    //Added by wjq in 2017-10-10 for fix bug PRODUCTION-2009 start
                    checkBox.setChecked(false);
                    //Added by wjq in 2017-10-10 for fix bug PRODUCTION-2009 end
                }
                checkBox.setTag(itemInfo);
            }
        }

        @Override
        public long getItemId(int position) {
            if (!isMapEmpty()) {
                return getKey(mAllApps, position).hashCode();
            }
            return 0;
        }

        @Override
        public int getItemCount() {
            return mAllApps.size();
        }

//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//            Holder1 holder;
//            String key = getKey(mAllApps, position);
//            List<ItemInfo> itemInfos = mAllApps.get(key);
//                holder = new Holder1();
//                convertView = LayoutInflater.from(mContext).inflate(R.layout.folder_all_apps_item, null);
//                holder.mAppsLetter = (TextView) convertView.findViewById(R.id.apps_letter);
//                holder.mCellListView = (LinearLayout) convertView.findViewById(R.id.view_cell_list);
//                addCellItem(itemInfos, holder.mCellListView);
//                convertView.setTag(holder);
//
//
//            holder.mAppsLetter.setText(key);
//            for(int i = 0; i < itemInfos.size(); i++ ) {
//                ItemInfo itemInfo =  mAllApps.get(key).get(i);
//                RelativeLayout cellLayout = (RelativeLayout) holder.mCellListView.getChildAt(i);
//                if (cellLayout == null) {
//                    Log.e("wjq","cellLayout = " + itemInfo.title.toString());
//                    continue;
//                }
//                CheckBox checkBox = (CheckBox) cellLayout.findViewById(R.id.app_checkbox);
//                BubbleTextView appView = (BubbleTextView) cellLayout.findViewById(R.id.app_icon);
//                appView.applyFromShortcutInfo((ShortcutInfo) itemInfo, mIconCache);
//                if (mAllAppsList.contain(mAllAppsList.data, itemInfo)) {
//                    checkBox.setChecked(true);
//                } else {
//                    //Added by wjq in 2017-10-10 for fix bug PRODUCTION-2009 start
//                    checkBox.setChecked(false);
//                    //Added by wjq in 2017-10-10 for fix bug PRODUCTION-2009 end
//                }
//                checkBox.setTag(itemInfo);
//            }
//
//            return convertView;
//        }

        private boolean isMapEmpty() {
            return null == mAllApps || mAllApps.isEmpty();
        }


        private void addCellItem(List<ItemInfo> itemInfos, GridLayout parent) {
            parent.removeAllViews();
            for (int i = 0; i < itemInfos.size(); i++) {
                GridLayout.Spec rowSpec = GridLayout.spec(i / COLUMN_COUNT);
                GridLayout.Spec columnSpec = GridLayout.spec(i % COLUMN_COUNT);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, columnSpec);
                //add by weijiaqi 2017/11/23 for fix bug PRODUCTION-7747 start
                params.width = (DisplayMetricsUtils.getDeviceWidth()
                        - mResources.getDimensionPixelSize(R.dimen.app_search_view_wave_bar_width)
                        - mResources.getDimensionPixelSize(R.dimen.badge_10dp)) / COLUMN_COUNT;
                //add by weijiaqi 2017/11/23 for fix bug PRODUCTION-7747 end
                params.height = mResources.getDimensionPixelSize(R.dimen.app_search_view_grid_item_height);
                if ((i / COLUMN_COUNT) % 2 != 0) {
                    params.topMargin = mResources.getDimensionPixelSize(R.dimen.app_search_view_padding_top);
                }
                RelativeLayout cellLayout = (RelativeLayout) LayoutInflater.from(mContext).inflate(R.layout.folder_all_apps_grid_item, null);
                final CheckBox checkBox = (CheckBox) cellLayout.findViewById(R.id.app_checkbox);
                BubbleTextView appView = (BubbleTextView) cellLayout.findViewById(R.id.app_icon);
                checkBox.setOnCheckedChangeListener(mWindow);
                appView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (checkBox.isChecked()) {
                            checkBox.setChecked(false);
                        } else {
                            checkBox.setChecked(true);
                        }
                    }
                });
                parent.addView(cellLayout,params);
            }

        }

    }

    /**
     * 网格适配器
     * add  by huanghaihao in 2017-7-13 for adding more app in folder
     */
    private class AppsGirdAdapter extends BaseAdapter {
        AppSearchWindow mWindow;
        Context mContext;
        List<ItemInfo> mAllApps;

        public AppsGirdAdapter(AppSearchWindow window) {
            mWindow = window;
            mContext = window.getContentView().getContext();
        }

        public void setAllApps(List<ItemInfo> allApps) {
            this.mAllApps = allApps;
        }

        @Override
        public int getCount() {
            if (!isListEmpty()) {
                return mAllApps.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (!isListEmpty()) {
                return mAllApps.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            if (!isListEmpty()) {
                return mAllApps.get(position).hashCode();
            }
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Holder2 holder;
            if (null == convertView) {
                holder = new Holder2();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.folder_all_apps_grid_item, null);
                holder.mCheckBox = (CheckBox) convertView.findViewById(R.id.app_checkbox);
                holder.mAppView = (BubbleTextView) convertView.findViewById(R.id.app_icon);
                holder.mCheckBox.setOnCheckedChangeListener(mWindow);
                holder.mAppView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (holder.mCheckBox.isChecked()) {
                            holder.mCheckBox.setChecked(false);
                        } else {
                            holder.mCheckBox.setChecked(true);
                        }
                    }
                });
                convertView.setTag(holder);
            } else {
                holder = (Holder2) convertView.getTag();
            }
            if (!isListEmpty() && mAllApps.get(position) instanceof ShortcutInfo) {
                ItemInfo itemInfo = mAllApps.get(position);
                holder.mAppView.applyFromShortcutInfo((ShortcutInfo) itemInfo, mIconCache);
                holder.mCheckBox.setTag(mAllApps.get(position));
                if (mAllAppsList.contain(mAllAppsList.data, mAllApps.get(position))) {
                    holder.mCheckBox.setChecked(true);
                } else {
                    //Added by wjq in 2017-10-10 for fix bug PRODUCTION-2009 start
                    holder.mCheckBox.setChecked(false);
                    //Added by wjq in 2017-10-10 for fix bug PRODUCTION-2009 end
                }
            }
            return convertView;
        }

        private boolean isListEmpty() {
            return null == mAllApps || mAllApps.isEmpty();
        }
    }

    private final static class Holder1 extends RecyclerView.ViewHolder{
        TextView mAppsLetter;
        GridLayout mCellListView;

        public Holder1(View itemView) {
            super(itemView);
            mAppsLetter = (TextView) itemView.findViewById(R.id.apps_letter);
            mCellListView = (GridLayout) itemView.findViewById(R.id.view_cell_list);
        }
    }

    private final static class Holder2 {
        TextView mAppsLetter;
        GridLayout mCellListView;

        CheckBox mCheckBox;
        BubbleTextView mAppView;
    }

    private class AllAppsList {
        /**
         * The list off folder apps.
         */
        public List<ItemInfo> content = new ArrayList<>();
        /**
         * The list off all apps.
         */
        public List<ItemInfo> data = new ArrayList<>();
        /**
         * The list of apps that have been added since the last notify() call.
         */
        public List<ItemInfo> added = new ArrayList<>();
        /**
         * The list of apps that have been removed since the last notify() call.
         */
        public List<ItemInfo> removed = new ArrayList<>();

        public void add(ItemInfo item) {
            if (!contain(content, item)) {
                if (!contain(added, item)) {
                    added.add(item);
                }
            } else {
                remove(removed, item);
            }
            if (!contain(data, item)) {
                data.add(item);
            }
        }

        public void remove(ItemInfo item) {
            if (contain(content, item)) {
                if (!contain(removed, item)) {
                    removed.add(item);
                }
            } else {
                remove(added, item);
            }
            remove(data, item);
        }

        public void clear() {
            content.clear();
            data.clear();
            added.clear();
            removed.clear();
        }

        public boolean remove(List<ItemInfo> data, ItemInfo item) {
            for (ItemInfo app : data) {
                if (app.id == item.id) {
                    return data.remove(app);
                }
            }
            return false;
        }

        public boolean contain(List<ItemInfo> data, ItemInfo item) {
            for (ItemInfo app : data) {
                if (app.id == item.id) {
                    return true;
                }
            }
            return false;
        }
    }

}
