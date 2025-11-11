/**
 * Main App Module - Build Configuration
 *
 * This is the main entry point of the Kavi Kannada Keyboard app.
 * It brings together all feature modules and configures the application.
 *
 * Think of this as the conductor of an orchestra - it coordinates all the different
 * instruments (modules) to create a beautiful symphony (the keyboard app)!
 */

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.compose.compiler)
    // TODO: Add these back later
    // alias(libs.plugins.ksp)
    // alias(libs.plugins.hilt)
    // alias(libs.plugins.google.services)
    // alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.kannada.kavi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kannada.kavi"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Enable vector drawables support
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Load Bhashini API configuration from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        val bhashiniApiKey = localProperties.getProperty("BHASHINI_API_KEY") ?: ""
        val bhashiniUserId = localProperties.getProperty("BHASHINI_USER_ID") ?: ""
        val bhashiniInferenceKey = localProperties.getProperty("BHASHINI_INFERENCE_KEY") ?: ""

        buildConfigField("String", "BHASHINI_API_KEY", "\"$bhashiniApiKey\"")
        buildConfigField("String", "BHASHINI_USER_ID", "\"$bhashiniUserId\"")
        buildConfigField("String", "BHASHINI_INFERENCE_KEY", "\"$bhashiniInferenceKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    androidResources {
        noCompress += listOf("tflite", "bin")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core modules - The foundation of the keyboard
    implementation(project(":core:common"))
    implementation(project(":core:keyboard-engine"))
    implementation(project(":core:input-method-service"))
    implementation(project(":core:layout-manager"))

    // Feature modules - Individual features that are implemented
    implementation(project(":features:suggestion-engine"))
    implementation(project(":features:clipboard"))
    implementation(project(":features:converter"))
    implementation(project(":features:themes"))
    implementation(project(":features:settings"))
    implementation(project(":features:analytics"))

    // Data modules
    implementation(project(":data:preferences"))
    implementation(project(":data:repositories"))
    implementation(project(":data:database"))

    // UI modules - User interface components
    implementation(project(":ui:keyboard-view"))

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.ktx)

    // Jetpack Compose for Settings UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // TODO: Add Hilt back later
    // implementation(libs.hilt.android)
    // ksp(libs.hilt.android.compiler)
    // implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime.ktx)

    // Firebase Analytics and Crashlytics
    // Note: Requires google-services.json in app/ directory
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
