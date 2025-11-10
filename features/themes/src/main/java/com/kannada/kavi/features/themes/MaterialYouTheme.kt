package com.kannada.kavi.features.themes

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import com.kannada.kavi.features.themes.tokens.ColorScheme
import com.kannada.kavi.features.themes.tokens.TypographyScheme
import com.kannada.kavi.features.themes.tokens.SpacingScheme
import com.kannada.kavi.features.themes.tokens.ShapeScheme
import com.kannada.kavi.features.themes.tokens.ElevationScheme

/**
 * MaterialYouTheme - Complete Material You Theme System
 *
 * This is the main theme interface that combines all design tokens:
 * - Colors (ColorScheme)
 * - Typography (TypographyScheme)
 * - Spacing (SpacingScheme)
 * - Shapes (ShapeScheme)
 * - Elevation (ElevationScheme)
 *
 * USAGE:
 * 1. Create a theme:
 *    val theme = MaterialYouTheme.light()
 *
 * 2. Access tokens:
 *    val primaryColor = theme.colors.primary
 *    val keySize = theme.typography.keyLabel
 *    val keyGap = theme.spacing.keyGap
 *    val keyRadius = theme.shapes.keyCornerRadius
 *    val popupElevation = theme.elevation.popupElevation
 *
 * 3. Convert to View (Canvas) colors:
 *    val color = theme.colors.primary.toArgb()
 *    paint.color = color
 *
 * 4. Customize themes:
 *    val customTheme = theme.copy(
 *        colors = myCustomColors,
 *        spacing = SpacingScheme.compact()
 *    )
 */
data class MaterialYouTheme(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val colors: ColorScheme,
    val typography: TypographyScheme,
    val spacing: SpacingScheme,
    val shapes: ShapeScheme,
    val elevation: ElevationScheme
) {
    companion object {
        /**
         * Create default light theme
         */
        fun light() = MaterialYouTheme(
            id = "material_you_light",
            name = "Material You Light",
            isDark = false,
            colors = ColorScheme.light(),
            typography = TypographyScheme.default(),
            spacing = SpacingScheme.default(),
            shapes = ShapeScheme.default(),
            elevation = ElevationScheme.default()
        )

        /**
         * Create default dark theme
         */
        fun dark() = MaterialYouTheme(
            id = "material_you_dark",
            name = "Material You Dark",
            isDark = true,
            colors = ColorScheme.dark(),
            typography = TypographyScheme.default(),
            spacing = SpacingScheme.default(),
            shapes = ShapeScheme.default(),
            elevation = ElevationScheme.default()
        )

        /**
         * Create compact theme (tighter layout)
         */
        fun compact(isDark: Boolean = false) = MaterialYouTheme(
            id = if (isDark) "compact_dark" else "compact_light",
            name = if (isDark) "Compact Dark" else "Compact Light",
            isDark = isDark,
            colors = if (isDark) ColorScheme.dark() else ColorScheme.light(),
            typography = TypographyScheme.default(),
            spacing = SpacingScheme.compact(),
            shapes = ShapeScheme.sharp(),
            elevation = ElevationScheme.subtle()
        )

        /**
         * Create comfortable theme (more breathing room)
         */
        fun comfortable(isDark: Boolean = false) = MaterialYouTheme(
            id = if (isDark) "comfortable_dark" else "comfortable_light",
            name = if (isDark) "Comfortable Dark" else "Comfortable Light",
            isDark = isDark,
            colors = if (isDark) ColorScheme.dark() else ColorScheme.light(),
            typography = TypographyScheme.default(),
            spacing = SpacingScheme.comfortable(),
            shapes = ShapeScheme.rounded(),
            elevation = ElevationScheme.pronounced()
        )

        /**
         * Get theme based on system settings
         */
        fun fromSystem(context: Context, isDark: Boolean): MaterialYouTheme {
            return if (isDark) dark() else light()
        }
    }

    /**
     * Convert theme to KeyboardColorScheme for compatibility with existing code
     */
    fun toKeyboardColorScheme(): KeyboardColorScheme {
        return KeyboardColorScheme(
            // Regular keys
            keyBackground = colors.surface.toArgb(),
            keyPressed = blendColor(colors.surface.toArgb(), colors.primary.toArgb(), 0.12f),
            keyText = colors.onSurface.toArgb(),

            // Special keys (Shift, ?123, Emoji, Language, Backspace)
            specialKeyBackground = colors.surfaceVariant.toArgb(),
            specialKeyPressed = blendColor(colors.surfaceVariant.toArgb(), colors.primary.toArgb(), 0.15f),
            specialKeyText = colors.onSurfaceVariant.toArgb(),
            specialKeyIcon = colors.onSurfaceVariant.toArgb(),

            // Action key (Enter)
            actionKeyBackground = colors.primary.toArgb(),
            actionKeyPressed = blendColor(colors.primary.toArgb(), colors.onPrimary.toArgb(), 0.2f),
            actionKeyText = colors.onPrimary.toArgb(),
            actionKeyIcon = colors.onPrimary.toArgb(),

            // Spacebar
            spacebarBackground = colors.surface.toArgb(),
            spacebarText = blendColor(colors.onSurface.toArgb(), colors.surface.toArgb(), 0.5f),

            // Hints (numbers above keys)
            keyHintText = blendColor(colors.onSurface.toArgb(), colors.surface.toArgb(), 0.6f),

            // Keyboard background
            keyboardBackground = colors.surfaceContainerHighest.toArgb(),

            // Emoji icon (keep yellow for visibility)
            emojiFill = 0xFFFFEB3B.toInt(),
            emojiOutline = 0xFFF9A825.toInt(),
            emojiEyes = colors.onSurface.toArgb(),
            emojiSmile = colors.onSurface.toArgb()
        )
    }

    /**
     * Blend two ARGB colors
     */
    private fun blendColor(color1: Int, color2: Int, ratio: Float): Int {
        return androidx.core.graphics.ColorUtils.blendARGB(color1, color2, ratio)
    }
}

/**
 * ThemeVariant - Different theme style variants
 */
enum class ThemeVariant {
    DEFAULT,    // Standard Material You theme
    COMPACT,    // Tighter spacing, smaller elements
    COMFORTABLE // More spacing, larger elements
}

/**
 * ThemePreset - Pre-configured theme presets
 * Easy-to-use theme configurations
 */
object ThemePresets {
    /**
     * Get all available presets
     */
    fun all(): List<MaterialYouTheme> = listOf(
        MaterialYouTheme.light(),
        MaterialYouTheme.dark(),
        MaterialYouTheme.compact(isDark = false),
        MaterialYouTheme.compact(isDark = true),
        MaterialYouTheme.comfortable(isDark = false),
        MaterialYouTheme.comfortable(isDark = true)
    )

    /**
     * Get preset by ID
     */
    fun getById(id: String): MaterialYouTheme? {
        return all().find { it.id == id }
    }

    /**
     * Get preset by variant and dark mode
     */
    fun getByVariant(variant: ThemeVariant, isDark: Boolean): MaterialYouTheme {
        return when (variant) {
            ThemeVariant.DEFAULT -> if (isDark) MaterialYouTheme.dark() else MaterialYouTheme.light()
            ThemeVariant.COMPACT -> MaterialYouTheme.compact(isDark)
            ThemeVariant.COMFORTABLE -> MaterialYouTheme.comfortable(isDark)
        }
    }
}
