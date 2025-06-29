package com.hondaafr.Libs.UI.Scientific;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.Libs.UI.ScientificView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class CornerStatsPanel extends Panel {

    private final TextView textBig;
    private final TextView textSmaller;
    private final TripComputer mTripComputer;
    private LinearLayout panel;
    private ScientificView scView;

    public CornerStatsPanel(MainActivity mainActivity, ScientificView scView, TripComputer tripComputer) {
        panel = mainActivity.findViewById(R.id.layoutCornerStats);
        textBig = mainActivity.findViewById(R.id.textCornerStatsBig);
        textSmaller = mainActivity.findViewById(R.id.textCornerStatsSmall);
        mTripComputer = tripComputer;
        this.scView = scView;

        tripComputer.addListener("corner_stats", new TripComputerListener() {
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

            @Override
            public void onAfrValue(Double afr) {

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
                displayValues();
            }
        });
    }

    public void displayValues() {
        if (isInPip()) {
            if (scView.fuelStatsPanel.modeIs(FuelStatsPanel.FuelDisplayMode.FuelTotal)) {
                displayTotalValues();
            } else if (scView.fuelStatsPanel.modeIs(FuelStatsPanel.FuelDisplayMode.FuelTrip)) {
                displayTripValues();
            } else if (scView.fuelStatsPanel.modeIs(FuelStatsPanel.FuelDisplayMode.FuelTotal)) {
                displayInstValues();
            } else {
                displayAfrValues();
            }
        } else {
            displayAfrValues();
        }
    }

    @SuppressLint("DefaultLocale")
    public void displayTripValues() {
        textBig.setText(String.format("%.2f", mTripComputer.tripStats.getLitersPer100km()));
        textSmaller.setText(String.format("%.2f", mTripComputer.tripStats.getLiters()));
    }

    @SuppressLint("DefaultLocale")
    public void displayTotalValues() {
        textBig.setText(String.format("%.2f", mTripComputer.totalStats.getLitersPer100km()));
        textSmaller.setText(String.format("%.2f", mTripComputer.totalStats.getLiters()));
    }

    @SuppressLint("DefaultLocale")
    public void displayInstValues() {
        textBig.setText(String.format("%.2f", mTripComputer.instStats.getLphAvg()));
        textSmaller.setText(String.format("%.2f", mTripComputer.tripStats.getLiters()));
    }

    @SuppressLint("DefaultLocale")
    public void displayAfrValues() {
        textBig.setText(String.format("%.2f", mTripComputer.mSpartanStudio.lastSensorAfr));
        textSmaller.setText(String.format("%.2f", mTripComputer.afrHistory.getAvgDeviation(
                mTripComputer.mSpartanStudio.targetAfr)));
    }

    @Override
    public View getContainerView() {
        return panel;
    }

    @Override
    public boolean visibleInPip() {
        return true;
    }

    @Override
    public void enterPip() {
        super.enterPip();

        displayValues();
    }

    @Override
    public void exitPip() {
        super.exitPip();

        displayValues();
    }
}
