package com.hondaafr.Libs.UI.Scientific;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Helpers.TimeChart;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class ChartPanel extends Panel {

    private final LineChart panel;
    private final TripComputer mTripComputer;
    private final MainActivity mainActivity;
    private final Context context;
    private TimeChart mChart;

    private long startTimestamp;

    public ChartPanel(MainActivity mainActivity, TripComputer mTripComputer) {
        this.mTripComputer = mTripComputer;
        this.mainActivity = mainActivity;
        this.context = mainActivity;

        panel = mainActivity.findViewById(R.id.graph);
        startTimestamp = System.currentTimeMillis();

        mChart = new TimeChart(mainActivity, panel);
        mChart.init();
        mChart.invalidate();
        mTripComputer.addListener("chart", new TripComputerListener() {
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
                onTargetAfrUpdated(targetAfr);
            }

            @Override
            public void onAfrValue(Double afr) {
                onAfrUpdated(afr);
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

    public void onTargetAfrUpdated(double targetAfr) {
        mChart.setLimitLines(null, null, (float) targetAfr);
        mChart.invalidate();
    }

    public void onAfrUpdated(Double afr) {
        mainActivity.runOnUiThread(() -> {
            float time = System.currentTimeMillis() - startTimestamp; // Cant use full timestamp, too big
            mChart.addToData(time, afr.floatValue(), true);
            mChart.invalidate();
        });
    }

    public void clear() {
        mChart.clearData();
        startTimestamp = System.currentTimeMillis();
        mChart.invalidate();
    }

    @Override
    public View getContainerView() {
        return panel;
    }

    @Override
    public boolean visibleInPip() {
        return true;
    }
}
