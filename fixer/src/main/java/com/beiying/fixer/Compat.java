package com.beiying.fixer;

import android.os.Build;

import java.lang.reflect.Method;

/**
 * Created by beiying on 2015/10/19.
 */
public class Compat {
    public static boolean isChecked = false;
    public static boolean isSupport = false;

    public static synchronized  boolean isSupport() {
        if (isChecked)
            return isSupport;
        isChecked = true;
        if (isYunOS() && AndFix.setup() && isSupportSDKVersion()) {//判断系统是否是YunOs系统，YunOs系统是阿里巴巴的系统;判断是Dalvik还是Art虚拟机，来注册Native方法;根据sdk版本判断是否支持
            isSupport = true;
        }

        if (isBlackList()) {
            isSupport = false;
        }

        return isSupport;
    }

    private static boolean isSupportSDKVersion() {
        if (Build.VERSION.SDK_INT >= 8 && Build.VERSION.SDK_INT <=23) {
            return true;
        }
        return false;
    }

    private static boolean isYunOS() {
        String version = null;
        String vmName = null;
        try {
            Method m = Class.forName("android.os.SystemProperties").getMethod("get", String.class);
            version = (String) m.invoke(null, "ro.yunos.version");
            vmName = (String) m.invoke(null, "java.vm.name");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if ((vmName != null && vmName.toLowerCase().contains("lemur"))
                || (version != null && version.trim().length() > 0)) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isBlackList() {
        return false;
    }
}
