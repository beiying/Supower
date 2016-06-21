package com.beiying.fixer.patch;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.beiying.fixer.AndFixManager;
import com.beiying.fixer.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by beiying on 2015/10/19.
 */
public class PatchManager {
    private static final String TAG = "PatchManager";
    private static final String DIR = "apatch";
    private static final String SUFFIX = ".apatch";
    private static final String SP_NAME = "_andfix";
    private static final String SP_VERSION = "version";

    private static PatchManager sInstance = null;
    private final Context mContext;
    private final AndFixManager mAndFixManager;
    private final File mPatchDir;
    private final SortedSet<Patch> mPatchs;
    private final Map<String, ClassLoader> mLoaders;

    private PatchManager(Context context) {
        mContext = context;
        mAndFixManager = new AndFixManager(context);
        mPatchDir = new File(mContext.getFilesDir(), DIR);
        mPatchs = new ConcurrentSkipListSet<Patch>();//初始化存在Patch类的集合,此类适合大并发
        mLoaders = new ConcurrentHashMap<String, ClassLoader>();//初始化存放类对应的类加载器集合
    }

    public static PatchManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PatchManager.class) {
                sInstance = new PatchManager(context);
            }
        }
        return sInstance;
    }

    public void init(String appVersion) {
        if (!mPatchDir.exists() && !mPatchDir.mkdirs()) {
            Log.e(TAG, "patch dir create error");
        } else if(!mPatchDir.isDirectory()) {
            mPatchDir.delete();
            return;
        }

        SharedPreferences sp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String version = sp.getString(SP_VERSION, null);//根据你传入的版本号和之前的对比，做不同的处理
        if (version == null || !version.equalsIgnoreCase(appVersion)) {
            cleanPath();//删除本地patch文件
            sp.edit().putString(SP_VERSION,appVersion).commit();//并把传入的版本号保存
        } else {
            initPatchs();//初始化patch列表，把本地的patch文件加载到内存
        }
    }

    private void initPatchs() {
        File[] files = mPatchDir.listFiles();
        for (File file : files) {
            addPatch(file);
        }
    }

    private Patch addPatch(File file) {
        Patch patch = null;
        if (file.getName().endsWith(SUFFIX)) {
            try {
                patch = new Patch(file);
                mPatchs.add(patch);//把patch实例存储到内存的集合中,在PatchManager实例化集合
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return patch;
    }

    private void cleanPath() {
        File[] files = mPatchDir.listFiles();
        for (File file : files) {
            mAndFixManager.removeOptFile(file);//删除所有的本地缓存patch文件
            if (FileUtil.deleteFile(file)) {
                Log.e(TAG, file.getName() + "delete error");
            }
         }
    }

    private void addPatch(String path) throws IOException {
        File src = new File(path);
        File dest = new File(mPatchDir, src.getName());

        if (!src.exists()) {
            throw new FileNotFoundException(path);
        }

        if (dest.exists()) {
            Log.d(TAG, "patch [" + path + "] has be loaded.");
            return;
        }

        FileUtil.copyFile(src, dest);// copy to patch's directory
        Patch patch = addPatch(dest);
        if (patch != null) {
            loadPatch(patch);
        }
    }

    /**
     * load patch,call when application start
     *
     */
    public void loadPatch() {
        mLoaders.put("*", mContext.getClassLoader());
        Set<String> patchNames;
        List<String> classes;
        for (Patch patch : mPatchs) {
            patchNames = patch.getPatchNames();
            for (String patchName : patchNames) {
                classes = patch.getClasses(patchName);//获取patch对用的class类的集合List
                mAndFixManager.fix(patch.getFile(), mContext.getClassLoader(), classes);//修复bug方法
            }
        }
    }

    /**
     * Load specific patch
     * @param patch
     */
    public void loadPatch(Patch patch) {
        Set<String> patchNames = patch.getPatchNames();
        List<String> classes;
        ClassLoader cl;
        for (String patchName : patchNames) {
            if (mLoaders.containsKey("*")) {
                cl = mContext.getClassLoader();
            } else {
                cl = mLoaders.get(patchName);
            }

            if (cl != null) {
                classes = patch.getClasses(patchName);
                mAndFixManager.fix(patch.getFile(), cl, classes);
            }
        }
    }

    /**
     * load patch,call when plugin be loaded. used for plugin architecture.</br>
     *
     * need name and classloader of the plugin
     *
     * @param patchName
     *            patch name
     * @param classLoader
     *            classloader
     */
    public void loadPatch(String patchName, ClassLoader classLoader) {
        mLoaders.put(patchName, classLoader);
        Set<String> patchNames;
        List<String> classes;
        for (Patch patch : mPatchs) {
            patchNames = patch.getPatchNames();
            if (patchNames.contains(patchName)) {
                classes = patch.getClasses(patchName);
                mAndFixManager.fix(patch.getFile(), classLoader, classes);
            }
        }
    }
}
