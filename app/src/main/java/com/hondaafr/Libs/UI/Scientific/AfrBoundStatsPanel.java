package com.hondaafr.Libs.UI.Scientific;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.Button;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class AfrBoundStatsPanel extends Panel {

    private final MaterialButtonToggleGroup panel;
    private final Button mToggleClearAfrMin;
    private final Button mToggleClearAfrAll;
    private final Button mToggleClearAfrMax;
    private final TripComputer mTripComputer;

    public AfrBoundStatsPanel(MainActivity mainActivity, TripComputer mTripComputer) {
        this.mTripComputer = mTripComputer;

        panel = mainActivity.findViewById(R.id.toggleAfrBoundStatsGroup);

        mToggleClearAfrMin = mainActivity.findViewById(R.id.buttonClearAfrMin);
        mToggleClearAfrMin.setOnClickListener(v -> mTripComputer.afrHistory.clearMin());

        mToggleClearAfrAll = mainActivity.findViewById(R.id.buttonClearAfrAll);
        mToggleClearAfrAll.setOnClickListener(v -> mTripComputer.afrHistory.clear());

        mToggleClearAfrMax = mainActivity.findViewById(R.id.buttonClearAfrMax);
        mToggleClearAfrMax.setOnClickListener(v -> mTripComputer.afrHistory.clearMax());

        this.mTripComputer.addListener("afr_bound_stats", new TripComputerListener() {
            @Override
            public void onGpsUpdate(Double speed, double distanceIncrement) {

            }

            @Override
            public void onGpsPulse(PhoneGps gps) {

            }

            @Override
            public void onAfrPulse(boolean isActive) {

            }

            @Override
            public void onAfrTargetValue(double targetAfr) {

            }

            @SuppressLint("DefaultLocale")
            @Override
            public void onAfrValue(Double afr) {
                mToggleClearAfrMin.setText(String.format("%.1f", mTripComputer.afrHistory.getMinValue()));
                mToggleClearAfrAll.setText(String.format("%.1f", mTripComputer.afrHistory.getAvg()));
                mToggleClearAfrMax.setText(String.format("%.1f", mTripComputer.afrHistory.getMaxValue()));
            }

            @Override
            public void onObdPulse(boolean isActive) {

            }

            @Override
            public void onObdActivePidsChanged() {

            }

            @Override
            public void onObdValue(ObdReading reading) {

            }

            @Override
            public void onCalculationsUpdated() {

            }
        });
    }

    @Override
    public View getContainerView() {
        return panel;
    }
}
