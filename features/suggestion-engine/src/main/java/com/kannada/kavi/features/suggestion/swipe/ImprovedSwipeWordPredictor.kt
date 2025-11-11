package com.kannada.kavi.features.suggestion.swipe

import android.graphics.RectF
import kotlin.math.*

/**
 * ImprovedSwipeWordPredictor - Enhanced Word Prediction using FlorisBoard's Approach
 *
 * Implements statistical swipe typing based on FlorisBoard's dual-channel scoring:
 * - Shape Channel: Scale/translation invariant pattern matching
 * - Location Channel: Absolute position matching on keyboard
 * - Probabilistic key detection with Gaussian distributions
 * - Two-stage pruning for performance
 *
 * ALGORITHM (Based on FlorisBoard/SHARK2):
 * ========================================
 * 1. Normalize and resample gesture to 100 points
 * 2. Prune dictionary by start/end keys and path length
 * 3. Generate ideal paths for candidate words
 * 4. Calculate shape distance (normalized patterns)
 * 5. Calculate location distance (absolute positions)
 * 6. Convert distances to probabilities using Gaussian models
 * 7. Combine with word frequency for final score
 * 8. Return top N predictions
 *
 * IMPROVEMENTS OVER BASIC PREDICTOR:
 * ==================================
 * - Coordinate normalization for device independence
 * - Resampled paths for consistent comparison
 * - Dual-channel scoring for better accuracy
 * - Gaussian probability models
 * - Efficient two-stage pruning
 * - Better handling of imprecise swipes
 *
 * TARGET PERFORMANCE:
 * ==================
 * - Accuracy: 80%+ for top suggestion
 * - Latency: < 100ms prediction time
 * - Memory: Minimal allocations
 */
class ImprovedSwipeWordPredictor {

    companion object {
        // Pruning parameters (from FlorisBoard)
        private const val START_KEY_N_CLOSEST = 2  // Consider 2 closest words for start key
        private const val END_KEY_N_CLOSEST = 2    // Consider 2 closest words for end key
        private const val LENGTH_TOLERANCE = 0.14f // Path length tolerance (normalized)

        // Scoring parameters (from FlorisBoard research)
        private const val SHAPE_WEIGHT = 0.4f      // Weight for shape similarity
        private const val LOCATION_WEIGHT = 0.4f   // Weight for location similarity
        private const val FREQUENCY_WEIGHT = 0.2f  // Weight for word frequency

        // Prediction limits
        private const val MAX_CANDIDATES = 2000    // Max candidates after pruning
        private const val MAX_PREDICTIONS = 10     // Max final predictions
    }

    // Key bounds in normalized coordinates
    private val normalizedKeyBounds = mutableMapOf<Char, SwipeAlgorithms.NormalizedPoint>()
    private val keyBoundsMap = mutableMapOf<Char, RectF>()

    // Dictionary with frequency ratings
    private val dictionary = mutableMapOf<String, Float>()  // Word -> frequency (0-1)

    // Ideal gesture templates cache
    private val templateCache = mutableMapOf<String, List<SwipeAlgorithms.NormalizedPoint>>()

    // Performance metrics
    private var lastPredictionTime = 0L

    /**
     * Set keyboard layout for key detection
     */
    fun setKeyboardLayout(
        keyBounds: Map<String, RectF>,
        keyboardWidth: Float,
        keyboardHeight: Float
    ) {
        keyBoundsMap.clear()
        normalizedKeyBounds.clear()

        keyBounds.forEach { (keyText, bounds) ->
            if (keyText.length == 1) {
                val char = keyText[0].lowercaseChar()
                keyBoundsMap[char] = bounds

                // Store normalized center position
                val centerX = bounds.centerX() / keyboardWidth
                val centerY = bounds.centerY() / keyboardHeight
                normalizedKeyBounds[char] = SwipeAlgorithms.NormalizedPoint(centerX, centerY)
            }
        }
    }

    /**
     * Set dictionary with frequency ratings
     */
    fun setDictionary(words: Map<String, Float>) {
        dictionary.clear()
        dictionary.putAll(words.mapKeys { it.key.lowercase() })
        templateCache.clear()
    }

    /**
     * Add word to dictionary with frequency
     */
    fun addWord(word: String, frequency: Float = 0.5f) {
        dictionary[word.lowercase()] = frequency.coerceIn(0f, 1f)
        templateCache.remove(word.lowercase())
    }

    /**
     * Main prediction method using SwipeGesture
     */
    fun predictFromGesture(gesture: SwipeGesture): List<SwipePrediction> {
        val startTime = System.currentTimeMillis()

        // Use the resampled path for consistent analysis
        val resampledPath = gesture.resampledPath
        if (resampledPath.size < SwipeAlgorithms.RESAMPLE_POINTS / 2) {
            return emptyList()
        }

        // Stage 1: Pruning - Reduce candidates
        val prunedCandidates = pruneDictionary(resampledPath)
        if (prunedCandidates.isEmpty()) {
            return emptyList()
        }

        // Stage 2: Score candidates using dual-channel approach
        val predictions = scoreCandidates(resampledPath, prunedCandidates)
            .sortedByDescending { it.confidence }
            .take(MAX_PREDICTIONS)

        lastPredictionTime = System.currentTimeMillis() - startTime
        android.util.Log.d("ImprovedPredictor",
            "Predicted ${predictions.size} words from ${prunedCandidates.size} candidates in ${lastPredictionTime}ms")

        return predictions
    }

    /**
     * Stage 1: Prune dictionary using start/end keys and path length
     * Based on FlorisBoard's two-stage pruning approach
     */
    private fun pruneDictionary(path: List<SwipeAlgorithms.NormalizedPoint>): List<String> {
        if (path.isEmpty()) return emptyList()

        val startPoint = path.first()
        val endPoint = path.last()
        val pathLength = SwipeAlgorithms.calculatePathLength(path)

        // Find closest keys to start and end points
        val startKeys = findClosestKeys(startPoint, START_KEY_N_CLOSEST)
        val endKeys = findClosestKeys(endPoint, END_KEY_N_CLOSEST)

        val candidates = mutableListOf<String>()

        // Filter dictionary
        for ((word, _) in dictionary) {
            // Check start key constraint
            val firstChar = word.firstOrNull()?.lowercaseChar()
            if (firstChar != null && !startKeys.contains(firstChar)) continue

            // Check end key constraint
            val lastChar = word.lastOrNull()?.lowercaseChar()
            if (lastChar != null && !endKeys.contains(lastChar)) continue

            // Check path length constraint
            val idealPath = generateIdealPath(word)
            val idealLength = SwipeAlgorithms.calculatePathLength(idealPath)
            val lengthDiff = abs(idealLength - pathLength)

            if (lengthDiff <= LENGTH_TOLERANCE) {
                candidates.add(word)

                // Limit candidates for performance
                if (candidates.size >= MAX_CANDIDATES) break
            }
        }

        android.util.Log.d("ImprovedPredictor",
            "Pruned dictionary: ${dictionary.size} -> ${candidates.size} candidates")

        return candidates
    }

    /**
     * Find N closest keys to a point
     */
    private fun findClosestKeys(point: SwipeAlgorithms.NormalizedPoint, n: Int): Set<Char> {
        val distances = mutableListOf<Pair<Char, Float>>()

        normalizedKeyBounds.forEach { (char, keyCenter) ->
            val distance = point.distanceTo(keyCenter)
            distances.add(char to distance)
        }

        return distances
            .sortedBy { it.second }
            .take(n)
            .map { it.first }
            .toSet()
    }

    /**
     * Generate ideal swipe path for a word
     * Creates path through key centers
     */
    private fun generateIdealPath(word: String): List<SwipeAlgorithms.NormalizedPoint> {
        // Check cache first
        templateCache[word]?.let { return it }

        val path = mutableListOf<SwipeAlgorithms.NormalizedPoint>()

        for (char in word) {
            val keyCenter = normalizedKeyBounds[char.lowercaseChar()]
            if (keyCenter != null) {
                // Add intermediate points for smooth path
                if (path.isNotEmpty()) {
                    val lastPoint = path.last()
                    // Add 3 intermediate points between keys
                    for (i in 1..3) {
                        val t = i / 4f
                        val interpX = lastPoint.x + (keyCenter.x - lastPoint.x) * t
                        val interpY = lastPoint.y + (keyCenter.y - lastPoint.y) * t
                        path.add(SwipeAlgorithms.NormalizedPoint(interpX, interpY))
                    }
                }
                path.add(keyCenter)
            }
        }

        // Resample to standard number of points
        val resampledPath = if (path.size >= 2) {
            SwipeAlgorithms.resamplePath(path, SwipeAlgorithms.RESAMPLE_POINTS)
        } else {
            path
        }

        // Cache the result
        templateCache[word] = resampledPath

        return resampledPath
    }

    /**
     * Stage 2: Score candidates using dual-channel approach
     * Combines shape and location scores with word frequency
     */
    private fun scoreCandidates(
        userPath: List<SwipeAlgorithms.NormalizedPoint>,
        candidates: List<String>
    ): List<SwipePrediction> {
        val predictions = mutableListOf<SwipePrediction>()

        for (word in candidates) {
            val idealPath = generateIdealPath(word)

            // Ensure both paths have same number of points
            if (idealPath.size != userPath.size) continue

            // Calculate shape distance (scale/translation invariant)
            val shapeDistance = SwipeAlgorithms.shapeDistance(userPath, idealPath)

            // Calculate location distance (absolute position)
            val locationDistance = SwipeAlgorithms.locationDistance(userPath, idealPath)

            // Get word frequency
            val frequency = dictionary[word] ?: 0.5f

            // Calculate combined score using FlorisBoard's approach
            val score = SwipeAlgorithms.combinedScore(
                shapeDistance,
                locationDistance,
                frequency
            )

            // Alternative scoring with weights
            val weightedScore = calculateWeightedScore(
                shapeDistance,
                locationDistance,
                frequency
            )

            predictions.add(
                SwipePrediction(
                    word = word,
                    confidence = weightedScore,
                    pathSimilarity = exp(-shapeDistance * 2).toFloat().coerceIn(0f, 1f),
                    letterMatch = exp(-locationDistance * 5).toFloat().coerceIn(0f, 1f)
                )
            )
        }

        return predictions
    }

    /**
     * Calculate weighted score (alternative to Gaussian model)
     */
    private fun calculateWeightedScore(
        shapeDistance: Float,
        locationDistance: Float,
        frequency: Float
    ): Float {
        // Convert distances to similarity scores (0-1, higher is better)
        val shapeSimilarity = exp(-shapeDistance * 2).toFloat().coerceIn(0f, 1f)
        val locationSimilarity = exp(-locationDistance * 5).toFloat().coerceIn(0f, 1f)

        // Weighted combination
        return (shapeSimilarity * SHAPE_WEIGHT +
                locationSimilarity * LOCATION_WEIGHT +
                frequency * FREQUENCY_WEIGHT)
    }

    /**
     * Get last prediction time in milliseconds
     */
    fun getLastPredictionTime(): Long = lastPredictionTime

    /**
     * Clear all caches
     */
    fun clearCache() {
        templateCache.clear()
    }
}