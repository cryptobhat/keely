"""
Module Generator Script for Kavi Kannada Keyboard

This script automatically creates all the module directories and build.gradle.kts files.
It saves time by creating the repetitive boilerplate code for all modules.

Think of this like a cookie cutter - instead of making each cookie by hand,
we use a cutter to make many cookies quickly with the same shape!
"""

import os
from pathlib import Path

# Define all modules with their dependencies
MODULES = {
    # Core modules
    "core/gesture-detector": {
        "namespace": "com.kannada.kavi.core.gesture",
        "dependencies": [":core:common"]
    },
    "core/layout-manager": {
        "namespace": "com.kannada.kavi.core.layout",
        "dependencies": [":core:common", "gson"]
    },
    "core/keyboard-engine": {
        "namespace": "com.kannada.kavi.core.keyboard",
        "dependencies": [":core:common", ":core:gesture-detector", ":core:layout-manager"]
    },
    "core/input-method-service": {
        "namespace": "com.kannada.kavi.core.ime",
        "dependencies": [":core:common", ":core:keyboard-engine", "hilt"]
    },

    # Feature modules
    "features/layouts": {
        "namespace": "com.kannada.kavi.features.layouts",
        "dependencies": [":core:common", ":core:layout-manager"]
    },
    "features/suggestion-engine": {
        "namespace": "com.kannada.kavi.features.suggestions",
        "dependencies": [":core:common", ":data:database", ":data:repositories", "room", "tensorflow"]
    },
    "features/clipboard": {
        "namespace": "com.kannada.kavi.features.clipboard",
        "dependencies": [":core:common", ":data:database", ":data:repositories"]
    },
    "features/voice": {
        "namespace": "com.kannada.kavi.features.voice",
        "dependencies": [":core:common", "retrofit", "okhttp"]
    },
    "features/converter": {
        "namespace": "com.kannada.kavi.features.converter",
        "dependencies": [":core:common"]
    },
    "features/themes": {
        "namespace": "com.kannada.kavi.features.themes",
        "dependencies": [":core:common", ":data:preferences", "compose"]
    },
    "features/settings": {
        "namespace": "com.kannada.kavi.features.settings",
        "dependencies": [":core:common", ":data:preferences", "compose", "hilt"]
    },
    "features/analytics": {
        "namespace": "com.kannada.kavi.features.analytics",
        "dependencies": [":core:common", "firebase"]
    },
    "features/notifications": {
        "namespace": "com.kannada.kavi.features.notifications",
        "dependencies": [":core:common"]
    },

    # Data modules
    "data/database": {
        "namespace": "com.kannada.kavi.data.database",
        "dependencies": [":core:common", "room", "hilt"]
    },
    "data/repositories": {
        "namespace": "com.kannada.kavi.data.repositories",
        "dependencies": [":core:common", ":data:database", ":data:preferences", "hilt"]
    },
    "data/preferences": {
        "namespace": "com.kannada.kavi.data.preferences",
        "dependencies": [":core:common", "datastore"]
    },
    "data/cache": {
        "namespace": "com.kannada.kavi.data.cache",
        "dependencies": [":core:common"]
    },

    # UI modules
    "ui/keyboard-view": {
        "namespace": "com.kannada.kavi.ui.keyboardview",
        "dependencies": [":core:common", ":core:keyboard-engine"]
    },
    "ui/popup-views": {
        "namespace": "com.kannada.kavi.ui.popupviews",
        "dependencies": [":core:common"]
    },
    "ui/settings-ui": {
        "namespace": "com.kannada.kavi.ui.settingsui",
        "dependencies": [":core:common", ":features:settings", "compose"]
    },
    "ui/theme-preview": {
        "namespace": "com.kannada.kavi.ui.themepreview",
        "dependencies": [":core:common", ":features:themes", "compose"]
    }
}

def get_dependency_string(deps):
    """Convert dependency list to implementation statements"""
    result = []

    for dep in deps:
        if dep.startswith(":"):
            # Project dependency
            result.append(f'    implementation(project("{dep}"))')
        elif dep == "hilt":
            result.append('    implementation(libs.hilt.android)')
            result.append('    ksp(libs.hilt.android.compiler)')
        elif dep == "room":
            result.append('    implementation(libs.androidx.room.runtime)')
            result.append('    implementation(libs.androidx.room.ktx)')
            result.append('    ksp(libs.androidx.room.compiler)')
        elif dep == "compose":
            result.append('    implementation(platform(libs.androidx.compose.bom))')
            result.append('    implementation(libs.androidx.compose.ui)')
            result.append('    implementation(libs.androidx.compose.material3)')
        elif dep == "firebase":
            result.append('    implementation(platform(libs.firebase.bom))')
            result.append('    implementation(libs.firebase.analytics)')
            result.append('    implementation(libs.firebase.crashlytics)')
        elif dep == "retrofit":
            result.append('    implementation(libs.retrofit)')
            result.append('    implementation(libs.retrofit.converter.gson)')
        elif dep == "okhttp":
            result.append('    implementation(libs.okhttp)')
            result.append('    implementation(libs.okhttp.logging.interceptor)')
        elif dep == "tensorflow":
            result.append('    implementation(libs.tensorflow.lite)')
            result.append('    implementation(libs.tensorflow.lite.support)')
        elif dep == "gson":
            result.append('    implementation(libs.gson)')
        elif dep == "datastore":
            result.append('    implementation(libs.androidx.datastore.preferences)')

    return "\n".join(result)

def create_build_file(module_path, namespace, dependencies):
    """Create build.gradle.kts for a module"""

    # Determine if Hilt or KSP is needed
    needs_hilt = "hilt" in dependencies or "room" in dependencies
    needs_ksp = needs_hilt or "room" in dependencies
    needs_compose = "compose" in dependencies

    plugins_list = [
        '    alias(libs.plugins.android.library)',
        '    alias(libs.plugins.kotlin.android)',
        '    alias(libs.plugins.kotlin.parcelize)'
    ]

    if needs_ksp:
        plugins_list.append('    alias(libs.plugins.ksp)')
    if needs_hilt:
        plugins_list.append('    alias(libs.plugins.hilt)')
    if needs_compose:
        plugins_list.append('    alias(libs.plugins.compose.compiler)')

    plugins = "\n".join(plugins_list)

    deps_string = get_dependency_string(dependencies)

    build_content = f'''/**
 * Build configuration for {module_path}
 * Generated automatically - Module namespace: {namespace}
 */

plugins {{
{plugins}
}}

android {{
    namespace = "{namespace}"
    compileSdk = 36

    defaultConfig {{
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }}

    buildTypes {{
        release {{
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }}
    }}

    compileOptions {{
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }}

    kotlinOptions {{
        jvmTarget = "11"
    }}

    {"buildFeatures { compose = true }" if needs_compose else ""}
}}

dependencies {{
    // Core dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Module-specific dependencies
{deps_string}

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}}
'''

    build_file_path = Path(module_path) / "build.gradle.kts"
    with open(build_file_path, 'w', encoding='utf-8') as f:
        f.write(build_content)

    print(f"✓ Created build file for {module_path}")

def create_manifest(module_path, namespace):
    """Create AndroidManifest.xml for a module"""
    src_main = Path(module_path) / "src" / "main"
    src_main.mkdir(parents=True, exist_ok=True)

    manifest_content = f'''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Module: {namespace} -->
</manifest>
'''

    manifest_path = src_main / "AndroidManifest.xml"
    with open(manifest_path, 'w', encoding='utf-8') as f:
        f.write(manifest_content)

    print(f"✓ Created manifest for {module_path}")

def create_source_dirs(module_path, namespace):
    """Create source directories"""
    java_path = namespace.replace(".", "/")

    # Create main source directory
    main_src = Path(module_path) / "src" / "main" / "java" / java_path
    main_src.mkdir(parents=True, exist_ok=True)

    # Create test directory
    test_src = Path(module_path) / "src" / "test" / "java" / java_path
    test_src.mkdir(parents=True, exist_ok=True)

    # Create androidTest directory
    android_test_src = Path(module_path) / "src" / "androidTest" / "java" / java_path
    android_test_src.mkdir(parents=True, exist_ok=True)

    print(f"✓ Created source directories for {module_path}")

def create_proguard_rules(module_path):
    """Create proguard-rules.pro"""
    rules_content = '''# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
'''

    rules_path = Path(module_path) / "proguard-rules.pro"
    with open(rules_path, 'w', encoding='utf-8') as f:
        f.write(rules_content)

def main():
    """Main function to generate all modules"""
    print("🚀 Generating Kavi Kannada Keyboard Modules...\n")

    for module_path, config in MODULES.items():
        print(f"\n📦 Creating module: {module_path}")

        # Create module directory
        Path(module_path).mkdir(parents=True, exist_ok=True)

        # Create build file
        create_build_file(module_path, config["namespace"], config["dependencies"])

        # Create manifest
        create_manifest(module_path, config["namespace"])

        # Create source directories
        create_source_dirs(module_path, config["namespace"])

        # Create ProGuard rules
        create_proguard_rules(module_path)

    print("\n\n✅ All modules created successfully!")
    print("📝 Next steps:")
    print("   1. Sync Gradle in Android Studio")
    print("   2. Start implementing core functionality")
    print("   3. Run tests to ensure everything compiles")

if __name__ == "__main__":
    main()
