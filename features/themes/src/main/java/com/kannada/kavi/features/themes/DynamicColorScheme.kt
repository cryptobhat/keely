package com.kannada.kavi.features.themes

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

/**
 * DynamicColorScheme - Material You Dynamic Color Support
 * 
 * Extracts colors from the system's Material You theme and maps them
 * to keyboard-specific color roles.
 * 
 * This enables the keyboard to adapt to the user's wallpaper and system theme,
 * providing a cohesive Material You experience.
 */
object DynamicColorScheme {
    
    /**
     * Get dynamic color scheme based on system theme
     * 
     * @param context Android context
     * @param isDark Whether dark theme is active
     * @return Material 3 ColorScheme with dynamic colors
     */
    @Composable
    fun getDynamicColorScheme(context: Context, isDark: Boolean): ColorScheme {
        return if (isDark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    }
    
    /**
     * Extract keyboard colors from Material 3 ColorScheme
     * 
     * Maps Material 3 color roles to keyboard-specific colors:
     * - Primary → Action key (Enter)
     * - Surface → Key background
     * - SurfaceVariant → Special keys
     * - OnSurface → Key text
     * - OnPrimary → Action key text
     * 
     * @param colorScheme Material 3 ColorScheme
     * @return KeyboardColorScheme with mapped colors
     */
    fun extractKeyboardColors(colorScheme: ColorScheme): KeyboardColorScheme {
        return KeyboardColorScheme(
            // Regular keys
            keyBackground = colorScheme.surface.toArgb(),
            keyPressed = blendColor(
                colorScheme.surface.toArgb(),
                colorScheme.primary.toArgb(),
                0.12f
            ),
            keyText = colorScheme.onSurface.toArgb(),
            
            // Special keys (Shift, ?123, Emoji, Language, Backspace)
            specialKeyBackground = colorScheme.surfaceVariant.toArgb(),
            specialKeyPressed = blendColor(
                colorScheme.surfaceVariant.toArgb(),
                colorScheme.primary.toArgb(),
                0.15f
            ),
            specialKeyText = colorScheme.onSurfaceVariant.toArgb(),
            specialKeyIcon = colorScheme.onSurfaceVariant.toArgb(),
            
            // Action key (Enter)
            actionKeyBackground = colorScheme.primary.toArgb(),
            actionKeyPressed = blendColor(
                colorScheme.primary.toArgb(),
                colorScheme.onPrimary.toArgb(),
                0.2f
            ),
            actionKeyText = colorScheme.onPrimary.toArgb(),
            actionKeyIcon = colorScheme.onPrimary.toArgb(),
            
            // Spacebar
            spacebarBackground = colorScheme.surface.toArgb(),
            spacebarText = blendColor(
                colorScheme.onSurface.toArgb(),
                colorScheme.surface.toArgb(),
                0.5f
            ),
            
            // Hints (numbers above keys)
            keyHintText = blendColor(
                colorScheme.onSurface.toArgb(),
                colorScheme.surface.toArgb(),
                0.6f
            ),
            
            // Keyboard background
            keyboardBackground = colorScheme.surfaceContainerHighest.toArgb(),
            
            // Emoji icon (keep yellow for visibility)
            emojiFill = 0xFFFFEB3B.toInt(),
            emojiOutline = 0xFFF9A825.toInt(),
            emojiEyes = colorScheme.onSurface.toArgb(),
            emojiSmile = colorScheme.onSurface.toArgb()
        )
    }
    
    /**
     * Blend two colors together
     * 
     * @param color1 First color (ARGB)
     * @param color2 Second color (ARGB)
     * @param ratio Ratio of color2 (0.0 = all color1, 1.0 = all color2)
     * @return Blended color (ARGB)
     */
    private fun blendColor(color1: Int, color2: Int, ratio: Float): Int {
        return ColorUtils.blendARGB(color1, color2, ratio)
    }
    
    /**
     * Check if dynamic colors are available on this device
     * 
     * Dynamic colors require Android 12+ (API 31+)
     * 
     * @param context Android context
     * @return true if dynamic colors are supported
     */
    fun isDynamicColorAvailable(context: Context): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    }
}

/**
 * Keyboard-specific color scheme extracted from Material You
 */
data class KeyboardColorScheme(
    // Regular keys
    val keyBackground: Int,
    val keyPressed: Int,
    val keyText: Int,
    
    // Special keys
    val specialKeyBackground: Int,
    val specialKeyPressed: Int,
    val specialKeyText: Int,
    val specialKeyIcon: Int,
    
    // Action key
    val actionKeyBackground: Int,
    val actionKeyPressed: Int,
    val actionKeyText: Int,
    val actionKeyIcon: Int,
    
    // Spacebar
    val spacebarBackground: Int,
    val spacebarText: Int,
    
    // Hints
    val keyHintText: Int,
    
    // Keyboard background
    val keyboardBackground: Int,
    
    // Emoji
    val emojiFill: Int,
    val emojiOutline: Int,
    val emojiEyes: Int,
    val emojiSmile: Int
)

