package com.hondaafr.Libs.Devices.Spartan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpartanSensorStatus {

    public Double afr;
    public Double temperature;

    public SpartanSensorStatus (Double afr, Double temp) {
        this.afr = afr;
        this.temperature = temp;
    }

}
