package com.hondaafr.Libs.Helpers.TripComputer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class TotalStats {
    protected double distanceKm;
    protected double litersConsumed;

    private final String prefsName;

    protected long timeUpdated = 0L;
    protected long timeSaved = 0L;

    // Constants for SharedPreferences keys
    protected static final String KEY_TOTAL_LITERS = "total_liters";
    protected static final String KEY_TOTAL_DISTANCE = "total_distance";
    protected static final String KEY_TIMESTAMP = "timestamp";


    public TotalStats(String prefsName) {
        this.prefsName = prefsName;
    }

    public void addDistance(double distance) {
        distanceKm += distance;
        timeUpdated = System.currentTimeMillis();
    }

    public void addLiters(double liters) {
        litersConsumed += liters;
        timeUpdated = System.currentTimeMillis();
    }

    public double getLiters() {
        return litersConsumed;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public Double getLitersPer100km() {
        if (distanceKm > 0) {
            return Math.min((litersConsumed / distanceKm) * 100, 30);
        } else {
            return 0D;
        }
    }

    protected SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
    }

    public void save(Context context) {
        if (timeSaved != timeUpdated) {
            SharedPreferences.Editor editor = getSharedPrefs(context).edit();

            editor.putFloat(KEY_TOTAL_LITERS, (float) litersConsumed);
            editor.putFloat(KEY_TOTAL_DISTANCE, (float) distanceKm);
            editor.putLong(KEY_TIMESTAMP, timeUpdated);
            editor.apply(); // Asynchronous save

            timeSaved = timeUpdated;

            Log.d("Stats", prefsName + " is saved");
        } else {
            Log.d("Stats", prefsName + " skipped saving (its the same)");
        }
    }

    public void load(Context context) {
        SharedPreferences prefs = getSharedPrefs(context);

        litersConsumed = prefs.getFloat(KEY_TOTAL_LITERS, 0f);
        distanceKm = prefs.getFloat(KEY_TOTAL_DISTANCE, 0f);
        timeUpdated = prefs.getLong(KEY_TIMESTAMP, System.currentTimeMillis());
        timeSaved = timeUpdated;

        Log.d("Stats", prefsName + " is loaded");
    }

    public void reset(Context context) {
        litersConsumed = 0;
        distanceKm = 0;
        timeUpdated = System.currentTimeMillis();

        save(context);
    }
}
