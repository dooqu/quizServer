package com.dooqu.quiz.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtils {
    public static boolean safeClose(Closeable stream) {
        if(stream == null) {
            return true;
        }
        try {
            stream.close();
            return true;
        }
        catch (IOException ex) {
            System.out.println(ex.toString());
        }
        return false;
    }
}
