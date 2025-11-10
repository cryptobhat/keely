package com.kannada.kavi.features.suggestion.swipe

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * SwipeWordPredictor - Predicts Words from Swipe Gestures
 *
 * Takes a swipe path and key positions to predict what word the user intended.
 * Uses geometric and statistical algorithms.
 *
 * ALGORITHM:
 * ==========
 * 1. Map swipe path to sequence of keys
 * 2. Find which keys the path passes through/near
 * 3. Generate candidate letter sequences
 * 4. Match against dictionary
 * 5. Score candidates based on:
 *    - Path similarity
 *    - Word frequency
 *    - Edit distance
 * 6. Return top predictions
 *
 * HOW IT WORKS:
 * =============
 * User swipes: Q → W → E → R → T → Y
 * Path analysis: Detect keys touched/nearby
 * Letter sequence: "qwerty"
 * Dictionary lookup: "qwerty" → "query", "quiet", "quest"
 * Scoring: Pick best match based on path shape
 * Result: "query" (if most likely)
 *
 * FEATURES:
 * =========
 * - Smart key detection (considers nearby keys)
 * - Fuzzy matching (handles imprecise swipes)
 * - Dictionary integration
 * - Multiple word suggestions
 * - Fast algorithm (< 50ms)
 *
 * PERFORMANCE:
 * ============
 * - Optimized key lookup
 * - Efficient path analysis
 * - Minimal allocations
 * - Target: < 50ms prediction time
 */
class SwipeWordPredictor {

    companion object {
        // Thresholds
        private const val KEY_PROXIMITY_THRESHOLD = 0.4f  // 40% of key size
        private const val MIN_PATH_POINTS = 3  // Minimum points for prediction
        private const val MAX_PREDICTIONS = 5  // Maximum word suggestions

        // Scoring weights
        private const val WEIGHT_PATH_SIMILARITY = 0.4f
        private const val WEIGHT_WORD_FREQUENCY = 0.3f
        private const val WEIGHT_LETTER_MATCH = 0.3f
    }

    // Key bounds mapping (set by KeyboardView)
    private val keyBoundsMap = mutableMapOf<String, RectF>()

    // Dictionary (simple for now, can be replaced with real dictionary)
    private val dictionary = mutableSetOf<String>()

    // Cache for performance
    private val predictionCache = mutableMapOf<String, List<SwipePrediction>>()

    /**
     * Set key bounds for detection
     */
    fun setKeyBounds(bounds: Map<String, RectF>) {
        keyBoundsMap.clear()
        keyBoundsMap.putAll(bounds)
    }

    /**
     * Set dictionary words
     */
    fun setDictionary(words: Set<String>) {
        dictionary.clear()
        dictionary.addAll(words.map { it.lowercase() })
        predictionCache.clear()
    }

    /**
     * Add word to dictionary
     */
    fun addWord(word: String) {
        dictionary.add(word.lowercase())
    }

    /**
     * Predict word from swipe path
     */
    fun predictWord(path: List<PointF>): List<SwipePrediction> {
        if (path.size < MIN_PATH_POINTS || keyBoundsMap.isEmpty()) {
            return emptyList()
        }

        // Step 1: Map path to key sequence
        val keySequence = mapPathToKeys(path)

        if (keySequence.isEmpty()) {
            return emptyList()
        }

        // Check cache
        val cacheKey = keySequence.joinToString("")
        predictionCache[cacheKey]?.let { return it }

        // Step 2: Generate letter pattern
        val letterPattern = keySequence.map { it.lowercase() }

        // Step 3: Find matching words in dictionary
        val candidates = findCandidateWords(letterPattern)

        // Step 4: Score and rank candidates
        val predictions = scoreCandidates(candidates, path, keySequence)
            .take(MAX_PREDICTIONS)

        // Cache result
        predictionCache[cacheKey] = predictions

        return predictions
    }

    /**
     * Map swipe path to sequence of keys
     */
    private fun mapPathToKeys(path: List<PointF>): List<String> {
        val keySequence = mutableListOf<String>()
        var lastKey: String? = null

        for (point in path) {
            // Find which key this point is over or near
            val key = findNearestKey(point)

            if (key != null && key != lastKey) {
                keySequence.add(key)
                lastKey = key
            }
        }

        return keySequence
    }

    /**
     * Find nearest key to a point
     */
    private fun findNearestKey(point: PointF): String? {
        var nearestKey: String? = null
        var minDistance = Float.MAX_VALUE

        for ((key, bounds) in keyBoundsMap) {
            // Skip special keys
            if (key.length > 1) continue

            // Calculate distance to key center
            val centerX = bounds.centerX()
            val centerY = bounds.centerY()
            val distance = distance(point.x, point.y, centerX, centerY)

            // Check if point is within key or nearby
            val keyRadius = (bounds.width() + bounds.height()) / 4f
            val threshold = keyRadius * (1f + KEY_PROXIMITY_THRESHOLD)

            if (distance < threshold && distance < minDistance) {
                minDistance = distance
                nearestKey = key
            }
        }

        return nearestKey
    }

    /**
     * Find candidate words matching letter pattern
     */
    private fun findCandidateWords(letterPattern: List<String>): List<String> {
        if (letterPattern.isEmpty()) return emptyList()

        val candidates = mutableListOf<String>()
        val firstLetter = letterPattern.first().lowercase()
        val patternLength = letterPattern.size

        // Filter dictionary for matches
        for (word in dictionary) {
            // Must start with same letter
            if (!word.startsWith(firstLetter)) continue

            // Length should be similar (allow some variation)
            if (abs(word.length - patternLength) > 3) continue

            // Check if word contains most of the pattern letters
            if (matchesPattern(word, letterPattern)) {
                candidates.add(word)
            }
        }

        return candidates
    }

    /**
     * Check if word matches letter pattern
     */
    private fun matchesPattern(word: String, pattern: List<String>): Boolean {
        if (pattern.isEmpty()) return false

        var wordIndex = 0
        var patternIndex = 0

        while (wordIndex < word.length && patternIndex < pattern.size) {
            if (word[wordIndex].toString() == pattern[patternIndex].lowercase()) {
                patternIndex++
            }
            wordIndex++
        }

        // Must match at least 60% of pattern
        val matchRatio = patternIndex.toFloat() / pattern.size
        return matchRatio >= 0.6f
    }

    /**
     * Score and rank candidate words
     */
    private fun scoreCandidates(
        candidates: List<String>,
        path: List<PointF>,
        keySequence: List<String>
    ): List<SwipePrediction> {
        val predictions = mutableListOf<SwipePrediction>()

        for (word in candidates) {
            val pathScore = calculatePathSimilarity(word, path)
            val frequencyScore = getWordFrequency(word)
            val letterScore = calculateLetterMatchScore(word, keySequence)

            val totalScore = (pathScore * WEIGHT_PATH_SIMILARITY) +
                           (frequencyScore * WEIGHT_WORD_FREQUENCY) +
                           (letterScore * WEIGHT_LETTER_MATCH)

            predictions.add(
                SwipePrediction(
                    word = word,
                    confidence = totalScore,
                    pathSimilarity = pathScore,
                    letterMatch = letterScore
                )
            )
        }

        // Sort by confidence (highest first)
        return predictions.sortedByDescending { it.confidence }
    }

    /**
     * Calculate path similarity score (0-1)
     */
    private fun calculatePathSimilarity(word: String, path: List<PointF>): Float {
        // Reconstruct expected path for this word
        val expectedPath = mutableListOf<PointF>()

        for (char in word) {
            val key = char.toString()
            keyBoundsMap[key]?.let { bounds ->
                expectedPath.add(PointF(bounds.centerX(), bounds.centerY()))
            }
        }

        if (expectedPath.isEmpty()) return 0f

        // Calculate similarity using dynamic time warping (simplified)
        var totalDistance = 0f
        var comparisons = 0

        for (i in path.indices) {
            val progress = i.toFloat() / path.size
            val expectedIndex = (progress * expectedPath.size).toInt().coerceIn(0, expectedPath.size - 1)
            val expected = expectedPath[expectedIndex]
            val actual = path[i]

            totalDistance += distance(actual.x, actual.y, expected.x, expected.y)
            comparisons++
        }

        if (comparisons == 0) return 0f

        // Normalize to 0-1 (lower distance = higher score)
        val avgDistance = totalDistance / comparisons
        val maxDistance = 500f  // Assume max reasonable distance
        return (1f - (avgDistance / maxDistance)).coerceIn(0f, 1f)
    }

    /**
     * Calculate letter match score (0-1)
     */
    private fun calculateLetterMatchScore(word: String, keySequence: List<String>): Float {
        if (keySequence.isEmpty()) return 0f

        var matches = 0
        var wordIndex = 0

        for (key in keySequence) {
            if (wordIndex < word.length && word[wordIndex].toString().equals(key, ignoreCase = true)) {
                matches++
                wordIndex++
            }
        }

        return matches.toFloat() / keySequence.size
    }

    /**
     * Get word frequency score (0-1)
     * TODO: Integrate with real frequency data
     */
    private fun getWordFrequency(word: String): Float {
        // Placeholder: prefer shorter common words
        return when {
            word.length <= 4 -> 0.9f
            word.length <= 6 -> 0.7f
            word.length <= 8 -> 0.5f
            else -> 0.3f
        }
    }

    /**
     * Calculate distance between two points
     */
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Clear prediction cache
     */
    fun clearCache() {
        predictionCache.clear()
    }
}

/**
 * SwipePrediction - A predicted word from swipe
 */
data class SwipePrediction(
    val word: String,
    val confidence: Float,  // 0-1 score
    val pathSimilarity: Float,
    val letterMatch: Float
)