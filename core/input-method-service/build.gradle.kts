/**
 * Build configuration for core/input-method-service
 * Generated automatically - Module namespace: com.kannada.kavi.core.ime
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    // TODO: Add these back when ready for dependency injection
    // alias(libs.plugins.ksp)
    // alias(libs.plugins.hilt)
}

android {
    namespace = "com.kannada.kavi.core.ime"
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
    // Core dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Module-specific dependencies
    implementation(project(":core:common"))
    implementation(project(":core:keyboard-engine"))
    implementation(project(":core:layout-manager"))
    implementation(project(":features:suggestion-engine"))
    implementation(project(":features:clipboard"))
    implementation(project(":features:converter"))
    implementation(project(":ui:keyboard-view"))

    // TODO: Add Hilt back when ready
    // implementation(libs.hilt.android)
    // ksp(libs.hilt.android.compiler)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
