package com.kannada.kavi.ui.keyboardview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import com.kannada.kavi.features.clipboard.models.ClipboardItem
import com.kannada.kavi.features.themes.KeyboardDesignSystem
import kotlin.math.max

/**
 * ClipboardStripView - FlorisBoard-style Horizontal Clipboard Strip
 *
 * A horizontal scrollable strip of clipboard items shown above the keyboard.
 * Similar to FlorisBoard's clipboard UI design.
 *
 * DESIGN:
 * =======
 * ┌─────────────────────────────────────────────────────┐
 * │ 📋 Clipboard ×                                      │
 * ├─────────────────────────────────────────────────────┤
 * │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐      │
 * │  │⭐ Hello│ │ World  │ │ Kannada│ │ Test   │  ... │
 * │  │  World │ │ Text   │ │ Typing │ │ 123    │      │
 * │  └────────┘ └────────┘ └────────┘ └────────┘      │
 * └─────────────────────────────────────────────────────┘
 *
 * FEATURES:
 * =========
 * - Horizontal scrolling
 * - Clipboard items as cards
 * - Tap to paste
 * - Long press for options (pin, delete)
 * - Pinned items marked with ⭐
 * - Close button (×)
 * - Smooth animations
 *
 * INTERACTION:
 * ============
 * - Tap item → Paste and close
 * - Long press item → Show options
 * - Swipe horizontally → Scroll
 * - Tap × → Close strip
 */
class ClipboardStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val HEADER_HEIGHT_DP = 40f
        private const val ITEM_WIDTH_DP = 120f
        private const val ITEM_HEIGHT_DP = 80f
        private const val ITEM_SPACING_DP = 8f
        private const val ITEM_PADDING_DP = 12f
        private const val CORNER_RADIUS_DP = 12f
        private const val LONG_PRESS_TIMEOUT_MS = 350L
    }

    // Clipboard items
    private var items: List<ClipboardItem> = emptyList()

    // Scroll position
    private var scrollX = 0f
    private var maxScrollX = 0f

    // Touch tracking
    private var lastTouchX = 0f
    private var isScrolling = false
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var touchDownTime = 0L

    // Item bounds
    private val itemBounds = mutableListOf<ItemBound>()
    private var closeBounds = RectF()

    // Pressed item
    private var pressedItem: ClipboardItem? = null
    private var longPressRunnable: Runnable? = null

    // Listeners
    private var onItemClickListener: ((ClipboardItem) -> Unit)? = null
    private var onItemLongClickListener: ((ClipboardItem) -> Unit)? = null
    private var onCloseListener: (() -> Unit)? = null

    // Dynamic colors
    private var backgroundColor = KeyboardDesignSystem.Colors.KEYBOARD_BACKGROUND_DYNAMIC
    private var headerColor = KeyboardDesignSystem.Colors.SPECIAL_KEY_BACKGROUND_DYNAMIC
    private var cardColor = KeyboardDesignSystem.Colors.KEY_BACKGROUND_DYNAMIC
    private var cardPressedColor = KeyboardDesignSystem.Colors.KEY_PRESSED_DYNAMIC
    private var textColor = KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC
    private var headerTextColor = KeyboardDesignSystem.Colors.SPECIAL_KEY_TEXT_DYNAMIC
    private var accentColor = KeyboardDesignSystem.Colors.ACTION_KEY_BACKGROUND_DYNAMIC
    private var dividerColor = 0

    // Dimensions
    private var headerHeight = 0f
    private var itemWidth = 0f
    private var itemHeight = 0f
    private var itemSpacing = 0f
    private var itemPadding = 0f
    private var cornerRadius = 0f

    // Paint objects
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val cardPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
    }

    private val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    init {
        applyColors()
    }

    /**
     * Apply colors from design system
     */
    private fun applyColors() {
        val density = resources.displayMetrics.density

        backgroundColor = KeyboardDesignSystem.Colors.KEYBOARD_BACKGROUND_DYNAMIC
        headerColor = KeyboardDesignSystem.Colors.SPECIAL_KEY_BACKGROUND_DYNAMIC
        cardColor = KeyboardDesignSystem.Colors.KEY_BACKGROUND_DYNAMIC
        cardPressedColor = KeyboardDesignSystem.Colors.KEY_PRESSED_DYNAMIC
        textColor = KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC
        headerTextColor = KeyboardDesignSystem.Colors.SPECIAL_KEY_TEXT_DYNAMIC
        accentColor = KeyboardDesignSystem.Colors.ACTION_KEY_BACKGROUND_DYNAMIC
        dividerColor = ColorUtils.setAlphaComponent(textColor, (0.12f * 255).toInt())

        // Update dimensions
        headerHeight = HEADER_HEIGHT_DP * density
        itemWidth = ITEM_WIDTH_DP * density
        itemHeight = ITEM_HEIGHT_DP * density
        itemSpacing = ITEM_SPACING_DP * density
        itemPadding = ITEM_PADDING_DP * density
        cornerRadius = CORNER_RADIUS_DP * density

        // Update paints
        backgroundPaint.color = backgroundColor
        headerPaint.color = headerColor
        cardPaint.color = cardColor
        cardPressedPaint.color = cardPressedColor
        dividerPaint.color = dividerColor

        textPaint.apply {
            color = textColor
            textSize = 14f * density
        }

        headerTextPaint.apply {
            color = headerTextColor
            textSize = 16f * density
            isFakeBoldText = true
        }

        iconPaint.apply {
            color = accentColor
            textSize = 18f * density
        }
    }

    /**
     * Refresh colors when theme changes
     */
    fun refreshColors() {
        applyColors()
        invalidate()
    }

    /**
     * Set clipboard items
     */
    fun setItems(items: List<ClipboardItem>) {
        this.items = items
        scrollX = 0f
        calculateBounds()
        invalidate()
    }

    /**
     * Set item click listener
     */
    fun setOnItemClickListener(listener: (ClipboardItem) -> Unit) {
        this.onItemClickListener = listener
    }

    /**
     * Set item long click listener (for pin/delete options)
     */
    fun setOnItemLongClickListener(listener: (ClipboardItem) -> Unit) {
        this.onItemLongClickListener = listener
    }

    /**
     * Set close listener
     */
    fun setOnCloseListener(listener: () -> Unit) {
        this.onCloseListener = listener
    }

    /**
     * Calculate item bounds
     */
    private fun calculateBounds() {
        itemBounds.clear()

        if (items.isEmpty()) {
            maxScrollX = 0f
            return
        }

        val startX = itemPadding - scrollX
        val startY = headerHeight + itemPadding

        var currentX = startX

        items.forEach { item ->
            val bounds = RectF(
                currentX,
                startY,
                currentX + itemWidth,
                startY + itemHeight
            )

            itemBounds.add(ItemBound(item, bounds))
            currentX += itemWidth + itemSpacing
        }

        // Calculate max scroll
        val totalWidth = (itemWidth + itemSpacing) * items.size + itemPadding
        maxScrollX = max(0f, totalWidth - width)

        // Close button bounds
        closeBounds = RectF(
            width - 80f,
            8f,
            width - 8f,
            headerHeight - 8f
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val density = resources.displayMetrics.density
        val desiredHeight = (headerHeight + itemHeight + itemPadding * 2).toInt()

        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateBounds()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawColor(backgroundColor)

        // Draw header
        drawHeader(canvas)

        // Draw divider
        canvas.drawRect(
            0f,
            headerHeight,
            width.toFloat(),
            headerHeight + 1f * resources.displayMetrics.density,
            dividerPaint
        )

        // Save canvas for clipping
        canvas.save()
        canvas.clipRect(0f, headerHeight, width.toFloat(), height.toFloat())

        // Draw items
        itemBounds.forEach { itemBound ->
            // Only draw visible items
            if (itemBound.bounds.right > 0 && itemBound.bounds.left < width) {
                drawItem(canvas, itemBound)
            }
        }

        canvas.restore()

        // Draw empty state
        if (items.isEmpty()) {
            drawEmptyState(canvas)
        }
    }

    /**
     * Draw header with title and close button
     */
    private fun drawHeader(canvas: Canvas) {
        // Header background
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, headerPaint)

        // Title with icon
        val titleX = itemPadding
        val titleY = headerHeight / 2 - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2)

        // Clipboard icon
        canvas.drawText("📋", titleX, titleY, iconPaint)

        // Title text
        val titleTextX = titleX + 60f
        canvas.drawText("Clipboard", titleTextX, titleY, headerTextPaint)

        // Close button (×)
        val closePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = headerTextColor
            textSize = 24f * resources.displayMetrics.density
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val closeX = closeBounds.centerX()
        val closeY = closeBounds.centerY() - ((closePaint.descent() + closePaint.ascent()) / 2)
        canvas.drawText("×", closeX, closeY, closePaint)
    }

    /**
     * Draw clipboard item card
     */
    private fun drawItem(canvas: Canvas, itemBound: ItemBound) {
        val item = itemBound.item
        val bounds = itemBound.bounds

        // Card background
        val bgPaint = if (item == pressedItem) cardPressedPaint else cardPaint
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, bgPaint)

        // Pin icon (if pinned)
        if (item.isPinned) {
            val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                textSize = 16f * resources.displayMetrics.density
            }
            val pinX = bounds.left + itemPadding
            val pinY = bounds.top + itemPadding + pinPaint.textSize
            canvas.drawText("⭐", pinX, pinY, pinPaint)
        }

        // Item text (truncated)
        val preview = item.getPreview(20) // Max 20 chars
        val textX = bounds.left + itemPadding
        val textY = bounds.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2)

        // Draw text with word wrap
        val maxWidth = bounds.width() - itemPadding * 2
        val lines = wrapText(preview, maxWidth)

        var currentY = if (item.isPinned) {
            bounds.top + itemPadding + 40f
        } else {
            bounds.top + itemPadding + textPaint.textSize
        }

        lines.take(2).forEach { line -> // Max 2 lines
            canvas.drawText(line, textX, currentY, textPaint)
            currentY += textPaint.textSize + 4f
        }
    }

    /**
     * Wrap text to fit width
     */
    private fun wrapText(text: String, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var currentLine = ""

        text.split(" ").forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = textPaint.measureText(testLine)

            if (width <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    /**
     * Draw empty state
     */
    private fun drawEmptyState(canvas: Canvas) {
        val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ColorUtils.setAlphaComponent(textColor, (0.5f * 255).toInt())
            textSize = 16f * resources.displayMetrics.density
            textAlign = Paint.Align.CENTER
        }

        val text = "No clipboard history"
        val textX = width / 2f
        val textY = (headerHeight + height) / 2f
        canvas.drawText(text, textX, textY, emptyPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                touchDownTime = System.currentTimeMillis()
                lastTouchX = event.x
                isScrolling = false

                // Check close button
                if (closeBounds.contains(event.x, event.y)) {
                    return true
                }

                // Check item tap
                val item = findItemAt(event.x, event.y)
                if (item != null) {
                    pressedItem = item
                    scheduleLongPress(item)
                    invalidate()
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = lastTouchX - event.x

                // Check if scrolling
                if (Math.abs(deltaX) > 10f || Math.abs(event.y - touchDownY) > 10f) {
                    isScrolling = true
                    pressedItem = null
                    cancelItemLongPress()

                    // Update scroll
                    scrollX = (scrollX + deltaX).coerceIn(0f, maxScrollX)
                    lastTouchX = event.x

                    calculateBounds()
                    invalidate()
                }

                return true
            }

            MotionEvent.ACTION_UP -> {
                cancelItemLongPress()

                // Check close button
                if (closeBounds.contains(event.x, event.y) &&
                    closeBounds.contains(touchDownX, touchDownY)) {
                    onCloseListener?.invoke()
                    pressedItem = null
                    invalidate()
                    return true
                }

                if (!isScrolling && pressedItem != null) {
                    val item = findItemAt(event.x, event.y)
                    if (item != null && item == pressedItem) {
                        // Tap detected
                        onItemClickListener?.invoke(item)
                    }
                }

                pressedItem = null
                isScrolling = false
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelItemLongPress()
                pressedItem = null
                isScrolling = false
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * Schedule long press detection
     */
    private fun scheduleLongPress(item: ClipboardItem) {
        cancelItemLongPress()

        longPressRunnable = Runnable {
            if (pressedItem == item && !isScrolling) {
                // Long press detected
                performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                onItemLongClickListener?.invoke(item)
                pressedItem = null
                invalidate()
            }
        }.also {
            postDelayed(it, LONG_PRESS_TIMEOUT_MS)
        }
    }

    /**
     * Cancel long press detection
     */
    private fun cancelItemLongPress() {
        longPressRunnable?.let {
            removeCallbacks(it)
            longPressRunnable = null
        }
    }

    /**
     * Find item at coordinates
     */
    private fun findItemAt(x: Float, y: Float): ClipboardItem? {
        return itemBounds.find { it.bounds.contains(x, y) }?.item
    }

    /**
     * Data class for item bounds
     */
    private data class ItemBound(
        val item: ClipboardItem,
        val bounds: RectF
    )

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelItemLongPress()
    }
}
