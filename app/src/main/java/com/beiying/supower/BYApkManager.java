package com.beiying.supower;

/**
 * Created by beiying on 2015/10/22.
 */
public class BYApkManager {
    private static final String TAG = "BYApkManager";

    public static BYApkManager sInstance;

    private BYApkManager() {
        init();
    }

    public static BYApkManager getInstance() {
        if (sInstance == null) {
            synchronized (BYApkManager.class) {
                sInstance = new BYApkManager();
            }
        }
        return sInstance;
    }

    public void init() {
        //获取与Android系统有关的信息
        final String mVMVersion = System.getProperty("java.vm.version");
        final String mVMName = System.getProperty("java.vm.name");
        final String mBootPath = System.getProperty("java.boot.class.path");
        final String mJavaHome = System.getProperty("java.home");


    }


}
