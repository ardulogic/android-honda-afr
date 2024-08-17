package com.hondaafr.Libs.Devices.Obd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // OBD command to request Throttle Position Sensor data
    public static String requestTps() {
        return "0111\r"; // Mode 01, PID 11 is for Throttle Position
    }

    public static String requestMap() {
        return "010B\r"; // Mode 01, PID 11 is for Throttle Position
    }

    public static String requestRpm() {
        return "010C\r";
    }

    public static String requestIntakeTemp() {
        return "010F\r";
    }

    public static String requestShortTermFuelTrimBank1() {
        return "0106\r"; // Mode 01, PID 06 is for Short Term Fuel Trim (Bank 1)
    }

    public static String requestSpeed() {
        return "010D\r"; // Mode 01, PID 0D is for Vehicle Speed
    }


    // Method to check if the received data is for TPS
    public static boolean dataIsTps(String data) {
        // Check if the response data contains the PID for Throttle Position
        return data.contains("41 11 ");
    }

    public static boolean dataIsSpeed(String data) {
        // Check if the response data contains the PID for Throttle Position
        return data.contains("41 0D");
    }

    public static boolean dataIsRpm(String data) {
        // Check if the response data contains the PID for Throttle Position
        return data.contains("41 0C");
    }

    public static boolean dataIsMap(String data) {
        // Check if the response data contains the PID for Throttle Position
        return data.contains("41 0B");
    }

    public static boolean dataIsCoolantTemp(String data) {
        // Check if the response data contains the PID for Throttle Position
        return data.contains("41 05");
    }


    public static boolean dataIsIntakeTemp(String data) {
        // Check if the response data contains the PID for Throttle Position
        return data.contains("41 0F");
    }


    public static boolean dataEcuConnected(String data) {
        // Check if the response data contains the PID for Throttle Position
        return data.contains("41 ");
    }

    // Method to parse the TPS data from the response
    public static Double parseTpsData(String data) {
        int reading = parseReading(data, "11", 1);

        if (reading > 0) {
            return (reading * 100) / 255.0; // Convert to percentage
        } else {
            return (double) reading;
        }
    }

    // Method to parse the TPS data from the response
    public static int parseSpeedData(String data) {
        return parseReading(data, "0D", 1);
    }

    public static int parseRpmData(String data) {
        return parseReading(data, "0C", 2) / 4;
    }

    public static int parseMapData(String data) {
        return parseReading(data, "0B", 1);
    }

    public static int parseIntakeTempData(String data) {
        return parseReading(data, "0F", 1) - 40;
    }



    private static Integer parseReading(String data, String pid, int bytes) {
        // Define the regex pattern to extract three two-digit hex numbers
        String pattern =  "41\\s" + pid + "\\s([0-9A-Fa-f]{2})";
        if (bytes == 2) {
            pattern = "41\\s" + pid + "\\s([0-9A-Fa-f]{2})\\s([0-9A-Fa-f]{2})";
        }
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(data);

        if (m.find()) {
            try {
                String hexValue = m.group(1); // Extract the third two-digit hex number
                if (bytes == 2) {
                    hexValue += m.group(2);
                }

                return Integer.parseInt(hexValue, 16); // Convert hex to decimal
            } catch (NumberFormatException e) {
                // Handle cases where parsing fails
                System.err.println("Error parsing value: " + e.getMessage());
                return 0; // Indicate error
            }
        }

        return 0; // Indicate error if format does not match
    }

    public static boolean dataInitComplete(String data) {
        return data.contains("ELM327");
    }
}
