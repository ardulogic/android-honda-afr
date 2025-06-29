package com.hondaafr.Libs.Devices.Spartan;

import android.annotation.SuppressLint;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpartanCommands {
    // Constant for stoichiometric AFR for gasoline
    private static final double STOICHIOMETRIC_AFR = 14.7;

    /**
     * Converts AFR to lambda.
     *
     * @param afr Air-Fuel Ratio to be converted.
     * @return Lambda value.
     */
    public static double afrToLambda(double afr) {
        return afr / STOICHIOMETRIC_AFR;
    }

    /**
     * Converts lambda to AFR.
     *
     * @param lambda Lambda value to be converted.
     * @return Air-Fuel Ratio.
     */
    public static double lambdaToAfr(double lambda) {
        return lambda * STOICHIOMETRIC_AFR;
    }

    public static boolean dataIsTargetLambda(String data) {
        // Define the regex pattern for any number formatted as x.xxx
        String patternString = "^\\d\\.\\d{3}$";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(data);

        // Check if the input data matches the pattern
        return matcher.find();
    }

    public static double parseTargetLambdaAndConvertToAfr(String data) {
        return lambdaToAfr(Double.parseDouble(data));
    }

    public static String requestCurrentAfr() {
        return "G\r\n";
    }

    @SuppressLint("DefaultLocale")
    public static String setAFR(double afr) {
        return String.format("SETNBSWLAM%.3f\r\n", afrToLambda(afr));
    }

    public static String getTargetAFR() {
        return "GETNBSWLAMB\r\n";
    }


    public static boolean dataIsSensorAfr(String data) {
        // Define the regex pattern to match the incoming data format
        Pattern pattern = Pattern.compile("0:a:(\\d+\\.\\d+|\\d+)");
        Matcher matcher = pattern.matcher(data);

        return matcher.find();
    }

    public static Double parseSensorAfr(String data) {
        // Define the regex pattern to match the incoming data format
        Pattern pattern = Pattern.compile("0:a:(\\d+\\.\\d+|\\d+)");
        Matcher matcher = pattern.matcher(data);

        // Check if the input data matches the pattern
        if (matcher.find()) {
            // Extract the numeric value from the matched group
            String numberStr = matcher.group(1);
            // Convert the extracted string to a double
            return Double.parseDouble(numberStr);
        }

        return null;
    }


    public static boolean dataIsSensorTemp(String data) {
        // Define the regex pattern to match the incoming data format
        Pattern pattern = Pattern.compile("1:a:(\\d+\\.\\d+|\\d+)");
        Matcher matcher = pattern.matcher(data);

        // Check if the input data matches the pattern
        return matcher.find();
    }

    public static Double parseSensorTemp(String data) {
        // Define the regex pattern to match the incoming data format
        Pattern pattern = Pattern.compile("1:a:(\\d+\\.\\d+|\\d+)");
        Matcher matcher = pattern.matcher(data);

        // Check if the input data matches the pattern
        if (matcher.find()) {
            // Extract the numeric value from the matched group
            String numberStr = matcher.group(1);
            // Convert the extracted string to a double
            return Double.parseDouble(numberStr);
        }

        return null;
    }
}
