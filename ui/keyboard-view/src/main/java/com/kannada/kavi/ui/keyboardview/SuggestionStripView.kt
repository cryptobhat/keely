package com.kannada.kavi.ui.keyboardview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.kannada.kavi.features.suggestion.models.Suggestion
import com.kannada.kavi.features.themes.KeyboardTheme

/**
 * SuggestionStripView - The Suggestion Bar Above Keyboard
 *
 * This shows word suggestions as you type, like:
 * [  ನಮಸ್ತೆ  ] [  ನಾನು  ] [  ನಮ್ಮ  ]
 *
 * WHAT IS A SUGGESTION STRIP?
 * ============================
 * It's the horizontal bar above the keyboard that shows predicted words.
 * Tap a suggestion to insert it instantly!
 *
 * WHY USE CANVAS?
 * ===============
 * Same reason as KeyboardView - speed and control!
 * - 60 FPS smooth animations
 * - Pixel-perfect positioning
 * - Low memory usage
 * - Custom styling
 *
 * DESIGN:
 * =======
 * ┌─────────────────────────────────────────┐
 * │ [suggestion1] [suggestion2] [suggestion3] │  ← Suggestion Strip
 * ├─────────────────────────────────────────┤
 * │  Q  W  E  R  T  Y  U  I  O  P           │  ← Keyboard
 * │   A  S  D  F  G  H  J  K  L             │
 * └─────────────────────────────────────────┘
 *
 * INTERACTION:
 * ============
 * - Tap suggestion → Insert word
 * - First suggestion (highest confidence) → Bold/larger
 * - Swipe left/right → See more suggestions (future)
 *
 * VISUAL FEEDBACK:
 * ================
 * - High confidence: Bold, larger font
 * - Medium confidence: Normal font
 * - Low confidence: Gray, smaller font
 * - Pressed state: Darker background
 *
 * PERFORMANCE:
 * ============
 * - Target: < 16ms render (60 FPS)
 * - Touch response: < 50ms
 * - Memory: < 5MB
 */
class SuggestionStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Current suggestions to display
    private var suggestions: List<Suggestion> = emptyList()

    // Bounds for each suggestion (for touch detection)
    private val suggestionBounds = mutableListOf<SuggestionBound>()

    // Currently pressed suggestion
    private var pressedSuggestion: Suggestion? = null

    // Listener for suggestion taps
    private var onSuggestionClickListener: ((Suggestion) -> Unit)? = null

    // Theme (Material You design system)
    private var theme: KeyboardTheme = KeyboardTheme.defaultLight()

    // Paint objects (reused for performance)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val suggestionBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pressedBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = 0xFF212121.toInt() // Almost black
    }

    private val highConfidenceTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 48f
        color = 0xFF000000.toInt() // Pure black
        isFakeBoldText = true
    }

    private val lowConfidenceTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    // Spacing (from theme)
    private var dividerWidth = 2f
    private var horizontalPadding = 16f
    private var verticalPadding = 12f

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
     */
    private fun applyTheme(theme: KeyboardTheme) {
        val density = resources.displayMetrics.density

        // Apply colors
        backgroundPaint.color = theme.colors.suggestionBackground
        suggestionBackgroundPaint.color = theme.colors.surface
        pressedBackgroundPaint.color = theme.colors.keyPressed
        dividerPaint.color = theme.colors.suggestionDivider

        // Apply typography
        textPaint.apply {
            color = theme.colors.suggestionText
            textSize = theme.typography.bodySize * density
        }

        highConfidenceTextPaint.apply {
            color = theme.colors.suggestionText
            textSize = theme.typography.buttonSize * density
            isFakeBoldText = true
        }

        lowConfidenceTextPaint.apply {
            color = theme.colors.onSurfaceVariant
            textSize = theme.typography.captionSize * density
        }

        // Apply spacing
        dividerWidth = 1f * density
        horizontalPadding = theme.spacing.containerPadding * density
        verticalPadding = theme.spacing.containerPadding * density
    }

    /**
     * Set suggestions to display
     *
     * @param suggestions List of suggestions from SuggestionEngine
     */
    fun setSuggestions(suggestions: List<Suggestion>) {
        this.suggestions = suggestions.take(3) // Show max 3 suggestions
        calculateSuggestionBounds()
        invalidate() // Request redraw
    }

    /**
     * Set listener for suggestion clicks
     *
     * @param listener Lambda that receives clicked suggestion
     */
    fun setOnSuggestionClickListener(listener: (Suggestion) -> Unit) {
        this.onSuggestionClickListener = listener
    }

    /**
     * Clear all suggestions
     */
    fun clear() {
        suggestions = emptyList()
        suggestionBounds.clear()
        invalidate()
    }

    /**
     * Calculate bounds for each suggestion
     *
     * Divides the strip width equally among suggestions
     */
    private fun calculateSuggestionBounds() {
        suggestionBounds.clear()

        if (suggestions.isEmpty()) return

        val availableWidth = width - (paddingLeft + paddingRight)
        val suggestionCount = suggestions.size
        val suggestionWidth = availableWidth / suggestionCount.toFloat()

        var currentX = paddingLeft.toFloat()

        suggestions.forEach { suggestion ->
            val bounds = RectF(
                currentX,
                paddingTop.toFloat(),
                currentX + suggestionWidth,
                height.toFloat() - paddingBottom
            )

            suggestionBounds.add(SuggestionBound(suggestion, bounds))
            currentX += suggestionWidth
        }
    }

    /**
     * onMeasure - Define desired height
     *
     * Width: Match parent (full keyboard width)
     * Height: 56dp (same as one keyboard row)
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val density = resources.displayMetrics.density
        val desiredHeight = (56 * density).toInt() + paddingTop + paddingBottom

        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    /**
     * onSizeChanged - Recalculate bounds when size changes
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateSuggestionBounds()
    }

    /**
     * onDraw - Paint the suggestion strip
     *
     * Called 60 times per second for smooth UI
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // If no suggestions, show nothing
        if (suggestions.isEmpty()) {
            return
        }

        // Draw each suggestion
        suggestionBounds.forEachIndexed { index, suggestionBound ->
            drawSuggestion(canvas, suggestionBound, index)

            // Draw divider between suggestions (except after last one)
            if (index < suggestionBounds.size - 1) {
                val dividerX = suggestionBound.bounds.right
                canvas.drawRect(
                    dividerX,
                    paddingTop.toFloat(),
                    dividerX + dividerWidth,
                    height.toFloat() - paddingBottom,
                    dividerPaint
                )
            }
        }
    }

    /**
     * Draw a single suggestion
     *
     * @param canvas The canvas to draw on
     * @param suggestionBound The suggestion and its bounds
     * @param index Position in the list (0 = first/highest confidence)
     */
    private fun drawSuggestion(canvas: Canvas, suggestionBound: SuggestionBound, index: Int) {
        val suggestion = suggestionBound.suggestion
        val bounds = suggestionBound.bounds

        // Choose background paint based on pressed state
        val bgPaint = if (suggestion == pressedSuggestion) {
            pressedBackgroundPaint
        } else {
            suggestionBackgroundPaint
        }

        // Draw suggestion background
        canvas.drawRect(bounds, bgPaint)

        // Choose text paint based on confidence
        val textPaint = when {
            suggestion.isHighConfidence() -> highConfidenceTextPaint
            suggestion.isLowConfidence() -> lowConfidenceTextPaint
            else -> this.textPaint
        }

        // First suggestion (index 0) always gets bold if not low confidence
        val finalTextPaint = if (index == 0 && !suggestion.isLowConfidence()) {
            highConfidenceTextPaint
        } else {
            textPaint
        }

        // Draw suggestion text
        val textX = bounds.centerX()
        val textY = bounds.centerY() - ((finalTextPaint.descent() + finalTextPaint.ascent()) / 2)

        // Truncate text if too long
        val displayText = truncateText(suggestion.word, bounds.width() - horizontalPadding * 2, finalTextPaint)
        canvas.drawText(displayText, textX, textY, finalTextPaint)
    }

    /**
     * Truncate text to fit within width
     *
     * Adds "..." if text is too long
     *
     * @param text Original text
     * @param maxWidth Maximum width in pixels
     * @param paint Paint to measure text width
     * @return Truncated text
     */
    private fun truncateText(text: String, maxWidth: Float, paint: Paint): String {
        val textWidth = paint.measureText(text)

        if (textWidth <= maxWidth) {
            return text
        }

        // Binary search for best length
        var low = 0
        var high = text.length
        var result = ""

        while (low <= high) {
            val mid = (low + high) / 2
            val candidate = text.substring(0, mid) + "..."
            val width = paint.measureText(candidate)

            if (width <= maxWidth) {
                result = candidate
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return result.ifEmpty { "..." }
    }

    /**
     * onTouchEvent - Handle taps on suggestions
     *
     * @param event Touch event
     * @return true if handled
     */
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
                clearPressed()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * Handle touch down
     *
     * Show pressed state
     */
    private fun handleTouchDown(x: Float, y: Float) {
        val suggestionBound = findSuggestionAt(x, y)

        if (suggestionBound != null) {
            pressedSuggestion = suggestionBound.suggestion
            invalidate() // Redraw to show pressed state

            // Haptic feedback
            performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    /**
     * Handle touch up
     *
     * Trigger click if released on same suggestion
     */
    private fun handleTouchUp(x: Float, y: Float) {
        val suggestionBound = findSuggestionAt(x, y)

        // Only trigger click if released on same suggestion as pressed
        if (suggestionBound != null && suggestionBound.suggestion == pressedSuggestion) {
            onSuggestionClickListener?.invoke(suggestionBound.suggestion)
        }

        clearPressed()
    }

    /**
     * Clear pressed state
     */
    private fun clearPressed() {
        pressedSuggestion = null
        invalidate()
    }

    /**
     * Find which suggestion is at the given coordinates
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return SuggestionBound if found, null otherwise
     */
    private fun findSuggestionAt(x: Float, y: Float): SuggestionBound? {
        return suggestionBounds.find { suggestionBound ->
            suggestionBound.bounds.contains(x, y)
        }
    }

    /**
     * Data class to hold suggestion and its bounds
     */
    private data class SuggestionBound(
        val suggestion: Suggestion,
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
 * private lateinit var suggestionStripView: SuggestionStripView
 *
 * override fun onCreateInputView(): View {
 *     // Create container with suggestion strip + keyboard
 *     val container = LinearLayout(this).apply {
 *         orientation = LinearLayout.VERTICAL
 *     }
 *
 *     // Add suggestion strip
 *     suggestionStripView = SuggestionStripView(this).apply {
 *         setOnSuggestionClickListener { suggestion ->
 *             onSuggestionClicked(suggestion)
 *         }
 *     }
 *     container.addView(suggestionStripView)
 *
 *     // Add keyboard view
 *     val keyboardView = KeyboardView(this)
 *     container.addView(keyboardView)
 *
 *     // Observe suggestions from engine
 *     suggestionEngine.suggestions.onEach { suggestions ->
 *         suggestionStripView.setSuggestions(suggestions)
 *     }.launchIn(serviceScope)
 *
 *     return container
 * }
 *
 * private fun onSuggestionClicked(suggestion: Suggestion) {
 *     // Insert the suggestion
 *     inputConnectionHandler.commitText(suggestion.word + " ", 1)
 *
 *     // Learn from user's choice
 *     suggestionEngine.onSuggestionSelected(suggestion)
 *
 *     // Clear suggestions
 *     suggestionStripView.clear()
 * }
 * ```
 *
 * VISUAL STATES:
 * ==============
 * - Normal: White background, dark text
 * - Pressed: Light gray background
 * - High confidence (first): Bold, larger text
 * - Low confidence: Gray, smaller text
 *
 * ACCESSIBILITY:
 * ==============
 * - Haptic feedback on touch
 * - Clear visual states
 * - Large touch targets (56dp height)
 * - High contrast text
 */
