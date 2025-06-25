package com.hondaafr.Libs.Helpers;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.hondaafr.Libs.Devices.Phone.PhoneLightSensor;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.MainActivity;
import com.hondaafr.R;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import android.os.Handler;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public class Cluster {

    private final MainActivity mainActivity;
    private final ImageView imageNeedle;
    private final TextView textClusterLcd;
    private final TextView textClusterLcdMode1;
    private final TextView textClusterLcdMode2;
    private final ImageView imageRichFuel;
    private final ImageView imageObd;
    private final PhoneLightSensor lightSensor;
    private final ImageView imageGauge;
    private final ImageButton buttonTripKnob;

    public static int MODE_TRIP_KM = 0;
    public static int MODE_TRIP_L = 1;
    public static int MODE_TRIP_L100 = 2;
    public static int MODE_TOTAL_KM = 3;
    public static int MODE_TOTAL_L = 4;
    public static int MODE_TOTAL_L100 = 5;

    private final TripComputer mTripComputer;
    private final ImageView imageLcd;
    private boolean isNightMode = false;
    private float currentNeedleRotation = 0f;
    private float targetNeedleRotation = 0f;
    private ValueAnimator needleAnimator;
    private float lastNeedleRotation = Float.NaN;

    public int mode = MODE_TRIP_KM;


    public Cluster(MainActivity mainActivity, TripComputer tripComputer) {
        this.mainActivity = mainActivity;
        this.imageNeedle = mainActivity.findViewById(R.id.imageClusterNeedle);
        this.imageGauge = mainActivity.findViewById(R.id.imageClusterGauge);
        this.imageLcd = mainActivity.findViewById(R.id.imageClusterLcd);
        this.textClusterLcd = mainActivity.findViewById(R.id.textClusterLcd);
        this.textClusterLcdMode1 = mainActivity.findViewById(R.id.textClusterLcdMode1);
        this.textClusterLcdMode2 = mainActivity.findViewById(R.id.textClusterLcdMode2);
        this.imageRichFuel = mainActivity.findViewById(R.id.imageClusterRichFuel);
        this.imageObd= mainActivity.findViewById(R.id.imageClusterObd);
        this.buttonTripKnob = mainActivity.findViewById(R.id.buttonClusterTripKnob);

        this.lightSensor = new PhoneLightSensor(mainActivity, intensity -> {
            Log.d("Light", String.valueOf(intensity));

            boolean lowLight = intensity < 15;
            boolean afterSunset = false;

            if (tripComputer.gps.getSunsetTime() != null) {
                afterSunset = LocalTime.now().isAfter(tripComputer.gps.getSunsetTime());
            }

            setNightMode(lowLight || afterSunset);
        });


        this.mTripComputer = tripComputer;
        setupTripKnobAnimation(buttonTripKnob);

        startNeedleAnimator();

        onDataUpdated();
    }


    public void setVisibility(boolean visible) {
        mainActivity.findViewById(R.id.layoutCluster).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTripKnobAnimation(ImageButton button) {
        button.setOnClickListener(v -> {
            if (mode == 5) {
                mode = 0;
            } else {
                mode += 1;
            }

            onDataUpdated();
        });

        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(0.9f)
                            .scaleY(0.9f)
                            .setDuration(100)
                            .start();
                    break;

                case MotionEvent.ACTION_UP:
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(new OvershootInterpolator())
                            .start();

                    // Properly trigger click behavior
                    v.performClick();
                    break;

                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(new OvershootInterpolator())
                            .start();
                    break;
            }
            return true; // we handled the touch
        });
    }

    private void setNightMode(boolean enabled) {
        if (this.isNightMode == enabled) {
            return; // No change, skip updating images
        }

        this.isNightMode = enabled;

        if (enabled) {
            this.imageNeedle.setImageResource(R.drawable.fuel_gauge_needle_night);
            this.imageGauge.setImageResource(R.drawable.fuel_gauge_night);
            this.imageLcd.setImageResource(R.drawable.fuel_gauge_lcd_night);
        } else {
            this.imageNeedle.setImageResource(R.drawable.fuel_gauge_needle_day);
            this.imageGauge.setImageResource(R.drawable.fuel_gauge_day);
            this.imageLcd.setImageResource(R.drawable.fuel_gauge_lcd_day);
        }
    }

    @SuppressLint("DefaultLocale")
    private final ModeDescriptor[] modeDescriptors = new ModeDescriptor[]{
            new ModeDescriptor("TRIP", "", tc -> String.format("%.1f", tc.getTripGpsDistance())),                  // MODE_TRIP_KM
            new ModeDescriptor("TRIP", "", tc -> String.format("%.2f L", tc.getTripLitres())),                     // MODE_TRIP_L
            new ModeDescriptor("TRIP", "/100", tc -> String.format("%.2f L", tc.getTripLitersPer100km())),         // MODE_TRIP_L100
            new ModeDescriptor("TOTAL", "", tc -> String.format("%05.0f", tc.getTotalDistanceKm())),               // MODE_TOTAL_KM
            new ModeDescriptor("TOTAL", "", tc -> String.format("%.2f L", tc.getTotalLiters())),                   // MODE_TOTAL_L
            new ModeDescriptor("TOTAL", "/100", tc -> String.format("%.2f L", tc.getTotalLitersPer100km()))        // MODE_TOTAL_L100
    };

    public void onDataUpdated() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(this::onDataUpdated);
            return;
        }

        if (mode >= 0 && mode < modeDescriptors.length) {
            ModeDescriptor descriptor = modeDescriptors[mode];
            textClusterLcdMode1.setText(descriptor.label1);
            textClusterLcdMode2.setText(descriptor.label2);
            textClusterLcd.setText(descriptor.formatter.format(mTripComputer));
        }

        // Animate needle rotation
        float targetRotation = calculateNeedleRotation(mTripComputer.getTripCurrentLitersPerHour());
        this.targetNeedleRotation = targetRotation;

        // Animate rich fuel indicator color (optional: fade transition)
        int richFuelIconColor = mTripComputer.afrIsRich() ? 0xFFFFA500 : 0xFF222222;

        // Use a ValueAnimator to animate the color if desired:
        imageRichFuel.setColorFilter(richFuelIconColor); // quick, or see optional below

        int obdConnectionColor = mTripComputer.isObdAlive() ? 0xFF222222 : 0xFFFFA500;
        imageObd.setColorFilter(obdConnectionColor); // quick, or see optional below
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

    private void animateNeedle(float targetRotation) {
        if (Float.isNaN(lastNeedleRotation) || Math.abs(targetRotation - lastNeedleRotation) > 0.1f) {
            if (needleAnimator != null && needleAnimator.isRunning()) {
                needleAnimator.cancel();
            }

            needleAnimator = ObjectAnimator.ofFloat(imageNeedle, "rotation", imageNeedle.getRotation(), targetRotation);
            needleAnimator.setDuration(100);
            needleAnimator.start();
            lastNeedleRotation = targetRotation;
        }
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
}
