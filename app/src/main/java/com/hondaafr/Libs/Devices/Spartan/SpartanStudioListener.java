package com.hondaafr.Libs.Devices.Spartan;

public interface SpartanStudioListener {
    void onTargetAfrUpdated(double targetAfr);

    void onSensorAfrReceived(Double afr);

    void onSensorTempReceived(Double temp);

}
