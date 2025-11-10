package com.kannada.kavi.ui.keyboardview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.graphics.ColorUtils
import com.kannada.kavi.ui.keyboardview.R
import com.kannada.kavi.core.layout.models.Key
import com.kannada.kavi.core.layout.models.KeyboardRow
import com.kannada.kavi.features.themes.KeyboardDesignSystem
import kotlin.math.min

/**
 * KeyboardView - The Visual Keyboard Component
 *
 * This is what users actually see and touch!
 * It draws the keyboard on screen using Canvas (like painting on a canvas).
 *
 * WHAT IS CANVAS?
 * ===============
 * Canvas is Android's drawing API - like a digital paintbrush.
 * Instead of using XML layouts (which are slow for keyboards),
 * we draw everything manually for maximum speed and control.
 *
 * Think of it like:
 * - XML layouts = LEGO instructions (pre-built, less flexible)
 * - Canvas = Painting (full control, much faster)
 *
 * WHY CANVAS FOR KEYBOARDS?
 * =========================
 * 1. SPEED: We need 60 FPS (frames per second) for smooth typing
 * 2. CONTROL: We need pixel-perfect positioning
 * 3. ANIMATIONS: Smooth key press effects
 * 4. MEMORY: Uses less memory than View-based keyboards
 *
 * HOW IT WORKS:
 * =============
 * 1. Android calls onMeasure() - "How big do you want to be?"
 * 2. Android calls onSizeChanged() - "Here's your size"
 * 3. Android calls onDraw() - "Draw yourself!"
 * 4. We draw each key using Canvas
 * 5. User touches screen
 * 6. Android calls onTouchEvent() - "User touched here!"
 * 7. We figure out which key was pressed
 * 8. Notify the InputMethodService
 * 9. Repeat!
 *
 * PERFORMANCE TARGET:
 * ===================
 * - Frame time: < 16ms (60 FPS)
 * - Touch latency: < 100ms
 * - Memory: < 10MB for keyboard view
 */
class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val LONG_PRESS_TIMEOUT_MS = 350L
        private const val LONG_PRESS_POPUP_OFFSET_DP = 8f
        private const val LONG_PRESS_POPUP_PADDING_DP = 8f
        private const val LONG_PRESS_OPTION_MARGIN_DP = 4f
        private const val LONG_PRESS_OPTION_PADDING_DP = 10f
    }

    // Keyboard data
    private var rows: List<KeyboardRow> = emptyList()
    private var keyBounds: MutableList<KeyBound> = mutableListOf()
    private var currentEnterAction: Int = android.view.inputmethod.EditorInfo.IME_ACTION_NONE
    private var isEmojiBoardVisible: Boolean = false

    // Height adjustment (percentage: 70-130, default 100)
    private var heightPercentage: Int = 100

    // Design system - exact screenshot values

    // Material You Paint objects (reused for performance)
    private val keyBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val keyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val keySelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Material You uses minimal borders or no borders
    private val keyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    // Typography paint with proper Material You font settings
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        // Material You uses medium weight (500) for key labels
        typeface = android.graphics.Typeface.DEFAULT
        // Improve glyph rendering for complex scripts
        isSubpixelText = true
        isLinearText = true
    }

    // Hint text paint (number hints etc.)
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
    }

    // Space bar label paint
    private val spaceBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT
        isSubpixelText = true
        isLinearText = true
    }

    // Material You ripple effect
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Long press support
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var longPressTarget: KeyBound? = null
    private var longPressPopup: PopupWindow? = null

    // Swipe typing and gesture support
    private var swipeGestureDetector: SwipeGestureDetector? = null
    private var swipePathView: SwipePathView? = null
    private var isSwipeTypingEnabled = false
    private var isGesturesEnabled = false

    // Swipe word callback - will be set by IME service
    private var onSwipeWord: ((String) -> Unit)? = null

    // Material You elevation shadow (very subtle, tonal)
    private val keyShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // Material You uses very subtle shadows
    }

    // Material You tonal elevation paint for surface tints
    private val elevationTintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val keyHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Special keys use surface variant color
    private val specialKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val specialKeyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Spacebar paint (for text color - spacebar background is white)
    private val spacebarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Action key uses primary color
    private val actionKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val actionKeyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Material UI outlined icon style
    private val iconStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 2.5f  // Material outlined icon standard stroke (will be scaled by density)
    }

    private val specialKeyTypes = setOf(
        com.kannada.kavi.core.layout.models.KeyType.SHIFT,
        com.kannada.kavi.core.layout.models.KeyType.DELETE,
        com.kannada.kavi.core.layout.models.KeyType.SYMBOLS,
        com.kannada.kavi.core.layout.models.KeyType.SYMBOLS_EXTRA,
        com.kannada.kavi.core.layout.models.KeyType.SYMBOLS_ALT,
        com.kannada.kavi.core.layout.models.KeyType.DEFAULT,
        com.kannada.kavi.core.layout.models.KeyType.LANGUAGE,
        com.kannada.kavi.core.layout.models.KeyType.EMOJI,
        com.kannada.kavi.core.layout.models.KeyType.VOICE,
        com.kannada.kavi.core.layout.models.KeyType.SETTINGS,
        com.kannada.kavi.core.layout.models.KeyType.CLIPBOARD
    )

    // Currently pressed key (for visual feedback)
    private var pressedKey: Key? = null
    private var pressedKeyBounds: RectF? = null
    private var selectedKey: Key? = null

    // Ripple animation
    private var rippleAnimator: ValueAnimator? = null
    private var rippleRadius = 0f
    private var rippleX = 0f
    private var rippleY = 0f

    // Key press animation
    private var keyPressAnimator: ValueAnimator? = null
    private var keyPressScale = 1.0f
    private var animatingKey: Key? = null

    // Key listener (sends key presses to InputMethodService)
    private var keyPressListener: ((Key) -> Unit)? = null

    // Keyboard dimensions
    private var keyHeight = 0f
    private var keyboardWidth = 0f
    private var keyboardHeight = 0f

    // Chip-style spacing (from screenshot - visible gaps between keys)
    private var keyHorizontalSpacing = 6f  // 6dp base spacing + inset = visible chip gaps
    private var keyVerticalSpacing = 6f   // 6dp base spacing + inset = visible chip gaps
    private var rowPaddingStart = 8f      // 8dp keyboard inset
    private var rowPaddingEnd = 8f        // 8dp keyboard inset

    private val density: Float
        get() = resources.displayMetrics.density

    // Current layout name to display on spacebar
    private var currentLayoutName: String = ""

    init {
        // Apply colors and setup
        applyColors()

        // Apply padding for chip-style keyboard (top and bottom padding for proper spacing)
        val padding = KeyboardDesignSystem.getKeyboardPadding(context)
        setPadding(
            padding.left.toInt(),
            padding.top.toInt(),  // 8dp - top padding for first row
            padding.right.toInt(),
            padding.bottom.toInt()  // 8dp - bottom padding after last row
        )

        // Set background
        setBackgroundColor(KeyboardDesignSystem.Colors.KEYBOARD_BACKGROUND_DYNAMIC)

        // Initialize swipe gesture detector
        initializeSwipeComponents()
    }

    /**
     * Initialize swipe typing and gesture components
     */
    private fun initializeSwipeComponents() {
        // Create swipe gesture detector
        swipeGestureDetector = SwipeGestureDetector().apply {
            setDensity(density)
            setListener(object : SwipeGestureDetector.GestureListener {
                override fun onTap(x: Float, y: Float) {
                    // Handle as normal tap
                    handleTouchDown(x, y)
                    handleTouchUp(x, y)
                }

                override fun onLongPress(x: Float, y: Float) {
                    // Handle long press
                    handleTouchDown(x, y)
                }

                override fun onSwipeStart(x: Float, y: Float) {
                    // Start swipe path visualization
                    swipePathView?.startSwipe(x, y)
                }

                override fun onSwipeMove(x: Float, y: Float, path: List<android.graphics.PointF>) {
                    // Update swipe path visualization
                    swipePathView?.updatePath(x, y)
                }

                override fun onSwipeEnd(gesture: SwipeGesture) {
                    // End swipe and extract text
                    swipePathView?.endSwipe()

                    // Only handle swipe typing gestures
                    if (gesture.type == SwipeType.SWIPE_TYPE) {
                        // Get letters from path
                        val word = extractWordFromPath(gesture.path)
                        if (word.isNotEmpty()) {
                            onSwipeWord?.invoke(word)
                        }
                    }
                }

                override fun onSwipeCancel() {
                    // Cancel swipe
                    swipePathView?.cancelSwipe()
                }
            })
        }
    }

    /**
     * Extract word from swipe path by checking which keys were touched
     */
    private fun extractWordFromPath(path: List<android.graphics.PointF>): String {
        val letters = mutableListOf<String>()
        var lastKey: Key? = null

        path.forEach { point ->
            findKeyAt(point.x, point.y)?.let { keyBound ->
                // Only add if it's a different key than last
                if (keyBound.key != lastKey && keyBound.key.label.length == 1) {
                    letters.add(keyBound.key.label)
                    lastKey = keyBound.key
                }
            }
        }

        return letters.joinToString("")
    }

    /**
     * Set theme (no-op - using design system constants)
     */
    fun setTheme(theme: Any) {
        // Design system values are constants - just reapply colors
        applyColors()
        calculateKeyBounds()
        setBackgroundColor(KeyboardDesignSystem.Colors.KEYBOARD_BACKGROUND_DYNAMIC)
        invalidate()
    }

    /**
     * Apply colors to all Paint objects from design system
     * Uses dynamic colors if available, otherwise falls back to static colors
     */
    private fun applyColors() {
        val density = resources.displayMetrics.density

        // Apply colors from design system (dynamic if available)
        keyBackgroundPaint.color = KeyboardDesignSystem.Colors.KEY_BACKGROUND_DYNAMIC
        keyPressedPaint.color = KeyboardDesignSystem.Colors.KEY_PRESSED_DYNAMIC
        keySelectedPaint.color = KeyboardDesignSystem.Colors.SPECIAL_KEY_BACKGROUND_DYNAMIC
        specialKeyPaint.color = KeyboardDesignSystem.Colors.SPECIAL_KEY_BACKGROUND_DYNAMIC
        specialKeyPressedPaint.color = KeyboardDesignSystem.Colors.SPECIAL_KEY_PRESSED_DYNAMIC
        actionKeyPaint.color = KeyboardDesignSystem.Colors.ACTION_KEY_BACKGROUND_DYNAMIC
        actionKeyPressedPaint.color = KeyboardDesignSystem.Colors.ACTION_KEY_PRESSED_DYNAMIC
        spacebarPaint.color = KeyboardDesignSystem.Colors.SPACEBAR_TEXT_DYNAMIC

        keyBorderPaint.apply {
            color = 0x00000000.toInt()  // No border
            strokeWidth = 0f
        }

        iconStrokePaint.strokeWidth = KeyboardDesignSystem.Dimensions.ICON_STROKE_WIDTH * density

        labelPaint.apply {
            color = KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC
            textSize = KeyboardDesignSystem.getTextSize(context, KeyboardDesignSystem.TextType.KEY_LABEL)
            typeface = android.graphics.Typeface.DEFAULT
            isSubpixelText = true
            isLinearText = true
        }

        hintPaint.apply {
            color = KeyboardDesignSystem.Colors.KEY_HINT_TEXT_DYNAMIC
            textSize = KeyboardDesignSystem.getTextSize(context, KeyboardDesignSystem.TextType.KEY_HINT)
        }

        spaceBarPaint.apply {
            color = KeyboardDesignSystem.Colors.SPACEBAR_TEXT_DYNAMIC
            textSize = KeyboardDesignSystem.getTextSize(context, KeyboardDesignSystem.TextType.SPACEBAR)
        }

        ripplePaint.color = 0x1F000000.toInt()  // Light ripple

        keyShadowPaint.color = 0x00000000.toInt()  // No shadow

        // Apply chip-style spacing from design system, scaled by heightPercentage
        keyHorizontalSpacing = KeyboardDesignSystem.Dimensions.KEY_HORIZONTAL_GAP * density
        keyVerticalSpacing = (KeyboardDesignSystem.Dimensions.ROW_VERTICAL_GAP * density * heightPercentage / 100f)
        val padding = KeyboardDesignSystem.getKeyboardPadding(context)
        rowPaddingStart = padding.left
        rowPaddingEnd = padding.right

        setLayerType(LAYER_TYPE_HARDWARE, null)
    }
    
    /**
     * Refresh colors (call when dynamic theme changes)
     */
    fun refreshColors() {
        applyColors()
        setBackgroundColor(KeyboardDesignSystem.Colors.KEYBOARD_BACKGROUND_DYNAMIC)
        invalidate()
    }

    /**
     * Set the keyboard rows to display
     *
     * @param rows List of keyboard rows from LayoutManager
     */
    fun setKeyboard(rows: List<KeyboardRow>) {
        android.util.Log.e("KeyboardView", "===== SET KEYBOARD CALLED with ${rows.size} rows =====")
        android.util.Log.d("KeyboardView", "setKeyboard: Old rows count = ${this.rows.size}, New rows count = ${rows.size}")
        this.rows = rows
        calculateKeyBounds()
        invalidate() // Request redraw
        requestLayout() // Force layout recalculation
        android.util.Log.e("KeyboardView", "===== SET KEYBOARD COMPLETE - invalidate() and requestLayout() called =====")
    }

    /**
     * Set the current layout name to display on spacebar
     *
     * @param layoutName The name of the current layout (e.g., "Phonetic", "Kavi", "QWERTY")
     */
    fun setLayoutName(layoutName: String) {
        android.util.Log.d("KeyboardView", "setLayoutName: Setting to '$layoutName'")
        this.currentLayoutName = layoutName
        invalidate() // Redraw to update spacebar label
        android.util.Log.d("KeyboardView", "setLayoutName: Spacebar label updated")
    }
    
    /**
     * Set the enter key action type for context-aware icon
     */
    fun setEnterAction(action: Int) {
        currentEnterAction = action
        invalidate()
    }
    
    /**
     * Set emoji board visibility state for icon toggle
     */
    fun setEmojiBoardVisible(visible: Boolean) {
        isEmojiBoardVisible = visible
        invalidate()
    }

    /**
     * Set keyboard height percentage (70-130%)
     *
     * @param percentage Height percentage (70 = 70%, 100 = default, 130 = 130%)
     */
    fun setKeyboardHeightPercentage(percentage: Int) {
        val clampedPercentage = percentage.coerceIn(70, 130)
        if (heightPercentage != clampedPercentage) {
            heightPercentage = clampedPercentage
            requestLayout() // Trigger re-measure with new height
        }
    }

    /**
     * Get current keyboard height percentage
     */
    fun getKeyboardHeightPercentage(): Int = heightPercentage

    /**
     * Set listener for key press events
     *
     * @param listener Lambda that receives the pressed key
     */
    fun setOnKeyPressListener(listener: (Key) -> Unit) {
        this.keyPressListener = listener
    }

    /**
     * Enable or disable swipe typing
     */
    fun setSwipeTypingEnabled(enabled: Boolean) {
        isSwipeTypingEnabled = enabled
        if (enabled) {
            // Update key bounds for swipe word predictor
            updateSwipeKeyBounds()
        }
    }

    /**
     * Enable or disable gestures
     */
    fun setGesturesEnabled(enabled: Boolean) {
        isGesturesEnabled = enabled
    }

    /**
     * Set swipe path view for visual feedback
     */
    fun setSwipePathView(pathView: SwipePathView) {
        swipePathView = pathView
    }

    /**
     * Set swipe word callback
     */
    fun setOnSwipeWordListener(listener: (String) -> Unit) {
        onSwipeWord = listener
    }

    /**
     * Update key bounds for swipe typing (no-op for now)
     */
    private fun updateSwipeKeyBounds() {
        // Placeholder - will be used when integrating SwipeWordPredictor
    }

    /**
     * Calculate bounds (position and size) for each key
     *
     * This is like creating a blueprint before building.
     * We figure out where each key should be drawn.
     * Now uses theme spacing values for Material You design.
     *
     * IMPORTANT: Keys must align in a proper grid across all rows
     * Similar to Gboard and the reference design
     */
    private fun calculateKeyBounds() {
        keyBounds.clear()

        if (rows.isEmpty()) return

        // Calculate key dimensions
        // Remove horizontal insets to eliminate unwanted padding
        val horizontalInsetStart = 0f
        val horizontalInsetEnd = 0f
        val availableWidth = width - (paddingLeft + paddingRight)
        val availableHeight = height - (paddingTop + paddingBottom)

        // Use compact key height scaled by heightPercentage
        val compactKeyHeightDp = KeyboardDesignSystem.Dimensions.KEY_HEIGHT_COMPACT
        keyHeight = (compactKeyHeightDp * density * heightPercentage / 100f)

        // Calculate base unit width from first row to fill available width
        // This ensures consistent key sizing across all rows
        val firstRow = rows.firstOrNull()
        val firstRowSumUnits = firstRow?.keys?.fold(0f) { acc, key -> acc + key.width } ?: 0f
        val firstRowSpaces = if (firstRow != null && firstRow.keyCount > 0) (firstRow.keyCount - 1) * keyHorizontalSpacing else 0f
        
        val baseUnitWidth = if (firstRow != null && firstRow.keyCount > 0 && firstRowSumUnits > 0f) {
            // Calculate unit width to fill available width
            val firstRowEffectiveWidth = (availableWidth - firstRowSpaces).coerceAtLeast(0f)
            (firstRowEffectiveWidth / firstRowSumUnits).coerceAtLeast(0.5f)
        } else {
            0f
        }

        // Calculate first row start position
        // First row starts from the left edge (paddingLeft only, no extra insets)
        val firstRowActualWidth = firstRowSumUnits * baseUnitWidth + firstRowSpaces
        val firstRowStartX = paddingLeft.toFloat()

        // Start from top with minimal padding
        var currentY = paddingTop.toFloat()

        rows.forEachIndexed { rowIndex, row ->
            val isLastRow = rowIndex == rows.size - 1

            // Calculate unit width for this row
            // Last row needs its own calculation to fill width properly
            val unitWidth = if (isLastRow) {
                // Calculate total units for bottom row from JSON widths
                val bottomRowUnits = row.keys.fold(0f) { acc, key -> acc + key.width }
                val bottomRowSpaces = if (row.keyCount > 0) (row.keyCount - 1) * keyHorizontalSpacing else 0f
                val bottomRowEffectiveWidth = (availableWidth - bottomRowSpaces).coerceAtLeast(0f)
                (bottomRowEffectiveWidth / bottomRowUnits).coerceAtLeast(0.5f)
            } else {
                // Other rows use base unit width from first row
                baseUnitWidth
            }

            // Calculate key widths using JSON values
            val keyWidths = row.keys.map { key -> unitWidth * key.width }

            // Calculate the actual width this row will occupy using calculated widths
            var rowActualWidth = keyWidths.sum()
            // Add spacing between keys (not after the last key)
            if (row.keyCount > 0) rowActualWidth += (row.keyCount - 1) * keyHorizontalSpacing

            // Calculate row start position for perfect QWERTY alignment
            // Standard QWERTY layout alignment:
            // - Row 0 (QWERTY): Starts at left edge
            // - Row 1 (ASDF): Offset by 0.5 key width (so 'a' sits between 'q' and 'w')
            // - Row 2 (ZXCV): The shift key takes up space, so 'z' naturally aligns correctly
            //                 Row starts at left edge, shift pushes letters to the right position
            // - Row 3+ (bottom row): Starts at left edge
            val rowStartX = when (rowIndex) {
                1 -> {
                    // Row 1 (ASDF row): Offset by half a key width
                    // This centers 'a' between 'q' and 'w', 'l' between 'o' and 'p'
                    paddingLeft.toFloat() + (unitWidth * 0.5f)
                }
                else -> {
                    // Row 0 (QWERTY), Row 2 (ZXCV with shift), Row 3+ (bottom row): Start from left edge
                    // Row 2 shift key automatically positions 'z' correctly between 'a' and 's'
                    paddingLeft.toFloat()
                }
            }

            var currentX = rowStartX

            row.keys.forEachIndexed { keyIndex, key ->
                // Use pre-calculated width
                val keyActualWidth = keyWidths[keyIndex]

                // Create bounds for this key
                val bounds = RectF(
                    currentX,
                    currentY,
                    currentX + keyActualWidth,
                    currentY + keyHeight
                )

                keyBounds.add(KeyBound(key, bounds))

                // Move to next key position (add spacing only if not the last key)
                currentX += keyActualWidth
                if (keyIndex < row.keyCount - 1) {
                    currentX += keyHorizontalSpacing
                }
            }

            currentY += keyHeight + keyVerticalSpacing
        }

        // Update swipe key bounds if swipe typing is enabled
        if (isSwipeTypingEnabled) {
            updateSwipeKeyBounds()
        }
    }

    private fun drawCustomIcon(canvas: Canvas, key: Key, bounds: RectF): Boolean {
        // Use text-based icons for reliability - they always render correctly
        val iconText = when (key.type) {
            com.kannada.kavi.core.layout.models.KeyType.ENTER -> {
                // Context-aware enter icon based on IME_ACTION
                when (currentEnterAction and android.view.inputmethod.EditorInfo.IME_MASK_ACTION) {
                    android.view.inputmethod.EditorInfo.IME_ACTION_SEND -> "📤" // Send
                    android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH -> "🔍" // Search
                    android.view.inputmethod.EditorInfo.IME_ACTION_GO -> "➡️" // Go
                    android.view.inputmethod.EditorInfo.IME_ACTION_NEXT -> "⏭️" // Next
                    android.view.inputmethod.EditorInfo.IME_ACTION_DONE -> "✓" // Done
                    else -> "⏎" // Default: return/newline
                }
            }
            com.kannada.kavi.core.layout.models.KeyType.DELETE -> "⌫"
            com.kannada.kavi.core.layout.models.KeyType.SHIFT -> "⇧"
            com.kannada.kavi.core.layout.models.KeyType.LANGUAGE -> "🌐"
            com.kannada.kavi.core.layout.models.KeyType.EMOJI -> {
                // Toggle icon: show keyboard icon when emoji board is visible, emoji when keyboard is visible
                if (isEmojiBoardVisible) "⌨️" else "😊"
            }
            else -> null
        }
        
        if (iconText != null) {
            val iconColor = when (key.type) {
                com.kannada.kavi.core.layout.models.KeyType.ENTER -> KeyboardDesignSystem.Colors.ACTION_KEY_ICON_DYNAMIC
                com.kannada.kavi.core.layout.models.KeyType.DELETE,
                com.kannada.kavi.core.layout.models.KeyType.SHIFT -> KeyboardDesignSystem.Colors.SPECIAL_KEY_ICON_DYNAMIC
                else -> KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC
            }
            
            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = iconColor
                textAlign = Paint.Align.CENTER
                textSize = min(bounds.width(), bounds.height()) * 0.5f
                typeface = Typeface.DEFAULT_BOLD
            }
            
            val textY = bounds.centerY() - ((iconPaint.descent() + iconPaint.ascent()) / 2)
            canvas.drawText(iconText, bounds.centerX(), textY, iconPaint)
            return true
        }
        
        // Fallback to custom drawing for other types
        return when (key.type) {
            else -> false
        }
    }

    /**
     * Draw Material Icons return/enter icon (keyboard_return style)
     * Simplified, reliable rendering
     */
    private fun drawSearchIcon(canvas: Canvas, bounds: RectF) {
        val d = density
        val iconColor = KeyboardDesignSystem.Colors.ACTION_KEY_ICON_DYNAMIC
        
        iconStrokePaint.style = Paint.Style.STROKE
        iconStrokePaint.strokeWidth = 3.5f * d
        iconStrokePaint.strokeCap = Paint.Cap.ROUND
        iconStrokePaint.strokeJoin = Paint.Join.ROUND
        iconStrokePaint.color = iconColor

        val size = min(bounds.width(), bounds.height()) * 0.5f
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val halfSize = size * 0.5f

        // Simple L-shape with arrow - more reliable coordinates
        val path = Path().apply {
            // Top horizontal line
            moveTo(centerX - halfSize, centerY - size * 0.15f)
            lineTo(centerX + halfSize * 0.6f, centerY - size * 0.15f)
            
            // Right vertical line down
            lineTo(centerX + halfSize * 0.6f, centerY + size * 0.35f)
            
            // Arrow head pointing down-left
            lineTo(centerX - halfSize * 0.1f, centerY + size * 0.1f)
            moveTo(centerX + halfSize * 0.6f, centerY + size * 0.35f)
            lineTo(centerX + halfSize * 0.25f, centerY + size * 0.5f)
        }
        canvas.drawPath(path, iconStrokePaint)
    }

    /**
     * Draw Material Icons backspace icon
     * Simplified, reliable rendering
     */
    private fun drawDeleteIcon(canvas: Canvas, bounds: RectF) {
        val d = density
        val iconColor = KeyboardDesignSystem.Colors.SPECIAL_KEY_ICON_DYNAMIC
        
        iconStrokePaint.style = Paint.Style.STROKE
        iconStrokePaint.strokeWidth = 3.5f * d
        iconStrokePaint.strokeCap = Paint.Cap.ROUND
        iconStrokePaint.strokeJoin = Paint.Join.ROUND
        iconStrokePaint.color = iconColor

        val size = min(bounds.width(), bounds.height()) * 0.5f
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val halfSize = size * 0.5f

        // Left arrow with X - simplified coordinates
        val path = Path().apply {
            // Arrow head (pointing left)
            moveTo(centerX - halfSize, centerY)
            lineTo(centerX - halfSize * 0.3f, centerY - halfSize * 0.35f)
            
            moveTo(centerX - halfSize, centerY)
            lineTo(centerX - halfSize * 0.3f, centerY + halfSize * 0.35f)

            // Arrow shaft (horizontal line)
            moveTo(centerX - halfSize * 0.3f, centerY)
            lineTo(centerX + halfSize * 0.6f, centerY)
        }
        canvas.drawPath(path, iconStrokePaint)

        // X mark on right side
        val xSize = halfSize * 0.2f
        val xOffset = halfSize * 0.4f
        
        canvas.drawLine(
            centerX + xOffset - xSize, centerY - xSize,
            centerX + xOffset + xSize, centerY + xSize,
            iconStrokePaint
        )
        canvas.drawLine(
            centerX + xOffset - xSize, centerY + xSize,
            centerX + xOffset + xSize, centerY - xSize,
            iconStrokePaint
        )
    }

    /**
     * Draw Material Icons shift icon (arrow_upward style)
     * Simplified, reliable rendering
     */
    private fun drawShiftIcon(canvas: Canvas, bounds: RectF) {
        val d = density
        val iconColor = KeyboardDesignSystem.Colors.SPECIAL_KEY_ICON_DYNAMIC
        
        iconStrokePaint.style = Paint.Style.STROKE
        iconStrokePaint.strokeWidth = 3.5f * d
        iconStrokePaint.strokeCap = Paint.Cap.ROUND
        iconStrokePaint.strokeJoin = Paint.Join.ROUND
        iconStrokePaint.color = iconColor

        val size = min(bounds.width(), bounds.height()) * 0.5f
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val halfSize = size * 0.5f

        // Upward arrow - simplified coordinates
        val path = Path().apply {
            // Arrow head (pointing up)
            moveTo(centerX, centerY - halfSize * 0.5f)
            lineTo(centerX - halfSize * 0.4f, centerY + halfSize * 0.1f)
            
            moveTo(centerX, centerY - halfSize * 0.5f)
            lineTo(centerX + halfSize * 0.4f, centerY + halfSize * 0.1f)

            // Arrow shaft (vertical line)
            moveTo(centerX, centerY - halfSize * 0.3f)
            lineTo(centerX, centerY + halfSize * 0.5f)
        }
        canvas.drawPath(path, iconStrokePaint)
    }

    /**
     * Draw Material Icons globe/language icon (language style)
     * Simplified, reliable rendering
     */
    private fun drawLanguageIcon(canvas: Canvas, bounds: RectF) {
        val d = density
        val iconColor = KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC
        
        iconStrokePaint.style = Paint.Style.STROKE
        iconStrokePaint.strokeWidth = 3.5f * d
        iconStrokePaint.strokeCap = Paint.Cap.ROUND
        iconStrokePaint.strokeJoin = Paint.Join.ROUND
        iconStrokePaint.color = iconColor

        val size = min(bounds.width(), bounds.height()) * 0.5f
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val radius = size * 0.4f

        // Draw main circle (globe outline)
        canvas.drawCircle(centerX, centerY, radius, iconStrokePaint)

        // Draw horizontal line (equator)
        canvas.drawLine(
            centerX - radius * 0.95f, centerY,
            centerX + radius * 0.95f, centerY,
            iconStrokePaint
        )

        // Draw vertical meridian arc
        val meridianPath = Path().apply {
            addArc(
                RectF(
                    centerX - radius * 0.5f, centerY - radius,
                    centerX + radius * 0.5f, centerY + radius
                ),
                -90f, 180f
            )
        }
        canvas.drawPath(meridianPath, iconStrokePaint)
    }

    /**
     * Draw Material Icons emoji icon (mood style)
     * Simplified, reliable rendering
     */
    private fun drawEmojiIcon(canvas: Canvas, bounds: RectF) {
        val d = density
        val iconColor = KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC
        val eyeColor = KeyboardDesignSystem.Colors.EMOJI_EYES_DYNAMIC
        
        val size = min(bounds.width(), bounds.height()) * 0.5f
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val faceRadius = size * 0.4f

        // Draw face circle outline
        iconStrokePaint.style = Paint.Style.STROKE
        iconStrokePaint.strokeWidth = 3.5f * d
        iconStrokePaint.strokeCap = Paint.Cap.ROUND
        iconStrokePaint.color = iconColor
        canvas.drawCircle(centerX, centerY, faceRadius, iconStrokePaint)

        // Draw eyes (filled circles)
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = eyeColor
        }
        val eyeRadius = faceRadius * 0.2f
        val eyeOffsetX = faceRadius * 0.35f
        val eyeOffsetY = faceRadius * 0.2f

        canvas.drawCircle(centerX - eyeOffsetX, centerY - eyeOffsetY, eyeRadius, eyePaint)
        canvas.drawCircle(centerX + eyeOffsetX, centerY - eyeOffsetY, eyeRadius, eyePaint)

        // Draw smile (arc)
        val smilePath = Path().apply {
            addArc(
                RectF(
                    centerX - faceRadius * 0.55f,
                    centerY - faceRadius * 0.05f,
                    centerX + faceRadius * 0.55f,
                    centerY + faceRadius * 0.7f
                ),
                0f, 180f
            )
        }
        canvas.drawPath(smilePath, iconStrokePaint)
    }

    /**
     * onMeasure - Tell Android how big we want to be
     *
     * Uses Desh design system responsive sizing:
     * - Adapts to screen size and orientation
     * - Respects maximum height constraints
     * - Provides optimal key sizes for each device
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val maxHeight = MeasureSpec.getSize(heightMeasureSpec)

        // Calculate desired height based on number of rows and screen size
        val rowCount = rows.size.coerceAtLeast(4) // At least 4 rows
        val density = resources.displayMetrics.density

        // Compact chip-style key height (from screenshot - smaller keys), scaled by heightPercentage
        val keyHeightDp = KeyboardDesignSystem.Dimensions.KEY_HEIGHT_COMPACT  // 42dp for compact chips
        val keyHeightPx = (keyHeightDp * density * heightPercentage / 100f)

        // Chip-style spacing, scaled by heightPercentage
        val verticalSpacingPx = (KeyboardDesignSystem.Dimensions.ROW_VERTICAL_GAP * density * heightPercentage / 100f)

        // Minimal padding - no top padding, only bottom for gesture nav
        val padding = KeyboardDesignSystem.getKeyboardPadding(context)
        val paddingTopPx = padding.top  // 0dp - no top padding
        val paddingBottomPx = padding.bottom

        // Calculate total height (compact chip-style)
        val totalSpacing = (rowCount - 1) * verticalSpacingPx
        var desiredHeight = ((rowCount * keyHeightPx) + totalSpacing + paddingTopPx + paddingBottomPx).toInt()

        // Apply maximum height constraint to prevent keyboard from being too tall
        val maxKeyboardHeightRatio = KeyboardDesignSystem.getMaxKeyboardHeightRatio(context)
        val screenHeight = resources.displayMetrics.heightPixels
        val maxKeyboardHeight = (screenHeight * maxKeyboardHeightRatio).toInt()

        // Ensure we don't exceed maximum height
        if (desiredHeight > maxKeyboardHeight) {
            desiredHeight = maxKeyboardHeight
        }

        // Also respect the height constraint from the measure spec
        if (maxHeight > 0 && desiredHeight > maxHeight) {
            desiredHeight = maxHeight
        }

        // Note: Height percentage is already applied to keyHeightPx and verticalSpacingPx above
        // Do NOT apply it again here or it will cause double scaling

        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    /**
     * onSizeChanged - Called when our size changes
     *
     * Recalculate key positions when size changes
     * (e.g., when screen rotates)
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        keyboardWidth = w.toFloat()
        keyboardHeight = h.toFloat()
        calculateKeyBounds()
    }

    /**
     * onDraw - THE MAIN DRAWING METHOD!
     *
     * This is where we paint the keyboard on screen.
     * Called 60 times per second for smooth animation!
     *
     * IMPORTANT: Keep this FAST! No heavy operations here!
     *
     * Now with Material You:
     * - Theme colors
     * - Ripple animation
     * - Selected state
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(KeyboardDesignSystem.Colors.KEYBOARD_BACKGROUND_DYNAMIC)

        // Draw ripple effect (if active)
        if (rippleRadius > 0) {
            canvas.drawCircle(rippleX, rippleY, rippleRadius, ripplePaint)
        }

        // Draw each key
        keyBounds.forEach { keyBound ->
            drawKey(canvas, keyBound)
        }
    }

    /**
     * Draw a single key - Material You Design
     *
     * @param canvas The canvas to draw on
     * @param keyBound The key and its bounds
     *
     * Material You keyboard design:
     * - 8dp corner radius (smooth, modern)
     * - Tonal elevation instead of shadows
     * - Surface tints for depth
     * - No borders (clean appearance)
     * - Proper contrast ratios (WCAG AA)
     * - Clean, minimal Material You aesthetic
     */
    private fun drawKey(canvas: Canvas, keyBound: KeyBound) {
        val key = keyBound.key
        val bounds = keyBound.bounds

        // Subtle inset to create gutters between keycaps
        val inset = KeyboardDesignSystem.Dimensions.KEY_INSET * density
        val drawBounds = RectF(
            bounds.left + inset,
            bounds.top + inset,
            bounds.right - inset,
            bounds.bottom - inset
        )

        val scale = if (key == animatingKey) keyPressScale else 1f
        if (scale != 1f) {
            val dx = drawBounds.width() * (1 - scale) / 2f
            val dy = drawBounds.height() * (1 - scale) / 2f
            drawBounds.inset(dx, dy)
        }

        val cornerRadius = KeyboardDesignSystem.Dimensions.KEY_CORNER_RADIUS * density  // Use chip-style radius
        val isActionKey = key.type == com.kannada.kavi.core.layout.models.KeyType.ENTER
        val isUtilityKey = key.type in specialKeyTypes
        val isSpaceKey = key.type == com.kannada.kavi.core.layout.models.KeyType.SPACE

        val backgroundPaint = when {
            isActionKey -> if (key == pressedKey) actionKeyPressedPaint else actionKeyPaint
            isUtilityKey -> if (key == pressedKey) specialKeyPressedPaint else specialKeyPaint
            isSpaceKey -> keyBackgroundPaint  // Spacebar is white (not light beige) from screenshot
            else -> if (key == pressedKey) keyPressedPaint else keyBackgroundPaint
        }

        // Draw subtle elevation shadow (only if not pressed)
        if (key != pressedKey && key != animatingKey) {
            val shadowOffsetY = 0.3f * density  // 0.3dp vertical offset for very subtle elevation
            val shadowBlur = 0.5f * density  // Minimal blur effect
            val shadowOpacity = 0.05f  // 5% opacity for very subtle shadow
            
            keyShadowPaint.color = ColorUtils.setAlphaComponent(0xFF000000.toInt(), (255 * shadowOpacity).toInt())
            keyShadowPaint.style = Paint.Style.FILL
            
            // Draw shadow slightly offset below (very subtle elevation effect)
            val shadowBounds = RectF(
                drawBounds.left,
                drawBounds.top + shadowOffsetY,
                drawBounds.right,
                drawBounds.bottom + shadowOffsetY + shadowBlur
            )
            
            // Draw shadow with rounded corners (very subtle elevation effect)
            canvas.drawRoundRect(shadowBounds, cornerRadius, cornerRadius, keyShadowPaint)
        }
        
        // Keycap (chip-style rounded rectangle)
        canvas.drawRoundRect(drawBounds, cornerRadius, cornerRadius, backgroundPaint)

        // Optional hint (e.g., numeric hint on top row) - top-left corner, small gray text
        key.hint?.takeIf { it.isNotBlank() }?.let { hint ->
            // Position hint in top-left corner (from screenshot)
            // Use drawBounds (after inset) for proper positioning
            val hintOffsetX = KeyboardDesignSystem.Dimensions.HINT_OFFSET_LEFT * density
            val hintOffsetY = KeyboardDesignSystem.Dimensions.HINT_OFFSET_TOP * density
            // Calculate text baseline position
            val hintY = drawBounds.top + hintOffsetY + hintPaint.textSize - hintPaint.descent()
            canvas.drawText(hint, drawBounds.left + hintOffsetX, hintY, hintPaint)
        }

        var displayLabel = key.label
        if (isSpaceKey && displayLabel.isBlank()) {
            // Show layout name on spacebar if available, otherwise show empty
            displayLabel = currentLayoutName
        }

        val iconDrawn = drawCustomIcon(canvas, key, drawBounds)

        if (!iconDrawn && displayLabel.isNotEmpty()) {
            val targetPaint = if (isSpaceKey) spaceBarPaint else labelPaint
            targetPaint.color = when {
                isActionKey -> KeyboardDesignSystem.Colors.ACTION_KEY_TEXT_DYNAMIC  // Dynamic text on action key
                isUtilityKey -> KeyboardDesignSystem.Colors.SPECIAL_KEY_TEXT_DYNAMIC  // Dynamic text on special keys
                else -> KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC  // Dynamic text on regular keys
            }

            if (!isSpaceKey) {
                // Use text size from design system
                val optimalTextSize = KeyboardDesignSystem.getTextSize(context, KeyboardDesignSystem.TextType.KEY_LABEL)
                labelPaint.textSize = optimalTextSize
                // Center text vertically (adjust slightly if hint is present)
                val hintOffset = if (!key.hint.isNullOrBlank()) hintPaint.textSize * 0.2f else 0f
                val textY = drawBounds.centerY() + hintOffset - ((labelPaint.descent() + labelPaint.ascent()) / 2)
                canvas.drawText(displayLabel, drawBounds.centerX(), textY, labelPaint)
            } else {
                val textY = drawBounds.centerY() - ((spaceBarPaint.descent() + spaceBarPaint.ascent()) / 2)
                canvas.drawText(displayLabel, drawBounds.centerX(), textY, spaceBarPaint)
            }
        }
    }

    /**
     * onTouchEvent - Handle user touches
     *
     * This is called when user touches the screen.
     * We need to:
     * 1. Figure out which key was touched
     * 2. Show visual feedback (key press animation)
     * 3. Notify the listener (send character)
     *
     * @param event The touch event
     * @return true if we handled the event
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // If swipe typing is enabled, delegate to swipe gesture detector
        if (isSwipeTypingEnabled && swipeGestureDetector != null) {
            val handled = swipeGestureDetector?.onTouchEvent(event) ?: false
            if (handled) {
                return true
            }
        }

        // Fall back to standard touch handling
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                // User touched down
                handleTouchDown(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                // User lifted finger
                handleTouchUp(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // Track movement for cancelling long press when sliding away
                handleTouchMove(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                // Touch was cancelled
                clearPressedKey()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * Handle touch down event
     *
     * Find which key was pressed and show visual feedback.
     *
     * Material You enhancements:
     * - Ripple animation (120ms)
     * - Theme-based haptic feedback intensity
     */
    private fun handleTouchDown(x: Float, y: Float) {
        cancelLongPressDetection()
        val key = findKeyAt(x, y)

        if (key != null) {
            pressedKey = key.key
            pressedKeyBounds = key.bounds

            // Start key press scale animation
            startKeyPressAnimation(key.key)

            // Start ripple animation
            startRippleAnimation(x, y, key.bounds)

            invalidate() // Redraw to show pressed state

            // Vibrate
            performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

            // Play sound (if enabled in theme)
            // TODO: implement sound effects based on theme.interaction.soundEnabled

            if (!key.key.longPressKeys.isNullOrEmpty()) {
                scheduleLongPress(key)
            }
        }
    }

    private fun handleTouchMove(x: Float, y: Float) {
        val key = findKeyAt(x, y)
        if (key == null || key.key != pressedKey) {
            cancelLongPressDetection()
        }
    }

    /**
     * Start key press scale animation
     *
     * @param key The key to animate
     */
    private fun startKeyPressAnimation(key: Key) {
        // Cancel any existing animation
        keyPressAnimator?.cancel()

        animatingKey = key
        keyPressScale = 1.0f

        // Create scale down animation
        keyPressAnimator = ValueAnimator.ofFloat(1.0f, KeyboardDesignSystem.Animations.KEY_PRESS_SCALE).apply {
            duration = KeyboardDesignSystem.Animations.KEY_PRESS_DURATION
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->
                keyPressScale = animator.animatedValue as Float
                invalidate()
            }

            // After press, animate back to normal
            doOnEnd {
                animateKeyRelease()
            }

            start()
        }
    }

    /**
     * Animate key release
     */
    private fun animateKeyRelease() {
        keyPressAnimator = ValueAnimator.ofFloat(keyPressScale, 1.0f).apply {
            duration = KeyboardDesignSystem.Animations.KEY_RELEASE_DURATION
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->
                keyPressScale = animator.animatedValue as Float
                invalidate()
            }

            doOnEnd {
                animatingKey = null
                keyPressScale = 1.0f
            }

            start()
        }
    }

    /**
     * Start Material You ripple animation
     *
     * @param x Touch X coordinate
     * @param y Touch Y coordinate
     * @param bounds Key bounds for ripple radius
     */
    private fun startRippleAnimation(x: Float, y: Float, bounds: RectF) {
        // Cancel any existing ripple
        rippleAnimator?.cancel()

        // Set ripple center
        rippleX = x
        rippleY = y

        // Calculate max ripple radius (diagonal of key)
        val maxRadius = kotlin.math.sqrt(
            (bounds.width() * bounds.width() + bounds.height() * bounds.height()).toDouble()
        ).toFloat() / 2f

        // Create ripple animator
        rippleAnimator = ValueAnimator.ofFloat(0f, maxRadius).apply {
            duration = KeyboardDesignSystem.Animations.RIPPLE_DURATION
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->
                rippleRadius = animator.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    /**
     * Handle touch up event
     *
     * Send the key press to listener and clear visual feedback
     */
    private fun handleTouchUp(x: Float, y: Float) {
        cancelLongPressDetection()
        val key = findKeyAt(x, y)
        val popupVisible = longPressPopup != null

        // Only trigger if finger lifted on the same key it pressed and no popup stole the event
        if (!popupVisible && key != null && key.key == pressedKey) {
            // Notify listener
            keyPressListener?.invoke(key.key)
        }

        if (popupVisible) {
            pressedKey = null
            pressedKeyBounds = null
            rippleAnimator?.cancel()
            rippleRadius = 0f
            invalidate()
        } else {
            clearPressedKey()
        }
    }

    /**
     * Clear pressed key state
     *
     * Also clears ripple animation.
     */
    private fun clearPressedKey(dismissPopup: Boolean = true) {
        pressedKey = null
        pressedKeyBounds = null
        cancelLongPressDetection()

        if (dismissPopup) {
            dismissLongPressPopup()
        }

        // Clear ripple
        rippleAnimator?.cancel()
        rippleRadius = 0f

        invalidate() // Redraw to clear pressed state
    }

    private fun scheduleLongPress(targetKey: KeyBound) {
        cancelLongPressDetection()
        if (targetKey.key.longPressKeys.isNullOrEmpty()) return

        longPressTarget = targetKey
        val runnable = Runnable {
            showLongPressPopup(targetKey)
        }
        longPressRunnable = runnable
        longPressHandler.postDelayed(runnable, LONG_PRESS_TIMEOUT_MS)
    }

    private fun cancelLongPressDetection() {
        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
        longPressRunnable = null
        longPressTarget = null
    }

    private fun showLongPressPopup(targetKey: KeyBound) {
        val alternatives = targetKey.key.longPressKeys ?: return
        if (alternatives.isEmpty()) return

        dismissLongPressPopup()

        val popupPadding = (LONG_PRESS_POPUP_PADDING_DP * density).toInt()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(popupPadding, popupPadding / 2, popupPadding, popupPadding / 2)
            background = GradientDrawable().apply {
                cornerRadius = KeyboardDesignSystem.Dimensions.KEY_CORNER_RADIUS * density
                setColor(KeyboardDesignSystem.Colors.SPECIAL_KEY_BACKGROUND_DYNAMIC)
            }
        }

        val optionMargin = (LONG_PRESS_OPTION_MARGIN_DP * density).toInt()

        alternatives.forEachIndexed { index, option ->
            val optionView = createLongPressOptionView(option, targetKey.key)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (index > 0) {
                params.leftMargin = optionMargin
            }
            optionView.layoutParams = params
            container.addView(optionView)
        }

        container.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
        )

        val popup = PopupWindow(
            container,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(0x00000000))
            setOnDismissListener {
                longPressPopup = null
                clearPressedKey(dismissPopup = false)
            }
        }

        val location = IntArray(2)
        getLocationOnScreen(location)
        val popupWidth = container.measuredWidth
        val popupHeight = container.measuredHeight
        val popupX = (location[0] + targetKey.bounds.centerX() - popupWidth / 2).toInt()
        val offsetY = (LONG_PRESS_POPUP_OFFSET_DP * density).toInt()
        val popupY = (location[1] + targetKey.bounds.top - popupHeight - offsetY).toInt()

        popup.showAtLocation(this, Gravity.START or Gravity.TOP, popupX, popupY)
        longPressPopup = popup
    }

    private fun createLongPressOptionView(option: String, parentKey: Key): TextView {
        val horizontalPadding = (LONG_PRESS_OPTION_PADDING_DP * density).toInt()
        val verticalPadding = (LONG_PRESS_OPTION_PADDING_DP * 0.6f * density).toInt()
        return TextView(context).apply {
            text = option
            setTextColor(KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, KeyboardDesignSystem.Typography.KEY_LABEL_SIZE)
            gravity = Gravity.CENTER
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            background = GradientDrawable().apply {
                cornerRadius = KeyboardDesignSystem.Dimensions.KEY_CORNER_RADIUS * density
                setColor(KeyboardDesignSystem.Colors.KEY_BACKGROUND_DYNAMIC)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val altKey = parentKey.copy(label = option, output = option, longPressKeys = null)
                keyPressListener?.invoke(altKey)
                dismissLongPressPopup()
            }
        }
    }

    private fun dismissLongPressPopup() {
        longPressPopup?.dismiss()
        longPressPopup = null
    }

    /**
     * Find which key is at the given coordinates
     *
     * @param x X coordinate of touch
     * @param y Y coordinate of touch
     * @return The key at that position, or null
     */
    private fun findKeyAt(x: Float, y: Float): KeyBound? {
        // Material You Profile: Add 6dp touch padding for 48dp minimum touch targets
        // Visual key is 42dp, touch area is 48dp (42 + 6dp padding)
        val density = resources.displayMetrics.density
        val touchPadding = KeyboardDesignSystem.Dimensions.TOUCH_PADDING_VERTICAL * density

        return keyBounds.find { keyBound ->
            // Expand bounds by touch padding for better accessibility
            val expandedBounds = RectF(
                keyBound.bounds.left,
                keyBound.bounds.top - touchPadding,
                keyBound.bounds.right,
                keyBound.bounds.bottom + touchPadding
            )
            expandedBounds.contains(x, y)
        }
    }

    /**
     * Data class to hold a key and its bounds together
     */
    private data class KeyBound(
        val key: Key,
        val bounds: RectF
    )

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelLongPressDetection()
        dismissLongPressPopup()
    }
}
