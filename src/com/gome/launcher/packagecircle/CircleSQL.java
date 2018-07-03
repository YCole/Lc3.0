package com.gome.launcher.packagecircle;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by gaoquan on 2017/7/20.
 */

public class CircleSQL {

    private SQLiteDatabase mSQLiteDatabase;
    private DatabaseHelper mDatabaseHelper;
    private static final String ID = "_id";
    private static final String CIRCLE_PACKAGE_NAME = "circlePackageName";
    private static final String CIRCLE_TYPE = "unreadType";
    private static final String DATABASE_NAME = "Circle.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_CIRCLE = "Circle";

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            //创建用于存储数据的表
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CIRCLE + "( " + ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    CIRCLE_PACKAGE_NAME + " VARCHAR, " +
                    CIRCLE_TYPE + " INTEGER);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CIRCLE);
            onCreate(db);
        }
    }

    public CircleSQL(Context context){
        mDatabaseHelper = new CircleSQL.DatabaseHelper(context.getApplicationContext());
    }

    public boolean queryExist(String packageName){
        mSQLiteDatabase = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = null;
        String sql = "select * from " + TABLE_CIRCLE + " where " + CIRCLE_PACKAGE_NAME + "=?";
        cursor = mSQLiteDatabase.rawQuery(sql, new String[] {packageName});
        while (cursor.moveToNext()) {
            if(!cursor.isClosed()) {
                cursor.close();
            }
            mSQLiteDatabase.close();
            return true;
        }
        if(!cursor.isClosed()) {
            cursor.close();
        }
        mSQLiteDatabase.close();
        return false;
    }

    /**
     * add one data
     */
    public void addOne(String packageName) {
        mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(CIRCLE_PACKAGE_NAME, packageName);
        cv.put(CIRCLE_TYPE, 0);
        mSQLiteDatabase.insert(TABLE_CIRCLE, ID, cv);
        mSQLiteDatabase.close();
    }

    /**
     * delete one data
     */
    public void deleteOne(String packageName) {
        mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
        int id = 0;
        id = mSQLiteDatabase.delete(TABLE_CIRCLE,
                CIRCLE_PACKAGE_NAME + "=?",
                new String[] { packageName });
        mSQLiteDatabase.close();
    }

    /**
     * get all data
     */
    public Cursor queryAll() {
        mSQLiteDatabase = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = null;
        String sql = "select * from " + TABLE_CIRCLE;
        cursor = mSQLiteDatabase.rawQuery(sql, null);
        return cursor;
    }
}
