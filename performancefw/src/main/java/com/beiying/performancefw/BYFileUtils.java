package com.beiying.performancefw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Created by beiying on 2016/3/21.
 */
public class BYFileUtils {

    public static void closeReader(Reader br) {
        if (br != null) {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
