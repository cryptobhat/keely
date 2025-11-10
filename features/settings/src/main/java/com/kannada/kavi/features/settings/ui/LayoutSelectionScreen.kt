package com.kannada.kavi.features.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.kannada.kavi.data.preferences.KeyboardPreferences
import com.kannada.kavi.features.settings.ui.components.*
import com.kannada.kavi.features.themes.tokens.SpacingTokens

/**
 * LayoutSelectionScreen - Keyboard layout selector
 *
 * Allows users to choose between:
 * - QWERTY (Standard English layout)
 * - Phonetic (Kannada phonetic layout)
 * - Kavi (Kannada traditional layout)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutSelectionScreen(
    preferences: KeyboardPreferences,
    onNavigateBack: () -> Unit
) {
    var currentLayout by remember { mutableStateOf(preferences.getCurrentLayout()) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Layout options
    val layouts = remember {
        listOf(
            LayoutOption(
                id = "qwerty",
                name = "QWERTY",
                description = "Standard English keyboard layout",
                language = "English"
            ),
            LayoutOption(
                id = "phonetic",
                name = "Phonetic",
                description = "Type Kannada using English phonetics",
                language = "ಕನ್ನಡ"
            ),
            LayoutOption(
                id = "kavi",
                name = "Kavi",
                description = "Traditional Kannada keyboard layout",
                language = "ಕನ್ನಡ"
            )
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Keyboard Layout",
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
            item {
                Text(
                    text = "Select your preferred keyboard layout",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = SpacingTokens.sm)
                )
            }

            item {
                SettingsCard {
                    layouts.forEachIndexed { index, layout ->
                        SettingsChoiceItem(
                            title = layout.name,
                            description = "${layout.description} • ${layout.language}",
                            icon = Icons.Default.Language,
                            selected = currentLayout == layout.id,
                            onSelect = {
                                currentLayout = layout.id
                                preferences.setCurrentLayout(layout.id)
                            }
                        )

                        if (index < layouts.size - 1) {
                            SettingsDivider()
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SpacingTokens.base),
                        verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
                    ) {
                        Text(
                            text = "Layout Features",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        when (currentLayout) {
                            "qwerty" -> {
                                Text(
                                    text = "• Standard English typing\n" +
                                            "• Number hints on top row\n" +
                                            "• Symbol layer for special characters\n" +
                                            "• Long-press for alternate characters",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            "phonetic" -> {
                                Text(
                                    text = "• Type Kannada using English sounds\n" +
                                            "• Automatic transliteration\n" +
                                            "• Word suggestions in Kannada\n" +
                                            "• Easy to learn for English users",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            "kavi" -> {
                                Text(
                                    text = "• Traditional Kannada layout\n" +
                                            "• Organized by character groups\n" +
                                            "• Optimized for native speakers\n" +
                                            "• Supports all Kannada characters",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(SpacingTokens.xl))
            }
        }
    }
}

/**
 * Layout option data class
 */
private data class LayoutOption(
    val id: String,
    val name: String,
    val description: String,
    val language: String
)
