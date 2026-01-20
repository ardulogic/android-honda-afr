package com.hondaafr.Libs.UI.Scientific.Panels;

import android.widget.TextView;

import com.hondaafr.Libs.Helpers.TripComputer.InstantStats;
import com.hondaafr.Libs.Helpers.TripComputer.TotalStats;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Scientific.DraggableFuelStatsDialog;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.R;
import com.hondaafr.MainActivity;

public class FuelStatsPanel extends Panel {

    private final TextView textStatsInfo, textStatsBig, textStatsMedium, textStatsSmall;
    private final TextView textStatsMediumLabel, textStatsSmallLabel;
    private DraggableFuelStatsDialog dialog;

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

        // Keep references to old views for backward compatibility, but they're now hidden
        textStatsInfo = mainActivity.findViewById(R.id.textStatsTitle);
        textStatsBig = mainActivity.findViewById(R.id.textStatsBig);
        textStatsMedium = mainActivity.findViewById(R.id.textStatsMedium);
        textStatsSmall = mainActivity.findViewById(R.id.textStatsSmall);
        textStatsMediumLabel = mainActivity.findViewById(R.id.textStatsMediumLabel);
        textStatsSmallLabel = mainActivity.findViewById(R.id.textStatsSmallLabel);

        // Hide the old overlay
        setVisibility(false);

        // Create dialog (but don't show it yet)
        dialog = new DraggableFuelStatsDialog(mainActivity);
        
        // Set up dismiss listener to sync button state
        dialog.setCustomDismissListener(() -> {
            mode = FuelDisplayMode.FuelOff;
            // Update button state through TopButtonsPanel
            try {
                TopButtonsPanel topButtonsPanel = (TopButtonsPanel) parent.getPanel(TopButtonsPanel.class);
                if (topButtonsPanel != null) {
                    topButtonsPanel.updateFuelButtonState(false);
                }
            } catch (Exception e) {
                // Ignore if panel not found
            }
        });
        
        // Set up mode switch listener
        dialog.setModeSwitchListener(() -> {
            toggleMode();
        });
        
        // Set up long click listener on dialog's big text view
        dialog.setOnShowListener(dialogInterface -> {
            TextView dialogBigText = dialog.getTextStatsBig();
            if (dialogBigText != null) {
                dialogBigText.setOnLongClickListener(v -> {
                    if (mode == FuelDisplayMode.FuelTotal) {
                        tripComputer.totalStats.reset(mainActivity);
                    } else {
                        tripComputer.tripStats.reset(mainActivity);
                    }
                    updateDisplay();
                    return true;
                });
            }
            // Update mode button text when dialog is shown
            updateModeButtonText();
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
        
        // Skip FuelOff when cycling from dialog - go back to FuelTrip
        if (mode == FuelDisplayMode.FuelOff && dialog != null && dialog.isShowing()) {
            mode = FuelDisplayMode.FuelTrip;
        }

        updateDisplay();
        updateModeButtonText();

        ((CornerStatsPanel) parent.getPanel(CornerStatsPanel.class)).updateDisplay();
    }
    
    private void updateModeButtonText() {
        if (dialog != null && dialog.isShowing()) {
            String modeText = getModeDisplayText();
            dialog.updateModeButtonText(modeText);
        }
    }
    
    private String getModeDisplayText() {
        switch (mode) {
            case FuelTrip:
                return "Trip";
            case FuelInst:
                return "Instant";
            case FuelTotal:
                return "Total";
            case FuelOff:
            default:
                return "Off";
        }
    }

    public void updateDisplay() {
        if (!isInPip()) {
            switch (mode) {
                case FuelTrip: {
                    showDialog();
                    dialog.displayFuelConsumption(tripComputer.tripStats);
                    updateModeButtonText();
                    break;
                }
                case FuelTotal: {
                    showDialog();
                    dialog.displayFuelConsumption(tripComputer.totalStats);
                    updateModeButtonText();
                    break;
                }

                case FuelInst: {
                    showDialog();
                    dialog.displayFuelConsumption(tripComputer.instStats);
                    updateModeButtonText();
                    break;
                }

                case FuelOff:
                default:
                    hideDialog();
                    break;
            }
        }
    }

    private void showDialog() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
            // Update button state
            try {
                TopButtonsPanel topButtonsPanel = (TopButtonsPanel) parent.getPanel(TopButtonsPanel.class);
                if (topButtonsPanel != null) {
                    topButtonsPanel.updateFuelButtonState(true);
                }
            } catch (Exception e) {
                // Ignore if panel not found
            }
        }
    }

    private void hideDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        // Update button state
        try {
            TopButtonsPanel topButtonsPanel = (TopButtonsPanel) parent.getPanel(TopButtonsPanel.class);
            if (topButtonsPanel != null) {
                topButtonsPanel.updateFuelButtonState(false);
            }
        } catch (Exception e) {
            // Ignore if panel not found
        }
    }

    public void displayFuelConsumption(TotalStats stats) {
        // This method is kept for backward compatibility but now updates the dialog
        if (dialog != null && dialog.isShowing()) {
            dialog.displayFuelConsumption(stats);
        }
    }

    public void displayFuelConsumption(InstantStats stats) {
        // This method is kept for backward compatibility but now updates the dialog
        if (dialog != null && dialog.isShowing()) {
            dialog.displayFuelConsumption(stats);
        }
    }
}
