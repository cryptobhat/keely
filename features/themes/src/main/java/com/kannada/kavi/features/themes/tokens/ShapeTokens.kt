package com.kannada.kavi.features.themes.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ShapeTokens - Material You Shape System
 *
 * Defines all shape tokens following Material You specification.
 * Shapes are used for corner radii of components.
 *
 * MATERIAL YOU SHAPE SCALE:
 * - None: 0dp (sharp corners, no rounding)
 * - Extra Small: 4dp (subtle rounding)
 * - Small: 8dp (moderate rounding)
 * - Medium: 12dp (noticeable rounding)
 * - Large: 16dp (prominent rounding)
 * - Extra Large: 24dp (very rounded)
 * - Full: 50% (pill shape / circular)
 *
 * USAGE:
 * - Use shape tokens for corner radii
 * - Reference by size (e.g., ShapeTokens.small)
 * - Sizes are in DP (density-independent pixels)
 */
object ShapeTokens {
    // Base shape scale
    val none = 0.dp        // Sharp corners (0dp)
    val extraSmall = 4.dp  // Subtle (4dp)
    val small = 8.dp       // Moderate (8dp)
    val medium = 12.dp     // Noticeable (12dp)
    val large = 16.dp      // Prominent (16dp)
    val extraLarge = 24.dp // Very rounded (24dp)
    val full = 9999.dp     // Pill/circular (999dp = full rounding)

    /**
     * Keyboard-specific shapes
     * Optimized for keyboard components
     */
    object Keyboard {
        // Key shapes
        val keyCornerRadius = 8.dp      // Main key corners (chip style)
        val keyCornerRadiusSmall = 6.dp // Compact key corners
        val keyCornerRadiusLarge = 12.dp // Large key corners (spacebar)

        // Popup shapes
        val popupCornerRadius = 12.dp   // Popup window corners
        val popupKeyCornerRadius = 8.dp // Keys inside popup

        // Suggestion strip shapes
        val suggestionCornerRadius = 8.dp // Suggestion chip corners

        // Toolbar shapes
        val toolbarCornerRadius = 12.dp // Toolbar corners
        val toolbarButtonRadius = 8.dp  // Toolbar button corners
    }

    /**
     * Component shapes
     * Standard component corner radii
     */
    object Component {
        // Button shapes
        val buttonSmall = 8.dp
        val buttonMedium = 12.dp
        val buttonLarge = 16.dp
        val buttonFull = full

        // Card shapes
        val cardSmall = 8.dp
        val cardMedium = 12.dp
        val cardLarge = 16.dp

        // Dialog shapes
        val dialog = 24.dp

        // Chip shapes
        val chip = 8.dp
        val chipFull = full

        // Text field shapes
        val textField = 8.dp

        // Icon button shapes
        val iconButton = full
    }
}

/**
 * ShapeScheme - Complete shape scheme for theming
 * Allows customization of all shape values
 */
data class ShapeScheme(
    // Base scale
    val none: Dp,
    val extraSmall: Dp,
    val small: Dp,
    val medium: Dp,
    val large: Dp,
    val extraLarge: Dp,
    val full: Dp,

    // Keyboard shapes
    val keyCornerRadius: Dp,
    val keyCornerRadiusSmall: Dp,
    val keyCornerRadiusLarge: Dp,
    val popupCornerRadius: Dp,
    val popupKeyCornerRadius: Dp,
    val suggestionCornerRadius: Dp,
    val toolbarCornerRadius: Dp,
    val toolbarButtonRadius: Dp
) {
    companion object {
        /**
         * Get default shape scheme
         */
        fun default() = ShapeScheme(
            none = ShapeTokens.none,
            extraSmall = ShapeTokens.extraSmall,
            small = ShapeTokens.small,
            medium = ShapeTokens.medium,
            large = ShapeTokens.large,
            extraLarge = ShapeTokens.extraLarge,
            full = ShapeTokens.full,
            keyCornerRadius = ShapeTokens.Keyboard.keyCornerRadius,
            keyCornerRadiusSmall = ShapeTokens.Keyboard.keyCornerRadiusSmall,
            keyCornerRadiusLarge = ShapeTokens.Keyboard.keyCornerRadiusLarge,
            popupCornerRadius = ShapeTokens.Keyboard.popupCornerRadius,
            popupKeyCornerRadius = ShapeTokens.Keyboard.popupKeyCornerRadius,
            suggestionCornerRadius = ShapeTokens.Keyboard.suggestionCornerRadius,
            toolbarCornerRadius = ShapeTokens.Keyboard.toolbarCornerRadius,
            toolbarButtonRadius = ShapeTokens.Keyboard.toolbarButtonRadius
        )

        /**
         * Get sharp shape scheme (minimal rounding)
         */
        fun sharp() = ShapeScheme(
            none = 0.dp,
            extraSmall = 2.dp,
            small = 4.dp,
            medium = 6.dp,
            large = 8.dp,
            extraLarge = 12.dp,
            full = ShapeTokens.full,
            keyCornerRadius = 4.dp,
            keyCornerRadiusSmall = 3.dp,
            keyCornerRadiusLarge = 6.dp,
            popupCornerRadius = 6.dp,
            popupKeyCornerRadius = 4.dp,
            suggestionCornerRadius = 4.dp,
            toolbarCornerRadius = 6.dp,
            toolbarButtonRadius = 4.dp
        )

        /**
         * Get rounded shape scheme (more rounding)
         */
        fun rounded() = ShapeScheme(
            none = 0.dp,
            extraSmall = 6.dp,
            small = 12.dp,
            medium = 16.dp,
            large = 20.dp,
            extraLarge = 28.dp,
            full = ShapeTokens.full,
            keyCornerRadius = 12.dp,
            keyCornerRadiusSmall = 8.dp,
            keyCornerRadiusLarge = 16.dp,
            popupCornerRadius = 16.dp,
            popupKeyCornerRadius = 12.dp,
            suggestionCornerRadius = 12.dp,
            toolbarCornerRadius = 16.dp,
            toolbarButtonRadius = 12.dp
        )
    }
}
