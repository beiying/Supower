package com.beiying.fixer;

import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by beiying on 2015/10/19.
 */
public class AndFix {
    private static final String TAG = "AndFix";

    static {
        try {
            Runtime.getRuntime().loadLibrary("andfix");
        } catch (Throwable e) {
            Log.e(TAG, "loadLibrary", e);
        }
    }

    private static native boolean setup(boolean isArt, int apiLevel);

    private static native void replaceMethod(Method dest, Method src);

    private static native void setFieldFlag(Field field);

    public static boolean setup() {
        String vmVersion = System.getProperty("java.vm.version");
        boolean isArt = vmVersion != null && vmVersion.startsWith("2");
        int apiLevel = Build.VERSION.SDK_INT;
        return setup(isArt, apiLevel);
    }

    public static Class<?> initTargetClass(Class<?> clzz) {
        try {
            Class<?> targetClazz = Class.forName(clzz.getName(), true, clzz.getClassLoader());
            initFields(targetClazz);
            return targetClazz;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void initFields(Class<?> clazz) {
        Field[] srcFields = clazz.getDeclaredFields();
        for (Field field : srcFields) {
            Log.d(TAG, "modify " + clazz.getName() + "." + field.getName()
                    + " flag:");
            setFieldFlag(field);
        }
    }

    public static void addReplaceMethod(Method src, Method method) {
        try {
            replaceMethod(src, method);//调用了native方法，next code
            initFields(method.getDeclaringClass());
        } catch (Throwable e) {
            Log.e(TAG, "addReplaceMethod", e);
        }
    }
}
