package com.hondaafr.Libs.Helpers;

import android.util.Log;

public class Debuggable {

    protected int D = 1;

    private static long start_ts;
    private static long end_ts;

    public String getTag() {
        return this.getClass().getSimpleName();
    }


    public void d(String msg, int level) {
        if (D >= level) {
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
