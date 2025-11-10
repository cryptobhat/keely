package com.kannada.kavi.features.settings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kannada.kavi.features.themes.MaterialYouTheme
import com.kannada.kavi.features.themes.tokens.SpacingTokens

/**
 * Material You Settings Components
 *
 * Following the design system from DESIGN_SYSTEM_QUICK_REFERENCE.md
 * Uses semantic colors, spacing tokens, and proper Material You styling
 */

/**
 * SettingsSection - Section header with title
 *
 * @param title Section title text
 * @param modifier Optional modifier
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = SpacingTokens.base,
                vertical = SpacingTokens.sm
            )
    )
}

/**
 * SettingsSwitchItem - Switch preference item
 *
 * @param title Main title
 * @param description Optional description text
 * @param icon Optional leading icon
 * @param checked Current checked state
 * @param onCheckedChange Callback when switch is toggled
 * @param enabled Whether the item is enabled
 * @param modifier Optional modifier
 */
@Composable
fun SettingsSwitchItem(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SpacingTokens.md))
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        color = colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Leading icon + content
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.base)
            ) {
                // Icon
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) colorScheme.onSurfaceVariant else colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title + Description
                Column(
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.xxs)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.38f)
                    )

                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (enabled) colorScheme.onSurfaceVariant else colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }

            // Switch
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

/**
 * SettingsNavigationItem - Navigation item with arrow
 *
 * @param title Main title
 * @param description Optional description
 * @param icon Optional leading icon
 * @param onClick Callback when clicked
 * @param enabled Whether the item is enabled
 * @param modifier Optional modifier
 */
@Composable
fun SettingsNavigationItem(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SpacingTokens.md))
            .clickable(enabled = enabled, onClick = onClick),
        color = colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Leading icon + content
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.base)
            ) {
                // Icon
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) colorScheme.onSurfaceVariant else colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title + Description
                Column(
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.xxs)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.38f)
                    )

                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (enabled) colorScheme.onSurfaceVariant else colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }

            // Arrow icon
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * SettingsChoiceItem - Single choice selection item
 *
 * @param title Main title
 * @param description Optional description
 * @param icon Optional leading icon
 * @param selected Current selected value
 * @param onSelect Callback when selected
 * @param enabled Whether the item is enabled
 * @param modifier Optional modifier
 */
@Composable
fun SettingsChoiceItem(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    selected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SpacingTokens.md))
            .clickable(enabled = enabled, onClick = onSelect),
        color = if (selected) colorScheme.primaryContainer else colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Leading icon + content
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.base)
            ) {
                // Icon
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title + Description
                Column(
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.xxs)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                    )

                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Radio button
            RadioButton(
                selected = selected,
                onClick = onSelect,
                enabled = enabled
            )
        }
    }
}

/**
 * SettingsSliderItem - Slider preference item
 *
 * @param title Main title
 * @param description Optional description
 * @param icon Optional leading icon
 * @param value Current value (0.0-1.0)
 * @param onValueChange Callback when value changes
 * @param valueLabel Optional function to format value label
 * @param enabled Whether the item is enabled
 * @param modifier Optional modifier
 */
@Composable
fun SettingsSliderItem(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueLabel: ((Float) -> String)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SpacingTokens.md)),
        color = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.base),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.base)
                ) {
                    // Icon
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (enabled) colorScheme.onSurfaceVariant else colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Title
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (enabled) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.38f)
                        )

                        if (description != null) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (enabled) colorScheme.onSurfaceVariant else colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }

                // Value label
                if (valueLabel != null) {
                    Text(
                        text = valueLabel(value),
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.primary
                    )
                }
            }

            // Slider
            Slider(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * SettingsCard - Card container for grouped settings
 *
 * @param modifier Optional modifier
 * @param content Card content
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SpacingTokens.md),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.sm),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
            content = content
        )
    }
}

/**
 * SettingsDivider - Subtle divider between items
 */
@Composable
fun SettingsDivider(
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    HorizontalDivider(
        modifier = modifier.padding(horizontal = SpacingTokens.base),
        color = colorScheme.outlineVariant,
        thickness = 1.dp
    )
}
