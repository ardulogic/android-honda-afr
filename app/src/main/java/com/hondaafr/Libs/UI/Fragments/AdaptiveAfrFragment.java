package com.hondaafr.Libs.UI.Fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.Libs.UI.Fragments.PipAware;
import com.hondaafr.MainActivity;
import com.hondaafr.R;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdaptiveAfrFragment extends Fragment implements TripComputerListener, PipAware {
    private static final String LISTENER_ID = "adaptive_afr_fragment";
    private static final double[] RPM_BINS = {1000, 2000, 3000, 4000, 5000, 6000};
    private static final double[] MAP_BINS = {30, 50, 70, 90, 100};
    private static final String PREFS_NAME = "AdaptiveAfrPrefs";
    private static final String PREF_ENABLED = "adaptive_enabled";
    private static final String PREF_CELL_PREFIX = "cell_";
    private static final String PREF_PRESET = "adaptive_preset";

    private LineChart rpmChart;
    private LineChart mapChart;
    private LineChart afrChart;
    private LineDataSet rpmDataSet;
    private LineDataSet mapDataSet;
    private LineDataSet afrDataSet;
    private LineDataSet targetDataSet;
    private TextView textRpm;
    private TextView textMap;
    private TextView textAfr;
    private TextView textTarget;
    private TableLayout table;
    private View tableContainer;
    private View infoContainer;
    private SwitchMaterial switchAdaptive;
    private MaterialButtonToggleGroup togglePresets;
    private TripComputer tripComputer;

    private double lastRpm = Double.NaN;
    private double lastMap = Double.NaN;
    private double lastAfr = Double.NaN;
    private long startTimestamp;
    private boolean adaptiveEnabled = true;
    private double[][] targetTable;
    private TextView[][] cellViews;
    private int activeRow = -1;
    private int activeCol = -1;
    private String activePreset = "eco";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_adaptive_afr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rpmChart = view.findViewById(R.id.adaptiveRpmChart);
        mapChart = view.findViewById(R.id.adaptiveMapChart);
        afrChart = view.findViewById(R.id.adaptiveAfrChart);
        textRpm = view.findViewById(R.id.textAdaptiveRpm);
        textMap = view.findViewById(R.id.textAdaptiveMap);
        textAfr = view.findViewById(R.id.textAdaptiveAfr);
        textTarget = view.findViewById(R.id.textAdaptiveTarget);
        table = view.findViewById(R.id.tableAdaptiveAfr);
        tableContainer = view.findViewById(R.id.scrollAdaptiveAfrTable);
        infoContainer = view.findViewById(R.id.layoutAdaptiveAfrInfo);
        switchAdaptive = view.findViewById(R.id.switchAdaptiveAfr);
        togglePresets = view.findViewById(R.id.toggleAdaptivePresets);

        tripComputer = ((MainActivity) requireActivity()).getTripComputer();
        initChart();
        loadPrefs();
        buildTable();
        startTimestamp = System.currentTimeMillis();

        switchAdaptive.setChecked(adaptiveEnabled);
        switchAdaptive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adaptiveEnabled = isChecked;
            saveEnabled();
        });

        togglePresets.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.buttonAdaptiveSport) {
                applyPreset("sport");
            } else if (checkedId == R.id.buttonAdaptiveEco) {
                applyPreset("eco");
            } else if (checkedId == R.id.buttonAdaptiveEcoPlus) {
                applyPreset("eco_plus");
            }
        });
        applyPresetSelection();
    }

    @Override
    public void onResume() {
        super.onResume();
        tripComputer.addListener(LISTENER_ID, this);
    }

    @Override
    public void onPause() {
        tripComputer.removeListener(LISTENER_ID);
        super.onPause();
    }

    private void initChart() {
        rpmDataSet = createDataSet("RPM", Color.parseColor("#4CAF50"));
        mapDataSet = createDataSet("MAP", Color.parseColor("#03A9F4"));
        afrDataSet = createDataSet("AFR", Color.parseColor("#FFC107"));
        targetDataSet = createDataSet("Target AFR", Color.parseColor("#FF5722"));

        configureChart(rpmChart, new LineData(rpmDataSet));
        configureChart(mapChart, new LineData(mapDataSet));
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
        int textColor = ContextCompat.getColor(requireContext(), R.color.app_text_primary);
        int backgroundColor = ContextCompat.getColor(requireContext(), R.color.app_background);
        chart.getAxisLeft().setTextColor(textColor);
        chart.getXAxis().setTextColor(textColor);
        chart.setBackgroundColor(backgroundColor);
        Legend legend = chart.getLegend();
        legend.setTextColor(textColor);
    }

    private void buildTable() {
        table.removeAllViews();
        cellViews = new TextView[RPM_BINS.length][MAP_BINS.length];

        TableRow header = new TableRow(requireContext());
        header.addView(makeHeaderCell("RPM\\MAP"));
        for (double map : MAP_BINS) {
            header.addView(makeHeaderCell(String.format("%.0f", map)));
        }
        table.addView(header);

        for (int r = 0; r < RPM_BINS.length; r++) {
            double rpm = RPM_BINS[r];
            TableRow row = new TableRow(requireContext());
            row.addView(makeHeaderCell(String.format("%.0f", rpm)));
            for (int m = 0; m < MAP_BINS.length; m++) {
                double afr = targetTable[r][m];
                TextView cell = makeValueCell(String.format(Locale.getDefault(), "%.1f", afr));
                int finalR = r;
                int finalM = m;
                cell.setOnClickListener(v -> showEditDialog(finalR, finalM));
                cellViews[r][m] = cell;
                row.addView(cell);
            }
            table.addView(row);
        }
        updateActiveCellHighlight();
    }

    private TextView makeHeaderCell(String text) {
        TextView view = new TextView(requireContext());
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_text_primary));
        view.setPadding(12, 8, 12, 8);
        return view;
    }

    private TextView makeValueCell(String text) {
        TextView view = new TextView(requireContext());
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_text_secondary));
        view.setPadding(12, 8, 12, 8);
        return view;
    }

    private double computeTargetAfr(double rpm, double map) {
        double rpmFactor = rpm / RPM_BINS[RPM_BINS.length - 1];
        double mapFactor = map / MAP_BINS[MAP_BINS.length - 1];
        double target = 14.7 - (mapFactor * 2.0) - (rpmFactor * 1.0);
        return Math.max(10.5, Math.min(15.5, target));
    }

    private void appendData() {
        if (Double.isNaN(lastRpm) || Double.isNaN(lastMap) || Double.isNaN(lastAfr)) {
            return;
        }
        float time = (System.currentTimeMillis() - startTimestamp) / 1000f;
        rpmDataSet.addEntry(new Entry(time, (float) lastRpm));
        mapDataSet.addEntry(new Entry(time, (float) lastMap));
        afrDataSet.addEntry(new Entry(time, (float) lastAfr));

        rpmChart.getData().notifyDataChanged();
        rpmChart.notifyDataSetChanged();
        rpmChart.setVisibleXRangeMaximum(30f);
        rpmChart.moveViewToX(time);
        rpmChart.invalidate();

        mapChart.getData().notifyDataChanged();
        mapChart.notifyDataSetChanged();
        mapChart.setVisibleXRangeMaximum(30f);
        mapChart.moveViewToX(time);
        mapChart.invalidate();

        afrChart.getData().notifyDataChanged();
        afrChart.notifyDataSetChanged();
        afrChart.setVisibleXRangeMaximum(30f);
        afrChart.moveViewToX(time);
        afrChart.invalidate();

        double targetAfr = adaptiveEnabled ? lookupTargetAfr(lastRpm, lastMap) : Double.NaN;
        textRpm.setText(String.format("RPM: %.0f", lastRpm));
        textMap.setText(String.format("MAP: %.0f", lastMap));
        textAfr.setText(String.format("AFR: %.2f", lastAfr));
        textTarget.setText(adaptiveEnabled
                ? String.format("Target: %.2f", targetAfr)
                : "Target: OFF");

        if (adaptiveEnabled) {
            targetDataSet.addEntry(new Entry(time, (float) targetAfr));
            afrChart.getData().notifyDataChanged();
            afrChart.notifyDataSetChanged();
            afrChart.invalidate();
            updateActiveCellFromLiveData(lastRpm, lastMap);
        }
    }

    @Override
    public void onObdValue(ObdReading reading) {
        String name = reading.getMachineName();
        if ("rpm".equals(name)) {
            Object val = reading.getValue();
            if (val instanceof Integer) {
                lastRpm = (Integer) val;
            } else if (val instanceof Double) {
                lastRpm = (Double) val;
            }
        } else if ("map".equals(name)) {
            Object val = reading.getValue();
            if (val instanceof Integer) {
                lastMap = (Integer) val;
            } else if (val instanceof Double) {
                lastMap = (Double) val;
            }
        }
        appendData();
    }

    @Override
    public void onAfrValue(Double afr) {
        if (afr != null && afr > 0) {
            lastAfr = afr;
            appendData();
        }
    }

    @Override
    public void onEnterPip() {
        if (tableContainer != null) {
            tableContainer.setVisibility(View.GONE);
        }
        if (infoContainer != null) {
            infoContainer.setVisibility(View.GONE);
        }
        if (switchAdaptive != null) {
            switchAdaptive.setVisibility(View.GONE);
        }
        if (togglePresets != null) {
            togglePresets.setVisibility(View.GONE);
        }
    }

    @Override
    public void onExitPip() {
        if (tableContainer != null) {
            tableContainer.setVisibility(View.VISIBLE);
        }
        if (infoContainer != null) {
            infoContainer.setVisibility(View.VISIBLE);
        }
        if (switchAdaptive != null) {
            switchAdaptive.setVisibility(View.VISIBLE);
        }
        if (togglePresets != null) {
            togglePresets.setVisibility(View.VISIBLE);
        }
    }

    @Override public void onGpsUpdate(Double speed, double distanceIncrement) {}
    @Override public void onGpsPulse(com.hondaafr.Libs.Devices.Phone.PhoneGps gps) {}
    @Override public void onAfrPulse(boolean isActive) {}
    @Override public void onAfrTargetValue(double targetAfr) {}
    @Override public void onObdPulse(boolean isActive) {}
    @Override public void onObdActivePidsChanged() {}
    @Override public void onCalculationsUpdated() {}
    @Override public void onNightModeUpdated(boolean isNight) {}

    private void loadPrefs() {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        adaptiveEnabled = prefs.getBoolean(PREF_ENABLED, true);
        activePreset = prefs.getString(PREF_PRESET, "eco");
        loadTableForPreset(activePreset);
    }

    private void saveEnabled() {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_ENABLED, adaptiveEnabled).apply();
    }

    private void saveCell(int r, int m, double value) {
        targetTable[r][m] = value;
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        String key = presetCellKey(activePreset, r, m);
        prefs.edit().putFloat(key, (float) value).apply();
    }

    private void savePresetSelection() {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_PRESET, activePreset).apply();
    }

    private void showEditDialog(int r, int m) {
        EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.getDefault(), "%.1f", targetTable[r][m]));

        new AlertDialog.Builder(requireContext())
                .setTitle("Target AFR")
                .setMessage("RPM " + (int) RPM_BINS[r] + ", MAP " + (int) MAP_BINS[m])
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        double value = Double.parseDouble(input.getText().toString().trim());
                        saveCell(r, m, value);
                        buildTable();
                    } catch (NumberFormatException ignored) {
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private double lookupTargetAfr(double rpm, double map) {
        int rIndex = nearestIndex(RPM_BINS, rpm);
        int mIndex = nearestIndex(MAP_BINS, map);
        return targetTable[rIndex][mIndex];
    }

    private void updateActiveCellFromLiveData(double rpm, double map) {
        int rIndex = nearestIndex(RPM_BINS, rpm);
        int mIndex = nearestIndex(MAP_BINS, map);
        if (rIndex != activeRow || mIndex != activeCol) {
            activeRow = rIndex;
            activeCol = mIndex;
            updateActiveCellHighlight();
        }
    }

    private void updateActiveCellHighlight() {
        if (cellViews == null) {
            return;
        }
        for (int r = 0; r < cellViews.length; r++) {
            for (int m = 0; m < cellViews[r].length; m++) {
                TextView cell = cellViews[r][m];
                if (cell == null) {
                    continue;
                }
                if (r == activeRow && m == activeCol) {
                    cell.setBackgroundColor(Color.parseColor("#2E7D32"));
                } else {
                    cell.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        }
    }

    private int nearestIndex(double[] bins, double value) {
        int bestIndex = 0;
        double bestDelta = Double.MAX_VALUE;
        for (int i = 0; i < bins.length; i++) {
            double delta = Math.abs(bins[i] - value);
            if (delta < bestDelta) {
                bestDelta = delta;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private void applyPresetSelection() {
        if (togglePresets == null) {
            return;
        }
        if ("sport".equals(activePreset)) {
            togglePresets.check(R.id.buttonAdaptiveSport);
        } else if ("eco_plus".equals(activePreset)) {
            togglePresets.check(R.id.buttonAdaptiveEcoPlus);
        } else {
            togglePresets.check(R.id.buttonAdaptiveEco);
        }
    }

    private void applyPreset(String preset) {
        activePreset = preset;
        loadTableForPreset(preset);
        savePresetSelection();
        buildTable();
    }

    private void loadTableForPreset(String preset) {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        targetTable = new double[RPM_BINS.length][MAP_BINS.length];
        for (int r = 0; r < RPM_BINS.length; r++) {
            for (int m = 0; m < MAP_BINS.length; m++) {
                String key = presetCellKey(preset, r, m);
                float stored = prefs.getFloat(key, Float.NaN);
                if (Float.isNaN(stored)) {
                    targetTable[r][m] = defaultPresetValue(preset, MAP_BINS[m]);
                } else {
                    targetTable[r][m] = stored;
                }
            }
        }
    }

    private String presetCellKey(String preset, int r, int m) {
        return PREF_CELL_PREFIX + preset + "_" + r + "_" + m;
    }

    private double defaultPresetValue(String preset, double map) {
        if ("sport".equals(preset)) {
            return sportPreset(map);
        }
        if ("eco_plus".equals(preset)) {
            return ecoPlusPreset(map);
        }
        if ("eco".equals(preset)) {
            return ecoPreset(map);
        }
        return computeTargetAfr(0, map);
    }

    private double ecoPlusPreset(double map) {
        if (map <= 30.0) {
            return lerp(15.0, 15.5, map / 30.0);
        }
        if (map <= 50.0) {
            return lerp(15.5, 16.7, (map - 30.0) / 20.0);
        }
        return lerp(16.7, 14.7, Math.min(1.0, (map - 50.0) / 50.0));
    }

    private double ecoPreset(double map) {
        if (map <= 30.0) {
            return lerp(15.0, 15.0, map / 30.0);
        }
        if (map <= 50.0) {
            return lerp(15.0, 15.7, (map - 30.0) / 20.0);
        }
        return lerp(15.7, 14.7, Math.min(1.0, (map - 50.0) / 50.0));
    }

    private double sportPreset(double map) {
        if (map <= 30.0) {
            return lerp(13.6, 13.2, map / 30.0);
        }
        if (map <= 50.0) {
            return lerp(13.2, 12.8, (map - 30.0) / 20.0);
        }
        return lerp(12.8, 12.3, Math.min(1.0, (map - 50.0) / 50.0));
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}

