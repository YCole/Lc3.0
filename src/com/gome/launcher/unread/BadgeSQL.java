package com.gome.launcher.unread;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by gaoquan on 2017/5/10.
 */

/**
 * change data by using SQLite
 *
 */
public class BadgeSQL {

    private BadgeUnreadLoader mBadgeUnreadLoader;

    private SQLiteDatabase mSQLiteDatabase;
    private DatabaseHelper mDatabaseHelper;
    private static final String ID = "_id";
    private static final String UNREAD_PACKAGE_NAME = "unreadPackageName";
    private static final String UNREAD_CLASS_NAME = "unreadClassName";
    private static final String UNREAD_TYPE = "unreadType";
    private static final String UNREAD_NUMS = "unreadNums";
    private static final String UNREAD_CLOSE = "unreadClose";
    private static final String DATABASE_NAME = "Badges.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_BADGES = "Badges";

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            //创建用于存储数据的表
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BADGES + "( " + ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    UNREAD_PACKAGE_NAME + " VARCHAR, " +
                    UNREAD_CLASS_NAME + " VARCHAR, " +
                    UNREAD_TYPE + " INTEGER, " +
                    UNREAD_NUMS + " INTEGER, " +
                    UNREAD_CLOSE + " INTEGER);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BADGES);
            onCreate(db);
        }
    }

    public BadgeSQL(BadgeUnreadLoader mBadgeUnreadLoader, Context context) {
        this.mBadgeUnreadLoader = mBadgeUnreadLoader;
        mDatabaseHelper = new DatabaseHelper(context);
    }

    /**
     * add one data
     */
    public void addOne(String packageName, String className) {
        mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(UNREAD_PACKAGE_NAME, packageName);
        cv.put(UNREAD_CLASS_NAME, className);
        cv.put(UNREAD_TYPE, 0);
        cv.put(UNREAD_NUMS, 0);
        cv.put(UNREAD_CLOSE, 0);
        mSQLiteDatabase.insert(TABLE_BADGES, ID, cv);
        mSQLiteDatabase.close();
    }

    /**
     * delete one data
     */
    public void deleteOne(String packageName) {
        mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
        int id = 0;
        id = mSQLiteDatabase.delete(TABLE_BADGES,
                UNREAD_PACKAGE_NAME + "=?",
                new String[] { packageName });
        mSQLiteDatabase.close();
    }

    /**
     * Modified by gaoquan 2017.10.16
     * fix:GMOS2.0GMOS-10023【Launcher】下载登录微信且有角标，进入微信的应用信息界面清除数据，桌面微信的角标还是存在
     */
    //-------------------------------start--------------///
    public boolean updateByPackageName(String unreadPackageName, int unreadNums) {
        Cursor c = null;
        try {
            mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
            c = mSQLiteDatabase.query(TABLE_BADGES,
                    new String[]{UNREAD_PACKAGE_NAME, UNREAD_CLASS_NAME, UNREAD_TYPE, UNREAD_NUMS},
                    UNREAD_PACKAGE_NAME + "=?",
                    new String[]{unreadPackageName}, null, null, null);
            if (c == null) {
                return false;
            }
            if (!c.moveToFirst()) { // insert
                c.close();
                return false;
            } else {
                c.close();
                int count = 0;
                ContentValues cv = new ContentValues();
                cv.put(UNREAD_NUMS, unreadNums);
                count = mSQLiteDatabase.update(TABLE_BADGES, cv,
                        UNREAD_PACKAGE_NAME + "=?",
                        new String[]{unreadPackageName});
                if (count > 0) {
                    return true;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
        return false;
    }
    //-------------------------------end--------------///

    /**
     * if data does not exist, insert init data
     */
    public void initData(String packageName, String className) {
        mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
        Cursor c = mSQLiteDatabase.query(TABLE_BADGES, new String[]{
                        UNREAD_PACKAGE_NAME, UNREAD_CLASS_NAME, UNREAD_TYPE, UNREAD_NUMS},
                UNREAD_PACKAGE_NAME + "=? and " + UNREAD_CLASS_NAME + "=?",
                new String[]{packageName, className}, null, null, null);
        if (c == null) {
            return;
        }
        if (!c.moveToFirst()) { // insert
            ContentValues cv = new ContentValues();
            cv.put(UNREAD_PACKAGE_NAME, packageName);
            cv.put(UNREAD_CLASS_NAME, className);
            cv.put(UNREAD_TYPE, 0);
            cv.put(UNREAD_NUMS, 0);
            cv.put(UNREAD_CLOSE, 0);
            mSQLiteDatabase.insert(TABLE_BADGES, ID, cv);
        }
        if(!c.isClosed()) {
            c.close();
        }
        mSQLiteDatabase.close();
    }

    /**
     * get all open switch app
     */
    public void queryOpen() {
        mSQLiteDatabase = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = null;
        String sql = "select * from " + TABLE_BADGES + " where " + UNREAD_CLOSE + " = 0";
        cursor = mSQLiteDatabase.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            String unreadPackageName = cursor.getString(1);
            String unreadClassName = cursor.getString(2);
            int unreadNums = cursor.getInt(4);
            mBadgeUnreadLoader.addWeiXinUnreadSupport(unreadPackageName, unreadClassName);
            mBadgeUnreadLoader.updateUnreadNums(new ComponentName(unreadPackageName, unreadClassName), unreadNums);
        }
        if(!cursor.isClosed()) {
            cursor.close();
        }
        mSQLiteDatabase.close();
    }

    /**
     * get all data
     */
    public Cursor queryAll() {
        mSQLiteDatabase = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = null;
        String sql = "select * from " + TABLE_BADGES;
        cursor = mSQLiteDatabase.rawQuery(sql, null);
        return cursor;
    }
}
