package com.hondaafr.Libs.Devices.Obd.Readings;

public class ObdSpeed extends ObdReading {

    @Override
    public String getPid() {
        return "0D";
    }

    @Override
    public String getName() {
        return "speed";
    }

    @Override
    public String getDisplayName() {
        return "OSPD";
    }

    @Override
    public String getMeasurement() {
        return "km/h";
    }

    @Override
    public int getDataByteCount() {
        return 1;
    }
}
