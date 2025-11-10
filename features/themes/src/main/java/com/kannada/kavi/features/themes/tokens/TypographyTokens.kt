package com.kannada.kavi.features.themes.tokens

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * TypographyTokens - Material You Typography System
 *
 * Defines all typography tokens following Material You specification.
 * Includes 13 type scales from Display (largest) to Label (smallest).
 *
 * MATERIAL YOU TYPE SCALE:
 * - Display: Largest text (headlines, hero text)
 * - Headline: Section headers
 * - Title: Sub-headers, card titles
 * - Body: Main content text
 * - Label: UI labels, buttons, captions
 *
 * USAGE:
 * - Use semantic type scales, not hardcoded sizes
 * - Reference by role (e.g., TypographyTokens.bodyLarge)
 * - Sizes are in SP (scale-independent pixels) for accessibility
 */
object TypographyTokens {

    /**
     * Display - Largest text style
     * For hero text, splash screens, large headlines
     */
    object Display {
        // Display Large - 57sp
        val large = TypeScale(
            size = 57.sp,
            lineHeight = 64.sp,
            weight = FontWeight.Normal,
            letterSpacing = (-0.25).sp
        )

        // Display Medium - 45sp
        val medium = TypeScale(
            size = 45.sp,
            lineHeight = 52.sp,
            weight = FontWeight.Normal,
            letterSpacing = 0.sp
        )

        // Display Small - 36sp
        val small = TypeScale(
            size = 36.sp,
            lineHeight = 44.sp,
            weight = FontWeight.Normal,
            letterSpacing = 0.sp
        )
    }

    /**
     * Headline - Large section headers
     * For page titles, section headers
     */
    object Headline {
        // Headline Large - 32sp
        val large = TypeScale(
            size = 32.sp,
            lineHeight = 40.sp,
            weight = FontWeight.Normal,
            letterSpacing = 0.sp
        )

        // Headline Medium - 28sp
        val medium = TypeScale(
            size = 28.sp,
            lineHeight = 36.sp,
            weight = FontWeight.Normal,
            letterSpacing = 0.sp
        )

        // Headline Small - 24sp
        val small = TypeScale(
            size = 24.sp,
            lineHeight = 32.sp,
            weight = FontWeight.Normal,
            letterSpacing = 0.sp
        )
    }

    /**
     * Title - Sub-headers and card titles
     * For sub-sections, card headers, dialog titles
     */
    object Title {
        // Title Large - 22sp
        val large = TypeScale(
            size = 22.sp,
            lineHeight = 28.sp,
            weight = FontWeight.Normal,
            letterSpacing = 0.sp
        )

        // Title Medium - 16sp (Medium weight)
        val medium = TypeScale(
            size = 16.sp,
            lineHeight = 24.sp,
            weight = FontWeight.Medium,
            letterSpacing = 0.15.sp
        )

        // Title Small - 14sp (Medium weight)
        val small = TypeScale(
            size = 14.sp,
            lineHeight = 20.sp,
            weight = FontWeight.Medium,
            letterSpacing = 0.1.sp
        )
    }

    /**
     * Body - Main content text
     * For paragraphs, descriptions, content
     */
    object Body {
        // Body Large - 16sp
        val large = TypeScale(
            size = 16.sp,
            lineHeight = 24.sp,
            weight = FontWeight.Normal,
            letterSpacing = 0.5.sp
        )

        // Body Medium - 14sp
        val medium = TypeScale(
            size = 14.sp,
            lineHeight = 20.sp,
            weight = FontWeight.Normal,
            letterSpacing = 0.25.sp
        )

        // Body Small - 12sp
        val small = TypeScale(
            size = 12.sp,
            lineHeight = 16.sp,
            weight = FontWeight.Normal,
            letterSpacing = 0.4.sp
        )
    }

    /**
     * Label - UI labels, buttons, captions
     * For button text, tabs, chips, captions
     */
    object Label {
        // Label Large - 14sp (Medium weight)
        val large = TypeScale(
            size = 14.sp,
            lineHeight = 20.sp,
            weight = FontWeight.Medium,
            letterSpacing = 0.1.sp
        )

        // Label Medium - 12sp (Medium weight)
        val medium = TypeScale(
            size = 12.sp,
            lineHeight = 16.sp,
            weight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )

        // Label Small - 11sp (Medium weight)
        val small = TypeScale(
            size = 11.sp,
            lineHeight = 16.sp,
            weight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }

    /**
     * Keyboard-specific typography
     * Custom scales optimized for keyboard UI
     */
    object Keyboard {
        // Key label - Main key text (q, w, e, etc.)
        val keyLabel = TypeScale(
            size = 16.sp,
            lineHeight = 20.sp,
            weight = FontWeight.Medium,
            letterSpacing = 0.sp
        )

        // Key hint - Small numbers on keys (1, 2, 3)
        val keyHint = TypeScale(
            size = 10.sp,
            lineHeight = 12.sp,
            weight = FontWeight.Normal,
            letterSpacing = 0.sp
        )

        // Spacebar text - Text on spacebar
        val spacebarLabel = TypeScale(
            size = 14.sp,
            lineHeight = 18.sp,
            weight = FontWeight.Normal,
            letterSpacing = 0.5.sp
        )

        // Suggestion text - Word suggestions
        val suggestionText = TypeScale(
            size = 14.sp,
            lineHeight = 20.sp,
            weight = FontWeight.Medium,
            letterSpacing = 0.25.sp
        )

        // Popup key - Long-press popup text
        val popupKey = TypeScale(
            size = 18.sp,
            lineHeight = 24.sp,
            weight = FontWeight.Medium,
            letterSpacing = 0.sp
        )
    }
}

/**
 * TypeScale - Typography scale definition
 * Contains all typography properties for a text style
 */
data class TypeScale(
    val size: TextUnit,
    val lineHeight: TextUnit,
    val weight: FontWeight,
    val letterSpacing: TextUnit
)

/**
 * TypographyScheme - Complete typography scheme for theming
 * Combines all type scales into a single theme definition
 */
data class TypographyScheme(
    // Display
    val displayLarge: TypeScale,
    val displayMedium: TypeScale,
    val displaySmall: TypeScale,

    // Headline
    val headlineLarge: TypeScale,
    val headlineMedium: TypeScale,
    val headlineSmall: TypeScale,

    // Title
    val titleLarge: TypeScale,
    val titleMedium: TypeScale,
    val titleSmall: TypeScale,

    // Body
    val bodyLarge: TypeScale,
    val bodyMedium: TypeScale,
    val bodySmall: TypeScale,

    // Label
    val labelLarge: TypeScale,
    val labelMedium: TypeScale,
    val labelSmall: TypeScale,

    // Keyboard-specific
    val keyLabel: TypeScale,
    val keyHint: TypeScale,
    val spacebarLabel: TypeScale,
    val suggestionText: TypeScale,
    val popupKey: TypeScale
) {
    companion object {
        /**
         * Get default typography scheme
         */
        fun default() = TypographyScheme(
            displayLarge = TypographyTokens.Display.large,
            displayMedium = TypographyTokens.Display.medium,
            displaySmall = TypographyTokens.Display.small,
            headlineLarge = TypographyTokens.Headline.large,
            headlineMedium = TypographyTokens.Headline.medium,
            headlineSmall = TypographyTokens.Headline.small,
            titleLarge = TypographyTokens.Title.large,
            titleMedium = TypographyTokens.Title.medium,
            titleSmall = TypographyTokens.Title.small,
            bodyLarge = TypographyTokens.Body.large,
            bodyMedium = TypographyTokens.Body.medium,
            bodySmall = TypographyTokens.Body.small,
            labelLarge = TypographyTokens.Label.large,
            labelMedium = TypographyTokens.Label.medium,
            labelSmall = TypographyTokens.Label.small,
            keyLabel = TypographyTokens.Keyboard.keyLabel,
            keyHint = TypographyTokens.Keyboard.keyHint,
            spacebarLabel = TypographyTokens.Keyboard.spacebarLabel,
            suggestionText = TypographyTokens.Keyboard.suggestionText,
            popupKey = TypographyTokens.Keyboard.popupKey
        )
    }
}