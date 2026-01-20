package com.hondaafr.Libs.Helpers;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.hondaafr.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class TimeChart {
    private final LineChart mChart;
    private final Typeface legendFont;
    private final Activity activity;
    private boolean legendIsInitialized = false;


//    private final String DATE_FORMAT = "dd MMM HH:mm";
    private final String DATE_FORMAT = "mm:ss.SSS";
    private final SimpleDateFormat mFormat;
    private Random random = new Random();
    public LineData data;

    public TimeChart(Activity activity, LineChart chart) {
        this.mChart = chart;
        this.activity = activity;
        this.mChart.setNoDataText("");

        mFormat = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        legendFont = ResourcesCompat.getFont(activity, R.font.opensans_bold);
    }

    public void setMaxZoom() {
        mChart.setVisibleXRangeMaximum(8000);
        mChart.moveViewToX(data.getXMax());
    }

    public void init() {
        // no description text
        mChart.getDescription().setEnabled(false);

        // enable touch gestures
        mChart.setTouchEnabled(true);

        mChart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setHighlightPerDragEnabled(true);

        // set an alternative background color (theme-aware)
        int backgroundColor = ResourcesCompat.getColor(activity.getResources(), R.color.chart_background, activity.getTheme());
        int noDataTextColor = ResourcesCompat.getColor(activity.getResources(), R.color.chart_no_data_text, activity.getTheme());
        mChart.setBackgroundColor(backgroundColor);
        mChart.setNoDataTextColor(noDataTextColor);
        mChart.setViewPortOffsets(0f, 0f, 0f, 0f);
    }

    public void setData(ArrayList<Entry> values) {
        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(values, "DataSet 1");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(ColorTemplate.getHoloBlue());
        set1.setValueTextColor(ColorTemplate.getHoloBlue());
        set1.setLineWidth(1.5f);
        set1.setDrawCircles(false);
        set1.setDrawValues(false);
        set1.setFillAlpha(65);
        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setDrawCircleHole(false);

        // create a data object with the data sets
        data = new LineData(set1);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(9f);

        // set data
        mChart.setData(data);


        if (!legendIsInitialized) {
            updateLegend();
        }
    }

    public void addToData(float timestamp, float value, boolean update) {
        if (data != null) {
            data.addEntry(new Entry(timestamp, value), 0);
            mChart.setData(data);
            updateLegend();
        } else {
            ArrayList<Entry> values = new ArrayList<>();
            values.add(new Entry(timestamp, value));
            setData(values);
        }

        if (update) {
            mChart.invalidate();
        }
    }

    public void updateLegend() {
        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();
        l.setEnabled(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        xAxis.setTypeface(legendFont);
        xAxis.setTextSize(10f);
        int textColor = ResourcesCompat.getColor(activity.getResources(), R.color.chart_text_color, activity.getTheme());
        xAxis.setTextColor(textColor);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(true);
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(1f); // one hour
        xAxis.setDrawLabels(false);

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return mFormat.format(new Date((long) value));
            }
        });

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        leftAxis.setTypeface(legendFont);
        int axisTextColor = ResourcesCompat.getColor(activity.getResources(), R.color.chart_text_color, activity.getTheme());
        leftAxis.setTextColor(axisTextColor);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setAxisMinimum(8);
        leftAxis.setAxisMaximum(22);
        leftAxis.setYOffset(-9f);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        legendIsInitialized = true;
        setMaxZoom();
    }

    public void setLimitLines(Float v1, Float v2, Float v3) {
        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines();

        if (v1 != null) {
            leftAxis.addLimitLine(createLimitLine(v1, v1.toString(), Color.RED));
        }

        if (v2 != null) {
            leftAxis.addLimitLine(createLimitLine(v2, v2.toString(), Color.GREEN));
        }

        if (v3 != null) {
            leftAxis.addLimitLine(createLimitLine(v3, v3.toString(), Color.rgb(220, 50, 50)));
        }
    }


    private LimitLine createLimitLine(float value, String label, int color) {
        LimitLine limitLine = new LimitLine(value, label);
        limitLine.setLineWidth(2f);
        limitLine.enableDashedLine(10f, 10f, 0f);
        limitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        limitLine.setTextSize(10f);
        limitLine.setTextColor(color);
        return limitLine;
    }

    public void setDemoData() {
        long now = 0;

        ArrayList<Entry> values = new ArrayList<>();

        int count = 1000;

        float x = now;
        for (float i = 0; i < count; i++, x += 100) {
            float y = getRandom(10, 50);
            values.add(new Entry(x, y)); // add one entry per hour
            Log.d("Demo value", String.valueOf(x) + " - " + String.valueOf(y));
        }

        setData(values);
    }

    private float getRandom(float range, float offset) {
        return random.nextFloat() * range + offset;
    }

    public void invalidate() {
        mChart.invalidate();
    }

    public void clearData() {
        if (data != null) {
            data.getDataSetByIndex(0).clear();
            mChart.notifyDataSetChanged();

            mChart.clear();
            data = null;

            legendIsInitialized = false;
        }
    }
}
