package com.hondaafr.Libs.Devices.Obd.Readings;

/**
 * Throttle position sensor
 */
public class ObdTps extends ObdReading {

    @Override
    public String getName() {
        return "tps";
    }

    @Override
    public String getMeasurement() {
        return "%";
    }

    @Override
    public String getPid() {
        return "11";
    }

    @Override
    public int getDataByteCount() {
        return 1;
    }

    @Override
    public Object parseIntValue(int value) {
        if (value > 0) {
            return (double) (value * 100) / 255.0; // Convert to percentage
        } else {
            return 0D;
        }
    }

}
