package com.kannada.kavi.features.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kannada.kavi.data.preferences.KeyboardPreferences
import com.kannada.kavi.features.settings.ui.ClipboardSettingsScreen
import com.kannada.kavi.features.settings.ui.ConverterScreen
import com.kannada.kavi.features.settings.ui.GestureSettingsScreen
import com.kannada.kavi.features.settings.ui.LayoutSelectionScreen
import com.kannada.kavi.features.settings.ui.SettingsScreen
import com.kannada.kavi.features.settings.ui.TextToSpeechScreen
import com.kannada.kavi.features.settings.ui.ThemeSelectionScreen
import com.kannada.kavi.features.themes.MaterialYouThemeManager
import com.kannada.kavi.features.themes.tokens.ColorTokens

/**
 * SettingsActivity - Main settings activity with Material You design
 *
 * Features:
 * - Modern Material You UI
 * - Navigation between settings screens
 * - Theme integration
 * - Edge-to-edge display
 */
class SettingsActivity : ComponentActivity() {

    private lateinit var themeManager: MaterialYouThemeManager
    private lateinit var preferences: KeyboardPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        themeManager = MaterialYouThemeManager(this)
        preferences = KeyboardPreferences(this)

        // Apply saved theme preferences
        themeManager.setDynamicColorEnabled(preferences.isDynamicThemeEnabled())
        val savedDarkMode = preferences.isDarkModeEnabled()
        if (savedDarkMode != null) {
            themeManager.setDarkMode(savedDarkMode)
        }

        // Enable edge-to-edge
        enableEdgeToEdge()

        setContent {
            SettingsApp(
                themeManager = themeManager,
                preferences = preferences,
                onFinish = { finish() }
            )
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Update theme when system theme changes
        themeManager.onConfigurationChanged(newConfig)
    }
}

/**
 * Main settings app composable
 */
@Composable
fun SettingsApp(
    themeManager: MaterialYouThemeManager,
    preferences: KeyboardPreferences,
    onFinish: () -> Unit
) {
    // Observe theme changes
    val currentTheme by themeManager.currentTheme.collectAsState()
    val isDark by themeManager.isDarkMode.collectAsState()
    val isDynamicEnabled by themeManager.isDynamicEnabled.collectAsState()
    val colorScheme by themeManager.colorScheme.collectAsState()

    // Apply Material You theme with dynamic colors if enabled
    MaterialTheme(
        colorScheme = if (isDynamicEnabled && colorScheme != null) {
            // Use dynamic colors from wallpaper
            colorScheme!!
        } else if (isDark) {
            // Use static dark theme
            darkColorScheme(
                primary = ColorTokens.Dark.primary,
                onPrimary = ColorTokens.Dark.onPrimary,
                primaryContainer = ColorTokens.Dark.primaryContainer,
                onPrimaryContainer = ColorTokens.Dark.onPrimaryContainer,
                secondary = ColorTokens.Dark.secondary,
                onSecondary = ColorTokens.Dark.onSecondary,
                secondaryContainer = ColorTokens.Dark.secondaryContainer,
                onSecondaryContainer = ColorTokens.Dark.onSecondaryContainer,
                tertiary = ColorTokens.Dark.tertiary,
                onTertiary = ColorTokens.Dark.onTertiary,
                tertiaryContainer = ColorTokens.Dark.tertiaryContainer,
                onTertiaryContainer = ColorTokens.Dark.onTertiaryContainer,
                surface = ColorTokens.Dark.surface,
                onSurface = ColorTokens.Dark.onSurface,
                surfaceVariant = ColorTokens.Dark.surfaceVariant,
                onSurfaceVariant = ColorTokens.Dark.onSurfaceVariant,
                surfaceContainerLowest = ColorTokens.Dark.surfaceContainerLowest,
                surfaceContainerLow = ColorTokens.Dark.surfaceContainerLow,
                surfaceContainer = ColorTokens.Dark.surfaceContainer,
                surfaceContainerHigh = ColorTokens.Dark.surfaceContainerHigh,
                surfaceContainerHighest = ColorTokens.Dark.surfaceContainerHighest,
                background = ColorTokens.Dark.background,
                onBackground = ColorTokens.Dark.onBackground,
                outline = ColorTokens.Dark.outline,
                outlineVariant = ColorTokens.Dark.outlineVariant,
                error = ColorTokens.Dark.error,
                onError = ColorTokens.Dark.onError,
                errorContainer = ColorTokens.Dark.errorContainer,
                onErrorContainer = ColorTokens.Dark.onErrorContainer
            )
        } else {
            lightColorScheme(
                primary = ColorTokens.Light.primary,
                onPrimary = ColorTokens.Light.onPrimary,
                primaryContainer = ColorTokens.Light.primaryContainer,
                onPrimaryContainer = ColorTokens.Light.onPrimaryContainer,
                secondary = ColorTokens.Light.secondary,
                onSecondary = ColorTokens.Light.onSecondary,
                secondaryContainer = ColorTokens.Light.secondaryContainer,
                onSecondaryContainer = ColorTokens.Light.onSecondaryContainer,
                tertiary = ColorTokens.Light.tertiary,
                onTertiary = ColorTokens.Light.onTertiary,
                tertiaryContainer = ColorTokens.Light.tertiaryContainer,
                onTertiaryContainer = ColorTokens.Light.onTertiaryContainer,
                surface = ColorTokens.Light.surface,
                onSurface = ColorTokens.Light.onSurface,
                surfaceVariant = ColorTokens.Light.surfaceVariant,
                onSurfaceVariant = ColorTokens.Light.onSurfaceVariant,
                surfaceContainerLowest = ColorTokens.Light.surfaceContainerLowest,
                surfaceContainerLow = ColorTokens.Light.surfaceContainerLow,
                surfaceContainer = ColorTokens.Light.surfaceContainer,
                surfaceContainerHigh = ColorTokens.Light.surfaceContainerHigh,
                surfaceContainerHighest = ColorTokens.Light.surfaceContainerHighest,
                background = ColorTokens.Light.background,
                onBackground = ColorTokens.Light.onBackground,
                outline = ColorTokens.Light.outline,
                outlineVariant = ColorTokens.Light.outlineVariant,
                error = ColorTokens.Light.error,
                onError = ColorTokens.Light.onError,
                errorContainer = ColorTokens.Light.errorContainer,
                onErrorContainer = ColorTokens.Light.onErrorContainer
            )
        }
    ) {
        // Navigation
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "settings"
        ) {
            // Main settings screen
            composable("settings") {
                SettingsScreen(
                    themeManager = themeManager,
                    preferences = preferences,
                    onNavigateBack = onFinish,
                    onNavigateToThemes = {
                        navController.navigate("themes")
                    },
                    onNavigateToLayouts = {
                        navController.navigate("layouts")
                    },
                    onNavigateToConverter = {
                        navController.navigate("converter")
                    },
                    onNavigateToTts = {
                        navController.navigate("tts")
                    },
                    onNavigateToGestures = {
                        navController.navigate("gestures")
                    },
                    onNavigateToClipboard = {
                        navController.navigate("clipboard")
                    },
                    onNavigateToAbout = {
                        // TODO: Navigate to about screen
                    }
                )
            }

            // Theme selection screen
            composable("themes") {
                ThemeSelectionScreen(
                    themeManager = themeManager,
                    preferences = preferences,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Layout selection screen
            composable("layouts") {
                LayoutSelectionScreen(
                    preferences = preferences,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Nudi-Unicode Converter screen
            composable("converter") {
                ConverterScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Text-to-Speech settings screen
            composable("tts") {
                TextToSpeechScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Gesture settings screen
            composable("gestures") {
                GestureSettingsScreen(
                    preferences = preferences,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Clipboard settings screen
            composable("clipboard") {
                ClipboardSettingsScreen(
                    preferences = preferences,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
