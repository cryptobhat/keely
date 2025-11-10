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
        // Thresholds
        private const val TAP_THRESHOLD_DP = 10f  // Movement less than this = tap
        private const val SWIPE_VELOCITY_THRESHOLD = 500f  // Minimum velocity for swipe
        private const val SWIPE_MIN_DISTANCE_DP = 40f  // Minimum distance for swipe

        // Swipe typing settings
        private const val SWIPE_SAMPLE_INTERVAL_MS = 16L  // 60 FPS sampling
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
    private val swipeKeySequence = mutableListOf<String>()

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
        swipeKeySequence.clear()

        // Add first point
        swipePath.add(PointF(x, y))

        return true
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
        val tapThreshold = TAP_THRESHOLD_DP * density
        if (totalDistance > tapThreshold && swipePath.size == 1) {
            // Started swiping
            listener?.onSwipeStart(startX, startY)
        }

        // Sample path at intervals (60 FPS)
        if (currentTime - lastSampleTime >= SWIPE_SAMPLE_INTERVAL_MS) {
            swipePath.add(PointF(x, y))
            lastSampleTime = currentTime

            // Notify listener of swipe progress
            if (swipePath.size > 1) {
                listener?.onSwipeMove(x, y, swipePath)
            }
        }

        lastX = x
        lastY = y

        return true
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

        // Add final point
        if (swipePath.isEmpty() || swipePath.last().let { it.x != x || it.y != y }) {
            swipePath.add(PointF(x, y))
        }

        // Classify gesture
        val tapThreshold = TAP_THRESHOLD_DP * density
        val swipeMinDistance = SWIPE_MIN_DISTANCE_DP * density

        val gesture = when {
            // Tap (minimal movement)
            distance < tapThreshold -> {
                listener?.onTap(x, y)
                null
            }
            // Swipe (significant movement)
            distance >= swipeMinDistance -> {
                classifySwipe(dx, dy, distance, duration)
            }
            else -> null
        }

        if (gesture != null) {
            listener?.onSwipeEnd(gesture)
        }

        // Reset state
        isTracking = false
        swipePath.clear()
        swipeKeySequence.clear()

        return true
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

        // Determine type based on path length and characteristics
        val type = when {
            // Swipe typing: long path with multiple points
            swipePath.size >= 5 && distance > SWIPE_MIN_DISTANCE_DP * density * 2 -> {
                SwipeType.SWIPE_TYPE
            }
            // Quick swipe: fast and short
            velocity > SWIPE_VELOCITY_THRESHOLD -> {
                when (direction) {
                    SwipeDirection.LEFT -> SwipeType.SWIPE_DELETE
                    SwipeDirection.UP -> SwipeType.SWIPE_SHIFT
                    else -> SwipeType.QUICK_SWIPE
                }
            }
            // Cursor movement: horizontal swipe
            abs(dx) > abs(dy) * 2 -> {
                SwipeType.SWIPE_CURSOR
            }
            else -> SwipeType.QUICK_SWIPE
        }

        return SwipeGesture(
            type = type,
            direction = direction,
            path = swipePath.toList(),
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
        // Avoid duplicate consecutive keys
        if (swipeKeySequence.isEmpty() || swipeKeySequence.last() != keyLabel) {
            swipeKeySequence.add(keyLabel)
        }
    }

    /**
     * Get current swipe key sequence
     */
    fun getKeySequence(): List<String> = swipeKeySequence.toList()

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
