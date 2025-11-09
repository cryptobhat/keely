package com.kannada.kavi.ui.keyboardview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import com.kannada.kavi.core.layout.models.Key
import com.kannada.kavi.core.layout.models.KeyboardRow
import com.kannada.kavi.features.themes.DeshDesignSystem
import com.kannada.kavi.features.themes.KeyboardTheme
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

    // Keyboard data
    private var rows: List<KeyboardRow> = emptyList()
    private var keyBounds: MutableList<KeyBound> = mutableListOf()

    // Theme (Desh design system)
    private var theme: KeyboardTheme = DeshDesignSystem.createDeshTheme(context)

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

    // Shadow for key depth (Desh design system)
    private val keyShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        maskFilter = android.graphics.BlurMaskFilter(
            DeshDesignSystem.Dimensions.KEY_SHADOW_RADIUS * resources.displayMetrics.density,
            android.graphics.BlurMaskFilter.Blur.NORMAL
        )
    }

    private val keyHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val specialKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val specialKeyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val actionKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val actionKeyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val iconStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
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

    // Spacing between keys - much tighter for modern look
    private var keyHorizontalSpacing = 3f  // Reduced from theme default for tighter layout
    private var keyVerticalSpacing = 4f   // Reduced from theme default for compact height
    private var rowPadding = 4f           // Side padding for the keyboard (in px)

    init {
        // Apply Desh theme
        applyTheme(theme)

        // Apply responsive padding from Desh design system
        val density = resources.displayMetrics.density
        val padding = DeshDesignSystem.getKeyboardPadding(context)
        setPadding(
            (padding.left * density).toInt(),
            (padding.top * density).toInt(),
            (padding.right * density).toInt(),
            (padding.bottom * density).toInt()
        )

        // Set background to match Desh design
        setBackgroundColor(DeshDesignSystem.Colors.KEYBOARD_BACKGROUND)
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
        val density = resources.displayMetrics.density

        // Apply colors
        keyBackgroundPaint.color = theme.colors.keyNormal
        keyPressedPaint.color = theme.colors.keyPressed
        keySelectedPaint.color = theme.colors.keySelected
        specialKeyPaint.color = DeshDesignSystem.Colors.SPECIAL_KEY_BACKGROUND
        specialKeyPressedPaint.color = DeshDesignSystem.Colors.SPECIAL_KEY_PRESSED
        actionKeyPaint.color = DeshDesignSystem.Colors.ACTION_KEY_BACKGROUND
        actionKeyPressedPaint.color = DeshDesignSystem.Colors.ACTION_KEY_PRESSED

        keyBorderPaint.apply {
            color = theme.colors.keyBorder
            strokeWidth = theme.shape.borderWidth * density
        }

        iconStrokePaint.strokeWidth = 2.5f * density

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

        // Apply responsive spacing from Desh design system
        val (horizontalSpacing, verticalSpacing) = DeshDesignSystem.getKeySpacing(context)
        keyHorizontalSpacing = horizontalSpacing * density
        keyVerticalSpacing = verticalSpacing * density
        val padding = DeshDesignSystem.getKeyboardPadding(context)
        rowPadding = padding.left * density

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
        val horizontalInset = rowPadding
        val availableWidth = width - (paddingLeft + paddingRight) - (horizontalInset * 2)
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

            // Center all rows for clean alignment
            // Removed special indentation for second row to maintain proper spacing
            val rowStartX = paddingLeft + horizontalInset + (availableWidth - rowActualWidth) / 2f

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

    private fun drawCustomIcon(canvas: Canvas, key: Key, bounds: RectF): Boolean {
        return when (key.type) {
            com.kannada.kavi.core.layout.models.KeyType.ENTER -> {
                drawSearchIcon(canvas, bounds)
                true
            }
            com.kannada.kavi.core.layout.models.KeyType.DELETE -> {
                drawDeleteIcon(canvas, bounds)
                true
            }
            com.kannada.kavi.core.layout.models.KeyType.SHIFT -> {
                drawShiftIcon(canvas, bounds)
                true
            }
            else -> false
        }
    }

    private fun drawSearchIcon(canvas: Canvas, bounds: RectF) {
        iconStrokePaint.style = Paint.Style.STROKE
        iconStrokePaint.strokeWidth = bounds.height() * 0.08f
        iconStrokePaint.color = DeshDesignSystem.Colors.ACTION_KEY_TEXT

        val radius = min(bounds.width(), bounds.height()) * 0.22f
        val centerX = bounds.centerX() - radius * 0.1f
        val centerY = bounds.centerY() - radius * 0.1f
        canvas.drawCircle(centerX, centerY, radius, iconStrokePaint)

        val handleLength = radius * 1.2f
        canvas.drawLine(
            centerX + radius * 0.8f,
            centerY + radius * 0.8f,
            centerX + radius * 0.8f + handleLength,
            centerY + radius * 0.8f + handleLength,
            iconStrokePaint
        )
    }

    private fun drawDeleteIcon(canvas: Canvas, bounds: RectF) {
        iconStrokePaint.style = Paint.Style.STROKE
        iconStrokePaint.strokeWidth = bounds.height() * 0.065f
        iconStrokePaint.color = DeshDesignSystem.Colors.KEY_TEXT

        val height = bounds.height() * 0.42f
        val width = bounds.width() * 0.55f
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val right = centerX + width / 2f
        val left = centerX - width / 2f
        val top = centerY - height / 2f
        val bottom = centerY + height / 2f
        val triangleWidth = height * 0.65f

        val path = Path().apply {
            moveTo(left - triangleWidth, centerY)
            lineTo(left, top)
            lineTo(right, top)
            lineTo(right, bottom)
            lineTo(left, bottom)
            close()
        }

        canvas.drawPath(path, iconStrokePaint)

        canvas.drawLine(
            left + width * 0.18f,
            top + height * 0.2f,
            right - width * 0.15f,
            bottom - height * 0.2f,
            iconStrokePaint
        )
        canvas.drawLine(
            left + width * 0.18f,
            bottom - height * 0.2f,
            right - width * 0.15f,
            top + height * 0.2f,
            iconStrokePaint
        )
    }

    private fun drawShiftIcon(canvas: Canvas, bounds: RectF) {
        iconStrokePaint.style = Paint.Style.STROKE
        iconStrokePaint.strokeWidth = bounds.height() * 0.06f
        iconStrokePaint.color = DeshDesignSystem.Colors.KEY_TEXT

        val arrowHeight = bounds.height() * 0.42f
        val arrowWidth = bounds.width() * 0.3f
        val centerX = bounds.centerX()
        val baseY = bounds.centerY() + arrowHeight * 0.35f

        val path = Path().apply {
            moveTo(centerX, baseY - arrowHeight)
            lineTo(centerX - arrowWidth, baseY - arrowHeight * 0.35f)
            lineTo(centerX - arrowWidth, baseY)
            lineTo(centerX + arrowWidth, baseY)
            lineTo(centerX + arrowWidth, baseY - arrowHeight * 0.35f)
            close()
        }

        canvas.drawPath(path, iconStrokePaint)

        val rectHeight = arrowHeight * 0.35f
        canvas.drawRoundRect(
            RectF(
                centerX - arrowWidth * 0.7f,
                baseY,
                centerX + arrowWidth * 0.7f,
                baseY + rectHeight
            ),
            rectHeight * 0.35f,
            rectHeight * 0.35f,
            iconStrokePaint
        )
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

        // Get responsive key height from Desh design system
        val keyHeightDp = DeshDesignSystem.getKeyHeight(context)
        val keyHeightPx = keyHeightDp * density

        // Get responsive spacing
        val (_, verticalSpacing) = DeshDesignSystem.getKeySpacing(context)
        val verticalSpacingPx = verticalSpacing * density

        // Get responsive padding
        val padding = DeshDesignSystem.getKeyboardPadding(context)
        val paddingTopPx = padding.top * density
        val paddingBottomPx = padding.bottom * density

        // Calculate total height
        val totalSpacing = (rowCount - 1) * verticalSpacingPx
        var desiredHeight = ((rowCount * keyHeightPx) + totalSpacing + paddingTopPx + paddingBottomPx).toInt()

        // Apply maximum height constraint to prevent keyboard from being too tall
        val maxKeyboardHeightRatio = DeshDesignSystem.getMaxKeyboardHeightRatio(context)
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
        // Very minimal inset to match Desh design - keys should be very close together
        val insetAmount = 1f * resources.displayMetrics.density // Very tight spacing like Desh

        // Apply scale animation if this key is being animated
        val scale = if (key == animatingKey) keyPressScale else 1.0f
        val scaledInset = if (key == animatingKey) {
            val scaleOffset = (1.0f - scale) * bounds.width() / 2f
            insetAmount + scaleOffset
        } else {
            insetAmount
        }

        val drawBounds = RectF(
            bounds.left + scaledInset,
            bounds.top + scaledInset,
            bounds.right - scaledInset,
            bounds.bottom - scaledInset
        )

        // Choose paint based on key type and state
        val isActionKey = key.type == com.kannada.kavi.core.layout.models.KeyType.ENTER
        val isSpecialKey = key.type in specialKeyTypes
        val backgroundPaint = when {
            isActionKey && key == pressedKey -> actionKeyPressedPaint
            isActionKey -> actionKeyPaint
            isSpecialKey && key == pressedKey -> specialKeyPressedPaint
            isSpecialKey || key == selectedKey -> specialKeyPaint
            key == pressedKey -> keyPressedPaint
            else -> keyBackgroundPaint
        }

        // Get corner radius from theme (convert dp to pixels)
        val cornerRadius = theme.shape.keyCornerRadius * resources.displayMetrics.density

        // Draw shadow for depth (Desh design system)
        if (key != pressedKey && theme.shape.borderEnabled) {
            // Draw subtle shadow only when not pressed
            val shadowOffset = DeshDesignSystem.Dimensions.KEY_SHADOW_DY * resources.displayMetrics.density
            val shadowBounds = RectF(
                drawBounds.left,
                drawBounds.top + shadowOffset,
                drawBounds.right,
                drawBounds.bottom + shadowOffset
            )
            // Use Desh shadow color
            keyShadowPaint.color = DeshDesignSystem.Colors.KEY_SHADOW
            canvas.drawRoundRect(shadowBounds, cornerRadius, cornerRadius, keyShadowPaint)
        }

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

        // Draw icon or label
        val displayLabel = when {
            key.type == com.kannada.kavi.core.layout.models.KeyType.SPACE && key.label.isBlank() -> "Desh Keyboard"
            else -> key.label
        }

        val iconDrawn = drawCustomIcon(canvas, key, drawBounds)

        if (!iconDrawn && displayLabel.isNotEmpty()) {
            // Calculate text position (center of key using drawBounds for accurate centering)
            val textX = drawBounds.centerX()
            val textY = drawBounds.centerY() - ((labelPaint.descent() + labelPaint.ascent()) / 2)

            // Desh design system: 18px text size for keys
            val optimalTextSize = (bounds.height() * 0.43f).coerceAtMost(
                theme.typography.buttonSize * resources.displayMetrics.scaledDensity
            )
            labelPaint.textSize = optimalTextSize

            // Set text color based on key type and state
            labelPaint.color = if (key.type == com.kannada.kavi.core.layout.models.KeyType.ENTER) {
                DeshDesignSystem.Colors.ACTION_KEY_TEXT
            } else {
                DeshDesignSystem.Colors.KEY_TEXT
            }

            canvas.drawText(displayLabel, textX, textY, labelPaint)
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

            // Start key press scale animation
            startKeyPressAnimation(key.key)

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
        keyPressAnimator = ValueAnimator.ofFloat(1.0f, DeshDesignSystem.Animations.KEY_PRESS_SCALE).apply {
            duration = DeshDesignSystem.Animations.KEY_PRESS_DURATION
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
            duration = DeshDesignSystem.Animations.KEY_RELEASE_DURATION
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
