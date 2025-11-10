package com.kannada.kavi.features.themes.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * SpacingTokens - Material You Spacing System
 *
 * Defines all spacing tokens following 8-point grid system.
 * Material You uses consistent spacing for visual rhythm.
 *
 * SPACING SCALE:
 * Based on 4dp/8dp grid system for consistency
 * - 4dp: Minimal spacing (gaps between keys)
 * - 8dp: Small spacing (padding within components)
 * - 12dp: Medium spacing (component margins)
 * - 16dp: Standard spacing (section padding)
 * - 24dp: Large spacing (section margins)
 * - 32dp: XL spacing (major sections)
 * - 48dp: XXL spacing (page margins)
 *
 * USAGE:
 * - Use spacing tokens, not hardcoded values
 * - Reference by semantic name (e.g., SpacingTokens.small)
 * - Sizes are in DP (density-independent pixels)
 */
object SpacingTokens {
    // Base spacing scale (8-point grid)
    val none = 0.dp
    val xxs = 2.dp   // Extra extra small - 2dp
    val xs = 4.dp    // Extra small - 4dp
    val sm = 8.dp    // Small - 8dp
    val md = 12.dp   // Medium - 12dp
    val base = 16.dp // Base - 16dp
    val lg = 24.dp   // Large - 24dp
    val xl = 32.dp   // Extra large - 32dp
    val xxl = 48.dp  // Extra extra large - 48dp
    val xxxl = 64.dp // Massive - 64dp

    /**
     * Keyboard-specific spacing
     * Optimized for keyboard layout
     */
    object Keyboard {
        // Key spacing
        val keyGap = 4.dp           // Gap between keys
        val keyInset = 1.5.dp       // Visual inset for key separation
        val rowGap = 4.dp           // Gap between rows

        // Keyboard padding
        val paddingHorizontal = 8.dp  // Side padding
        val paddingTop = 8.dp         // Top padding
        val paddingBottom = 32.dp     // Bottom padding (gesture area)

        // Key internal spacing
        val keyPaddingHorizontal = 8.dp
        val keyPaddingVertical = 12.dp

        // Hint positioning
        val hintOffsetLeft = 4.dp
        val hintOffsetTop = 2.dp

        // Popup spacing
        val popupPadding = 8.dp
        val popupKeyGap = 6.dp
        val popupElevation = 8.dp

        // Suggestion strip spacing
        val suggestionPadding = 12.dp
        val suggestionGap = 8.dp
        val suggestionHeight = 48.dp

        // Toolbar spacing
        val toolbarPadding = 16.dp
        val toolbarItemGap = 12.dp
        val toolbarHeight = 56.dp
    }

    /**
     * Component spacing
     * Standard component paddings
     */
    object Component {
        // Button spacing
        val buttonPaddingHorizontal = 24.dp
        val buttonPaddingVertical = 10.dp
        val buttonGap = 8.dp

        // Card spacing
        val cardPadding = 16.dp
        val cardGap = 12.dp

        // List spacing
        val listItemPadding = 16.dp
        val listItemGap = 8.dp

        // Dialog spacing
        val dialogPadding = 24.dp
        val dialogContentGap = 16.dp

        // Icon spacing
        val iconPadding = 8.dp
        val iconGap = 12.dp
    }

    /**
     * Layout spacing
     * Page-level spacing
     */
    object Layout {
        val screenPaddingHorizontal = 16.dp
        val screenPaddingVertical = 16.dp
        val sectionGap = 24.dp
        val contentMaxWidth = 600.dp
    }
}

/**
 * SpacingScheme - Complete spacing scheme for theming
 * Allows customization of all spacing values
 */
data class SpacingScheme(
    // Base scale
    val none: Dp,
    val xxs: Dp,
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val base: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val xxxl: Dp,

    // Keyboard spacing
    val keyGap: Dp,
    val keyInset: Dp,
    val rowGap: Dp,
    val paddingHorizontal: Dp,
    val paddingTop: Dp,
    val paddingBottom: Dp,
    val keyPaddingHorizontal: Dp,
    val keyPaddingVertical: Dp,
    val hintOffsetLeft: Dp,
    val hintOffsetTop: Dp,
    val popupPadding: Dp,
    val popupKeyGap: Dp,
    val suggestionPadding: Dp,
    val suggestionGap: Dp,
    val toolbarPadding: Dp,
    val toolbarItemGap: Dp
) {
    companion object {
        /**
         * Get default spacing scheme
         */
        fun default() = SpacingScheme(
            none = SpacingTokens.none,
            xxs = SpacingTokens.xxs,
            xs = SpacingTokens.xs,
            sm = SpacingTokens.sm,
            md = SpacingTokens.md,
            base = SpacingTokens.base,
            lg = SpacingTokens.lg,
            xl = SpacingTokens.xl,
            xxl = SpacingTokens.xxl,
            xxxl = SpacingTokens.xxxl,
            keyGap = SpacingTokens.Keyboard.keyGap,
            keyInset = SpacingTokens.Keyboard.keyInset,
            rowGap = SpacingTokens.Keyboard.rowGap,
            paddingHorizontal = SpacingTokens.Keyboard.paddingHorizontal,
            paddingTop = SpacingTokens.Keyboard.paddingTop,
            paddingBottom = SpacingTokens.Keyboard.paddingBottom,
            keyPaddingHorizontal = SpacingTokens.Keyboard.keyPaddingHorizontal,
            keyPaddingVertical = SpacingTokens.Keyboard.keyPaddingVertical,
            hintOffsetLeft = SpacingTokens.Keyboard.hintOffsetLeft,
            hintOffsetTop = SpacingTokens.Keyboard.hintOffsetTop,
            popupPadding = SpacingTokens.Keyboard.popupPadding,
            popupKeyGap = SpacingTokens.Keyboard.popupKeyGap,
            suggestionPadding = SpacingTokens.Keyboard.suggestionPadding,
            suggestionGap = SpacingTokens.Keyboard.suggestionGap,
            toolbarPadding = SpacingTokens.Keyboard.toolbarPadding,
            toolbarItemGap = SpacingTokens.Keyboard.toolbarItemGap
        )

        /**
         * Get compact spacing scheme (tighter layout)
         */
        fun compact() = SpacingScheme(
            none = SpacingTokens.none,
            xxs = 1.dp,
            xs = 2.dp,
            sm = 4.dp,
            md = 8.dp,
            base = 12.dp,
            lg = 16.dp,
            xl = 24.dp,
            xxl = 32.dp,
            xxxl = 48.dp,
            keyGap = 2.dp,
            keyInset = 1.dp,
            rowGap = 2.dp,
            paddingHorizontal = 4.dp,
            paddingTop = 4.dp,
            paddingBottom = 24.dp,
            keyPaddingHorizontal = 6.dp,
            keyPaddingVertical = 8.dp,
            hintOffsetLeft = 3.dp,
            hintOffsetTop = 1.dp,
            popupPadding = 6.dp,
            popupKeyGap = 4.dp,
            suggestionPadding = 8.dp,
            suggestionGap = 6.dp,
            toolbarPadding = 12.dp,
            toolbarItemGap = 8.dp
        )

        /**
         * Get comfortable spacing scheme (more breathing room)
         */
        fun comfortable() = SpacingScheme(
            none = SpacingTokens.none,
            xxs = 4.dp,
            xs = 6.dp,
            sm = 12.dp,
            md = 16.dp,
            base = 20.dp,
            lg = 32.dp,
            xl = 40.dp,
            xxl = 56.dp,
            xxxl = 72.dp,
            keyGap = 6.dp,
            keyInset = 2.dp,
            rowGap = 6.dp,
            paddingHorizontal = 12.dp,
            paddingTop = 12.dp,
            paddingBottom = 40.dp,
            keyPaddingHorizontal = 12.dp,
            keyPaddingVertical = 16.dp,
            hintOffsetLeft = 6.dp,
            hintOffsetTop = 3.dp,
            popupPadding = 12.dp,
            popupKeyGap = 8.dp,
            suggestionPadding = 16.dp,
            suggestionGap = 12.dp,
            toolbarPadding = 20.dp,
            toolbarItemGap = 16.dp
        )
    }
}
