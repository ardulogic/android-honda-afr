package com.hondaafr.Libs.UI.Cluster.Sound;

import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

import com.hondaafr.R;

public class BeeperPanel extends Panel {

    private static final String PREF = "cluster_beeper_prefs";
    private static final String KEY_BEEP_AFR_ENABLED = "beep_afr";
    private static final String KEY_BEEP_AFR_VALUE = "beep_afr_value";
    private static final String KEY_BEEP_LPH_ENABLED = "beep_lph";
    private static final String KEY_BEEP_LPH_VALUE = "beep_lph_value";
    private static final String KEY_BEEP_LP100_ENABLED = "beep_lp100";
    private static final String KEY_BEEP_LP100_VALUE = "beep_lp100_value";
    private long lastBeepTime = 0;
    private static final long BEEP_INTERVAL_MS = 150;
    private SoundPool soundPool;
    private int beepSoundId;
    private boolean soundLoaded = false;
    private boolean beepOnRichAfr = false;
    private float beepOnRichAfrValue = 12.0f;
    private boolean beepOnLph = false;
    private float beepOnLphValue = 6.0f;
    private boolean beepOnLp100 = false;
    private float beepOnLp100Value = 6.0f;

    @Override
    public int getContainerId() {
        return R.id.imageClusterConfigure;
    }

    @Override
    public String getListenerId() {
        return "beeper_panel";
    }

    public BeeperPanel(MainActivity activity, TripComputer tripComputer, AfrComputer afrComputer, UiView parent) {
        super(activity, tripComputer, afrComputer, parent);

        rootView.findViewById(getContainerId()).setOnClickListener(v -> showConfigDialog());

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .setMaxStreams(1)
                .build();

        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) {
                soundLoaded = true;
            }
        });

        beepSoundId = soundPool.load(activity, R.raw.beep, 1);
        Log.d("Beeper Panel", "Loading...");

        load();
        testBeepingLoop();
    }
    public void testBeepingLoop() {
        new Thread(() -> {
            for (int i = 0; i < 100000; i++) { // Loop 100 times
                beepIfNecessary();
                try {
                    Thread.sleep(20); // wait 20 milliseconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }).start();
    }

    public void load() {
        SharedPreferences sp = mainActivity.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        beepOnRichAfr = sp.getBoolean(KEY_BEEP_AFR_ENABLED, true);
        beepOnRichAfrValue = sp.getFloat(KEY_BEEP_AFR_VALUE, 6.0f);

        beepOnLph = sp.getBoolean(KEY_BEEP_LPH_ENABLED, true);
        beepOnLphValue = sp.getFloat(KEY_BEEP_LPH_VALUE, 5.0f);

        beepOnLp100 = sp.getBoolean(KEY_BEEP_LP100_ENABLED, true);
        beepOnLp100Value = sp.getFloat(KEY_BEEP_LP100_VALUE, 5.0f);
    }

    public void save() {
        SharedPreferences.Editor ed = mainActivity.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
        ed.putBoolean(KEY_BEEP_AFR_ENABLED, beepOnRichAfr);
        ed.putFloat(KEY_BEEP_AFR_VALUE, beepOnRichAfrValue);

        ed.putBoolean(KEY_BEEP_LPH_ENABLED, beepOnLph);
        ed.putFloat(KEY_BEEP_LPH_VALUE, beepOnLphValue);

        ed.putBoolean(KEY_BEEP_LP100_ENABLED, beepOnLp100);
        ed.putFloat(KEY_BEEP_LP100_VALUE, beepOnLp100Value);
        ed.apply();
    }

    public boolean shouldBeep() {
        boolean beep = false;

        if (tripComputer.mSpartanStudio.lastSensorAfr > 0) {
            if (beepOnRichAfr && (tripComputer.mSpartanStudio.lastSensorAfr < beepOnRichAfrValue)) {
                beep = true;
            }
        }

        if (tripComputer.getSpeed() > 30) {
            if (beepOnLp100 && (tripComputer.instStats.getLp100kmAvg() > beepOnLp100Value)) {
                beep = true;
            }
        }

        if (beepOnLph) {
            if (tripComputer.instStats.getLphAvg() > beepOnLphValue) {
                beep = true;
            }
        }

        return beep;
    }

    public void beepIfNecessary() {
        long currentTime = System.currentTimeMillis();

        if (shouldBeep() && (currentTime - lastBeepTime) >= BEEP_INTERVAL_MS) {
            if (soundLoaded) {
                soundPool.play(beepSoundId, 1f, 1f, 1, 0, 1f);
            }
            lastBeepTime = currentTime;
        }
    }

    public void showConfigDialog() {
        ClusterConfigurationDialogFragment dialog = ClusterConfigurationDialogFragment.newInstance(
                beepOnRichAfr, beepOnRichAfrValue,   // AFR setting
                beepOnLp100, beepOnLp100Value,   // Lp100 setting
                beepOnLph, beepOnLphValue,   // Lph setting
                (beepOnAfr, afr, beepOnLp100, lp100, beepOnLph, lph) -> {
                    this.beepOnRichAfr = beepOnAfr;
                    this.beepOnRichAfrValue = afr;

                    this.beepOnLph = beepOnLph;
                    this.beepOnLphValue = lph;

                    this.beepOnLp100 = beepOnLp100;
                    this.beepOnLp100Value = lp100;

                    // Save or apply the values here
                    save();
                }
        );

        dialog.show(mainActivity.getSupportFragmentManager(), "beeper_config_dialog");
        Log.d("BeeperPanel", "Opening dialog...");
    }

    @Override
    public void onCalculationsUpdated() {
        if (isParentVisible()) {
            beepIfNecessary();
        }
    }

    @Override
    public void onAfrPulse(boolean isActive) {

    }
}

