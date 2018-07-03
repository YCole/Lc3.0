package com.gome.launcher.unread;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;

/**
 * Created by gaoquan on 2017/5/9.
 */

/**
 * for unread badge write
 */
public class BadgeContentProvider extends ContentProvider {

    private static final String TAG = "BadgeContentProvider";
    private ContentResolver mContentResolver;
    private SQLiteDatabase mSQLiteDatabase;
    private DatabaseHelper mDatabaseHelper;

    private static final String BADGE_URI = "content://com.gome.launcher.unread.badgecontentprovider/badges";
    private static final String ID = "_id";
    private static final String UNREAD_PACKAGE_NAME = "unreadPackageName";
    private static final String UNREAD_CLASS_NAME = "unreadClassName";
    private static final String UNREAD_TYPE = "unreadType";
    private static final String UNREAD_NUMS = "unreadNums";
    private static final String UNREAD_CLOSE = "unreadClose";
    private static final String INSERT_OR_UPDATE = "insertOrUpdate";
    private static final String SUCCESS = "success";
    private static final String DATABASE_NAME = "Badges.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_BADGES = "Badges";
    private final static int BADGES = 1;

    private static UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        mUriMatcher.addURI("com.gome.launcher.unread.badgecontentprovider", "badges", BADGES);
    }


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

    @Override
    public boolean onCreate() {
        Context context = getContext();
        mContentResolver = context.getContentResolver();
        mDatabaseHelper = new DatabaseHelper(context);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        mSQLiteDatabase = mDatabaseHelper.getReadableDatabase();
        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        Cursor cursor = null;
        if(mUriMatcher.match(uri) == BADGES) {
            sqlBuilder.setTables(TABLE_BADGES);
            cursor = sqlBuilder.query(mSQLiteDatabase, projection, selection, selectionArgs, null, null, sortOrder);
                        cursor.setNotificationUri(mContentResolver, uri);
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
        Uri u = null;
        if ((mUriMatcher.match(uri) == BADGES &&
                getCallingPackage().equals(values.getAsString(UNREAD_PACKAGE_NAME))) ||
                getCallingPackage().equals(getContext().getPackageName())) {
            ContentValues cv = values;
            cv.put(UNREAD_CLOSE, 0);
            long id = mSQLiteDatabase.insert(TABLE_BADGES, ID, cv);
            u = ContentUris.withAppendedId(uri, id);
            mContentResolver.notifyChange(u, null);
        }
        return u;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
        int id = 0;
        if ((mUriMatcher.match(uri) == BADGES &&
                getCallingPackage().equals(selectionArgs[0])) ||
                getCallingPackage().equals(getContext().getPackageName())) {
            id = mSQLiteDatabase.delete(TABLE_BADGES, selection, selectionArgs);
            mContentResolver.notifyChange(uri, null);
        }
        return id;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
        int count = 0;
        if ((mUriMatcher.match(uri) == BADGES &&
                getCallingPackage().equals(values.getAsString(UNREAD_PACKAGE_NAME))) ||
                getCallingPackage().equals(getContext().getPackageName())) {
            count = mSQLiteDatabase.update(TABLE_BADGES, values, selection, selectionArgs);
            mContentResolver.notifyChange(uri, null);
        }
        return count;
    }

    /**
     * update if data exists
     * if not ,insert
     */
    public boolean insertOrUpdate(String unreadPackageName, String unreadClassName, int unreadNums) {
        mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
        Cursor c = mSQLiteDatabase.query(TABLE_BADGES,
                new String[]{UNREAD_PACKAGE_NAME, UNREAD_CLASS_NAME, UNREAD_TYPE, UNREAD_NUMS},
                UNREAD_PACKAGE_NAME + "=? and " + UNREAD_CLASS_NAME + "=?",
                new String[]{unreadPackageName, unreadClassName}, null, null, null);
        if (c == null) {
            return false;
        }
        if (!c.moveToFirst()) { // insert
            c.close();
            ContentValues cv = new ContentValues();
            cv.put(UNREAD_PACKAGE_NAME, unreadPackageName);
            cv.put(UNREAD_CLASS_NAME, unreadClassName);
            cv.put(UNREAD_TYPE, 0);
            cv.put(UNREAD_NUMS, unreadNums);
            cv.put(UNREAD_CLOSE, 0);
            long id = mSQLiteDatabase.insert(TABLE_BADGES, ID, cv);
            if (id >= 0) {
                mContentResolver.notifyChange(Uri.parse(BADGE_URI), null);
                return true;
            }
        } else {
            c.close();
            int count = 0;
            ContentValues cv = new ContentValues();
            cv.put(UNREAD_NUMS, unreadNums);
            count = mSQLiteDatabase.update(TABLE_BADGES, cv,
                    UNREAD_PACKAGE_NAME + "=? and " + UNREAD_CLASS_NAME + "=?",
                    new String[]{unreadPackageName, unreadClassName});
            if (count > 0) {
                mContentResolver.notifyChange(Uri.parse(BADGE_URI), null);
                return true;
            }
        }
        return false;
    }

    /**
     * other apps use the class to write unread badges
     */
    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Bundle bundle = new Bundle();
        boolean success = false;
        if ((INSERT_OR_UPDATE.equals(method) &&
                getCallingPackage().equals(extras.getString(UNREAD_PACKAGE_NAME))) ||
                getCallingPackage().equals(getContext().getPackageName())) {
            success = insertOrUpdate(extras.getString(UNREAD_PACKAGE_NAME),
                    extras.getString(UNREAD_CLASS_NAME), extras.getInt(UNREAD_NUMS));
        }
        bundle.putBoolean(SUCCESS, success);
        return bundle;
    }
}
