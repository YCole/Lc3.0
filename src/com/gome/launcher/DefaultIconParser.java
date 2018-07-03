package com.gome.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.mediatek.launcher3.LauncherLog;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by huanghaihao on 2017/6/30.
 */

public class DefaultIconParser {
    private static final String TAG = "DefaultIconParser";
    private static DefaultIconParser mDefaultIconParser;
    private static Map<String, String> mIcons;
    private TagParser mTagParser;

    private DefaultIconParser() {
    }

    public static DefaultIconParser getInstance() {
        if (null == mDefaultIconParser) {
            mDefaultIconParser = new DefaultIconParser();
        }
        return mDefaultIconParser;
    }

    /**
     * 加载resId配置中的所有icon名字以及
     * Added by huanghaihao in 2017-7-4 for update icon
     *
     * @param context
     * @param resId
     * @param tagParser
     */
    public synchronized void getAllIcons(Context context, int resId, TagParser tagParser) {
        if (null == mIcons) {
            mIcons = new HashMap<>();
        }
        mIcons.clear();
        mTagParser = tagParser;
        try {
            mIcons.putAll(tagParser.parseXml(context.getResources().getXml(resId)));
        } catch (XmlPullParserException e) {
            LauncherLog.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            LauncherLog.e(TAG, e.getMessage(), e);
        } catch (Resources.NotFoundException e) {
            LauncherLog.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * 获取替换后icon
     * Added by huanghaihao in 2017-7-4 for update icon
     *
     * @param context
     * @param componentName
     * @return
     */
    public Bitmap getReplaceIcon(Context context, ComponentName componentName) {
        return mTagParser == null ? null : mTagParser.getReplaceIcon(context, componentName);
    }

    /**
     * 包含此componentName的Icon,返回pathname,否则返回null
     * Added by huanghaihao in 2017-7-4 for update icon
     *
     * @param componentName
     * @return
     */
    protected static String containIcon(ComponentName componentName) {
        for (String key : mIcons.keySet()) {
            if (componentName.flattenToString().equals(key)) {
                return mIcons.get(key);
            }
        }
        return null;
    }

    /**
     * 获取支持屏幕的Density的icon路径
     * Added by huanghaihao in 2017-7-4 for update icon
     *
     * @param context
     * @return
     */
    protected static String getIconDensity(Context context) {
        String path;
        int densityDpi = context.getResources().getDisplayMetrics().densityDpi;
        switch (densityDpi) {
            case 640:
                path = "xxxhdpi";
                break;
            case 480:
                path = "xxhdpi";
                break;
            case 320:
            default:
                path = "xhdpi";
                break;
        }
        return path;
    }

    protected interface TagParser {
        /**
         * Parses the tag
         *
         * @param parser
         * @return <String, String> <ComponentName,资源路径>
         * @throws XmlPullParserException
         * @throws IOException
         */
        Map<String, String> parseXml(XmlResourceParser parser)
                throws XmlPullParserException, IOException;

        /**
         * 获取替换后icon
         *
         * @param context
         * @param componentName
         * @return
         */
        Bitmap getReplaceIcon(Context context, ComponentName componentName);
    }

    public static class GmDefaultParser implements TagParser {
        //TAG
        private static final String TAG_ICONS = "icons";
        private static final String TAG_ICON = "icon";
        //ATTRS

        private static final String ATTR_COMPONENT_NAME = "componentName";
        private static final String ATTR_APP_NAME = "appName";
        private static final String ATTR_ICON_PATH = "iconPath";

        @Override
        public Map<String, String> parseXml(XmlResourceParser parser)
                throws XmlPullParserException, IOException {
            Map<String, String> map = new HashMap<>();
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                final String name = parser.getName();
                if (TAG_ICON.equals(name)) {
                    String componentName = AutoInstallsLayout.getAttributeValue(parser, ATTR_COMPONENT_NAME);
                    String iconPath = AutoInstallsLayout.getAttributeValue(parser, ATTR_ICON_PATH);
                    if (!TextUtils.isEmpty(iconPath) && !TextUtils.isEmpty(componentName)) {
                        map.put(componentName, iconPath);
                    }
                } else {
                    LauncherLog.w(TAG, "GmDefaultParser, found other tag " + name);
                }
            }
            return map;
        }

        @Override
        public Bitmap getReplaceIcon(Context context, ComponentName componentName) {
            Bitmap bitmap = null;
            if (null == mIcons) {
                return bitmap;
            }
            String imgName = containIcon(componentName);
            if (TextUtils.isEmpty(imgName)) {
                return bitmap;
            }
            String path = getIconDensity(context) + "/icons" + "/" + imgName;
            InputStream inputStream = null;
            try {
                inputStream = context.getAssets().open(path);
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                LauncherLog.e(TAG, e.getMessage(), e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        LauncherLog.e(TAG, e.getMessage(), e);
                    }
                }
            }
            if (LauncherLog.DEBUG) {
                LauncherLog.d(TAG, "componentName:" + componentName + ",path:" + path + ",bitmap:" + bitmap);
            }
            return bitmap;
        }
    }
}
