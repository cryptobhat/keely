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
        const val KEYBOARD_BACKGROUND = 0xFFEDEFF2.toInt()     // Warm gray from reference UI
        const val KEY_BACKGROUND = 0xFFFFFFFF.toInt()          // Pure white keys
        const val KEY_PRESSED = 0xFFDDE3E7.toInt()            // Subtle press feedback
        const val KEY_TEXT = 0xFF1F252A.toInt()               // Deep charcoal text
        const val KEY_TEXT_SECONDARY = 0xFF7A868F.toInt()     // Muted gray for hints

        // Special keys (shift, delete, ?123, emoji, etc.)
        const val SPECIAL_KEY_BACKGROUND = 0xFFD8E4DB.toInt() // Soft green tint
        const val SPECIAL_KEY_PRESSED = 0xFFC6D6CD.toInt()    // Darker when pressed

        // Action key (enter/search)
        const val ACTION_KEY_BACKGROUND = 0xFF4F7C6D.toInt()  // Deep green accent
        const val ACTION_KEY_PRESSED = 0xFF406759.toInt()     // Darker when pressed
        const val ACTION_KEY_TEXT = 0xFFFFFFFF.toInt()        // White icon/text

        // Toolbar and suggestions
        const val TOOLBAR_BACKGROUND = KEYBOARD_BACKGROUND
        const val SUGGESTION_TEXT = KEY_TEXT
        const val SUGGESTION_DIVIDER = 0xFFD0D7DC.toInt()

        // Shadows and borders
        const val KEY_SHADOW = 0x14000000.toInt()             // Subtle elevation
        const val KEY_BORDER = 0xFFD9E0E5.toInt()

        // Emoji bar
        const val EMOJI_BAR_BACKGROUND = KEYBOARD_BACKGROUND

        // Input field hint
        const val HINT_TEXT = KEY_TEXT_SECONDARY
    }

    // ==================== DIMENSIONS ====================
    object Dimensions {
        // Key dimensions (in dp) - Exact match to Desh reference
        const val KEY_HEIGHT_PHONE_PORTRAIT = 48f        // Matches reference UI exactly
        const val KEY_HEIGHT_PHONE_LANDSCAPE = 40f
        const val KEY_HEIGHT_TABLET = 52f

        // Spacing (in dp) - Very tight like Desh
        const val KEY_HORIZONTAL_GAP = 3f                // Very tight horizontal spacing
        const val KEY_VERTICAL_GAP = 8f                  // Moderate vertical spacing
        const val KEYBOARD_PADDING_HORIZONTAL = 3f       // Almost no side padding
        const val KEYBOARD_PADDING_TOP = 4f              // Minimal top padding
        const val KEYBOARD_PADDING_BOTTOM = 20f          // Large bottom padding like Desh

        // Corner radius (in dp)
        const val KEY_CORNER_RADIUS = 7f                 // Subtle rounded corners
        const val SPECIAL_KEY_CORNER_RADIUS = 7f

        // Shadow (in dp)
        const val KEY_SHADOW_RADIUS = 1.5f
        const val KEY_SHADOW_DY = 0.5f

        // Toolbar height
        const val TOOLBAR_HEIGHT = 42f                   // Compact toolbar
        const val EMOJI_BAR_HEIGHT = 42f

        // Text sizes (in sp)
        const val KEY_TEXT_SIZE = 23f                    // Main key label
        const val KEY_HINT_TEXT_SIZE = 11f               // Small hint text
        const val SUGGESTION_TEXT_SIZE = 15f             // Suggestion text
        const val SPECIAL_KEY_TEXT_SIZE = 19f            // Special key labels
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
                if (orientation == Orientation.PORTRAIT) 42f else 34f
            }
            ScreenSize.NORMAL -> {
                if (orientation == Orientation.PORTRAIT) {
                    Dimensions.KEY_HEIGHT_PHONE_PORTRAIT  // 48f
                } else {
                    Dimensions.KEY_HEIGHT_PHONE_LANDSCAPE  // 40f
                }
            }
            ScreenSize.LARGE -> {
                if (orientation == Orientation.PORTRAIT) 50f else 42f
            }
            ScreenSize.XLARGE -> {
                Dimensions.KEY_HEIGHT_TABLET  // 52f
            }
        }
    }

    /**
     * Get responsive spacing based on screen size
     */
    fun getKeySpacing(context: Context): Pair<Float, Float> {
        val screenSize = getScreenSize(context)

        return when (screenSize) {
            ScreenSize.SMALL -> Pair(2.5f, 7f)
            ScreenSize.NORMAL -> Pair(Dimensions.KEY_HORIZONTAL_GAP, Dimensions.KEY_VERTICAL_GAP)  // 3f, 8f
            ScreenSize.LARGE -> Pair(4f, 9f)
            ScreenSize.XLARGE -> Pair(5f, 10f)
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
                KeyboardPadding(2f, 3f, 2f, 16f)
            }
            ScreenSize.NORMAL -> {
                if (orientation == Orientation.LANDSCAPE) {
                    KeyboardPadding(3f, 4f, 3f, 10f)
                } else {
                    KeyboardPadding(
                        Dimensions.KEYBOARD_PADDING_HORIZONTAL,  // 3f
                        Dimensions.KEYBOARD_PADDING_TOP,         // 4f
                        Dimensions.KEYBOARD_PADDING_HORIZONTAL,  // 3f
                        Dimensions.KEYBOARD_PADDING_BOTTOM       // 20f - Large bottom padding like Desh
                    )
                }
            }
            ScreenSize.LARGE -> {
                KeyboardPadding(4f, 5f, 4f, 22f)
            }
            ScreenSize.XLARGE -> {
                KeyboardPadding(6f, 6f, 6f, 24f)
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
                    ScreenSize.SMALL -> 0.52f   // 52% of screen height
                    ScreenSize.NORMAL -> 0.48f  // 48% of screen height
                    ScreenSize.LARGE -> 0.43f   // 43% of screen height
                    ScreenSize.XLARGE -> 0.38f  // 38% of screen height
                }
            }
            else -> {  // Portrait - Compact but with room for padding
                when (screenSize) {
                    ScreenSize.SMALL -> 0.36f   // 36% of screen height
                    ScreenSize.NORMAL -> 0.34f  // 34% of screen height (room for 20dp bottom padding)
                    ScreenSize.LARGE -> 0.32f   // 32% of screen height
                    ScreenSize.XLARGE -> 0.30f  // 30% of screen height
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
