package com.hondaafr.Libs.Devices.Obd;

import android.annotation.SuppressLint;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObdCommands {


    // OBD command to request Throttle Position Sensor data
    public static String requestTps() {
        return "0111"; // Mode 01, PID 11 is for Throttle Position
    }

    // Method to check if the received data is for TPS
    public static boolean dataIsTps(String data) {
        // Check if the response data contains the PID for Throttle Position
        return data.startsWith("41 11");
    }

    // Method to parse the TPS data from the response
    public static Double parseTpsData(String data) {
        // Response format for TPS: "41 11 XX" where XX is the TPS value in hex
        String[] parts = data.split(" ");
        if (parts.length >= 3) {
            int tpsValue = Integer.parseInt(parts[2], 16);
            return (tpsValue * 100) / 255.0; // Convert to percentage
        }

        return null; // Indicate error
    }

}
