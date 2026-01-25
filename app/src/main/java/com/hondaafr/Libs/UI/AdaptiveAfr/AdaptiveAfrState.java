package com.hondaafr.Libs.UI.AdaptiveAfr;

public class AdaptiveAfrState {
    public static final double[] RPM_BINS = {1000, 2000, 3000, 4000, 5000, 6000};
    public static final double[] MAP_BINS = {30, 50, 70, 90, 100};
    public static final String PREFS_NAME = "AdaptiveAfrPrefs";
    public static final String PREF_ENABLED = "adaptive_enabled";
    public static final String PREF_CELL_PREFIX = "cell_";
    public static final String PREF_PRESET = "adaptive_preset";
    public static final double AFR_CHANGE_THRESHOLD = 0.1;

    private Integer lastRpm = null;
    private Integer lastMap = null;
    private double lastAfr = Double.NaN;
    private long startTimestamp;
    private boolean adaptiveEnabled = true;
    private double[][] targetTable;
    private int activeRow = -1;
    private int activeCol = -1;
    private String activePreset = "eco";
    private double lastSetTargetAfr = Double.NaN;
    private double lastTargetAfr = Double.NaN;

    public Integer getLastRpm() {
        return lastRpm;
    }

    public void setLastRpm(Integer lastRpm) {
        this.lastRpm = lastRpm;
    }

    public Integer getLastMap() {
        return lastMap;
    }

    public void setLastMap(Integer lastMap) {
        this.lastMap = lastMap;
    }

    public double getLastAfr() {
        return lastAfr;
    }

    public void setLastAfr(double lastAfr) {
        this.lastAfr = lastAfr;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public boolean isAdaptiveEnabled() {
        return adaptiveEnabled;
    }

    public void setAdaptiveEnabled(boolean adaptiveEnabled) {
        this.adaptiveEnabled = adaptiveEnabled;
    }

    public double[][] getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(double[][] targetTable) {
        this.targetTable = targetTable;
    }

    public int getActiveRow() {
        return activeRow;
    }

    public void setActiveRow(int activeRow) {
        this.activeRow = activeRow;
    }

    public int getActiveCol() {
        return activeCol;
    }

    public void setActiveCol(int activeCol) {
        this.activeCol = activeCol;
    }

    public String getActivePreset() {
        return activePreset;
    }

    public void setActivePreset(String activePreset) {
        this.activePreset = activePreset;
    }

    public double getLastSetTargetAfr() {
        return lastSetTargetAfr;
    }

    public void setLastSetTargetAfr(double lastSetTargetAfr) {
        this.lastSetTargetAfr = lastSetTargetAfr;
    }

    public double getLastTargetAfr() {
        return lastTargetAfr;
    }

    public void setLastTargetAfr(double lastTargetAfr) {
        this.lastTargetAfr = lastTargetAfr;
    }
}

