package com.hondaafr.Libs.Helpers.TripComputer;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;

public interface TripComputerListener {

    void onGpsUpdate(Double speed, double distanceIncrement);
    void onGpsPulse(PhoneGps gps);

    void onAfrPulse(boolean isActive);
    void onAfrTargetValue(double targetAfr);

    void onAfrValue(Double afr);

    void onObdPulse(boolean isActive);

    void onObdActivePidsChanged();

    void onObdValue(ObdReading reading);

    void onCalculationsUpdated();

}
