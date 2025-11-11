package com.kannada.kavi.features.themes

import android.content.Context
import android.graphics.Color
import java.util.concurrent.atomic.AtomicReference

/**
 * KeyboardDesignSystem - Exact screenshot design values
 *
 * Simple design system matching the exact keyboard layout from screenshot.
 * All values extracted from visual analysis of the reference design.
 *
 * Now supports Material You dynamic colors when DynamicThemeManager is provided.
 */
object KeyboardDesignSystem {

    // Thread-safe color scheme storage using AtomicReference
    private val colorSchemeRef = AtomicReference<KeyboardColorScheme>(defaultLightScheme())

    /**
     * Set color scheme (thread-safe)
     * Both dynamic and fallback now use the same mechanism
     */
    fun setDynamicColorScheme(scheme: KeyboardColorScheme?) {
        val actualScheme = scheme ?: defaultLightScheme()
        colorSchemeRef.set(actualScheme)
        android.util.Log.d("KeyboardDesignSystem", "Color scheme updated: dynamic=${scheme != null}, " +
            "keyBg=${Integer.toHexString(actualScheme.keyBackground)}")
    }

    /**
     * Set fallback color scheme (now same as dynamic)
     */
    fun setFallbackColorScheme(scheme: KeyboardColorScheme) {
        colorSchemeRef.set(scheme)
        android.util.Log.d("KeyboardDesignSystem", "Fallback scheme set: keyBg=${Integer.toHexString(scheme.keyBackground)}")
    }

    /**
     * Get current color scheme (thread-safe)
     */
    private fun getColorScheme(): KeyboardColorScheme {
        return colorSchemeRef.get()
    }
    
    private fun defaultLightScheme(): KeyboardColorScheme {
        return KeyboardColorScheme(
            keyBackground = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFE8F5F3.toInt(),
            keyText = 0xFF191C1C.toInt(),
            specialKeyBackground = 0xFFDAE5E3.toInt(),
            specialKeyPressed = 0xFFC5D5D3.toInt(),
            specialKeyText = 0xFF191C1C.toInt(),
            specialKeyIcon = 0xFF191C1C.toInt(),
            actionKeyBackground = 0xFF3F8C80.toInt(),
            actionKeyPressed = 0xFF2F6C60.toInt(),
            actionKeyText = 0xFFFFFFFF.toInt(),
            actionKeyIcon = 0xFFFFFFFF.toInt(),
            spacebarBackground = 0xFFFFFFFF.toInt(),
            spacebarText = 0xFFBBBBBB.toInt(),
            keyHintText = 0xFFAAAAAA.toInt(),
            keyboardBackground = 0xFFF5F5F5.toInt(),
            emojiFill = 0xFFFFEB3B.toInt(),
            emojiOutline = 0xFFF9A825.toInt(),
            emojiEyes = 0xFF000000.toInt(),
            emojiSmile = 0xFF000000.toInt()
        )
    }

    fun getFallbackColorScheme(): KeyboardColorScheme = defaultLightScheme()

    // ==================== COLORS ====================
    object Colors {
        // Regular keys
        const val KEY_BACKGROUND = 0xFFFFFFFF.toInt()  // Pure white
        const val KEY_PRESSED = 0xFFE8F5F3.toInt()  // Light teal tint when pressed
        const val KEY_TEXT = 0xFF191C1C.toInt()  // Near black text
        
        // Special keys (Shift, ?123, Emoji, Language, Backspace)
        const val SPECIAL_KEY_BACKGROUND = 0xFFDAE5E3.toInt()  // Light grey-teal
        const val SPECIAL_KEY_PRESSED = 0xFFC5D5D3.toInt()  // Darker grey-teal when pressed
        const val SPECIAL_KEY_TEXT = 0xFF191C1C.toInt()  // Dark grey/black text
        const val SPECIAL_KEY_ICON = 0xFF191C1C.toInt()  // Dark grey/black icon
        
        // Action key (Search/Enter)
        const val ACTION_KEY_BACKGROUND = 0xFF3F8C80.toInt()  // Dark teal
        const val ACTION_KEY_PRESSED = 0xFF2F6C60.toInt()  // Darker teal when pressed
        const val ACTION_KEY_TEXT = 0xFFFFFFFF.toInt()  // White text
        const val ACTION_KEY_ICON = 0xFFFFFFFF.toInt()  // White icon
        
        // Spacebar
        const val SPACEBAR_BACKGROUND = 0xFFFFFFFF.toInt()  // White (same as regular keys)
        const val SPACEBAR_TEXT = 0xFFBBBBBB.toInt()  // Medium grey text
        
        // Hints (numbers above keys)
        const val KEY_HINT_TEXT = 0xFFAAAAAA.toInt()  // Medium grey
        
        // Keyboard background
        const val KEYBOARD_BACKGROUND = 0xFFF5F5F5.toInt()  // Very light grey-green
        
        // Emoji icon
        const val EMOJI_FILL = 0xFFFFEB3B.toInt()  // Yellow
        const val EMOJI_OUTLINE = 0xFFF9A825.toInt()  // Darker yellow
        const val EMOJI_EYES = 0xFF000000.toInt()  // Black
        const val EMOJI_SMILE = 0xFF000000.toInt()  // Black
        
        // Dynamic color getters (use dynamic colors if available, otherwise static)
        val KEY_BACKGROUND_DYNAMIC: Int get() = getColorScheme().keyBackground
        val KEY_PRESSED_DYNAMIC: Int get() = getColorScheme().keyPressed
        val KEY_TEXT_DYNAMIC: Int get() = getColorScheme().keyText
        val SPECIAL_KEY_BACKGROUND_DYNAMIC: Int get() = getColorScheme().specialKeyBackground
        val SPECIAL_KEY_PRESSED_DYNAMIC: Int get() = getColorScheme().specialKeyPressed
        val SPECIAL_KEY_TEXT_DYNAMIC: Int get() = getColorScheme().specialKeyText
        val SPECIAL_KEY_ICON_DYNAMIC: Int get() = getColorScheme().specialKeyIcon
        val ACTION_KEY_BACKGROUND_DYNAMIC: Int get() = getColorScheme().actionKeyBackground
        val ACTION_KEY_PRESSED_DYNAMIC: Int get() = getColorScheme().actionKeyPressed
        val ACTION_KEY_TEXT_DYNAMIC: Int get() = getColorScheme().actionKeyText
        val ACTION_KEY_ICON_DYNAMIC: Int get() = getColorScheme().actionKeyIcon
        val SPACEBAR_BACKGROUND_DYNAMIC: Int get() = getColorScheme().spacebarBackground
        val SPACEBAR_TEXT_DYNAMIC: Int get() = getColorScheme().spacebarText
        val KEY_HINT_TEXT_DYNAMIC: Int get() = getColorScheme().keyHintText
        val KEYBOARD_BACKGROUND_DYNAMIC: Int get() = getColorScheme().keyboardBackground
        val EMOJI_EYES_DYNAMIC: Int get() = getColorScheme().emojiEyes
        val EMOJI_SMILE_DYNAMIC: Int get() = getColorScheme().emojiSmile
    }

    // ==================== DIMENSIONS ====================
    object Dimensions {
        // Key dimensions - larger chips for better visibility
        const val KEY_HEIGHT_COMPACT = 52f  // 52dp - increased height for better touch targets
        const val KEY_CORNER_RADIUS = 8f  // 8dp - smaller rounded chip corners (matching screenshot)
        const val KEY_INSET = 1.5f  // 1.5dp - creates small visible gaps between keys
        
        // Spacing - consistent small gaps for tighter layout
        // Visible gap = KEY_HORIZONTAL_GAP - (2 * KEY_INSET) = 4 - 3 = 1dp visible gap
        const val KEY_HORIZONTAL_GAP = 4f  // 4dp - horizontal gap between key bounds
        const val ROW_VERTICAL_GAP = 4f  // 4dp - vertical gap between row bounds
        const val KEYBOARD_PADDING_HORIZONTAL = 8f  // 8dp - side padding
        const val KEYBOARD_PADDING_TOP = 8f  // 8dp - top padding for first row
        const val KEYBOARD_PADDING_BOTTOM = 32f  // 32dp - bottom padding after last row (extra generous spacing to prevent cutoff)
        
        // Hint positioning (numbers in top-left)
        const val HINT_OFFSET_LEFT = 4f  // 4dp from left edge
        const val HINT_OFFSET_TOP = 2f  // 2dp from top edge
        
        // Touch targets
        const val TOUCH_PADDING_VERTICAL = 0f  // 0dp - keys are already 48dp (no extra padding needed)
        
        // Icon stroke
        const val ICON_STROKE_WIDTH = 2.5f  // 2.5dp - Material outlined icon stroke
    }

    // ==================== TYPOGRAPHY ====================
    object Typography {
        // Text sizes (in sp)
        const val KEY_LABEL_SIZE = 16f  // Main key labels (q, w, e, etc.)
        const val KEY_HINT_SIZE = 10f  // Number hints (1, 2, 3, etc.)
        const val SPACEBAR_TEXT_SIZE = 14f  // Spacebar label
    }

    // ==================== ANIMATIONS ====================
    object Animations {
        const val KEY_PRESS_SCALE = 0.95f  // Scale down to 95% when pressed
        const val KEY_PRESS_DURATION = 67L  // 67ms - fast press animation
        const val KEY_RELEASE_DURATION = 150L  // 150ms - release animation
        const val RIPPLE_DURATION = 300L  // 300ms - ripple effect
    }

    // ==================== HELPER FUNCTIONS ====================
    
    /**
     * Get keyboard padding
     */
    fun getKeyboardPadding(context: Context): Padding {
        val density = context.resources.displayMetrics.density
        return Padding(
            left = Dimensions.KEYBOARD_PADDING_HORIZONTAL * density,
            top = Dimensions.KEYBOARD_PADDING_TOP * density,
            right = Dimensions.KEYBOARD_PADDING_HORIZONTAL * density,
            bottom = Dimensions.KEYBOARD_PADDING_BOTTOM * density
        )
    }
    
    /**
     * Get maximum keyboard height ratio
     */
    fun getMaxKeyboardHeightRatio(context: Context): Float {
        return 0.5f  // Max 50% of screen height
    }
    
    /**
     * Get text size in pixels
     */
    fun getTextSize(context: Context, textType: TextType): Float {
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        return when (textType) {
            TextType.KEY_LABEL -> Typography.KEY_LABEL_SIZE * scaledDensity
            TextType.KEY_HINT -> Typography.KEY_HINT_SIZE * scaledDensity
            TextType.SPACEBAR -> Typography.SPACEBAR_TEXT_SIZE * scaledDensity
        }
    }
    
    /**
     * Padding data class
     */
    data class Padding(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )
    
    /**
     * Text type enum
     */
    enum class TextType {
        KEY_LABEL,
        KEY_HINT,
        SPACEBAR
    }
}

