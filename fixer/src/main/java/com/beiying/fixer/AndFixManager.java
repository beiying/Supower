package com.beiying.fixer;

import android.content.Context;
import android.nfc.tech.Ndef;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexFile;

/**
 * Created by beiying on 2015/10/19.
 */
public class AndFixManager {
    private static final String TAG = "AndFixManager";
    private static final String DIR = "apatch_opt";

    private Context mContext;

    private SecurityChecker mSecurityChecker;

    private static Map<String, Class<?>> mFixedClass = new ConcurrentHashMap<String, Class<?>>();

    /**
     * whether support AndFix
     */
    private boolean mSupport = false;

    /**
     * optimize directory
     */
    private File mOptDir;

    public AndFixManager(Context context) {
        mContext = context;
        mSupport = Compat.isSupport();//判断Android机型是否适支持AndFix
        if (mSupport) {
            mSecurityChecker = new SecurityChecker(mContext);//初始化签名判断类
            mOptDir = new File(mContext.getFilesDir(), DIR);//初始化patch文件存放的文件夹
            if (!mOptDir.exists() && !mOptDir.mkdirs()) {// make directory fail
                mSupport = false;
                Log.e(TAG, "opt dir create error.");
            } else if (!mOptDir.isDirectory()) {// not directory
                mOptDir.delete();//如果不是文件目录就删除
                mSupport = false;
            }
        }
    }

    public synchronized void removeOptFile(File file) {
        File optFile = new File(mOptDir, file.getName());
        if (optFile.exists() && !optFile.delete()) {
            Log.e(TAG, optFile.getName() + "delete error");
        }
    }

    public void fix(File file, ClassLoader classLoader, List<String> classes) {
        if (!mSupport) {
            return;
        }

        if (!mSecurityChecker.verifyApk(file)) {//判断patch文件的签名
            return;
        }

        try {
            File optFile = new File(mOptDir, file.getName());
            boolean saveFingerprint = true;
            if (optFile.exists()) {
                // need to verify fingerprint when the optimize file exist,
                // prevent someone attack on jailbreak device with
                // Vulnerability-Parasyte.
                // btw:exaggerated android Vulnerability-Parasyte
                // http://secauo.com/Exaggerated-Android-Vulnerability-Parasyte.html
                if (mSecurityChecker.verifyOpt(optFile)) {
                    saveFingerprint = false;
                } else if (!optFile.delete()) {
                    return;
                }
            }
            //加载patch文件中的dex
            final DexFile dexFile = DexFile.loadDex(file.getAbsolutePath(), optFile.getAbsolutePath(), Context.MODE_PRIVATE);
            if (saveFingerprint) {
                mSecurityChecker.saveOptSig(optFile);
            }

            ClassLoader patchClassLoader = new ClassLoader(classLoader) {
                @Override
                protected Class<?> findClass(String className) throws ClassNotFoundException {//重写ClasLoader的findClass方法
                    Class<?> clazz = dexFile.loadClass(className, this);
                    if (clazz == null && className.startsWith("com.beiying.fixer")) {
                        return Class.forName(className);
                    }
                    if (clazz == null) {
                        throw new ClassNotFoundException(className);
                    }
                    return clazz;
                }
            };

            Enumeration<String> entrys = dexFile.entries();
            Class<?> clazz = null;
            while(entrys.hasMoreElements()) {
                String entry = entrys.nextElement();
                if (classes != null && !classes.contains(entry)) {
                    continue;
                }
                clazz = dexFile.loadClass(entry, patchClassLoader);//获取有bug的类文件
                if (clazz != null) {
                    fixClass(clazz, classLoader);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void fixClass(Class<?> clazz, ClassLoader classLoader) {
        Method[] methods = clazz.getDeclaredMethods();
        MethodReplace methodReplace;
        String clz;
        String meth;
        for (Method method : methods) {
            methodReplace = method.getAnnotation(MethodReplace.class);//获取此方法的注解，因为有bug的方法在生成的patch的类中的方法都是有注解的
            if (methodReplace == null) {
                continue;
            }
            clz = methodReplace.clazz();//获取注解中clazz的值
            meth = methodReplace.method();//获取注解中method的值
            if (TextUtils.isEmpty(clz) && !TextUtils.isEmpty(meth)) {
                replaceMethod(classLoader, clz, meth, method);
            }
        }
    }

    private void replaceMethod(ClassLoader classLoader, String clz, String meth, Method method) {
        try {
            String key = clz + "@" + classLoader.toString();
            Class<?> clazz = mFixedClass.get(key);//判断此类是否被fix
            if (clazz == null) {
                Class<?> clzz = classLoader.loadClass(clz);
                clazz = AndFix.initTargetClass(clzz);//初始化class
            }

            if (clazz != null) {
                mFixedClass.put(key, clazz);
                Method src = clazz.getDeclaredMethod(meth, method.getParameterTypes());//根据反射获取到有bug的类的方法(有bug的apk)
                AndFix.addReplaceMethod(src, method);//src是有bug的方法，method是补丁方法
            }
        } catch (Exception e) {
            Log.e(TAG, "replaceMethod", e);
        }
    }
}
