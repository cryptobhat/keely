package com.kannada.kavi.features.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.kannada.kavi.data.preferences.KeyboardPreferences
import com.kannada.kavi.features.settings.ui.components.*
import com.kannada.kavi.features.themes.tokens.SpacingTokens

/**
 * GestureSettingsScreen - Configure swipe typing and gesture controls
 *
 * Settings include:
 * - Swipe typing toggle
 * - Swipe path visibility
 * - Swipe sensitivity
 * - Individual gesture toggles (delete, cursor, select, etc.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSettingsScreen(
    preferences: KeyboardPreferences,
    onNavigateBack: () -> Unit = {}
) {
    // State for all gesture settings
    var swipeTypingEnabled by remember { mutableStateOf(preferences.isSwipeTypingEnabled()) }
    var gesturesEnabled by remember { mutableStateOf(preferences.isGesturesEnabled()) }
    var swipePathVisible by remember { mutableStateOf(preferences.isSwipePathVisible()) }
    var swipeSensitivity by remember { mutableStateOf(preferences.getSwipeTypingSensitivity()) }

    // Individual gesture toggles
    var swipeToDeleteEnabled by remember { mutableStateOf(preferences.isSwipeToDeleteEnabled()) }
    var swipeCursorEnabled by remember { mutableStateOf(preferences.isSwipeCursorMoveEnabled()) }
    var swipeSelectEnabled by remember { mutableStateOf(true) } // Default to true
    var doubleTapShiftEnabled by remember { mutableStateOf(true) } // Default to true
    var longPressPunctuationEnabled by remember { mutableStateOf(true) } // Default to true

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Gesture Settings",
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
            // ==================== SWIPE TYPING SECTION ====================
            item {
                SettingsSection(title = "Swipe Typing")
            }

            item {
                SettingsCard {
                    SettingsSwitchItem(
                        title = "Enable Swipe Typing",
                        description = "Type words by swiping across keys",
                        icon = Icons.Default.Swipe,
                        checked = swipeTypingEnabled,
                        onCheckedChange = { checked ->
                            swipeTypingEnabled = checked
                            preferences.setSwipeTyping(checked)
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchItem(
                        title = "Show Swipe Path",
                        description = "Display trail while swiping",
                        icon = Icons.Default.Timeline,
                        checked = swipePathVisible,
                        enabled = swipeTypingEnabled,
                        onCheckedChange = { checked ->
                            swipePathVisible = checked
                            preferences.setSwipePathVisible(checked)
                        }
                    )

                    SettingsDivider()

                    // Swipe Sensitivity Slider
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = if (swipeTypingEnabled)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                )
                                Text(
                                    text = "Sensitivity",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (swipeTypingEnabled)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                            Text(
                                text = when {
                                    swipeSensitivity < 0.7f -> "Low"
                                    swipeSensitivity < 1.3f -> "Medium"
                                    else -> "High"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (swipeTypingEnabled)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                        Spacer(modifier = Modifier.height(SpacingTokens.xs))
                        Slider(
                            value = swipeSensitivity,
                            enabled = swipeTypingEnabled,
                            onValueChange = { newValue ->
                                swipeSensitivity = newValue
                                preferences.setSwipeTypingSensitivity(newValue)
                            },
                            valueRange = 0.5f..2.0f,
                            steps = 14, // 0.5, 0.6, 0.7, ..., 2.0
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        )
                        Text(
                            text = "Adjust how easily swipes are detected",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (swipeTypingEnabled)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                }
            }

            // ==================== GESTURE CONTROLS SECTION ====================
            item {
                SettingsSection(title = "Gesture Controls")
            }

            item {
                SettingsCard {
                    SettingsSwitchItem(
                        title = "Enable Gestures",
                        description = "Use swipe gestures for quick actions",
                        icon = Icons.Default.Gesture,
                        checked = gesturesEnabled,
                        onCheckedChange = { checked ->
                            gesturesEnabled = checked
                            preferences.setGesturesEnabled(checked)
                        }
                    )
                }
            }

            // ==================== INDIVIDUAL GESTURES SECTION ====================
            item {
                SettingsSection(title = "Individual Gestures")
            }

            item {
                SettingsCard {
                    SettingsSwitchItem(
                        title = "Swipe Left to Delete",
                        description = "Delete previous word by swiping left on backspace",
                        icon = Icons.Default.KeyboardArrowLeft,
                        checked = swipeToDeleteEnabled,
                        enabled = gesturesEnabled,
                        onCheckedChange = { checked ->
                            swipeToDeleteEnabled = checked
                            preferences.setSwipeToDeleteEnabled(checked)
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchItem(
                        title = "Swipe Cursor Movement",
                        description = "Swipe on space bar to move cursor",
                        icon = Icons.Default.SwapHoriz,
                        checked = swipeCursorEnabled,
                        enabled = gesturesEnabled,
                        onCheckedChange = { checked ->
                            swipeCursorEnabled = checked
                            preferences.setSwipeCursorMoveEnabled(checked)
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchItem(
                        title = "Swipe to Select Text",
                        description = "Swipe with shift to select text",
                        icon = Icons.Default.SelectAll,
                        checked = swipeSelectEnabled,
                        enabled = gesturesEnabled,
                        onCheckedChange = { checked ->
                            swipeSelectEnabled = checked
                            // TODO: Add setSwipeToSelectEnabled to preferences
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchItem(
                        title = "Double Tap Shift for Caps",
                        description = "Enable caps lock by double tapping shift",
                        icon = Icons.Default.KeyboardCapslock,
                        checked = doubleTapShiftEnabled,
                        enabled = gesturesEnabled,
                        onCheckedChange = { checked ->
                            doubleTapShiftEnabled = checked
                            // TODO: Add setDoubleTapShiftEnabled to preferences
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchItem(
                        title = "Long Press for Punctuation",
                        description = "Long press keys for alternate characters",
                        icon = Icons.Default.MoreHoriz,
                        checked = longPressPunctuationEnabled,
                        enabled = gesturesEnabled,
                        onCheckedChange = { checked ->
                            longPressPunctuationEnabled = checked
                            // TODO: Add setLongPressPunctuationEnabled to preferences
                        }
                    )
                }
            }

            // ==================== GESTURE HELP SECTION ====================
            item {
                SettingsSection(title = "Gesture Guide")
            }

            item {
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SpacingTokens.base),
                        verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
                    ) {
                        Text(
                            text = "How to use gestures:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        GestureHelpItem(
                            icon = Icons.Default.Swipe,
                            title = "Swipe Typing",
                            description = "Glide your finger across letters to form a word"
                        )

                        GestureHelpItem(
                            icon = Icons.Default.KeyboardArrowLeft,
                            title = "Delete Word",
                            description = "Swipe left from backspace key"
                        )

                        GestureHelpItem(
                            icon = Icons.Default.SwapHoriz,
                            title = "Move Cursor",
                            description = "Swipe left or right on space bar"
                        )

                        GestureHelpItem(
                            icon = Icons.Default.SelectAll,
                            title = "Select Text",
                            description = "Hold shift and swipe on space bar"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper composable for gesture help items
 */
@Composable
private fun GestureHelpItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(SpacingTokens.lg)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
