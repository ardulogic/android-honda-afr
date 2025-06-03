package com.hondaafr.Libs.Helpers;

public class FuelConsumption {

    // Constants
    private static final double GASOLINE_DENSITY = 0.745; // kg/L
    private static final double VE = 0.85; // Volumetric Efficiency (typical)
    private static final double R_SPECIFIC_AIR = 287.058; // J/(kg·K)
    private static final double ATM_PRESSURE = 101.325; // kPa (for total pressure calc)

    /**
     * Calculates air density using ideal gas law.
     *
     * @param pressureKPa Absolute pressure in kPa (e.g., MAP)
     * @param tempC Temperature in Celsius (IAT)
     * @return Air density in kg/m³
     */
    public static double calculateAirDensity(double pressureKPa, double tempC) {
        double pressurePa = pressureKPa * 1000.0;
        double tempK = tempC + 273.15;
        return pressurePa / (R_SPECIFIC_AIR * tempK);
    }


    /* ───────────── VE table with part-throttle correction ───────────── */
    /*  Look-up VE at WOT from a 1 k-rpm table, then
     *  scale linearly with manifold pressure.          */
    public static double estimateVE(double rpm,
                                    double mapKpa,
                                    double atmPressureKpa) {

        /* D16W7 WOT VE, 1 000 → 6 000 rpm               */
        final double[] wotVe = { 0.68, 0.78, 0.86, 0.92, 0.88, 0.80 };

        int idx = (int) (rpm / 1_000);
        if (idx >= wotVe.length) idx = wotVe.length - 1;

        double veWot    = wotVe[idx];
        double mapFactor = mapKpa / atmPressureKpa;      // 0.0–1.0

//        return veWot * mapFactor;                        // no TPS!
        return 0.85;                        // no TPS!ddddddddd
    }




    /**
     * Calculates estimated fuel consumption in L/100km using dynamic air density.
     *
     * @param afr Air-Fuel Ratio
     * @param rpm Engine RPM
     * @param map MAP in kPa
     * @param iat Intake Air Temp in °C
     * @param displacement Engine displacement in liters
     * @return Fuel consumption in L/100km
     */
    public static double calculateFuelConsumptionLperHour(double afr, double rpm, double map, double iat, double atm,
                                                  double displacement) {

        if (afr <= 0 || rpm <= 0 || map <= 0 || displacement <= 0) {
            return 0;
        }

        // Calculate dynamic air density
        double airDensity = calculateAirDensity(map, iat); // kg/m³

        // Calculate engine airflow in m³/s
        double engineCyclesPerSec = rpm / 120.0;
        double airVolumePerCycle =       (displacement / 1000.0) * estimateVE(rpm, map, atm);
        double airFlowVolumePerSec = engineCyclesPerSec * airVolumePerCycle;

        // Airflow in kg/s
        double airMassFlowKgPerSec = airFlowVolumePerSec * airDensity;

        // Fuel flow in kg/s
        double fuelMassFlowKgPerSec = airMassFlowKgPerSec / afr;

        // Convert to L/h
        double fuelVolumeLPerHour = (fuelMassFlowKgPerSec * 3600.0) / GASOLINE_DENSITY;

        return fuelVolumeLPerHour;
    }

    /**
     * Calculates estimated fuel consumption in L/100km using dynamic air density.
     *
     * @param speedKmH Vehicle speed in km/h
     * @return Fuel consumption in L/100km
     */
    public static double calculateFuelConsumptionPer100km(double fuelVolumeLPerHour, double speedKmH) {

        // Fuel consumption in L/100km
        if (speedKmH > 10) {
            return (fuelVolumeLPerHour / speedKmH) * 100.0;
        } else {
            return fuelVolumeLPerHour;
        }
    }

    public static double calculateFuelConsumptionPer100km(double afr, double rpm, double map, double iat,
                                                          double atm, double displacement, double speedKmh) {

        return calculateFuelConsumptionPer100km(calculateFuelConsumptionLperHour(afr, rpm, map, iat, atm, displacement), speedKmh);
    }
}

