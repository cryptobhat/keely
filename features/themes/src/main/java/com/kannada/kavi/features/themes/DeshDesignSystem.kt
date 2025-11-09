package com.kannada.kavi.features.themes

import android.content.Context
import android.graphics.Color
import android.util.DisplayMetrics
import kotlin.math.min

/**
 * Desh Kannada Design System
 *
 * Complete design system based on Desh Kannada keyboard specifications.
 * This design system provides:
 * - Exact colors from the reference keyboard
 * - Responsive sizing based on screen dimensions
 * - Proper spacing and padding values
 * - Animation specifications
 * - Shadow and elevation system
 */
object DeshDesignSystem {

    // ==================== COLORS ====================
    object Colors {
        // Primary colors from Desh keyboard
        const val KEYBOARD_BACKGROUND = 0xFFF5F6F7.toInt()  // Light gray background
        const val KEY_BACKGROUND = 0xFFFFFFFF.toInt()       // Pure white keys
        const val KEY_PRESSED = 0xFFE8E8E8.toInt()         // Light gray when pressed
        const val KEY_TEXT = 0xFF000000.toInt()            // Pure black text
        const val KEY_TEXT_SECONDARY = 0xFF757575.toInt()  // Gray for secondary labels

        // Special keys (shift, delete, etc)
        const val SPECIAL_KEY_BACKGROUND = 0xFFE8F4F1.toInt()  // Light teal for special keys
        const val SPECIAL_KEY_PRESSED = 0xFFD0E8E3.toInt()     // Darker teal when pressed

        // Action key (enter/search)
        const val ACTION_KEY_BACKGROUND = 0xFF4A9B8E.toInt()   // Teal green for action
        const val ACTION_KEY_PRESSED = 0xFF3A8B7E.toInt()      // Darker when pressed
        const val ACTION_KEY_TEXT = 0xFFFFFFFF.toInt()         // White text on action

        // Toolbar and suggestions
        const val TOOLBAR_BACKGROUND = 0xFFFFFFFF.toInt()      // White toolbar
        const val SUGGESTION_TEXT = 0xFF424242.toInt()         // Dark gray text
        const val SUGGESTION_DIVIDER = 0xFFE0E0E0.toInt()      // Light divider

        // Shadows and borders
        const val KEY_SHADOW = 0x1A000000.toInt()              // 10% black shadow
        const val KEY_BORDER = 0xFFE5E5E5.toInt()              // Very light border

        // Emoji bar
        const val EMOJI_BAR_BACKGROUND = 0xFFFAFAFA.toInt()    // Slightly off-white

        // Input field hint
        const val HINT_TEXT = 0xFF9E9E9E.toInt()               // Medium gray
    }

    // ==================== DIMENSIONS ====================
    object Dimensions {
        // Key dimensions (in dp)
        const val KEY_HEIGHT_PHONE_PORTRAIT = 52f       // Optimal for portrait phones
        const val KEY_HEIGHT_PHONE_LANDSCAPE = 42f      // Compact for landscape
        const val KEY_HEIGHT_TABLET = 56f                // Larger for tablets

        // Spacing (in dp)
        const val KEY_HORIZONTAL_GAP = 3f                // Horizontal gap between keys
        const val KEY_VERTICAL_GAP = 6f                  // Vertical gap between rows
        const val KEYBOARD_PADDING_HORIZONTAL = 3f       // Left/right padding
        const val KEYBOARD_PADDING_TOP = 8f              // Top padding
        const val KEYBOARD_PADDING_BOTTOM = 8f           // Bottom padding

        // Corner radius (in dp)
        const val KEY_CORNER_RADIUS = 6f                 // Rounded corners for keys
        const val SPECIAL_KEY_CORNER_RADIUS = 8f         // Slightly more rounded

        // Shadow (in dp)
        const val KEY_SHADOW_RADIUS = 2f                 // Shadow blur radius
        const val KEY_SHADOW_DY = 1f                     // Shadow Y offset

        // Toolbar height
        const val TOOLBAR_HEIGHT = 44f                   // Suggestion bar height
        const val EMOJI_BAR_HEIGHT = 48f                 // Emoji quick access height

        // Text sizes (in sp)
        const val KEY_TEXT_SIZE = 22f                    // Main key label
        const val KEY_HINT_TEXT_SIZE = 12f               // Small hint text
        const val SUGGESTION_TEXT_SIZE = 16f             // Suggestion text
        const val SPECIAL_KEY_TEXT_SIZE = 18f            // Special key labels
    }

    // ==================== SCREEN SIZE DETECTION ====================
    enum class ScreenSize {
        SMALL,    // < 4 inches
        NORMAL,   // 4-6 inches
        LARGE,    // 6-7 inches
        XLARGE    // > 7 inches (tablets)
    }

    enum class Orientation {
        PORTRAIT,
        LANDSCAPE
    }

    /**
     * Detect screen size category
     */
    fun getScreenSize(context: Context): ScreenSize {
        val displayMetrics = context.resources.displayMetrics
        val widthDp = displayMetrics.widthPixels / displayMetrics.density
        val heightDp = displayMetrics.heightPixels / displayMetrics.density
        val screenSizeDp = min(widthDp, heightDp)

        return when {
            screenSizeDp < 360 -> ScreenSize.SMALL
            screenSizeDp < 600 -> ScreenSize.NORMAL
            screenSizeDp < 720 -> ScreenSize.LARGE
            else -> ScreenSize.XLARGE
        }
    }

    /**
     * Get current orientation
     */
    fun getOrientation(context: Context): Orientation {
        val displayMetrics = context.resources.displayMetrics
        return if (displayMetrics.widthPixels < displayMetrics.heightPixels) {
            Orientation.PORTRAIT
        } else {
            Orientation.LANDSCAPE
        }
    }

    /**
     * Get responsive key height based on screen size and orientation
     */
    fun getKeyHeight(context: Context): Float {
        val screenSize = getScreenSize(context)
        val orientation = getOrientation(context)

        return when (screenSize) {
            ScreenSize.SMALL -> {
                if (orientation == Orientation.PORTRAIT) 46f else 38f
            }
            ScreenSize.NORMAL -> {
                if (orientation == Orientation.PORTRAIT) {
                    Dimensions.KEY_HEIGHT_PHONE_PORTRAIT
                } else {
                    Dimensions.KEY_HEIGHT_PHONE_LANDSCAPE
                }
            }
            ScreenSize.LARGE -> {
                if (orientation == Orientation.PORTRAIT) 54f else 44f
            }
            ScreenSize.XLARGE -> {
                Dimensions.KEY_HEIGHT_TABLET
            }
        }
    }

    /**
     * Get responsive spacing based on screen size
     */
    fun getKeySpacing(context: Context): Pair<Float, Float> {
        val screenSize = getScreenSize(context)

        return when (screenSize) {
            ScreenSize.SMALL -> Pair(2f, 4f)  // Tighter spacing
            ScreenSize.NORMAL -> Pair(Dimensions.KEY_HORIZONTAL_GAP, Dimensions.KEY_VERTICAL_GAP)
            ScreenSize.LARGE -> Pair(4f, 7f)
            ScreenSize.XLARGE -> Pair(5f, 8f)  // More spacing on tablets
        }
    }

    /**
     * Get keyboard padding based on screen size
     */
    fun getKeyboardPadding(context: Context): KeyboardPadding {
        val screenSize = getScreenSize(context)
        val orientation = getOrientation(context)

        return when (screenSize) {
            ScreenSize.SMALL -> {
                KeyboardPadding(2f, 4f, 2f, 6f)
            }
            ScreenSize.NORMAL -> {
                if (orientation == Orientation.LANDSCAPE) {
                    KeyboardPadding(4f, 2f, 4f, 4f)  // Less vertical padding
                } else {
                    KeyboardPadding(
                        Dimensions.KEYBOARD_PADDING_HORIZONTAL,
                        Dimensions.KEYBOARD_PADDING_TOP,
                        Dimensions.KEYBOARD_PADDING_HORIZONTAL,
                        Dimensions.KEYBOARD_PADDING_BOTTOM
                    )
                }
            }
            ScreenSize.LARGE -> {
                KeyboardPadding(6f, 10f, 6f, 10f)
            }
            ScreenSize.XLARGE -> {
                KeyboardPadding(12f, 14f, 12f, 14f)  // More padding on tablets
            }
        }
    }

    /**
     * Get maximum keyboard height ratio (percentage of screen height)
     */
    fun getMaxKeyboardHeightRatio(context: Context): Float {
        val orientation = getOrientation(context)
        val screenSize = getScreenSize(context)

        return when {
            orientation == Orientation.LANDSCAPE -> {
                when (screenSize) {
                    ScreenSize.SMALL -> 0.65f   // 65% of screen height
                    ScreenSize.NORMAL -> 0.60f  // 60% of screen height
                    ScreenSize.LARGE -> 0.55f   // 55% of screen height
                    ScreenSize.XLARGE -> 0.50f  // 50% of screen height
                }
            }
            else -> {  // Portrait
                when (screenSize) {
                    ScreenSize.SMALL -> 0.45f   // 45% of screen height
                    ScreenSize.NORMAL -> 0.40f  // 40% of screen height
                    ScreenSize.LARGE -> 0.38f   // 38% of screen height
                    ScreenSize.XLARGE -> 0.35f  // 35% of screen height
                }
            }
        }
    }

    /**
     * Get text size based on screen size
     */
    fun getKeyTextSize(context: Context): Float {
        val screenSize = getScreenSize(context)

        return when (screenSize) {
            ScreenSize.SMALL -> 18f
            ScreenSize.NORMAL -> Dimensions.KEY_TEXT_SIZE
            ScreenSize.LARGE -> 24f
            ScreenSize.XLARGE -> 26f
        }
    }

    // ==================== ANIMATION SPECIFICATIONS ====================
    object Animations {
        const val KEY_PRESS_DURATION = 50L              // Quick press animation
        const val KEY_RELEASE_DURATION = 100L           // Release animation
        const val RIPPLE_DURATION = 200L                // Ripple effect duration
        const val POPUP_SHOW_DURATION = 100L            // Long-press popup show
        const val POPUP_HIDE_DURATION = 50L             // Popup hide
        const val LAYOUT_TRANSITION_DURATION = 200L     // Layout changes

        // Scale factors
        const val KEY_PRESS_SCALE = 0.95f               // Scale down when pressed
        const val KEY_POPUP_SCALE = 1.2f                // Scale up for popup
    }

    // ==================== ELEVATION ====================
    object Elevation {
        const val KEYBOARD_ELEVATION = 4f               // dp - Main keyboard elevation
        const val KEY_ELEVATION = 1f                    // dp - Individual key elevation
        const val PRESSED_KEY_ELEVATION = 0f            // dp - Pressed key (flat)
        const val POPUP_ELEVATION = 8f                  // dp - Long-press popup
        const val TOOLBAR_ELEVATION = 2f                // dp - Suggestion bar elevation
    }

    /**
     * Data class for keyboard padding
     */
    data class KeyboardPadding(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    /**
     * Create Desh-themed keyboard theme
     */
    fun createDeshTheme(context: Context): KeyboardTheme {
        val screenSize = getScreenSize(context)
        val (horizontalSpacing, verticalSpacing) = getKeySpacing(context)
        val padding = getKeyboardPadding(context)

        return KeyboardTheme(
            id = "desh_kannada",
            name = "Desh Kannada",
            mode = ThemeMode.LIGHT,
            isDynamic = false,
            colors = ThemeColors(
                primary = Colors.ACTION_KEY_BACKGROUND,
                onPrimary = Colors.ACTION_KEY_TEXT,
                primaryContainer = Colors.SPECIAL_KEY_BACKGROUND,
                onPrimaryContainer = Colors.KEY_TEXT,
                secondary = Colors.SPECIAL_KEY_BACKGROUND,
                onSecondary = Colors.KEY_TEXT,
                secondaryContainer = Colors.KEY_BACKGROUND,
                onSecondaryContainer = Colors.KEY_TEXT,
                tertiary = Colors.ACTION_KEY_BACKGROUND,
                onTertiary = Colors.ACTION_KEY_TEXT,
                tertiaryContainer = Colors.SPECIAL_KEY_BACKGROUND,
                onTertiaryContainer = Colors.KEY_TEXT,
                surface = Colors.KEY_BACKGROUND,
                onSurface = Colors.KEY_TEXT,
                surfaceVariant = Colors.KEYBOARD_BACKGROUND,
                onSurfaceVariant = Colors.KEY_TEXT_SECONDARY,
                background = Colors.KEYBOARD_BACKGROUND,
                onBackground = Colors.KEY_TEXT,
                outline = Colors.KEY_BORDER,
                outlineVariant = Colors.SUGGESTION_DIVIDER,
                error = 0xFFB00020.toInt(),
                onError = 0xFFFFFFFF.toInt(),
                errorContainer = 0xFFFFDAD6.toInt(),
                onErrorContainer = 0xFF410002.toInt(),
                keyNormal = Colors.KEY_BACKGROUND,
                keyPressed = Colors.KEY_PRESSED,
                keySelected = Colors.SPECIAL_KEY_BACKGROUND,
                keyBorder = Colors.KEY_BORDER,
                keySelectedBorder = Colors.ACTION_KEY_BACKGROUND,
                ripple = Colors.KEY_PRESSED,
                toolbarBackground = Colors.TOOLBAR_BACKGROUND,
                toolbarIcon = Colors.KEY_TEXT_SECONDARY,
                suggestionBackground = Colors.TOOLBAR_BACKGROUND,
                suggestionText = Colors.SUGGESTION_TEXT,
                suggestionDivider = Colors.SUGGESTION_DIVIDER,
                clipboardBackground = Colors.EMOJI_BAR_BACKGROUND,
                clipboardCard = Colors.KEY_BACKGROUND,
                clipboardBorder = Colors.KEY_BORDER
            ),
            typography = ThemeTypography(
                fontFamily = "google_sans",
                captionSize = Dimensions.KEY_HINT_TEXT_SIZE,
                bodySize = Dimensions.SUGGESTION_TEXT_SIZE,
                buttonSize = getKeyTextSize(context),
                headingSize = Dimensions.SPECIAL_KEY_TEXT_SIZE,
                labelWeight = 400,  // Regular weight
                headingWeight = 500,  // Medium weight
                bodyWeight = 400  // Regular weight
            ),
            shape = ThemeShape(
                keyCornerRadius = Dimensions.KEY_CORNER_RADIUS,
                containerCornerRadius = 0f,  // No rounding for container
                buttonCornerRadius = Dimensions.SPECIAL_KEY_CORNER_RADIUS,
                borderEnabled = true,
                borderWidth = 0.5f
            ),
            spacing = ThemeSpacing(
                keyHorizontalSpacing = horizontalSpacing,
                keyVerticalSpacing = verticalSpacing,
                rowPadding = padding.left,
                containerPadding = padding.top
            ),
            interaction = ThemeInteraction(
                vibrationEnabled = true,
                vibrationIntensity = 0.3f,  // Light haptic
                soundEnabled = false,
                soundVolume = 0.5f,
                rippleDuration = Animations.RIPPLE_DURATION,
                transitionFast = 100L,
                transitionMedium = 200L,
                transitionSlow = 300L
            )
        )
    }
}