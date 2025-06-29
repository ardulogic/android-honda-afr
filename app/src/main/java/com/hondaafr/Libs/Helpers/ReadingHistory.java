package com.hondaafr.Libs.Helpers;

public class ReadingHistory {
    private final AverageList shortAfrAvgHistory = new AverageList(80);
    private final AverageList shortAfrMinHistory = new AverageList(80);
    private final AverageList shortAfrMaxHistory = new AverageList(80);

    public void clear() {
        shortAfrAvgHistory.clear();
        shortAfrMinHistory.clear();
        shortAfrMaxHistory.clear();
    }

    public void clearMax() {
        shortAfrMaxHistory.clear();
    }

    public void clearMin() {
        shortAfrMinHistory.clear();
    }

    public void add(Double value) {
        shortAfrAvgHistory.add(value);
        shortAfrMinHistory.add(value);
        shortAfrMaxHistory.add(value);
    }

    public Double getAvg() {
        return shortAfrAvgHistory.getAvg();
    }

    public double getAvgDeviation(double target) {
        if (!shortAfrAvgHistory.isEmpty()) {
            return shortAfrAvgHistory.getAverageDistanceFromTarget(target);
        } else {
            return 0;
        }
    }

    public double getMinValue() {
        return shortAfrMinHistory.getMinValue();
    }

    public double getMaxValue() {
        return shortAfrMaxHistory.getMaxValue();
    }

}
