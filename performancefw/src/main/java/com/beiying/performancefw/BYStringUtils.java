package com.beiying.performancefw;

/**
 * Created by beiying on 2016/3/23.
 */
public class BYStringUtils {

    /**
     * 判断字符串是否为数值
     * @param s
     * @return
     */
    public static boolean isNumeric(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        int numStartPos = 0;
        if (s.charAt(0) == '-') {
            numStartPos = 1;
        }
        for (int i = s.length();--i >= numStartPos;) {
            int chr = s.charAt(i);
            if (chr < 48 || chr > 57) {
                return false;
            }
        }

        return true;
    }
}
