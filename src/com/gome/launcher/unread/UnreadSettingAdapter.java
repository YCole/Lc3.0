package com.gome.launcher.unread;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import gome.widget.GomeSwitch;

import com.gome.launcher.LauncherAppState;
import com.gome.launcher.R;

import java.util.ArrayList;

/**
 * Created by gaoquan on 2017/5/11.
 */

public class UnreadSettingAdapter extends RecyclerView.Adapter<UnreadSettingAdapter.UnreadViewHolder>{

    public static final String TAG = "UnreadSettingAdapter";
    public static final String BADGE_URI = "content://com.gome.launcher.unread.badgecontentprovider/badges";
    private static final String UNREAD_PACKAGE_NAME = "unreadPackageName";
    private static final String UNREAD_CLASS_NAME = "unreadClassName";
    private static final String UNREAD_CLOSE = "unreadClose";

    private Context mContext;
    private ArrayList<UnreadItem> mUnreadItems;

    public UnreadSettingAdapter(ArrayList<UnreadItem> mUnreadItems, Context mContext) {
        super();
        this.mUnreadItems = mUnreadItems;
        this.mContext = mContext;
    }

    @Override
    public UnreadViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        UnreadViewHolder mUnreadViewHolder = new UnreadViewHolder(LayoutInflater.from(mContext)
                .inflate(R.layout.unread_item, parent, false));
        return mUnreadViewHolder;
    }

    @Override
    public void onBindViewHolder(final UnreadViewHolder holder, final int position) {
        holder.img.setImageBitmap(mUnreadItems.get(position).mShortcutInfo.getIcon(LauncherAppState.getInstance().getIconCache()));
        holder.title.setText(mUnreadItems.get(position).mShortcutInfo.title);

        holder.mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) { // 开启switch
                    holder.summary.setText(((UnreadSettingActivity) mContext).getResources()
                            .getString(R.string.unread_open));

                    mUnreadItems.get(position).mClose = 0;

                    boolean allOpen = true;
                    for(UnreadItem ui : mUnreadItems) {
                        if(ui.mClose == 1) {
                            allOpen = false;
                            break;
                        }
                    }
                    if(allOpen) {
                        ((UnreadSettingActivity) mContext).mTouchAll = false;
                        ((UnreadSettingActivity) mContext).mSwitch.setChecked(true);
                        ((UnreadSettingActivity) mContext).setUnreadAllClose(false);
                        ((UnreadSettingActivity) mContext).mTouchAll = true;
                    }
                } else { // 关闭swtich
                    holder.summary.setText(((UnreadSettingActivity) mContext).getResources()
                            .getString(R.string.unread_close));

                    mUnreadItems.get(position).mClose = 1;

                    ((UnreadSettingActivity) mContext).mTouchAll = false;
                    ((UnreadSettingActivity) mContext).mSwitch.setChecked(false);
                    ((UnreadSettingActivity) mContext).setUnreadAllClose(true);
                    ((UnreadSettingActivity) mContext).mTouchAll = true;
                }
            }
        });

        if(1 == mUnreadItems.get(position).mClose) {
            holder.summary.setText(((UnreadSettingActivity) mContext).getResources()
                    .getString(R.string.unread_close));
            holder.mSwitch.setChecked(false);

        } else {
            holder.summary.setText(((UnreadSettingActivity) mContext).getResources()
                    .getString(R.string.unread_open));
            holder.mSwitch.setChecked(true);

        }
    }

    public void updateAppNoChecked(String pName, String cName){
        Uri uri = Uri.parse(BADGE_URI);
        ContentValues cv = new ContentValues();
        cv.put(UNREAD_CLOSE, 1);
        ((UnreadSettingActivity) mContext).getContentResolver().update(uri, cv,
                UNREAD_PACKAGE_NAME + "=? and " + UNREAD_CLASS_NAME + "=?",
                new String[] {pName, cName});
        ComponentName mComponentName = new ComponentName(pName, cName);
        ((UnreadSettingActivity) mContext).getMTKUnreadLoader().updateUnreadNums(
                mComponentName, 0);
    }

    public void updateAppChecked(String pName, String cName){
        Uri uri = Uri.parse(BADGE_URI);
        ContentValues cv = new ContentValues();
        cv.put(UNREAD_CLOSE, 0);
        ((UnreadSettingActivity) mContext).getContentResolver().update(uri, cv,
                UNREAD_PACKAGE_NAME + "=? and " + UNREAD_CLASS_NAME + "=?",
                new String[] {pName, cName});
    }

    @Override
    public int getItemCount() {
        return mUnreadItems.size();
    }

    class UnreadViewHolder extends RecyclerView.ViewHolder {

        ImageView img;
        TextView title;
        TextView summary;
        GomeSwitch mSwitch;

        public UnreadViewHolder(View view) {
            super(view);
            img = (ImageView) view.findViewById(R.id.icon_shortcut);
            title = (TextView) view.findViewById(R.id.title_shortcut);
            summary = (TextView) view.findViewById(R.id.summary_shortcut);
            mSwitch = (GomeSwitch) view.findViewById(R.id.switch_shortcut);
        }
    }
}
