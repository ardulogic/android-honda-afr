    package com.hondaafr.Libs.Helpers;

    public class FuelConsumption {

        // Constants
        private static final double GASOLINE_DENSITY = 0.745; // kg/L
        private static final double VE = 0.85; // Volumetric Efficiency (typical)
        private static final double R_SPECIFIC_AIR = 287.058; // J/(kg·K)
        private static final double ATM_PRESSURE = 101.325; // kPa (for total pressure calc)

        public static double estimateVE(double rpm, double mapKpa, double atmPressureKpa) {
            // RPM breakpoints (x-axis)
            final double[] rpmBins = {1000, 2000, 3000, 4000, 5000, 6000};

            // MAP breakpoints (y-axis, in kPa)
            final double[] mapBins = {30, 50, 70, 90, 100};

            // VE table: veTable[rpmIdx][mapIdx]
            final double[][] veTable = {
                    {0.28, 0.38, 0.45, 0.52, 0.60}, // 1000 rpm
                    {0.32, 0.45, 0.58, 0.65, 0.70}, // 2000 rpm
                    {0.35, 0.52, 0.65, 0.72, 0.78}, // 3000 rpm
                    {0.40, 0.60, 0.70, 0.78, 0.85}, // 4000 rpm
                    {0.42, 0.65, 0.75, 0.82, 0.88}, // 5000 rpm
                    {0.44, 0.68, 0.78, 0.85, 0.87}  // 6000 rpm
            };

            // Clamp values to table bounds
            rpm = Math.max(rpmBins[0], Math.min(rpm, rpmBins[rpmBins.length - 1]));
            mapKpa = Math.max(mapBins[0], Math.min(mapKpa, mapBins[mapBins.length - 1]));

            // Find bounding RPM indices
            int rpmIdx = 0;
            while (rpmIdx < rpmBins.length - 1 && rpm > rpmBins[rpmIdx + 1]) {
                rpmIdx++;
            }

            // Find bounding MAP indices
            int mapIdx = 0;
            while (mapIdx < mapBins.length - 1 && mapKpa > mapBins[mapIdx + 1]) {
                mapIdx++;
            }

            // Axis ranges
            double rpmLow = rpmBins[rpmIdx];
            double rpmHigh = rpmBins[rpmIdx + 1];
            double mapLow = mapBins[mapIdx];
            double mapHigh = mapBins[mapIdx + 1];

            // VE values at the 4 corners
            double ve00 = veTable[rpmIdx][mapIdx];         // lower-left
            double ve10 = veTable[rpmIdx + 1][mapIdx];     // lower-right
            double ve01 = veTable[rpmIdx][mapIdx + 1];     // upper-left
            double ve11 = veTable[rpmIdx + 1][mapIdx + 1]; // upper-right

            // Bilinear interpolation
            double t = (rpm - rpmLow) / (rpmHigh - rpmLow);
            double u = (mapKpa - mapLow) / (mapHigh - mapLow);

            double veLower = ve00 + t * (ve10 - ve00);
            double veUpper = ve01 + t * (ve11 - ve01);
            double ve = veLower + u * (veUpper - veLower);

            return ve * 1.48;
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
         * Calculates air density using the Ideal Gas Law.
         *
         * @param pressureKPa Absolute manifold pressure in kPa (from MAP sensor)
         * @param tempC Intake Air Temperature in Celsius (from IAT sensor)
         * @return Air density in kg/m³
         */
        public static double calculateAirDensity(double pressureKPa, double tempC) {
            final double R = 287.058; // Specific gas constant for dry air [J/(kg·K)]

            // Convert pressure to Pascals and temperature to Kelvin
            double pressurePa = pressureKPa * 1000.0;
            double tempK = tempC + 273.15;

            // Ideal gas law: ρ = p / (R * T)
            return pressurePa / (R * tempK);
        }


        public static double calculateLiters100km(double litersPerHour, double speedKmh) {
            if (speedKmh <= 0) {
                // Avoid division by zero and handle vehicle at rest
                return 0;
            }

            return (litersPerHour / speedKmh) * 100.0;
        }

    }

