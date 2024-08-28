package com.hondaafr.Libs.Devices.Obd.Readings;

/**
 * Intake manifold air pressure
 */
public class ObdMap extends ObdReading {

    @Override
    public String getName() {
        return "map";
    }

    @Override
    public String getMeasurement() {
        return "kPa";
    }

    @Override
    public String getPid() {
        return "0B";
    }

    @Override
    public int getDataByteCount() {
        return 1;
    }

}
