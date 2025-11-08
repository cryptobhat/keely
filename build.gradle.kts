// Top-level build file where you can add configuration options common to all sub-projects/modules.
// This file configures plugins that will be used across all modules in the Kavi Kannada Keyboard project

plugins {
    // Android application plugin - used for the main app module
    alias(libs.plugins.android.application) apply false

    // Android library plugin - used for feature modules (layouts, suggestions, clipboard, etc.)
    alias(libs.plugins.android.library) apply false

    // Kotlin Android plugin - enables Kotlin language support in Android modules
    alias(libs.plugins.kotlin.android) apply false

    // KAPT (Kotlin Annotation Processing Tool) - used for Room, Hilt code generation
    alias(libs.plugins.kotlin.kapt) apply false

    // KSP (Kotlin Symbol Processing) - faster alternative to KAPT for Room and Hilt
    alias(libs.plugins.ksp) apply false

    // Hilt Dependency Injection - provides automatic dependency management
    alias(libs.plugins.hilt) apply false

    // Parcelize - automatically creates Parcelable implementations for data classes
    alias(libs.plugins.kotlin.parcelize) apply false

    // Compose Compiler - enables Jetpack Compose with Material You theming
    alias(libs.plugins.compose.compiler) apply false

    // Google Services - required for Firebase Analytics and Crashlytics
    alias(libs.plugins.google.services) apply false

    // Firebase Crashlytics - automatic crash reporting and analytics
    alias(libs.plugins.firebase.crashlytics) apply false
}