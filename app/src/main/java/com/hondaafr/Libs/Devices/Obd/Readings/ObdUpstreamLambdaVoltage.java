package com.hondaafr.Libs.Devices.Obd.Readings;

import java.util.regex.Matcher;

/**
 * Throttle position sensor
 */
public class ObdUpstreamLambdaVoltage extends ObdReading {

    @Override
    public String getName() {
        return "uo2v";
    }

    @Override
    public String getMeasurement() {
        return "V";
    }

    @Override
    public String getPid() {
        return "14";
    }

    @Override
    public int getDataByteCount() {
        return 2;
    }

    @Override
    public void onData(String data) {
        if (incomingDataIsReply(data)) {
            this.value = parseVoltageReading(data);
        }
    }

}
