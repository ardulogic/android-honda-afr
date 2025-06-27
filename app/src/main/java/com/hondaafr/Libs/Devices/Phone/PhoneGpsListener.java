package com.hondaafr.Libs.Devices.Phone;

public interface PhoneGpsListener {

    void onGpsSpeedUpdated(double speedKmh);
    void onGpsDistanceIncrement(double deltaKm);

}
