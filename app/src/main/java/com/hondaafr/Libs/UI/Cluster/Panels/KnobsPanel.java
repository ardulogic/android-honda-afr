package com.hondaafr.Libs.UI.Cluster.Panels;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import com.hondaafr.Libs.Helpers.TripComputer.TotalStats;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class KnobsPanel extends Panel {

    private final TextView textKnobTotals;
    private final TextView textKnobTrip;
    private final TextView textKnobInst;
    private final ImageButton buttonTripKnob;
    private final ImageButton buttonTotalsKnob;
    private final ImageButton buttonInstKnob;

    Vibrator vibrator;

    public KnobsPanel(MainActivity mainActivity, TripComputer tripComputer, UiView view) {
        super(mainActivity, tripComputer, view);

        this.buttonTripKnob = rootView.findViewById(R.id.buttonClusterTripKnob);
        this.buttonTotalsKnob = rootView.findViewById(R.id.buttonClusterTotalsKnob);
        this.buttonInstKnob = rootView.findViewById(R.id.buttonClusterInstKnob);
        this.textKnobTotals = rootView.findViewById(R.id.textViewKnobTotals);
        this.textKnobInst = rootView.findViewById(R.id.textViewKnobInst);
        this.textKnobTrip = rootView.findViewById(R.id.textViewKnobTrip);
        this.vibrator = (Vibrator) mainActivity.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void onParentReady() {
        setupKnobAnimation(buttonTripKnob);
        setKnobOnShortClick(buttonTripKnob, 0, 2);
        setKnobOnLongClick(buttonTripKnob, tripComputer.tripStats);

        setupKnobAnimation(buttonTotalsKnob);
        setKnobOnShortClick(buttonTotalsKnob, 3, 5);
        setKnobOnLongClick(buttonTotalsKnob, tripComputer.totalStats);

        setupKnobAnimation(buttonInstKnob);
        setKnobOnShortClick(buttonInstKnob, 6, 8);

        updateKnobAppearance();
    }

    public void updateGauge() {
        getGaugePanel().updateDisplay();
    }

    public GaugePanel getGaugePanel() {
        return ((GaugePanel) parent.getPanel(GaugePanel.class));
    }

    private void setKnobOnLongClick(ImageButton b, TotalStats stats) {
        b.setOnLongClickListener(v -> {
            stats.reset(mainActivity);
            updateGauge();

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

                if (getGaugePanel().getModeIndex() < fromModeIndex || getGaugePanel().getModeIndex() > toModeIndex) {
                    getGaugePanel().setModeIndex(fromModeIndex);
                    Log.d("KnobPanel", "Setting mode (long click):" + fromModeIndex);
                    updateKnobAppearance();
                }

                return; // Don't handle short click
            }

            int newModeIndex = getGaugePanel().getModeIndex() + 1;
            if (newModeIndex < fromModeIndex || newModeIndex > toModeIndex) {
                getGaugePanel().setModeIndex(fromModeIndex);
            } else {
                getGaugePanel().setModeIndex(newModeIndex);
            }

            updateKnobAppearance();

            if (vibrator != null) {
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
            }
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

    @Override
    public int getContainerId() {
        return R.id.layoutClusterKnobs;
    }

    @Override
    public String getListenerId() {
        return "cluster_knobs";
    }

    @Override
    public void onNightModeChanged(boolean isNightMode) {
        updateKnobAppearance();
    }

    private void updateKnobAppearance() {
        if (getGaugePanel() != null) {
            int activeMode = getGaugePanel().getModeIndex();

            setKnobState(buttonTripKnob, textKnobTrip, isTripMode(activeMode));
            setKnobState(buttonTotalsKnob, textKnobTotals, isTotalsMode(activeMode));
            setKnobState(buttonInstKnob, textKnobInst, isInstMode(activeMode));
        }
    }

    private void setKnobState(ImageButton button, TextView label, boolean isActive) {
        int imageRes;
        int textColor;

        if (isActive) {
            imageRes = R.drawable.trip_knob_night_on;
            textColor = 0xFFC0B682;
        } else {
            imageRes = isNightMode ? R.drawable.trip_knob_night_off : R.drawable.trip_knob_day;
            textColor = isNightMode ? 0xFF7E7752 : 0xFF666666;
        }

        button.setImageResource(imageRes);
        label.setTextColor(textColor);
    }

    private boolean isTripMode(int mode) {
        return mode < 3;
    }

    private boolean isTotalsMode(int mode) {
        return mode >= 3 && mode < 6;
    }

    private boolean isInstMode(int mode) {
        return mode >= 6;
    }

}


