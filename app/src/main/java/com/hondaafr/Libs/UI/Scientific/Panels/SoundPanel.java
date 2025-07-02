package com.hondaafr.Libs.UI.Scientific.Panels;

import android.content.Context;
import android.widget.SeekBar;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.EngineSound.EngineSound;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class SoundPanel  extends Panel {
    private final EngineSound mEngineSound;

    private boolean isEnabled = false;

    @Override
    public int getContainerId() {
        return R.id.layoutSound;
    }

    @Override
    public String getListenerId() {
        return "sound_panel";
    }

    public SoundPanel(MainActivity mainActivity, TripComputer tripCcomputer, UiView parentView) {
        super(mainActivity, tripCcomputer, parentView);

        mEngineSound = new EngineSound();
        mEngineSound.init(mainActivity);

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

    public boolean isEnabled() {
        return isEnabled;
    }

    private void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
        mEngineSound.setState(isEnabled);

        setVisibility(isEnabled);
    }

    public void toggle() {
        setEnabled(!isEnabled());
    }

    @Override
    public void onResume(Context context) {
        mEngineSound.onResume(context);
    }

    @Override
    public void onPause(Context context) {
        mEngineSound.onPause(context);
    }

    @Override
    public void onDestroy(Context context) {
        mEngineSound.onDestroy();
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

}
