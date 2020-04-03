package com.dooqu.quiz.utils;

public class ThreadUtils
{
    public static void safeSleep(long sleeptMilli) {
        try {
            Thread.sleep(sleeptMilli);
        }
        catch (InterruptedException ex) {
            System.out.println(ex.toString());
        }
    }

    public static void safeJoin(Thread thread) {
        if(thread != null) {
            try {
                thread.join();
            }
            catch (InterruptedException ex) {
                System.out.println(ex.toString());
            }
        }
    }
}
