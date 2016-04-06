package com.beiying.performancefw;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 与进程信息相关的工具类
 * Created by beiying on 2016/3/21.
 */
public class BYProcessUtils {

    private static IProcess sProcessUtils;

    synchronized public static void init() {
        if (Build.VERSION.SDK_INT <= 21) {
            sProcessUtils = new Process4x();
        } else {
            sProcessUtils = new Process5x();
        }
    }
    public static String getPackageByUid(int uid)
    {
        return sProcessUtils.getPackageByUid(uid);
    }

    /*
     * 在选择被测应用后应该更新，包括广播和sdk自动化中，注意广播和sdk自动化中执行su操作有风险
     */
    public static boolean initUidPkgCache()
    {
        return sProcessUtils.initUidPkgCache();
    }

    /**
     * 是否至少有一个进程在运行指定包名的应用程序
     *
     * @param pkgName
     *            指定的包名
     * @return 是否至少有一个进程在运行指定包名的应用程序
     */
    public static boolean hasProcessRunPkg(String pkgName) {
        return sProcessUtils.hasProcessRunPkg(pkgName);
    }

    /**
     * 根据进程名，获取进程UID，反查UID，性能需要高
     *
     * @param context
     *            当前进程的上下文环境
     * @param pName
     *            进程名
     * @return 进程UID
     */
    public static int getProcessUID(Context context,String pName) {
        return sProcessUtils.getProcessUID(context, pName);
    }

    /**
     * 根据进程名，获取进程PID
     *
     * @param context
     *            当前进程的上下文环境
     * @param pName
     *            进程名
     * @return 进程PID
     */
    public static int getProcessPID(Context context, String pName) {
        return sProcessUtils.getProcessPID(context, pName);
    }

    /**
     * 判断进程是否在运行。
     *
     * @param sPid
     *            进程号
     * @return true 正在运行；false 停止运行
     */
    public static boolean isProcessAlive(Context context, String sPid) {
        return sProcessUtils.isProcessAlive(context, sPid);
    }

    public static List<ProcessInfo> getAllRunningAppProcessInfo(Context context) {
        return sProcessUtils.getAllRunningAppProcessInfo(context);
    }

    public static void killprocess(String proc) {
        sProcessUtils.killProcess(proc);
    }

    static class Process4x implements IProcess {
        // uid和package的对应
        private SparseArray<String> uidPkgCache = null;

        private SparseArray<String> getUidPkgCache()
        {
            return uidPkgCache;
        }

        @Override
        public List<ProcessInfo> getAllRunningAppProcessInfo(Context context) {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcessList = am
                    .getRunningAppProcesses();
            List<ProcessInfo> ret = new ArrayList<ProcessInfo>();
            for (ActivityManager.RunningAppProcessInfo info : appProcessList)
            {
                // pid目前不需要，默认赋值为-1
                ProcessInfo processInfo = new ProcessInfo(info.pid, info.processName, -1, info.uid);
                ret.add(processInfo);
            }

            return ret;
        }

        @Override
        public String getPackageByUid(int uid) {
            // Android4.x不需要此方法
            throw new UnsupportedOperationException();
        }

        @Override
        public int getProcessPID(Context context, String pname) {
            int pId = -1;
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcessList = am
                    .getRunningAppProcesses();
            int pLength = appProcessList.size();
            for (int i = 0; i < pLength; i++) {
                if (appProcessList.get(i).processName.equals(pname)) {
                    pId = appProcessList.get(i).pid;
                    break;
                }
            }
            return pId;
        }

        @Override
        public int getProcessUID(Context context, String pname) {
            int uId = 0;
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcessList = am
                    .getRunningAppProcesses();
            int pLength = appProcessList.size();
            for (int i = 0; i < pLength; i++) {
                if (appProcessList.get(i).processName.equals(pname)) {
                    uId = appProcessList.get(i).uid;
                    break;
                }
            }
            return uId;
        }

        @Override
        public boolean hasProcessRunPkg(String pname) {
            if (pname == null) return false;
            int uid = -1;
            int len = getUidPkgCache().size();
            for (int i = 0; i < len; i++)
            {
                if (pname.equals(getUidPkgCache().valueAt(i)))
                {
                    uid = getUidPkgCache().keyAt(i);
                    break;
                }
            }

            List<ProcessInfo> appProcessInfos = getAllRunningAppProcessInfo(null);
            for (ProcessInfo info : appProcessInfos) {
                if (info.uid == uid)
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isProcessAlive(Context context, String pid) {
            boolean isAlive = false;
            if (pid != null && context != null) {
                int processid = -1;
                try
                {
                    processid = Integer.parseInt(pid);
                }
                catch (Exception e)
                {
                    return false;
                }

                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> appProcessList = am
                        .getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo info : appProcessList) {
                    if (info.pid == processid) {
                        isAlive = true;
                        break;
                    }
                }
            }

            return isAlive;
        }

        @Override
        public void killProcess(String proc) {
            if (TextUtils.isEmpty(proc)) {
                return;
            }

            ArrayList<String> pid_list = new ArrayList<String>();
            ProcessBuilder pb = null;
            Process process = null;
            try {
                pb = new ProcessBuilder("");
                pb.redirectErrorStream(true);

                process = pb.start();
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                while((line = br.readLine()) != null) {
                    String regEx = "\\s[0-9][0-9]*\\s";
                    Pattern pat = Pattern.compile(regEx);
                    Matcher mat = pat.matcher(line);
                    if (mat.find()) {
                        String temp =  mat.group();
                        temp = temp.replaceAll("\\s","");
                        pid_list.add(temp);
                    }
                }

                for (int i = 0; i < pid_list.size(); i++) {
                    pb = new ProcessBuilder("su", "-c", "kill","-9",pid_list.get(i));
                    process = null;
                    process = pb.start();

                    pb = new ProcessBuilder("su", "-c", "kill"+" -9 "+pid_list.get(i));
                    process = null;
                    process = pb.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean initUidPkgCache() {
            return false;
        }
    }

    static class Process5x implements IProcess {
        private boolean isRootcheckedResult = true;

        private Map<String, ProcessInfo> procInfoCache =
                new HashMap<String, ProcessInfo>();

        // uid和package的对应
        private SparseArray<String> uidPkgCache = null;

        private SparseArray<String> getUidPkgCache()
        {
            return uidPkgCache;
        }

        @Override
        public List<ProcessInfo> getAllRunningAppProcessInfo(Context context) {
            List<ProcessInfo> appProcessList = new ArrayList<ProcessInfo>();
            int zygotePid = -1;
            int zygotePid64 = -1;


            ProcessBuilder pb = null;
            BufferedReader br = null;
            try {
                pb = new ProcessBuilder("sh","-c","ps | grep zygote");
                pb.redirectErrorStream(true);
                Process processZ = pb.start();
                br = new BufferedReader(new InputStreamReader(processZ.getInputStream()));

                String lineZ = "";
                while((lineZ = br.readLine()) != null) {
                    String[] arrayZ = lineZ.trim().split("\\s+");
                    if (arrayZ.length > 9) {
                        if (arrayZ[8].equals("zygote")) {
                            zygotePid = Integer.parseInt(arrayZ[1]);
                        } else if (arrayZ[8].equals("zygote64")) {
                            zygotePid64 = Integer.parseInt(arrayZ[1]);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                BYFileUtils.closeReader(br);
            }

            if (zygotePid < 0) {
                return appProcessList;
            }

            try {
                pb = new ProcessBuilder("su", "-c", "ps | grep u0_a");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                br = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line = "";
                while((line = br.readLine()) != null) {
                    String[] array = line.split("\\s+");
                    int uid = Integer.parseInt(array[0].substring(4)) + 1000;
                    int pid = Integer.parseInt(array[1]);
                    int ppid = Integer.parseInt(array[2]);
                    String name = array[8];
                    if (ppid == zygotePid || ppid == zygotePid64) {// 过滤掉系统子进程，只留下父进程是zygote的进程
                        ProcessInfo processInfo = new ProcessInfo(pid, name, ppid, uid);
                        appProcessList.add(processInfo);
                        procInfoCache.put(name, processInfo);
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                BYFileUtils.closeReader(br);
            }
            return appProcessList;
        }

        @Override
        public String getPackageByUid(int uid) {
            if (uidPkgCache == null) {
                initUidPkgCache();
            }

            return uidPkgCache.get(uid);
        }

        @Override
        public int getProcessPID(Context context,String pname) {
            int pid = -1;
            if (TextUtils.isEmpty(pname)) {
                return pid;
            }
            List<ProcessInfo> processInfos = getAllRunningAppProcessInfo(null);
            for (ProcessInfo info : processInfos) {
                if (info.name.equals(pname)) {
                    pid = info.pid;
                    break;
                }
            }
            return pid;
        }

        @Override
        public int getProcessUID(Context context, String pname) {
            int uid = -1;
            if (TextUtils.isEmpty(pname)) {
                return uid;
            }
            if (procInfoCache.isEmpty()) {
                List<ProcessInfo> processInfos = getAllRunningAppProcessInfo(null);
                for (ProcessInfo processInfo: processInfos) {
                    if (processInfo.name.equals(pname)) {
                        uid = processInfo.uid;
                        break;
                    }
                }
            } else {
                ProcessInfo info = procInfoCache.get(pname);
                /*
				 * 但是有初始记录的进程并不是主进程的情况，比如：
				 * 微信往往是com.tencent.mm:push进程活着，而com.tencent.mm进程初始是不在的
				 * 此时需要找与com.tencent.mm相似的com.tencent.mm:push进程信息作为替代
				 */
                if (info == null) {
                    for (ProcessInfo tpi : procInfoCache.values()) {
                        if (tpi.name.startsWith(pname)) {
                            info = tpi;
                            procInfoCache.put(pname, info);
                            break;
                        }
                    }
                }
                uid = info == null ? -1 : info.uid;
            }
            return uid;
        }

        @Override
        public boolean hasProcessRunPkg(String pname) {
            if (TextUtils.isEmpty(pname)) {
                return false;
            }

            int uid = -1;
            int len = getUidPkgCache().size();

            //如果是没有root过的手机，uidPkgCache是空的，采用替代方案，但对于进程命名中不包括包名的没有办法
            if  (len != 0) {
                for (int i = 0;i < len;i++) {
                    if (pname.equals(getUidPkgCache().valueAt(i))) {
                        uid = getUidPkgCache().keyAt(i);
                        break;
                    }
                }

                if (uid == -1) return false;
                List<ProcessInfo> appProcessInfos = getAllRunningAppProcessInfo(null);
                for (ProcessInfo info : appProcessInfos) {
                    if (info.uid == uid)
                    {
                        return true;
                    }
                }
                return false;
            } else {
                List<ProcessInfo> appProcessInfos = getAllRunningAppProcessInfo(null);
                for (ProcessInfo info : appProcessInfos) {
                    if (info.name.contains(pname))
                    {
                        return true;
                    }
                }
                return false;
            }
        }

        @Override
        public boolean isProcessAlive(Context context, String pid) {
            boolean isAlive = false;
            if (TextUtils.isEmpty(pid)) {
                return isAlive;
            }
            // 采用进入目录的方式判断会比较快
            BufferedReader reader = null;
            try {
                ProcessBuilder execBuilder = null;
                execBuilder = new ProcessBuilder("sh", "-c", "cd proc/" + pid);
                execBuilder.redirectErrorStream(true);
                Process exec = null;
                exec = execBuilder.start();
                InputStream is = exec.getInputStream();
                reader = new BufferedReader(
                        new InputStreamReader(is));

                String line = reader.readLine();
                if (line == null) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                BYFileUtils.closeReader(reader);
            }
            return isAlive;
        }

        @Override
        public void killProcess(String proc) {
            if (TextUtils.isEmpty(proc)) {
                return;
            }

            ArrayList<String> pid_list = new ArrayList<String>();
            ProcessBuilder pb = null;
            Process process = null;
            try {
                pb = new ProcessBuilder("");
                pb.redirectErrorStream(true);

                process = pb.start();
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                while((line = br.readLine()) != null) {
                    String regEx = "\\s[0-9][0-9]*\\s";
                    Pattern pat = Pattern.compile(regEx);
                    Matcher mat = pat.matcher(line);
                    if (mat.find()) {
                        String temp =  mat.group();
                        temp = temp.replaceAll("\\s","");
                        pid_list.add(temp);
                    }
                }

                for (int i = 0; i < pid_list.size(); i++) {
                    pb = new ProcessBuilder("su", "-c", "kill","-9",pid_list.get(i));
                    process = null;
                    process = pb.start();

                    pb = new ProcessBuilder("su", "-c", "kill"+" -9 "+pid_list.get(i));
                    process = null;
                    process = pb.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean initUidPkgCache() {
            if (!isRootcheckedResult) {
                return false;
            }

            uidPkgCache = new SparseArray<String>();
            String pkgListPath = "/data/system/packages.list";
            try {
                BYSystemTools.doCMD("chmod 777" + pkgListPath);
            } catch (Exception e) {
                isRootcheckedResult = false;
                e.printStackTrace();
                return false;
            }

            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(pkgListPath));
                String temp;
                while((temp = br.readLine()) != null) {
                    String[] tempArray = temp.trim().split("\\s+");
                    if (tempArray.length > 2) {
                        if(BYStringUtils.isNumeric(tempArray[1])) {
                            uidPkgCache.put(Integer.parseInt(tempArray[1]), tempArray[0]);
                        }
                    }
                }

                if (uidPkgCache.size() <= 0) {
                    isRootcheckedResult = false;
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                isRootcheckedResult = false;
                return false;
            }

            return false;
        }
    }

    static interface IProcess {
        List<ProcessInfo> getAllRunningAppProcessInfo(Context context);
        String getPackageByUid(int uid);
        int getProcessPID(Context context,String pname);
        int getProcessUID(Context context,String pname);
        boolean hasProcessRunPkg(String pname);
        boolean isProcessAlive(Context context,String pid);
        void killProcess(String proc);
        boolean initUidPkgCache();
    }

    public static class ProcessInfo {
        public String name; // 进程名
        public int pid;  // PID
        public int ppid; // 父PID
        public int uid; //  UID

        public ProcessInfo(int pid, String name, int ppid, int uid)
        {
            this.pid = pid;
            this.name = name;
            this.ppid = ppid;
            this.uid = uid;
        }

        @Override
        public int hashCode()
        {
            int result = 17;
            if (name != null)
                result = 37 * result + name.hashCode();
            result = 37 * result + (int) (pid ^ (pid >>> 32));
            result = 37 * result + (int) (ppid ^ (ppid >>> 32));
            result = 37 * result + (int) (uid ^ (uid >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o instanceof ProcessInfo)
            {
                ProcessInfo another = (ProcessInfo)o;
                if (this.pid == another.pid
                        && this.ppid == another.ppid
                        && this.name != null
                        && another.name != null
                        && this.name.equals(another.name))
                {
                    return true;
                }
            }
            return false;
        }
    }
}
