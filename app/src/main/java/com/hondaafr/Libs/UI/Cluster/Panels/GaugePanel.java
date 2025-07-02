package com.hondaafr.Libs.UI.Cluster.Panels;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class GaugePanel extends Panel  {

    private final ImageView imageNeedle;
    private final TextView textClusterLcd;
    private final TextView textClusterLcdMode1;
    private final TextView textClusterLcdMode2;
    private final ImageView imageRichFuel;
    private final ImageView imageObd;
    private final ImageView imageAfr;
    private final ImageView imageGps;
    private final ImageView imageGauge;
    private final ImageView imageLcd;
    private ValueAnimator needleAnimator;
    private ValueAnimator gpsAnimator;
    private float targetNeedleRotation = 0;
    private float currentNeedleRotation = 0f;
    private int modeIndex = 0;
    int gray = 0xFF222222;
    int red = 0xFFC82B28;
    int orange = 0xFFFFA500;

    @Override
    public int getContainerId() {
        return R.id.layoutClusterGauge;
    }

    @Override
    public String getListenerId() {
        return "cluster_gauge";
    }


    public GaugePanel(MainActivity mainActivity, TripComputer tripComputer, UiView view) {
        super(mainActivity, tripComputer, view);

        this.imageNeedle = mainActivity.findViewById(R.id.imageClusterNeedle);
        this.imageGauge = mainActivity.findViewById(R.id.imageClusterGauge);
        this.imageLcd = mainActivity.findViewById(R.id.imageClusterLcd);
        this.textClusterLcd = mainActivity.findViewById(R.id.textClusterLcd);
        this.textClusterLcdMode1 = mainActivity.findViewById(R.id.textClusterLcdMode1);
        this.textClusterLcdMode2 = mainActivity.findViewById(R.id.textClusterLcdMode2);
        this.imageRichFuel = mainActivity.findViewById(R.id.imageClusterRichFuel);
        this.imageObd = mainActivity.findViewById(R.id.imageClusterObd);
        this.imageAfr = mainActivity.findViewById(R.id.imageClusterAfr);
        this.imageGps = mainActivity.findViewById(R.id.imageClusterGps);
    }

    @Override
    public void onParentReady() {
        startNeedleAnimator();

        updateAfrCel();
        updateObdCel();
        updateDisplay();
    }

    @Override
    public View[] getViewsHiddenInPip() {
        return new View[] {
                imageLcd,
                textClusterLcd,
                textClusterLcdMode1,
                textClusterLcdMode2,
                imageRichFuel,
                imageObd,
                imageAfr
        };
    }

    @Override
    public void onNightModeChanged(boolean isNightMode) {
        if (isNightMode) {
            imageNeedle.setImageResource(R.drawable.fuel_gauge_needle_night);
            imageGauge.setImageResource(R.drawable.fuel_gauge_night);
            imageLcd.setImageResource(R.drawable.fuel_gauge_lcd_night);

            imageObd.setImageResource(R.drawable.obd_night);
            imageAfr.setImageResource(R.drawable.afr_night);
            imageGps.setImageResource(R.drawable.gps_night);
            imageRichFuel.setImageResource(R.drawable.fuel_gauge_rich_fuel_night);
        } else {
            imageNeedle.setImageResource(R.drawable.fuel_gauge_needle_day);
            imageGauge.setImageResource(R.drawable.fuel_gauge_day);
            imageLcd.setImageResource(R.drawable.fuel_gauge_lcd_day);

            imageObd.setImageResource(R.drawable.obd);
            imageAfr.setImageResource(R.drawable.afr);
            imageGps.setImageResource(R.drawable.gps);
            imageRichFuel.setImageResource(R.drawable.fuel_gauge_rich_fuel);
        }
    }

    @SuppressLint("DefaultLocale")
    private final ModeDescriptor[] modeDescriptors = new ModeDescriptor[]{
            new ModeDescriptor("TRIP", "", tc -> String.format("%.1f", tc.tripStats.getDistanceKm())),                   // MODE_TRIP_KM
            new ModeDescriptor("TRIP", "", tc -> String.format("%.2f L", tc.tripStats.getLiters())),                     // MODE_TRIP_L
            new ModeDescriptor("TRIP", "/100", tc -> String.format("%.2f L", tc.tripStats.getLitersPer100km())),         // MODE_TRIP_L100

            new ModeDescriptor("TOTAL", "", tc -> String.format("%05.0f", tc.totalStats.getDistanceKm())),               // MODE_TOTAL_KM
            new ModeDescriptor("TOTAL", "", tc -> String.format("%.2f L", tc.totalStats.getLiters())),                   // MODE_TOTAL_L
            new ModeDescriptor("TOTAL", "/100", tc -> String.format("%.2f L", tc.totalStats.getLitersPer100km())),        // MODE_TOTAL_L100

            new ModeDescriptor("INST", "/h", tc -> String.format("%.2f L", tc.instStats.getLphAvg())),                // MODE_TRIP_L
            new ModeDescriptor("INST", "/100", tc -> String.format("%.2f L", tc.instStats.getLp100kmAvg())),                // MODE_TRIP_L
            new ModeDescriptor("INST", "AFR", tc -> String.format("%.2f", tc.afrHistory.getAvg()))                     // MODE_AFR
    };

    public void updateDisplay() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(this::updateDisplay);
            return;
        }

        if (modeIndex >= 0 && modeIndex < modeDescriptors.length) {
            ModeDescriptor descriptor = modeDescriptors[modeIndex];
            textClusterLcdMode1.setText(descriptor.label1);
            textClusterLcdMode2.setText(descriptor.label2);
            textClusterLcd.setText(descriptor.formatter.format(tripComputer));
        }

        if (tripComputer.mObdStudio.isReading()) {
            this.targetNeedleRotation = calculateNeedleRotation(tripComputer.instStats.getLphAvg());
        } else {
            this.targetNeedleRotation = calculateNeedleRotation(0);
        }

        int richFuelIconColor = tripComputer.afrIsRich() && tripComputer.mSpartanStudio.lastSensorAfr > 0 ? orange : gray;

        imageRichFuel.setColorFilter(richFuelIconColor);
        imageRichFuel.setTag(richFuelIconColor);

        Log.d("GaugePanel", "Updated display.");
    }

    /**
     * Calculates the rotation angle for the needle based on consumption.
     * This is a placeholder mapping. Adjust the logic as needed.
     */
    private float calculateNeedleRotation(double consumption) {
        double minVal = 0.0;
        double maxVal = 13.0;
        double startDeg = -45;
        double endDeg = 198.0;

        // Clamp value
        consumption = Math.max(minVal, Math.min(maxVal, consumption));

        // Linear interpolation
        double angle = startDeg + (consumption - minVal) / (maxVal - minVal) * (endDeg - startDeg);
        return (float) angle;
    }

    private void startNeedleAnimator() {
        needleAnimator = ValueAnimator.ofFloat(0f, 1f);
        needleAnimator.setDuration(1000); // not important; we loop
        needleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        needleAnimator.setRepeatMode(ValueAnimator.RESTART);
        needleAnimator.setInterpolator(null); // linear

        needleAnimator.addUpdateListener(animation -> {
            // Smoothly move toward the target (low-pass filter)
            float alpha = 0.1f; // smoothing factor
            currentNeedleRotation += alpha * (targetNeedleRotation - currentNeedleRotation);
            imageNeedle.setRotation(currentNeedleRotation);
        });

        needleAnimator.start();
    }


    private void animateGpsIcon() {
        if (gpsAnimator != null && gpsAnimator.isRunning()) {
            return; // Avoid restarting if already animating
        }

        int startColor = 0xFF222222;
        int endColor = 0xFF5F9529;   // green

        gpsAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
        gpsAnimator.setDuration(1000); // 1 second
        gpsAnimator.setRepeatMode(ValueAnimator.REVERSE);
        gpsAnimator.setRepeatCount(ValueAnimator.INFINITE);
        gpsAnimator.addUpdateListener(animation -> {
            int animatedColor = (int) animation.getAnimatedValue();
            imageGps.setColorFilter(animatedColor);
        });
        gpsAnimator.start();
    }

    public int getModeIndex() {
        return modeIndex;
    }

    public void setModeIndex(int index) {
        modeIndex = index;
        updateDisplay();
    }


    private static class ModeDescriptor {
        final String label1;
        final String label2;
        final Formatter formatter;

        ModeDescriptor(String label1, String label2, Formatter formatter) {
            this.label1 = label1;
            this.label2 = label2;
            this.formatter = formatter;
        }

        interface Formatter {
            String format(TripComputer tripComputer);
        }
    }

    @Override
    public void onGpsPulse(PhoneGps gps) {
        if (gps.isAlive()) {
            if (!tripComputer.isGpsLogging()) {
                animateGpsIcon();
            } else {
                if (gpsAnimator != null && gpsAnimator.isRunning()) {
                    gpsAnimator.cancel();
                }

                imageGps.setColorFilter(gray);
            }
        } else {
            if (gpsAnimator != null && gpsAnimator.isRunning()) {
                gpsAnimator.cancel();
            }

            imageGps.setColorFilter(orange);
        }
    }

    @Override
    public void onObdPulse(boolean isActive) {
        updateObdCel();
        updateDisplay();
    }

    public void updateObdCel() {
        int obdConnectionColor = tripComputer.mObdStudio.isAlive() ? orange : red;
        obdConnectionColor = tripComputer.mObdStudio.isReading() ? gray : obdConnectionColor;
        imageObd.setColorFilter(obdConnectionColor);
        imageObd.setTag(obdConnectionColor);
    }

    @Override
    public void onAfrPulse(boolean isActive) {
        updateAfrCel();

        if (!isActive) {
            targetNeedleRotation = calculateNeedleRotation(0);
        }

        updateDisplay();
    }

    public void updateAfrCel() {
        int afrConnectionColor = tripComputer.mSpartanStudio.isAlive() ? orange : red;
        afrConnectionColor = tripComputer.mSpartanStudio.isReading() ? gray : afrConnectionColor;
        imageAfr.setColorFilter(afrConnectionColor);
        imageAfr.setTag(afrConnectionColor);
    }

    @Override
    public void onObdValue(ObdReading reading) {
        updateDisplay();
    }
    @Override
    public void onAfrValue(Double afr) {
        updateDisplay();
    }
    @Override
    public boolean visibleInPip() {
        return true;
    }
    @Override
    public void enterPip() {
        super.enterPip();
    }
    @Override
    public void exitPip() {
        super.exitPip();
    }
}
