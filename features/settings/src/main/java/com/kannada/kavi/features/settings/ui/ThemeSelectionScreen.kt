package com.kannada.kavi.features.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.ViewCompact
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
 * ThemeSelectionScreen - Theme style selector
 *
 * Allows users to choose between:
 * - Default (standard spacing and sizing)
 * - Compact (tighter spacing, smaller elements)
 * - Comfortable (more spacing, larger elements)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionScreen(
    themeManager: MaterialYouThemeManager,
    preferences: KeyboardPreferences,
    onNavigateBack: () -> Unit
) {
    val currentVariant by themeManager.currentVariant.collectAsState()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Theme Style",
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
                    text = "Choose your preferred keyboard style",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = SpacingTokens.sm)
                )
            }

            item {
                SettingsCard {
                    SettingsChoiceItem(
                        title = "Default",
                        description = "Standard spacing and sizing for balanced typing",
                        icon = Icons.Default.ViewCompact,
                        selected = currentVariant == ThemeVariant.DEFAULT,
                        onSelect = {
                            themeManager.setThemeVariant(ThemeVariant.DEFAULT)
                            preferences.setThemeVariant(ThemeVariant.DEFAULT.name)
                        }
                    )

                    SettingsDivider()

                    SettingsChoiceItem(
                        title = "Compact",
                        description = "Tighter spacing, smaller elements for one-handed use",
                        icon = Icons.Default.Compress,
                        selected = currentVariant == ThemeVariant.COMPACT,
                        onSelect = {
                            themeManager.setThemeVariant(ThemeVariant.COMPACT)
                            preferences.setThemeVariant(ThemeVariant.COMPACT.name)
                        }
                    )

                    SettingsDivider()

                    SettingsChoiceItem(
                        title = "Comfortable",
                        description = "More spacing, larger elements for easier typing",
                        icon = Icons.Default.OpenInFull,
                        selected = currentVariant == ThemeVariant.COMFORTABLE,
                        onSelect = {
                            themeManager.setThemeVariant(ThemeVariant.COMFORTABLE)
                            preferences.setThemeVariant(ThemeVariant.COMFORTABLE.name)
                        }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SpacingTokens.base),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Changes apply immediately to your keyboard",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Preview section (optional - could show keyboard preview)
            item {
                Spacer(modifier = Modifier.height(SpacingTokens.xl))
            }
        }
    }
}
