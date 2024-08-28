package com.hondaafr.Libs.Devices.Obd.Readings;


public class ObdRpm extends ObdReading {

    @Override
    public String getName() {
        return "rpm";
    }

    @Override
    public String getMeasurement() {
        return "";
    }

    @Override
    public String getPid() {
        return "0C";
    }

    @Override
    public int getDataByteCount() {
        return 2;
    }

    @Override
    public Object parseIntValue(int value) {
        if (value > 0) {
            return value / 4;
        } else {
            return 0;
        }
    }
}
