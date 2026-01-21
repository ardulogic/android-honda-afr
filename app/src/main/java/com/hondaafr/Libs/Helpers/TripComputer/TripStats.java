package com.hondaafr.Libs.Helpers.TripComputer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;


import java.util.UUID;

public class TripStats extends TotalStats {
    private static final String KEY_TRIP_SESSION_ID = "trip_session_id";

    private boolean isTripDialogOpen = false;

    public TripStats(String prefsName) {
        super(prefsName);
    }

    @Override
    public void load(Context context) {
        if (isTripDialogOpen) {
            return;
        }

        super.load(context);
        ensureSessionId(context);

        if (dataIsOld() && distanceKm > 0.1 && litersConsumed > 0.1) {
            isTripDialogOpen = true;
            new AlertDialog.Builder(context)
                    .setTitle("Continue Trip?")
                    .setMessage("Do you want to continue previous trip?")
                    .setPositiveButton("Start New", (dialog, which) -> {
                        reset(context.getApplicationContext());
                        isTripDialogOpen = false;
                    })
                    .setNegativeButton("Continue", (dialog, which) -> {
                        isTripDialogOpen = false;
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    public void reset(Context context) {
        updateSessionId(context);
        TripFuelTrackStore.cleanupOldTracks(context, getSessionId(context));
        super.reset(context);
    }

    public String getSessionId(Context context) {
        ensureSessionId(context);
        SharedPreferences prefs = getSharedPrefs(context);
        return prefs.getString(KEY_TRIP_SESSION_ID, "");
    }

    private void ensureSessionId(Context context) {
        SharedPreferences prefs = getSharedPrefs(context);
        if (!prefs.contains(KEY_TRIP_SESSION_ID)) {
            updateSessionId(context);
        }
    }

    private void updateSessionId(Context context) {
        SharedPreferences.Editor editor = getSharedPrefs(context).edit();
        editor.putString(KEY_TRIP_SESSION_ID, UUID.randomUUID().toString());
        editor.apply();
    }

    protected boolean dataIsOld() {
        return System.currentTimeMillis() - timeUpdated > 24 * 3600 * 1000;
    }

    @Override
    public String getName() {
        return "Fuel Trip";
    }

}
