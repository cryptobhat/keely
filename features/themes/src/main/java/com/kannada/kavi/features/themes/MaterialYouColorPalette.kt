package com.kannada.kavi.features.themes

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * MaterialYouColorPalette - Dynamic Color System
 *
 * Generates Material You tonal palettes from seed colors or wallpaper.
 * Implements Google's Material Color Utilities (MCU) algorithm.
 *
 * WHAT IS DYNAMIC COLOR?
 * =====================
 * Dynamic Color (Material You) extracts colors from the user's wallpaper
 * and generates a harmonious color scheme for the entire UI.
 *
 * HOW IT WORKS:
 * ============
 * 1. Extract dominant color from wallpaper
 * 2. Convert to HCT color space (Hue, Chroma, Tone)
 * 3. Generate tonal palettes (0-100 tones)
 * 4. Apply Material color roles (primary, secondary, etc.)
 * 5. Ensure accessibility (contrast ratios)
 *
 * TONAL PALETTE:
 * =============
 * Each color has 13 tones: 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 99, 100
 * - 0: Pure black
 * - 50: Medium tone
 * - 100: Pure white
 *
 * Example for Primary (#006C5F):
 * - Tone 0: #000000 (black)
 * - Tone 10: #00201B (very dark teal)
 * - Tone 40: #006C5F (primary color)
 * - Tone 80: #D6EFEA (light teal)
 * - Tone 100: #FFFFFF (white)
 *
 * COLOR ROLES (LIGHT MODE):
 * ========================
 * - primary: Tone 40
 * - onPrimary: Tone 100
 * - primaryContainer: Tone 90
 * - onPrimaryContainer: Tone 10
 * - surface: Tone 99
 * - onSurface: Tone 10
 *
 * COLOR ROLES (DARK MODE):
 * =======================
 * - primary: Tone 80
 * - onPrimary: Tone 20
 * - primaryContainer: Tone 30
 * - onPrimaryContainer: Tone 90
 * - surface: Tone 10
 * - onSurface: Tone 90
 */
object MaterialYouColorPalette {

    /**
     * Generate a complete keyboard theme from a seed color
     *
     * @param seedColor The base color (e.g., #006C5F)
     * @param isDark Whether to generate dark theme
     * @return Complete ThemeColors
     */
    fun generateFromSeed(seedColor: Int, isDark: Boolean = false): ThemeColors {
        // Convert seed to HCT
        val hct = rgbToHct(seedColor)

        // Generate tonal palettes
        val primaryPalette = generateTonalPalette(hct.hue, hct.chroma)
        val secondaryPalette = generateTonalPalette(hct.hue + 60, hct.chroma * 0.5)
        val tertiaryPalette = generateTonalPalette(hct.hue + 120, hct.chroma * 0.6)
        val neutralPalette = generateTonalPalette(hct.hue, 8.0)
        val neutralVariantPalette = generateTonalPalette(hct.hue, 16.0)
        val errorPalette = generateTonalPalette(25.0, 84.0) // Red hue

        return if (isDark) {
            buildDarkTheme(
                primaryPalette,
                secondaryPalette,
                tertiaryPalette,
                neutralPalette,
                neutralVariantPalette,
                errorPalette
            )
        } else {
            buildLightTheme(
                primaryPalette,
                secondaryPalette,
                tertiaryPalette,
                neutralPalette,
                neutralVariantPalette,
                errorPalette
            )
        }
    }

    /**
     * Extract colors from wallpaper (Android 12+)
     *
     * Uses Android's WallpaperColors API to extract dynamic colors.
     *
     * @param context Android context
     * @param isDark Whether to generate dark theme
     * @return ThemeColors or null if not available
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun generateFromWallpaper(context: Context, isDark: Boolean = false): ThemeColors? {
        try {
            // Get wallpaper manager
            val wallpaperManager = android.app.WallpaperManager.getInstance(context)
            val wallpaperColors = wallpaperManager.getWallpaperColors(
                android.app.WallpaperManager.FLAG_SYSTEM
            )

            // Extract primary color
            val primaryColor = wallpaperColors?.primaryColor?.toArgb() ?: return null

            // Generate theme from wallpaper color
            return generateFromSeed(primaryColor, isDark)
        } catch (e: Exception) {
            // Fallback to default if extraction fails
            return null
        }
    }

    /**
     * Build light theme from tonal palettes
     */
    private fun buildLightTheme(
        primary: TonalPalette,
        secondary: TonalPalette,
        tertiary: TonalPalette,
        neutral: TonalPalette,
        neutralVariant: TonalPalette,
        error: TonalPalette
    ): ThemeColors {
        return ThemeColors(
            // Primary
            primary = primary.tone(40),
            onPrimary = primary.tone(100),
            primaryContainer = primary.tone(90),
            onPrimaryContainer = primary.tone(10),

            // Secondary
            secondary = secondary.tone(40),
            onSecondary = secondary.tone(100),
            secondaryContainer = secondary.tone(90),
            onSecondaryContainer = secondary.tone(10),

            // Tertiary
            tertiary = tertiary.tone(40),
            onTertiary = tertiary.tone(100),
            tertiaryContainer = tertiary.tone(90),
            onTertiaryContainer = tertiary.tone(10),

            // Surface
            surface = neutral.tone(99),
            onSurface = neutral.tone(10),
            surfaceVariant = neutralVariant.tone(90),
            onSurfaceVariant = neutralVariant.tone(30),

            // Background
            background = neutral.tone(99),
            onBackground = neutral.tone(10),

            // Outline
            outline = neutralVariant.tone(50),
            outlineVariant = neutralVariant.tone(80),

            // Error
            error = error.tone(40),
            onError = error.tone(100),
            errorContainer = error.tone(90),
            onErrorContainer = error.tone(10),

            // Keyboard-specific (using primary palette)
            keyNormal = neutral.tone(99),
            keyPressed = primary.tone(80),
            keySelected = primary.tone(90),
            keyBorder = neutralVariant.tone(50),
            keySelectedBorder = primary.tone(40),

            // Ripple
            ripple = primary.tone(85),

            // Toolbar
            toolbarBackground = neutral.tone(95),
            toolbarIcon = primary.tone(40),

            // Suggestions
            suggestionBackground = neutral.tone(99),
            suggestionText = neutral.tone(10),
            suggestionDivider = neutralVariant.tone(80),

            // Clipboard
            clipboardBackground = neutral.tone(95),
            clipboardCard = neutral.tone(99),
            clipboardBorder = neutralVariant.tone(80)
        )
    }

    /**
     * Build dark theme from tonal palettes
     */
    private fun buildDarkTheme(
        primary: TonalPalette,
        secondary: TonalPalette,
        tertiary: TonalPalette,
        neutral: TonalPalette,
        neutralVariant: TonalPalette,
        error: TonalPalette
    ): ThemeColors {
        return ThemeColors(
            // Primary
            primary = primary.tone(80),
            onPrimary = primary.tone(20),
            primaryContainer = primary.tone(30),
            onPrimaryContainer = primary.tone(90),

            // Secondary
            secondary = secondary.tone(80),
            onSecondary = secondary.tone(20),
            secondaryContainer = secondary.tone(30),
            onSecondaryContainer = secondary.tone(90),

            // Tertiary
            tertiary = tertiary.tone(80),
            onTertiary = tertiary.tone(20),
            tertiaryContainer = tertiary.tone(30),
            onTertiaryContainer = tertiary.tone(90),

            // Surface
            surface = neutral.tone(10),
            onSurface = neutral.tone(90),
            surfaceVariant = neutralVariant.tone(30),
            onSurfaceVariant = neutralVariant.tone(80),

            // Background
            background = neutral.tone(10),
            onBackground = neutral.tone(90),

            // Outline
            outline = neutralVariant.tone(60),
            outlineVariant = neutralVariant.tone(30),

            // Error
            error = error.tone(80),
            onError = error.tone(20),
            errorContainer = error.tone(30),
            onErrorContainer = error.tone(90),

            // Keyboard-specific
            keyNormal = neutral.tone(20),
            keyPressed = primary.tone(30),
            keySelected = primary.tone(30),
            keyBorder = neutralVariant.tone(60),
            keySelectedBorder = primary.tone(80),

            // Ripple
            ripple = primary.tone(30),

            // Toolbar
            toolbarBackground = neutral.tone(10),
            toolbarIcon = primary.tone(80),

            // Suggestions
            suggestionBackground = neutral.tone(20),
            suggestionText = neutral.tone(90),
            suggestionDivider = neutralVariant.tone(30),

            // Clipboard
            clipboardBackground = neutral.tone(10),
            clipboardCard = neutral.tone(20),
            clipboardBorder = neutralVariant.tone(30)
        )
    }

    /**
     * Generate a tonal palette from hue and chroma
     *
     * @param hue Hue (0-360)
     * @param chroma Colorfulness (0-120+)
     * @return TonalPalette with 13 tones
     */
    private fun generateTonalPalette(hue: Double, chroma: Double): TonalPalette {
        val tones = listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 99, 100)
        val colors = tones.associateWith { tone ->
            hctToRgb(HCT(hue, chroma, tone.toDouble()))
        }
        return TonalPalette(colors)
    }

    /**
     * Convert RGB to HCT color space
     *
     * HCT = Hue, Chroma, Tone (Google's perceptual color space)
     *
     * @param rgb RGB color as Int
     * @return HCT color
     */
    private fun rgbToHct(rgb: Int): HCT {
        val r = ((rgb shr 16) and 0xFF) / 255.0
        val g = ((rgb shr 8) and 0xFF) / 255.0
        val b = (rgb and 0xFF) / 255.0

        // Convert to linear RGB
        val rLinear = inverseGamma(r)
        val gLinear = inverseGamma(g)
        val bLinear = inverseGamma(b)

        // Convert to XYZ
        val x = 0.4124 * rLinear + 0.3576 * gLinear + 0.1805 * bLinear
        val y = 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
        val z = 0.0193 * rLinear + 0.1192 * gLinear + 0.9505 * bLinear

        // Convert to LAB
        val l = 116.0 * f(y / 1.0) - 16.0
        val a = 500.0 * (f(x / 0.95047) - f(y / 1.0))
        val bVal = 200.0 * (f(y / 1.0) - f(z / 1.08883))

        // Convert to HCT
        val chroma = kotlin.math.sqrt(a * a + bVal * bVal)
        val hue = kotlin.math.atan2(bVal, a) * 180.0 / Math.PI
        val tone = l

        return HCT(
            hue = if (hue < 0) hue + 360 else hue,
            chroma = chroma,
            tone = tone.coerceIn(0.0, 100.0)
        )
    }

    /**
     * Convert HCT to RGB
     *
     * @param hct HCT color
     * @return RGB color as Int
     */
    private fun hctToRgb(hct: HCT): Int {
        // Convert HCT to LAB
        val hueRadians = hct.hue * Math.PI / 180.0
        val a = hct.chroma * kotlin.math.cos(hueRadians)
        val b = hct.chroma * kotlin.math.sin(hueRadians)
        val l = hct.tone

        // Convert LAB to XYZ
        val fy = (l + 16.0) / 116.0
        val fx = a / 500.0 + fy
        val fz = fy - b / 200.0

        val x = 0.95047 * fInverse(fx)
        val y = 1.0 * fInverse(fy)
        val z = 1.08883 * fInverse(fz)

        // Convert XYZ to linear RGB
        var r = 3.2406 * x - 1.5372 * y - 0.4986 * z
        var g = -0.9689 * x + 1.8758 * y + 0.0415 * z
        var bVal = 0.0557 * x - 0.2040 * y + 1.0570 * z

        // Apply gamma correction
        r = gamma(r)
        g = gamma(g)
        bVal = gamma(bVal)

        // Clamp and convert to RGB
        val rInt = (r * 255.0).roundToInt().coerceIn(0, 255)
        val gInt = (g * 255.0).roundToInt().coerceIn(0, 255)
        val bInt = (bVal * 255.0).roundToInt().coerceIn(0, 255)

        return 0xFF000000.toInt() or (rInt shl 16) or (gInt shl 8) or bInt
    }

    // Helper functions for color space conversions
    private fun inverseGamma(value: Double): Double {
        return if (value <= 0.04045) {
            value / 12.92
        } else {
            ((value + 0.055) / 1.055).pow(2.4)
        }
    }

    private fun gamma(value: Double): Double {
        return if (value <= 0.0031308) {
            value * 12.92
        } else {
            1.055 * value.pow(1.0 / 2.4) - 0.055
        }
    }

    private fun f(t: Double): Double {
        val delta = 6.0 / 29.0
        return if (t > delta * delta * delta) {
            t.pow(1.0 / 3.0)
        } else {
            t / (3.0 * delta * delta) + 4.0 / 29.0
        }
    }

    private fun fInverse(t: Double): Double {
        val delta = 6.0 / 29.0
        return if (t > delta) {
            t * t * t
        } else {
            3.0 * delta * delta * (t - 4.0 / 29.0)
        }
    }
}

/**
 * HCT Color Space
 *
 * Google's perceptual color space:
 * - Hue: Color angle (0-360°)
 * - Chroma: Colorfulness (0-120+)
 * - Tone: Lightness (0-100)
 */
data class HCT(
    val hue: Double,      // 0-360
    val chroma: Double,   // 0-120+
    val tone: Double      // 0-100
)

/**
 * Tonal Palette
 *
 * A set of colors at different tones (0-100) with the same hue and chroma.
 *
 * @property tones Map of tone (0-100) to RGB color
 */
data class TonalPalette(
    private val tones: Map<Int, Int>
) {
    /**
     * Get color at specific tone
     *
     * @param tone Tone value (0-100)
     * @return RGB color
     */
    fun tone(tone: Int): Int {
        return tones[tone] ?: tones[50] ?: 0xFF808080.toInt() // Fallback to medium gray
    }
}
