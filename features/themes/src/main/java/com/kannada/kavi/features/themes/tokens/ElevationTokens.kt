package com.kannada.kavi.features.themes.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ElevationTokens - Material You Elevation System
 *
 * Defines all elevation tokens following Material You specification.
 * Material You uses tonal elevation (surface tint) instead of shadows.
 *
 * MATERIAL YOU ELEVATION LEVELS:
 * - Level 0: 0dp (base surface, no elevation)
 * - Level 1: 1dp (subtle elevation, cards at rest)
 * - Level 2: 3dp (moderate elevation, raised buttons)
 * - Level 3: 6dp (prominent elevation, menus)
 * - Level 4: 8dp (high elevation, modal dialogs)
 * - Level 5: 12dp (highest elevation, floating action buttons)
 *
 * TONAL ELEVATION:
 * Material You applies surface tint (primary color overlay)
 * instead of traditional shadows for elevation.
 *
 * USAGE:
 * - Use elevation tokens for z-depth
 * - Higher elevation = more surface tint
 * - Reference by level (e.g., ElevationTokens.level1)
 */
object ElevationTokens {
    // Base elevation scale
    val level0 = 0.dp   // Base surface
    val level1 = 1.dp   // Subtle
    val level2 = 3.dp   // Moderate
    val level3 = 6.dp   // Prominent
    val level4 = 8.dp   // High
    val level5 = 12.dp  // Highest

    /**
     * Keyboard-specific elevations
     * Optimized for keyboard components
     */
    object Keyboard {
        // Key elevations
        val keyElevation = level0          // Keys are flat (no elevation)
        val keyPressedElevation = level0   // Pressed keys stay flat

        // Popup elevations
        val popupElevation = level3        // Popups float above keyboard
        val longPressPopupElevation = level3

        // Suggestion strip elevation
        val suggestionElevation = level1   // Slight elevation

        // Toolbar elevation
        val toolbarElevation = level2      // Moderate elevation

        // Overlay elevation
        val overlayElevation = level5      // High elevation (emoji picker, settings)
    }

    /**
     * Component elevations
     * Standard component z-depths
     */
    object Component {
        // Button elevations
        val buttonElevation = level0
        val buttonHoverElevation = level1
        val buttonPressedElevation = level0

        // Card elevations
        val cardElevation = level1
        val cardHoverElevation = level2

        // Dialog elevations
        val dialogElevation = level4

        // Menu elevations
        val menuElevation = level3

        // FAB elevations
        val fabElevation = level3
        val fabHoverElevation = level4
        val fabPressedElevation = level2

        // Snackbar elevation
        val snackbarElevation = level3
    }

    /**
     * Tonal elevation opacities
     * Surface tint overlay strengths for each elevation level
     */
    object TonalOpacity {
        const val level0 = 0.00f  // 0% surface tint
        const val level1 = 0.05f  // 5% surface tint
        const val level2 = 0.08f  // 8% surface tint
        const val level3 = 0.11f  // 11% surface tint
        const val level4 = 0.12f  // 12% surface tint
        const val level5 = 0.14f  // 14% surface tint
    }

    /**
     * Shadow opacities
     * Shadow strengths for legacy shadow rendering
     */
    object ShadowOpacity {
        const val ambient = 0.15f   // Ambient shadow (soft, diffuse)
        const val spot = 0.30f      // Spot shadow (sharp, directional)
    }
}

/**
 * ElevationScheme - Complete elevation scheme for theming
 * Allows customization of all elevation values
 */
data class ElevationScheme(
    // Base scale
    val level0: Dp,
    val level1: Dp,
    val level2: Dp,
    val level3: Dp,
    val level4: Dp,
    val level5: Dp,

    // Keyboard elevations
    val keyElevation: Dp,
    val keyPressedElevation: Dp,
    val popupElevation: Dp,
    val suggestionElevation: Dp,
    val toolbarElevation: Dp,
    val overlayElevation: Dp,

    // Tonal opacity
    val tonalOpacityLevel0: Float,
    val tonalOpacityLevel1: Float,
    val tonalOpacityLevel2: Float,
    val tonalOpacityLevel3: Float,
    val tonalOpacityLevel4: Float,
    val tonalOpacityLevel5: Float
) {
    companion object {
        /**
         * Get default elevation scheme
         */
        fun default() = ElevationScheme(
            level0 = ElevationTokens.level0,
            level1 = ElevationTokens.level1,
            level2 = ElevationTokens.level2,
            level3 = ElevationTokens.level3,
            level4 = ElevationTokens.level4,
            level5 = ElevationTokens.level5,
            keyElevation = ElevationTokens.Keyboard.keyElevation,
            keyPressedElevation = ElevationTokens.Keyboard.keyPressedElevation,
            popupElevation = ElevationTokens.Keyboard.popupElevation,
            suggestionElevation = ElevationTokens.Keyboard.suggestionElevation,
            toolbarElevation = ElevationTokens.Keyboard.toolbarElevation,
            overlayElevation = ElevationTokens.Keyboard.overlayElevation,
            tonalOpacityLevel0 = ElevationTokens.TonalOpacity.level0,
            tonalOpacityLevel1 = ElevationTokens.TonalOpacity.level1,
            tonalOpacityLevel2 = ElevationTokens.TonalOpacity.level2,
            tonalOpacityLevel3 = ElevationTokens.TonalOpacity.level3,
            tonalOpacityLevel4 = ElevationTokens.TonalOpacity.level4,
            tonalOpacityLevel5 = ElevationTokens.TonalOpacity.level5
        )

        /**
         * Get subtle elevation scheme (minimal depth)
         */
        fun subtle() = ElevationScheme(
            level0 = 0.dp,
            level1 = 0.5.dp,
            level2 = 1.5.dp,
            level3 = 3.dp,
            level4 = 4.dp,
            level5 = 6.dp,
            keyElevation = 0.dp,
            keyPressedElevation = 0.dp,
            popupElevation = 3.dp,
            suggestionElevation = 0.5.dp,
            toolbarElevation = 1.5.dp,
            overlayElevation = 6.dp,
            tonalOpacityLevel0 = 0.00f,
            tonalOpacityLevel1 = 0.03f,
            tonalOpacityLevel2 = 0.05f,
            tonalOpacityLevel3 = 0.07f,
            tonalOpacityLevel4 = 0.09f,
            tonalOpacityLevel5 = 0.11f
        )

        /**
         * Get pronounced elevation scheme (more depth)
         */
        fun pronounced() = ElevationScheme(
            level0 = 0.dp,
            level1 = 2.dp,
            level2 = 4.dp,
            level3 = 8.dp,
            level4 = 12.dp,
            level5 = 16.dp,
            keyElevation = 0.dp,
            keyPressedElevation = 0.dp,
            popupElevation = 8.dp,
            suggestionElevation = 2.dp,
            toolbarElevation = 4.dp,
            overlayElevation = 16.dp,
            tonalOpacityLevel0 = 0.00f,
            tonalOpacityLevel1 = 0.08f,
            tonalOpacityLevel2 = 0.11f,
            tonalOpacityLevel3 = 0.14f,
            tonalOpacityLevel4 = 0.16f,
            tonalOpacityLevel5 = 0.18f
        )
    }
}
