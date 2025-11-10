/**
 * Core Common Module - Build Configuration
 *
 * This module contains shared utilities, extensions, and constants used across all other modules.
 * It's like a toolbox that every other part of the app can use.
 *
 * Think of it like this: If you're building with LEGO blocks, this module contains
 * the basic pieces that you'll use everywhere - like connectors, base plates, etc.
 */

plugins {
    // This makes it an Android library (not an app)
    alias(libs.plugins.android.library)

    // Enable Kotlin language
    alias(libs.plugins.kotlin.android)

    // Enable Parcelize for easy data passing between screens
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.kannada.kavi.core.common"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // AndroidX Core - Basic Android utilities
    implementation(libs.androidx.core.ktx)

    // Coroutines - For background tasks and async operations
    // Think of coroutines as workers that can do tasks without blocking the main screen
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Gson - For converting data to/from JSON format
    // JSON is like a universal language for data
    implementation(libs.gson)

    // AndroidX Security - For encrypted storage and secure data handling
    // Provides EncryptedSharedPreferences and Android Keystore integration
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Testing libraries
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
