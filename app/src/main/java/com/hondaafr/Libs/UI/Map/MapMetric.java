package com.hondaafr.Libs.UI.Map;

import com.hondaafr.Libs.Helpers.TripComputer.TripFuelTrackStore;

/**
 * Track dimension shown on the map (metric value per point).
 * Used for legend label, value extraction, and color scale.
 * Each metric has configurable thresholds for four zones: cool, green, orange, red.
 * <p>
 * Constructor: (label, ...five values..., inverted). Same threshold order (cool→green→orange→red).
 * Non-inverted: (min, coolToGreen, greenToOrange, orangeToRed, max) — lowest to highest.
 * Inverted: (max, coolToGreen, greenToOrange, orangeToRed, min) — highest to lowest; only the value↔color mapping is inverted (high value = cool).
 */
public enum MapMetric {
    L_PER_HOUR("L/h", 0.0, 2.0, 7.0, 10.0, 30.0, false),
    L_PER_100KM("L/100km", 0.0, 3.0, 7.0, 12.0, 30.0, false),
    /** AFR: 17 cool, 15 cool→green, 14 green→orange, 10 orange→red, 8 red. */
    AFR("Afr", 17.0, 15.2, 14.7, 13.0, 8.0, true),
    RPM("RPM", 800.0, 1500.0, 3500.0, 5000.0, 6000.0, false),
    MAP("MAP", 20.0, 40.0, 60.0, 80.0, 100.0, false),
    KMH("Km/h", 0.0, 50.0, 90.0, 140.0, 200.0, false);

    private final String label;
    private final double minForColor;
    private final double maxForColor;
    private final boolean invertedForColor;
    private final double coolToGreen;
    private final double greenToOrange;
    private final double orangeToRed;

    MapMetric(String label, double a, double b, double c, double d, double e, boolean invertedForColor) {
        this.label = label;
        this.invertedForColor = invertedForColor;
        if (invertedForColor) {
            this.maxForColor = a;
            this.coolToGreen = b;
            this.greenToOrange = c;
            this.orangeToRed = d;
            this.minForColor = e;
        } else {
            this.minForColor = a;
            this.coolToGreen = b;
            this.greenToOrange = c;
            this.orangeToRed = d;
            this.maxForColor = e;
        }
    }

    public String label() {
        return label;
    }

    public double minForColor() {
        return minForColor;
    }

    public double maxForColor() {
        return maxForColor;
    }

    public boolean invertedForColor() {
        return invertedForColor;
    }

    /** Threshold: value <= this → cool (or red when inverted). */
    public double coolToGreen() {
        return coolToGreen;
    }

    /** Threshold: value <= this → green (or orange when inverted). */
    public double greenToOrange() {
        return greenToOrange;
    }

    /** Threshold: value <= this → orange (or green when inverted). Value above → red (or cool when inverted). */
    public double orangeToRed() {
        return orangeToRed;
    }

    /** Next metric when cycling (for "Next" button). */
    public MapMetric next() {
        MapMetric[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /** Extracts the chosen metric from a track point, or null if invalid/NaN. */
    public Double extract(TripFuelTrackStore.TrackPoint p) {
        switch (this) {
            case L_PER_HOUR:
                return Double.isNaN(p.litersPerHour) ? null : p.litersPerHour;
            case L_PER_100KM:
                return Double.isNaN(p.litersPer100km) ? null : p.litersPer100km;
            case AFR:
                return Double.isNaN(p.afr) ? null : p.afr;
            case RPM:
                return Double.isNaN(p.rpm) ? null : p.rpm;
            case MAP:
                return Double.isNaN(p.mapKpa) ? null : p.mapKpa;
            case KMH:
                return Double.isNaN(p.speedKmh) ? null : p.speedKmh;
            default:
                return null;
        }
    }
}
