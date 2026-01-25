package com.hondaafr.Libs.UI.AdaptiveAfr.Panels;

import android.graphics.Color;

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

public class AfrAfrChartPanel extends Panel implements AfrComputerListener {

    private AfrComputer afrComputer;
    private LineChart afrChart;
    private LineDataSet afrDataSet;
    private LineDataSet targetDataSet;

    @Override
    public int getContainerId() {
        // This panel manages a chart that is a direct child of the root layout
        return R.id.layoutAdaptiveAfr;
    }

    @Override
    public String getListenerId() {
        return "adaptive_afr_afr_chart_panel";
    }

    public AfrAfrChartPanel(MainActivity mainActivity, TripComputer tripComputer, UiView parent, AfrComputer afrComputer) {
        super(mainActivity, tripComputer, parent);
        this.afrComputer = afrComputer;
        
        afrChart = rootView.findViewById(R.id.adaptiveAfrChart);
        initChart();

        if (afrComputer != null) {
            afrComputer.addListener(getListenerId(), this);
        }
    }

    private void initChart() {
        afrDataSet = createDataSet("AFR", Color.parseColor("#FFC107"));
        targetDataSet = createDataSet("Target AFR", Color.parseColor("#FF5722"));
        
        LineData afrData = new LineData();
        afrData.addDataSet(afrDataSet);
        afrData.addDataSet(targetDataSet);
        configureChart(afrChart, afrData);
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
    public void onAdaptiveAfrDataUpdated(AdaptiveAfrState state) {
        if (state == null) {
            return;
        }
        float time = (System.currentTimeMillis() - state.getStartTimestamp()) / 1000f;
        
        // Add AFR value if available
        if (!Double.isNaN(state.getLastAfr()) && state.getLastAfr() > 0) {
            afrDataSet.addEntry(new Entry(time, (float) state.getLastAfr()));
        }

        // Add target AFR from state (red line)
        if (!Double.isNaN(state.getLastTargetAfr())) {
            targetDataSet.addEntry(new Entry(time, (float) state.getLastTargetAfr()));
        }

        afrChart.getData().notifyDataChanged();
        afrChart.notifyDataSetChanged();
        afrChart.setVisibleXRangeMaximum(30f);
        afrChart.moveViewToX(time);
        afrChart.invalidate();
    }

    @Override
    public void detachTripComputerListener() {
        super.detachTripComputerListener();
        if (afrComputer != null) {
            afrComputer.removeListener(getListenerId());
        }
    }
}

