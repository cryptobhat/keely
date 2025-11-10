package com.kannada.kavi.features.themes

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import com.kannada.kavi.features.themes.tokens.ColorScheme
import com.kannada.kavi.features.themes.tokens.ColorTokens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MaterialYouThemeManager - Modern Theme Management System
 *
 * Manages Material You themes with support for:
 * - Light/Dark mode switching
 * - Dynamic color extraction (Android 12+)
 * - Theme presets (default, compact, comfortable)
 * - Custom theme creation
 * - Reactive theme updates via StateFlow
 *
 * USAGE:
 * 1. Create manager:
 *    val themeManager = MaterialYouThemeManager(context)
 *
 * 2. Observe theme changes:
 *    themeManager.currentTheme.collect { theme ->
 *        // Update UI with new theme
 *    }
 *
 * 3. Switch themes:
 *    themeManager.setTheme(MaterialYouTheme.dark())
 *    themeManager.setThemeVariant(ThemeVariant.COMPACT)
 *    themeManager.setDarkMode(true)
 *
 * 4. Enable dynamic colors:
 *    themeManager.setDynamicColorEnabled(true)
 */
class MaterialYouThemeManager(private val context: Context) {

    // Current theme state
    private val _currentTheme = MutableStateFlow(getDefaultTheme())
    val currentTheme: StateFlow<MaterialYouTheme> = _currentTheme.asStateFlow()

    // Theme configuration
    private val _isDarkMode = MutableStateFlow(isSystemDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _currentVariant = MutableStateFlow(ThemeVariant.DEFAULT)
    val currentVariant: StateFlow<ThemeVariant> = _currentVariant.asStateFlow()

    private val _isDynamicColorEnabled = MutableStateFlow(false)
    val isDynamicColorEnabled: StateFlow<Boolean> = _isDynamicColorEnabled.asStateFlow()

    init {
        // Initialize with default theme
        updateTheme()
    }

    /**
     * Set theme variant (DEFAULT, COMPACT, COMFORTABLE)
     */
    fun setThemeVariant(variant: ThemeVariant) {
        if (_currentVariant.value != variant) {
            _currentVariant.value = variant
            updateTheme()
        }
    }

    /**
     * Set dark mode
     */
    fun setDarkMode(isDark: Boolean) {
        if (_isDarkMode.value != isDark) {
            _isDarkMode.value = isDark
            updateTheme()
        }
    }

    /**
     * Toggle dark mode
     */
    fun toggleDarkMode() {
        setDarkMode(!_isDarkMode.value)
    }

    /**
     * Enable or disable dynamic colors (Android 12+)
     */
    fun setDynamicColorEnabled(enabled: Boolean) {
        if (enabled && !isDynamicColorAvailable()) {
            // Can't enable on unsupported devices
            return
        }
        if (_isDynamicColorEnabled.value != enabled) {
            _isDynamicColorEnabled.value = enabled
            updateTheme()
        }
    }

    /**
     * Set custom theme
     */
    fun setTheme(theme: MaterialYouTheme) {
        _currentTheme.value = theme
        _isDarkMode.value = theme.isDark
    }

    /**
     * Update theme when system theme changes
     */
    fun onSystemThemeChanged() {
        val newDarkMode = isSystemDarkMode()
        if (_isDarkMode.value != newDarkMode) {
            _isDarkMode.value = newDarkMode
            updateTheme()
        }
    }

    /**
     * Update theme based on current settings
     */
    private fun updateTheme() {
        val baseTheme = ThemePresets.getByVariant(_currentVariant.value, _isDarkMode.value)

        val finalTheme = if (_isDynamicColorEnabled.value && isDynamicColorAvailable()) {
            try {
                // Try to extract dynamic colors
                val dynamicColors = extractDynamicColors(_isDarkMode.value)
                baseTheme.copy(colors = dynamicColors)
            } catch (e: Exception) {
                // Fallback to static colors if extraction fails
                _isDynamicColorEnabled.value = false
                baseTheme
            }
        } else {
            baseTheme
        }

        _currentTheme.value = finalTheme
    }

    /**
     * Extract dynamic colors from system wallpaper (Android 12+)
     */
    private fun extractDynamicColors(isDark: Boolean): ColorScheme {
        if (!isDynamicColorAvailable()) {
            return if (isDark) ColorScheme.dark() else ColorScheme.light()
        }

        try {
            // Create temporary ComposeView to extract colors
            // This is a workaround since dynamic colors require Compose context
            val view = ComposeView(context)
            var extractedColors: androidx.compose.material3.ColorScheme? = null

            view.setContent {
                extractedColors = if (isDark) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            }

            // Trigger composition
            view.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(1, android.view.View.MeasureSpec.UNSPECIFIED),
                android.view.View.MeasureSpec.makeMeasureSpec(1, android.view.View.MeasureSpec.UNSPECIFIED)
            )

            // Convert Compose ColorScheme to our ColorScheme
            extractedColors?.let { composeColors ->
                return convertComposeColorScheme(composeColors)
            }

            // Fallback if extraction failed
            return if (isDark) ColorScheme.dark() else ColorScheme.light()
        } catch (e: Exception) {
            // Fallback to static colors
            return if (isDark) ColorScheme.dark() else ColorScheme.light()
        }
    }

    /**
     * Convert Compose ColorScheme to our ColorScheme
     */
    private fun convertComposeColorScheme(composeColors: androidx.compose.material3.ColorScheme): ColorScheme {
        return ColorScheme(
            primary = composeColors.primary,
            onPrimary = composeColors.onPrimary,
            primaryContainer = composeColors.primaryContainer,
            onPrimaryContainer = composeColors.onPrimaryContainer,
            secondary = composeColors.secondary,
            onSecondary = composeColors.onSecondary,
            secondaryContainer = composeColors.secondaryContainer,
            onSecondaryContainer = composeColors.onSecondaryContainer,
            tertiary = composeColors.tertiary,
            onTertiary = composeColors.onTertiary,
            tertiaryContainer = composeColors.tertiaryContainer,
            onTertiaryContainer = composeColors.onTertiaryContainer,
            surface = composeColors.surface,
            onSurface = composeColors.onSurface,
            surfaceVariant = composeColors.surfaceVariant,
            onSurfaceVariant = composeColors.onSurfaceVariant,
            surfaceTint = composeColors.surfaceTint,
            surfaceContainerLowest = composeColors.surfaceContainerLowest,
            surfaceContainerLow = composeColors.surfaceContainerLow,
            surfaceContainer = composeColors.surfaceContainer,
            surfaceContainerHigh = composeColors.surfaceContainerHigh,
            surfaceContainerHighest = composeColors.surfaceContainerHighest,
            background = composeColors.background,
            onBackground = composeColors.onBackground,
            outline = composeColors.outline,
            outlineVariant = composeColors.outlineVariant,
            error = composeColors.error,
            onError = composeColors.onError,
            errorContainer = composeColors.errorContainer,
            onErrorContainer = composeColors.onErrorContainer,
            inverseSurface = composeColors.inverseSurface,
            inverseOnSurface = composeColors.inverseOnSurface,
            inversePrimary = composeColors.inversePrimary,
            scrim = composeColors.scrim
        )
    }

    /**
     * Check if system is in dark mode
     */
    private fun isSystemDarkMode(): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Check if dynamic colors are available (Android 12+)
     */
    private fun isDynamicColorAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    /**
     * Get default theme based on system settings
     */
    private fun getDefaultTheme(): MaterialYouTheme {
        return if (isSystemDarkMode()) {
            MaterialYouTheme.dark()
        } else {
            MaterialYouTheme.light()
        }
    }

    /**
     * Get current theme as KeyboardColorScheme for compatibility
     */
    fun getCurrentKeyboardColorScheme(): KeyboardColorScheme {
        return _currentTheme.value.toKeyboardColorScheme()
    }
}
