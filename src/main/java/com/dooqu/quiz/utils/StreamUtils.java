package com.dooqu.quiz.utils;

import java.io.IOException;
import java.io.InputStream;

public class StreamUtils {
    public static boolean safeClose(InputStream inputStream) {
        if(inputStream == null) {
            return true;
        }
        try {
            inputStream.close();
            return true;
        }
        catch (IOException ex) {

            System.out.println(ex.toString());
        }
        return false;
    }
}
