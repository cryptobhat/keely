package com.kannada.kavi.ui.keyboardview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
// Design system removed

/**
 * FontStyleToolbarView - Font Style Selector Toolbar
 *
 * Displays font style options at the top of the keyboard:
 * Default, Written, BOLD, italic, Outline, CAPIT
 *
 * Based on the screenshot design with:
 * - Back button on the left
 * - Horizontal list of font style options
 * - Active style indicator (underline)
 * - Dynamic theming support
 */
class FontStyleToolbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Font style options
    data class FontStyleOption(
        val label: String,
        val isActive: Boolean = false,
        val isItalic: Boolean = false
    )

    private var fontStyles = listOf(
        FontStyleOption("Default", isActive = true),
        FontStyleOption("Written"),
        FontStyleOption("BOLD"),
        FontStyleOption("italic", isItalic = true),
        FontStyleOption("Outline"),
        FontStyleOption("CAPIT")
    )

    // Simple colors (design system removed)
    private val BACKGROUND_COLOR = 0xFFFFFFFF.toInt()
    private val TEXT_COLOR = 0xFF191C1C.toInt()
    private val PRIMARY_COLOR = 0xFF006C5F.toInt()

    // Paint objects
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        isSubpixelText = true
        isLinearText = true
    }

    private val activeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        isSubpixelText = true
        isLinearText = true
    }

    private val underlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val backButtonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Bounds for touch detection
    private val styleBounds = mutableListOf<StyleBound>()
    private var backButtonBounds: RectF? = null

    // Currently pressed item
    private var pressedStyle: FontStyleOption? = null
    private var isBackButtonPressed = false

    // Listeners
    private var onStyleSelectedListener: ((String) -> Unit)? = null
    private var onBackClickListener: (() -> Unit)? = null

    // Dimensions
    private val density: Float
        get() = resources.displayMetrics.density

    private val toolbarHeight = 48f * density // 48dp height
    private val horizontalPadding = 16f * density // 16dp padding
    private val styleSpacing = 24f * density // 24dp between styles
    private val backButtonSize = 24f * density // 24dp back button
    private val underlineHeight = 2f * density // 2dp underline
    private val textSize = 15f * density // 15sp text size

    init {
        applyColors()
    }

    /**
     * Set theme (no-op - design system removed)
     */
    fun setTheme(theme: Any) {
        // Design system removed - using hardcoded values
        applyColors()
        invalidate()
    }

    /**
     * Apply colors
     */
    private fun applyColors() {
        // Background
        backgroundPaint.color = BACKGROUND_COLOR

        // Text colors
        textPaint.color = ColorUtils.setAlphaComponent(TEXT_COLOR, 0x80) // 50% opacity for inactive
        textPaint.textSize = textSize
        textPaint.typeface = android.graphics.Typeface.DEFAULT

        activeTextPaint.color = TEXT_COLOR // Full opacity for active
        activeTextPaint.textSize = textSize
        activeTextPaint.typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD
        )

        // Underline uses primary color
        underlinePaint.color = PRIMARY_COLOR

        // Back button icon
        backButtonPaint.color = TEXT_COLOR
        backButtonPaint.strokeWidth = 2f * density
    }

    /**
     * Set active font style
     */
    fun setActiveStyle(styleLabel: String) {
        fontStyles = fontStyles.map { 
            it.copy(isActive = it.label.equals(styleLabel, ignoreCase = true))
        }
        invalidate()
    }

    /**
     * Set listener for style selection
     */
    fun setOnStyleSelectedListener(listener: (String) -> Unit) {
        this.onStyleSelectedListener = listener
    }

    /**
     * Set listener for back button
     */
    fun setOnBackClickListener(listener: () -> Unit) {
        this.onBackClickListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = toolbarHeight.toInt()
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateBounds()
    }

    /**
     * Calculate bounds for all style options and back button
     */
    private fun calculateBounds() {
        styleBounds.clear()

        val startX = horizontalPadding + backButtonSize + horizontalPadding
        var currentX = startX
        val centerY = height / 2f

        fontStyles.forEach { style ->
            val textWidth = textPaint.measureText(style.label)
            val bounds = RectF(
                currentX,
                0f,
                currentX + textWidth,
                height.toFloat()
            )
            styleBounds.add(StyleBound(style, bounds))
            currentX += textWidth + styleSpacing
        }

        // Back button bounds
        val backButtonX = horizontalPadding
        val backButtonY = centerY - backButtonSize / 2f
        backButtonBounds = RectF(
            backButtonX,
            backButtonY,
            backButtonX + backButtonSize,
            backButtonY + backButtonSize
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Draw back button
        backButtonBounds?.let { bounds ->
            val centerX = bounds.centerX()
            val centerY = bounds.centerY()
            val arrowSize = backButtonSize * 0.4f

            val path = Path().apply {
                moveTo(centerX + arrowSize, centerY - arrowSize)
                lineTo(centerX, centerY)
                lineTo(centerX + arrowSize, centerY + arrowSize)
            }
            canvas.drawPath(path, backButtonPaint)
        }

        // Draw font style options
        styleBounds.forEach { styleBound ->
            val style = styleBound.style
            val bounds = styleBound.bounds
            val paint = if (style.isActive) activeTextPaint else textPaint

            // Apply italic style if needed
            if (style.isItalic) {
                paint.textSkewX = -0.25f
            } else {
                paint.textSkewX = 0f
            }

            // Draw text
            val textY = bounds.centerY() - ((paint.descent() + paint.ascent()) / 2)
            canvas.drawText(style.label, bounds.left, textY, paint)

            // Draw underline for active style
            if (style.isActive) {
                val underlineY = bounds.bottom - underlineHeight * 2
                canvas.drawRect(
                    bounds.left,
                    underlineY,
                    bounds.right,
                    underlineY + underlineHeight,
                    underlinePaint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleTouchDown(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                handleTouchUp(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                clearPressedState()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTouchDown(x: Float, y: Float) {
        // Check back button
        backButtonBounds?.let { bounds ->
            if (bounds.contains(x, y)) {
                isBackButtonPressed = true
                invalidate()
                return
            }
        }

        // Check style options
        styleBounds.forEach { styleBound ->
            if (styleBound.bounds.contains(x, y)) {
                pressedStyle = styleBound.style
                invalidate()
                return
            }
        }
    }

    private fun handleTouchUp(x: Float, y: Float) {
        // Handle back button
        if (isBackButtonPressed) {
            backButtonBounds?.let { bounds ->
                if (bounds.contains(x, y)) {
                    onBackClickListener?.invoke()
                }
            }
            clearPressedState()
            return
        }

        // Handle style selection
        pressedStyle?.let { style ->
            styleBounds.forEach { styleBound ->
                if (styleBound.bounds.contains(x, y) && styleBound.style == style) {
                    onStyleSelectedListener?.invoke(style.label)
                }
            }
            clearPressedState()
        }
    }

    private fun clearPressedState() {
        pressedStyle = null
        isBackButtonPressed = false
        invalidate()
    }

    private data class StyleBound(
        val style: FontStyleOption,
        val bounds: RectF
    )
}

