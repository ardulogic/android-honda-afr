#include <jni.h>
#include <android/log.h>
#include "fmod_studio.hpp"
#include "fmod.hpp"
#include "fmod_errors.h"

#define LOG_TAG "EngineSound"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define CHECK_ERR(result) if (result != FMOD_OK) { LOGE("FMOD error %d - %s", result, FMOD_ErrorString(result)); }

FMOD::Studio::System* studioSystem = nullptr;
FMOD::Studio::EventInstance* engineEvent = nullptr;
FMOD::Studio::Bank* bank = nullptr;

extern "C" FMOD_DSP_DESCRIPTION* FMODGetDSPDescription();

extern "C"
JNIEXPORT bool JNICALL
Java_com_hondaafr_Libs_EngineSound_EngineSoundPlayer_init(JNIEnv* env, jobject /*thiz*/) {
    LOGD("Calling FMOD::Studio::System::create()");
    FMOD_RESULT result = FMOD::Studio::System::create(&studioSystem);
    if (result != FMOD_OK) {
        LOGE("FMOD create failed: %s", FMOD_ErrorString(result));
        return JNI_FALSE;
    }

    LOGD("FMOD create success");

    result = studioSystem->initialize(1024, FMOD_STUDIO_INIT_NORMAL, FMOD_INIT_NORMAL, nullptr);
    if (result != FMOD_OK) {
        LOGE("FMOD initialize failed: %s", FMOD_ErrorString(result));
        return JNI_FALSE;
    }

    LOGD("FMOD initialize success");

    // Get the Core FMOD System
    FMOD::System* coreSystem = nullptr;
    result = studioSystem->getCoreSystem(&coreSystem);
    if (result != FMOD_OK || !coreSystem) {
        LOGE("Failed to get core system: %s", FMOD_ErrorString(result));
        return JNI_FALSE;
    }

    // Register custom DSP
    FMOD_DSP_DESCRIPTION* desc = FMODGetDSPDescription();
    if (desc) {
        result = coreSystem->registerDSP(desc, nullptr);  // now using coreSystem
        if (result != FMOD_OK) {
            LOGE("FMOD DSP registration failed: %s", FMOD_ErrorString(result));
        }
    } else {
        LOGE("FMOD DSP description is null");
    }

    FMOD::Studio::Bank* stringsBank = nullptr;

// Load Master.strings.bank first
    result = studioSystem->loadBankFile("file:///android_asset/Master.strings.bank", FMOD_STUDIO_LOAD_BANK_NORMAL, &stringsBank);
    if (result != FMOD_OK) {
        LOGE("Failed to load strings bank: %s", FMOD_ErrorString(result));
        return JNI_FALSE;
    }

    // Load bank
    result = studioSystem->loadBankFile("file:///android_asset/Master.bank", FMOD_STUDIO_LOAD_BANK_NORMAL, &bank);
    if (result != FMOD_OK) {
        LOGE("Failed to load bank: %s", FMOD_ErrorString(result));
        return JNI_FALSE;
    }

    LOGD("FMOD load bank success");

    return JNI_TRUE;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_hondaafr_Libs_EngineSound_EngineSoundPlayer_playEngine(JNIEnv* env, jobject /*thiz*/, jfloat rpm) {
    LOGD("FMOD: Playing engine at RPM: %f", rpm);

    if (!studioSystem) {
        LOGE("FMOD error: Studio system not initialized");
        return;
    }

    if (engineEvent) {
        FMOD_STUDIO_PLAYBACK_STATE state;
        engineEvent->getPlaybackState(&state);

        bool isPaused = false;
        engineEvent->getPaused(&isPaused);

        if (state == FMOD_STUDIO_PLAYBACK_PLAYING && !isPaused) {
            LOGD("FMOD: Engine already playing and not paused — skipping new instance");
            return;
        }

        if (isPaused) {
            LOGD("FMOD: Engine was paused — resuming playback");
            engineEvent->setPaused(false);  // Unpause
            engineEvent->setParameterByName("RPM", rpm);  // Update RPM
            return;
        }
    }

    FMOD::Studio::EventDescription* engineDesc = nullptr;
    FMOD_RESULT result = studioSystem->getEvent("event:/Engine", &engineDesc);
    if (result != FMOD_OK || !engineDesc) {
        LOGE("FMOD error: Failed to get engine event: %s", FMOD_ErrorString(result));
        return;
    }

    result = engineDesc->createInstance(&engineEvent);
    if (result != FMOD_OK || !engineEvent) {
        LOGE("FMOD error: Failed to create engine instance: %s", FMOD_ErrorString(result));
        return;
    }

    result = engineEvent->setParameterByName("RPM", rpm);
    if (result != FMOD_OK) {
        LOGE("FMOD error: Failed to set RPM: %s", FMOD_ErrorString(result));
    }

    result = engineEvent->start();
    if (result != FMOD_OK) {
        LOGE("FMOD error: Failed to start engine event: %s", FMOD_ErrorString(result));
        return;
    }

    LOGD("FMOD: Engine event started successfully");
}


extern "C"
JNIEXPORT void JNICALL
Java_com_hondaafr_Libs_EngineSound_EngineSoundPlayer_setRPM(JNIEnv* env, jobject /*thiz*/, jfloat rpm) {
    if (engineEvent) {
        engineEvent->setParameterByName("RPM", rpm);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hondaafr_Libs_EngineSound_EngineSoundPlayer_release(JNIEnv* env, jobject /*thiz*/) {
    if (engineEvent) engineEvent->release();
    if (studioSystem) {
        studioSystem->unloadAll();
        studioSystem->release();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hondaafr_Libs_EngineSound_EngineSoundPlayer_pauseEngine(JNIEnv* env, jobject /*thiz*/) {
    if (engineEvent) {
        engineEvent->setPaused(true);  // Resume with setPaused(false)
        LOGD("FMOD: Engine sound paused");
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hondaafr_Libs_EngineSound_EngineSoundPlayer_update(JNIEnv* env, jobject /*thiz*/) {
    if (studioSystem) {
        studioSystem->update();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hondaafr_Libs_EngineSound_EngineSoundPlayer_shutdown(JNIEnv* env, jobject /*thiz*/) {
    LOGD("FMOD: Shutting down");

    if (engineEvent) {
        engineEvent->stop(FMOD_STUDIO_STOP_IMMEDIATE);
        engineEvent->release();
        engineEvent = nullptr;
    }

    if (bank) {
        bank->unload();
        bank = nullptr;
    }

    // Unload all remaining banks (e.g., strings bank)
    if (studioSystem) {
        studioSystem->unloadAll();
        studioSystem->release();
        studioSystem = nullptr;
    }

    LOGD("FMOD: Shutdown complete");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hondaafr_Libs_EngineSound_EngineSoundPlayer_setTPS(JNIEnv *env, jclass clazz,
                                                            jint tps) {
    if (engineEvent) {
        engineEvent->setParameterByName("TPS", tps);
    }
}