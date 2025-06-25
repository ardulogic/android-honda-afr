package com.hondaafr.Libs.Devices.Obd;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;

public interface ObdStudioListener {
    void onObdConnectionActive();
    void onObdConnectionLost();

    void onObdReadingUpdate(ObdReading reading);

    void onActivePidsChanged();

    void onObdConnectionError(String s);
}
