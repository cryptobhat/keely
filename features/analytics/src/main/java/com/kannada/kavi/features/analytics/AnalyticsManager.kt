package com.kannada.kavi.features.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.data.repositories.AnalyticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Calendar

/**
 * AnalyticsManager - Firebase Analytics Integration
 * 
 * Handles all analytics tracking for the keyboard app:
 * - Event tracking (key presses, layout switches, suggestions, etc.)
 * - User properties (DAU/MAU, layout preferences, etc.)
 * - Screen tracking (settings screens, etc.)
 * 
 * Usage:
 * ```kotlin
 * val analytics = AnalyticsManager(context)
 * analytics.trackKeyPress("q")
 * analytics.trackLayoutSwitch("phonetic")
 * ```
 */
class AnalyticsManager private constructor(
    context: Context,
    private val analyticsRepository: AnalyticsRepository? = null
) {

    private val firebaseAnalytics: FirebaseAnalytics? by lazy {
        try {
            Firebase.analytics
        } catch (e: Exception) {
            android.util.Log.w("AnalyticsManager", "Firebase Analytics not available", e)
            null
        }
    }
    private val crashlytics: FirebaseCrashlytics? by lazy {
        try {
            Firebase.crashlytics
        } catch (e: Exception) {
            android.util.Log.w("AnalyticsManager", "Firebase Crashlytics not available", e)
            null
        }
    }
    private val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        @Volatile
        private var INSTANCE: AnalyticsManager? = null
        
        /**
         * Get singleton instance of AnalyticsManager
         */
        fun getInstance(context: Context, analyticsRepository: AnalyticsRepository? = null): AnalyticsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnalyticsManager(context.applicationContext, analyticsRepository).also { INSTANCE = it }
            }
        }
        
        // User property keys
        private const val USER_PROPERTY_LAST_ACTIVE_DATE = "last_active_date"
        private const val USER_PROPERTY_PREFERRED_LAYOUT = "preferred_layout"
        private const val USER_PROPERTY_THEME_MODE = "theme_mode"
        private const val USER_PROPERTY_DYNAMIC_COLORS_ENABLED = "dynamic_colors_enabled"
        
        // Event parameter keys
        private const val PARAM_KEY_TYPE = "key_type"
        private const val PARAM_KEY_CHARACTER = "key_character"
        private const val PARAM_LAYOUT_ID = "layout_id"
        private const val PARAM_LAYOUT_NAME = "layout_name"
        private const val PARAM_SUGGESTION_WORD = "suggestion_word"
        private const val PARAM_SUGGESTION_POSITION = "suggestion_position"
        private const val PARAM_THEME_ID = "theme_id"
        private const val PARAM_CLIPBOARD_ITEM_COUNT = "clipboard_item_count"
        private const val PARAM_VOICE_DURATION_MS = "voice_duration_ms"
    }
    
    init {
        // Set default user properties
        updateUserProperties()

        // Enable Crashlytics collection if available
        crashlytics?.apply {
            setCrashlyticsCollectionEnabled(true)
            setCustomKey("app_version", getAppVersion())
            setCustomKey("android_version", android.os.Build.VERSION.SDK_INT)
        }
    }
    
    /**
     * Track a key press event
     * 
     * @param keyType Type of key (character, delete, enter, space, etc.)
     * @param keyCharacter The character that was pressed (for character keys)
     */
    fun trackKeyPress(keyType: String, keyCharacter: String? = null) {
        val params = Bundle().apply {
            putString(PARAM_KEY_TYPE, keyType)
            keyCharacter?.let { putString(PARAM_KEY_CHARACTER, it) }
        }
        logEvent(Constants.Analytics.EVENT_KEY_PRESS, params)
    }
    
    /**
     * Track layout switch event
     * 
     * @param layoutId Layout identifier (phonetic, kavi, qwerty)
     * @param layoutName Display name of the layout
     */
    fun trackLayoutSwitch(layoutId: String, layoutName: String) {
        val params = Bundle().apply {
            putString(PARAM_LAYOUT_ID, layoutId)
            putString(PARAM_LAYOUT_NAME, layoutName)
            putString(Constants.Analytics.PROPERTY_LAYOUT_TYPE, layoutId)
        }
        logEvent(Constants.Analytics.EVENT_LAYOUT_SWITCH, params)
        
        // Update user property for preferred layout
        setUserProperty(USER_PROPERTY_PREFERRED_LAYOUT, layoutId)
    }
    
    /**
     * Track suggestion acceptance
     * 
     * @param suggestionWord The word that was selected
     * @param position Position of suggestion (0 = first, 1 = second, etc.)
     */
    fun trackSuggestionAccepted(suggestionWord: String, position: Int) {
        val params = Bundle().apply {
            putString(PARAM_SUGGESTION_WORD, suggestionWord)
            putInt(PARAM_SUGGESTION_POSITION, position)
            putInt(Constants.Analytics.PROPERTY_SUGGESTION_POSITION, position)
        }
        logEvent(Constants.Analytics.EVENT_SUGGESTION_ACCEPTED, params)
    }
    
    /**
     * Track theme change
     * 
     * @param themeId Theme identifier
     * @param isDark Whether dark theme is enabled
     * @param isDynamic Whether dynamic colors are enabled
     */
    fun trackThemeChanged(themeId: String, isDark: Boolean, isDynamic: Boolean) {
        val params = Bundle().apply {
            putString(PARAM_THEME_ID, themeId)
            putString(Constants.Analytics.PROPERTY_THEME_ID, themeId)
            putBoolean("is_dark", isDark)
            putBoolean("is_dynamic", isDynamic)
        }
        logEvent(Constants.Analytics.EVENT_THEME_CHANGED, params)
        
        // Update user properties
        setUserProperty(USER_PROPERTY_THEME_MODE, if (isDark) "dark" else "light")
        setUserProperty(USER_PROPERTY_DYNAMIC_COLORS_ENABLED, isDynamic.toString())
    }
    
    /**
     * Track clipboard usage
     * 
     * @param itemCount Number of items in clipboard
     */
    fun trackClipboardUsed(itemCount: Int) {
        val params = Bundle().apply {
            putInt(PARAM_CLIPBOARD_ITEM_COUNT, itemCount)
        }
        logEvent(Constants.Analytics.EVENT_CLIPBOARD_USED, params)
    }
    
    /**
     * Track voice input usage
     * 
     * @param durationMs Duration of voice input in milliseconds
     */
    fun trackVoiceInputUsed(durationMs: Long) {
        val params = Bundle().apply {
            putLong(PARAM_VOICE_DURATION_MS, durationMs)
            putLong(Constants.Analytics.PROPERTY_VOICE_DURATION, durationMs)
        }
        logEvent(Constants.Analytics.EVENT_VOICE_INPUT_USED, params)
    }
    
    /**
     * Track settings screen view
     *
     * @param screenName Name of the settings screen
     */
    fun trackSettingsScreenView(screenName: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "SettingsScreen")
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
    
    /**
     * Track keyboard session start
     * Called when keyboard is shown
     */
    fun trackKeyboardSessionStart() {
        logEvent("keyboard_session_start", Bundle())
        updateDailyActiveUser()
    }
    
    /**
     * Track keyboard session end
     * Called when keyboard is hidden
     * 
     * @param durationMs Session duration in milliseconds
     * @param keysPressed Number of keys pressed during session
     */
    fun trackKeyboardSessionEnd(durationMs: Long, keysPressed: Int) {
        val params = Bundle().apply {
            putLong("session_duration_ms", durationMs)
            putInt("keys_pressed", keysPressed)
        }
        logEvent("keyboard_session_end", params)
    }
    
    /**
     * Track error event (non-fatal)
     * 
     * @param errorType Type of error
     * @param errorMessage Error message
     * @param exception Optional exception for detailed tracking
     */
    fun trackError(errorType: String, errorMessage: String, exception: Throwable? = null) {
        // Log to Firebase Analytics
        val params = Bundle().apply {
            putString("error_type", errorType)
            putString("error_message", errorMessage)
            exception?.let {
                putString("error_class", it.javaClass.simpleName)
                putString("error_stack", getStackTrace(it))
            }
        }
        logEvent("error_occurred", params)
        
        // Log to Crashlytics as non-fatal
        crashlytics?.apply {
            if (exception != null) {
                recordException(exception)
                log("Non-fatal error: $errorType - $errorMessage")
            } else {
                log("Error: $errorType - $errorMessage")
            }
        }
    }
    
    /**
     * Record a crash/exception
     * 
     * @param exception The exception that caused the crash
     * @param additionalInfo Additional context information
     */
    fun recordCrash(exception: Throwable, additionalInfo: Map<String, String>? = null) {
        crashlytics?.apply {
            // Set additional context
            additionalInfo?.forEach { (key, value) ->
                setCustomKey(key, value)
            }

            // Record the exception
            recordException(exception)
        }

        // Also track as error event
        trackError(
            errorType = "crash",
            errorMessage = exception.message ?: "Unknown crash",
            exception = exception
        )
    }
    
    /**
     * Log a message to Crashlytics
     *
     * @param message Log message
     */
    fun log(message: String) {
        crashlytics?.log(message)
    }

    /**
     * Set user identifier for crash reporting
     *
     * @param userId User identifier (can be anonymized)
     */
    fun setUserId(userId: String) {
        crashlytics?.setUserId(userId)
        firebaseAnalytics?.setUserId(userId)
    }

    /**
     * Set custom key for crash reporting
     *
     * @param key Key name
     * @param value Key value
     */
    fun setCustomKey(key: String, value: String) {
        crashlytics?.setCustomKey(key, value)
    }

    /**
     * Set custom key for crash reporting (Int)
     */
    fun setCustomKey(key: String, value: Int) {
        crashlytics?.setCustomKey(key, value)
    }

    /**
     * Set custom key for crash reporting (Boolean)
     */
    fun setCustomKey(key: String, value: Boolean) {
        crashlytics?.setCustomKey(key, value)
    }

    /**
     * Set custom key for crash reporting (Float)
     */
    fun setCustomKey(key: String, value: Float) {
        crashlytics?.setCustomKey(key, value)
    }

    /**
     * Set custom key for crash reporting (Double)
     */
    fun setCustomKey(key: String, value: Double) {
        crashlytics?.setCustomKey(key, value)
    }
    
    /**
     * Get stack trace as string
     */
    private fun getStackTrace(exception: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        exception.printStackTrace(pw)
        return sw.toString()
    }
    
    /**
     * Get app version name
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Track feature usage
     * 
     * @param featureName Name of the feature
     * @param featureValue Value or state of the feature
     */
    fun trackFeatureUsage(featureName: String, featureValue: String? = null) {
        val params = Bundle().apply {
            putString("feature_name", featureName)
            featureValue?.let { putString("feature_value", it) }
        }
        logEvent("feature_used", params)
    }
    
    /**
     * Update daily active user (DAU) tracking
     * Called when keyboard is used
     */
    private fun updateDailyActiveUser() {
        scope.launch {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis.toString()
            
            val lastActive = getSharedPreference(USER_PROPERTY_LAST_ACTIVE_DATE)
            
            if (lastActive != today) {
                // New day - user is active today
                setSharedPreference(USER_PROPERTY_LAST_ACTIVE_DATE, today)
                setUserProperty(USER_PROPERTY_LAST_ACTIVE_DATE, today)
                
                // Track DAU event
                logEvent("daily_active_user", Bundle())
            }
        }
    }
    
    /**
     * Update all user properties
     */
    fun updateUserProperties() {
        // This will be called from various places to keep user properties up to date
        // For now, just track last active date
        updateDailyActiveUser()
    }
    
    /**
     * Set a user property
     */
    private fun setUserProperty(name: String, value: String) {
        firebaseAnalytics?.setUserProperty(name, value)
    }
    
    /**
     * Log an event to Firebase Analytics
     */
    private fun logEvent(eventName: String, params: Bundle) {
        // Convert Bundle to Map for repository
        val properties = mutableMapOf<String, Any>()
        params.keySet().forEach { key ->
            params.get(key)?.let { value ->
                properties[key] = value
            }
        }

        // Queue event in local database first (for offline support)
        scope.launch {
            try {
                val eventId = analyticsRepository?.queueEvent(eventName, properties)

                // Try to sync to Firebase immediately
                try {
                    firebaseAnalytics?.logEvent(eventName, params)

                    // Mark as synced if successful
                    if (eventId != null) {
                        analyticsRepository?.markAsSynced(eventId)
                    }
                } catch (e: Exception) {
                    // Failed to sync - event remains in queue for later retry
                    android.util.Log.w("AnalyticsManager", "Failed to sync event to Firebase: $eventName", e)

                    // If repository not available, try Firebase anyway
                    if (analyticsRepository == null) {
                        firebaseAnalytics?.logEvent(eventName, params)
                    }
                }
            } catch (e: Exception) {
                // If queueing fails, try Firebase directly
                android.util.Log.w("AnalyticsManager", "Failed to queue event: $eventName", e)
                try {
                    firebaseAnalytics?.logEvent(eventName, params)
                } catch (fe: Exception) {
                    android.util.Log.w("AnalyticsManager", "Failed to log event to Firebase: $eventName", fe)
                }
            }
        }
    }
    
    /**
     * Get shared preference value
     */
    private fun getSharedPreference(key: String): String? {
        return appContext.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)
            .getString(key, null)
    }
    
    /**
     * Set shared preference value
     */
    private fun setSharedPreference(key: String, value: String) {
        appContext.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(key, value)
            .apply()
    }
}

