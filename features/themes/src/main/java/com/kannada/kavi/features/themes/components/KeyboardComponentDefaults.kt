package com.kannada.kavi.features.themes.components

import android.content.Context
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import com.kannada.kavi.features.themes.MaterialYouTheme

/**
 * KeyboardComponentDefaults - Default styling for keyboard components
 *
 * Provides pre-configured Paint objects and styling for all keyboard components:
 * - Keys (regular, special, action)
 * - Suggestions
 * - Popups
 * - Toolbars
 *
 * USAGE:
 * 1. Create defaults:
 *    val defaults = KeyboardComponentDefaults(context, theme)
 *
 * 2. Use pre-configured Paints:
 *    canvas.drawRect(rect, defaults.keyBackgroundPaint)
 *    canvas.drawText(label, x, y, defaults.keyTextPaint)
 *
 * 3. Access dimensions:
 *    val keyHeight = defaults.keyHeightPx
 *    val cornerRadius = defaults.keyCornerRadiusPx
 */
class KeyboardComponentDefaults(
    private val context: Context,
    private val theme: MaterialYouTheme
) {
    // Display metrics
    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    // ==================== KEY STYLING ====================

    /**
     * Regular key background paint
     */
    val keyBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = theme.colors.surface.toArgb()
    }

    /**
     * Pressed key background paint
     */
    val keyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = blendColor(theme.colors.surface, theme.colors.primary, 0.12f).toArgb()
    }

    /**
     * Key text paint
     */
    val keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = theme.colors.onSurface.toArgb()
        textSize = theme.typography.keyLabel.size.value * scaledDensity
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.NORMAL
        )
        isSubpixelText = true
        isLinearText = true
    }

    /**
     * Key hint text paint (numbers on top row)
     */
    val keyHintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        color = blendColor(theme.colors.onSurface, theme.colors.surface, 0.6f).toArgb()
        textSize = theme.typography.keyHint.size.value * scaledDensity
        typeface = android.graphics.Typeface.DEFAULT
    }

    // ==================== SPECIAL KEY STYLING ====================

    /**
     * Special key background paint (Shift, ?123, etc.)
     */
    val specialKeyBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = theme.colors.surfaceVariant.toArgb()
    }

    /**
     * Pressed special key background paint
     */
    val specialKeyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = blendColor(theme.colors.surfaceVariant, theme.colors.primary, 0.15f).toArgb()
    }

    /**
     * Special key text/icon paint
     */
    val specialKeyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = theme.colors.onSurfaceVariant.toArgb()
        textSize = theme.typography.keyLabel.size.value * scaledDensity
        typeface = android.graphics.Typeface.DEFAULT
    }

    // ==================== ACTION KEY STYLING ====================

    /**
     * Action key background paint (Enter)
     */
    val actionKeyBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = theme.colors.primary.toArgb()
    }

    /**
     * Pressed action key background paint
     */
    val actionKeyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = blendColor(theme.colors.primary, theme.colors.onPrimary, 0.2f).toArgb()
    }

    /**
     * Action key text/icon paint
     */
    val actionKeyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = theme.colors.onPrimary.toArgb()
        textSize = theme.typography.keyLabel.size.value * scaledDensity
        typeface = android.graphics.Typeface.DEFAULT
    }

    // ==================== SPACEBAR STYLING ====================

    /**
     * Spacebar background paint
     */
    val spacebarBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = theme.colors.surface.toArgb()
    }

    /**
     * Spacebar text paint
     */
    val spacebarTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = blendColor(theme.colors.onSurface, theme.colors.surface, 0.5f).toArgb()
        textSize = theme.typography.spacebarLabel.size.value * scaledDensity
        typeface = android.graphics.Typeface.DEFAULT
    }

    // ==================== KEYBOARD BACKGROUND ====================

    /**
     * Keyboard background paint
     */
    val keyboardBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = theme.colors.surfaceContainerHighest.toArgb()
    }

    // ==================== DIMENSIONS ====================

    /**
     * Key corner radius (pixels)
     */
    val keyCornerRadiusPx: Float
        get() = theme.shapes.keyCornerRadius.value * density

    /**
     * Key height (pixels)
     */
    val keyHeightPx: Float
        get() = 52f * density // Default 52dp

    /**
     * Key gap (pixels)
     */
    val keyGapPx: Float
        get() = theme.spacing.keyGap.value * density

    /**
     * Row gap (pixels)
     */
    val rowGapPx: Float
        get() = theme.spacing.rowGap.value * density

    /**
     * Key inset (pixels)
     */
    val keyInsetPx: Float
        get() = theme.spacing.keyInset.value * density

    /**
     * Horizontal padding (pixels)
     */
    val paddingHorizontalPx: Float
        get() = theme.spacing.paddingHorizontal.value * density

    /**
     * Top padding (pixels)
     */
    val paddingTopPx: Float
        get() = theme.spacing.paddingTop.value * density

    /**
     * Bottom padding (pixels)
     */
    val paddingBottomPx: Float
        get() = theme.spacing.paddingBottom.value * density

    /**
     * Hint offset left (pixels)
     */
    val hintOffsetLeftPx: Float
        get() = theme.spacing.hintOffsetLeft.value * density

    /**
     * Hint offset top (pixels)
     */
    val hintOffsetTopPx: Float
        get() = theme.spacing.hintOffsetTop.value * density

    // ==================== HELPER FUNCTIONS ====================

    /**
     * Blend two colors together
     */
    private fun blendColor(
        color1: androidx.compose.ui.graphics.Color,
        color2: androidx.compose.ui.graphics.Color,
        ratio: Float
    ): androidx.compose.ui.graphics.Color {
        val argb = androidx.core.graphics.ColorUtils.blendARGB(
            color1.toArgb(),
            color2.toArgb(),
            ratio
        )
        return androidx.compose.ui.graphics.Color(argb)
    }

    /**
     * Convert DP to pixels
     */
    fun dpToPx(dp: Float): Float {
        return dp * density
    }

    /**
     * Convert SP to pixels
     */
    fun spToPx(sp: Float): Float {
        return sp * scaledDensity
    }

    /**
     * Update all paints when theme changes
     */
    fun updateTheme(newTheme: MaterialYouTheme) {
        // Update key paints
        keyBackgroundPaint.color = newTheme.colors.surface.toArgb()
        keyPressedPaint.color = blendColor(newTheme.colors.surface, newTheme.colors.primary, 0.12f).toArgb()
        keyTextPaint.color = newTheme.colors.onSurface.toArgb()
        keyTextPaint.textSize = newTheme.typography.keyLabel.size.value * scaledDensity
        keyHintPaint.color = blendColor(newTheme.colors.onSurface, newTheme.colors.surface, 0.6f).toArgb()
        keyHintPaint.textSize = newTheme.typography.keyHint.size.value * scaledDensity

        // Update special key paints
        specialKeyBackgroundPaint.color = newTheme.colors.surfaceVariant.toArgb()
        specialKeyPressedPaint.color = blendColor(newTheme.colors.surfaceVariant, newTheme.colors.primary, 0.15f).toArgb()
        specialKeyTextPaint.color = newTheme.colors.onSurfaceVariant.toArgb()
        specialKeyTextPaint.textSize = newTheme.typography.keyLabel.size.value * scaledDensity

        // Update action key paints
        actionKeyBackgroundPaint.color = newTheme.colors.primary.toArgb()
        actionKeyPressedPaint.color = blendColor(newTheme.colors.primary, newTheme.colors.onPrimary, 0.2f).toArgb()
        actionKeyTextPaint.color = newTheme.colors.onPrimary.toArgb()
        actionKeyTextPaint.textSize = newTheme.typography.keyLabel.size.value * scaledDensity

        // Update spacebar paints
        spacebarBackgroundPaint.color = newTheme.colors.surface.toArgb()
        spacebarTextPaint.color = blendColor(newTheme.colors.onSurface, newTheme.colors.surface, 0.5f).toArgb()
        spacebarTextPaint.textSize = newTheme.typography.spacebarLabel.size.value * scaledDensity

        // Update keyboard background
        keyboardBackgroundPaint.color = newTheme.colors.surfaceContainerHighest.toArgb()
    }
}

/**
 * SuggestionStripDefaults - Default styling for suggestion strip
 */
class SuggestionStripDefaults(
    private val context: Context,
    private val theme: MaterialYouTheme
) {
    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = theme.colors.surfaceContainerLow.toArgb()
    }

    val suggestionTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = theme.colors.onSurface.toArgb()
        textSize = theme.typography.suggestionText.size.value * scaledDensity
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.NORMAL
        )
    }

    val suggestionHighConfidencePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = theme.colors.primary.toArgb()
        textSize = theme.typography.suggestionText.size.value * scaledDensity
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD
        )
    }

    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = theme.colors.outlineVariant.toArgb()
    }

    val heightPx: Float
        get() = theme.spacing.suggestionPadding.value * density + (theme.typography.suggestionText.size.value * scaledDensity)

    val paddingPx: Float
        get() = theme.spacing.suggestionPadding.value * density

    val gapPx: Float
        get() = theme.spacing.suggestionGap.value * density
}

/**
 * PopupDefaults - Default styling for popup windows
 */
class PopupDefaults(
    private val context: Context,
    private val theme: MaterialYouTheme
) {
    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = theme.colors.surfaceContainerHigh.toArgb()
    }

    val keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = theme.colors.onSurface.toArgb()
        textSize = theme.typography.popupKey.size.value * scaledDensity
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.NORMAL
        )
    }

    val cornerRadiusPx: Float
        get() = theme.shapes.popupCornerRadius.value * density

    val keyCornerRadiusPx: Float
        get() = theme.shapes.popupKeyCornerRadius.value * density

    val paddingPx: Float
        get() = theme.spacing.popupPadding.value * density

    val keyGapPx: Float
        get() = theme.spacing.popupKeyGap.value * density

    val elevationPx: Float
        get() = theme.elevation.popupElevation.value * density
}
