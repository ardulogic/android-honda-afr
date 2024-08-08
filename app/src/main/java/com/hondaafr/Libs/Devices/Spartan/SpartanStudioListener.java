package com.hondaafr.Libs.Devices.Spartan;

import java.util.ArrayList;

public interface SpartanStudioListener {
    void onTargetAfrUpdated(double targetAfr);


    void onSensorAfrReceived(Double afr);

    void onSensorTempReceived(Double temp);
}
