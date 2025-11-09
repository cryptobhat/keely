package com.kannada.kavi.features.themes

/**
 * KeyboardTheme - Complete Material You 2025 Design System
 *
 * This data class represents a complete keyboard theme with all visual properties
 * based on Material You (Material Design 3) design system for Android.
 *
 * WHAT IS MATERIAL YOU?
 * ====================
 * Material You is Google's next-generation design system that:
 * - Adapts to user's wallpaper colors (dynamic theming)
 * - Uses tonal color palettes for harmonious UI
 * - Provides accessible contrast ratios
 * - Supports light/dark modes seamlessly
 * - Creates personalized, adaptive experiences
 *
 * DESIGN SYSTEM PRINCIPLES:
 * ========================
 * 1. **Dynamic Color**: Extract colors from wallpaper
 * 2. **Tonal Palettes**: Generate harmonious color scales
 * 3. **Elevation**: Use tonal layers instead of shadows
 * 4. **Shape**: Rounded corners create friendly, modern UI
 * 5. **Typography**: Clear hierarchy with Google Sans
 * 6. **Motion**: Smooth, purposeful animations
 *
 * THEME STRUCTURE:
 * ===============
 * This theme contains 6 major sections:
 * 1. Metadata (id, name, mode)
 * 2. Colors (Material 3 color system)
 * 3. Typography (fonts, sizes, weights)
 * 4. Shape (corner radii, borders)
 * 5. Spacing (padding, gaps)
 * 6. Interaction (feedback, animations)
 *
 * @property id Unique theme identifier
 * @property name Display name (e.g., "Material You Light")
 * @property mode Theme mode (light, dark, auto)
 * @property isDynamic Whether theme uses wallpaper colors
 * @property colors All color properties
 * @property typography Font settings
 * @property shape Corner radii and borders
 * @property spacing Padding and gaps
 * @property interaction Feedback settings
 */
data class KeyboardTheme(
    // ==================== METADATA ====================
    val id: String,
    val name: String,
    val mode: ThemeMode,
    val isDynamic: Boolean = false,

    // ==================== COLORS ====================
    val colors: ThemeColors,

    // ==================== TYPOGRAPHY ====================
    val typography: ThemeTypography,

    // ==================== SHAPE ====================
    val shape: ThemeShape,

    // ==================== SPACING ====================
    val spacing: ThemeSpacing,

    // ==================== INTERACTION ====================
    val interaction: ThemeInteraction
) {
    companion object {
        /**
         * Default Material You Light theme
         * Based on design system specification
         */
        fun defaultLight(): KeyboardTheme {
            return KeyboardTheme(
                id = "material_you_light",
                name = "Material You Light",
                mode = ThemeMode.LIGHT,
                isDynamic = false,
                colors = ThemeColors.defaultLight(),
                typography = ThemeTypography.default(),
                shape = ThemeShape.default(),
                spacing = ThemeSpacing.default(),
                interaction = ThemeInteraction.default()
            )
        }

        /**
         * Default Material You Dark theme
         */
        fun defaultDark(): KeyboardTheme {
            return KeyboardTheme(
                id = "material_you_dark",
                name = "Material You Dark",
                mode = ThemeMode.DARK,
                isDynamic = false,
                colors = ThemeColors.defaultDark(),
                typography = ThemeTypography.default(),
                shape = ThemeShape.default(),
                spacing = ThemeSpacing.default(),
                interaction = ThemeInteraction.default()
            )
        }
    }
}

/**
 * ThemeMode - Light, Dark, or Auto (system-based)
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    AUTO  // Follows system theme
}

/**
 * ThemeColors - Material 3 Color System
 *
 * Material You uses a tonal palette system with:
 * - Primary: Main brand color (#006C5F in our design system)
 * - Secondary: Complementary accent colors
 * - Tertiary: Additional accent for variety
 * - Neutral/Neutral Variant: Surfaces and backgrounds
 * - Error: For error states
 *
 * Each color has tonal variants (0-100) for different use cases.
 *
 * COLOR ROLES:
 * ===========
 * - primary: Main interactive elements
 * - onPrimary: Text/icons on primary color
 * - primaryContainer: Less prominent primary elements
 * - onPrimaryContainer: Text/icons on primary container
 * - surface: Background surfaces
 * - onSurface: Text/icons on surfaces
 * - surfaceVariant: Alternative surface color
 * - outline: Borders and dividers
 *
 * @property primary Main brand color (#006C5F)
 * @property onPrimary Text on primary (usually white)
 * @property primaryContainer Tinted primary backgrounds
 * @property onPrimaryContainer Text on primary container
 * @property secondary Complementary accent
 * @property surface Background color
 * @property onSurface Text color
 * @property surfaceVariant Alternative surface
 * @property outline Border color
 * @property background Overall background
 * @property keyNormal Normal key background
 * @property keyPressed Pressed key background
 * @property keySelected Selected key background
 * @property keyBorder Key border color
 */
data class ThemeColors(
    // Primary colors
    val primary: Int,                    // #006C5F (from design system)
    val onPrimary: Int,                  // White or very light
    val primaryContainer: Int,           // Light tint of primary
    val onPrimaryContainer: Int,         // Dark text on container

    // Secondary colors
    val secondary: Int,                  // Complementary color
    val onSecondary: Int,
    val secondaryContainer: Int,
    val onSecondaryContainer: Int,

    // Tertiary colors (optional accent)
    val tertiary: Int,
    val onTertiary: Int,
    val tertiaryContainer: Int,
    val onTertiaryContainer: Int,

    // Surface colors
    val surface: Int,                    // #FFFFFF (light) or #1C1B1F (dark)
    val onSurface: Int,                  // Text color
    val surfaceVariant: Int,             // Alternative surface
    val onSurfaceVariant: Int,           // Text on surface variant

    // Background
    val background: Int,                 // #F8F8F8 (from design system)
    val onBackground: Int,               // Text on background

    // Outline and borders
    val outline: Int,                    // #DADADA (from design system)
    val outlineVariant: Int,             // Lighter borders

    // Error colors
    val error: Int,
    val onError: Int,
    val errorContainer: Int,
    val onErrorContainer: Int,

    // Keyboard-specific colors
    val keyNormal: Int,                  // Normal key background
    val keyPressed: Int,                 // Pressed state (#BCE2D8)
    val keySelected: Int,                // Selected state (#D6EFEA)
    val keyBorder: Int,                  // Key border (#DADADA)
    val keySelectedBorder: Int,          // Selected border (#00A894)

    // Ripple and feedback
    val ripple: Int,                     // Ripple color (#CCEAE4)

    // Toolbar
    val toolbarBackground: Int,          // #EDEDED (from design system)
    val toolbarIcon: Int,                // Icon color

    // Suggestion strip
    val suggestionBackground: Int,
    val suggestionText: Int,
    val suggestionDivider: Int,

    // Clipboard
    val clipboardBackground: Int,        // #F5F5F5
    val clipboardCard: Int,              // Card background
    val clipboardBorder: Int             // #E0E0E0
) {
    companion object {
        /**
         * Default light theme colors
         * Based on Material You + design system specification
         */
        fun defaultLight(): ThemeColors {
            return ThemeColors(
                // Primary (#006C5F - teal green from design system)
                primary = 0xFF006C5F.toInt(),
                onPrimary = 0xFFFFFFFF.toInt(),
                primaryContainer = 0xFFD6EFEA.toInt(),          // Light teal
                onPrimaryContainer = 0xFF00201B.toInt(),        // Dark teal

                // Secondary (complementary purple-gray)
                secondary = 0xFF5F5F5F.toInt(),
                onSecondary = 0xFFFFFFFF.toInt(),
                secondaryContainer = 0xFFE0E0E0.toInt(),
                onSecondaryContainer = 0xFF1C1C1C.toInt(),

                // Tertiary (optional warm accent)
                tertiary = 0xFF7D5260.toInt(),
                onTertiary = 0xFFFFFFFF.toInt(),
                tertiaryContainer = 0xFFFFD8E4.toInt(),
                onTertiaryContainer = 0xFF31111D.toInt(),

                // Surfaces
                surface = 0xFFFFFFFF.toInt(),                   // White
                onSurface = 0xFF1C1B1F.toInt(),                 // Near black
                surfaceVariant = 0xFFF3F3F3.toInt(),            // Light gray
                onSurfaceVariant = 0xFF49454F.toInt(),          // Medium gray

                // Background (#F8F8F8 from design system)
                background = 0xFFF8F8F8.toInt(),
                onBackground = 0xFF1C1B1F.toInt(),

                // Outlines
                outline = 0xFFDADADA.toInt(),                   // From design system
                outlineVariant = 0xFFE8E8E8.toInt(),

                // Error (Material standard red)
                error = 0xFFB3261E.toInt(),
                onError = 0xFFFFFFFF.toInt(),
                errorContainer = 0xFFF9DEDC.toInt(),
                onErrorContainer = 0xFF410E0B.toInt(),

                // Keyboard keys - clean modern look like reference
                keyNormal = 0xFFF8F9FA.toInt(),                 // Very light gray background
                keyPressed = 0xFFE1E3E6.toInt(),                // Slightly darker when pressed
                keySelected = 0xFFD6EFEA.toInt(),               // From design system
                keyBorder = 0xFFE5E5E5.toInt(),                 // Very subtle border
                keySelectedBorder = 0xFF00A894.toInt(),         // Bright teal

                // Ripple
                ripple = 0xFFCCEAE4.toInt(),                    // From design system

                // Toolbar
                toolbarBackground = 0xFFEDEDED.toInt(),         // From design system
                toolbarIcon = 0xFF006C5F.toInt(),               // Primary color

                // Suggestions
                suggestionBackground = 0xFFFFFFFF.toInt(),
                suggestionText = 0xFF1C1B1F.toInt(),
                suggestionDivider = 0xFFE0E0E0.toInt(),

                // Clipboard
                clipboardBackground = 0xFFF5F5F5.toInt(),       // From design system
                clipboardCard = 0xFFFFFFFF.toInt(),
                clipboardBorder = 0xFFE0E0E0.toInt()            // From design system
            )
        }

        /**
         * Default dark theme colors
         * Material You dark theme with proper contrast
         */
        fun defaultDark(): ThemeColors {
            return ThemeColors(
                // Primary (brighter in dark mode)
                primary = 0xFF00D9BC.toInt(),                   // Bright teal
                onPrimary = 0xFF003731.toInt(),                 // Dark teal
                primaryContainer = 0xFF005048.toInt(),          // Medium teal
                onPrimaryContainer = 0xFFD6EFEA.toInt(),        // Light teal

                // Secondary
                secondary = 0xFFBDBDBD.toInt(),                 // Light gray
                onSecondary = 0xFF2C2C2C.toInt(),
                secondaryContainer = 0xFF424242.toInt(),
                onSecondaryContainer = 0xFFE0E0E0.toInt(),

                // Tertiary
                tertiary = 0xFFEFB8C8.toInt(),
                onTertiary = 0xFF492532.toInt(),
                tertiaryContainer = 0xFF633B48.toInt(),
                onTertiaryContainer = 0xFFFFD8E4.toInt(),

                // Surfaces
                surface = 0xFF1C1B1F.toInt(),                   // Dark surface
                onSurface = 0xFFE6E1E5.toInt(),                 // Light text
                surfaceVariant = 0xFF2B2930.toInt(),
                onSurfaceVariant = 0xFFCAC4D0.toInt(),

                // Background
                background = 0xFF121212.toInt(),                // Very dark
                onBackground = 0xFFE6E1E5.toInt(),

                // Outlines
                outline = 0xFF4A4A4A.toInt(),
                outlineVariant = 0xFF3A3A3A.toInt(),

                // Error
                error = 0xFFF2B8B5.toInt(),
                onError = 0xFF601410.toInt(),
                errorContainer = 0xFF8C1D18.toInt(),
                onErrorContainer = 0xFFF9DEDC.toInt(),

                // Keyboard keys
                keyNormal = 0xFF2B2930.toInt(),                 // Dark gray keys
                keyPressed = 0xFF1A4A42.toInt(),                // Dark teal pressed
                keySelected = 0xFF005048.toInt(),               // Medium teal selected
                keyBorder = 0xFF4A4A4A.toInt(),
                keySelectedBorder = 0xFF00D9BC.toInt(),         // Bright teal

                // Ripple
                ripple = 0xFF1A4A42.toInt(),

                // Toolbar
                toolbarBackground = 0xFF1C1B1F.toInt(),
                toolbarIcon = 0xFF00D9BC.toInt(),

                // Suggestions
                suggestionBackground = 0xFF2B2930.toInt(),
                suggestionText = 0xFFE6E1E5.toInt(),
                suggestionDivider = 0xFF4A4A4A.toInt(),

                // Clipboard
                clipboardBackground = 0xFF1C1B1F.toInt(),
                clipboardCard = 0xFF2B2930.toInt(),
                clipboardBorder = 0xFF4A4A4A.toInt()
            )
        }
    }
}

/**
 * ThemeTypography - Font System
 *
 * Based on Google Sans / Noto Sans from design system.
 * Uses Material 3 type scale with custom sizes from design system.
 *
 * FONT FAMILIES:
 * =============
 * - Primary: Google Sans (modern, friendly)
 * - Fallback: Noto Sans (universal support)
 *
 * TYPE SCALE:
 * ==========
 * - Caption: 12sp (small labels)
 * - Body: 14sp (normal text)
 * - Button: 15sp (button labels)
 * - Heading: 18sp (titles, headers)
 *
 * @property fontFamily Font family name
 * @property captionSize Small text size (12sp)
 * @property bodySize Normal text size (14sp)
 * @property buttonSize Button text size (15sp)
 * @property headingSize Heading size (18sp)
 * @property labelWeight Key label weight (500 medium)
 * @property headingWeight Heading weight (600 semibold)
 * @property bodyWeight Body text weight (400 regular)
 */
data class ThemeTypography(
    val fontFamily: String,              // "google_sans" or "noto_sans"

    // Sizes (from design system)
    val captionSize: Float,              // 12sp
    val bodySize: Float,                 // 14sp
    val buttonSize: Float,               // 15sp
    val headingSize: Float,              // 18sp

    // Weights (from design system)
    val labelWeight: Int,                // 500 (medium)
    val headingWeight: Int,              // 600 (semibold)
    val bodyWeight: Int                  // 400 (regular)
) {
    companion object {
        fun default(): ThemeTypography {
            return ThemeTypography(
                fontFamily = "google_sans",
                captionSize = 12f,
                bodySize = 14f,
                buttonSize = 15f,
                headingSize = 18f,
                labelWeight = 500,
                headingWeight = 600,
                bodyWeight = 400
            )
        }
    }
}

/**
 * ThemeShape - Corner Radii and Borders
 *
 * Material You uses rounded shapes for friendly, modern UI.
 * All values from design system specification.
 *
 * SHAPE SYSTEM:
 * ============
 * - Key caps: 8dp rounded corners
 * - Containers: 12dp (cards, sheets)
 * - Buttons: 24dp (pill-shaped)
 *
 * BORDERS:
 * =======
 * - Enabled by default
 * - Color: #DADADA (outline color)
 * - Width: 1dp
 *
 * @property keyCornerRadius Key corner radius (8dp)
 * @property containerCornerRadius Container radius (12dp)
 * @property buttonCornerRadius Button radius (24dp)
 * @property borderEnabled Show borders
 * @property borderWidth Border width (1dp)
 */
data class ThemeShape(
    val keyCornerRadius: Float,          // 8dp
    val containerCornerRadius: Float,    // 12dp
    val buttonCornerRadius: Float,       // 24dp
    val borderEnabled: Boolean,          // true
    val borderWidth: Float               // 1dp
) {
    companion object {
        fun default(): ThemeShape {
            return ThemeShape(
                keyCornerRadius = 5f,              // Smaller radius for cleaner look
                containerCornerRadius = 12f,
                buttonCornerRadius = 24f,
                borderEnabled = false,             // No borders for cleaner appearance
                borderWidth = 0.5f                 // Thinner if enabled
            )
        }
    }
}

/**
 * ThemeSpacing - Padding and Gaps
 *
 * Consistent spacing system from design system.
 *
 * SPACING SYSTEM:
 * ==============
 * - Key spacing: 4dp horizontal, 6dp vertical
 * - Row padding: 6dp
 * - Container padding: 8dp
 *
 * @property keyHorizontalSpacing Horizontal gap between keys (4dp)
 * @property keyVerticalSpacing Vertical gap between keys (6dp)
 * @property rowPadding Padding around rows (6dp)
 * @property containerPadding Padding inside containers (8dp)
 */
data class ThemeSpacing(
    val keyHorizontalSpacing: Float,     // 4dp
    val keyVerticalSpacing: Float,       // 6dp
    val rowPadding: Float,               // 6dp
    val containerPadding: Float          // 8dp
) {
    companion object {
        fun default(): ThemeSpacing {
            return ThemeSpacing(
                keyHorizontalSpacing = 4f,
                keyVerticalSpacing = 6f,
                rowPadding = 6f,
                containerPadding = 8f
            )
        }
    }
}

/**
 * ThemeInteraction - Feedback and Animation Settings
 *
 * Controls haptic feedback, sounds, and animation durations.
 *
 * FEEDBACK:
 * ========
 * - Vibration: short_low intensity
 * - Sound: soft_click
 * - Volume: 50% (0.5f)
 *
 * ANIMATIONS:
 * ==========
 * - Ripple: 120ms
 * - Fast: 100ms
 * - Medium: 200ms
 * - Slow: 300ms
 *
 * @property vibrationEnabled Enable haptic feedback
 * @property vibrationIntensity Intensity (0.0 to 1.0)
 * @property soundEnabled Enable key sounds
 * @property soundVolume Volume (0.0 to 1.0)
 * @property rippleDuration Ripple animation duration (120ms)
 * @property transitionFast Fast animations (100ms)
 * @property transitionMedium Medium animations (200ms)
 * @property transitionSlow Slow animations (300ms)
 */
data class ThemeInteraction(
    val vibrationEnabled: Boolean,       // true
    val vibrationIntensity: Float,       // 0.3f (short_low)
    val soundEnabled: Boolean,           // true
    val soundVolume: Float,              // 0.5f (50%)
    val rippleDuration: Long,            // 120ms
    val transitionFast: Long,            // 100ms
    val transitionMedium: Long,          // 200ms
    val transitionSlow: Long             // 300ms
) {
    companion object {
        fun default(): ThemeInteraction {
            return ThemeInteraction(
                vibrationEnabled = true,
                vibrationIntensity = 0.3f,
                soundEnabled = true,
                soundVolume = 0.5f,
                rippleDuration = 120L,
                transitionFast = 100L,
                transitionMedium = 200L,
                transitionSlow = 300L
            )
        }
    }
}
