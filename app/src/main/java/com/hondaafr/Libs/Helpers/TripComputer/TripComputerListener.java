package com.hondaafr.Libs.Helpers.TripComputer;

import com.hondaafr.Libs.Helpers.ReadingHistory;

public interface TripComputerListener {

    void onTripComputerReadingsUpdated();

    void onGpsUpdated(Double speed, double distanceIncrement);

}
