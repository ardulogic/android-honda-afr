package com.hondaafr.Libs.UI.Scientific.Panels;

import com.github.mikephil.charting.charts.LineChart;
import com.hondaafr.Libs.Helpers.TimeChart;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class ChartPanel extends Panel {

    private TimeChart mChart;

    private long startTimestamp;

    public ChartPanel(MainActivity mainActivity, TripComputer tripComputer, UiView view) {
        super(mainActivity, tripComputer, view);

        startTimestamp = System.currentTimeMillis();

        mChart = new TimeChart(mainActivity, getContainerView());
        mChart.init();
        mChart.invalidate();
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
    public LineChart getContainerView() {
        return mainActivity.findViewById(getContainerId());
    }

    @Override
    public int getContainerId() {
        return R.id.chart;
    }

    @Override
    public String getListenerId() {
        return "chart_panel";
    }

    @Override
    public boolean visibleInPip() {
        return true;
    }

    @Override
    public void onAfrValue(Double afr) {
        onAfrUpdated(afr);
    }

    @Override
    public void onAfrTargetValue(double targetAfr) {
        onTargetAfrUpdated(targetAfr);
    }
}
