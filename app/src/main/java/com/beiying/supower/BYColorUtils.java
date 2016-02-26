package com.beiying.supower;

import android.content.Context;
import android.graphics.Color;

import java.util.regex.Pattern;

/**
 * Created by beiying on 2016/2/15.
 */
public class BYColorUtils {
    public static int getResourceColor(Context context, int color) {
        int ret = 0x00ffffff;
        try {
            ret = context.getApplicationContext().getResources().getColor(color);
        } catch (Exception e) {

        }
        return ret;
    }

    /**
     * 将十六进制颜色代码转换为int
     * @param color
     * @return
     */
    public static int HexToColor(String color) {
        String reg = "#[a-f0-9A-F]{8}";
        if (!Pattern.matches(reg, color)) {
            color = "0x00ffffff";
        }

        return Color.parseColor(color);
    }

    /**
     * 修改颜色透明度
     * @param color
     * @param alpha
     * @return
     */
    public static int changeAlpha(int color, int alpha) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        return Color.argb(alpha, red, green, blue);
    }
}
