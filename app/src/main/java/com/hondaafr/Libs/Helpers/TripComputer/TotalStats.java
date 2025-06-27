package com.hondaafr.Libs.Helpers.TripComputer;

import android.content.Context;
import android.content.SharedPreferences;

public class TotalStats {
    protected double distanceKm;
    protected double litersConsumed;

    private final String prefsName;

    // Constants for SharedPreferences keys
    protected static final String KEY_TOTAL_LITERS = "total_liters";
    protected static final String KEY_TOTAL_DISTANCE = "total_distance";

    public TotalStats(String prefsName) {
        this.prefsName = prefsName;
    }

    public void addDistance(double distance) {
        distanceKm += distance;
    }

    public void addLiters(double liters) {
        litersConsumed += liters;
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
        SharedPreferences.Editor editor = getSharedPrefs(context).edit();

        editor.putFloat(KEY_TOTAL_LITERS, (float) litersConsumed);
        editor.putFloat(KEY_TOTAL_DISTANCE, (float) distanceKm);

        editor.apply(); // Asynchronous save
    }

    public void load(Context context) {
        SharedPreferences prefs = getSharedPrefs(context);

        litersConsumed = prefs.getFloat(KEY_TOTAL_LITERS, 0f);
        distanceKm = prefs.getFloat(KEY_TOTAL_DISTANCE, 0f);
    }

    public void reset() {
        litersConsumed = 0;
        distanceKm = 0;
    }
}
