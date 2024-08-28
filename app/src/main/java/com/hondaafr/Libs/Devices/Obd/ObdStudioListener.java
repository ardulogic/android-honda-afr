package com.hondaafr.Libs.Devices.Obd;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;

public interface ObdStudioListener {

    void onObdReadingUpdate(ObdReading reading);
}
