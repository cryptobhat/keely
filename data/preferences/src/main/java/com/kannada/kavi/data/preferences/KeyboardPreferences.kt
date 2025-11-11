package com.kannada.kavi.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.kannada.kavi.core.common.Constants

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
class KeyboardPreferences(context: Context) {
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

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun isDarkModeEnabled(): Boolean? = if (prefs.contains(KEY_DARK_MODE)) {
        prefs.getBoolean(KEY_DARK_MODE, false)
    } else {
        null // Return null if not set (use system default)
    }

    fun setCustomThemeJson(json: String?) {
        if (json == null) {
            prefs.edit().remove(KEY_CUSTOM_THEME).apply()
        } else {
            prefs.edit().putString(KEY_CUSTOM_THEME, json).apply()
        }
    }

    fun getCustomThemeJson(): String? = prefs.getString(KEY_CUSTOM_THEME, null)

    fun setThemeVariant(variant: String) {
        prefs.edit().putString(KEY_THEME_VARIANT, variant).apply()
    }

    fun getThemeVariant(): String = prefs.getString(KEY_THEME_VARIANT, THEME_VARIANT_DEFAULT) ?: THEME_VARIANT_DEFAULT

    // Layout preferences
    fun setCurrentLayout(layoutId: String) {
        prefs.edit().putString(KEY_CURRENT_LAYOUT, layoutId).apply()
    }

    fun getCurrentLayout(): String = prefs.getString(KEY_CURRENT_LAYOUT, DEFAULT_LAYOUT) ?: DEFAULT_LAYOUT

    // Multiple layout support
    fun getEnabledLayouts(): Set<String> {
        // Get saved enabled layouts or default to all available layouts
        val saved = prefs.getStringSet(KEY_ENABLED_LAYOUTS, null)
        return if (saved != null && saved.isNotEmpty()) {
            saved
        } else {
            // Default to all layouts enabled if not set
            setOf("qwerty", "phonetic", "kavi")
        }
    }

    fun setEnabledLayouts(layouts: Set<String>) {
        prefs.edit().putStringSet(KEY_ENABLED_LAYOUTS, layouts).apply()
    }

    fun isLayoutEnabled(layoutId: String): Boolean {
        return getEnabledLayouts().contains(layoutId)
    }

    fun toggleLayoutEnabled(layoutId: String) {
        val currentLayouts = getEnabledLayouts().toMutableSet()
        if (currentLayouts.contains(layoutId)) {
            // Don't allow disabling all layouts
            if (currentLayouts.size > 1) {
                currentLayouts.remove(layoutId)
                // If current layout is being disabled, switch to first enabled layout
                if (getCurrentLayout() == layoutId) {
                    setCurrentLayout(currentLayouts.first())
                }
            }
        } else {
            currentLayouts.add(layoutId)
        }
        setEnabledLayouts(currentLayouts)
    }

    // Input preferences
    fun setAutoCapitalization(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CAPS, enabled).apply()
    }

    fun isAutoCapitalizationEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_CAPS, true)

    // Auto-capitalization modes: "none", "words", "sentences", "all"
    fun setAutoCapitalizationMode(mode: String) {
        prefs.edit().putString(KEY_AUTO_CAP_MODE, mode).apply()
    }

    fun getAutoCapitalizationMode(): String = prefs.getString(KEY_AUTO_CAP_MODE, "sentences") ?: "sentences"

    fun setKeyPressSound(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    fun isKeyPressSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND, false)

    fun setKeyPressSoundVolume(volume: Float) {
        val clamped = volume.coerceIn(
            Constants.Audio.MIN_SOUND_VOLUME,
            Constants.Audio.MAX_SOUND_VOLUME
        )
        prefs.edit().putFloat(KEY_SOUND_VOLUME, clamped).apply()
    }

    fun getKeyPressSoundVolume(): Float {
        return prefs.getFloat(KEY_SOUND_VOLUME, Constants.Audio.DEFAULT_SOUND_VOLUME)
            .coerceIn(Constants.Audio.MIN_SOUND_VOLUME, Constants.Audio.MAX_SOUND_VOLUME)
    }

    fun setKeyPressVibration(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
    }

    fun isKeyPressVibrationEnabled(): Boolean = prefs.getBoolean(KEY_VIBRATION, true)

    fun setKeyPressVibrationDuration(duration: Int) {
        prefs.edit().putInt(KEY_VIBRATION_DURATION, duration.coerceIn(10, 100)).apply()
    }

    fun getKeyPressVibrationDuration(): Int = prefs.getInt(KEY_VIBRATION_DURATION, 20)

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

    // Gesture controls
    fun setGesturesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GESTURES, enabled).apply()
    }

    fun isGesturesEnabled(): Boolean = prefs.getBoolean(KEY_GESTURES, true)

    fun setSwipeToDeleteEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_DELETE, enabled).apply()
    }

    fun isSwipeToDeleteEnabled(): Boolean = prefs.getBoolean(KEY_SWIPE_DELETE, true)

    fun setSwipeCursorMoveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_CURSOR, enabled).apply()
    }

    fun isSwipeCursorMoveEnabled(): Boolean = prefs.getBoolean(KEY_SWIPE_CURSOR, true)

    // Clipboard features
    fun setClipboardHistoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLIPBOARD_HISTORY, enabled).apply()
    }

    fun isClipboardHistoryEnabled(): Boolean = prefs.getBoolean(KEY_CLIPBOARD_HISTORY, true)

    fun setClipboardSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLIPBOARD_SYNC, enabled).apply()
    }

    fun isClipboardSyncEnabled(): Boolean = prefs.getBoolean(KEY_CLIPBOARD_SYNC, false)

    // Number row preference
    fun setNumberRowEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NUMBER_ROW, enabled).apply()
    }

    fun isNumberRowEnabled(): Boolean = prefs.getBoolean(KEY_NUMBER_ROW, false)

    // One-handed mode preference
    fun setOneHandedMode(mode: String) {  // "off", "left", "right"
        prefs.edit().putString(KEY_ONE_HANDED_MODE, mode).apply()
    }

    fun getOneHandedMode(): String = prefs.getString(KEY_ONE_HANDED_MODE, "off") ?: "off"

    // Swipe typing sensitivity (0.5-2.0, default 1.0 to match slider)
    fun setSwipeTypingSensitivity(sensitivity: Float) {
        val clampedSensitivity = sensitivity.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(KEY_SWIPE_SENSITIVITY, clampedSensitivity).apply()
    }

    fun getSwipeTypingSensitivity(): Float {
        val stored = prefs.getFloat(KEY_SWIPE_SENSITIVITY, 1.0f)
        return stored.coerceIn(0.5f, 2.0f)
    }

    // Swipe path visibility
    fun setSwipePathVisible(visible: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_PATH_VISIBLE, visible).apply()
    }

    fun isSwipePathVisible(): Boolean = prefs.getBoolean(KEY_SWIPE_PATH_VISIBLE, true)

    // Keyboard height adjustment (percentage: 70-130, default 100)
    fun setKeyboardHeightPercentage(percentage: Int) {
        // Clamp to valid range
        val clampedPercentage = percentage.coerceIn(70, 130)
        prefs.edit().putInt(KEY_KEYBOARD_HEIGHT, clampedPercentage).apply()
    }

    fun getKeyboardHeightPercentage(): Int = prefs.getInt(KEY_KEYBOARD_HEIGHT, 100)

    // TTS (Text-to-Speech) preferences
    fun isTtsEnabled(): Boolean = prefs.getBoolean(KEY_TTS_ENABLED, false)

    fun setTtsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
    }

    fun getTtsEngine(): String? = prefs.getString(KEY_TTS_ENGINE, "android_default")

    fun setTtsEngine(engine: String) {
        prefs.edit().putString(KEY_TTS_ENGINE, engine).apply()
    }

    fun getTtsSpeed(): Float = prefs.getFloat(KEY_TTS_SPEED, 1.0f).coerceIn(0.5f, 2.0f)

    fun setTtsSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_TTS_SPEED, speed.coerceIn(0.5f, 2.0f)).apply()
    }

    fun getTtsPitch(): Float = prefs.getFloat(KEY_TTS_PITCH, 1.0f).coerceIn(0.5f, 2.0f)

    fun setTtsPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_TTS_PITCH, pitch.coerceIn(0.5f, 2.0f)).apply()
    }

    fun getBhashiniApiKey(): String? = prefs.getString(KEY_BHASHINI_API_KEY, null)

    fun setBhashiniApiKey(apiKey: String?) {
        if (apiKey == null) {
            prefs.edit().remove(KEY_BHASHINI_API_KEY).apply()
        } else {
            prefs.edit().putString(KEY_BHASHINI_API_KEY, apiKey).apply()
        }
    }

    // Smart Punctuation preferences
    fun isSmartPunctuationEnabled(): Boolean = prefs.getBoolean(KEY_SMART_PUNCTUATION, true)

    fun setSmartPunctuationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_PUNCTUATION, enabled).apply()
    }

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

    /**
     * Register a listener for preference changes
     */
    fun registerChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Unregister a preference change listener
     */
    fun unregisterChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREFS_NAME = "keyboard_preferences"

        // Theme keys
        private const val KEY_THEME_ID = "theme_id"
        private const val KEY_DYNAMIC_THEME = "dynamic_theme"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_CUSTOM_THEME = "custom_theme_json"
        const val KEY_THEME_VARIANT = "theme_variant"
        private const val THEME_VARIANT_DEFAULT = "DEFAULT"

        // Layout keys
        private const val KEY_CURRENT_LAYOUT = "current_layout"
        private const val KEY_ENABLED_LAYOUTS = "enabled_layouts"
        private const val DEFAULT_LAYOUT = "qwerty"

        // Input keys
        private const val KEY_AUTO_CAPS = "auto_capitalization"
        private const val KEY_AUTO_CAP_MODE = "auto_cap_mode"
        private const val KEY_SOUND = "key_press_sound"
        private const val KEY_SOUND_VOLUME = "key_press_sound_volume"
        private const val KEY_VIBRATION = "key_press_vibration"

        // Advanced feature keys
        private const val KEY_PREDICTIVE = "predictive_text"
        private const val KEY_SWIPE = "swipe_typing"
        private const val KEY_VOICE = "voice_input"

        // Gesture control keys
        private const val KEY_GESTURES = "gestures_enabled"
        private const val KEY_SWIPE_DELETE = "swipe_to_delete"
        private const val KEY_SWIPE_CURSOR = "swipe_cursor_move"

        // Clipboard feature keys
        private const val KEY_CLIPBOARD_HISTORY = "clipboard_history"
        private const val KEY_CLIPBOARD_SYNC = "clipboard_sync"

        // Layout feature keys
        private const val KEY_NUMBER_ROW = "number_row_enabled"
        private const val KEY_ONE_HANDED_MODE = "one_handed_mode"

        // Vibration feature keys
        private const val KEY_VIBRATION_DURATION = "vibration_duration"

        // Swipe typing settings
        private const val KEY_SWIPE_SENSITIVITY = "swipe_sensitivity"
        private const val KEY_SWIPE_PATH_VISIBLE = "swipe_path_visible"

        // Keyboard appearance keys
        private const val KEY_KEYBOARD_HEIGHT = "keyboard_height_percentage"

        // TTS (Text-to-Speech) keys
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_TTS_ENGINE = "tts_engine"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_TTS_PITCH = "tts_pitch"
        private const val KEY_BHASHINI_API_KEY = "bhashini_api_key"

        // Smart Punctuation keys
        private const val KEY_SMART_PUNCTUATION = "smart_punctuation_enabled"
    }
}
