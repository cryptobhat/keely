/**
 * Main App Module - Build Configuration
 *
 * This is the main entry point of the Kavi Kannada Keyboard app.
 * It brings together all feature modules and configures the application.
 *
 * Think of this as the conductor of an orchestra - it coordinates all the different
 * instruments (modules) to create a beautiful symphony (the keyboard app)!
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
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
    implementation(project(":core:gesture-detector"))

    // Feature modules - Individual features that can be enabled/disabled
    implementation(project(":features:layouts"))
    implementation(project(":features:suggestion-engine"))
    implementation(project(":features:clipboard"))
    implementation(project(":features:voice"))
    implementation(project(":features:converter"))
    implementation(project(":features:themes"))
    implementation(project(":features:settings"))
    implementation(project(":features:analytics"))
    implementation(project(":features:notifications"))

    // Data modules - Handle all data operations
    implementation(project(":data:database"))
    implementation(project(":data:repositories"))
    implementation(project(":data:preferences"))
    implementation(project(":data:cache"))

    // UI modules - User interface components
    implementation(project(":ui:keyboard-view"))
    implementation(project(":ui:popup-views"))
    implementation(project(":ui:settings-ui"))
    implementation(project(":ui:theme-preview"))

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
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Dependency Injection - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime.ktx)

    // Firebase
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