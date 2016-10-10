package com.beiying.supower.framework.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by beiying on 2016/2/26.
 */
public class BYSuperUtils {
    public static final String CPU_ARMEABI = "armeabi";
    public static final String CPU_X86 = "x86";
    public static final String CPU_MIPS = "mips";

    /**
     *
     * @return ARM、ARMV7、X86、MIPS
     */
    public static String getCPUName() {
        try {
            FileReader fr = new FileReader("/proc/cpuinfo");
            BufferedReader br = new BufferedReader(fr);
            String text = br.readLine();
            br.close();
            String[] array = text.split(":\\s+", 2);
            if (array.length >= 2) {
                return array[1];
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getCPUArch(String cpuName) {
        String cpuArchitect = CPU_ARMEABI;
        if (cpuName.toLowerCase().contains("arm")) {
            cpuArchitect = CPU_ARMEABI;
        } else if (cpuName.toLowerCase().contains("x86")) {
            cpuArchitect = CPU_X86;
        } else if (cpuName.toLowerCase().contains("mips")) {
            cpuArchitect = CPU_MIPS;
        }

        return cpuArchitect;
    }
}
