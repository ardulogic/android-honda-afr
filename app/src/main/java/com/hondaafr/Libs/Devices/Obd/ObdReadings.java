package com.hondaafr.Libs.Devices.Obd;

import android.content.Context;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdCoolantTemp;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdIntakeTemp;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdLtftTrim;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdMap;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdRpm;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdSpeed;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdStftTrim;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdTps;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdUpstreamLambdaVoltage;

import java.util.ArrayList;

import java.util.HashMap;

public class ObdReadings {
    private final Context context;
    public final HashMap<String, ObdReading> available = new HashMap<>();
    public final HashMap<String, ObdReading> active = new HashMap<>();
    private int requestIndex = 0;
    private final ArrayList<String> activeKeys = new ArrayList<>();

    public ObdReadings(Context context, ArrayList<String> pid_names) {
        this.context = context;

        addAvailable(new ObdCoolantTemp());
        addAvailable(new ObdIntakeTemp());
        addAvailable(new ObdLtftTrim());
        addAvailable(new ObdStftTrim());
        addAvailable(new ObdRpm());
        addAvailable(new ObdMap());
        addAvailable(new ObdTps());
        addAvailable(new ObdSpeed());
        addAvailable(new ObdUpstreamLambdaVoltage());

        setAsActiveAdd(pid_names);
    }

    private void addAvailable(ObdReading reading) {
        available.put(reading.getName(), reading);
    }

    public void setAsActiveOnly(ArrayList<String> pid_names) {
        active.clear();
        activeKeys.clear();

        for (String pid_name : pid_names) {
            ObdReading reading = available.get(pid_name);
            if (reading != null) {
                active.put(pid_name, reading);
                activeKeys.add(pid_name);
            }
        }
    }

    public void setAsActiveAdd(ArrayList<String> pid_names) {
        for (String pid_name : pid_names) {
            ObdReading reading = available.get(pid_name);
            if (reading != null) {
                active.put(pid_name, reading);
                activeKeys.add(pid_name);
            }
        }
    }

    public void addActive(String name) {
        if (!active.containsKey(name)) {
            ObdReading reading = available.get(name);
            if (reading != null) {
                active.put(name, reading);
                activeKeys.add(name);
            }
        }
    }

    public ObdReading getActive(String name) {
        return active.get(name);
    }

    public ObdReading getAvailable(String name) {
        return available.get(name);
    }

    public ArrayList<String> getActiveIds() {
        return new ArrayList<>(active.keySet());
    }

    public boolean toggleActive(String name) {
        if (active.containsKey(name)) {
            active.remove(name);
            activeKeys.remove(name);
            return false;
        } else {
            addActive(name);
            return true;
        }
    }

    public void requestNextReading() {
        if (active.isEmpty()) return;

        requestIndex = (requestIndex + 1) % activeKeys.size();
        String key = activeKeys.get(requestIndex);
        ObdReading reading = active.get(key);
        if (reading != null) {
            reading.request(context);
        }
    }

    public boolean isActive(String name) {
        return active.containsKey(name);
    }

    public void setAllInactive() {
        active.clear();
        activeKeys.clear();
    }
}

