package com.kannada.kavi.ui.keyboardview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.kannada.kavi.core.layout.models.Key
import com.kannada.kavi.core.layout.models.KeyboardRow

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

    // Paint objects (reused for performance)
    private val keyBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFE0E0E0.toInt() // Light gray
    }

    private val keyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFBDBDBD.toInt() // Darker gray when pressed
    }

    private val keyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0xFF9E9E9E.toInt() // Border color
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 48f // Will be adjusted based on key size
        color = 0xFF212121.toInt() // Almost black
    }

    // Currently pressed key (for visual feedback)
    private var pressedKey: Key? = null
    private var pressedKeyBounds: RectF? = null

    // Key listener (sends key presses to InputMethodService)
    private var keyPressListener: ((Key) -> Unit)? = null

    // Keyboard dimensions
    private var keyHeight = 0f
    private var keyboardWidth = 0f
    private var keyboardHeight = 0f

    // Spacing between keys
    private val keyPadding = 8f // Pixels between keys
    private val rowPadding = 8f // Pixels between rows

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
     */
    private fun calculateKeyBounds() {
        keyBounds.clear()

        if (rows.isEmpty()) return

        // Calculate key dimensions
        val availableWidth = width - (paddingLeft + paddingRight)
        val availableHeight = height - (paddingTop + paddingBottom)

        keyHeight = (availableHeight - (rows.size - 1) * rowPadding) / rows.size

        var currentY = paddingTop.toFloat()

        rows.forEach { row ->
            // Calculate total width units for this row
            val totalWidth = row.totalWidth

            // Calculate actual key width
            val unitWidth = (availableWidth - (row.keyCount - 1) * keyPadding) / totalWidth

            var currentX = paddingLeft.toFloat()

            row.keys.forEach { key ->
                // Calculate key width based on its width multiplier
                val keyWidth = (unitWidth * key.width) + ((key.width - 1) * keyPadding)

                // Create bounds for this key
                val bounds = RectF(
                    currentX,
                    currentY,
                    currentX + keyWidth - keyPadding,
                    currentY + keyHeight - rowPadding
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
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)

        // Calculate desired height based on number of rows
        // Each row gets 56dp (standard key height)
        val rowCount = rows.size.coerceAtLeast(4) // At least 4 rows
        val density = resources.displayMetrics.density
        val desiredHeight = (rowCount * 56 * density).toInt() + paddingTop + paddingBottom

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
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

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
     */
    private fun drawKey(canvas: Canvas, keyBound: KeyBound) {
        val key = keyBound.key
        val bounds = keyBound.bounds

        // Choose paint based on whether key is pressed
        val backgroundPaint = if (key == pressedKey) {
            keyPressedPaint
        } else {
            keyBackgroundPaint
        }

        // Draw key background (rounded rectangle)
        val cornerRadius = 8f
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, backgroundPaint)

        // Draw key border
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, keyBorderPaint)

        // Draw key label (text)
        if (key.label.isNotEmpty()) {
            // Calculate text position (center of key)
            val textX = bounds.centerX()
            val textY = bounds.centerY() - ((labelPaint.descent() + labelPaint.ascent()) / 2)

            // Adjust text size based on key size
            val optimalTextSize = (bounds.height() * 0.4f).coerceAtMost(48f)
            labelPaint.textSize = optimalTextSize

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
     * Find which key was pressed and show visual feedback
     */
    private fun handleTouchDown(x: Float, y: Float) {
        val key = findKeyAt(x, y)

        if (key != null) {
            pressedKey = key.key
            pressedKeyBounds = key.bounds
            invalidate() // Redraw to show pressed state

            // Vibrate (if enabled)
            performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

            // Play sound (TODO: add sound effects)
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
     */
    private fun clearPressedKey() {
        pressedKey = null
        pressedKeyBounds = null
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
