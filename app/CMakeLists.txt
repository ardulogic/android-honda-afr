cmake_minimum_required(VERSION 3.4.1)

# Detect FMOD Debug suffix
set(FMOD_SUFFIX "")
if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(FMOD_SUFFIX "L")
endif()

# FMOD root (adjust path if needed)
set(FMOD_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/Libs/fmodstudio/api)

# Include your project's native source directory
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp)

# Include FMOD headers from your plugin copy
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/fmod_plugin)  # <--- plugin folder

# Declare FMOD dynamic libraries
add_library(fmod SHARED IMPORTED)
set_target_properties(fmod PROPERTIES IMPORTED_LOCATION
        ${FMOD_ROOT}/core/lib/${ANDROID_ABI}/libfmod${FMOD_SUFFIX}.so)

add_library(fmodstudio SHARED IMPORTED)
set_target_properties(fmodstudio PROPERTIES IMPORTED_LOCATION
        ${FMOD_ROOT}/studio/lib/${ANDROID_ABI}/libfmodstudio${FMOD_SUFFIX}.so)

# Build your custom DSP plugin statically
add_library(fmod_distance_filter STATIC
        src/main/cpp/fmod_plugin/fmod_distance_filter.cpp)  # <--- plugin source

# Your main native code (replace with your actual .cpp files)
add_library(fmod_audio SHARED
        src/main/cpp/engine_sound.cpp)

# Add FMOD include directories to your main code
target_include_directories(fmod_audio PRIVATE
        ${FMOD_ROOT}/core/inc
        ${FMOD_ROOT}/studio/inc)

# Link everything
target_link_libraries(fmod_audio
        fmod
        fmodstudio
        fmod_distance_filter  # <--- link the plugin statically
        log
        android)
