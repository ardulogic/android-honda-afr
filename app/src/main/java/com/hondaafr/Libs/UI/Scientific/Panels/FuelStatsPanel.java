package com.hondaafr.Libs.UI.Scientific.Panels;

import android.widget.TextView;

import com.hondaafr.Libs.Helpers.TripComputer.InstantStats;
import com.hondaafr.Libs.Helpers.TripComputer.TotalStats;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.R;
import com.hondaafr.MainActivity;

public class FuelStatsPanel extends Panel {

    private final TextView textStatsInfo, textStatsBig, textStatsMedium, textStatsSmall;
    private final TextView textStatsMediumLabel, textStatsSmallLabel;

    public boolean isVisible() {
        return mode != FuelDisplayMode.FuelOff;
    }

    public boolean modeIs(FuelDisplayMode fuelDisplayMode) {
        return mode == fuelDisplayMode;
    }

    public enum FuelDisplayMode {
        FuelOff, FuelTrip, FuelInst, FuelTotal
    }

    private FuelDisplayMode mode = FuelDisplayMode.FuelOff;

    @Override
    public int getContainerId() {
        return R.id.layoutFuelStats;
    }

    @Override
    public String getListenerId() {
        return "fuel_stats_panel";
    }

    public FuelStatsPanel(MainActivity mainActivity, TripComputer tripComputer, UiView view) {
        super(mainActivity, tripComputer, view);

        textStatsInfo = mainActivity.findViewById(R.id.textStatsTitle);
        textStatsBig = mainActivity.findViewById(R.id.textStatsBig);
        textStatsMedium = mainActivity.findViewById(R.id.textStatsMedium);
        textStatsSmall = mainActivity.findViewById(R.id.textStatsSmall);
        textStatsMediumLabel = mainActivity.findViewById(R.id.textStatsMediumLabel);
        textStatsSmallLabel = mainActivity.findViewById(R.id.textStatsSmallLabel);

        textStatsBig.setOnLongClickListener(v -> {
            if (mode == FuelDisplayMode.FuelTotal) {
                tripComputer.totalStats.reset(mainActivity);
            } else {
                tripComputer.tripStats.reset(mainActivity);
            }

            updateDisplay();
            return true;
        });
    }

    @Override
    public void onCalculationsUpdated() {
        updateDisplay();
    }

    public void toggleMode() {
        FuelDisplayMode[] values = FuelDisplayMode.values();
        int nextOrdinal = (mode.ordinal() + 1) % values.length;
        mode = values[nextOrdinal];
        updateDisplay();

        ((CornerStatsPanel) parent.getPanel(CornerStatsPanel.class)).updateDisplay();
    }

    public void updateDisplay() {
        if (!isInPip()) {
            switch (mode) {
                case FuelTrip: {
                    setVisibility(true);
                    displayFuelConsumption(tripComputer.tripStats);
                    break;
                }
                case FuelTotal: {
                    setVisibility(true);
                    displayFuelConsumption(tripComputer.totalStats);
                    break;
                }

                case FuelInst: {
                    setVisibility(true);
                    displayFuelConsumption(tripComputer.instStats);
                    break;
                }

                case FuelOff:
                default:
                    setVisibility(false);
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
