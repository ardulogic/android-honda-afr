package com.hondaafr.Libs.UI.Scientific.Panels;

import android.annotation.SuppressLint;
import android.widget.TextView;

import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class CornerStatsPanel extends Panel {

    private final TextView textBig;
    private final TextView textSmaller;

    @Override
    public int getContainerId() {
        return R.id.layoutCornerStats;
    }

    @Override
    public String getListenerId() {
        return "corner_stats_panel";
    }

    public CornerStatsPanel(MainActivity mainActivity, TripComputer tripComputer, UiView parent) {
        super(mainActivity, tripComputer, parent);

        textBig = mainActivity.findViewById(R.id.textCornerStatsBig);
        textSmaller = mainActivity.findViewById(R.id.textCornerStatsSmall);
    }

    @Override
    public void onCalculationsUpdated() {
        updateDisplay();
    }

    public void updateDisplay() {
        if (isInPip()) {
            FuelStatsPanel fuelStatsPanel = parent.getPanel(FuelStatsPanel.class);

            if (fuelStatsPanel.modeIs(FuelStatsPanel.FuelDisplayMode.FuelTotal)) {
                displayTotalValues();
            } else if (fuelStatsPanel.modeIs(FuelStatsPanel.FuelDisplayMode.FuelTrip)) {
                displayTripValues();
            } else if (fuelStatsPanel.modeIs(FuelStatsPanel.FuelDisplayMode.FuelTotal)) {
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
        textBig.setText(String.format("%.2f", tripComputer.tripStats.getLitersPer100km()));
        textSmaller.setText(String.format("%.2f", tripComputer.tripStats.getLiters()));
    }

    @SuppressLint("DefaultLocale")
    public void displayTotalValues() {
        textBig.setText(String.format("%.2f", tripComputer.totalStats.getLitersPer100km()));
        textSmaller.setText(String.format("%.2f", tripComputer.totalStats.getLiters()));
    }

    @SuppressLint("DefaultLocale")
    public void displayInstValues() {
        textBig.setText(String.format("%.2f", tripComputer.instStats.getLphAvg()));
        textSmaller.setText(String.format("%.2f", tripComputer.tripStats.getLiters()));
    }

    @SuppressLint("DefaultLocale")
    public void displayAfrValues() {
        textBig.setText(String.format("%.2f", tripComputer.mSpartanStudio.lastSensorAfr));
        textSmaller.setText(String.format("%.2f", tripComputer.afrHistory.getAvgDeviation(
                tripComputer.mSpartanStudio.targetAfr)));
    }

    @Override
    public boolean visibleInPip() {
        return true;
    }

    @Override
    public void enterPip() {
        super.enterPip();

        updateDisplay();
    }

    @Override
    public void exitPip() {
        super.exitPip();

        updateDisplay();
    }
}
