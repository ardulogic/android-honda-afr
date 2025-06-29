package com.hondaafr.Libs.Helpers.TripComputer;

import android.content.Context;

public class TripStats extends TotalStats {

    public TripStats(String prefsName) {
        super(prefsName);
    }

    @Override
    public void load(Context context) {
        super.load(context);

        if (dataIsOld()) {
            reset(context);
        }
    }

    private boolean dataIsOld() {
        return System.currentTimeMillis() - timeUpdated > 15 * 60 * 1000;
    }

    @Override
    public String getName() {
        return "Fuel Trip";
    }

}
