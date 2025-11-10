package com.kannada.kavi.features.themes.tokens

import androidx.compose.ui.graphics.Color

/**
 * ColorTokens - Material You Color Token System
 *
 * This defines all color tokens following Material You specification.
 * Colors are organized by role (Primary, Secondary, Tertiary, Surface, etc.)
 * and include both light and dark theme variants.
 *
 * USAGE:
 * - Use semantic color roles, not hardcoded values
 * - Colors should be referenced by role (e.g., ColorTokens.Light.primary)
 * - Each color role has an "on" variant for text/icons on that color
 *
 * MATERIAL YOU COLOR ROLES:
 * - Primary: Main brand color (action buttons, key interactive elements)
 * - Secondary: Supporting color (less prominent than primary)
 * - Tertiary: Accent color (highlights, special states)
 * - Surface: Component backgrounds
 * - SurfaceVariant: Subtle differentiation from surface
 * - Background: App/keyboard background
 * - Error: Error states and destructive actions
 * - Outline: Borders and dividers
 */
object ColorTokens {

    /**
     * Light Theme Colors - Material You Profile
     * Optimized for light backgrounds
     */
    object Light {
        // Primary colors - Main brand color (Teal/Green)
        val primary = Color(0xFF006C5F)
        val onPrimary = Color(0xFFFFFFFF)
        val primaryContainer = Color(0xFF9FF1E0)
        val onPrimaryContainer = Color(0xFF00201B)

        // Secondary colors - Supporting color
        val secondary = Color(0xFF4A635F)
        val onSecondary = Color(0xFFFFFFFF)
        val secondaryContainer = Color(0xFFCCE8E2)
        val onSecondaryContainer = Color(0xFF051F1C)

        // Tertiary colors - Accent color
        val tertiary = Color(0xFF416376)
        val onTertiary = Color(0xFFFFFFFF)
        val tertiaryContainer = Color(0xFFC5E7FF)
        val onTertiaryContainer = Color(0xFF001E2D)

        // Surface colors - Component backgrounds
        val surface = Color(0xFFFAFDFD)
        val onSurface = Color(0xFF191C1C)
        val surfaceVariant = Color(0xFFDAE5E3)
        val onSurfaceVariant = Color(0xFF3F4948)
        val surfaceTint = primary

        // Surface container variants (elevation levels)
        val surfaceContainerLowest = Color(0xFFFFFFFF)
        val surfaceContainerLow = Color(0xFFF4F7F6)
        val surfaceContainer = Color(0xFFEEF1F0)
        val surfaceContainerHigh = Color(0xFFE8EBEA)
        val surfaceContainerHighest = Color(0xFFE2E5E4)

        // Background colors
        val background = Color(0xFFFFFFFF)
        val onBackground = Color(0xFF191C1C)

        // Outline colors - Borders and dividers
        val outline = Color(0xFF6F7978)
        val outlineVariant = Color(0xFFBEC9C7)

        // Error colors
        val error = Color(0xFFB3261E)
        val onError = Color(0xFFFFFFFF)
        val errorContainer = Color(0xFFF9DEDC)
        val onErrorContainer = Color(0xFF410E0B)

        // Inverse colors - For high contrast scenarios
        val inverseSurface = Color(0xFF2D3131)
        val inverseOnSurface = Color(0xFFEFF1F0)
        val inversePrimary = Color(0xFF83D4C1)

        // Scrim - Semi-transparent overlay
        val scrim = Color(0xFF000000)
    }

    /**
     * Dark Theme Colors - Material You Profile
     * Optimized for dark backgrounds
     */
    object Dark {
        // Primary colors - Brighter in dark mode
        val primary = Color(0xFF83D4C1)
        val onPrimary = Color(0xFF003730)
        val primaryContainer = Color(0xFF005048)
        val onPrimaryContainer = Color(0xFF9FF1E0)

        // Secondary colors
        val secondary = Color(0xFFB0CCCB)
        val onSecondary = Color(0xFF1B3532)
        val secondaryContainer = Color(0xFF324B49)
        val onSecondaryContainer = Color(0xFFCCE5E1)

        // Tertiary colors
        val tertiary = Color(0xFFA6CFB4)
        val onTertiary = Color(0xFF113724)
        val tertiaryContainer = Color(0xFF294E3A)
        val onTertiaryContainer = Color(0xFFC2ECCF)

        // Surface colors
        val surface = Color(0xFF191C1C)
        val onSurface = Color(0xFFE0E3E2)
        val surfaceVariant = Color(0xFF3F4948)
        val onSurfaceVariant = Color(0xFFBEC9C7)
        val surfaceTint = primary

        // Surface container variants (elevation levels)
        val surfaceContainerLowest = Color(0xFF0E1111)
        val surfaceContainerLow = Color(0xFF1A1C1C)
        val surfaceContainer = Color(0xFF1E2020)
        val surfaceContainerHigh = Color(0xFF282B2A)
        val surfaceContainerHighest = Color(0xFF333635)

        // Background colors
        val background = Color(0xFF191C1C)
        val onBackground = Color(0xFFE0E3E2)

        // Outline colors
        val outline = Color(0xFF899392)
        val outlineVariant = Color(0xFF3F4948)

        // Error colors
        val error = Color(0xFFFFB4AB)
        val onError = Color(0xFF690005)
        val errorContainer = Color(0xFF93000A)
        val onErrorContainer = Color(0xFFFFDAD6)

        // Inverse colors
        val inverseSurface = Color(0xFFE0E3E2)
        val inverseOnSurface = Color(0xFF2D3131)
        val inversePrimary = Color(0xFF006C5F)

        // Scrim
        val scrim = Color(0xFF000000)
    }

    /**
     * State Layer Opacities - Material You Interaction States
     * Used for hover, focus, pressed, and dragged states
     */
    object StateLayerOpacity {
        const val hover = 0.08f
        const val focus = 0.12f
        const val pressed = 0.12f
        const val dragged = 0.16f
        const val disabled = 0.38f
    }

    /**
     * Elevation Tint Opacities - Material You Elevation System
     * Surface tint overlay for elevated components
     */
    object ElevationTintOpacity {
        const val level0 = 0.00f  // 0dp elevation
        const val level1 = 0.05f  // 1dp elevation
        const val level2 = 0.08f  // 3dp elevation
        const val level3 = 0.11f  // 6dp elevation
        const val level4 = 0.12f  // 8dp elevation
        const val level5 = 0.14f  // 12dp elevation
    }
}

/**
 * ColorScheme - Complete color scheme for theming
 * Combines all color tokens into a single theme definition
 */
data class ColorScheme(
    // Primary
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,

    // Secondary
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,

    // Tertiary
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,

    // Surface
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceTint: Color,

    // Surface containers
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,

    // Background
    val background: Color,
    val onBackground: Color,

    // Outline
    val outline: Color,
    val outlineVariant: Color,

    // Error
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,

    // Inverse
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,

    // Scrim
    val scrim: Color
) {
    companion object {
        /**
         * Get light color scheme
         */
        fun light() = ColorScheme(
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
            surfaceTint = ColorTokens.Light.surfaceTint,
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
            onErrorContainer = ColorTokens.Light.onErrorContainer,
            inverseSurface = ColorTokens.Light.inverseSurface,
            inverseOnSurface = ColorTokens.Light.inverseOnSurface,
            inversePrimary = ColorTokens.Light.inversePrimary,
            scrim = ColorTokens.Light.scrim
        )

        /**
         * Get dark color scheme
         */
        fun dark() = ColorScheme(
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
            surfaceTint = ColorTokens.Dark.surfaceTint,
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
            onErrorContainer = ColorTokens.Dark.onErrorContainer,
            inverseSurface = ColorTokens.Dark.inverseSurface,
            inverseOnSurface = ColorTokens.Dark.inverseOnSurface,
            inversePrimary = ColorTokens.Dark.inversePrimary,
            scrim = ColorTokens.Dark.scrim
        )
    }
}