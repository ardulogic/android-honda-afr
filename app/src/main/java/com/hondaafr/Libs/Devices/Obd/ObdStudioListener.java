package com.hondaafr.Libs.Devices.Obd;

public interface ObdStudioListener {
    void onObdTpsReceived(Double throttle_position);
    void onObdSpeedReceived(Integer speed);

    void onObdRpmReceived(Integer rpm);
    void onObdMapReceived(Integer mapKpa);
    void onObdIntakeTempReceived(Integer tempC);
}
