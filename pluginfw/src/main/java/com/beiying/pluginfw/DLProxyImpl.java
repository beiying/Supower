package com.beiying.pluginfw;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.beiying.pluginfw.util.DLConstants;

import java.lang.reflect.Constructor;

/**
 * Created by beiying on 2016/3/1.
 */
public class DLProxyImpl {

    private Activity mProxyActivity;

    private String mClass;
    private String mPackageName;

    private DLPluginPackage mPluginPackage;
    private DLPluginManager mPluginManager;

    private AssetManager mAssetManager;
    private Resources mResources;
    private Resources.Theme mTheme;
    private ActivityInfo mActivityInfo;
    protected DLPlugin mPluginActivity;


    public DLProxyImpl(Activity activity) {
        mProxyActivity = activity;
    }

    public void onCreate(Intent intent) {
        // set the extra's class loader
        intent.setExtrasClassLoader(DLConfigs.sPluginClassloader);

        mPackageName = intent.getStringExtra(DLConstants.EXTRA_PACKAGE);
        mClass = intent.getStringExtra(DLConstants.EXTRA_CLASS);

        mPluginManager = DLPluginManager.getInstance(mProxyActivity);
        mPluginPackage = mPluginManager.getPluginPackage(mPackageName);
        mAssetManager = mPluginPackage.assetManager;
        mResources = mPluginPackage.resources;

        initializeActivityInfo();
        handleActivityInfo();
        launchTargetActivity();
    }

    private void initializeActivityInfo() {
        PackageInfo packageInfo = mPluginPackage.packageInfo;
        if ((packageInfo.activities != null) && (packageInfo.activities.length > 0)) {
            if (mClass == null) {
                mClass = packageInfo.activities[0].name;
            }

            //Finals 修复主题BUG
            int defaultTheme = packageInfo.applicationInfo.theme;
            for (ActivityInfo a : packageInfo.activities) {
                if (a.name.equals(mClass)) {
                    mActivityInfo = a;
                    // Finals ADD 修复主题没有配置的时候插件异常
                    if (mActivityInfo.theme == 0) {
                        if (defaultTheme != 0) {
                            mActivityInfo.theme = defaultTheme;
                        } else {
                            if (Build.VERSION.SDK_INT >= 14) {
                                mActivityInfo.theme = android.R.style.Theme_DeviceDefault;
                            } else {
                                mActivityInfo.theme = android.R.style.Theme;
                            }
                        }
                    }
                }
            }

        }

    }

    /***
     * 设置Activity的主题
     */
    private void handleActivityInfo() {
        if (mActivityInfo.theme > 0) {
            mProxyActivity.setTheme(mActivityInfo.theme);
        }
        Resources.Theme superTheme = mProxyActivity.getTheme();
        mTheme = mResources.newTheme();
        mTheme.setTo(superTheme);
        // Finals适配三星以及部分加载XML出现异常BUG
        try {
            mTheme.applyStyle(mActivityInfo.theme, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // TODO: handle mActivityInfo.launchMode here in the future.
    }

    private void launchTargetActivity() {
        try {
            Class<?> localClaass = getClassLoader().loadClass(mClass);
            Constructor<?> localConstructor = localClaass.getConstructor(new Class[]{});
            Object instance = localConstructor.newInstance(new Object(){});
            mPluginActivity = (DLPlugin) instance;
            ((DLAttachable) mProxyActivity).attach(mPluginActivity, mPluginManager);
            mPluginActivity.attach(mProxyActivity, mPluginPackage);

            Bundle bundle = new Bundle();
            bundle.putInt(DLConstants.FROM, DLConstants.FROM_EXTERNAL);
            mPluginActivity.onCreate(bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public ClassLoader getClassLoader() {
        return mPluginPackage.classLoader;
    }

    public AssetManager getAssets() {
        return mAssetManager;
    }

    public Resources getResources() {
        return mResources;
    }

    public Resources.Theme getTheme() {
        return mTheme;
    }

    public DLPlugin getRemoteActivity() {
        return mPluginActivity;
    }
}
