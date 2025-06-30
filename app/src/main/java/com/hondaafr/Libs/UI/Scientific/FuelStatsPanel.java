package com.hondaafr.Libs.UI.Scientific;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Helpers.TripComputer.InstantStats;
import com.hondaafr.Libs.Helpers.TripComputer.TotalStats;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.Libs.UI.ScientificView;
import com.hondaafr.R;
import com.hondaafr.MainActivity;

public class FuelStatsPanel extends Panel {

    public final LinearLayout panel;
    private final TextView textStatsInfo, textStatsBig, textStatsMedium, textStatsSmall;
    private final TextView textStatsMediumLabel, textStatsSmallLabel;
    private final TripComputer mTripComputer;
    private final ScientificView sc;

    public boolean isVisible() {
        return mode != FuelDisplayMode.FuelOff;
    }

    @Override
    public View getContainerView() {
        return panel;
    }

    public boolean modeIs(FuelDisplayMode fuelDisplayMode) {
        return mode == fuelDisplayMode;
    }

    public enum FuelDisplayMode {
        FuelOff, FuelTrip, FuelInst, FuelTotal
    }

    private FuelDisplayMode mode = FuelDisplayMode.FuelOff;

    public FuelStatsPanel(MainActivity mainActivity, ScientificView sc, TripComputer mTripComputer) {
        this.mTripComputer = mTripComputer;
        this.sc = sc;

        panel = mainActivity.findViewById(R.id.layoutStats);
        textStatsInfo = mainActivity.findViewById(R.id.textStatsTitle);
        textStatsBig = mainActivity.findViewById(R.id.textStatsBig);
        textStatsMedium = mainActivity.findViewById(R.id.textStatsMedium);
        textStatsSmall = mainActivity.findViewById(R.id.textStatsSmall);
        textStatsMediumLabel = mainActivity.findViewById(R.id.textStatsMediumLabel);
        textStatsSmallLabel = mainActivity.findViewById(R.id.textStatsSmallLabel);

        textStatsBig.setOnLongClickListener(v -> {
            if (mode == FuelDisplayMode.FuelTotal) {
                mTripComputer.totalStats.reset(mainActivity);
            } else {
                mTripComputer.tripStats.reset(mainActivity);
            }

            updateDisplayedValues();
            return true;
        });

        mTripComputer.addListener("fuel_stats_panel", new TripComputerListener() {
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
                updateDisplayedValues();
            }
        });
    }

    public void hide() {
        panel.setVisibility(View.GONE);
    }

    public void show() {
        panel.setVisibility(View.VISIBLE);
    }

    public void toggleMode() {
        FuelDisplayMode[] values = FuelDisplayMode.values();
        int nextOrdinal = (mode.ordinal() + 1) % values.length;
        mode = values[nextOrdinal];
        updateDisplayedValues();

        sc.cornerStatsPanel.displayValues();
    }

    public void updateDisplayedValues() {
        if (!isInPip()) {
            switch (mode) {
                case FuelTrip: {
                    show();
                    displayFuelConsumption(mTripComputer.tripStats);
                    break;
                }
                case FuelTotal: {
                    show();
                    displayFuelConsumption(mTripComputer.totalStats);
                    break;
                }

                case FuelInst: {
                    show();
                    displayFuelConsumption(mTripComputer.instStats);
                    break;
                }

                case FuelOff:
                default:
                    hide();
                    break;
            }
        }
    }


    public void displayFuelConsumption(TotalStats stats) {
        textStatsInfo.setText(stats.getName());
        textStatsBig.setText(String.format("%06.1f", stats.getDistanceKm()));

        textStatsMediumLabel.setText("TOTAL");
        textStatsMedium.setText(String.format("%.2f l", stats.getLiters()));

        textStatsSmallLabel.setText("100KM");
        textStatsSmall.setText(String.format("%.1f l", stats.getLitersPer100km()));
    }

    public void displayFuelConsumption(InstantStats stats) {
        textStatsInfo.setText(stats.getName());
        textStatsBig.setText(String.format("%.2f l/h", stats.getLphAvg()));

        textStatsMediumLabel.setText("100KM");
        textStatsMedium.setText(String.format("%.2f l", stats.getLp100km()));

        textStatsSmallLabel.setText("100KM");
        textStatsSmall.setText(String.format("%.1f l", stats.getLp100kmAvg()));
    }
}
