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
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
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
import kotlin.math.max
import kotlin.math.abs
import kotlin.math.sqrt

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
    private var isSwipeDeleteEnabled = true
    private var isSwipeCursorEnabled = true
    private var swipeSensitivity = 1.0f  // Default normal sensitivity

    // Swipe word callback - will be set by IME service
    private var onSwipeWord: ((String) -> Unit)? = null
    private var commaEmojiLongPressListener: (() -> Unit)? = null
    private var emojiLongPressListener: ((String) -> Unit)? = null
    private var customLongPressHandled = false

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

    // Material You icon drawables
    private var iconEnter: Drawable? = null
    private var iconEnterSend: Drawable? = null
    private var iconEnterSearch: Drawable? = null
    private var iconEnterDone: Drawable? = null
    private var iconDelete: Drawable? = null
    private var iconShift: Drawable? = null
    private var iconLanguage: Drawable? = null
    private var iconEmoji: Drawable? = null

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
    private var isAnimationActive = false  // Track if any animation is running to avoid partial invalidation conflicts

    // Layout change animation (e.g., number row toggle)
    private lateinit var layoutAnimator: ValueAnimator

    // Key listener (sends key presses to InputMethodService)
    private var keyPressListener: ((Key) -> Unit)? = null

    // Swipe gesture listener for cursor movement and selection
    private var swipeGestureListener: ((String, Int) -> Unit)? = null

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

        // Load Material You icon drawables
        loadIconDrawables()

        // Initialize swipe gesture detector
        initializeSwipeComponents()
    }

    /**
     * Load Material You icon drawables from resources
     */
    private fun loadIconDrawables() {
        iconEnter = loadIconDrawable(R.drawable.keyboard_return_24)
        iconEnterSend = loadIconDrawable(R.drawable.send_24)
        iconEnterSearch = loadIconDrawable(R.drawable.search_24)
        iconEnterDone = loadIconDrawable(R.drawable.check_24)
        iconDelete = loadIconDrawable(R.drawable.backspace_24)
        iconShift = loadIconDrawable(R.drawable.arrow_upward_24)
        iconLanguage = loadIconDrawable(R.drawable.language)
        iconEmoji = loadIconDrawable(R.drawable.mood_24)
    }
    
    /**
     * Load a drawable by id, returning null if not found
     */
    private fun loadIconDrawable(@DrawableRes resId: Int): Drawable? {
        return ContextCompat.getDrawable(context, resId)?.mutate()
    }

    /**
     * Initialize swipe typing and gesture components
     */
    private fun initializeSwipeComponents() {
        // Create swipe gesture detector
        swipeGestureDetector = SwipeGestureDetector().apply {
            setDensity(density)
            setSensitivity(swipeSensitivity)  // Apply sensitivity setting
            setListener(object : SwipeGestureDetector.GestureListener {
                override fun onTap(x: Float, y: Float) {
                    // Tap is handled by normal touch flow - do nothing here
                    // The detector returns false for taps, so normal handling continues
                }

                override fun onLongPress(x: Float, y: Float) {
                    // Handle long press for popup
                    val keyBound = findKeyAt(x, y)
                    if (keyBound != null && keyBound.key.showPopup && !keyBound.key.longPressKeys.isNullOrEmpty()) {
                        // Show popup for long press - will be handled by the popup system
                        pressedKey = keyBound.key
                        longPressHandler.removeCallbacksAndMessages(null)
                        // The long press popup is shown by the scheduleLongPress system
                        invalidate()
                    }
                }

                override fun onSwipeStart(x: Float, y: Float) {
                    // Start swipe path visualization
                    // Coordinates are already in KeyboardView's coordinate space
                    // Validate coordinates are within bounds
                    if (x < 0 || x > width || y < 0 || y > height) {
                        android.util.Log.w("KeyboardView", "onSwipeStart: Coordinates out of bounds: ($x, $y) in view ${width}x${height}")
                    }

                    // Ensure SwipePathView is aligned with our bounds
                    swipePathView?.let { pathView ->
                        // Verify SwipePathView size matches KeyboardView
                        if (pathView.width != width || pathView.height != height) {
                            android.util.Log.w("KeyboardView", "SwipePathView size mismatch: view=$width x $height, pathView=${pathView.width} x ${pathView.height}")
                        }
                    }

                    // Log coordinate mapping for debugging
                    val key = findKeyAt(x, y)
                    android.util.Log.d("KeyboardView", "SwipeStart at ($x, $y) - key: ${key?.key?.label ?: "none"}")

                    swipePathView?.startSwipe(x, y)
                }

                override fun onSwipeMove(x: Float, y: Float, path: List<android.graphics.PointF>) {
                    // Update swipe path visualization
                    swipePathView?.updatePath(x, y)

                    // Track which key is being swiped over
                    findKeyAt(x, y)?.let { keyBound ->
                        val key = keyBound.key
                        val keyText = key.output.ifBlank { key.label }

                        if (!isSpecialKey(key) && keyText.isNotEmpty()) {
                            val sequence = swipeGestureDetector?.getKeySequence() ?: emptyList()
                            val lastKey = sequence.lastOrNull()

                            // Spatial deduplication: only skip if we're still over the same physical key
                            val shouldAdd = if (keyText == lastKey) {
                                // Check if we've moved to a different physical key with same label
                                // This can happen with duplicate letters (like 'l' in 'hello')
                                val lastAddedAt = swipeGestureDetector?.getLastKeyAddedAt()
                                if (lastAddedAt != null) {
                                    val distanceMoved = kotlin.math.sqrt(
                                        (x - lastAddedAt.x) * (x - lastAddedAt.x) +
                                        (y - lastAddedAt.y) * (y - lastAddedAt.y)
                                    )
                                    // Add if we've moved significantly (more than half key width)
                                    distanceMoved > keyBound.bounds.width() * 0.5f
                                } else {
                                    true
                                }
                            } else {
                                true  // Different letter, always add
                            }

                            if (shouldAdd) {
                                swipeGestureDetector?.addKeyToSequence(keyText)
                                swipeGestureDetector?.setLastKeyAddedAt(android.graphics.PointF(x, y))
                                android.util.Log.d("KeyboardView", "Added key: '$keyText' at ($x, $y)")
                            }
                        }
                    }
                }

                override fun onSwipeEnd(gesture: SwipeGesture) {
                    // End swipe and extract text
                    swipePathView?.endSwipe()

                    // Handle different gesture types
                    when (gesture.type) {
                        SwipeType.SWIPE_TYPE -> {
                            if (!isSwipeTypingEnabled) return@onSwipeEnd
                            // Extract word using probabilistic detection
                            val word = extractWordFromGesture(gesture)
                            android.util.Log.d("KeyboardView", "SWIPE_TYPE: extracted word='$word'")
                            if (word.isNotEmpty()) {
                                onSwipeWord?.invoke(word)
                            }
                        }
                        SwipeType.SWIPE_DELETE -> {
                            if (!isSwipeDeleteEnabled) return@onSwipeEnd
                            // Quick left swipe - delete word
                            android.util.Log.d("KeyboardView", "SWIPE_DELETE: deleting word")
                            // Use swipe gesture listener for word deletion
                            swipeGestureListener?.invoke("delete_word", 1)
                        }
                        SwipeType.SWIPE_CURSOR -> {
                            val startKeyType = findKeyAt(gesture.startX, gesture.startY)?.key?.type
                            val startsOnSpace = startKeyType == com.kannada.kavi.core.layout.models.KeyType.SPACE
                            val startsNearBottom = gesture.startY >= height * 0.65f
                            if (isSwipeTypingEnabled && !startsOnSpace && !startsNearBottom) {
                                android.util.Log.d("KeyboardView", "SWIPE_CURSOR fallback to swipe typing - start key=$startKeyType")
                                val word = extractWordFromGesture(gesture)
                                if (word.isNotEmpty()) {
                                    onSwipeWord?.invoke(word)
                                }
                                return@onSwipeEnd
                            }
                            if (!isSwipeCursorEnabled) {
                                android.util.Log.d("KeyboardView", "SWIPE_CURSOR: disabled, ignoring")
                                return@onSwipeEnd
                            }
                            // Horizontal swipe - move cursor or select text
                            val distancePx = gesture.endX - gesture.startX
                            val distanceDp = distancePx / resources.displayMetrics.density
                            
                            // Calculate steps: 1 step per ~15dp of movement (more responsive)
                            // Ensure at least 1 step for any meaningful swipe
                            val steps = when {
                                distanceDp > 0 -> max(1, (distanceDp / 15f).toInt())
                                distanceDp < 0 -> min(-1, (distanceDp / 15f).toInt())
                                else -> 0
                            }

                            // Check if this is a selection gesture (long press + swipe)
                            val isSelection = gesture.duration > 500 // Long press for selection

                            android.util.Log.d("KeyboardView", "SWIPE_CURSOR: dist=${distanceDp}dp, steps=$steps, selection=$isSelection, enabled=$isSwipeCursorEnabled")
                            
                            if (isSelection) {
                                android.util.Log.d("KeyboardView", "SWIPE_SELECT: selecting ${abs(steps)} characters")
                                if (steps > 0) {
                                    swipeGestureListener?.invoke("select_right", steps)
                                } else if (steps < 0) {
                                    swipeGestureListener?.invoke("select_left", -steps)
                                }
                            } else {
                                android.util.Log.d("KeyboardView", "SWIPE_CURSOR: moving cursor $steps steps")
                                if (steps > 0) {
                                    swipeGestureListener?.invoke("cursor_right", steps)
                                } else if (steps < 0) {
                                    swipeGestureListener?.invoke("cursor_left", -steps)
                                }
                            }
                        }
                        SwipeType.SWIPE_SHIFT -> {
                            // Quick upward swipe - toggle shift
                            android.util.Log.d("KeyboardView", "SWIPE_SHIFT: toggling shift")
                            // Find shift key and trigger it
                            rows.flatMap { it.keys }.find { it.type == com.kannada.kavi.core.layout.models.KeyType.SHIFT }?.let { shiftKey ->
                                keyPressListener?.invoke(shiftKey)
                            }
                        }
                        SwipeType.QUICK_SWIPE -> {
                            // Generic quick swipe - could be used for other actions
                            android.util.Log.d("KeyboardView", "QUICK_SWIPE: direction=${gesture.direction}")
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
     * Check if a key is a special key (not a letter)
     */
    private fun isSpecialKey(key: Key): Boolean {
        // Check if it's not a character key (using isCharacter property from Key)
        return !key.isCharacter || key.label in listOf("123", "?123", "abc", "ABC", "⇧")
    }

    /**
     * Extracts a swipe word using the recorded key sequence, falling back to path sampling.
     */
    private fun extractWordFromSwipe(path: List<android.graphics.PointF>): String {
        val sequenceWord = swipeGestureDetector
            ?.getKeySequence()
            ?.let { buildWordFromKeySequence(it) }
            .orEmpty()

        if (sequenceWord.isNotEmpty()) {
            android.util.Log.d("KeyboardView", "Sequence-based swipe word detected='$sequenceWord'")
            return sequenceWord
        }

        return extractWordFromPath(path)
    }

    /**
     * Builds a word from the collected key sequence (already filtered to character keys).
     */
    private fun buildWordFromKeySequence(sequence: List<String>): String {
        if (sequence.isEmpty()) return ""

        val letters = mutableListOf<String>()
        var lastValue: String? = null

        sequence.forEach { raw ->
            val value = raw.trim()
            if (value.isNotEmpty() && value != lastValue) {
                letters.add(value)
                lastValue = value
            }
        }

        return if (letters.size >= 2) letters.joinToString("") else ""
    }

    /**
     * Extract word from swipe path - PRACTICAL APPROACH
     * Focus on what actually works: start/end keys + path sampling
     */
    private fun extractWordFromPath(path: List<android.graphics.PointF>, velocity: Float = 0f): String {
        if (path.size < 3) {
            android.util.Log.d("KeyboardView", "Path too short: ${path.size} points")
            return ""  // Too short for a word
        }

        val letters = mutableListOf<String>()
        var lastKey: Key? = null
        var keyChangeCount = 0

        // Path points are already in view coordinates (from SwipeGestureDetector)
        // No coordinate translation needed - use points directly

        // Use distance-based sampling to ensure we don't miss keys during fast swipes
        // This ensures we check points at regular spatial intervals, not just count intervals
        val sampledPoints = mutableListOf<Pair<Int, android.graphics.PointF>>()
        sampledPoints.add(0 to path[0])  // Always include start point

        // Calculate minimum sample distance based on average key width
        val avgKeyWidth = if (keyBounds.isNotEmpty()) {
            keyBounds.map { it.bounds.width() }.average().toFloat()
        } else {
            50f * density  // Default to 50dp if no keys loaded
        }

        // Dynamic threshold based on swipe velocity (adaptive sampling)
        // Fast swipes (high velocity) use lower threshold to capture more keys in long words
        // Slow swipes use higher threshold for precision
        val velocityFactor = (velocity / 1000f).coerceIn(0.2f, 2.0f)  // Normalize velocity to 0.2-2.0 multiplier
        val dynamicThreshold = avgKeyWidth * (0.25f / velocityFactor)  // Lower threshold for fast swipes
        val minSampleDistance = dynamicThreshold.coerceIn(avgKeyWidth * 0.15f, avgKeyWidth * 0.4f)  // Clamp between sensible bounds

        var lastSampledPoint = path[0]
        for (i in 1 until path.size - 1) {
            val point = path[i]
            val dx = point.x - lastSampledPoint.x
            val dy = point.y - lastSampledPoint.y
            val distance = sqrt(dx * dx + dy * dy)

            // Sample if we've moved far enough OR this is a direction change
            if (distance >= minSampleDistance) {
                sampledPoints.add(i to point)
                lastSampledPoint = point
            }
        }

        // Always include end point
        if (path.size > 1) {
            sampledPoints.add(path.size - 1 to path.last())
        }

        android.util.Log.d("KeyboardView", "Extracting word: path=${path.size} pts, sampled=${sampledPoints.size} pts, min dist=${minSampleDistance}px")

        // Track keys with their positions to avoid duplicates and improve accuracy
        val keyPositions = mutableMapOf<Key, Int>()  // Key -> first occurrence index
        var lastKeyPosition: android.graphics.PointF? = null

        // Store avgKeyWidth for use in spatial deduplication
        val keyWidth = avgKeyWidth

         for ((originalIndex, point) in sampledPoints) {
             
             // Use point coordinates directly (already in view space from SwipeGestureDetector)
             // Ensure coordinates are within view bounds
             val clampedX = point.x.coerceIn(0f, width.toFloat())
             val clampedY = point.y.coerceIn(0f, height.toFloat())
             
             findKeyAt(clampedX, clampedY)?.let { keyBound ->
                 val key = keyBound.key
                 val keyText = key.output.ifBlank { key.label }
                 
                 // Only process letter keys (single character, not special)
                 if (!isSpecialKey(key) && keyText.isNotEmpty()) {
                     // Better spatial deduplication: only add if we've moved to a different key
                     // OR we've returned to a key after visiting another one
                     val isNewKey = key != lastKey

                     // Check spatial distance from last key position to avoid duplicates
                     val hasSpatiallyMoved = if (lastKeyPosition != null) {
                         val spatialDx = clampedX - lastKeyPosition!!.x
                         val spatialDy = clampedY - lastKeyPosition!!.y
                         val spatialDistance = sqrt(spatialDx * spatialDx + spatialDy * spatialDy)
                         spatialDistance > keyWidth * 0.2f  // Moved at least 20% of key width (reduced from 30% for faster swipes)
                     } else {
                         true  // First key, always add
                     }

                     if (isNewKey && hasSpatiallyMoved) {
                         letters.add(keyText)
                         keyPositions[key] = originalIndex
                         lastKey = key
                         lastKeyPosition = android.graphics.PointF(clampedX, clampedY)
                         keyChangeCount++
                         android.util.Log.d("KeyboardView", "Added letter: '$keyText' at index $originalIndex, pos=($clampedX, $clampedY), bounds=${keyBound.bounds}")
                     }
                 }
             } ?: run {
                 // Log when point doesn't match any key (helps debug coordinate issues)
                 if (sampledPoints.size < 50 && originalIndex % 5 == 0) {  // Limit logging to avoid spam
                     android.util.Log.v("KeyboardView", "No key found at path point $originalIndex: ($clampedX, $clampedY)")
                 }
             }
         }

         // Also check the last point explicitly (might be missed by sampling)
         if (path.isNotEmpty()) {
             val lastPoint = path.last()
             val clampedX = lastPoint.x.coerceIn(0f, width.toFloat())
             val clampedY = lastPoint.y.coerceIn(0f, height.toFloat())
             
             findKeyAt(clampedX, clampedY)?.let { keyBound ->
                 val key = keyBound.key
                 val keyText = key.output.ifBlank { key.label }
                 if (!isSpecialKey(key) && key != lastKey && keyText.isNotEmpty()) {
                     letters.add(keyText)
                     keyChangeCount++
                     android.util.Log.d("KeyboardView", "Added final letter: '$keyText' at ($clampedX, $clampedY)")
                 }
             }
         }

        val word = if (keyChangeCount >= 2) letters.joinToString("") else ""
        android.util.Log.d("KeyboardView", "Extracted word: '$word' from $keyChangeCount unique keys (path had ${path.size} points)")
        return word
    }

    /**
     * Extract word from swipe gesture using probabilistic key detection
     * Based on FlorisBoard's statistical approach with resampled paths
     */
    private fun extractWordFromGesture(gesture: SwipeGesture): String {
        // Use the resampled path for consistent analysis
        val resampledPath = gesture.resampledPath
        if (resampledPath.size < SwipeAlgorithms.RESAMPLE_POINTS / 2) {
            android.util.Log.d("KeyboardView", "Resampled path too short: ${resampledPath.size} points")
            return extractWordFromPath(gesture.path, gesture.velocity)  // Fallback to old method with velocity
        }

        val keyProbabilities = mutableMapOf<Int, MutableMap<Key, Float>>()  // Sample index -> Key probabilities
        val detectedKeys = mutableListOf<Pair<Key, Float>>()  // Key sequence with probabilities

        // For each resampled point, calculate probability for all nearby keys
        resampledPath.forEachIndexed { index, point ->
            val probabilities = mutableMapOf<Key, Float>()

            // Convert normalized point back to view coordinates
            val viewPoint = SwipeAlgorithms.denormalizePoint(
                point, 0f, 0f, keyboardWidth, keyboardHeight
            )

            // Check all keys for probability (not just the closest one)
            keyBounds.forEach { keyBound ->
                val key = keyBound.key
                if (!isSpecialKey(key) && key.output.isNotEmpty()) {
                    // Calculate key center in normalized coordinates
                    val keyCenterX = (keyBound.bounds.centerX() / keyboardWidth)
                    val keyCenterY = (keyBound.bounds.centerY() / keyboardHeight)
                    val keyCenter = SwipeAlgorithms.NormalizedPoint(keyCenterX, keyCenterY)

                    // Calculate key dimensions in normalized coordinates
                    val keyWidth = keyBound.bounds.width() / keyboardWidth
                    val keyHeight = keyBound.bounds.height() / keyboardHeight

                    // Calculate probability using Gaussian distribution
                    val probability = SwipeAlgorithms.keyHitProbability(
                        point, keyCenter, keyWidth, keyHeight
                    )

                    if (probability > 0.01f) {  // Threshold to avoid noise
                        probabilities[key] = probability
                    }
                }
            }

            keyProbabilities[index] = probabilities
        }

        // Extract key sequence using Viterbi-like algorithm
        // Find the most probable key sequence considering transitions
        val keySequence = mutableListOf<String>()
        var lastKey: Key? = null

        for (i in resampledPath.indices) {
            val probabilities = keyProbabilities[i] ?: continue

            if (probabilities.isNotEmpty()) {
                // Get the most probable key at this position
                val (bestKey, bestProb) = probabilities.maxByOrNull { it.value } ?: continue

                // Add key if it's different from the last one (avoid duplicates)
                // Or if probability is very high (strong hit)
                if (bestKey != lastKey || bestProb > 0.8f) {
                    val keyText = bestKey.output.ifBlank { bestKey.label }
                    if (keyText.isNotEmpty() && (bestKey != lastKey)) {
                        keySequence.add(keyText)
                        detectedKeys.add(bestKey to bestProb)
                        lastKey = bestKey

                        android.util.Log.d("KeyboardView",
                            "Probabilistic detection: '$keyText' at sample $i with prob=${bestProb}")
                    }
                }
            }
        }

        // Build word from key sequence
        val word = if (keySequence.size >= 2) keySequence.joinToString("") else ""
        android.util.Log.d("KeyboardView",
            "Probabilistic extraction: '$word' from ${keySequence.size} keys (${resampledPath.size} samples)")

        // If probabilistic method fails, fallback to old method
        return if (word.isEmpty() && gesture.path.size > 3) {
            extractWordFromPath(gesture.path)
        } else {
            word
        }
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
     * OPTIMIZATION: Pre-configure all Paint objects to avoid modifications during onDraw
     */
    private fun applyColors() {
        val density = resources.displayMetrics.density

        // Re-read colors from design system each time to get latest values
        // This ensures we get the updated colors after theme changes
        keyBackgroundPaint.color = KeyboardDesignSystem.Colors.KEY_BACKGROUND_DYNAMIC
        keyPressedPaint.color = KeyboardDesignSystem.Colors.KEY_PRESSED_DYNAMIC
        keySelectedPaint.color = KeyboardDesignSystem.Colors.SPECIAL_KEY_BACKGROUND_DYNAMIC
        specialKeyPaint.color = KeyboardDesignSystem.Colors.SPECIAL_KEY_BACKGROUND_DYNAMIC
        specialKeyPressedPaint.color = KeyboardDesignSystem.Colors.SPECIAL_KEY_PRESSED_DYNAMIC
        actionKeyPaint.color = KeyboardDesignSystem.Colors.ACTION_KEY_BACKGROUND_DYNAMIC
        actionKeyPressedPaint.color = KeyboardDesignSystem.Colors.ACTION_KEY_PRESSED_DYNAMIC
        spacebarPaint.color = KeyboardDesignSystem.Colors.SPACEBAR_TEXT_DYNAMIC

        android.util.Log.d("KeyboardView", "applyColors: keyBackground=${Integer.toHexString(keyBackgroundPaint.color)}, " +
            "specialKey=${Integer.toHexString(specialKeyPaint.color)}, " +
            "actionKey=${Integer.toHexString(actionKeyPaint.color)}")

        keyBorderPaint.apply {
            color = 0x00000000.toInt()  // No border
            strokeWidth = 0f
        }

        iconStrokePaint.apply {
            strokeWidth = KeyboardDesignSystem.Dimensions.ICON_STROKE_WIDTH * density
            // Pre-configure all icon stroke attributes
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        // Pre-configure label paint completely
        labelPaint.apply {
            color = KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC
            textSize = KeyboardDesignSystem.getTextSize(context, KeyboardDesignSystem.TextType.KEY_LABEL)
            typeface = android.graphics.Typeface.DEFAULT
            isSubpixelText = true
            isLinearText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Pre-configure hint paint
        hintPaint.apply {
            color = KeyboardDesignSystem.Colors.KEY_HINT_TEXT_DYNAMIC
            textSize = KeyboardDesignSystem.getTextSize(context, KeyboardDesignSystem.TextType.KEY_HINT)
            isAntiAlias = true
            isSubpixelText = true
        }

        // Pre-configure spacebar paint
        spaceBarPaint.apply {
            color = KeyboardDesignSystem.Colors.SPACEBAR_TEXT_DYNAMIC
            textSize = KeyboardDesignSystem.getTextSize(context, KeyboardDesignSystem.TextType.SPACEBAR)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isSubpixelText = true
        }

        // Pre-configure ripple paint
        ripplePaint.apply {
            color = 0x1F000000.toInt()  // Light ripple
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Pre-configure shadow paint with proper opacity (5% for subtle elevation)
        keyShadowPaint.apply {
            color = ColorUtils.setAlphaComponent(0xFF000000.toInt(), 13)  // ~5% opacity (13/255)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Apply chip-style spacing from design system, scaled by heightPercentage
        keyHorizontalSpacing = KeyboardDesignSystem.Dimensions.KEY_HORIZONTAL_GAP * density
        keyVerticalSpacing = (KeyboardDesignSystem.Dimensions.ROW_VERTICAL_GAP * density * heightPercentage / 100f)
        val padding = KeyboardDesignSystem.getKeyboardPadding(context)
        rowPaddingStart = padding.left
        rowPaddingEnd = padding.right

        // Enable hardware acceleration for smooth animations
        // Use software layer only when necessary for specific operations
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }
    
    /**
     * Refresh colors (call when dynamic theme changes)
     * Only triggers redraw, NOT layout recalculation (colors don't affect layout)
     */
    fun refreshColors() {
        android.util.Log.d("KeyboardView", "refreshColors() called")

        // Force Paint objects to update by recreating them with new colors
        applyColors()

        // Update background color
        val newBgColor = KeyboardDesignSystem.Colors.KEYBOARD_BACKGROUND_DYNAMIC
        setBackgroundColor(newBgColor)
        android.util.Log.d("KeyboardView", "Background color set to: ${Integer.toHexString(newBgColor)}")

        // Only redraw - DO NOT call requestLayout() as colors don't affect dimensions
        invalidate()

        android.util.Log.d("KeyboardView", "Colors refreshed and view invalidated")
    }

    /**
     * Set the keyboard rows to display
     *
     * @param rows List of keyboard rows from LayoutManager
     */
    fun setKeyboard(rows: List<KeyboardRow>) {
        android.util.Log.d("KeyboardView", "setKeyboard: Old rows count = ${this.rows.size}, New rows count = ${rows.size}")
        val rowsChanged = this.rows != rows
        val rowCountChanged = this.rows.size != rows.size
        this.rows = rows

        if (rowsChanged) {
            // CRITICAL: Recalculate key bounds when layout changes
            // This ensures gesture tracking aligns with visual layout
            // Must happen after layout is measured (width/height > 0)
            if (width > 0 && height > 0) {
                calculateKeyBounds()
                android.util.Log.d("KeyboardView", "setKeyboard: Key bounds recalculated (${keyBounds.size} keys)")

                // Update swipe key bounds if swipe typing is enabled
                if (isSwipeTypingEnabled) {
                    updateSwipeKeyBounds()
                }

                // If row count changed (e.g., number row toggled), animate the layout change
                if (rowCountChanged) {
                    animateLayoutChange()
                } else {
                    invalidate() // Request redraw for content changes
                }
            } else {
                // Layout not measured yet - will recalculate in onSizeChanged
                android.util.Log.d("KeyboardView", "setKeyboard: Layout not measured yet, will recalculate in onSizeChanged")
                requestLayout() // Trigger measurement
            }
            android.util.Log.d("KeyboardView", "setKeyboard: Rows updated successfully")
        }
    }

    /**
     * Animate keyboard layout changes (e.g., number row toggle, layer switch)
     * Creates smooth transition when row count changes
     */
    private fun animateLayoutChange() {
        // Cancel any existing layout animation
        if (::layoutAnimator.isInitialized && layoutAnimator.isRunning) {
            layoutAnimator.cancel()
        }

        val oldHeight = measuredHeight

        // Trigger re-measurement with new row count
        requestLayout()

        // Use handler to wait for layout measurement to complete (single frame)
        Handler(Looper.getMainLooper()).post {
            val newHeight = measuredHeight

            // Only animate if height actually changed
            if (oldHeight != newHeight && oldHeight > 0) {
                android.util.Log.d("KeyboardView", "animateLayoutChange: $oldHeight -> $newHeight px (ratio: ${newHeight.toFloat() / oldHeight})")

                // Store original layout params
                val lp = layoutParams

                layoutAnimator = ValueAnimator.ofInt(oldHeight, newHeight).apply {
                    duration = 200 // Smooth 200ms transition
                    interpolator = DecelerateInterpolator(1.5f) // Ease-out animation

                    addUpdateListener { valueAnimator ->
                        val currentHeight = valueAnimator.animatedValue as Int

                        // Update view height through layout params
                        lp?.let {
                            it.height = currentHeight
                            layoutParams = it
                        }
                        invalidate()
                    }

                    doOnEnd {
                        // Ensure final state is correct
                        lp?.let {
                            it.height = newHeight
                            layoutParams = it
                        }
                        invalidate()
                    }
                }

                layoutAnimator.start()
            } else {
                invalidate()
            }
        }
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
        android.util.Log.d("KeyboardView", "setSwipeTypingEnabled: $enabled")
        isSwipeTypingEnabled = enabled

        // Initialize swipe components if not already done
        if (enabled && swipeGestureDetector == null) {
            android.util.Log.d("KeyboardView", "Initializing swipe components")
            initializeSwipeComponents()
        }

        if (enabled) {
            // Update key bounds for swipe word predictor after layout
            // Use postDelayed to avoid interfering with current layout pass
            postDelayed({
                if (width > 0 && height > 0) {
                    updateSwipeKeyBounds()
                }
            }, 50) // Small delay to let layout settle
        } else {
            swipePathView?.cancelSwipe()
        }
        
        android.util.Log.d("KeyboardView", "Swipe typing enabled: $isSwipeTypingEnabled, detector: ${swipeGestureDetector != null}")
        // Don't invalidate or requestLayout - this can mess up suggestion bar
        // Just update internal state
    }

    /**
     * Enable or disable gestures
     */
    fun setGesturesEnabled(enabled: Boolean) {
        android.util.Log.d("KeyboardView", "setGesturesEnabled: $enabled")
        isGesturesEnabled = enabled

        // Initialize swipe components if not already done
        if (enabled && swipeGestureDetector == null) {
            android.util.Log.d("KeyboardView", "Initializing swipe components for gestures")
            initializeSwipeComponents()
        }
        
        android.util.Log.d("KeyboardView", "Gestures enabled: $isGesturesEnabled, detector: ${swipeGestureDetector != null}")
        // Don't invalidate or requestLayout - this can mess up suggestion bar
        // Just update internal state
    }

    /**
     * Set swipe typing sensitivity
     */
    fun setSwipeSensitivity(sensitivity: Float) {
        swipeSensitivity = sensitivity
        swipeGestureDetector?.setSensitivity(sensitivity)
    }

    fun setSwipeToDeleteEnabled(enabled: Boolean) {
        isSwipeDeleteEnabled = enabled
    }

    fun setSwipeCursorEnabled(enabled: Boolean) {
        isSwipeCursorEnabled = enabled
    }

    /**
     * Set swipe path view for visual feedback
     */
    fun setSwipePathView(pathView: SwipePathView) {
        swipePathView = pathView
    }

    fun setOnCommaEmojiLongPressListener(listener: (() -> Unit)?) {
        commaEmojiLongPressListener = listener
    }

    fun setOnEmojiLongPressListener(listener: ((String) -> Unit)?) {
        emojiLongPressListener = listener
    }

    /**
     * Set swipe word callback
     */
    fun setOnSwipeWordListener(listener: (String) -> Unit) {
        onSwipeWord = listener
    }

    /**
     * Set the swipe gesture listener for cursor and selection
     */
    fun setOnSwipeGestureListener(listener: (String, Int) -> Unit) {
        swipeGestureListener = listener
    }

    /**
     * Update key bounds for swipe typing
     * Ensures gesture tracking uses current key layout
     */
    private fun updateSwipeKeyBounds() {
        // Key bounds are already calculated in calculateKeyBounds()
        // Just verify they're valid for swipe detection
        if (keyBounds.isEmpty()) {
            android.util.Log.w("KeyboardView", "updateSwipeKeyBounds: Key bounds are empty!")
            return
        }
        
        android.util.Log.d("KeyboardView", "updateSwipeKeyBounds: ${keyBounds.size} keys ready for swipe detection")
        
        // Log key bounds for debugging (first few keys only)
        keyBounds.take(5).forEachIndexed { index, keyBound ->
            android.util.Log.v("KeyboardView", "Key $index '${keyBound.key.label}': bounds=${keyBound.bounds}")
        }
    }

    /**
     * Calculate bounds (position and size) for each key
     *
     * CRITICAL: This must be called whenever:
     * - Layout changes (setKeyboard)
     * - Size changes (onSizeChanged)
     * - Height percentage changes
     *
     * This ensures gesture tracking coordinates match visual layout
     *
     * IMPORTANT: Keys must align in a proper grid across all rows
     * Similar to Gboard and the reference design
     */
    private fun calculateKeyBounds() {
        keyBounds.clear()

        if (rows.isEmpty()) {
            android.util.Log.w("KeyboardView", "calculateKeyBounds: No rows to calculate bounds for")
            return
        }
        
        // Verify view is measured
        if (width <= 0 || height <= 0) {
            android.util.Log.w("KeyboardView", "calculateKeyBounds: View not measured yet (width=$width, height=$height)")
            return
        }

        // Calculate key dimensions
        // Remove horizontal insets to eliminate unwanted padding
        val horizontalInsetStart = 0f
        val horizontalInsetEnd = 0f
        val availableWidth = width - (paddingLeft + paddingRight)
        val availableHeight = height - (paddingTop + paddingBottom)
        
        android.util.Log.d("KeyboardView", "calculateKeyBounds: View size=$width x $height, available=$availableWidth x $availableHeight, padding=($paddingLeft, $paddingTop, $paddingRight, $paddingBottom)")

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

        android.util.Log.d("KeyboardView", "calculateKeyBounds: Calculated ${keyBounds.size} key bounds for ${rows.size} rows")
        
        // Update swipe key bounds if swipe typing is enabled
        if (isSwipeTypingEnabled) {
            updateSwipeKeyBounds()
        }
    }

    private fun drawCustomIcon(canvas: Canvas, key: Key, bounds: RectF): Boolean {
        // Get Material You icon drawable based on key type
        val iconDrawable = when (key.type) {
            com.kannada.kavi.core.layout.models.KeyType.ENTER -> {
                // Context-aware enter icon based on IME_ACTION
                when (currentEnterAction and android.view.inputmethod.EditorInfo.IME_MASK_ACTION) {
                    android.view.inputmethod.EditorInfo.IME_ACTION_SEND -> iconEnterSend
                    android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH -> iconEnterSearch
                    android.view.inputmethod.EditorInfo.IME_ACTION_DONE -> iconEnterDone
                    else -> iconEnter // Default: return/newline
                }
            }
            com.kannada.kavi.core.layout.models.KeyType.DELETE -> iconDelete
            com.kannada.kavi.core.layout.models.KeyType.SHIFT -> iconShift
            com.kannada.kavi.core.layout.models.KeyType.LANGUAGE -> iconLanguage
            com.kannada.kavi.core.layout.models.KeyType.EMOJI -> iconEmoji
            else -> null
        }
        
        if (iconDrawable != null) {
            // Get icon color based on key type
            val iconColor = when (key.type) {
                com.kannada.kavi.core.layout.models.KeyType.ENTER -> KeyboardDesignSystem.Colors.ACTION_KEY_ICON_DYNAMIC
                com.kannada.kavi.core.layout.models.KeyType.DELETE,
                com.kannada.kavi.core.layout.models.KeyType.SHIFT -> KeyboardDesignSystem.Colors.SPECIAL_KEY_ICON_DYNAMIC
                else -> KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC
            }
            
            // Calculate icon size - use 50% of smaller dimension for Material You icons
            val iconSize = min(bounds.width(), bounds.height()) * 0.5f
            
            // Calculate icon bounds (centered in key bounds)
            val iconLeft = bounds.centerX() - iconSize / 2f
            val iconTop = bounds.centerY() - iconSize / 2f
            val iconRight = bounds.centerX() + iconSize / 2f
            val iconBottom = bounds.centerY() + iconSize / 2f
            
            // Set drawable bounds
            iconDrawable.setBounds(
                iconLeft.toInt(),
                iconTop.toInt(),
                iconRight.toInt(),
                iconBottom.toInt()
            )
            
            // Apply color tinting to the drawable
            iconDrawable.setTint(iconColor)
            
            // Draw the icon
            iconDrawable.draw(canvas)
            return true
        }
        
        // Fallback to text-based icons if drawables not available
        val iconText = when (key.type) {
            com.kannada.kavi.core.layout.models.KeyType.ENTER -> {
                when (currentEnterAction and android.view.inputmethod.EditorInfo.IME_MASK_ACTION) {
                    android.view.inputmethod.EditorInfo.IME_ACTION_SEND -> "📤"
                    android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH -> "🔍"
                    android.view.inputmethod.EditorInfo.IME_ACTION_GO -> "➡️"
                    android.view.inputmethod.EditorInfo.IME_ACTION_NEXT -> "⏭️"
                    android.view.inputmethod.EditorInfo.IME_ACTION_DONE -> "✓"
                    else -> "⏎"
                }
            }
            com.kannada.kavi.core.layout.models.KeyType.DELETE -> "⌫"
            com.kannada.kavi.core.layout.models.KeyType.SHIFT -> "⇧"
            com.kannada.kavi.core.layout.models.KeyType.LANGUAGE -> "🌐"
            com.kannada.kavi.core.layout.models.KeyType.EMOJI -> {
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
            
            val iconSize = min(bounds.width(), bounds.height()) * 0.42f
            
            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = iconColor
                textAlign = Paint.Align.CENTER
                textSize = iconSize
                typeface = Typeface.DEFAULT_BOLD
                isSubpixelText = true
            }
            
            val textMetrics = iconPaint.fontMetrics
            val textY = bounds.centerY() - ((textMetrics.ascent + textMetrics.descent) / 2f)
            
            canvas.drawText(iconText, bounds.centerX(), textY, iconPaint)
            return true
        }
        
        return false
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
        
        // Always recalculate key bounds when size changes
        // This ensures gesture coordinates match visual layout
        calculateKeyBounds()
        android.util.Log.d("KeyboardView", "onSizeChanged: Key bounds recalculated (${keyBounds.size} keys), size=$w x $h")

        // Update swipe key bounds if swipe typing is enabled
        if (isSwipeTypingEnabled) {
            updateSwipeKeyBounds()

            // Set keyboard bounds for coordinate normalization (FlorisBoard approach)
            swipeGestureDetector?.setKeyboardBounds(
                0f, 0f, keyboardWidth, keyboardHeight
            )
        }
        
        // Notify SwipePathView of size change to ensure coordinate alignment
        swipePathView?.let { pathView ->
            // Validate and synchronize SwipePathView size
            pathView.validateSize(w, h)
            android.util.Log.d("KeyboardView", "onSizeChanged: Synchronized SwipePathView to $w x $h")
            // Enforce Z-order: SwipePathView must be on top
            pathView.bringToFront()
        }
    }

    /**
     * Enforce SwipePathView Z-order on layout changes
     * Called after view layout to ensure proper overlay ordering
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            // Enforce SwipePathView stays on top during layout changes
            swipePathView?.bringToFront()
        }
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

        // Skip drawing if not visible to improve performance
        if (!isShown) return

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

        // Draw subtle elevation shadow (only if not pressed or animating)
        // Skip shadow during animations to prevent background layer animation artifact
        if (key != pressedKey && key != animatingKey && keyPressScale == 1.0f) {
            val shadowOffsetY = 0.3f * density  // 0.3dp vertical offset for very subtle elevation
            val shadowBlur = 0.5f * density  // Minimal blur effect

            // Use pre-configured shadow paint (don't modify during draw for performance)
            // Shadow paint is already configured with correct opacity in applyColors()

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
            // OPTIMIZATION: Use pre-configured Paint objects - no modifications during onDraw
            if (isSpaceKey) {
                // Spacebar uses its own pre-configured paint
                val textY = drawBounds.centerY() - ((spaceBarPaint.descent() + spaceBarPaint.ascent()) / 2)
                canvas.drawText(displayLabel, drawBounds.centerX(), textY, spaceBarPaint)
            } else {
                // Regular keys - paint is already configured with correct color and size
                // No need to modify paint objects during draw
                val hintOffset = if (!key.hint.isNullOrBlank()) hintPaint.textSize * 0.2f else 0f
                val textY = drawBounds.centerY() + hintOffset - ((labelPaint.descent() + labelPaint.ascent()) / 2)
                canvas.drawText(displayLabel, drawBounds.centerX(), textY, labelPaint)
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
        // CRITICAL: Ensure coordinates are relative to this view
        // MotionEvent coordinates are already in view coordinates, but verify
        val eventX = event.x
        val eventY = event.y
        
        // Verify coordinates are within bounds (helps catch coordinate space issues)
        if (eventX < 0 || eventX > width || eventY < 0 || eventY > height) {
            android.util.Log.w("KeyboardView", "Touch event outside bounds: ($eventX, $eventY), view size=$width x $height")
        }
        
        // If swipe typing or gestures are enabled, try swipe detector first
        if ((isSwipeTypingEnabled || isGesturesEnabled) && swipeGestureDetector != null) {
            // Always pass events to swipe detector first
            // Event coordinates are already in KeyboardView's coordinate space
            val swipeHandled = swipeGestureDetector?.onTouchEvent(event) ?: false

            // For ACTION_MOVE during a swipe, always consume the event
            if (event.action == MotionEvent.ACTION_MOVE && swipeGestureDetector?.isSwiping() == true) {
                return true
            }

            // If the detector consumed the event (it's a swipe), we're done
            if (swipeHandled) {
                android.util.Log.d("KeyboardView", "Swipe detector handled event: ${event.action}")
                return true
            }

            // For ACTION_DOWN, let normal handling proceed even if swipe detector didn't consume
            // This allows swipe detector to track the start, but normal touch can also handle it
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Swipe detector tracks the start but returns false for taps
                // Continue with normal touch handling
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                // For MOVE, if swipe detector consumed it, we're done
                // The detector will consume if movement exceeds tap threshold
                if (swipeHandled) {
                    // Swipe detector is handling this - don't process as normal touch
                    return true
                }
                // If swipe detector didn't consume, check if we're tracking a swipe
                val isSwiping = swipeGestureDetector?.isSwiping() == true
                if (isSwiping) {
                    // We're tracking a swipe - let swipe detector handle it
                    android.util.Log.d("KeyboardView", "Swipe in progress, skipping normal touch")
                    return true
                }
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                // For UP/CANCEL, if we were swiping, let swipe detector handle it
                if (swipeGestureDetector?.isSwiping() == true || swipeHandled) {
                    android.util.Log.d("KeyboardView", "Swipe ending, skipping normal touch handling")
                    return true
                }
            }
        }

        // Handle touch events (for both normal touches and tap callbacks)
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                // User touched down
                handleTouchDown(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                // If we were swiping, let swipe detector handle the UP event first
                if ((isSwipeTypingEnabled || isGesturesEnabled) && swipeGestureDetector?.isSwiping() == true) {
                    // Swipe detector will handle this in onTouchEvent above
                    // Don't process as normal touch
                    return true
                }
                // User lifted finger (normal tap)
                handleTouchUp(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // If we're swiping, don't process as normal touch
                if ((isSwipeTypingEnabled || isGesturesEnabled) && swipeGestureDetector?.isSwiping() == true) {
                    // Swipe is in progress, let swipe detector handle it
                    return true
                }
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
        customLongPressHandled = false
        cancelLongPressDetection()
        val key = findKeyAt(x, y)

        if (key != null) {
            pressedKey = key.key
            pressedKeyBounds = key.bounds

            // Start key press scale animation
            startKeyPressAnimation(key.key)

            // Start ripple animation
            startRippleAnimation(x, y, key.bounds)

            // Invalidate only the pressed key area for better performance
            val bounds = key.bounds
            invalidate(bounds.left.toInt() - 10, bounds.top.toInt() - 10,
                      bounds.right.toInt() + 10, bounds.bottom.toInt() + 10)

            // Vibrate
            performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

            // Play sound (if enabled in theme)
            // TODO: implement sound effects based on theme.interaction.soundEnabled

            val shouldScheduleLongPress = !key.key.longPressKeys.isNullOrEmpty() ||
                (key.key.type == com.kannada.kavi.core.layout.models.KeyType.COMMA_EMOJI &&
                    commaEmojiLongPressListener != null)
            if (shouldScheduleLongPress) {
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
     * Start key press scale animation with improved timing
     *
     * @param key The key to animate
     */
    private fun startKeyPressAnimation(key: Key) {
        // Cancel any existing animation
        keyPressAnimator?.cancel()

        // Mark view as having transient state for smoother animations
        androidx.core.view.ViewCompat.setHasTransientState(this, true)

        animatingKey = key
        keyPressScale = 1.0f

        // Pre-calculate key bounds for better performance
        val keyBound = keyBounds.find { it.key == key }
        val animBounds = keyBound?.bounds?.let { bounds ->
            RectF(
                bounds.left - 20,
                bounds.top - 20,
                bounds.right + 20,
                bounds.bottom + 20
            )
        }

        // Create scale down animation with coordinated timing
        keyPressAnimator = ValueAnimator.ofFloat(1.0f, KeyboardDesignSystem.Animations.KEY_PRESS_SCALE).apply {
            duration = KeyboardDesignSystem.Animations.KEY_PRESS_DURATION
            interpolator = AccelerateDecelerateInterpolator() // Smooth motion

            addUpdateListener { animator ->
                keyPressScale = animator.animatedValue as Float
                isAnimationActive = true
                // Use full invalidate during animations to prevent partial invalidation flickering
                // when multiple animations overlap (ripple + key scale)
                invalidate()
            }

            // Chain release animation without delay for smooth transition
            doOnEnd {
                try {
                    // Start release animation immediately for smooth transition
                    animateKeyRelease()
                } finally {
                    // Ensure transient state is cleared even if animation is cancelled
                    androidx.core.view.ViewCompat.setHasTransientState(this@KeyboardView, false)
                    animatingKey = null
                    isAnimationActive = false
                }
            }

            start()
        }
    }

    /**
     * Animate key release with smooth transition
     */
    private fun animateKeyRelease() {
        // Pre-calculate bounds if not already cached
        val animBounds = animatingKey?.let { key ->
            keyBounds.find { it.key == key }?.bounds?.let { b ->
                RectF(b.left - 20, b.top - 20, b.right + 20, b.bottom + 20)
            }
        }

        keyPressAnimator = ValueAnimator.ofFloat(keyPressScale, 1.0f).apply {
            duration = KeyboardDesignSystem.Animations.KEY_RELEASE_DURATION
            interpolator = DecelerateInterpolator() // Smooth deceleration

            // Small start delay for better visual separation between press and release
            startDelay = 30L

            addUpdateListener { animator ->
                keyPressScale = animator.animatedValue as Float
                isAnimationActive = true
                // Use full invalidate during animations to prevent flickering
                invalidate()
            }

            doOnEnd {
                try {
                    animatingKey = null
                    keyPressScale = 1.0f
                } finally {
                    // Clear transient state
                    androidx.core.view.ViewCompat.setHasTransientState(this@KeyboardView, false)
                    isAnimationActive = false
                }
            }

            start()
        }
    }

    /**
     * Start Material You ripple animation with improved timing
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

        // Pre-calculate animation bounds for performance
        val maxRippleBounds = (maxRadius + 10).toInt()
        val rippleAnimBounds = RectF(
            rippleX - maxRippleBounds,
            rippleY - maxRippleBounds,
            rippleX + maxRippleBounds,
            rippleY + maxRippleBounds
        )

        // Create ripple animator with Material motion
        rippleAnimator = ValueAnimator.ofFloat(0f, maxRadius).apply {
            duration = KeyboardDesignSystem.Animations.RIPPLE_DURATION
            interpolator = AccelerateDecelerateInterpolator() // Smooth motion

            // Delay ripple start to avoid simultaneous updates with key press animation
            // This prevents partial invalidation conflicts and flickering
            startDelay = 20L

            addUpdateListener { animator ->
                rippleRadius = animator.animatedValue as Float
                isAnimationActive = true
                // Use full invalidate to prevent flickering when ripple overlaps with key scale
                invalidate()
            }

            // Auto-fade after reaching max size
            doOnEnd {
                // Fade out animation for smooth finish
                fadeOutRipple()
            }

            start()
        }
    }

    /**
     * Fade out ripple animation for smooth finish
     */
    private fun fadeOutRipple() {
        val currentRadius = rippleRadius
        if (currentRadius <= 0) return

        val fadeAnimator = ValueAnimator.ofFloat(currentRadius, 0f).apply {
            duration = 100L // Quick fade
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->
                rippleRadius = animator.animatedValue as Float
                isAnimationActive = true
                // Use full invalidate to ensure ripple fades smoothly
                invalidate()
            }

            doOnEnd {
                try {
                    rippleRadius = 0f
                } finally {
                    isAnimationActive = false
                }
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
        val skipKeyPress = customLongPressHandled
        customLongPressHandled = false

        // Only trigger if finger lifted on the same key it pressed and no popup stole the event
        if (!popupVisible && !skipKeyPress && key != null && key.key == pressedKey) {
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
        val hasPopupOptions = !targetKey.key.longPressKeys.isNullOrEmpty()
        val supportsCommaEmoji = targetKey.key.type == com.kannada.kavi.core.layout.models.KeyType.COMMA_EMOJI &&
                commaEmojiLongPressListener != null
        val supportsEmojiSkinTone = targetKey.key.type == com.kannada.kavi.core.layout.models.KeyType.EMOJI &&
                emojiLongPressListener != null
        if (!hasPopupOptions && !supportsCommaEmoji && !supportsEmojiSkinTone) return

        longPressTarget = targetKey
        val runnable = Runnable {
            when {
                targetKey.key.type == com.kannada.kavi.core.layout.models.KeyType.COMMA_EMOJI &&
                commaEmojiLongPressListener != null -> {
                    customLongPressHandled = true
                    commaEmojiLongPressListener?.invoke()
                    clearPressedKey()
                }
                targetKey.key.type == com.kannada.kavi.core.layout.models.KeyType.EMOJI &&
                emojiLongPressListener != null -> {
                    customLongPressHandled = true
                    emojiLongPressListener?.invoke(targetKey.key.output)
                    clearPressedKey()
                }
                else -> showLongPressPopup(targetKey)
            }
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
     * @param x X coordinate of touch (in view coordinates)
     * @param y Y coordinate of touch (in view coordinates)
     * @return The key at that position, or null
     */
    private fun findKeyAt(x: Float, y: Float): KeyBound? {
        if (keyBounds.isEmpty()) {
            android.util.Log.w("KeyboardView", "findKeyAt: keyBounds is empty!")
            return null
        }

        // For swipe detection, check exact bounds first
        // This provides the most accurate key detection during swipes
        val exactMatch = keyBounds.find { keyBound ->
            keyBound.bounds.contains(x, y)
        }

        if (exactMatch != null) {
            return exactMatch
        }

        // If no exact match and we're in the middle of a swipe,
        // find the closest key within a reasonable distance
        if (swipeGestureDetector?.isSwiping() == true) {
            var closestKey: KeyBound? = null
            var closestDistance = Float.MAX_VALUE

            keyBounds.forEach { keyBound ->
                // Calculate distance from point to key center
                val centerX = keyBound.bounds.centerX()
                val centerY = keyBound.bounds.centerY()
                val distance = kotlin.math.sqrt(
                    (x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)
                )

                // Use larger radius for better swipe detection
                // 60% of key width for more forgiving detection
                val maxDistance = keyBound.bounds.width() * 0.6f
                if (distance < maxDistance && distance < closestDistance) {
                    closestKey = keyBound
                    closestDistance = distance
                }
            }

            return closestKey
        } else {
            // For taps (not swipes), use expanded bounds for better touch targets
            val density = resources.displayMetrics.density
            val touchPadding = KeyboardDesignSystem.Dimensions.TOUCH_PADDING_VERTICAL * density

            return keyBounds.find { keyBound ->
                // Expand bounds by touch padding for better accessibility
                val expandedBounds = RectF(
                    keyBound.bounds.left - touchPadding,
                    keyBound.bounds.top - touchPadding,
                    keyBound.bounds.right + touchPadding,
                    keyBound.bounds.bottom + touchPadding
                )
                expandedBounds.contains(x, y)
            }
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
