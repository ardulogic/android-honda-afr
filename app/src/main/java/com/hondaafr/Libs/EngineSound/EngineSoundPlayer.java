package com.hondaafr.Libs.EngineSound;

import android.content.Context;

import org.fmod.FMOD;

public class EngineSoundPlayer {

    static {
        System.loadLibrary("fmod");
        System.loadLibrary("fmodstudio");
        System.loadLibrary("fmod_audio"); // <- your engine_sound.cpp output
    }

    public static void load(Context context) {
        FMOD.init(context);  // Initialize FMOD
        init();
    }

    public static native void init();
    public static native void shutdown();
    public static native void update();
    public static native void setRPM(float rpm);

    public static native boolean playEngine(float rpm);
    public static native void pauseEngine();

    public static native void release();

    public static void close() {
        shutdown();
        FMOD.close();  // Shutdown FMOD
    }

    public static native void setTPS(int currentMap);
}
