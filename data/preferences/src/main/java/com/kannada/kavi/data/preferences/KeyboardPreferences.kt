package com.kannada.kavi.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * KeyboardPreferences - Centralized Preference Management
 *
 * Manages all keyboard-related preferences and settings.
 * Uses SharedPreferences for simple key-value storage.
 *
 * PREFERENCE CATEGORIES:
 * =====================
 * 1. **Layout Preferences**
 *    - Current keyboard layout
 *    - Layout history
 *
 * 2. **Theme Preferences**
 *    - Selected theme ID
 *    - Custom theme JSON
 *    - Dynamic theme enabled
 *
 * 3. **Input Preferences**
 *    - Auto-capitalization
 *    - Sound on key press
 *    - Vibration feedback
 *
 * 4. **Advanced Features**
 *    - Predictive text
 *    - Swipe typing
 *    - Voice input
 */
@Singleton
class KeyboardPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Theme preferences
    fun setThemeId(themeId: String) {
        prefs.edit().putString(KEY_THEME_ID, themeId).apply()
    }

    fun getThemeId(): String? = prefs.getString(KEY_THEME_ID, null)

    fun setDynamicThemeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_THEME, enabled).apply()
    }

    fun isDynamicThemeEnabled(): Boolean = prefs.getBoolean(KEY_DYNAMIC_THEME, false)

    fun setCustomThemeJson(json: String?) {
        if (json == null) {
            prefs.edit().remove(KEY_CUSTOM_THEME).apply()
        } else {
            prefs.edit().putString(KEY_CUSTOM_THEME, json).apply()
        }
    }

    fun getCustomThemeJson(): String? = prefs.getString(KEY_CUSTOM_THEME, null)

    // Layout preferences
    fun setCurrentLayout(layoutId: String) {
        prefs.edit().putString(KEY_CURRENT_LAYOUT, layoutId).apply()
    }

    fun getCurrentLayout(): String = prefs.getString(KEY_CURRENT_LAYOUT, DEFAULT_LAYOUT) ?: DEFAULT_LAYOUT

    // Input preferences
    fun setAutoCapitalization(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CAPS, enabled).apply()
    }

    fun isAutoCapitalizationEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_CAPS, true)

    fun setKeyPressSound(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    fun isKeyPressSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND, false)

    fun setKeyPressVibration(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
    }

    fun isKeyPressVibrationEnabled(): Boolean = prefs.getBoolean(KEY_VIBRATION, true)

    // Advanced features
    fun setPredictiveText(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PREDICTIVE, enabled).apply()
    }

    fun isPredictiveTextEnabled(): Boolean = prefs.getBoolean(KEY_PREDICTIVE, true)

    fun setSwipeTyping(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE, enabled).apply()
    }

    fun isSwipeTypingEnabled(): Boolean = prefs.getBoolean(KEY_SWIPE, false)

    fun setVoiceInput(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE, enabled).apply()
    }

    fun isVoiceInputEnabled(): Boolean = prefs.getBoolean(KEY_VOICE, true)

    // Utility methods
    fun getString(key: String, defaultValue: String? = null): String? {
        return prefs.getString(key, defaultValue)
    }

    fun putString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return prefs.getLong(key, defaultValue)
    }

    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return prefs.getFloat(key, defaultValue)
    }

    fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "keyboard_preferences"

        // Theme keys
        private const val KEY_THEME_ID = "theme_id"
        private const val KEY_DYNAMIC_THEME = "dynamic_theme"
        private const val KEY_CUSTOM_THEME = "custom_theme_json"

        // Layout keys
        private const val KEY_CURRENT_LAYOUT = "current_layout"
        private const val DEFAULT_LAYOUT = "qwerty"

        // Input keys
        private const val KEY_AUTO_CAPS = "auto_capitalization"
        private const val KEY_SOUND = "key_press_sound"
        private const val KEY_VIBRATION = "key_press_vibration"

        // Advanced feature keys
        private const val KEY_PREDICTIVE = "predictive_text"
        private const val KEY_SWIPE = "swipe_typing"
        private const val KEY_VOICE = "voice_input"
    }
}