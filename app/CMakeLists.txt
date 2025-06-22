cmake_minimum_required(VERSION 3.4.1)

set(FMOD_SUFFIX "")
if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(FMOD_SUFFIX "L")
endif()

set(FMOD_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/Libs/fmodstudio/api)

add_library(fmod SHARED IMPORTED)
set_target_properties(fmod PROPERTIES IMPORTED_LOCATION
        ${FMOD_ROOT}/core/lib/${ANDROID_ABI}/libfmod${FMOD_SUFFIX}.so)

add_library(fmodstudio SHARED IMPORTED)
set_target_properties(fmodstudio PROPERTIES IMPORTED_LOCATION
        ${FMOD_ROOT}/studio/lib/${ANDROID_ABI}/libfmodstudio${FMOD_SUFFIX}.so)

add_library(fmod_audio SHARED src/main/cpp/engine_sound.cpp)

target_include_directories(fmod_audio PRIVATE
    ${FMOD_ROOT}/core/inc
    ${FMOD_ROOT}/studio/inc)

target_link_libraries(fmod_audio
    fmod
    fmodstudio
    log
    android)
