package com.hondaafr.Libs.UI.Scientific;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.EngineSound.EngineSound;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.Libs.UI.ScientificView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class SoundPanel {
    public final LinearLayout panel;
    private final TripComputer mTripComputer;
    private final MainActivity mainActivity;
    private final Context context;
    private final EngineSound mEngineSound;

    private boolean isEnabled = false;

    public SoundPanel(MainActivity mainActivity, TripComputer mTripComputer) {
        this.context = mainActivity;
        this.mainActivity = mainActivity;
        this.mTripComputer = mTripComputer;

        mEngineSound = new EngineSound();
        mEngineSound.init(mainActivity);

        this.panel = mainActivity.findViewById(R.id.layoutSound);
        this.mTripComputer.addListener("soundpanel", tcListener);

        ((SeekBar) mainActivity.findViewById(R.id.seekTps)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mEngineSound.setTargetTPS(seekBar.getProgress());

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        ((SeekBar) mainActivity.findViewById(R.id.seekRev)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mEngineSound.setTargetRpm((int) (6000 * ((float) seekBar.getProgress() / 100)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        ((SeekBar) mainActivity.findViewById(R.id.seekSmooth)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mEngineSound.setTargetSmoothness(seekBar.getProgress());

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
        mEngineSound.setState(isEnabled);

        panel.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    public void onResume() {
        mEngineSound.onResume(context);
    }

    public void onPause() {
        mEngineSound.onPause(context);
    }

    public void onDestroy() {
        mEngineSound.onDestroy();
    }

    private TripComputerListener tcListener = new TripComputerListener() {
        @Override
        public void onGpsUpdate(Double speed, double distanceIncrement) {

        }

        @Override
        public void onGpsPulse(PhoneGps gps) {

        }

        @Override
        public void onAfrPulse(boolean isActive) {

        }

        @Override
        public void onAfrTargetValue(double targetAfr) {

        }

        @Override
        public void onAfrValue(Double afr) {

        }

        @Override
        public void onObdPulse(boolean isActive) {

        }

        @Override
        public void onObdActivePidsChanged() {

        }

        @Override
        public void onObdValue(ObdReading reading) {
            final String name = reading.getMachineName();

            switch (name) {
                case "tps":
                    mEngineSound.setTargetTPS(((Double) reading.getValue()).intValue());
                    break;
                case "rpm":
                    mEngineSound.setTargetRpm((Integer) reading.getValue());
                    break;
            }
        }

        @Override
        public void onCalculationsUpdated() {

        }
    };

    public void toggle() {
        setEnabled(!isEnabled());
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
