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
 * ClipboardSettingsScreen - Configure clipboard manager settings
 *
 * Settings include:
 * - Enable/disable clipboard history
 * - Maximum clipboard items
 * - Auto-paste toggle
 * - Clear clipboard history
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardSettingsScreen(
    preferences: KeyboardPreferences,
    onNavigateBack: () -> Unit = {}
) {
    // State for clipboard settings
    var clipboardHistoryEnabled by remember { mutableStateOf(preferences.isClipboardHistoryEnabled()) }
    var maxClipboardItems by remember { mutableStateOf(50f) } // Default to 50 items
    var autoPasteEnabled by remember { mutableStateOf(false) } // Default to false
    var clipboardSyncEnabled by remember { mutableStateOf(preferences.isClipboardSyncEnabled()) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Clipboard Manager",
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
            // ==================== CLIPBOARD HISTORY SECTION ====================
            item {
                SettingsSection(title = "Clipboard History")
            }

            item {
                SettingsCard {
                    SettingsSwitchItem(
                        title = "Enable Clipboard History",
                        description = "Save copied items for quick access",
                        icon = Icons.Default.ContentPaste,
                        checked = clipboardHistoryEnabled,
                        onCheckedChange = { checked ->
                            clipboardHistoryEnabled = checked
                            preferences.setClipboardHistoryEnabled(checked)
                        }
                    )

                    SettingsDivider()

                    // Max Items Slider
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
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = if (clipboardHistoryEnabled)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                )
                                Text(
                                    text = "Max History Items",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (clipboardHistoryEnabled)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                            Text(
                                text = "${maxClipboardItems.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (clipboardHistoryEnabled)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                        Spacer(modifier = Modifier.height(SpacingTokens.xs))
                        Slider(
                            value = maxClipboardItems,
                            enabled = clipboardHistoryEnabled,
                            onValueChange = { newValue ->
                                maxClipboardItems = newValue
                                // TODO: Add setMaxClipboardItems to preferences
                            },
                            valueRange = 10f..100f,
                            steps = 17, // 10, 15, 20, ..., 100 (5 increments)
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        )
                        Text(
                            text = "Number of items to keep in history",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (clipboardHistoryEnabled)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                }
            }

            // ==================== CLIPBOARD FEATURES SECTION ====================
            item {
                SettingsSection(title = "Clipboard Features")
            }

            item {
                SettingsCard {
                    SettingsSwitchItem(
                        title = "Auto-Paste on Selection",
                        description = "Automatically paste when item is selected",
                        icon = Icons.Default.TouchApp,
                        checked = autoPasteEnabled,
                        enabled = clipboardHistoryEnabled,
                        onCheckedChange = { checked ->
                            autoPasteEnabled = checked
                            // TODO: Add setAutoPaste to preferences
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchItem(
                        title = "Sync Clipboard",
                        description = "Monitor system clipboard for new items",
                        icon = Icons.Default.Sync,
                        checked = clipboardSyncEnabled,
                        enabled = clipboardHistoryEnabled,
                        onCheckedChange = { checked ->
                            clipboardSyncEnabled = checked
                            preferences.setClipboardSyncEnabled(checked)
                        }
                    )
                }
            }

            // ==================== CLIPBOARD MANAGEMENT SECTION ====================
            item {
                SettingsSection(title = "Manage Clipboard")
            }

            item {
                SettingsCard {
                    // Clear All Button
                    Surface(
                        onClick = { showClearConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(SpacingTokens.base),
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.base),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clear Clipboard History",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Delete all saved clipboard items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    SettingsDivider()

                    // Export/Import Clipboard Items (placeholder for future feature)
                    Surface(
                        onClick = { /* TODO: Export clipboard */ },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(SpacingTokens.base),
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.base),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SaveAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Export Clipboard Data",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                                Text(
                                    text = "Coming soon",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                )
                            }
                        }
                    }
                }
            }

            // ==================== CLIPBOARD INFO SECTION ====================
            item {
                SettingsSection(title = "About Clipboard Manager")
            }

            item {
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SpacingTokens.base),
                        verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
                    ) {
                        ClipboardFeatureItem(
                            icon = Icons.Default.History,
                            title = "Clipboard History",
                            description = "Access recently copied text"
                        )

                        ClipboardFeatureItem(
                            icon = Icons.Default.Search,
                            title = "Search Clipboard",
                            description = "Find items quickly with search"
                        )

                        ClipboardFeatureItem(
                            icon = Icons.Default.PushPin,
                            title = "Pin Important Items",
                            description = "Keep frequently used items at the top"
                        )

                        ClipboardFeatureItem(
                            icon = Icons.Default.Category,
                            title = "Category Filters",
                            description = "Filter by URLs, emails, phone numbers"
                        )
                    }
                }
            }
        }
    }

    // Clear Confirmation Dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(text = "Clear Clipboard History?")
            },
            text = {
                Text(text = "This will permanently delete all saved clipboard items. Pinned items will also be removed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO: Clear clipboard history
                        showClearConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Helper composable for clipboard feature items
 */
@Composable
private fun ClipboardFeatureItem(
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
