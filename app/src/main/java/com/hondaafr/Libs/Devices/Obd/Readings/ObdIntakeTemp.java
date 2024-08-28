package com.hondaafr.Libs.Devices.Obd.Readings;

/**
 * Intake manifold temperature
 */
public class ObdIntakeTemp extends ObdReading {

    @Override
    public String getName() {
        return "iat";
    }

    @Override
    public String getMeasurement() {
        return "°C";
    }

    @Override
    public String getPid() {
        return "0F";
    }

    @Override
    public int getDataByteCount() {
        return 1;
    }

    @Override
    public Object parseIntValue(int value) {
        if (value > 0) {
            return value - 40;
        }

        return 0;
    }


}
