package com.dooqu.quiz.utils;

public class ThreadUtil
{
    public static void safeSleep(long sleeptMilli) {
        try {
            Thread.sleep(sleeptMilli);
        }
        catch (InterruptedException ex) {
            System.out.println(ex.toString());
        }
    }
}
