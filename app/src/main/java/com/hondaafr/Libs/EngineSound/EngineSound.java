package com.hondaafr.Libs.EngineSound;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.os.HandlerCompat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EngineSound {

    private ScheduledExecutorService executor;

    private volatile boolean enabled = false;

    private volatile boolean initalized = false;

    private volatile int targetRpm = 0;
    private volatile int currentRpm = 0;
    private volatile int currentTps = 0;

    private final long tickIntervalMs = 16;
    private int maxDeltaPerTick = 40;
    private int minIncrement = 20;

    private int smoothnessFactor = 30;

    private double rpmMultiplier = 1;

    private final Handler mainHandler = HandlerCompat.createAsync(Looper.getMainLooper());

    private final Runnable updateTask = () -> {
        if (initalized) {
            if (enabled) {
                int delta = targetRpm - currentRpm;
                if (delta != 0) {
                    int step = Math.min(Math.abs(delta), maxDeltaPerTick);
                    currentRpm += (delta > 0) ? step : -step;

                    EngineSoundPlayer.setRPM((float) ((float) currentRpm * rpmMultiplier));
                    EngineSoundPlayer.setTPS(currentTps);
                }
            }

            // Ensure FMOD.update() happens on main thread
            mainHandler.post(EngineSoundPlayer::update);
            Log.d("EngineSound", "Tick");
        }
    };

    public synchronized void start() {
        enabled = true;

        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(updateTask, 0, tickIntervalMs, TimeUnit.MILLISECONDS);
        }

        EngineSoundPlayer.playEngine(800);
    }

    public synchronized void stop() {
        enabled = false;

        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }

        EngineSoundPlayer.pauseEngine();
        // It needs to call update to actually pause engine
        mainHandler.post(EngineSoundPlayer::update);
    }

    public synchronized void setTargetRpm(int rpm) {
        targetRpm = rpm;

        int delta = Math.abs(targetRpm - currentRpm);

        // Desired transition time increases with smoothness
        long baseTransitionTime = 10; // in ms, adjust to suit
        long transitionTime = baseTransitionTime + (smoothnessFactor * 5L); // higher factor → longer duration

        int estimatedTicks = (int) (transitionTime / tickIntervalMs);

        if (delta > 0 && estimatedTicks > 0) {
            maxDeltaPerTick = Math.max(delta / estimatedTicks, minIncrement);
        } else {
            maxDeltaPerTick = delta;
        }
    }

    public void setTargetTPS(Integer value) {
        currentTps = value;
        EngineSoundPlayer.setTPS(value);
    }

    public int getCurrentRpm() {
        return currentRpm;
    }

    public int getTargetRpm() {
        return targetRpm;
    }

    public boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }

    public void init(Context context) {
        EngineSoundPlayer.load(context);

        initalized = true;
    }

    public void onResume(Context c) {
        if (enabled && isRunning()) {
            EngineSoundPlayer.playEngine(targetRpm);
        }
    }

    public void onPause(Context c) {
//        EngineSoundPlayer.shutdown();
        if (enabled && isRunning()){
            EngineSoundPlayer.pauseEngine();
        }
    }

    public void onDestroy() {
        stop();
        EngineSoundPlayer.shutdown();
    }


    public void setTargetSmoothness(int progress) {
        smoothnessFactor = progress;
    }

    public void setRpmMultiplier(double multiplier) {
        rpmMultiplier = multiplier;
    }

    public void setState(Boolean started) {
        if (started) {
            start();
        } else {
            stop();
        }
    }
}
