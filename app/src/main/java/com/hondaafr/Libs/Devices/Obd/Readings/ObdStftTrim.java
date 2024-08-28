package com.hondaafr.Libs.Devices.Obd.Readings;

/**
 * Short-term fuel trim
 */
public class ObdStftTrim extends ObdReading {

    @Override
    public String getName() {
        return "stft";
    }

    @Override
    public String getMeasurement() {
        return "%";
    }

    @Override
    public String getPid() {
        return "06";
    }

    @Override
    public int getDataByteCount() {
        return 1;
    }

    @Override
    public Object parseIntValue(int value) {
        // Convert the raw byte value to a percentage
        return (double) (value - 128) * 100 / 128.0;
    }
}
