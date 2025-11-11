# ============================================================================
# Kavi Kannada Keyboard - ProGuard Rules
# ============================================================================
# These rules configure code obfuscation and optimization for release builds.
#
# SECURITY GOALS:
# ===============
# 1. Obfuscate code to prevent reverse engineering
# 2. Remove unused code to reduce attack surface
# 3. Protect sensitive logic and algorithms
# 4. Keep necessary classes for reflection and frameworks
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
# ============================================================================

# ============================================================================
# GENERAL CONFIGURATION
# ============================================================================

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable

# Keep annotations for runtime processing
-keepattributes *Annotation*

# Keep generic signatures for reflection
-keepattributes Signature

# Keep exception classes for proper error handling
-keep public class * extends java.lang.Exception

# Optimize and obfuscate aggressively
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# ============================================================================
# ANDROID FRAMEWORK
# ============================================================================

# Keep Android components (Activities, Services, etc.)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View

# Keep InputMethodService (our keyboard service)
-keep public class * extends android.inputmethodservice.InputMethodService
-keep class com.kannada.kavi.core.ime.KaviInputMethodService { *; }

# Keep Application class
-keep class com.kannada.kavi.KaviApplication { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep View constructors for XML inflation
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================================
# SECURITY - PROTECT SENSITIVE CODE
# ============================================================================

# Keep security utilities (but obfuscate internals)
-keep class com.kannada.kavi.core.common.security.SecureStorage {
    public <methods>;
}
-keep class com.kannada.kavi.core.common.security.PermissionManager {
    public <methods>;
}
-keep class com.kannada.kavi.core.common.security.InputValidator {
    public <methods>;
}

# Obfuscate encryption implementation details
-keepclassmembers class com.kannada.kavi.core.common.security.SecureStorage {
    private <methods>;
    private <fields>;
}

# ============================================================================
# FIREBASE
# ============================================================================

# Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.android.gms.measurement.** { *; }
-dontwarn com.google.firebase.analytics.**

# Keep Analytics classes
-keep class com.kannada.kavi.features.analytics.** { *; }
-dontwarn com.kannada.kavi.features.analytics.**

# ============================================================================
# ROOM DATABASE
# ============================================================================

# Keep database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep database entities and DAOs
-keep class com.kannada.kavi.data.database.** { *; }

# Keep Room annotations
-keepattributes *Annotation*
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# ============================================================================
# KOTLIN
# ============================================================================

# Kotlin metadata
-keep class kotlin.Metadata { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ============================================================================
# JETPACK COMPOSE
# ============================================================================

# Keep Compose classes for settings UI
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }

# ============================================================================
# GSON / JSON PARSING
# ============================================================================

# Keep generic signature for GSON
-keepattributes Signature

# Keep GSON classes
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Keep JSON model classes
-keep class com.kannada.kavi.**.models.** { *; }

# Keep TypeAdapters for GSON
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ============================================================================
# ANDROIDX SECURITY
# ============================================================================

# Keep EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ============================================================================
# CUSTOM CLASSES
# ============================================================================

# Keep keyboard layouts (JSON parsing)
-keep class com.kannada.kavi.core.layout.models.** { *; }

# Keep clipboard models
-keep class com.kannada.kavi.features.clipboard.models.** { *; }

# Keep theme models
-keep class com.kannada.kavi.features.themes.** { *; }

# Keep settings activity for launcher
-keep class com.kannada.kavi.features.settings.SettingsActivity { *; }

# ============================================================================
# OBFUSCATION DICTIONARY (Enhanced Security)
# ============================================================================

# Use custom dictionary to make reverse engineering harder
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt

# ============================================================================
# REMOVE LOGGING (Security - Prevent info leakage)
# ============================================================================

# Remove all Log.d, Log.v, Log.i calls in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep Log.w and Log.e for production error tracking
# -assumenosideeffects class android.util.Log {
#     public static *** w(...);
#     public static *** e(...);
# }

# Remove println statements
-assumenosideeffects class java.io.PrintStream {
    public void println(%);
    public void println(**);
}

# ============================================================================
# OPTIMIZATION
# ============================================================================

# Allow aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Merge similar classes
-allowaccessmodification
-repackageclasses ''

# ============================================================================
# WARNINGS TO IGNORE
# ============================================================================

# Ignore warnings from third-party libraries
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ============================================================================
# DEBUG BUILD RULES
# ============================================================================
# These rules only apply to debug builds (automatically included)
# Keep everything for easier debugging

# ============================================================================
# VERIFICATION
# ============================================================================
# After building with ProGuard, verify:
# 1. APK size reduced (should be ~30-50% smaller)
# 2. Decompiled code is obfuscated (use jadx or similar)
# 3. App still functions correctly
# 4. Crash reports show correct line numbers
# 5. No ClassNotFoundException or MethodNotFoundException
# ============================================================================