package com.hondaafr.Libs.Devices.Obd.Readings;

import android.annotation.SuppressLint;
import android.content.Context;

import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ObdReading {

    public int intValue = 0;
    public Object value = 0;
    public Long timeUpdated = null;

    abstract public String getPid();

    abstract public String getName();

    abstract public String getMeasurement();

    abstract public int getDataByteCount();

    public String getDisplayName() {
        return getName().toUpperCase();
    }

    public String getMachineName() {
        return getName().toLowerCase();
    }

    private String getRequestCommand() {
        return "01" + getPid() + "\r";
    }

    public void request(Context context) {
        BluetoothService.send(context, getRequestCommand(), "obd");
    }

    public void onData(String data) {
        if (incomingDataIsReply(data)) {
            this.intValue = parseReading(data);

            timeUpdated = System.currentTimeMillis();
        }

        this.value = parseIntValue(this.intValue);
    }

    /**
     * Do required conversions
     *
     * @param value
     * @return Object
     */
    public Object parseIntValue(int value) {
        return value;
    }

    public Object getValue() {
        return value;
    }

    public boolean incomingDataIsReply(String data) {
        // Check if the response data contains the PID for Throttle Position
        return data.contains("41 " + getPid() + " ");
    }

    protected int parseReading(String data) {
        Matcher m = parseHexValues(data);

        if (m != null) {
            try {
                String hexValue = m.group(1); // Extract the third two-digit hex number
                if (getDataByteCount() == 2) {
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

    protected Double parseVoltageReading(String data) {
        Matcher m = parseHexValues(data);

        if (m != null) {
            try {
                String hexValueA = m.group(1); // Extract the third two-digit hex number
                String hexValueB = m.group(2);

                // Convert hex values to integers
                int A = Integer.parseInt(hexValueA, 16); // Convert 'B3' to an integer
                int B = Integer.parseInt(hexValueB, 16); // Apparently this is a fuel trim

                return A / 200.0;
            } catch (NumberFormatException e) {
                // Handle cases where parsing fails
                System.err.println("Error parsing value: " + e.getMessage());
                return 0D;
            }
        }

        return 0D;
    }

    protected Matcher parseHexValues(String data) {
        // Define the regex pattern to extract three two-digit hex numbers
        String pattern = "41\\s" + getPid() + "\\s([0-9A-Fa-f]{2})";
        if (getDataByteCount() == 2) {
            pattern = "41\\s" + getPid() + "\\s([0-9A-Fa-f]{2})\\s([0-9A-Fa-f]{2})";
        }
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(data);

        if (m.find()) {
            return m;
        }

        return null;
    }

    public String getValueAsString() {
        return String.valueOf(this.getValue());
    }

    public String getFormattedValue() {
        return formatValue(getValue());
    }

    /**
     * Prepare value for display, ex.: Round it, etc
     *
     * @return String
     */
    @SuppressLint("DefaultLocale")
    protected String formatValue(Object value) {
        if (value instanceof Double) {
            return String.format("%.1f", value);
        } else {
            return getValueAsString();
        }
    }

    public String getDisplayValue() {
        return getDisplayName() + ": " + getFormattedValue() + " " + getMeasurement();
    }

    public Long getTimeSinceLastUpdate() {
        if (timeUpdated != null) {
            return System.currentTimeMillis() - timeUpdated;
        }

        return Long.MAX_VALUE; // Acts as a stand-in for infinity
    }
}
