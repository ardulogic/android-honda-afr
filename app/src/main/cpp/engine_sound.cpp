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

extern "C"
JNIEXPORT void JNICALL
Java_com_hondaafr_Libs_EngineSound_EngineSoundPlayer_init(JNIEnv* env, jobject /*thiz*/) {
    FMOD::Studio::System::create(&studioSystem);
    studioSystem->initialize(1024, FMOD_STUDIO_INIT_NORMAL, FMOD_INIT_NORMAL, nullptr);

    studioSystem->loadBankFile("file:///android_asset/bmw_1m.bank", FMOD_STUDIO_LOAD_BANK_NORMAL, &bank);

    FMOD::Studio::EventDescription* engineDesc = nullptr;
    studioSystem->getEvent("event:/Engine", &engineDesc);

    if (engineDesc) {
        engineDesc->createInstance(&engineEvent);
        engineEvent->start();
    } else {
        LOGE("Engine event not found");
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_hondaafr_Libs_EngineSound_EngineSoundPlayer_test(JNIEnv* env, jobject /*thiz*/) {
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

    result = studioSystem->loadBankFile("file:///android_asset/dodge_challenger.bank", FMOD_STUDIO_LOAD_BANK_NORMAL, &bank);
    if (result != FMOD_OK) {
        LOGE("Failed to load bank: %s", FMOD_ErrorString(result));
        return JNI_FALSE;
    }

    LOGD("FMOD load bank success");

    // Log all DSPs for events in this bank
    FMOD::Studio::EventDescription* events[128];
    int count = 0;
    result = bank->getEventList(events, 128, &count);
    if (result != FMOD_OK) {
        LOGE("Failed to get event list: %s", FMOD_ErrorString(result));
        return JNI_FALSE;
    }

    LOGD("Found %d events in bank", count);

    for (int i = 0; i < count; ++i) {
        char path[256];
        int retrieved = 0;
        events[i]->getPath(path, sizeof(path), &retrieved);
        LOGD("Inspecting event: %s", path);

        FMOD::Studio::EventInstance* instance = nullptr;
        result = events[i]->createInstance(&instance);
        if (result != FMOD_OK || !instance) {
            LOGE("  Failed to create instance: %s", FMOD_ErrorString(result));
            continue;
        }

        instance->start();
        instance->setPaused(true);

        FMOD::ChannelGroup* channelGroup = nullptr;
        result = instance->getChannelGroup(&channelGroup);
        if (result != FMOD_OK || !channelGroup) {
            LOGE("  No channel group found");
            instance->release();
            continue;
        }

        int numDSPs = 0;
        channelGroup->getNumDSPs(&numDSPs);
        LOGD("  DSP count: %d", numDSPs);

        for (int d = 0; d < numDSPs; ++d) {
            FMOD::DSP* dsp = nullptr;
            channelGroup->getDSP(d, &dsp);
            if (!dsp) continue;

            char dspName[256] = {};
            dsp->getInfo(dspName, nullptr, nullptr, nullptr, nullptr);
            LOGD("    DSP[%d]: %s", d, dspName);
        }

        instance->stop(FMOD_STUDIO_STOP_IMMEDIATE);
        instance->release();
    }

    return JNI_TRUE;
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
