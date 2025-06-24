package com.hondaafr.Libs.Helpers;

import java.time.Duration;
import java.time.Instant;

public class FuelTotalHistory {

    private final AverageList fuelPerHourShortHistory = new AverageList(15);
    private final AverageList fuelPer100kmShortHistory = new AverageList(15);
    private double totalConsumedLitres = 0.0;
    private Instant lastSampleTime = null;
    private Double lastRateLph = null;

    /**
     * Add a new sample using the current time and both fuel rate and speed.
     *
     * @param litresPerHour current fuel flow (L/h)
     * @param speedKmh      current speed (km/h)
     */
    public void add(double litresPerHour, double speedKmh) {
        Instant now = Instant.now();
        fuelPerHourShortHistory.addNumber(litresPerHour);

        if (speedKmh > 0) {
            fuelPer100kmShortHistory.addNumber(FuelConsumption.calculateLiters100km(litresPerHour, speedKmh));
        }

        if (lastSampleTime != null) {
            double deltaHours = Duration.between(lastSampleTime, now).toMillis() / 3_600_000.0;

            double previousRate = (lastRateLph != null) ? lastRateLph : litresPerHour;
            totalConsumedLitres += 0.5 * (previousRate + litresPerHour) * deltaHours;
        }

        lastSampleTime = now;
        lastRateLph = litresPerHour;
    }

    public void clear() {
        fuelPerHourShortHistory.clear();
        fuelPer100kmShortHistory.clear();
        totalConsumedLitres = 0.0;
        lastSampleTime = null;
        lastRateLph = null;
    }

    public double getCurrentFuelPerHour() {
        return fuelPerHourShortHistory.getAvg();
    }

    public double getCurrentFuelPer100km() {
        return fuelPer100kmShortHistory.getAvg();
    }

    public double getTotalConsumedLitres() {
        return totalConsumedLitres;
    }

}
