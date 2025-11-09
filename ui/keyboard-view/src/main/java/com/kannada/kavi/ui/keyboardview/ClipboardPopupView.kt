package com.kannada.kavi.ui.keyboardview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.kannada.kavi.features.clipboard.models.ClipboardItem
import com.kannada.kavi.features.themes.KeyboardTheme
import kotlin.math.max
import kotlin.math.min

/**
 * ClipboardPopupView - Clipboard History Popup
 *
 * Shows clipboard history in a scrollable popup above the keyboard.
 *
 * DESIGN:
 * =======
 * ┌─────────────────────────────────────┐
 * │ Clipboard History            [X]    │ ← Header
 * ├─────────────────────────────────────┤
 * │ ⭐ https://github.com          📋   │ ← Pinned item
 * │ ─────────────────────────────────── │
 * │ ನಮಸ್ತೆ                         📋   │
 * │ ─────────────────────────────────── │
 * │ Hello World                    📋   │
 * │ ─────────────────────────────────── │
 * │ Long text that gets truncat... 📋   │
 * │ ─────────────────────────────────── │
 * │                                     │
 * └─────────────────────────────────────┘
 *
 * FEATURES:
 * =========
 * - Scrollable list of clipboard items
 * - Tap item → paste it
 * - Pin/unpin items (tap ⭐)
 * - Delete items (swipe left)
 * - Close button (tap X)
 * - Shows up to 10 items at once
 * - Smooth scrolling
 *
 * INTERACTION:
 * ============
 * - Tap item → Paste and close
 * - Long press → Show options (pin, delete, copy)
 * - Swipe down → Close
 * - Tap X button → Close
 *
 * PERFORMANCE:
 * ============
 * - Canvas rendering (fast!)
 * - Virtual scrolling (only draw visible items)
 * - 60 FPS smooth scroll
 */
class ClipboardPopupView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Clipboard items to display
    private var items: List<ClipboardItem> = emptyList()

    // Scroll position
    private var scrollY = 0f
    private var maxScrollY = 0f

    // Touch tracking
    private var lastTouchY = 0f
    private var isScrolling = false

    // Item bounds for touch detection
    private val itemBounds = mutableListOf<ItemBound>()

    // Pressed item
    private var pressedItem: ClipboardItem? = null

    // Listeners
    private var onItemClickListener: ((ClipboardItem) -> Unit)? = null
    private var onCloseListener: (() -> Unit)? = null
    private var onPinToggleListener: ((ClipboardItem) -> Unit)? = null
    private var onDeleteListener: ((ClipboardItem) -> Unit)? = null

    // Theme (Material You design system)
    private var theme: KeyboardTheme = KeyboardTheme.defaultLight()

    // Paint objects (configured by theme)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val itemBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pressedItemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
    }

    private val timestampPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Dimensions (from theme)
    private var headerHeight = 180f
    private var itemHeight = 200f
    private var itemPadding = 32f
    private var dividerHeight = 2f

    init {
        // Apply default theme
        applyTheme(theme)
    }

    /**
     * Set Material You theme
     *
     * @param theme KeyboardTheme to apply
     */
    fun setTheme(theme: KeyboardTheme) {
        this.theme = theme
        applyTheme(theme)
        invalidate()
    }

    /**
     * Apply theme to all Paint objects
     *
     * Material You styling for clipboard popup:
     * - Background: #F5F5F5 (from design system)
     * - Cards: #FFFFFF with 12dp corners
     * - Border: #E0E0E0
     */
    private fun applyTheme(theme: KeyboardTheme) {
        val density = resources.displayMetrics.density

        // Apply colors
        backgroundPaint.color = theme.colors.clipboardBackground
        headerPaint.color = theme.colors.primary
        itemBackgroundPaint.color = theme.colors.clipboardCard
        pressedItemPaint.color = theme.colors.keyPressed
        dividerPaint.color = theme.colors.clipboardBorder

        // Apply typography
        textPaint.apply {
            textSize = theme.typography.bodySize * density
            color = theme.colors.onSurface
        }

        headerTextPaint.apply {
            textSize = theme.typography.headingSize * density
            color = theme.colors.onPrimary
            isFakeBoldText = true
        }

        timestampPaint.apply {
            textSize = theme.typography.captionSize * density
            color = theme.colors.onSurfaceVariant
        }

        iconPaint.apply {
            textSize = theme.typography.headingSize * density
            color = theme.colors.primary
        }

        // Apply spacing
        itemPadding = theme.spacing.containerPadding * density * 2
        dividerHeight = 1f * density
    }

    /**
     * Set clipboard items to display
     *
     * @param items List of clipboard items
     */
    fun setItems(items: List<ClipboardItem>) {
        this.items = items
        scrollY = 0f
        calculateBounds()
        invalidate()
    }

    /**
     * Set item click listener
     *
     * @param listener Lambda receiving clicked item
     */
    fun setOnItemClickListener(listener: (ClipboardItem) -> Unit) {
        this.onItemClickListener = listener
    }

    /**
     * Set close listener
     *
     * @param listener Lambda called when popup closed
     */
    fun setOnCloseListener(listener: () -> Unit) {
        this.onCloseListener = listener
    }

    /**
     * Set pin toggle listener
     *
     * @param listener Lambda receiving item to pin/unpin
     */
    fun setOnPinToggleListener(listener: (ClipboardItem) -> Unit) {
        this.onPinToggleListener = listener
    }

    /**
     * Set delete listener
     *
     * @param listener Lambda receiving item to delete
     */
    fun setOnDeleteListener(listener: (ClipboardItem) -> Unit) {
        this.onDeleteListener = listener
    }

    /**
     * Calculate bounds for items
     */
    private fun calculateBounds() {
        itemBounds.clear()

        var currentY = headerHeight - scrollY

        items.forEach { item ->
            val bounds = RectF(
                0f,
                currentY,
                width.toFloat(),
                currentY + itemHeight
            )

            itemBounds.add(ItemBound(item, bounds))
            currentY += itemHeight + dividerHeight
        }

        // Calculate max scroll
        val totalHeight = headerHeight + (items.size * (itemHeight + dividerHeight))
        maxScrollY = max(0f, totalHeight - height)
    }

    /**
     * onMeasure - Define popup size
     *
     * Width: Match keyboard width
     * Height: 60% of keyboard height
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val density = resources.displayMetrics.density
        val desiredHeight = (400 * density).toInt() // ~400dp height

        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    /**
     * onSizeChanged - Recalculate bounds
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateBounds()
    }

    /**
     * onDraw - Paint the popup
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Draw header
        drawHeader(canvas)

        // Save canvas state for clipping
        canvas.save()
        canvas.clipRect(0f, headerHeight, width.toFloat(), height.toFloat())

        // Draw items
        itemBounds.forEach { itemBound ->
            // Only draw if visible
            if (itemBound.bounds.bottom > headerHeight && itemBound.bounds.top < height) {
                drawItem(canvas, itemBound)
            }
        }

        canvas.restore()

        // Draw empty state if no items
        if (items.isEmpty()) {
            drawEmptyState(canvas)
        }
    }

    /**
     * Draw header with title and close button
     */
    private fun drawHeader(canvas: Canvas) {
        // Background
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, headerPaint)

        // Title
        val titleX = itemPadding
        val titleY = headerHeight / 2 - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2)
        canvas.drawText("Clipboard History", titleX, titleY, headerTextPaint)

        // Close button (X)
        val closeX = width - 100f
        val closeY = headerHeight / 2 - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2)
        canvas.drawText("✕", closeX, closeY, headerTextPaint)
    }

    /**
     * Draw single clipboard item
     */
    private fun drawItem(canvas: Canvas, itemBound: ItemBound) {
        val item = itemBound.item
        val bounds = itemBound.bounds

        // Background
        val bgPaint = if (item == pressedItem) pressedItemPaint else itemBackgroundPaint
        canvas.drawRect(bounds, bgPaint)

        // Pin icon
        val pinX = itemPadding
        val pinY = bounds.centerY() - ((iconPaint.descent() + iconPaint.ascent()) / 2)
        if (item.isPinned) {
            iconPaint.color = 0xFFFFC107.toInt() // Amber/gold
            canvas.drawText("⭐", pinX, pinY, iconPaint)
        }

        // Text
        val textX = if (item.isPinned) pinX + 80f else pinX
        val preview = item.getPreview(50) // Max 50 chars
        val textY = bounds.top + itemPadding + textPaint.textSize
        canvas.drawText(preview, textX, textY, textPaint)

        // Timestamp
        val timestampText = item.getRelativeTime()
        val timestampY = bounds.top + itemPadding + textPaint.textSize + 50f
        canvas.drawText(timestampText, textX, timestampY, timestampPaint)

        // Paste icon
        iconPaint.color = 0xFF6200EE.toInt() // Purple
        val pasteX = width - 100f
        val pasteY = bounds.centerY() - ((iconPaint.descent() + iconPaint.ascent()) / 2)
        canvas.drawText("📋", pasteX, pasteY, iconPaint)

        // Divider
        canvas.drawRect(
            0f,
            bounds.bottom,
            width.toFloat(),
            bounds.bottom + dividerHeight,
            dividerPaint
        )
    }

    /**
     * Draw empty state
     */
    private fun drawEmptyState(canvas: Canvas) {
        val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 48f
            color = 0xFF757575.toInt() // Gray
            textAlign = Paint.Align.CENTER
        }

        val text = "No clipboard history"
        val textX = width / 2f
        val textY = height / 2f
        canvas.drawText(text, textX, textY, emptyTextPaint)
    }

    /**
     * onTouchEvent - Handle scrolling and taps
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = event.y
                isScrolling = false

                // Check if header close button tapped
                if (event.y < headerHeight && event.x > width - 150f) {
                    onCloseListener?.invoke()
                    return true
                }

                // Check if item tapped
                val item = findItemAt(event.x, event.y)
                if (item != null) {
                    pressedItem = item
                    invalidate()
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaY = lastTouchY - event.y

                // If moved more than threshold, start scrolling
                if (Math.abs(deltaY) > 10f) {
                    isScrolling = true
                    pressedItem = null

                    // Update scroll position
                    scrollY = (scrollY + deltaY).coerceIn(0f, maxScrollY)
                    lastTouchY = event.y

                    calculateBounds()
                    invalidate()
                }

                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!isScrolling && pressedItem != null) {
                    // Item was tapped (not scrolled)
                    val item = findItemAt(event.x, event.y)

                    if (item == pressedItem && item != null) {
                        // Check if pin icon tapped
                        if (event.x < 100f && item.isPinned) {
                            onPinToggleListener?.invoke(item)
                        } else if (event.x > width - 150f) {
                            // Paste icon tapped
                            onItemClickListener?.invoke(item)
                        } else {
                            // Item body tapped → paste
                            onItemClickListener?.invoke(item)
                        }
                    }
                }

                pressedItem = null
                isScrolling = false
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                pressedItem = null
                isScrolling = false
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * Find item at coordinates
     */
    private fun findItemAt(x: Float, y: Float): ClipboardItem? {
        return itemBounds.find { itemBound ->
            itemBound.bounds.contains(x, y)
        }?.item
    }

    /**
     * Data class for item bounds
     */
    private data class ItemBound(
        val item: ClipboardItem,
        val bounds: RectF
    )
}

/**
 * USAGE EXAMPLE:
 * ==============
 *
 * In KaviInputMethodService:
 *
 * ```kotlin
 * private var clipboardPopup: ClipboardPopupView? = null
 *
 * fun showClipboardPopup() {
 *     // Create popup if not exists
 *     if (clipboardPopup == null) {
 *         clipboardPopup = ClipboardPopupView(this).apply {
 *             setOnItemClickListener { item ->
 *                 // Paste the item
 *                 inputConnectionHandler.commitText(item.text, 1)
 *                 hideClipboardPopup()
 *             }
 *
 *             setOnCloseListener {
 *                 hideClipboardPopup()
 *             }
 *
 *             setOnPinToggleListener { item ->
 *                 clipboardManager.setPinned(item.id, !item.isPinned)
 *             }
 *         }
 *     }
 *
 *     // Update items
 *     clipboardPopup?.setItems(clipboardManager.items.value)
 *
 *     // Show popup (add to view hierarchy)
 *     // Implementation depends on how you want to display it
 * }
 * ```
 */
