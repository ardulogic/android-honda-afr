package com.hondaafr.Libs.Helpers;

import java.time.Duration;
import java.time.Instant;

/**
 * Keeps a rolling history of instantaneous fuel-flow readings (L/h) and
 * integrates them to produce total fuel consumed.
 *
 * <h2>How to use</h2>
 * <ol>
 *   <li>Call {@link #add(double, Instant)} with the current fuel-flow rate and its timestamp,
 *       <em>or</em> use {@link #add(double, double)} when you know the exact time-step in hours.</li>
 *   <li>Retrieve totals with {@link #getTotalConsumedLitres()}.</li>
 * </ol>
 *
 * If you sample once per second, call<br>
 * <code>history.add(lph, 1.0 / 3600.0);</code>
 */
public class FuelTotalHistory {

    /** optional rolling buffer for stats (min/avg/max) */
    private final AverageList rateHistory = new AverageList(20);

    /** litres consumed since last {@link #clear()} */
    private double totalConsumedLitres = 0.0;

    /** timestamp of the previous reading, or {@code null} if none yet */
    private Instant lastSampleTime = null;

    /** last fuel-flow reading (L/h), or {@code null} if none yet */
    private Double lastRateLph = null;

    /* ────────────────────────── public API ───────────────────────── */

    /**
     * Convenience overload that stamps the reading with the current system time.
     * Internally delegates to {@link #add(double, Instant)}.
     *
     * <p>Example, sampling once per second:</p>
     * <pre>
     *   history.add(currentLph);   // timestamp is Instant.now()
     * </pre>
     *
     * @param litresPerHour current instantaneous rate (L/h)
     */
    public void add(double litresPerHour) {
        add(litresPerHour, Instant.now());
    }

    /**
     * Add a fuel-flow reading using timestamps.  Fuel burned in the interval is
     * integrated with the trapezoidal rule (average of previous and current rates).
     *
     * @param litresPerHour current instantaneous rate (L/h)
     * @param sampleTime    timestamp of this reading
     */
    public void add(double litresPerHour, Instant sampleTime) {
        rateHistory.addNumber(litresPerHour);

        if (lastSampleTime != null) {
            double hours =
                    Duration.between(lastSampleTime, sampleTime).toMillis() / 3_600_000.0;

            // Trapezoidal rule: ½·(prev + current)·Δt
            double previous = (lastRateLph != null) ? lastRateLph : litresPerHour;
            totalConsumedLitres += 0.5 * (previous + litresPerHour) * hours;
        }

        lastSampleTime = sampleTime;
        lastRateLph    = litresPerHour;
    }

    /**
     * Convenience overload when you know the exact time-step in hours
     * (e.g. 1 s → 0.000 277 h, 1 min → 0.016 667 h, 1 h → 1 h).
     * Uses the trapezoidal rule when a previous rate is available.
     */
    public void add(double litresPerHour, double deltaHours) {
        rateHistory.addNumber(litresPerHour);

        if (lastRateLph != null) {
            totalConsumedLitres += 0.5 * (lastRateLph + litresPerHour) * deltaHours;
        } else {
            // First sample: no previous rate, so step-hold is fine
            totalConsumedLitres += litresPerHour * deltaHours;
        }

        lastRateLph = litresPerHour;
        // lastSampleTime intentionally left untouched (not used by this overload)
    }

    /** Clear <em>everything</em>: history and cumulative total. */
    public void clear() {
        rateHistory.clear();
        totalConsumedLitres = 0.0;
        lastSampleTime = null;
        lastRateLph = null;
    }

    /* ───────────── getters: stats + integrated total ────────────── */

    /** Rolling average of the fuel-flow rate (L/h). */
    public double getAverageRate() {
        return rateHistory.getAvg();
    }

    /** Total fuel consumed since last {@link #clear()} (litres). */
    public double getTotalConsumedLitres() {
        return totalConsumedLitres;
    }

    /**
     * Helper: distance (L/h) that the rolling average sits from a target rate.
     */
    public double getAverageDistanceFromTarget(double targetLph) {
        return rateHistory.getAverageDistanceFromTarget(targetLph);
    }
}
