package com.hondaafr.Libs.Helpers;
public class DataLogEntry {
    public double target_afr;
    public double afr;
    public int map;
    public long timestamp;
    public double temperature;
    public int speed;
    public double tps;

    // Constructor
    public DataLogEntry(double targetAfr, Double afr, Double tps, Integer map, int speed, Double sensorTemp) {
        this.timestamp = System.currentTimeMillis();
        this.target_afr = targetAfr;
        this.afr = afr;
        this.tps = tps;
        this.map = map;
        this.speed = speed;
        this.temperature = sensorTemp;

    }


    public static String[] getHeader() {
        return new String[]{"Timestamp",  "TAFR",  "AFR", "Throttle", "MAP", "Speed", "Temperature"};
    }


    public String[] toStringArray() {
        return new String[] {
                String.valueOf(timestamp),
                String.valueOf(target_afr),
                String.valueOf(afr),
                String.valueOf(tps),
                String.valueOf(map),
                String.valueOf(speed),
                String.valueOf(temperature),
        };
    }
}
