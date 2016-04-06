package com.beiying.performancefw;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by beiying on 2016/3/2.
 */
public class BYMemeoryManager {

    private static BYMemeoryManager sInstance;
    private Context mContext;

    private BYMemeoryManager(Context context) {
        mContext = context;
    }

    public static BYMemeoryManager getInstance(Context context) {
        synchronized (BYMemeoryManager.class) {
            if (sInstance == null) {
                sInstance = new BYMemeoryManager(context);
            }

            return sInstance;
        }
    }

    public Debug.MemoryInfo getProccessMemoryInfo(int pid) {
        final ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        Debug.MemoryInfo[] memoryInfos = am.getProcessMemoryInfo(new int[]{ pid });
        if (memoryInfos != null && memoryInfos.length > 0) {
            return memoryInfos[0];
        }
        //Debug.getMemoryInfo(memoryInfo);获取一个进程内存使用情况
        return null;
    }

    /**
     *
     * @param pid 进程id
     * @return 单位是kb
     */
    public int getProcessUSS(int pid) {
        Debug.MemoryInfo memoryInfo = getProccessMemoryInfo(pid);
        if (memoryInfo != null) {
            return memoryInfo.getTotalPrivateDirty();
        }
        return -1;
    }

    /**
     *
     * @param pid 进程id
     * @return 单位是kb
     */
    public int getProcessPSS(int pid) {
        Debug.MemoryInfo memoryInfo = getProccessMemoryInfo(pid);
        if (memoryInfo != null) {
            return memoryInfo.getTotalPss();
        }
        return -1;
    }

    /**
     *
     * @param pid 进程id
     * @return 单位是kb
     */
    public int getProcessRSS(int pid) {
        Debug.MemoryInfo memoryInfo = getProccessMemoryInfo(pid);
        if (memoryInfo != null) {
            return memoryInfo.getTotalSharedDirty();
        }

        return -1;
    }

    /**
     * 通过执行命令行获取应用的内存信息
     * */
    public static BYMemInfo getMemInfoByCMD(String packageName) {
        BYMemInfo result = null;
        String resultStr = null;
        resultStr = runCMD("dumpsys meminfo" + packageName);

        if(Build.VERSION.SDK_INT < 14)
        {
            result = parseMemInfoFrom2x(resultStr);
        }
        else if (Build.VERSION.SDK_INT < 19)
        {
            result = parseMemInfoFrom4x(resultStr);
        }
        else
        {
            result = parseMemInfoFrom44(resultStr);
        }

        return result;

    }

    public static BYMemInfo parseMemInfoFrom2x(String result) {
        String[] rows = null;

        BYMemInfo mi = new BYMemInfo();

        if (null == result) {
            return mi;
        }

        rows = result.split("\r\n");

        for (int i = 4; i < rows.length; ++i) {

            rows[i] = rows[i].trim();
            if (rows[i].indexOf("size") != -1) {
                mi.nativeHeapSize = Long.parseLong(rows[i].split("\\s+")[1]);
                mi.dalvikHeapSize = Long.parseLong(rows[i].split("\\s+")[2]);
            }

            if (rows[i].indexOf("allocated") != -1) {
                mi.nativeAllocated = Long.parseLong(rows[i].split("\\s+")[1]);
                mi.dalvikAllocated = Long.parseLong(rows[i].split("\\s+")[2]);
            }

            if (rows[i].indexOf("(Pss):") != -1) {
                mi.pss_Total= Long.parseLong(rows[i].split("\\s+")[4]);
            }

            if (rows[i].indexOf("(priv") != -1) {
                mi.private_dirty= Long.parseLong(rows[i].split("\\s+")[5]);
                break;
            }
        }
        return mi;
    }

    public static BYMemInfo parseMemInfoFrom4x(String result) {
        String[] rows = null;
        boolean nativeIsFind = false;
        boolean dalvikIsFind = false;
        boolean ashemIsFind = false;
        boolean ohterDevIsFind = false;
        boolean unknownIsFind = false;

        BYMemInfo mi = new BYMemInfo();

        rows = result.split("\r\n");

        for(int i = 7;i < rows.length;++i){

            rows[i] = rows[i].trim();

            if (!nativeIsFind && rows[i].indexOf("Native") != -1) {
                nativeIsFind = true;
                mi.pss_Native = Long.parseLong(rows[i].split("\\s+")[1]);
                mi.nativeHeapSize = Long.parseLong(rows[i].split("\\s+")[4]);
                mi.nativeAllocated = Long.parseLong(rows[i].split("\\s+")[5]);
                continue;
            }
            else if (!dalvikIsFind && rows[i].indexOf("Dalvik") != -1) {
                dalvikIsFind = true;
                mi.pss_Dalvik = Long.parseLong(rows[i].split("\\s+")[1]);
                mi.dalvikHeapSize = Long.parseLong(rows[i].split("\\s+")[4]);
                mi.dalvikAllocated = Long.parseLong(rows[i].split("\\s+")[5]);
                continue;
            }
            else if (!ashemIsFind && rows[i].indexOf("Ashmem") != -1) {
                ashemIsFind = true;
                mi.pss_Ashmem = Long.parseLong(rows[i].split("\\s+")[1]);
                continue;

            }
            else if (!ohterDevIsFind && rows[i].indexOf("Other dev") != -1) {
                ohterDevIsFind = true;
                mi.pss_OtherDev = Long.parseLong(rows[i].split("\\s+")[2]); // 注意这行从2开始，Other dev中间有个空格
                i += 6; // to Unknow
                continue;
            }
            else if (!unknownIsFind && rows[i].indexOf("Unknown") != -1) {
                unknownIsFind = true;
                mi.pss_Unknown = Long.parseLong(rows[i].split("\\s+")[1]);
                continue;
            }
            if (rows[i].indexOf("TOTAL") != -1) {
                mi.pss_Total = Long.parseLong(rows[i].split("\\s+")[1]);
                mi.private_dirty = Long.parseLong(rows[i].split("\\s+")[3]);
                break;
            }
        }

        return mi;
    }

    public static BYMemInfo parseMemInfoFrom44(String result) {
        String[] rows = null;

        boolean nativeIsFind = false;
        boolean dalvikIsFind = false;
        boolean otherDevIsFind = false;
        boolean graphicsIsFind = false; // since Android4.4
        boolean glIsFind = false; // since Android4.4
        boolean unknownIsFind = false;

        BYMemInfo memInfo = new BYMemInfo();
        rows = result.split("\t\n");
        for (int i = 7;i < rows.length;i++) {
            rows[i] = rows[i].trim();
            if (!nativeIsFind && rows[i].startsWith("Native")) {
                nativeIsFind = true;
                memInfo.pss_Native = Long.parseLong(rows[i].split("\\s+")[2]);
                memInfo.nativeHeapSize = Long.parseLong(rows[i].split("\\s+")[6]);
                memInfo.nativeAllocated = Long.parseLong(rows[i].split("\\s+")[7]);
            } else if (!dalvikIsFind && rows[i].indexOf("Dalvik") != -1) {
                dalvikIsFind = true;
                memInfo.pss_Dalvik = Long.parseLong(rows[i].split("\\s+")[2]);
                memInfo.dalvikHeapSize = Long.parseLong(rows[i].split("\\s+")[6]);
                memInfo.dalvikAllocated = Long.parseLong(rows[i].split("\\s+")[7]);
                continue;
            } else if (!otherDevIsFind && rows[i].indexOf("Other dev") != -1) {
                otherDevIsFind = true;
                memInfo.pss_OtherDev = Long.parseLong(rows[i].split("\\s+")[2]); // 注意这行从2开始，Other dev中间有个空格
                i += 5; // to Graphics
                continue;
            } else if (!graphicsIsFind && rows[i].indexOf("Graphics") != -1) {
                graphicsIsFind = true;
                memInfo.pss_graphics = Long.parseLong(rows[i].split("\\s+")[1]);
                continue;
            } else if (!glIsFind && rows[i].indexOf("GL") != -1) {
                glIsFind = true;
                memInfo.pss_gl = Long.parseLong(rows[i].split("\\s+")[1]);
                continue;
            } else if (!unknownIsFind && rows[i].indexOf("Unknown") != -1) {
                unknownIsFind = true;
                memInfo.pss_Unknown = Long.parseLong(rows[i].split("\\s+")[1]);
                continue;
            }

            if (rows[i].indexOf("TOTAL") != -1) {
                memInfo.pss_Total = Long.parseLong(rows[i].split("\\s+")[1]);
                memInfo.private_dirty = Long.parseLong(rows[i].split("\\s+")[2]);
                break;
            }
        }
        return memInfo;
    }
    public static String runCMD(String cmd) {
        ProcessBuilder execBuilder = null;
        execBuilder = new ProcessBuilder("su", "-c", cmd);
        Process exec = null;
        try {
            exec = execBuilder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        InputStream is = exec.getInputStream();
        String result = "";
        String line = "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                result += line;
                result += "\r\n";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void gc() {

    }

    public static class BYMemInfo {
        public final static BYMemInfo EMPTY = new BYMemInfo();

        public long time = 0;
        public long dalvikHeapSize = 0;
        public long dalvikAllocated = 0;
        public long nativeHeapSize = 0;
        public long nativeAllocated = 0;

        public long pss_Total = 0;
        public long pss_Native = 0;
        public long pss_Dalvik = 0;
        public long pss_OtherDev = 0;
        public long pss_Unknown = 0;
        public long pss_Ashmem = 0;
        public long pss_Stack = 0;
        public long pss_graphics = 0;
        public long pss_gl = 0;
        public long private_dirty = 0;

        @Override
        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            if (Build.VERSION.SDK_INT < 14)
            {
                sb.append("dHeapSize：");
                sb.append(dalvikHeapSize);
                sb.append(" KB");
                sb.append("\ndAllocated：");
                sb.append(dalvikAllocated);
                sb.append(" KB");
                sb.append("\nnHeapSize：");
                sb.append(nativeHeapSize);
                sb.append(" KB");
                sb.append("\nnAllocated：");
                sb.append(nativeAllocated);
                sb.append(" KB");
                sb.append("\npri_dirty : ");
                sb.append(private_dirty);
                sb.append(" KB");
                sb.append("\npss：");
                sb.append(pss_Total);
                sb.append(" KB");
            }
            else if(Build.VERSION.SDK_INT < 19)
            {
                sb.append("dHeapSize：");
                sb.append(dalvikHeapSize);
                sb.append(" KB");
                sb.append("\ndAllocated：");
                sb.append(dalvikAllocated);
                sb.append(" KB");
                sb.append("\npri_dirty : ");
                sb.append(private_dirty);
                sb.append(" KB");
                sb.append("\npss_T：");
                sb.append(pss_Total);
                sb.append(" KB");
                sb.append("\npss_D：");
                sb.append(pss_Dalvik);
                sb.append(" KB");
                sb.append("\npss_N：");
                sb.append(pss_Native);
                sb.append(" KB");
                sb.append("\npss_O：");
                sb.append(pss_OtherDev);
                sb.append(" KB");
                sb.append("\npss_U：");
                sb.append(pss_Unknown);
                sb.append(" KB");
            }
            else
            {
                sb.append("dHeapSize：");
                sb.append(dalvikHeapSize);
                sb.append(" KB");
                sb.append("\ndAllocated：");
                sb.append(dalvikAllocated);
                sb.append(" KB");
                sb.append("\npri_dirty : ");
                sb.append(private_dirty);
                sb.append(" KB");
                sb.append("\npss_T：");
                sb.append(pss_Total);
                sb.append(" KB");
                sb.append("\npss_D：");
                sb.append(pss_Dalvik);
                sb.append(" KB");
                sb.append("\npss_N：");
                sb.append(pss_Native);
                sb.append(" KB");
                sb.append("\npss_O：");
                sb.append(pss_OtherDev);
                sb.append(" KB");
                sb.append("\npss_U：");
                sb.append(pss_Unknown);
                sb.append(" KB");
                sb.append("\npss_GR：");
                sb.append(pss_graphics);
                sb.append(" KB");
                sb.append("\npss_GL：");
                sb.append(pss_gl);
                sb.append(" KB");
            }

            return sb.toString();
        }
    }


}
