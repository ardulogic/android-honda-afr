package com.hondaafr.Libs.Helpers.TripComputer;

import android.app.AlertDialog;
import android.content.Context;

public class TripStats extends TotalStats {

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

        if (dataIsOld()) {
            isTripDialogOpen = true;
            new AlertDialog.Builder(context)
                    .setTitle("Continue Trip?")
                    .setMessage("Do you want to continue previous trip?")
                    .setPositiveButton("Start New", (dialog, which) -> {
                        timeUpdated = System.currentTimeMillis();
                        isTripDialogOpen = false;
                    })
                    .setNegativeButton("Continue", (dialog, which) -> {
                        reset(context.getApplicationContext());
                        isTripDialogOpen = false;
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    protected boolean dataIsOld() {
        return System.currentTimeMillis() - timeUpdated > 6 * 3600 * 1000;
    }

    @Override
    public String getName() {
        return "Fuel Trip";
    }

}
