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

public class ObdReadings {
    private final Context context;
    public final ArrayList<ObdReading> available = new ArrayList<>();
    public final ArrayList<ObdReading> active = new ArrayList<>();
    private int requestIndex = 0; // We're requesting data one-by-one


    public ObdReadings(Context context, ArrayList<String> pid_names) {
        this.context = context;

        this.available.add(new ObdCoolantTemp());
        this.available.add(new ObdRpm());
        this.available.add(new ObdIntakeTemp());
        this.available.add(new ObdLtftTrim());
        this.available.add(new ObdStftTrim());
        this.available.add(new ObdRpm());
        this.available.add(new ObdMap());
        this.available.add(new ObdTps());
        this.available.add(new ObdSpeed());
        this.available.add(new ObdUpstreamLambdaVoltage());

        setAsActive(pid_names);
    }

    public void setAsActive(ArrayList<String> pid_names) {
        this.active.clear();

        for (String pid_name : pid_names) {
            for (ObdReading reading : this.available) {
                if (reading.getName().equals(pid_name)) {
                    this.active.add(reading);
                }
            }
        }
    }

    public void addActive(String name) {
        for (ObdReading reading : this.available) {
            if (reading.getName().equals(name)) {
                this.active.add(reading);
                break;
            }
        }
    }

    public ObdReading getActive(String name) {
        // Search for the reading in the targetReadings list
        ObdReading r = null;

        for (ObdReading reading : this.active) {
            if (reading.getName().equals(name)) {
                r = reading;
                break;
            }
        }

        return r;
    }

    public ObdReading getAvailable(String name) {
        // Search for the reading in the targetReadings list
        ObdReading r = null;

        for (ObdReading reading : this.available) {
            if (reading.getName().equals(name)) {
                r = reading;
                break;
            }
        }

        return r;
    }

    public ArrayList<String> getActiveIds() {
        ArrayList<String> obdPids = new ArrayList<>();
        for (ObdReading reading : this.active) {
            obdPids.add(reading.getName());
        }

        return obdPids;
    }


    public boolean toggleActive(String name) {
        // Search for the reading in the targetReadings list
        ObdReading t = getActive(name);

        if (t != null) {
            this.active.remove(t);

            return false;
        } else {
            addActive(name);

            return true;
        }
    }


    public void requestNextReading() {
        if (this.active.isEmpty()) {
            return; // Handle the case where there are no readings.
        }

        requestIndex = (requestIndex + 1) % this.active.size(); // Wraps around when it reaches the end.

        this.active.get(requestIndex).request(this.context);
    }

    public boolean isActive(String name) {
        ObdReading r = getActive(name);

        return r != null;
    }


    public void setAllInactive() {
        this.active.clear();
    }
}
