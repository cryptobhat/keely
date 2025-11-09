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

    // Very subtle shadow for depth (almost invisible)
    private val keyShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        maskFilter = android.graphics.BlurMaskFilter(1f, android.graphics.BlurMaskFilter.Blur.NORMAL)
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

    // Spacing between keys - much tighter for modern look
    private var keyHorizontalSpacing = 3f  // Reduced from theme default for tighter layout
    private var keyVerticalSpacing = 4f   // Reduced from theme default for compact height
    private var rowPadding = 4f           // Side padding for the keyboard

    init {
        // Apply default theme
        applyTheme(theme)

        // Minimal padding for maximum key area
        val density = resources.displayMetrics.density
        setPadding(
            (4 * density).toInt(), // left - reduced for wider keys
            (4 * density).toInt(), // top - reduced for taller keys
            (4 * density).toInt(), // right - reduced for wider keys
            (6 * density).toInt()  // bottom - slightly more for gesture bar
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

        // Configure shadow paint - very subtle for clean look
        keyShadowPaint.color = if (theme.mode == com.kannada.kavi.features.themes.ThemeMode.DARK) {
            0x0A000000.toInt() // rgba(0,0,0,0.04) for dark mode
        } else {
            0x08000000.toInt() // rgba(0,0,0,0.03) very subtle shadow
        }

        // Apply tighter spacing for modern compact look (override theme for better layout)
        val density = resources.displayMetrics.density
        keyHorizontalSpacing = 3f * density  // 3dp between keys horizontally
        keyVerticalSpacing = 4f * density    // 4dp between rows
        rowPadding = 4f * density            // 4dp side padding

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
     *
     * IMPORTANT: Keys must align in a proper grid across all rows
     * Similar to Gboard and the reference design
     */
    private fun calculateKeyBounds() {
        keyBounds.clear()

        if (rows.isEmpty()) return

        // Calculate key dimensions
        val availableWidth = width - (paddingLeft + paddingRight)
        val availableHeight = height - (paddingTop + paddingBottom)

        // Standard keyboard layout uses 10 unit width as reference
        // Row 1: 10 keys x 1.0 width = 10 units
        // Row 2: 9 keys x 1.0 width = 9 units (centered)
        // Row 3: 1.5 + 7x1.0 + 1.5 = 10 units
        // Row 4: 1.5 + 1.0 + 1.0 + 4.0 + 1.0 + 1.5 = 10 units
        val standardRowWidth = 10f

        // Calculate base unit width (width of a standard 1.0 width key)
        // This ensures consistent key sizes across all rows
        val totalSpacingWidth = (9 * keyHorizontalSpacing) // Assuming 10 keys with 9 spaces between them
        val baseUnitWidth = (availableWidth - totalSpacingWidth) / standardRowWidth

        // Calculate row height
        keyHeight = (availableHeight - (rows.size - 1) * keyVerticalSpacing) / rows.size

        var currentY = paddingTop.toFloat()

        rows.forEachIndexed { rowIndex, row ->
            // Calculate the actual width this row will occupy
            var rowActualWidth = 0f
            row.keys.forEach { key ->
                rowActualWidth += baseUnitWidth * key.width
            }
            // Add spacing between keys (not after the last key)
            if (row.keyCount > 0) {
                rowActualWidth += (row.keyCount - 1) * keyHorizontalSpacing
            }

            // Special handling for row alignment to match standard QWERTY layout
            val rowStartX = when (rowIndex) {
                1 -> {
                    // Second row (A row) - indent by half a key width for QWERTY alignment
                    paddingLeft + (baseUnitWidth * 0.5f)
                }
                else -> {
                    // Other rows - center normally
                    paddingLeft + (availableWidth - rowActualWidth) / 2f
                }
            }

            var currentX = rowStartX

            row.keys.forEachIndexed { keyIndex, key ->
                // Calculate key width based on its width multiplier
                val keyActualWidth = baseUnitWidth * key.width

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
    }

    /**
     * onMeasure - Tell Android how big we want to be
     *
     * Android asks: "How big do you want to be?"
     * We answer: "This big!"
     *
     * Modern keyboard specifications:
     * - Key height: ~48dp for optimal touch target
     * - Compact vertical spacing for clean look
     * - Total height around 220-240dp for standard 4-row layout
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)

        // Calculate desired height based on number of rows
        val rowCount = rows.size.coerceAtLeast(4) // At least 4 rows
        val density = resources.displayMetrics.density

        // Optimal key height for touch targets while keeping keyboard compact
        val keyHeightDp = 48f // Standard touch target height
        val keyHeightPx = keyHeightDp * density

        // Calculate total spacing between rows
        val verticalSpacingTotal = (rowCount - 1) * keyVerticalSpacing

        // Total height: rows + spacing + padding
        val desiredHeight = ((rowCount * keyHeightPx) + verticalSpacingTotal + paddingTop + paddingBottom).toInt()

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
     * Desh Kannada Keyboard design system:
     * - Border radius: 6px (per design system)
     * - Shadow: 0 1px 2px rgba(0,0,0,0.1)
     * - Theme colors for different states
     * - Selected state support
     * - Optional borders (#E0E0E0)
     * - Clean, minimal appearance
     */
    private fun drawKey(canvas: Canvas, keyBound: KeyBound) {
        val key = keyBound.key
        val bounds = keyBound.bounds

        // Create inset bounds to account for spacing (visual gap between keys)
        // Very minimal inset for keys to appear close together like in reference
        val insetAmount = 0.5f * resources.displayMetrics.density // 0.5dp gap on each side
        val drawBounds = RectF(
            bounds.left + insetAmount,
            bounds.top + insetAmount,
            bounds.right - insetAmount,
            bounds.bottom - insetAmount
        )

        // Choose paint based on key state
        val backgroundPaint = when {
            key == pressedKey -> keyPressedPaint
            key == selectedKey -> keySelectedPaint
            else -> keyBackgroundPaint
        }

        // Get corner radius from theme (convert dp to pixels)
        val cornerRadius = theme.shape.keyCornerRadius * resources.displayMetrics.density

        // Skip shadow drawing for cleaner, flatter look matching reference
        // Shadow can be re-enabled if needed by uncommenting below
        /*
        if (key != pressedKey) { // No shadow when pressed (looks flatter)
            val shadowOffset = 0.5f * resources.displayMetrics.density
            val shadowBounds = RectF(
                drawBounds.left,
                drawBounds.top + shadowOffset,
                drawBounds.right,
                drawBounds.bottom + shadowOffset
            )
            canvas.drawRoundRect(shadowBounds, cornerRadius, cornerRadius, keyShadowPaint)
        }
        */

        // Draw key background (rounded rectangle)
        canvas.drawRoundRect(drawBounds, cornerRadius, cornerRadius, backgroundPaint)

        // Draw key border (if enabled in theme)
        if (theme.shape.borderEnabled) {
            // Use selected border color if key is selected
            if (key == selectedKey) {
                val selectedBorderPaint = Paint(keyBorderPaint).apply {
                    color = theme.colors.keySelectedBorder
                }
                canvas.drawRoundRect(drawBounds, cornerRadius, cornerRadius, selectedBorderPaint)
            } else {
                canvas.drawRoundRect(drawBounds, cornerRadius, cornerRadius, keyBorderPaint)
            }
        }

        // Draw key label (text)
        if (key.label.isNotEmpty()) {
            // Calculate text position (center of key using drawBounds for accurate centering)
            val textX = drawBounds.centerX()
            val textY = drawBounds.centerY() - ((labelPaint.descent() + labelPaint.ascent()) / 2)

            // Desh design system: 18px text size for keys
            val optimalTextSize = (bounds.height() * 0.43f).coerceAtMost(
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
