package com.hondaafr.Libs.Devices.Phone;

public interface PhoneGpsListener {

    void onUpdate(double speedKmh, double deltaKm, float accuracy);

}
