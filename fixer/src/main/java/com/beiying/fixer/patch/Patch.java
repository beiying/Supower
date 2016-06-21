package com.beiying.fixer.patch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Created by beiying on 2015/10/20.
 */
public class Patch implements Comparable<Patch> {
    private static final String ENTRY_NAME = "META-INF/PATCH.MF";
    private static final String CLASSES = "-Classes";
    private static final String PATCH_CLASSES = "Patch-Classes";
    private static final String CREATED_TIME = "Created-Time";
    private static final String PATCH_NAME = "Patch-Name";

    private final File mFile;
    private String mName;
    private Date mTime;

    /**
     * classes of patch
     */
    private Map<String, List<String>> mClassesMap;

    public Patch(File file) throws IOException{
        mFile = file;
        init();
    }

    private void init() throws IOException{
        JarFile jarFile = null;
        InputStream inputStream = null;

        try {
            jarFile = new JarFile(mFile);//使用JarFile读取Patch文件
            JarEntry entry = jarFile.getJarEntry(ENTRY_NAME);//获取META-INF/PATCH.MF文件
            inputStream = jarFile.getInputStream(entry);
            Manifest manifest = new Manifest(inputStream);
            Attributes main = manifest.getMainAttributes();
            mName = main.getValue(PATCH_NAME);//获取PATCH.MF属性Patch-Name
            mTime = new Date(main.getValue(CREATED_TIME));//获取PATCH.MF属性Created-Time

            mClassesMap = new HashMap<String, List<String>>();
            Attributes.Name attrName;
            String name;
            List<String> strings;
            for (Iterator<?> it = main.keySet().iterator();it.hasNext();) {
                attrName = (Attributes.Name) it.next();
                name = attrName.toString();
                if (name.endsWith(CLASSES)) {//判断name的后缀是否是-Classes，并把name对应的值加入到集合中，对应的值就是class类名的列表
                    strings = Arrays.asList(main.getValue(attrName).split(","));
                    if (name.equalsIgnoreCase(PATCH_CLASSES)) {
                        mClassesMap.put(mName, strings);
                    } else {
                        mClassesMap.put(name.trim().substring(0, name.length() - 8), strings);
                    }
                }
            }

        } finally {
            if (jarFile != null)
                jarFile.close();
            if (inputStream != null)
                inputStream.close();
        }
    }

    public String getName() {
        return mName;
    }

    public File getFile() {
        return mFile;
    }

    public Set<String> getPatchNames() {
        return mClassesMap.keySet();
    }

    public List<String> getClasses(String patchName) {
        return mClassesMap.get(patchName);
    }

    public Date getTime() {
        return mTime;
    }

    @Override
    public int compareTo(Patch patch) {
        return mTime.compareTo(patch.getTime());
    }
}
