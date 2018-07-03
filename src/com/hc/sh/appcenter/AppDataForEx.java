package com.hc.sh.appcenter;

/**
 * Created by admin on 2017/7/25.
 */

import android.os.Parcel;
import android.os.Parcelable;

public class AppDataForEx implements Parcelable {

    public String mPkgName;
    public String mAppName;
    public String mIconUrl;
    public int mState;//0,暂停；1，下载；2，安装中；3，安装完毕
    public long mProcess;

    public AppDataForEx() {

    }

    protected AppDataForEx(Parcel in) {
        mPkgName = in.readString();
        mAppName = in.readString();
        mIconUrl = in.readString();
        mState = in.readInt();
        mProcess = in.readLong();
    }

    public static final Creator<AppDataForEx> CREATOR = new Creator<AppDataForEx>() {
        @Override
        public AppDataForEx createFromParcel(Parcel in) {
            return new AppDataForEx(in);
        }

        @Override
        public AppDataForEx[] newArray(int size) {
            return new AppDataForEx[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPkgName);
        dest.writeString(mAppName);
        dest.writeString(mIconUrl);
        dest.writeInt(mState);
        dest.writeLong(mProcess);
    }


    public void readFromParcel(Parcel source) {
        mPkgName = source.readString();
        mAppName = source.readString();
        mIconUrl = source.readString();
        mState = source.readInt();
        mProcess = source.readLong();
    }
}

