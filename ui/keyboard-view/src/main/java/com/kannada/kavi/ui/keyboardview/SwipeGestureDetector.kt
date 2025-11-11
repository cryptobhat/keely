package com.kannada.kavi.ui.keyboardview

import android.graphics.PointF
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * SwipeGestureDetector - Detects Various Swipe Gestures
 *
 * Handles different types of gestures on the keyboard:
 * - Swipe typing (gliding across keys to form words)
 * - Swipe to delete (swipe left on delete key)
 * - Cursor movement (swipe on spacebar)
 * - Quick swipes (navigation gestures)
 *
 * HOW SWIPE TYPING WORKS:
 * =======================
 * 1. User touches keyboard and starts dragging
 * 2. We track the path of their finger
 * 3. We record which keys they pass over
 * 4. When they lift finger, we predict the word
 * 5. We insert the predicted word
 *
 * GESTURE TYPES:
 * ==============
 * - SWIPE_TYPE: Continuous path across keys (word prediction)
 * - SWIPE_DELETE: Quick left swipe on delete key (delete word)
 * - SWIPE_CURSOR: Left/right swipe on spacebar (move cursor)
 * - SWIPE_UP: Upward swipe (show symbols)
 * - TAP: Simple tap (regular key press)
 *
 * PERFORMANCE:
 * ============
 * - Samples path at 60 FPS
 * - Lightweight path tracking
 * - Fast gesture classification
 */
class SwipeGestureDetector {

    companion object {
        // Thresholds - Optimized for better swipe detection
        // Lower tap threshold for more responsive swipe detection
        private const val TAP_THRESHOLD_DP = 15f  // Movement less than this = tap
        private const val SWIPE_VELOCITY_THRESHOLD = 500f  // Lower velocity for easier swipes
        private const val SWIPE_MIN_DISTANCE_DP = 30f  // Lower minimum distance for easier swipe initiation

        // Swipe typing specific thresholds
        private const val SWIPE_TYPE_MIN_DISTANCE_DP = 60f  // Lower for easier swipe typing
        private const val SWIPE_TYPE_MIN_POINTS = 3  // Fewer points needed for word detection
        private const val SWIPE_TYPE_MIN_DURATION_MS = 150L  // Shorter duration for faster swipes

        // Swipe settings
        private const val SWIPE_SAMPLE_INTERVAL_MS = 8L  // 120 FPS sampling for smoother tracking
        private const val SWIPE_MIN_KEYS = 2  // Need at least 2 keys for word
    }

    // Gesture callbacks
    interface GestureListener {
        fun onTap(x: Float, y: Float)
        fun onLongPress(x: Float, y: Float)
        fun onSwipeStart(x: Float, y: Float)
        fun onSwipeMove(x: Float, y: Float, path: List<PointF>)
        fun onSwipeEnd(gesture: SwipeGesture)
        fun onSwipeCancel()
    }

    private var listener: GestureListener? = null
    private var density: Float = 1f
    private var sensitivity: Float = 1.0f  // Default sensitivity

    // Touch state
    private var isTracking = false
    private var startX = 0f
    private var startY = 0f
    private var startTime = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var lastSampleTime = 0L

    // Path tracking for swipe typing
    private val swipePath = mutableListOf<PointF>()
    private val normalizedPath = mutableListOf<SwipeAlgorithms.NormalizedPoint>()  // Normalized path for better accuracy
    private val swipeKeySequence = mutableListOf<String>()
    private var lastKeyAddedAt: PointF? = null  // Track position where last key was added

    // Keyboard bounds for normalization
    private var keyboardLeft = 0f
    private var keyboardTop = 0f
    private var keyboardWidth = 0f
    private var keyboardHeight = 0f

    // Long press detection
    private var longPressRunnable: Runnable? = null
    private var isLongPressed = false

    /**
     * Set gesture listener
     */
    fun setListener(listener: GestureListener) {
        this.listener = listener
    }

    /**
     * Set screen density
     */
    fun setDensity(density: Float) {
        this.density = density
    }

    /**
     * Set swipe sensitivity (0.5 = less sensitive, 1.0 = normal, 2.0 = more sensitive)
     */
    fun setSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity.coerceIn(0.5f, 2.0f)
    }

    /**
     * Set keyboard bounds for coordinate normalization
     * Essential for FlorisBoard-style gesture recognition
     */
    fun setKeyboardBounds(left: Float, top: Float, width: Float, height: Float) {
        keyboardLeft = left
        keyboardTop = top
        keyboardWidth = width
        keyboardHeight = height
    }
    
    /**
     * Get start X position (for checking swipe distance)
     */
    fun getStartX(): Float = startX
    
    /**
     * Get start Y position (for checking swipe distance)
     */
    fun getStartY(): Float = startY

    /**
     * Process touch event
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                return handleTouchDown(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                return handleTouchMove(event.x, event.y)
            }
            MotionEvent.ACTION_UP -> {
                return handleTouchUp(event.x, event.y)
            }
            MotionEvent.ACTION_CANCEL -> {
                handleTouchCancel()
                return true
            }
        }
        return false
    }

    /**
     * Handle touch down
     */
    private fun handleTouchDown(x: Float, y: Float): Boolean {
        isTracking = true
        isLongPressed = false
        startX = x
        startY = y
        lastX = x
        lastY = y
        startTime = System.currentTimeMillis()
        lastSampleTime = startTime

        // Clear previous path
        swipePath.clear()
        normalizedPath.clear()
        swipeKeySequence.clear()
        lastKeyAddedAt = null  // Reset position tracking

        // Add first point
        swipePath.add(PointF(x, y))

        // Add normalized point if keyboard bounds are set
        if (keyboardWidth > 0 && keyboardHeight > 0) {
            val normalizedPoint = SwipeAlgorithms.normalizePoint(
                x, y, keyboardLeft, keyboardTop, keyboardWidth, keyboardHeight
            )
            normalizedPath.add(normalizedPoint)
        }

        // Don't consume the event yet - wait to see if it's a tap or swipe
        // This allows normal tap handling to proceed if it's just a tap
        return false
    }

    /**
     * Handle touch move
     */
    private fun handleTouchMove(x: Float, y: Float): Boolean {
        if (!isTracking) return false

        val currentTime = System.currentTimeMillis()
        val dx = abs(x - startX)
        val dy = abs(y - startY)
        val totalDistance = sqrt(dx * dx + dy * dy)

        // Check if this is a swipe (moved beyond tap threshold)
        // Apply sensitivity: higher sensitivity = lower threshold (more sensitive)
        val tapThreshold = (TAP_THRESHOLD_DP * density) / sensitivity

        // Check if movement exceeds tap threshold (indicates swipe)
        if (totalDistance > tapThreshold) {
            // Started swiping - notify only once
            if (swipePath.size == 1) {
                listener?.onSwipeStart(startX, startY)
                android.util.Log.d("SwipeDetector", "Swipe started at ($startX, $startY)")
            }

            // Sample path at intervals (120 FPS)
            if (currentTime - lastSampleTime >= SWIPE_SAMPLE_INTERVAL_MS) {
                swipePath.add(PointF(x, y))

                // Add normalized point if keyboard bounds are set
                if (keyboardWidth > 0 && keyboardHeight > 0) {
                    val normalizedPoint = SwipeAlgorithms.normalizePoint(
                        x, y, keyboardLeft, keyboardTop, keyboardWidth, keyboardHeight
                    )
                    normalizedPath.add(normalizedPoint)
                }

                lastSampleTime = currentTime

                // Notify listener of swipe progress
                if (swipePath.size > 1) {
                    listener?.onSwipeMove(x, y, swipePath)
                }
            }

            lastX = x
            lastY = y

            // Consume event as this is a swipe (movement beyond tap threshold)
            // This prevents normal touch handling from interfering
            android.util.Log.v("SwipeDetector", "Swipe move: dist=${totalDistance}px, threshold=${tapThreshold}px, consuming event")
            return true
        }

        // Still within tap threshold, don't consume (let normal touch handle)
        return false
    }

    /**
     * Handle touch up
     */
    private fun handleTouchUp(x: Float, y: Float): Boolean {
        if (!isTracking) return false

        val duration = System.currentTimeMillis() - startTime
        val dx = x - startX
        val dy = y - startY
        val distance = sqrt(dx * dx + dy * dy)

        // Add final point if we have a path
        if (swipePath.isNotEmpty() && swipePath.last().let { it.x != x || it.y != y }) {
            swipePath.add(PointF(x, y))

            // Add normalized point if keyboard bounds are set
            if (keyboardWidth > 0 && keyboardHeight > 0) {
                val normalizedPoint = SwipeAlgorithms.normalizePoint(
                    x, y, keyboardLeft, keyboardTop, keyboardWidth, keyboardHeight
                )
                normalizedPath.add(normalizedPoint)
            }
        }

        // Classify gesture
        // Apply sensitivity to thresholds: higher sensitivity = lower thresholds (more sensitive)
        val tapThreshold = (TAP_THRESHOLD_DP * density) / sensitivity
        val swipeMinDistance = (SWIPE_MIN_DISTANCE_DP * density) / sensitivity

        var wasHandled = false

        when {
            // Tap (minimal movement)
            distance < tapThreshold -> {
                android.util.Log.d("SwipeDetector", "Tap detected: dist=${distance}px, threshold=${tapThreshold}px")
                listener?.onTap(x, y)
                wasHandled = false  // Let normal tap handling proceed
            }
            // Swipe (significant movement)
            distance >= swipeMinDistance -> {
                val gesture = classifySwipe(dx, dy, distance, duration)
                if (gesture != null) {
                    android.util.Log.d("SwipeDetector", "Swipe detected: type=${gesture.type}, dist=${distance}px")
                    listener?.onSwipeEnd(gesture)
                    wasHandled = true  // We handled this as a swipe
                } else {
                    android.util.Log.w("SwipeDetector", "Swipe movement detected but classification failed")
                    wasHandled = false
                }
            }
            // Movement between tap and swipe threshold - treat as tap
            else -> {
                android.util.Log.d("SwipeDetector", "Small movement, treating as tap: dist=${distance}px")
                listener?.onTap(x, y)
                wasHandled = false
            }
        }

        // Reset state
        isTracking = false
        swipePath.clear()
        normalizedPath.clear()
        swipeKeySequence.clear()

        return wasHandled
    }

    /**
     * Handle touch cancel
     */
    private fun handleTouchCancel() {
        if (isTracking) {
            listener?.onSwipeCancel()
        }
        isTracking = false
        swipePath.clear()
        normalizedPath.clear()
        swipeKeySequence.clear()
    }

    /**
     * Classify swipe gesture type
     */
    private fun classifySwipe(dx: Float, dy: Float, distance: Float, duration: Long): SwipeGesture {
        val velocity = distance / (duration / 1000f)  // pixels per second
        val angle = atan2(dy.toDouble(), dx.toDouble()) * (180.0 / Math.PI)

        // Determine direction
        val direction = when {
            angle > -45 && angle <= 45 -> SwipeDirection.RIGHT
            angle > 45 && angle <= 135 -> SwipeDirection.DOWN
            angle > -135 && angle <= -45 -> SwipeDirection.UP
            else -> SwipeDirection.LEFT
        }

        // Log for debugging
        android.util.Log.d("SwipeDetector", "Classify: path=${swipePath.size} pts, dist=${distance}px, dur=${duration}ms, vel=${velocity}px/s")

        // Apply sensitivity to swipe typing threshold
        val swipeTypeMinDistance = (SWIPE_TYPE_MIN_DISTANCE_DP * density) / sensitivity
        
        // Determine type based on path length and characteristics
        // Check cursor movement FIRST (before swipe typing) if it's clearly horizontal
        val isHorizontal = abs(dx) > abs(dy) * 1.5f  // More lenient horizontal check
        val isVertical = abs(dy) > abs(dx) * 1.5f
        
        val type = when {
            // PRIORITY 1: Swipe typing - check FIRST if we have enough points and keys
            // This prevents horizontal word swipes from being misclassified as cursor
            swipePath.size >= SWIPE_TYPE_MIN_POINTS &&
            swipeKeySequence.size >= 2 &&  // Need at least 2 different keys
            distance >= swipeTypeMinDistance &&
            duration >= SWIPE_TYPE_MIN_DURATION_MS -> {
                android.util.Log.d("SwipeDetector", "Detected SWIPE_TYPE: keys=${swipeKeySequence.size}, path=${swipePath.size}, dist=${distance}px")
                SwipeType.SWIPE_TYPE
            }
            // PRIORITY 2: Quick delete - fast leftward swipe
            direction == SwipeDirection.LEFT &&
            velocity > SWIPE_VELOCITY_THRESHOLD &&
            duration < 200 -> {
                android.util.Log.d("SwipeDetector", "Detected SWIPE_DELETE")
                SwipeType.SWIPE_DELETE
            }
            // PRIORITY 3: Quick shift - fast upward swipe
            direction == SwipeDirection.UP &&
            velocity > SWIPE_VELOCITY_THRESHOLD &&
            duration < 200 -> {
                android.util.Log.d("SwipeDetector", "Detected SWIPE_SHIFT")
                SwipeType.SWIPE_SHIFT
            }
            // PRIORITY 4: Cursor movement - horizontal swipe WITHOUT multiple keys
            // Only classify as cursor if we haven't collected multiple keys (not typing)
            isHorizontal &&
            swipeKeySequence.size < 2 &&  // Not typing if we haven't hit multiple keys
            distance >= SWIPE_MIN_DISTANCE_DP * density &&
            duration >= 100L -> {
                android.util.Log.d("SwipeDetector", "Detected SWIPE_CURSOR: horizontal, no keys")
                SwipeType.SWIPE_CURSOR
            }
            // Default to quick swipe for other gestures
            else -> {
                android.util.Log.d("SwipeDetector", "Detected QUICK_SWIPE")
                SwipeType.QUICK_SWIPE
            }
        }

        // Prepare normalized and resampled path for better accuracy
        val resampledPath = if (normalizedPath.size >= 2) {
            // First smooth the normalized path
            val smoothedPath = SwipeAlgorithms.smoothPath(normalizedPath)
            // Then resample to fixed number of points
            SwipeAlgorithms.resamplePath(smoothedPath, SwipeAlgorithms.RESAMPLE_POINTS)
        } else {
            emptyList()
        }

        return SwipeGesture(
            type = type,
            direction = direction,
            path = swipePath.toList(),
            normalizedPath = normalizedPath.toList(),
            resampledPath = resampledPath,
            distance = distance,
            velocity = velocity,
            duration = duration,
            startX = startX,
            startY = startY,
            endX = lastX,
            endY = lastY
        )
    }

    /**
     * Add key to swipe sequence
     * Called by KeyboardView when swipe passes over a key
     */
    fun addKeyToSequence(keyLabel: String) {
        swipeKeySequence.add(keyLabel)
    }

    /**
     * Get current swipe key sequence
     */
    fun getKeySequence(): List<String> = swipeKeySequence.toList()

    /**
     * Get position where last key was added
     */
    fun getLastKeyAddedAt(): PointF? = lastKeyAddedAt

    /**
     * Set position where key was just added
     */
    fun setLastKeyAddedAt(point: PointF) {
        lastKeyAddedAt = point
    }

    /**
     * Get current swipe path
     */
    fun getPath(): List<PointF> = swipePath.toList()

    /**
     * Is currently swiping?
     */
    fun isSwiping(): Boolean = isTracking && swipePath.size > 1
}

/**
 * SwipeGesture - Represents a detected swipe gesture
 */
data class SwipeGesture(
    val type: SwipeType,
    val direction: SwipeDirection,
    val path: List<PointF>,
    val normalizedPath: List<SwipeAlgorithms.NormalizedPoint> = emptyList(),  // Normalized 0-1 coordinates
    val resampledPath: List<SwipeAlgorithms.NormalizedPoint> = emptyList(),  // Resampled to fixed points
    val distance: Float,
    val velocity: Float,
    val duration: Long,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
)

/**
 * SwipeType - Type of swipe gesture
 */
enum class SwipeType {
    SWIPE_TYPE,      // Continuous swipe across keys for word input
    SWIPE_DELETE,    // Quick left swipe to delete word
    SWIPE_CURSOR,    // Horizontal swipe to move cursor
    SWIPE_SHIFT,     // Upward swipe to capitalize
    QUICK_SWIPE      // Generic quick swipe
}

/**
 * SwipeDirection - Direction of swipe
 */
enum class SwipeDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}
