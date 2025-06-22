package com.hondaafr.Libs.EngineSound;

import org.fmod.FMOD;

public class EngineSoundPlayer {

    static {
        System.loadLibrary("fmod");
        System.loadLibrary("fmodstudio");
        System.loadLibrary("fmod_audio"); // <- your engine_sound.cpp output
    }

    public static native void init();
//    public static native void shutdown();
//    public static native void update();
    public static native void setRPM(float rpm);

    public static native boolean test();
}
