package com.hondaafr.Libs.Devices.Obd;

import android.content.Context;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdCoolantTemp;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdIntakeTemp;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdLtftTrim;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdMap;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdRpm;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdStftTrim;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdTps;

import java.util.ArrayList;

public class ObdCommands {

    public static String resetObd() {
        return "ATZ\r"; // Mode 01, PID 11 is for Throttle Position
    }

    public static boolean dataIsBusy(String data) {
        return data.toLowerCase().contains("searching");
    }

    public static boolean dataCantConnectToEcu(String data) {
        return data.toLowerCase().contains("unable to connect") ||
                data.toLowerCase().contains("no data") ||
                data.toLowerCase().contains("nodata");
    }

    // OBD command to request all pids
    public static String requestPids() {
        return "0100\r";
    }

    public static boolean dataEcuConnected(String data) {
        // Check if the response data contains the PID for Throttle Position
        return data.contains("41 ") || data.contains("ELM327");
    }


}
