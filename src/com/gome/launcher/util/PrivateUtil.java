package com.gome.launcher.util;

import android.util.Log;

import java.lang.reflect.Method;

public class PrivateUtil {

    public static String TAG = "PrivateUtil";

    public static Method getMethod(Class clazz, String methodName,
                                   final Class[] classes) throws Exception {
        Method method = null;
        try {
            method = clazz.getDeclaredMethod(methodName, classes);
        } catch (NoSuchMethodException e) {
            try {
                method = clazz.getMethod(methodName, classes);
            } catch (NoSuchMethodException ex) {
                if (clazz.getSuperclass() == null) {
                    return method;
                } else {
                    method = getMethod(clazz.getSuperclass(), methodName,
                            classes);
                }
            }
        }
        return method;
    }


    public static Object invoke(final Object obj, final String methodName,
                                final Class[] classes, final Object[] objects) {
        try {
            Method method = getMethod(obj.getClass(), methodName, classes);
            method.setAccessible(true);
            return method.invoke(obj, objects);
        } catch (Exception e) {
            //throw new RuntimeException(e);
            Log.e(TAG, e.toString());
            return null;
        }
    }

    public static Object invoke2(final Object obj, final String methodName,
                                final Class[] classes, final Object[] objects) {
        try {
            Method method = getMethod((Class) obj, methodName, classes);
            method.setAccessible(true);
            return method.invoke(obj, objects);
        } catch (Exception e) {
            //throw new RuntimeException(e);
            Log.e(TAG, e.toString());
            return null;
        }
    }

    public static Object invoke(final Object obj, final String methodName,
                                final Class[] classes) {
        return invoke(obj, methodName, classes, new Object[] {});
    }

    public static Object invoke(final Object obj, final String methodName) {
        return invoke(obj, methodName, new Class[] {}, new Object[] {});
    }





}
