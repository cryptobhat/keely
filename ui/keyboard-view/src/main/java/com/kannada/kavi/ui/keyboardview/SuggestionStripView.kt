package com.kannada.kavi.ui.keyboardview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import com.kannada.kavi.features.suggestion.models.Suggestion
import com.kannada.kavi.features.themes.KeyboardDesignSystem
import com.kannada.kavi.ui.keyboardview.R

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

    // Icon resources
    enum class IconType {
        VIEW_COZY,
        CONTENT_PASTE,
        SETTINGS,
        PALETTE,
        MIC
    }

    // Icon drawables
    private var iconDrawables: Map<IconType, Drawable?> = emptyMap()
    
    // Icon bounds for touch detection
    private val iconBounds = mutableListOf<IconBound>()
    
    // Currently pressed icon
    private var pressedIcon: IconType? = null
    
    // Icon click listeners
    private var onIconClickListener: ((IconType) -> Unit)? = null

    private var primaryTextColor = KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC
    private var secondaryTextColor = KeyboardDesignSystem.Colors.KEY_HINT_TEXT_DYNAMIC
    private var iconAccentColor = KeyboardDesignSystem.Colors.ACTION_KEY_BACKGROUND_DYNAMIC
    private var pinnedIconColor = KeyboardDesignSystem.Colors.ACTION_KEY_BACKGROUND_DYNAMIC
    private var pasteIconColor = KeyboardDesignSystem.Colors.ACTION_KEY_BACKGROUND_DYNAMIC

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
    private val selectionIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val bottomBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
    private var chipCornerRadius = 16f

    init {
        applyColors()
        loadIcons()
    }
    
    /**
     * Load icon drawables from resources
     */
    private fun loadIcons() {
        iconDrawables = mapOf(
            IconType.VIEW_COZY to loadIconDrawable(R.drawable.view_cozy_24),
            IconType.CONTENT_PASTE to loadIconDrawable(R.drawable.content_paste_24),
            IconType.SETTINGS to loadIconDrawable(R.drawable.settings_24),
            IconType.PALETTE to loadIconDrawable(R.drawable.palette_24),
            IconType.MIC to loadIconDrawable(R.drawable.mic_24)
        )
    }
    
    /**
     * Load a drawable by resource id, returning null if not found.
     * Returns the original drawable (not mutated) so we can create fresh copies when drawing.
     */
    private fun loadIconDrawable(@DrawableRes resId: Int): Drawable? {
        return ContextCompat.getDrawable(context, resId)
    }
    
    /**
     * Set listener for icon clicks
     */
    fun setOnIconClickListener(listener: (IconType) -> Unit) {
        this.onIconClickListener = listener
    }

    /**
     * Set theme (no-op - design system removed)
     */
    fun setTheme(theme: Any?) {
        refreshColors()
    }

    fun refreshColors() {
        applyColors()
        // Reload icons to ensure they're properly tinted with new colors
        loadIcons()
        invalidate()
    }

    /**
     * Apply colors to all Paint objects
     */
    private fun applyColors() {
        val density = resources.displayMetrics.density
        val keyboardBackground = KeyboardDesignSystem.Colors.KEYBOARD_BACKGROUND_DYNAMIC
        val chipBackground = KeyboardDesignSystem.Colors.KEY_BACKGROUND_DYNAMIC
        val pressedBackground = KeyboardDesignSystem.Colors.KEY_PRESSED_DYNAMIC
        val accentColor = KeyboardDesignSystem.Colors.ACTION_KEY_BACKGROUND_DYNAMIC
        val dividerColor = ColorUtils.setAlphaComponent(
            KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC,
            (0.12f * 255).toInt()
        )
        val borderColor = ColorUtils.setAlphaComponent(
            KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC,
            (0.06f * 255).toInt()
        )

        primaryTextColor = KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC
        secondaryTextColor = KeyboardDesignSystem.Colors.KEY_HINT_TEXT_DYNAMIC
        iconAccentColor = accentColor
        pinnedIconColor = accentColor
        pasteIconColor = accentColor

        setBackgroundColor(keyboardBackground)
        backgroundPaint.color = keyboardBackground
        suggestionBackgroundPaint.color = chipBackground
        pressedBackgroundPaint.color = pressedBackground
        dividerPaint.color = dividerColor
        selectionIndicatorPaint.color = accentColor
        bottomBorderPaint.color = borderColor
        chipCornerRadius = 18f * density

        textPaint.apply {
            color = primaryTextColor
            textSize = 14f * density
        }

        highConfidenceTextPaint.apply {
            color = primaryTextColor
            textSize = 16f * density
            isFakeBoldText = true
        }

        lowConfidenceTextPaint.apply {
            color = secondaryTextColor
            textSize = 12f * density
        }

        dividerWidth = 0f
        horizontalPadding = 16f * density
        verticalPadding = 12f * density
    }

    /**
     * Set suggestions to display
     *
     * @param suggestions List of suggestions from SuggestionEngine
     */
    fun setSuggestions(suggestions: List<Suggestion>) {
        this.suggestions = suggestions.take(3) // Show max 3 suggestions
        calculateIconBounds() // Recalculate icon bounds based on suggestion state
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
        calculateIconBounds() // Recalculate icon bounds to show all icons
        invalidate()
    }

    /**
     * Calculate bounds for each suggestion
     *
     * When suggestions are shown, reserves space for first and last icons,
     * then divides remaining space among suggestions
     */
    private fun calculateSuggestionBounds() {
        suggestionBounds.clear()

        if (suggestions.isEmpty()) return

        val availableWidth = width - (paddingLeft + paddingRight)
        val density = resources.displayMetrics.density
        val singleIconAreaWidth = 48 * density // 24dp icon + 24dp padding on each side
        val totalIconAreaWidth = singleIconAreaWidth * 2
        
        // Calculate space for suggestions (between first and last icons)
        val suggestionAreaWidth = (availableWidth - totalIconAreaWidth).coerceAtLeast(0f)
        val suggestionCount = suggestions.size
        val suggestionWidth = suggestionAreaWidth / suggestionCount.toFloat()

        // Start after first icon area
        var currentX = paddingLeft.toFloat() + singleIconAreaWidth

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
     * Calculate bounds for icons
     * 
     * When idle: Divides the strip width equally among 5 icons
     * When suggestions shown: First icon on left, last icon on right
     */
    private fun calculateIconBounds() {
        iconBounds.clear()
        
        val availableWidth = width - (paddingLeft + paddingRight)
        val density = resources.displayMetrics.density
        val iconAreaWidth = 48 * density // 24dp icon + 24dp total padding
        
        if (suggestions.isEmpty()) {
            // Idle state: divide equally among 5 icons
            val iconCount = 5
            val iconWidth = availableWidth / iconCount.toFloat()
            
            var currentX = paddingLeft.toFloat()
            
            IconType.values().forEach { iconType ->
                val bounds = RectF(
                    currentX,
                    paddingTop.toFloat(),
                    currentX + iconWidth,
                    height.toFloat() - paddingBottom
                )
                
                iconBounds.add(IconBound(iconType, bounds))
                currentX += iconWidth
            }
        } else {
            // Suggestions shown: first icon on left, last icon on right
            val iconTypes = IconType.values()
            
            // First icon (left side)
            iconBounds.add(IconBound(
                iconTypes[0],
                RectF(
                    paddingLeft.toFloat(),
                    paddingTop.toFloat(),
                    paddingLeft + iconAreaWidth,
                    height.toFloat() - paddingBottom
                )
            ))
            
            // Last icon (right side)
            iconBounds.add(IconBound(
                iconTypes[iconTypes.size - 1],
                RectF(
                    width - paddingRight - iconAreaWidth,
                    paddingTop.toFloat(),
                    width - paddingRight.toFloat(),
                    height.toFloat() - paddingBottom
                )
            ))
        }
    }

    /**
     * onMeasure - Define desired height
     *
     * Width: Match parent (full keyboard width)
     * Height: 40dp (reduced from 56dp for more compact design)
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val density = resources.displayMetrics.density
        val desiredHeight = (40 * density).toInt() + paddingTop + paddingBottom

        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    /**
     * onSizeChanged - Recalculate bounds when size changes
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateSuggestionBounds()
        calculateIconBounds()
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

        // If suggestions are empty, show all icons (idle state)
        if (suggestions.isEmpty()) {
            drawIcons(canvas, showAll = true)
            return
        }

        // If suggestions exist, show only first and last icons, and suggestions in between
        drawIcons(canvas, showAll = false)
        
        // Draw each suggestion
        suggestionBounds.forEachIndexed { index, suggestionBound ->
            drawSuggestion(canvas, suggestionBound, index)
        }

        // No border in suggestion bar - separator will be between suggestion bar and keyboard
    }
    
    /**
     * Draw icons in the suggestion bar
     * 
     * @param canvas The canvas to draw on
     * @param showAll If true, show all 5 icons. If false, show only first and last icons.
     */
    private fun drawIcons(canvas: Canvas, showAll: Boolean) {
        if (iconBounds.isEmpty()) {
            calculateIconBounds()
        }
        
        val density = resources.displayMetrics.density
        val iconSize = (24 * density).toInt() // 24dp icon size
        
        iconBounds.forEach { iconBound ->
            val iconType = iconBound.iconType
            val bounds = iconBound.bounds
            
            val originalDrawable = iconDrawables[iconType]
            if (originalDrawable == null) {
                android.util.Log.w("SuggestionStripView", "Icon drawable is null for type: $iconType")
                return@forEach
            }
            
            // Calculate icon position (centered in bounds)
            val iconLeft = bounds.centerX() - iconSize / 2f
            val iconTop = bounds.centerY() - iconSize / 2f
            val iconRight = iconLeft + iconSize
            val iconBottom = iconTop + iconSize
            
            // Draw pressed state background if needed
            if (pressedIcon == iconType) {
                canvas.drawRect(bounds, pressedBackgroundPaint)
            }
            
            // Apply color tinting to the icon based on type
            val iconColor = when (iconType) {
                IconType.VIEW_COZY,
                IconType.CONTENT_PASTE,
                IconType.SETTINGS,
                IconType.PALETTE,
                IconType.MIC -> iconAccentColor
            }

            // Debug logging for icon tinting
            android.util.Log.d("SuggestionStripView", "Drawing icon $iconType with color: #${Integer.toHexString(iconColor)}")

            // Get a fresh mutable copy from constant state to avoid state pollution
            // This ensures each draw uses a clean drawable instance
            val freshDrawable = originalDrawable.constantState?.newDrawable()?.mutate()
                ?: originalDrawable.mutate()
            
            // Set bounds on the fresh drawable
            freshDrawable.setBounds(
                iconLeft.toInt(),
                iconTop.toInt(),
                iconRight.toInt(),
                iconBottom.toInt()
            )
            
            // Wrap with DrawableCompat for proper vector drawable tinting
            val tintedDrawable = DrawableCompat.wrap(freshDrawable).mutate()

            // Apply tint to drawable using DrawableCompat for proper vector support
            DrawableCompat.setTint(tintedDrawable, iconColor)

            // Set proper tint mode for color replacement
            DrawableCompat.setTintMode(tintedDrawable, android.graphics.PorterDuff.Mode.SRC_IN)

            // Draw the icon
            tintedDrawable.draw(canvas)
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
        val isPrimary = index == 0

        val textPaint = when {
            isPrimary -> highConfidenceTextPaint
            suggestion.isLowConfidence() -> lowConfidenceTextPaint
            else -> this.textPaint
        }

        val maxWidth = bounds.width() - horizontalPadding * 2
        val text = truncateText(suggestion.word, maxWidth, textPaint)
        val textY = bounds.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(text, bounds.centerX(), textY, textPaint)

        // Removed unnecessary underlines and dividers - cleaner design
        // Only show visual feedback on press if needed (can be added back later)
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
        // Check if icon was pressed first
        val iconBound = findIconAt(x, y)
        if (iconBound != null) {
            pressedIcon = iconBound.iconType
            invalidate() // Redraw to show pressed state
            performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            return
        }
        
        // Check if suggestion was pressed
        val suggestionBound = findSuggestionAt(x, y)
        if (suggestionBound != null) {
            pressedSuggestion = suggestionBound.suggestion
            invalidate() // Redraw to show pressed state
            performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    /**
     * Handle touch up
     *
     * Trigger click if released on same suggestion or icon
     */
    private fun handleTouchUp(x: Float, y: Float) {
        // Check if icon was released
        val iconBound = findIconAt(x, y)
        if (iconBound != null && iconBound.iconType == pressedIcon) {
            onIconClickListener?.invoke(iconBound.iconType)
            clearPressed()
            return
        }
        
        // Check if suggestion was released
        val suggestionBound = findSuggestionAt(x, y)
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
        pressedIcon = null
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
     * Find which icon is at the given coordinates
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return IconBound if found, null otherwise
     */
    private fun findIconAt(x: Float, y: Float): IconBound? {
        // iconBounds only contains visible icons (all 5 when idle, first+last when suggestions shown)
        return iconBounds.find { iconBound ->
            iconBound.bounds.contains(x, y)
        }
    }

    /**
     * Data class to hold suggestion and its bounds
     */
    private data class SuggestionBound(
        val suggestion: Suggestion,
        val bounds: RectF
    )
    
    /**
     * Data class to hold icon and its bounds
     */
    private data class IconBound(
        val iconType: IconType,
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
