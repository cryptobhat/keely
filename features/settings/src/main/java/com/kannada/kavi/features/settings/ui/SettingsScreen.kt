package com.kannada.kavi.features.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.kannada.kavi.data.preferences.KeyboardPreferences
import com.kannada.kavi.features.settings.ui.components.*
import com.kannada.kavi.features.themes.MaterialYouThemeManager
import com.kannada.kavi.features.themes.ThemeVariant
import com.kannada.kavi.features.themes.tokens.SpacingTokens

/**
 * SettingsScreen - Main settings screen with Material You design
 *
 * Following Material You design system:
 * - Uses semantic colors from theme
 * - Spacing tokens for consistent layout
 * - Material 3 components
 * - Proper elevation and surface hierarchy
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeManager: MaterialYouThemeManager,
    preferences: KeyboardPreferences,
    onNavigateBack: () -> Unit = {},
    onNavigateToThemes: () -> Unit = {},
    onNavigateToLayouts: () -> Unit = {},
    onNavigateToConverter: () -> Unit = {},
    onNavigateToTts: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    // State
    var isDarkMode by remember { mutableStateOf(themeManager.isDarkMode.value) }
    var isDynamicColorEnabled by remember { mutableStateOf(themeManager.isDynamicColorEnabled.value) }
    var themeVariant by remember { mutableStateOf(themeManager.currentVariant.value) }

    var autoCapitalization by remember { mutableStateOf(preferences.isAutoCapitalizationEnabled()) }
    var keyPressSound by remember { mutableStateOf(preferences.isKeyPressSoundEnabled()) }
    var keyPressVibration by remember { mutableStateOf(preferences.isKeyPressVibrationEnabled()) }
    var keyboardHeight by remember { mutableStateOf(preferences.getKeyboardHeightPercentage().toFloat()) }

    var predictiveText by remember { mutableStateOf(preferences.isPredictiveTextEnabled()) }
    var swipeTyping by remember { mutableStateOf(preferences.isSwipeTypingEnabled()) }
    var voiceInput by remember { mutableStateOf(preferences.isVoiceInputEnabled()) }

    // Scroll behavior
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Keyboard Settings",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = SpacingTokens.base,
                vertical = SpacingTokens.base
            ),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.base)
        ) {
            // ==================== APPEARANCE SECTION ====================
            item {
                SettingsSection(title = "Appearance")
            }

            item {
                SettingsCard {
                    SettingsSwitchItem(
                        title = "Dark Mode",
                        description = "Use dark theme",
                        icon = Icons.Default.DarkMode,
                        checked = isDarkMode,
                        onCheckedChange = { checked ->
                            isDarkMode = checked
                            themeManager.setDarkMode(checked)
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchItem(
                        title = "Dynamic Colors",
                        description = "Match your wallpaper colors (Android 12+)",
                        icon = Icons.Default.Palette,
                        checked = isDynamicColorEnabled,
                        onCheckedChange = { checked ->
                            isDynamicColorEnabled = checked
                            themeManager.setDynamicColorEnabled(checked)
                            // Also save to preferences so IME service can read it
                            preferences.setDynamicThemeEnabled(checked)
                        }
                    )

                    SettingsDivider()

                    SettingsNavigationItem(
                        title = "Theme Style",
                        description = when (themeVariant) {
                            ThemeVariant.DEFAULT -> "Default"
                            ThemeVariant.COMPACT -> "Compact"
                            ThemeVariant.COMFORTABLE -> "Comfortable"
                        },
                        icon = Icons.Default.Brush,
                        onClick = onNavigateToThemes
                    )

                    SettingsDivider()

                    // Keyboard Height Slider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SpacingTokens.base)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Keyboard Height",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${keyboardHeight.toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(SpacingTokens.xs))
                        Slider(
                            value = keyboardHeight,
                            onValueChange = { newValue ->
                                keyboardHeight = newValue
                                preferences.setKeyboardHeightPercentage(newValue.toInt())
                            },
                            valueRange = 70f..130f,
                            steps = 11, // 70, 75, 80, ..., 130 (5% increments)
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        Text(
                            text = "Adjust keyboard size (70% - 130%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ==================== KEYBOARD LAYOUT SECTION ====================
            item {
                SettingsSection(title = "Keyboard Layout")
            }

            item {
                SettingsCard {
                    SettingsNavigationItem(
                        title = "Active Layout",
                        description = preferences.getCurrentLayout().uppercase(),
                        icon = Icons.Default.Keyboard,
                        onClick = onNavigateToLayouts
                    )

                    SettingsDivider()

                    SettingsSwitchItem(
                        title = "Auto-Capitalization",
                        description = "Automatically capitalize first letter",
                        icon = Icons.Default.FormatSize,
                        checked = autoCapitalization,
                        onCheckedChange = { checked ->
                            autoCapitalization = checked
                            preferences.setAutoCapitalization(checked)
                        }
                    )
                }
            }

            // ==================== FEEDBACK SECTION ====================
            item {
                SettingsSection(title = "Feedback")
            }

            item {
                SettingsCard {
                    SettingsSwitchItem(
                        title = "Sound on Key Press",
                        description = "Play sound when keys are pressed",
                        icon = Icons.Default.VolumeUp,
                        checked = keyPressSound,
                        onCheckedChange = { checked ->
                            keyPressSound = checked
                            preferences.setKeyPressSound(checked)
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchItem(
                        title = "Vibration on Key Press",
                        description = "Vibrate when keys are pressed",
                        icon = Icons.Default.Vibration,
                        checked = keyPressVibration,
                        onCheckedChange = { checked ->
                            keyPressVibration = checked
                            preferences.setKeyPressVibration(checked)
                        }
                    )
                }
            }

            // ==================== SMART FEATURES SECTION ====================
            item {
                SettingsSection(title = "Smart Features")
            }

            item {
                SettingsCard {
                    SettingsSwitchItem(
                        title = "Predictive Text",
                        description = "Show word suggestions while typing",
                        icon = Icons.Default.Lightbulb,
                        checked = predictiveText,
                        onCheckedChange = { checked ->
                            predictiveText = checked
                            preferences.setPredictiveText(checked)
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchItem(
                        title = "Swipe Typing",
                        description = "Type by swiping across keys",
                        icon = Icons.Default.Swipe,
                        checked = swipeTyping,
                        onCheckedChange = { checked ->
                            swipeTyping = checked
                            preferences.setSwipeTyping(checked)
                        },
                        enabled = false // Feature not implemented yet
                    )

                    SettingsDivider()

                    SettingsSwitchItem(
                        title = "Voice Input",
                        description = "Enable microphone for voice typing",
                        icon = Icons.Default.Mic,
                        checked = voiceInput,
                        onCheckedChange = { checked ->
                            voiceInput = checked
                            preferences.setVoiceInput(checked)
                        },
                        enabled = false // Feature not implemented yet
                    )
                }
            }

            // ==================== TOOLS SECTION ====================
            item {
                SettingsSection(title = "Tools")
            }

            item {
                SettingsCard {
                    SettingsNavigationItem(
                        title = "Nudi-Unicode Converter",
                        description = "Convert between Nudi and Unicode formats",
                        icon = Icons.Default.SwapHoriz,
                        onClick = onNavigateToConverter
                    )

                    SettingsDivider()

                    SettingsNavigationItem(
                        title = "Text-to-Speech",
                        description = "Configure voice and speech settings",
                        icon = Icons.Default.VolumeUp,
                        onClick = onNavigateToTts
                    )
                }
            }

            // ==================== ADVANCED SECTION ====================
            item {
                SettingsSection(title = "Advanced")
            }

            item {
                SettingsCard {
                    SettingsNavigationItem(
                        title = "Gesture Settings",
                        description = "Customize swipe gestures",
                        icon = Icons.Default.Gesture,
                        onClick = { /* TODO: Navigate to gesture settings */ },
                        enabled = false
                    )

                    SettingsDivider()

                    SettingsNavigationItem(
                        title = "Clipboard Manager",
                        description = "Manage clipboard history",
                        icon = Icons.Default.ContentPaste,
                        onClick = { /* TODO: Navigate to clipboard settings */ },
                        enabled = false
                    )

                    SettingsDivider()

                    SettingsNavigationItem(
                        title = "Dictionary",
                        description = "Personal dictionary and learned words",
                        icon = Icons.Default.Book,
                        onClick = { /* TODO: Navigate to dictionary */ },
                        enabled = false
                    )
                }
            }

            // ==================== ABOUT SECTION ====================
            item {
                SettingsSection(title = "About")
            }

            item {
                SettingsCard {
                    SettingsNavigationItem(
                        title = "App Info",
                        description = "Version, licenses, and more",
                        icon = Icons.Default.Info,
                        onClick = onNavigateToAbout
                    )

                    SettingsDivider()

                    SettingsNavigationItem(
                        title = "Help & Support",
                        description = "User guide and support",
                        icon = Icons.Default.Help,
                        onClick = { /* TODO: Navigate to help */ }
                    )

                    SettingsDivider()

                    SettingsNavigationItem(
                        title = "Privacy Policy",
                        description = "Learn how we protect your data",
                        icon = Icons.Default.PrivacyTip,
                        onClick = { /* TODO: Open privacy policy */ }
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(SpacingTokens.xl))
            }
        }
    }
}
