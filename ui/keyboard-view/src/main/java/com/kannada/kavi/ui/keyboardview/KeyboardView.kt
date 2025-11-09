package com.kannada.kavi.ui.keyboardview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.kannada.kavi.core.layout.models.Key
import com.kannada.kavi.core.layout.models.KeyboardRow
import com.kannada.kavi.features.themes.KeyboardTheme

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

    // Keyboard data
    private var rows: List<KeyboardRow> = emptyList()
    private var keyBounds: MutableList<KeyBound> = mutableListOf()

    // Theme (Material You design system)
    private var theme: KeyboardTheme = KeyboardTheme.defaultLight()

    // Paint objects (reused for performance)
    private val keyBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val keyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val keySelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val keyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        // Gboard uses slightly bolder text for better readability
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.NORMAL
        )
    }

    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Gboard-inspired visual enhancements
    private val keyShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        maskFilter = android.graphics.BlurMaskFilter(4f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }

    private val keyHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Currently pressed key (for visual feedback)
    private var pressedKey: Key? = null
    private var pressedKeyBounds: RectF? = null
    private var selectedKey: Key? = null

    // Ripple animation
    private var rippleAnimator: ValueAnimator? = null
    private var rippleRadius = 0f
    private var rippleX = 0f
    private var rippleY = 0f

    // Key listener (sends key presses to InputMethodService)
    private var keyPressListener: ((Key) -> Unit)? = null

    // Keyboard dimensions
    private var keyHeight = 0f
    private var keyboardWidth = 0f
    private var keyboardHeight = 0f

    // Spacing between keys (Gboard-inspired: more generous spacing)
    private var keyHorizontalSpacing = 6f  // Increased from 4f
    private var keyVerticalSpacing = 8f   // Increased from 6f
    private var rowPadding = 8f           // Increased from 6f

    init {
        // Apply default theme
        applyTheme(theme)

        // Gboard-style padding for better edge alignment
        val density = resources.displayMetrics.density
        setPadding(
            (4 * density).toInt(), // left
            (6 * density).toInt(), // top
            (4 * density).toInt(), // right
            (6 * density).toInt()  // bottom
        )
    }

    /**
     * Set Material You theme
     *
     * Applies all theme properties: colors, typography, shape, spacing, interactions.
     *
     * @param theme KeyboardTheme to apply
     */
    fun setTheme(theme: KeyboardTheme) {
        this.theme = theme
        applyTheme(theme)
        calculateKeyBounds() // Recalculate with new spacing
        invalidate()
    }

    /**
     * Apply theme to all Paint objects
     *
     * Updates colors, sizes, and styles from theme.
     */
    private fun applyTheme(theme: KeyboardTheme) {
        // Apply colors
        keyBackgroundPaint.color = theme.colors.keyNormal
        keyPressedPaint.color = theme.colors.keyPressed
        keySelectedPaint.color = theme.colors.keySelected

        keyBorderPaint.apply {
            color = theme.colors.keyBorder
            strokeWidth = theme.shape.borderWidth * resources.displayMetrics.density
        }

        labelPaint.apply {
            color = theme.colors.onSurface
            textSize = theme.typography.buttonSize * resources.displayMetrics.scaledDensity
            typeface = android.graphics.Typeface.create(
                "google_sans", // Will fallback to system font if not available
                when (theme.typography.labelWeight) {
                    500 -> android.graphics.Typeface.NORMAL
                    600 -> android.graphics.Typeface.BOLD
                    else -> android.graphics.Typeface.NORMAL
                }
            )
        }

        ripplePaint.color = theme.colors.ripple

        // Configure shadow paint (Gboard-style subtle elevation)
        keyShadowPaint.color = if (theme.mode == com.kannada.kavi.features.themes.ThemeMode.DARK) {
            0x40000000.toInt() // Darker shadow for dark mode
        } else {
            0x20000000.toInt() // Subtle shadow for light mode
        }

        // Configure highlight paint (top shine effect)
        keyHighlightPaint.color = if (theme.mode == com.kannada.kavi.features.themes.ThemeMode.DARK) {
            0x10FFFFFF.toInt() // Subtle white highlight for dark mode
        } else {
            0x20FFFFFF.toInt() // Subtle white highlight for light mode
        }

        // Apply spacing (Gboard uses slightly more generous spacing)
        val density = resources.displayMetrics.density
        keyHorizontalSpacing = theme.spacing.keyHorizontalSpacing * density
        keyVerticalSpacing = theme.spacing.keyVerticalSpacing * density
        rowPadding = theme.spacing.rowPadding * density

        // Enable hardware acceleration for shadows
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * Set the keyboard rows to display
     *
     * @param rows List of keyboard rows from LayoutManager
     */
    fun setKeyboard(rows: List<KeyboardRow>) {
        this.rows = rows
        calculateKeyBounds()
        invalidate() // Request redraw
    }

    /**
     * Set listener for key press events
     *
     * @param listener Lambda that receives the pressed key
     */
    fun setOnKeyPressListener(listener: (Key) -> Unit) {
        this.keyPressListener = listener
    }

    /**
     * Calculate bounds (position and size) for each key
     *
     * This is like creating a blueprint before building.
     * We figure out where each key should be drawn.
     * Now uses theme spacing values for Material You design.
     */
    private fun calculateKeyBounds() {
        keyBounds.clear()

        if (rows.isEmpty()) return

        // Calculate key dimensions
        val availableWidth = width - (paddingLeft + paddingRight)
        val availableHeight = height - (paddingTop + paddingBottom)

        keyHeight = (availableHeight - (rows.size - 1) * keyVerticalSpacing) / rows.size

        var currentY = paddingTop.toFloat()

        rows.forEach { row ->
            // Calculate total width units for this row
            val totalWidth = row.totalWidth

            // Calculate actual key width
            val unitWidth = (availableWidth - (row.keyCount - 1) * keyHorizontalSpacing) / totalWidth

            var currentX = paddingLeft.toFloat()

            row.keys.forEach { key ->
                // Calculate key width based on its width multiplier
                val keyWidth = (unitWidth * key.width) + ((key.width - 1) * keyHorizontalSpacing)

                // Create bounds for this key
                val bounds = RectF(
                    currentX,
                    currentY,
                    currentX + keyWidth - keyHorizontalSpacing,
                    currentY + keyHeight - keyVerticalSpacing
                )

                keyBounds.add(KeyBound(key, bounds))

                currentX += keyWidth
            }

            currentY += keyHeight
        }
    }

    /**
     * onMeasure - Tell Android how big we want to be
     *
     * Android asks: "How big do you want to be?"
     * We answer: "This big!"
     *
     * Gboard-inspired proportions:
     * - Taller keys (60dp vs 56dp) for better accuracy
     * - Includes spacing between rows
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)

        // Calculate desired height based on number of rows
        // Gboard uses ~60dp per row for better touch targets
        val rowCount = rows.size.coerceAtLeast(4) // At least 4 rows
        val density = resources.displayMetrics.density
        val keyHeightDp = 60f // Increased from 56dp (Gboard-style)
        val verticalSpacingTotal = (rowCount - 1) * keyVerticalSpacing
        val desiredHeight = ((rowCount * keyHeightDp * density) + verticalSpacingTotal).toInt() +
                           paddingTop + paddingBottom

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
     * Draw a single key
     *
     * @param canvas The canvas to draw on
     * @param keyBound The key and its bounds
     *
     * Material You + Gboard-inspired enhancements:
     * - Theme-based corner radius (8dp)
     * - Theme colors for different states
     * - Selected state support
     * - Optional borders
     * - Subtle shadow for elevation (Gboard-style)
     * - Highlight on top edge for depth
     */
    private fun drawKey(canvas: Canvas, keyBound: KeyBound) {
        val key = keyBound.key
        val bounds = keyBound.bounds

        // Choose paint based on key state
        val backgroundPaint = when {
            key == pressedKey -> keyPressedPaint
            key == selectedKey -> keySelectedPaint
            else -> keyBackgroundPaint
        }

        // Get corner radius from theme (convert dp to pixels)
        val cornerRadius = theme.shape.keyCornerRadius * resources.displayMetrics.density

        // Gboard-style elevation: Draw subtle shadow beneath key
        if (key != pressedKey) { // No shadow when pressed (looks flatter)
            val shadowOffset = 2f * resources.displayMetrics.density
            val shadowBounds = RectF(
                bounds.left,
                bounds.top + shadowOffset,
                bounds.right,
                bounds.bottom + shadowOffset
            )
            canvas.drawRoundRect(shadowBounds, cornerRadius, cornerRadius, keyShadowPaint)
        }

        // Draw key background (rounded rectangle)
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, backgroundPaint)

        // Gboard-style highlight: Subtle shine on top portion of key
        if (key != pressedKey) { // No highlight when pressed
            val highlightBounds = RectF(
                bounds.left,
                bounds.top,
                bounds.right,
                bounds.top + (bounds.height() * 0.3f) // Top 30% of key
            )
            canvas.save()
            canvas.clipRect(bounds) // Clip to key bounds
            canvas.drawRoundRect(highlightBounds, cornerRadius, cornerRadius, keyHighlightPaint)
            canvas.restore()
        }

        // Draw key border (if enabled in theme)
        if (theme.shape.borderEnabled) {
            // Use selected border color if key is selected
            if (key == selectedKey) {
                val selectedBorderPaint = Paint(keyBorderPaint).apply {
                    color = theme.colors.keySelectedBorder
                }
                canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, selectedBorderPaint)
            } else {
                canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, keyBorderPaint)
            }
        }

        // Draw key label (text)
        if (key.label.isNotEmpty()) {
            // Calculate text position (center of key)
            val textX = bounds.centerX()
            val textY = bounds.centerY() - ((labelPaint.descent() + labelPaint.ascent()) / 2)

            // Gboard-style text sizing: proportional to key height, slightly larger
            val optimalTextSize = (bounds.height() * 0.42f).coerceAtMost(
                theme.typography.buttonSize * resources.displayMetrics.scaledDensity
            )
            labelPaint.textSize = optimalTextSize

            // Set text color based on key state
            labelPaint.color = when {
                key == pressedKey -> theme.colors.onSurface
                key == selectedKey -> theme.colors.primary
                else -> theme.colors.onSurface
            }

            canvas.drawText(key.label, textX, textY, labelPaint)
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
                // User is moving finger (for swipe typing in future)
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
        val key = findKeyAt(x, y)

        if (key != null) {
            pressedKey = key.key
            pressedKeyBounds = key.bounds

            // Start ripple animation
            startRippleAnimation(x, y, key.bounds)

            invalidate() // Redraw to show pressed state

            // Vibrate (if enabled in theme)
            if (theme.interaction.vibrationEnabled) {
                performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }

            // Play sound (if enabled in theme)
            // TODO: implement sound effects based on theme.interaction.soundEnabled
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
            duration = theme.interaction.rippleDuration
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
        val key = findKeyAt(x, y)

        // Only trigger if finger lifted on the same key it pressed
        if (key != null && key.key == pressedKey) {
            // Notify listener
            keyPressListener?.invoke(key.key)
        }

        clearPressedKey()
    }

    /**
     * Clear pressed key state
     *
     * Also clears ripple animation.
     */
    private fun clearPressedKey() {
        pressedKey = null
        pressedKeyBounds = null

        // Clear ripple
        rippleAnimator?.cancel()
        rippleRadius = 0f

        invalidate() // Redraw to clear pressed state
    }

    /**
     * Find which key is at the given coordinates
     *
     * @param x X coordinate of touch
     * @param y Y coordinate of touch
     * @return The key at that position, or null
     */
    private fun findKeyAt(x: Float, y: Float): KeyBound? {
        return keyBounds.find { keyBound ->
            keyBound.bounds.contains(x, y)
        }
    }

    /**
     * Data class to hold a key and its bounds together
     */
    private data class KeyBound(
        val key: Key,
        val bounds: RectF
    )
}
