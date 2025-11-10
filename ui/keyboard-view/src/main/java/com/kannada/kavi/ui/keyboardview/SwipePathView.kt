package com.kannada.kavi.ui.keyboardview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.graphics.ColorUtils
import com.kannada.kavi.features.themes.KeyboardDesignSystem

/**
 * SwipePathView - Visual Overlay for Swipe Typing
 *
 * Draws the swipe path as user drags their finger across keys.
 * Shows a smooth, animated trail behind the finger.
 *
 * DESIGN:
 * =======
 * - Gradient trail that fades out
 * - Smooth bezier curves between points
 * - Subtle glow effect
 * - Animates out on completion
 *
 * HOW IT WORKS:
 * =============
 * 1. User starts swiping
 * 2. KeyboardView calls updatePath() with each move
 * 3. We draw smooth curves through the points
 * 4. Trail fades from bright at finger to transparent at start
 * 5. On swipe end, trail animates out
 *
 * PERFORMANCE:
 * ============
 * - Hardware accelerated
 * - Efficient path drawing
 * - 60 FPS smooth animation
 */
class SwipePathView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TRAIL_WIDTH_DP = 12f  // Width of swipe trail
        private const val TRAIL_SEGMENTS = 20  // Number of gradient segments
        private const val FADE_OUT_DURATION_MS = 200L  // Fade out animation
    }

    // Current swipe path
    private val swipePath = mutableListOf<PointF>()
    private var isActive = false

    // Paint for trail
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Paint for glow effect
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Fade out animation
    private var fadeAnimator: ValueAnimator? = null
    private var fadeAlpha = 1f

    // Key highlight paint (shows which keys are being swiped over)
    private val keyHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val density: Float
        get() = resources.displayMetrics.density

    init {
        // Transparent background
        setBackgroundColor(0x00000000)

        // Apply colors from design system
        applyColors()

        // Enable hardware acceleration
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    /**
     * Apply theme colors
     */
    private fun applyColors() {
        val primaryColor = KeyboardDesignSystem.Colors.ACTION_KEY_BACKGROUND_DYNAMIC

        // Trail color (semi-transparent primary)
        trailPaint.apply {
            color = ColorUtils.setAlphaComponent(primaryColor, (255 * 0.8f).toInt())
            strokeWidth = TRAIL_WIDTH_DP * density
        }

        // Glow effect (very transparent, wider)
        glowPaint.apply {
            color = ColorUtils.setAlphaComponent(primaryColor, (255 * 0.3f).toInt())
            strokeWidth = TRAIL_WIDTH_DP * density * 1.5f
        }

        // Key highlight (very subtle)
        keyHighlightPaint.color = ColorUtils.setAlphaComponent(primaryColor, (255 * 0.15f).toInt())
    }

    /**
     * Start swipe tracking
     */
    fun startSwipe(x: Float, y: Float) {
        isActive = true
        fadeAlpha = 1f
        swipePath.clear()
        swipePath.add(PointF(x, y))
        fadeAnimator?.cancel()
        visibility = VISIBLE
        invalidate()
    }

    /**
     * Update swipe path
     */
    fun updatePath(x: Float, y: Float) {
        if (!isActive) return

        // Add point if it's far enough from last point (smoothing)
        val lastPoint = swipePath.lastOrNull()
        if (lastPoint == null || distance(lastPoint.x, lastPoint.y, x, y) > 5f * density) {
            swipePath.add(PointF(x, y))
            invalidate()
        }
    }

    /**
     * End swipe (animate out)
     */
    fun endSwipe() {
        if (!isActive) return

        isActive = false

        // Fade out animation
        fadeAnimator?.cancel()
        fadeAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = FADE_OUT_DURATION_MS
            addUpdateListener { animator ->
                fadeAlpha = animator.animatedValue as Float
                invalidate()
            }
            doOnEnd {
                swipePath.clear()
                visibility = GONE
            }
            start()
        }
    }

    /**
     * Cancel swipe (immediate clear)
     */
    fun cancelSwipe() {
        isActive = false
        fadeAnimator?.cancel()
        swipePath.clear()
        visibility = GONE
        invalidate()
    }

    /**
     * Draw the swipe path
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (swipePath.size < 2) return

        // Apply fade alpha
        val trailAlpha = (255 * 0.8f * fadeAlpha).toInt()
        val glowAlpha = (255 * 0.3f * fadeAlpha).toInt()
        trailPaint.alpha = trailAlpha
        glowPaint.alpha = glowAlpha

        // Create smooth path through points using bezier curves
        val path = Path()
        path.moveTo(swipePath[0].x, swipePath[0].y)

        for (i in 1 until swipePath.size) {
            val prevPoint = swipePath[i - 1]
            val currentPoint = swipePath[i]

            // Use quadratic bezier for smooth curves
            if (i < swipePath.size - 1) {
                val nextPoint = swipePath[i + 1]
                val controlX = currentPoint.x
                val controlY = currentPoint.y
                val endX = (currentPoint.x + nextPoint.x) / 2f
                val endY = (currentPoint.y + nextPoint.y) / 2f
                path.quadTo(controlX, controlY, endX, endY)
            } else {
                // Last segment - just line to endpoint
                path.lineTo(currentPoint.x, currentPoint.y)
            }
        }

        // Draw glow first (behind)
        canvas.drawPath(path, glowPaint)

        // Draw main trail on top
        canvas.drawPath(path, trailPaint)
    }

    /**
     * Calculate distance between two points
     */
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /**
     * Get current path points
     */
    fun getPath(): List<PointF> = swipePath.toList()

    /**
     * Is swipe active?
     */
    fun isSwipeActive(): Boolean = isActive
}