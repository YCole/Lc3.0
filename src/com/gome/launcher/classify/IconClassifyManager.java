package com.gome.launcher.classify;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.gome.launcher.AppInfo;
import com.gome.launcher.LauncherApplication;
import com.gome.launcher.R;
import com.gome.launcher.ShortcutInfo;
import com.gome.launcher.db.DBManager;

import java.util.ArrayList;

/**
 * Created by liuqiushou on 2017/07/05.
 * tranplant from Launcher1.0
 */

public class IconClassifyManager {

    private static final String TAG = "IconClassifyManager";
    public static final String DB_NAME = "apptype.db"; //保存的数据库文件名
    public static final String TABLE_NAME = "google_table_union_item";

    public static class Column {
        public static final String PACKAGE_NAME = "packageName";
        public static final String TYPE = "type";
        public static final String TYPE_ID = "typeId";
    }

//    public static final String TYPE_MEDIA = "Media";//媒体
//    public static final String TYPE_ENTERTAINMENT = "Entertainment";  //游戏娱乐
//    public static final String TYPE_SOCIAL = "Social";//社会
//    public static final String TYPE_BOOKS = "Books"; //书籍
//    public static final String TYPE_FINANCE = "Finance"; //财务
//    public static final String TYPE_SHOPPING = "Shopping"; //购物
//    public static final String TYPE_LIFESTYLE = "Lifestyle"; //生活方式
//    public static final String TYPE_TRANSPORTATION = "Transportation";  //交通运输
//    public static final String TYPE_GAMES = "Games";  // 游戏
//    public static final String TYPE_TOOL = "Tool";  //工具
//    public static final String TYPE_WEATHER = "Weather"; //天气
//    public static final String TYPE_UNCOMMON = "Uncommon"; //不常用的

    public static final int TYPE_MEDIA_ID = 1;
    public static final int TYPE_ENTERTAINMENT_ID = 2;
    public static final int TYPE_SOCIAL_ID = 3;
    public static final int TYPE_BOOKS_ID = 4;
    public static final int TYPE_FINANCE_ID = 5;
    public static final int TYPE_SHOPPING_ID = 6;
    public static final int TYPE_LIFESTYLE_ID = 7;
    public static final int TYPE_TRANSPORTATION_ID = 8;
    public static final int TYPE_GAMES_ID = 9;
    public static final int TYPE_TOOL_ID = 10;
    public static final int TYPE_WEATHER_ID = 11;
    public static final int TYPE_OFFICE_ID = 12;
    public static final int TYPE_UNCOMMON_ID = 13;

    public static final int TYPE_NON_CLASSIFY_ID = 30; //不属于文件夹的应用
    private static final ArrayList<ClassifyInfo> sClassifyInfos = new ArrayList<ClassifyInfo>();

    public static void loadClassifyInfos() {
        Log.i(TAG, "loadClassifyInfos sClassifyInfos.size()=" + sClassifyInfos.size());
        if (sClassifyInfos.size() == 0) {
            Cursor cursor = null;
            SQLiteDatabase db = null;
            try {
                db = DBManager.getInstance().openDatabase(DB_NAME);
                cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
                if(cursor != null && !cursor.isClosed()) {
                    while (cursor.moveToNext()) {
                        ClassifyInfo classifyInfo = new ClassifyInfo();
                        classifyInfo.packageName = cursor.getString(cursor.getColumnIndex(Column.PACKAGE_NAME));
                        classifyInfo.type = cursor.getString(cursor.getColumnIndex(Column.TYPE));
                        classifyInfo.typeId = mergeAppTypeByTypeId(
                                cursor.getInt(cursor.getColumnIndex(Column.TYPE_ID)));
                        sClassifyInfos.add(classifyInfo);
                    }
                }
            } catch (Exception e){
                Log.e(TAG, "IconClassifyManager -- query error = "+e.getMessage());
            } finally {
                if(cursor != null){
                    cursor.close();
                }
                DBManager.getInstance().closeDatabase(db);
            }
        }
    }

    /* get smart name for list of ShortcutInfo,
     * The smart name is type for the most of ShortcutInfos
     */
    public static String getSmartName(ArrayList<ShortcutInfo> list) {
        String name = "";
        SparseArray<ArrayList<ShortcutInfo>> classifyInfosList =
                queryByShortcutInfoListPkg(list);
        if (classifyInfosList == null || classifyInfosList.size() == 0) {
            return name;
        } else {
            int max = 0;
            int index = -1;

            for (int i = 0; i < classifyInfosList.size(); i++) {
                ArrayList<ShortcutInfo> values = classifyInfosList.valueAt(i);
                if (values == null) {
                    continue;
                } else if (values.size() > max){
                    index = i;
                    max = values.size();
                }
            }

            if (index >= 0) {
                name = getAppTypeByTypeId(classifyInfosList.keyAt(index));
            }
        }

        return name;
    }

    public static SparseArray<ArrayList<ShortcutInfo>> queryByShortcutInfoListPkg(ArrayList<ShortcutInfo> list) {
        SparseArray<ArrayList<ShortcutInfo>> resultList = new SparseArray<>();
        ArrayList<ShortcutInfo> noInFolderList = new ArrayList<>();
        ArrayList<ShortcutInfo> inFolderList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ShortcutInfo appInfo = list.get(i);
            String pkg ;
            if(appInfo.getTargetComponent() != null)
            {
                pkg = appInfo.getTargetComponent().getPackageName();
            }else
            {
                pkg = appInfo.getPkgName();
            }
            //Added by wjq in 2017-12-26 for fixed bug PRODUCTION-12079 & PRODUCTION-12026 start
            if (null != pkg) {
                for (ClassifyInfo classifyInfo : sClassifyInfos) {
                    if (pkg.equals(classifyInfo.getPackageName())) {
                        int typeId = classifyInfo.getTypeId();
                        ArrayList<ShortcutInfo> items = resultList.get(typeId);
                        if (items == null) {
                            items = new ArrayList<>();
                            resultList.put(typeId, items);
                        }
                        inFolderList.add(appInfo);
                        items.add(appInfo);
                        break;
                    }
                }
            }
            //Added by wjq in 2017-12-26 for fixed bug PRODUCTION-12079 & PRODUCTION-12026 end
        }
        for(ShortcutInfo info : list){
            if(!inFolderList.contains(info)){
                noInFolderList.add(info);
            }
        }
        resultList.put(TYPE_NON_CLASSIFY_ID, noInFolderList);
        return resultList;
    }

    /* get the suited app type of type id */
    public static String getAppTypeByTypeId(int typeId){
        String type = "";
        switch (typeId){
            case TYPE_MEDIA_ID: // 1  ---- Media  //媒体
                type = LauncherApplication.getAppContext().getString(R.string.cat_name_media);
                break;
            case TYPE_ENTERTAINMENT_ID:// 2   ---- Entertainment    //娱乐
                type = LauncherApplication.getAppContext().getString(R.string.cat_name_entertainment);
                break;
            case TYPE_SOCIAL_ID: // 3    ---- Social   //社会
                type = LauncherApplication.getAppContext().getString(R.string.cat_name_social);
                break;
            case TYPE_BOOKS_ID: // 4  ---- Books   //书籍
                type = LauncherApplication.getAppContext().getString(R.string.cat_name_reading);
                break;
            case TYPE_FINANCE_ID:// 5  ---- Finance    //财务
                type = LauncherApplication.getAppContext().getString(R.string.cat_name_finance);
                break;
            case TYPE_SHOPPING_ID:// 6  ---- Shopping    //购物
                type = LauncherApplication.getAppContext().getString(R.string.cat_name_shopping);
                break;
            case TYPE_LIFESTYLE_ID:// 7 ---- Lifestyle   //生活方式
                type = LauncherApplication.getAppContext().getString(R.string.cat_name_lifestyle);
                break;
            case TYPE_TRANSPORTATION_ID:// 8 ---- Transportation   //交通运输
                type = LauncherApplication.getAppContext().getString(R.string.cat_name_transportation);
                break;
            case TYPE_GAMES_ID:// 9   ---- Games    //游戏
                type = LauncherApplication.getAppContext().getString(R.string.cat_name_game);
                break;
            case TYPE_TOOL_ID: // 10   ---- Tool  //工具
                type = LauncherApplication.getAppContext().getString(R.string.cat_name_utilities);
                break;
            case TYPE_WEATHER_ID:// 11 ---- Weather    //天气
                type = LauncherApplication.getAppContext().getString(R.string.cat_name_weather);
                break;
            case TYPE_OFFICE_ID: // 12 ---- Office    //办公
                type = LauncherApplication.getAppContext().getString(R.string.cat_name_office);
                break;
            case TYPE_UNCOMMON_ID: //13   ----   //不常用的
                type = LauncherApplication.getAppContext().getString(R.string.cat_name_builtin);
                break;
            default:
                break;
        }
        return type;
    }

    /* get merged app type of type id */
    public static int mergeAppTypeByTypeId(int typeId){
        int typeid = typeId;
        switch (typeId){
            case TYPE_MEDIA_ID: // 1  ---- Media  //媒体
                typeid = TYPE_LIFESTYLE_ID;
                break;
            case TYPE_ENTERTAINMENT_ID:// 2   ---- Entertainment    //娱乐
                typeid = TYPE_LIFESTYLE_ID;
                break;
            case TYPE_SOCIAL_ID: // 3    ---- Social   //社会
                typeid = TYPE_LIFESTYLE_ID;
                break;
            case TYPE_BOOKS_ID: // 4  ---- Books   //书籍
                typeid = TYPE_LIFESTYLE_ID;
                break;
            case TYPE_FINANCE_ID:// 5  ---- Finance    //财务
                typeid = TYPE_SHOPPING_ID;
                break;
            case TYPE_SHOPPING_ID:// 6  ---- Shopping    //购物
                typeid = TYPE_SHOPPING_ID;
                break;
            case TYPE_LIFESTYLE_ID:// 7 ---- Lifestyle   //生活方式
                typeid = TYPE_LIFESTYLE_ID;
                break;
            case TYPE_TRANSPORTATION_ID:// 8 ---- Transportation   //交通运输
                break;
            case TYPE_GAMES_ID:// 9   ---- Games    //游戏
                break;
            case TYPE_TOOL_ID: // 10   ---- Tool  //工具
                break;
            case TYPE_WEATHER_ID:// 11 ---- Weather    //天气
                typeid = TYPE_LIFESTYLE_ID;
                break;
            case TYPE_OFFICE_ID: // 12 ---- Office    //办公
                break;
            case TYPE_UNCOMMON_ID: //13   ----   //不常用的
                break;
        }
        return typeid;
    }

    public static class ClassifyInfo {

        public String packageName;
        public String type;
        public int typeId;

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getTypeId() {
            return typeId;
        }

        public void setTypeId(int typeId) {
            this.typeId = typeId;
        }

        @Override
        public String toString() {
            return "ClassifyInfo{" +
                    "packageName='" + packageName + '\'' +
                    ", type='" + type + '\'' +
                    ", typeId=" + typeId +
                    '}';
        }
    }

}
