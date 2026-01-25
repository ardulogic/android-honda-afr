package com.hondaafr.Libs.UI.AdaptiveAfr.Panels;

import android.app.AlertDialog;
import android.graphics.Color;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputerListener;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.AdaptiveAfr.AdaptiveAfrState;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

import java.util.Locale;

public class AfrTablePanel extends Panel implements AfrComputerListener {

    private AfrComputer afrComputer;
    private TableLayout table;
    private TextView[][] cellViews;

    @Override
    public int getContainerId() {
        return R.id.scrollAdaptiveAfrTable;
    }

    @Override
    public String getListenerId() {
        return "adaptive_afr_table_panel";
    }

    public AfrTablePanel(MainActivity mainActivity, TripComputer tripComputer, UiView parent, AfrComputer afrComputer) {
        super(mainActivity, tripComputer, parent);
        this.afrComputer = afrComputer;
        
        table = rootView.findViewById(R.id.tableAdaptiveAfr);
        
        if (afrComputer != null) {
            afrComputer.addListener(getListenerId(), this);
        }
    }

    @Override
    public void onAdaptiveAfrDataUpdated(AdaptiveAfrState state) {
        if (state != null) {
            Integer rpmValue = state.getLastRpm();
            Integer mapValue = state.getLastMap();
            if (rpmValue != null && mapValue != null) {
                updateActiveCellFromLiveData(rpmValue, mapValue);
            }
        }
    }

    @Override
    public void onTableDataChanged(AdaptiveAfrState state) {
        buildTable();
    }

    public void buildTable() {
        if (afrComputer == null) {
            return;
        }
        AdaptiveAfrState currentState = afrComputer.getState();
        if (table == null || currentState == null || currentState.getTargetTable() == null) {
            return;
        }
        
        table.removeAllViews();
        cellViews = new TextView[AdaptiveAfrState.RPM_BINS.length][AdaptiveAfrState.MAP_BINS.length];

        TableRow header = new TableRow(mainActivity);
        header.addView(makeHeaderCell("MAP\nRPM", true));
        for (double map : AdaptiveAfrState.MAP_BINS) {
            header.addView(makeHeaderCell(String.format("%.0f", map)));
        }
        table.addView(header);

        for (int r = 0; r < AdaptiveAfrState.RPM_BINS.length; r++) {
            double rpm = AdaptiveAfrState.RPM_BINS[r];
            TableRow row = new TableRow(mainActivity);
            row.addView(makeHeaderCell(String.format("%.0f", rpm)));
            for (int m = 0; m < AdaptiveAfrState.MAP_BINS.length; m++) {
                double afr = currentState.getTargetTable()[r][m];
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
        return makeHeaderCell(text, false);
    }

    private TextView makeHeaderCell(String text, boolean isSmall) {
        TextView view = new TextView(mainActivity);
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(mainActivity, R.color.app_text_primary));
        view.setPadding(20, 16, 20, 16);
        view.setGravity(android.view.Gravity.CENTER);
        view.setSingleLine(false);
        if (isSmall) {
            view.setTextSize(10);
        }
        return view;
    }

    private TextView makeValueCell(String text) {
        TextView view = new TextView(mainActivity);
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(mainActivity, R.color.app_text_secondary));
        view.setPadding(20, 16, 20, 16);
        view.setGravity(android.view.Gravity.CENTER);
        return view;
    }

    private void showEditDialog(int r, int m) {
        if (afrComputer == null) {
            return;
        }
        AdaptiveAfrState currentState = afrComputer.getState();
        if (currentState == null || currentState.getTargetTable() == null) {
            return;
        }
        EditText input = new EditText(mainActivity);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.getDefault(), "%.1f", currentState.getTargetTable()[r][m]));

        new AlertDialog.Builder(mainActivity)
                .setTitle("Target AFR")
                .setMessage("RPM " + (int) AdaptiveAfrState.RPM_BINS[r] + ", MAP " + (int) AdaptiveAfrState.MAP_BINS[m])
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        double value = Double.parseDouble(input.getText().toString().trim());
                        AdaptiveAfrPresetTogglePanel presetPanel = parent.getPanel(AdaptiveAfrPresetTogglePanel.class);
                        if (presetPanel != null) {
                            presetPanel.saveCell(r, m, value);
                        }
                        buildTable();
                    } catch (NumberFormatException ignored) {
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public double lookupTargetAfr(double rpm, double map) {
        if (afrComputer == null) {
            return Double.NaN;
        }
        AdaptiveAfrState currentState = afrComputer.getState();
        if (currentState == null || currentState.getTargetTable() == null) {
            return Double.NaN;
        }
        int rIndex = nearestIndex(AdaptiveAfrState.RPM_BINS, rpm);
        int mIndex = nearestIndex(AdaptiveAfrState.MAP_BINS, map);
        return currentState.getTargetTable()[rIndex][mIndex];
    }

    public void updateActiveCellFromLiveData(double rpm, double map) {
        if (afrComputer == null) {
            return;
        }
        AdaptiveAfrState currentState = afrComputer.getState();
        if (currentState == null || Double.isNaN(rpm) || Double.isNaN(map)) {
            return;
        }
        int rIndex = nearestIndex(AdaptiveAfrState.RPM_BINS, rpm);
        int mIndex = nearestIndex(AdaptiveAfrState.MAP_BINS, map);
        if (rIndex != currentState.getActiveRow() || mIndex != currentState.getActiveCol()) {
            currentState.setActiveRow(rIndex);
            currentState.setActiveCol(mIndex);
            updateActiveCellHighlight();
        }
    }

    @Override
    public void detachTripComputerListener() {
        super.detachTripComputerListener();
        if (afrComputer != null) {
            afrComputer.removeListener(getListenerId());
        }
    }

    private void updateActiveCellHighlight() {
        if (afrComputer == null) {
            return;
        }
        AdaptiveAfrState currentState = afrComputer.getState();
        if (cellViews == null || currentState == null) {
            return;
        }
        for (int r = 0; r < cellViews.length; r++) {
            for (int m = 0; m < cellViews[r].length; m++) {
                TextView cell = cellViews[r][m];
                if (cell == null) {
                    continue;
                }
                if (r == currentState.getActiveRow() && m == currentState.getActiveCol()) {
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
}

