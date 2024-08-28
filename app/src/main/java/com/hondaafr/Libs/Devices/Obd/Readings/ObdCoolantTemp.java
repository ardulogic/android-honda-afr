package com.hondaafr.Libs.Devices.Obd.Readings;

/**
 * Coolant temp in celsius
 */
public class ObdCoolantTemp extends ObdReading {

    @Override
    public String getName() {
        return "ect";
    }

    @Override
    public String getMeasurement() {
        return "°C";
    }

    @Override
    public String getPid() {
        return "05";
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
