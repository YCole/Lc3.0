package com.gome.launcher.db;

import android.database.sqlite.SQLiteDatabase;

import com.gome.launcher.LauncherApplication;
import com.gome.launcher.util.DLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DBManager {

    private static final String TAG = "DBManager";
    private static final int BUFFER_SIZE = 65536;
    private static final String CLASSIFY_DB_PATH = "db/apptype.db";

    public static final String DATA = "data";
    public static final String DATABASES = "databases";
    public static final String SEPARATOR = File.separator;

    public static final String IN_DESCENDING_ORDER = "desc";
    public static final String IN_ASCENDING_ORDER = "asc";

    private String getDbPath(String dbName){
        StringBuilder builder = new StringBuilder();
        builder.append(SEPARATOR);
        builder.append(DATA);
        builder.append(SEPARATOR);
        builder.append(DATA);
        builder.append(SEPARATOR);
        builder.append(LauncherApplication.getAppContext().getPackageName());
        builder.append(SEPARATOR);
        builder.append(DATABASES);
        builder.append(SEPARATOR);
        builder.append(dbName);
        return builder.toString();
    }

    private DBManager(){}

    public static DBManager getInstance() {
        return DBManagerHolder.sInstance;
    }

    private static class DBManagerHolder {
        private static final DBManager sInstance = new DBManager();
    }

    public SQLiteDatabase openDatabase(String dbName) {
        return createDatabase(getDbPath(dbName));
    }

    private SQLiteDatabase createDatabase(String dbFilePath) {
        long time = System.currentTimeMillis();
        SQLiteDatabase db = null;
        try {
            File file = new File(dbFilePath);
            // To determine whether the mDatabase file exists,
            // if the file does not exist, import,
            // or to open the mDatabase directly
            if (!(file.exists())) {
                //InputStream is = this.mContext.getResources().openRawResource(
                //        R.raw.apptype); //Database needs to be imported
                InputStream is = LauncherApplication.getAppContext().getAssets().open(CLASSIFY_DB_PATH);
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[BUFFER_SIZE];
                int count;
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                is.close();
            }
            db = SQLiteDatabase.openOrCreateDatabase(dbFilePath,null);
        } catch (FileNotFoundException e) {
            DLog.e(TAG, "File not found");
            e.printStackTrace();
        } catch (IOException e) {
            DLog.e(TAG, "IO exception");
            e.printStackTrace();
        } catch (Exception e){
            DLog.e(TAG, "other exception");
            e.printStackTrace();
        }
        DLog.e("zzzz","zzzz openDatabase time = "+(System.currentTimeMillis() - time));
        return db;
    }

    public void closeDatabase(SQLiteDatabase db) {
        try {
            if(db != null){
                db.close();
            }
        } catch (Exception e){
            DLog.e(TAG,"DBManager -- closeDatabase = error = " + e.getMessage());
        }
    }


}


