package com.hondaafr.Libs.Devices.Obd;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;

public interface ObdStudioListener {
    void onObdConnectionPulse(boolean isActive);

    void onObdReadingUpdate(ObdReading reading);

    void onObdActivePidsChanged();

    void onObdConnectionError(String s);
}
