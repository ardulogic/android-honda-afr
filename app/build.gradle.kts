plugins {
    alias(libs.plugins.androidApplication)
}

val fmodRoot = "Libs/fmodstudio"

android {
    namespace = "com.hondaafr"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.hondaafr"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                arguments += listOf(
                        "-DANDROID_PLATFORM=android-21",
                        "-DANDROID_TOOLCHAIN=clang",
                        "-DANDROID_STL=c++_static"
                )

                // ADD THIS to ensure debug symbols are included
                cFlags += "-g"
            }
        }
    }


    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("String[]", "FMOD_LIBS", "{ \"fmodL\", \"fmodstudioL\" }")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets {
        getByName("main") {
            // jniLibs for FMOD .so files
            jniLibs.srcDirs(fmodRoot + "/core/lib", fmodRoot + "/studio/lib")

            // assets for your banks
            assets.srcDirs("src/main/assets")

            sourceSets["main"].jni.srcDirs("src/main/cpp")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets.getByName("main") {
        jniLibs.srcDirs("src/main/jniLibs")
    }

    buildToolsVersion = "34.0.0"

    namespace = "com.hondaafr"
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.opencsv:opencsv:5.5.2")
    implementation("com.luckycatlabs:SunriseSunsetCalculator:1.2")
    implementation(files("libs/fmod.jar"))
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}