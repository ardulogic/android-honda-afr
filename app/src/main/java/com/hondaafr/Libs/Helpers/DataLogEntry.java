package com.hondaafr.Libs.Helpers;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

public class DataLogEntry {

    private LinkedHashMap<String, String> values;

    // Constructor
    public DataLogEntry(HashMap<String, String> values) {
        this.values = new LinkedHashMap<>();

        this.values.put("Timestamp", String.valueOf(System.currentTimeMillis()));
        this.values.putAll(values);
    }


    // Method to return the keys of LinkedHashMap as an array of strings
    public String[] getHeader() {
        return this.values.keySet().toArray(new String[0]);
    }


    public String[] toStringArray() {
        return this.values.values().toArray(new String[0]);
    }
}
