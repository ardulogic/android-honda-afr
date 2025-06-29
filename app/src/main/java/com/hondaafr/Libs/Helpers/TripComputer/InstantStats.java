package com.hondaafr.Libs.Helpers.TripComputer;

import android.content.Context;

import com.hondaafr.Libs.Devices.Phone.PhoneBarometer;
import com.hondaafr.Libs.Helpers.AverageList;
import com.hondaafr.Libs.Helpers.FuelConsumption;
import com.hondaafr.Libs.Helpers.TotalLitersConsumed;

import java.time.Duration;
import java.time.Instant;

public class InstantStats {

    // Constants for SharedPreferences keys
    private final PhoneBarometer barometer;

    private static Double lastRateLph;
    private static double rateLph = 0;
    private static double rateLp100km = 0;
    private static Instant lastSampleTime = null;
    private final static AverageList lphHistory = new AverageList(15);
    private final static AverageList lp100kmHistory = new AverageList(15);

    public InstantStats(Context c) {
        barometer = new PhoneBarometer(c);
    }

    public void onReadingsReceived(Double afr, Integer iatObd, Integer rpmObd, Integer mapObd, double v, double speed) {
        double iat = iatObd > 0 ? iatObd : 35;

        rateLph = FuelConsumption.calculateFuelConsumptionLperHour(
                afr,
                rpmObd,
                mapObd,
                iat,
                barometer.getPressureKPa(),
                1.590);

        lphHistory.add(rateLph);

        if (speed > 0) {
            lp100kmHistory.add(rateLp100km);
            rateLp100km = FuelConsumption.calculateLiters100km(rateLph, speed);
        }
    }

    public double getLph() {
        return rateLph;
    }

    public double getLphAvg() {
        return lphHistory.getAvg();
    }

    public double getLp100km() {
        return Math.min(rateLp100km, 30);
    }

    public double getLp100kmAvg() {
        return Math.min(lp100kmHistory.getAvg(), 30);
    }

    public double getLitersIncrement() {
        Instant now = Instant.now();
        double incrementL = 0D;

        if (lastSampleTime != null) {
            double deltaHours = Duration.between(lastSampleTime, now).toMillis() / 3_600_000.0;

            double previousRate = (lastRateLph != null) ? lastRateLph : rateLph;

            incrementL = 0.5 * (previousRate + rateLph) * deltaHours;
        }

        lastSampleTime = now;
        lastRateLph = rateLph;

        return incrementL;
    }

    public String getName() {
        return "Instant";
    }

}
