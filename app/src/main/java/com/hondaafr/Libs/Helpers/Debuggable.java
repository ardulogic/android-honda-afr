package com.hondaafr.Libs.Helpers;

import android.util.Log;

public class Debuggable {

    protected int D = VERBOSE;
    public static int VERBOSE = 1;
    public static int INFO = 2;
    public static int IMPORTANT = 3;
    private static long start_ts;
    private static long end_ts;

    public String getTag() {
        return this.getClass().getSimpleName();
    }


    public void d(String msg, int level) {
        if (level >= D) {
            Log.d(getTag(), msg);
        }
    }

    public static void startStopwatch() {
        start_ts = System.currentTimeMillis();
    }

    public static void stopStopwatch() {
        end_ts = System.currentTimeMillis();

        Log.d("Debuggable", "Delay: " + (end_ts - start_ts) + "ms");
    }
}
