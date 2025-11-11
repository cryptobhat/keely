package com.kannada.kavi.features.suggestion.swipe

import android.graphics.PointF
import kotlin.math.*

/**
 * Swipe Algorithms - Based on FlorisBoard's statistical approach
 *
 * This class implements advanced algorithms for swipe typing including:
 * - Coordinate normalization (0-1 range)
 * - Path resampling to uniform points
 * - Gaussian probability distributions
 * - Statistical scoring methods
 */
object SwipeAlgorithms {

    // Standard resampling count used by FlorisBoard and SHARK2
    const val RESAMPLE_POINTS = 100

    // Standard deviations for Gaussian models (from FlorisBoard)
    const val SHAPE_SIGMA = 22.9f
    const val LOCATION_SIGMA = 0.525f

    // Pruning thresholds
    const val LENGTH_THRESHOLD = 0.14f
    const val MIN_PATH_LENGTH = 0.05f

    /**
     * Normalize coordinates to 0-1 range relative to keyboard bounds
     * Essential for device-independent gesture recognition
     */
    data class NormalizedPoint(
        val x: Float,  // 0.0 to 1.0
        val y: Float,  // 0.0 to 1.0
        val timestamp: Long = 0L
    ) {
        fun toPointF() = PointF(x, y)

        fun distanceTo(other: NormalizedPoint): Float {
            val dx = x - other.x
            val dy = y - other.y
            return sqrt(dx * dx + dy * dy)
        }
    }

    /**
     * Normalize a point to 0-1 range based on keyboard dimensions
     */
    fun normalizePoint(
        x: Float,
        y: Float,
        keyboardLeft: Float,
        keyboardTop: Float,
        keyboardWidth: Float,
        keyboardHeight: Float
    ): NormalizedPoint {
        val normalizedX = (x - keyboardLeft) / keyboardWidth
        val normalizedY = (y - keyboardTop) / keyboardHeight
        return NormalizedPoint(
            x = normalizedX.coerceIn(0f, 1f),
            y = normalizedY.coerceIn(0f, 1f)
        )
    }

    /**
     * Denormalize a point from 0-1 range back to pixel coordinates
     */
    fun denormalizePoint(
        point: NormalizedPoint,
        keyboardLeft: Float,
        keyboardTop: Float,
        keyboardWidth: Float,
        keyboardHeight: Float
    ): PointF {
        return PointF(
            keyboardLeft + point.x * keyboardWidth,
            keyboardTop + point.y * keyboardHeight
        )
    }

    /**
     * Resample path to fixed number of equidistant points
     * This is crucial for comparing gestures of different speeds/lengths
     * Based on SHARK2 algorithm used by FlorisBoard
     */
    fun resamplePath(
        path: List<NormalizedPoint>,
        numPoints: Int = RESAMPLE_POINTS
    ): List<NormalizedPoint> {
        if (path.size < 2) return path

        // Calculate total path length
        var totalLength = 0f
        for (i in 1 until path.size) {
            totalLength += path[i].distanceTo(path[i - 1])
        }

        // If path is too short, return original
        if (totalLength < MIN_PATH_LENGTH) return path

        // Calculate interval between resampled points
        val interval = totalLength / (numPoints - 1)

        val resampled = mutableListOf<NormalizedPoint>()
        resampled.add(path.first())

        var accumulatedDist = 0f
        var currentIndex = 1
        var remainingDist = interval

        while (currentIndex < path.size && resampled.size < numPoints - 1) {
            val prevPoint = path[currentIndex - 1]
            val currPoint = path[currentIndex]
            val segmentDist = currPoint.distanceTo(prevPoint)

            if (accumulatedDist + segmentDist >= remainingDist) {
                // Interpolate point at exact distance
                val ratio = remainingDist / segmentDist
                val interpolatedX = prevPoint.x + ratio * (currPoint.x - prevPoint.x)
                val interpolatedY = prevPoint.y + ratio * (currPoint.y - prevPoint.y)

                resampled.add(NormalizedPoint(interpolatedX, interpolatedY))

                // Reset for next interval
                accumulatedDist = segmentDist - remainingDist
                remainingDist = interval
            } else {
                accumulatedDist += segmentDist
                remainingDist -= segmentDist
                currentIndex++
            }
        }

        // Ensure we have exactly numPoints
        resampled.add(path.last())

        // If we have fewer points than requested, interpolate to fill
        while (resampled.size < numPoints) {
            val idx = resampled.size - 1
            val prevPoint = resampled[idx - 1]
            val lastPoint = resampled[idx]
            val midPoint = NormalizedPoint(
                (prevPoint.x + lastPoint.x) / 2,
                (prevPoint.y + lastPoint.y) / 2
            )
            resampled.add(idx, midPoint)
        }

        return resampled.take(numPoints)
    }

    /**
     * Apply moving average smoothing to reduce noise
     * Window size of 3 is common for gesture smoothing
     */
    fun smoothPath(
        path: List<NormalizedPoint>,
        windowSize: Int = 3
    ): List<NormalizedPoint> {
        if (path.size <= windowSize) return path

        val smoothed = mutableListOf<NormalizedPoint>()
        smoothed.add(path.first())

        for (i in 1 until path.size - 1) {
            val startIdx = maxOf(0, i - windowSize / 2)
            val endIdx = minOf(path.size - 1, i + windowSize / 2)

            var sumX = 0f
            var sumY = 0f
            var count = 0

            for (j in startIdx..endIdx) {
                sumX += path[j].x
                sumY += path[j].y
                count++
            }

            smoothed.add(NormalizedPoint(sumX / count, sumY / count))
        }

        smoothed.add(path.last())
        return smoothed
    }

    /**
     * Calculate Gaussian probability for a distance
     * Used for probabilistic key detection
     */
    fun gaussianProbability(distance: Float, sigma: Float): Float {
        // P(x) = exp(-0.5 * (distance/sigma)^2)
        val normalized = distance / sigma
        return exp(-0.5f * normalized * normalized).toFloat()
    }

    /**
     * Calculate probability that a point hits a key
     * Based on Gaussian distribution centered at key center
     */
    fun keyHitProbability(
        point: NormalizedPoint,
        keyCenter: NormalizedPoint,
        keyWidth: Float,  // in normalized coordinates
        keyHeight: Float  // in normalized coordinates
    ): Float {
        val dx = abs(point.x - keyCenter.x)
        val dy = abs(point.y - keyCenter.y)

        // Use different sigma for x and y based on key dimensions
        val sigmaX = keyWidth * 0.5f
        val sigmaY = keyHeight * 0.5f

        // Combined probability (product of x and y probabilities)
        val probX = gaussianProbability(dx, sigmaX)
        val probY = gaussianProbability(dy, sigmaY)

        return probX * probY
    }

    /**
     * Calculate shape distance between two paths (scale/translation invariant)
     * Both paths should be resampled to same number of points
     */
    fun shapeDistance(path1: List<NormalizedPoint>, path2: List<NormalizedPoint>): Float {
        if (path1.size != path2.size) {
            throw IllegalArgumentException("Paths must have same number of points")
        }

        // Normalize both paths to unit bounding box for shape comparison
        val normalized1 = normalizeToBoundingBox(path1)
        val normalized2 = normalizeToBoundingBox(path2)

        // Calculate mean Euclidean distance
        var totalDistance = 0f
        for (i in normalized1.indices) {
            totalDistance += normalized1[i].distanceTo(normalized2[i])
        }

        return totalDistance / normalized1.size
    }

    /**
     * Calculate location distance between two paths (absolute position)
     * Preserves spatial information unlike shape distance
     */
    fun locationDistance(path1: List<NormalizedPoint>, path2: List<NormalizedPoint>): Float {
        if (path1.size != path2.size) {
            throw IllegalArgumentException("Paths must have same number of points")
        }

        // Calculate mean Euclidean distance without normalization
        var totalDistance = 0f
        for (i in path1.indices) {
            totalDistance += path1[i].distanceTo(path2[i])
        }

        return totalDistance / path1.size
    }

    /**
     * Normalize path to unit bounding box (0-1 range within its bounds)
     * Used for scale-invariant shape comparison
     */
    private fun normalizeToBoundingBox(path: List<NormalizedPoint>): List<NormalizedPoint> {
        if (path.isEmpty()) return path

        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        for (point in path) {
            minX = minOf(minX, point.x)
            maxX = maxOf(maxX, point.x)
            minY = minOf(minY, point.y)
            maxY = maxOf(maxY, point.y)
        }

        val width = maxX - minX
        val height = maxY - minY

        // Avoid division by zero
        if (width == 0f || height == 0f) return path

        // Scale to unit square preserving aspect ratio
        val scale = maxOf(width, height)

        return path.map { point ->
            NormalizedPoint(
                (point.x - minX) / scale,
                (point.y - minY) / scale
            )
        }
    }

    /**
     * Calculate curvature at each point for curve analysis
     * Helps detect loops and sharp turns in gestures
     */
    fun calculateCurvature(path: List<NormalizedPoint>): List<Float> {
        if (path.size < 3) return List(path.size) { 0f }

        val curvatures = mutableListOf<Float>()
        curvatures.add(0f) // First point has no curvature

        for (i in 1 until path.size - 1) {
            val p0 = path[i - 1]
            val p1 = path[i]
            val p2 = path[i + 1]

            // Calculate angle between vectors
            val v1x = p1.x - p0.x
            val v1y = p1.y - p0.y
            val v2x = p2.x - p1.x
            val v2y = p2.y - p1.y

            val angle = atan2(v2y, v2x) - atan2(v1y, v1x)

            // Normalize angle to [-π, π]
            val normalizedAngle = when {
                angle > PI -> angle - 2 * PI
                angle < -PI -> angle + 2 * PI
                else -> angle
            }.toFloat()

            curvatures.add(abs(normalizedAngle))
        }

        curvatures.add(0f) // Last point has no curvature
        return curvatures
    }

    /**
     * Detect significant direction changes in path
     * Used to identify key turning points in gestures
     */
    fun detectDirectionChanges(
        path: List<NormalizedPoint>,
        threshold: Float = PI.toFloat() / 4  // 45 degrees
    ): List<Int> {
        val curvatures = calculateCurvature(path)
        val directionChanges = mutableListOf<Int>()

        for (i in 1 until curvatures.size - 1) {
            if (curvatures[i] > threshold) {
                directionChanges.add(i)
            }
        }

        return directionChanges
    }

    /**
     * Calculate path length in normalized coordinates
     */
    fun calculatePathLength(path: List<NormalizedPoint>): Float {
        if (path.size < 2) return 0f

        var length = 0f
        for (i in 1 until path.size) {
            length += path[i].distanceTo(path[i - 1])
        }

        return length
    }

    /**
     * Combine shape and location scores using Gaussian probabilities
     * Based on FlorisBoard's dual-channel scoring
     */
    fun combinedScore(
        shapeDistance: Float,
        locationDistance: Float,
        wordFrequency: Float = 1f
    ): Float {
        val shapeProbability = gaussianProbability(shapeDistance, SHAPE_SIGMA)
        val locationProbability = gaussianProbability(locationDistance, LOCATION_SIGMA)

        // Combine probabilities with word frequency
        return shapeProbability * locationProbability * wordFrequency
    }
}