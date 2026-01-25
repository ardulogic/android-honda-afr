package com.hondaafr.Libs.UI.AdaptiveAfr.Panels;

import android.graphics.Color;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputerListener;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.AdaptiveAfr.AdaptiveAfrState;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

import java.util.ArrayList;

public class AfrRpmChartPanel extends Panel implements AfrComputerListener {

    private AfrComputer afrComputer;
    private LineChart rpmChart;
    private LineDataSet rpmDataSet;

    @Override
    public int getContainerId() {
        return R.id.layoutAdaptiveAfr;
    }

    @Override
    public String getListenerId() {
        return "adaptive_afr_rpm_chart_panel";
    }

    public AfrRpmChartPanel(MainActivity mainActivity, TripComputer tripComputer, UiView parent, AfrComputer afrComputer) {
        super(mainActivity, tripComputer, parent);
        this.afrComputer = afrComputer;
        
        rpmChart = rootView.findViewById(R.id.adaptiveRpmChart);
        initChart();
        
        if (afrComputer != null) {
            afrComputer.addListener(getListenerId(), this);
        }
    }

    private void initChart() {
        rpmDataSet = createDataSet("RPM", Color.parseColor("#4CAF50"));
        configureChart(rpmChart, new LineData(rpmDataSet));
    }

    private LineDataSet createDataSet(String label, int color) {
        LineDataSet set = new LineDataSet(new ArrayList<>(), label);
        set.setColor(color);
        set.setDrawCircles(false);
        set.setLineWidth(2f);
        set.setDrawValues(false);
        return set;
    }

    private void configureChart(LineChart chart, LineData data) {
        chart.setData(data);
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("Waiting for data...");
        chart.setTouchEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getAxisRight().setEnabled(false);
        int textColor = ContextCompat.getColor(mainActivity, R.color.app_text_primary);
        int backgroundColor = ContextCompat.getColor(mainActivity, R.color.app_background);
        chart.getAxisLeft().setTextColor(textColor);
        chart.getXAxis().setTextColor(textColor);
        chart.setBackgroundColor(backgroundColor);
        Legend legend = chart.getLegend();
        legend.setTextColor(textColor);
    }

    @Override
    public void setVisibility(boolean visible) {
        // Don't hide the container - manage chart visibility individually
    }

    @Override
    public View[] getViewsHiddenInPip() {
        return new View[]{rpmChart};
    }

    @Override
    public void onAdaptiveAfrDataUpdated(AdaptiveAfrState state) {
        if (state == null || state.getLastRpm() == null) {
            return;
        }
        float time = (System.currentTimeMillis() - state.getStartTimestamp()) / 1000f;
        rpmDataSet.addEntry(new Entry(time, state.getLastRpm()));

        rpmChart.getData().notifyDataChanged();
        rpmChart.notifyDataSetChanged();
        rpmChart.setVisibleXRangeMaximum(30f);
        rpmChart.moveViewToX(time);
        rpmChart.invalidate();
    }

    @Override
    public void detachTripComputerListener() {
        super.detachTripComputerListener();
        if (afrComputer != null) {
            afrComputer.removeListener(getListenerId());
        }
    }
}

