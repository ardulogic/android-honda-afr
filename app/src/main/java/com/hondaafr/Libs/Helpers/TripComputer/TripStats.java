package com.hondaafr.Libs.Helpers.TripComputer;

import android.content.Context;
import android.content.SharedPreferences;

public class TripStats extends TotalStats {

    // Constants for SharedPreferences keys
    private static final String KEY_TIMESTAMP = "timestamp";

    private long timeUpdated = 0L;

    public TripStats(String prefsName) {
        super(prefsName);
    }

    @Override
    public void addDistance(double distance) {
        super.addDistance(distance);

        timeUpdated = System.currentTimeMillis();
    }

    @Override
    public void addLiters(double liters) {
        super.addLiters(liters);

        timeUpdated = System.currentTimeMillis();
    }

    @Override
    public void save(Context context) {
        SharedPreferences.Editor editor = getSharedPrefs(context).edit();

        editor.putFloat(KEY_TOTAL_LITERS, (float) litersConsumed);
        editor.putFloat(KEY_TOTAL_DISTANCE, (float) distanceKm);
        editor.putLong(KEY_TIMESTAMP, timeUpdated);

        editor.apply(); // Asynchronous save
    }

    public void load(Context context) {
        SharedPreferences prefs = getSharedPrefs(context);

        litersConsumed = prefs.getFloat(KEY_TOTAL_LITERS, 0f);
        distanceKm = prefs.getFloat(KEY_TOTAL_DISTANCE, 0f);
        timeUpdated = prefs.getLong(KEY_TIMESTAMP, System.currentTimeMillis());

        if (dataIsOld()) {
            litersConsumed = 0;
            distanceKm = 0;
        }
    }

    private boolean dataIsOld() {
        return System.currentTimeMillis() - timeUpdated > 24 * 3600 * 1000;
    }
}
