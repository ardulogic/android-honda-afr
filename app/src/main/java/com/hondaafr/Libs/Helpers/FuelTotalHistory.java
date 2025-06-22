package com.hondaafr.Libs.Helpers;

import java.time.Duration;
import java.time.Instant;

public class FuelTotalHistory {

    private final AverageList fuelPerHourHistory = new AverageList(20);
    private final AverageList fuelPer100KmHistory = new AverageList(20);
    private double totalConsumedLitres = 0.0;
    private double totalDistanceKm = 0.0;

    private Instant lastSampleTime = null;
    private Double lastRateLph = null;
    private Double lastSpeedKmh = null;

    /**
     * Add a new sample using the current time and both fuel rate and speed.
     *
     * @param litresPerHour current fuel flow (L/h)
     * @param speedKmh      current speed (km/h)
     */
    public void add(double litresPerHour, double speedKmh) {
        Instant now = Instant.now();
        fuelPerHourHistory.addNumber(litresPerHour);

        if (speedKmh > 0) {
            fuelPer100KmHistory.addNumber(FuelConsumption.calculateLiters100km(litresPerHour, speedKmh));
        }

        if (lastSampleTime != null) {
            double deltaHours = Duration.between(lastSampleTime, now).toMillis() / 3_600_000.0;

            double previousRate = (lastRateLph != null) ? lastRateLph : litresPerHour;
            totalConsumedLitres += 0.5 * (previousRate + litresPerHour) * deltaHours;

            double previousSpeed = (lastSpeedKmh != null) ? lastSpeedKmh : speedKmh;
            totalDistanceKm += 0.5 * (previousSpeed + speedKmh) * deltaHours;
        }

        lastSampleTime = now;
        lastRateLph = litresPerHour;
        lastSpeedKmh = speedKmh;
    }

    public void clear() {
        fuelPerHourHistory.clear();
        fuelPer100KmHistory.clear();
        totalConsumedLitres = 0.0;
        totalDistanceKm = 0.0;
        lastSampleTime = null;
        lastRateLph = null;
        lastSpeedKmh = null;
    }

    public double getAverageFuelPerHour() {
        return fuelPerHourHistory.getAvg();
    }

    public double getAverageFuelPer100km() {
        return fuelPer100KmHistory.getAvg();
    }

    public double getTotalConsumedLitres() {
        return totalConsumedLitres;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public double getLitresPer100Km() {
        return totalDistanceKm > 0.0 ? (totalConsumedLitres / totalDistanceKm) * 100.0 : 0.0;
    }

}
