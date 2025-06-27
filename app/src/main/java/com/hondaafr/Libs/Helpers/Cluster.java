package com.hondaafr.Libs.Helpers;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.hondaafr.Libs.Devices.Phone.PhoneLightSensor;
import com.hondaafr.Libs.Helpers.TripComputer.TotalStats;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

import android.os.Handler;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.time.LocalTime;

public class Cluster {

    private final MainActivity mainActivity;
    private final ImageView imageNeedle;
    private final TextView textClusterLcd;
    private final TextView textClusterLcdMode1;
    private final TextView textClusterLcdMode2;
    private final TextView textKnobTotals;
    private final TextView textKnobTrip;
    private final TextView textKnobInst;
    private final ImageView imageRichFuel;
    private final ImageView imageObd;
    private final ImageView imageAfr;

    private final ImageView imageGps;
    private final PhoneLightSensor lightSensor;
    private final ImageView imageGauge;
    private final ImageButton buttonTripKnob;
    private final ImageButton buttonTotalsKnob;
    private final ImageButton buttonInstKnob;

    private final TripComputer mTripComputer;
    private final ImageView imageLcd;
    private final ConstraintLayout layoutCluster;
    private boolean isNightMode = false;
    private float currentNeedleRotation = 0f;
    private float targetNeedleRotation = 0f;
    private ValueAnimator needleAnimator;

    private ValueAnimator gpsAnimator; // Add this as a class field

    private float lastNeedleRotation = Float.NaN;

    public int mode = 0;
    private final Handler supervisorHandler = new Handler(Looper.getMainLooper());

    Vibrator vibrator;

    int gray = 0xFF222222;
    int red = 0xFFC82B28;
    int orange = 0xFFFFA500;

    public Cluster(MainActivity mainActivity, TripComputer tripComputer) {
        this.mainActivity = mainActivity;
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
        this.buttonTripKnob = mainActivity.findViewById(R.id.buttonClusterTripKnob);
        this.buttonTotalsKnob = mainActivity.findViewById(R.id.buttonClusterTotalsKnob);
        this.buttonInstKnob = mainActivity.findViewById(R.id.buttonClusterInstKnob);
        this.textKnobTotals = mainActivity.findViewById(R.id.textViewKnobTotals);
        this.textKnobInst = mainActivity.findViewById(R.id.textViewKnobInst);
        this.textKnobTrip = mainActivity.findViewById(R.id.textViewKnobTrip);
        this.layoutCluster = mainActivity.findViewById(R.id.layoutCluster);
        this.vibrator = (Vibrator) mainActivity.getSystemService(Context.VIBRATOR_SERVICE);

        this.lightSensor = new PhoneLightSensor(mainActivity, intensity -> {
            Log.d("Light", String.valueOf(intensity));

            boolean lowLight = intensity < 15;
            boolean afterSunset = false;
            boolean beforeSunrise = false;

            LocalTime now = LocalTime.now();

            if (tripComputer.gps.getSunsetTime() != null && tripComputer.gps.getSunriseTime() != null) {
                afterSunset = now.isAfter(tripComputer.gps.getSunsetTime());
                beforeSunrise = now.isBefore(tripComputer.gps.getSunriseTime());
            }

            boolean isNight = lowLight || afterSunset || beforeSunrise;
            setNightMode(isNight);
        });

        this.mTripComputer = tripComputer;
        setupKnobAnimation(buttonTripKnob);
        setKnobOnShortClick(buttonTripKnob, 0, 2);
        setKnobOnLongClick(buttonTripKnob, mTripComputer.tripStats);

        setupKnobAnimation(buttonTotalsKnob);
        setKnobOnShortClick(buttonTotalsKnob, 3, 5);
        setKnobOnLongClick(buttonTotalsKnob, mTripComputer.totalStats);


        setupKnobAnimation(buttonInstKnob);
        setKnobOnShortClick(buttonInstKnob, 6, 8);

        layoutCluster.setOnClickListener(v -> toggleSystemUI());

        startNeedleAnimator();
        onDataUpdated();
        startSupervisor();
    }

    private void toggleSystemUI() {
        View decorView = mainActivity.getWindow().getDecorView();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController insetsController = mainActivity.getWindow().getInsetsController();
            if (insetsController != null) {
                boolean isVisible = mainActivity.getWindow()
                        .getDecorView()
                        .getRootWindowInsets()
                        .isVisible(WindowInsets.Type.navigationBars());

                if (isVisible) {
                    insetsController.hide(WindowInsets.Type.navigationBars());
                    insetsController.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                } else {
                    insetsController.show(WindowInsets.Type.navigationBars());
                }
            }
        } else {
            // For Android 10 and below
            int uiOptions = decorView.getSystemUiVisibility();
            boolean isVisible = (uiOptions & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;

            if (isVisible) {
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            } else {
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
    }

    private void startSupervisor() {
        supervisorHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mTripComputer.ensureObdAndAfrAreAlive();
                supervisorHandler.postDelayed(this, 1000); // Reschedule
            }
        }, 1000);
    }

    public void stopSupervisor() {
        supervisorHandler.removeCallbacksAndMessages(null);
    }

    public void setVisibility(boolean visible) {
        mainActivity.findViewById(R.id.layoutCluster).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setKnobOnLongClick(ImageButton b, TotalStats stats) {
        b.setOnLongClickListener(v -> {
            stats.reset(mainActivity);
            onDataUpdated();

            if (vibrator != null) {
                vibrator.vibrate(VibrationEffect.createOneShot(550, VibrationEffect.DEFAULT_AMPLITUDE)); // short click
            }

            v.setTag(R.id.was_long_clicked, true);
            return false;
        });
    }

    private void setKnobOnShortClick(ImageButton b, int fromModeIndex, int toModeIndex) {
        b.setOnClickListener(v -> {
            Boolean longClicked = (Boolean) v.getTag(R.id.was_long_clicked);
            if (longClicked != null && longClicked) {
                v.setTag(R.id.was_long_clicked, false); // Reset the flag

                if (mode < fromModeIndex || mode > toModeIndex) {
                    mode = fromModeIndex;
                    lightUpActiveKnob();
                    onDataUpdated();
                }

                return; // Don't handle short click
            }

            mode += 1;
            if (mode < fromModeIndex || mode > toModeIndex) {
                mode = fromModeIndex;
            }

            if (vibrator != null) {
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
            }

            lightUpActiveKnob();
            onDataUpdated();
        });
    }
    @SuppressLint("ClickableViewAccessibility")
    private void setupKnobAnimation(ImageButton button) {
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
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(new OvershootInterpolator())
                            .start();
                    break;
            }

            // Let the system handle long click detection
            return false; // <--- Important: let event propagate
        });
    }

    private void setNightMode(boolean enabled) {
        if (isNightMode == enabled) {
            return; // No change, skip updating images
        }

        isNightMode = enabled;

        if (enabled) {
            imageNeedle.setImageResource(R.drawable.fuel_gauge_needle_night);
            imageGauge.setImageResource(R.drawable.fuel_gauge_night);
            imageLcd.setImageResource(R.drawable.fuel_gauge_lcd_night);
            buttonTripKnob.setImageResource(R.drawable.trip_knob_night_off);
            buttonTotalsKnob.setImageResource(R.drawable.trip_knob_night_off);
            buttonInstKnob.setImageResource(R.drawable.trip_knob_night_off);
            layoutCluster.setBackgroundColor(Color.parseColor("#000000"));

            imageObd.setImageResource(R.drawable.obd_night);
            imageAfr.setImageResource(R.drawable.afr_night);
            imageGps.setImageResource(R.drawable.gps_night);
            imageRichFuel.setImageResource(R.drawable.fuel_gauge_rich_fuel_night);
        } else {
            imageNeedle.setImageResource(R.drawable.fuel_gauge_needle_day);
            imageGauge.setImageResource(R.drawable.fuel_gauge_day);
            imageLcd.setImageResource(R.drawable.fuel_gauge_lcd_day);
            buttonTripKnob.setImageResource(R.drawable.trip_knob_day);
            buttonTotalsKnob.setImageResource(R.drawable.trip_knob_day);
            buttonInstKnob.setImageResource(R.drawable.trip_knob_day);
            layoutCluster.setBackgroundColor(Color.parseColor("#0E0E0E"));

            imageObd.setImageResource(R.drawable.obd);
            imageAfr.setImageResource(R.drawable.afr);
            imageGps.setImageResource(R.drawable.gps);
            imageRichFuel.setImageResource(R.drawable.fuel_gauge_rich_fuel);
        }

        lightUpActiveKnob();
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

    private void lightUpActiveKnob() {
        if (isNightMode) {
            buttonTripKnob.setImageResource(R.drawable.trip_knob_night_off);
            buttonTotalsKnob.setImageResource(R.drawable.trip_knob_night_off);
            buttonInstKnob.setImageResource(R.drawable.trip_knob_night_off);

            textKnobTotals.setTextColor(0xFF7E7752);
            textKnobTrip.setTextColor(0xFF7E7752);
            textKnobInst.setTextColor(0xFF7E7752);

            if (mode < 3) {
                buttonTripKnob.setImageResource(R.drawable.trip_knob_night_on);
                textKnobTrip.setTextColor(0xFFC0B682);
            } else if (mode < 6) {
                buttonTotalsKnob.setImageResource(R.drawable.trip_knob_night_on);
                textKnobTotals.setTextColor(0xFFC0B682);
            } else {
                buttonInstKnob.setImageResource(R.drawable.trip_knob_night_on);
                textKnobInst.setTextColor(0xFFC0B682);
            }
        }
    }

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
        float targetRotation = calculateNeedleRotation(mTripComputer.instStats.getLphAvg());
        this.targetNeedleRotation = targetRotation;

        int richFuelIconColor = mTripComputer.afrIsRich() && mTripComputer.mSpartanStudio.lastSensorAfr > 0 ? orange : gray;
        imageRichFuel.setColorFilter(richFuelIconColor);
        imageRichFuel.setTag(richFuelIconColor);

        int obdConnectionColor = mTripComputer.mObdStudio.isAlive() ? red : orange;
        obdConnectionColor = mTripComputer.mObdStudio.isReading() ? gray : obdConnectionColor;
        imageObd.setColorFilter(obdConnectionColor);
        imageObd.setTag(obdConnectionColor);
        imageObd.setTag(obdConnectionColor);

        int afrConnectionColor = mTripComputer.mSpartanStudio.isAlive() ? red : orange;
        afrConnectionColor = mTripComputer.mSpartanStudio.isReading() ? gray : afrConnectionColor;
        imageAfr.setColorFilter(afrConnectionColor);
        imageAfr.setTag(afrConnectionColor);

        boolean gpsAlive = mTripComputer.gps.isAlive();
        boolean isLogging = mTripComputer.isGpsLogging();

        if (gpsAlive) {
            if (!isLogging) {
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

    public void onPause(Context context) {
        stopSupervisor();
    }

    public void onResume(Context context) {
        startSupervisor();
    }
}
