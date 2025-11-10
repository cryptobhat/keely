package com.kannada.kavi.core.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * VibrationManager - Haptic Feedback for Keyboard
 *
 * Provides vibration feedback when keys are pressed.
 * Uses modern VibrationEffect API on Android 8.0+ (API 26+)
 * Falls back to deprecated vibrate() on older versions.
 *
 * WHY VIBRATION FEEDBACK?
 * =======================
 * 1. TACTILE FEEDBACK: Makes typing feel more physical and satisfying
 * 2. CONFIRMATION: Lets user know key was successfully pressed
 * 3. ACCESSIBILITY: Helps users with visual impairments
 * 4. CUSTOMIZATION: Can be adjusted or disabled by user
 *
 * VIBRATION PATTERNS:
 * ===================
 * - Standard key: 10ms short pulse
 * - Delete key: 15ms slightly longer
 * - Space key: 12ms medium pulse
 * - Enter key: 20ms longer for emphasis
 * - Modifier keys: 8ms subtle pulse
 *
 * ANDROID VIBRATION API:
 * ======================
 * Android 12+ (API 31+):
 * - Use VibratorManager.getDefaultVibrator()
 *
 * Android 8.0+ (API 26+):
 * - Use VibrationEffect for precise control
 * - VibrationEffect.createOneShot(duration, amplitude)
 *
 * Android < 8.0:
 * - Use deprecated vibrate(duration) method
 *
 * PERMISSIONS:
 * ============
 * Requires: <uses-permission android:name="android.permission.VIBRATE" />
 * (Should be added in AndroidManifest.xml)
 */
class VibrationManager(private val context: Context) {

    // Vibrator instance
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ (API 31+) - Use VibratorManager
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        // Older Android versions - Direct Vibrator service
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // Is vibration enabled?
    private var enabled: Boolean = true

    // Vibration durations (in milliseconds)
    companion object {
        private const val DURATION_STANDARD = 10L
        private const val DURATION_DELETE = 15L
        private const val DURATION_SPACE = 12L
        private const val DURATION_ENTER = 20L
        private const val DURATION_MODIFIER = 8L

        // Amplitude (strength) of vibration (1-255, or DEFAULT_AMPLITUDE)
        private const val AMPLITUDE = VibrationEffect.DEFAULT_AMPLITUDE
    }

    /**
     * Check if device has vibrator capability
     */
    fun hasVibrator(): Boolean {
        return vibrator?.hasVibrator() == true
    }

    /**
     * Vibrate for standard key press (letters, numbers)
     */
    fun vibrateStandardKey() {
        vibrate(DURATION_STANDARD)
    }

    /**
     * Vibrate for delete key press
     */
    fun vibrateDeleteKey() {
        vibrate(DURATION_DELETE)
    }

    /**
     * Vibrate for space key press
     */
    fun vibrateSpaceKey() {
        vibrate(DURATION_SPACE)
    }

    /**
     * Vibrate for enter key press
     */
    fun vibrateEnterKey() {
        vibrate(DURATION_ENTER)
    }

    /**
     * Vibrate for modifier key press (shift, symbols, etc.)
     */
    fun vibrateModifierKey() {
        vibrate(DURATION_MODIFIER)
    }

    /**
     * Perform vibration
     *
     * @param duration Duration in milliseconds
     */
    private fun vibrate(duration: Long) {
        if (!enabled || vibrator == null) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0+ (API 26+) - Use VibrationEffect
                val effect = VibrationEffect.createOneShot(duration, AMPLITUDE)
                vibrator.vibrate(effect)
            } else {
                // Older versions - Use deprecated method
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            // Silently fail - vibration is not critical
            // Could be due to missing permission or unsupported device
            e.printStackTrace()
        }
    }

    /**
     * Enable vibration feedback
     */
    fun enable() {
        enabled = true
    }

    /**
     * Disable vibration feedback
     */
    fun disable() {
        enabled = false
    }

    /**
     * Check if vibration is enabled
     */
    fun isEnabled(): Boolean = enabled

    /**
     * Toggle vibration on/off
     *
     * @return New enabled state
     */
    fun toggle(): Boolean {
        enabled = !enabled
        return enabled
    }
}

/**
 * USAGE EXAMPLE:
 * ==============
 *
 * In KaviInputMethodService.kt:
 *
 * ```kotlin
 * private lateinit var vibrationManager: VibrationManager
 *
 * override fun onCreate() {
 *     super.onCreate()
 *     vibrationManager = VibrationManager(this)
 *
 *     // Check if device supports vibration
 *     if (!vibrationManager.hasVibrator()) {
 *         // Hide vibration toggle in settings
 *     }
 *
 *     // Load setting from preferences
 *     val isVibrationEnabled = preferences.isKeyPressVibrationEnabled()
 *     if (isVibrationEnabled) {
 *         vibrationManager.enable()
 *     } else {
 *         vibrationManager.disable()
 *     }
 * }
 *
 * fun onKeyPressed(key: Key) {
 *     // Vibrate based on key type
 *     when (key.type) {
 *         KeyType.CHARACTER -> vibrationManager.vibrateStandardKey()
 *         KeyType.DELETE -> vibrationManager.vibrateDeleteKey()
 *         KeyType.SPACE -> vibrationManager.vibrateSpaceKey()
 *         KeyType.ENTER -> vibrationManager.vibrateEnterKey()
 *         KeyType.SHIFT, KeyType.SYMBOLS -> vibrationManager.vibrateModifierKey()
 *     }
 *
 *     // ... rest of key handling
 * }
 * ```
 *
 * DON'T FORGET:
 * =============
 * Add to AndroidManifest.xml:
 * <uses-permission android:name="android.permission.VIBRATE" />
 */
