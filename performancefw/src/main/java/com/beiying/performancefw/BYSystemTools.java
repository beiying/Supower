package com.beiying.performancefw;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by beiying on 2016/3/2.
 */
public class BYSystemTools {
    public static BufferedReader getInfoByExecCmd(String  cmd) throws IOException {
        Process process =Runtime.getRuntime().exec(cmd);
        if (process != null) {
            InputStreamReader ir = new InputStreamReader(process.getInputStream());
            return new BufferedReader(ir);
        }
        return null;
    }

    /**
     * 使用dalvik虚拟机内存编码
     * 有待检验
     * @param context
     * @param img
     * @return
     */
    public static Bitmap getBitmapByDalvik(Context context, String img) {
        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open(img);
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 使用native memory空间编码
     * 有待检验性能
     * @param context
     * @param img
     * @return
     */
    public static Bitmap getBitmapByNative(Context context, String img) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            AssetManager am = context.getAssets();
            InputStream is = am.open(img);
            return BitmapFactory.decodeStream(is, null, options);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 执行命令行;返回命令执行结果，每行数据以“\r\n”分隔
     * @param cmd
     * @return
     */
    public static String runCMD(String cmd) {
        if (TextUtils.isEmpty(cmd)) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        String line = "";
        Process process;
        try {
            process = Runtime.getRuntime().exec(new String[]{"su","-c",cmd});
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while((line = br.readLine()) != null) {
                line = line.trim();
                if (TextUtils.isEmpty(line)) {
                    continue;
                }

                sb.append(line);
                sb.append("\r\n");
            }

            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static void doCMD(String cmd) throws Exception{
        Process process= Runtime.getRuntime().exec("su");
        DataOutputStream dos = new DataOutputStream(process.getOutputStream());
        dos.writeBytes(cmd + "\n");
        dos.writeBytes("exit\n");
        dos.flush();
        dos.close();

        process.waitFor();
    }

    public static void doCMDs(List<String> cmds) throws Exception{
        Process process = Runtime.getRuntime().exec("su");
        DataOutputStream dos = new DataOutputStream(process.getOutputStream());
        for (String cmd : cmds) {
            dos.writeBytes(cmd + "\n");
        }
        dos.writeBytes("exit\n");
        dos.flush();
        dos.close();

        process.waitFor();
    }
}
