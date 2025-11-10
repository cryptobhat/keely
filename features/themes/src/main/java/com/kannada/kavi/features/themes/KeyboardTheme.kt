package com.kannada.kavi.features.themes

import androidx.compose.ui.graphics.Color

/**
 * KeyboardTheme - Represents a complete keyboard theme
 */
data class KeyboardTheme(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val colors: KeyboardColors
) {
    companion object {
        fun defaultLight() = KeyboardTheme(
            id = "material_you_light",
            name = "Material You Light",
            isDark = false,
            colors = KeyboardColors(
                background = Color(0xFFF5F5F5),
                keyBackground = Color.White,
                keyForeground = Color(0xFF1F1F1F),
                primaryKeyBackground = Color(0xFF1976D2),
                primaryKeyForeground = Color.White,
                accent = Color(0xFF1976D2),
                suggestion = Color(0xFF1976D2),
                divider = Color(0xFFE0E0E0)
            )
        )

        fun defaultDark() = KeyboardTheme(
            id = "material_you_dark",
            name = "Material You Dark",
            isDark = true,
            colors = KeyboardColors(
                background = Color(0xFF1F1F1F),
                keyBackground = Color(0xFF2C2C2C),
                keyForeground = Color(0xFFE0E0E0),
                primaryKeyBackground = Color(0xFF64B5F6),
                primaryKeyForeground = Color.Black,
                accent = Color(0xFF64B5F6),
                suggestion = Color(0xFF64B5F6),
                divider = Color(0xFF3C3C3C)
            )
        )
    }
}

/**
 * KeyboardColors - Color scheme for keyboard theme
 */
data class KeyboardColors(
    val background: Color,
    val keyBackground: Color,
    val keyForeground: Color,
    val primaryKeyBackground: Color,
    val primaryKeyForeground: Color,
    val accent: Color,
    val suggestion: Color,
    val divider: Color
)