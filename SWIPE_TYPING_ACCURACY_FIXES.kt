/**
 * SWIPE TYPING ACCURACY IMPROVEMENTS
 * 
 * This file contains code improvements to make swipe typing more accurate.
 * Copy these functions into the appropriate files.
 */

// ============================================================================
// 1. IMPROVED KEY DETECTION WITH ADAPTIVE RADIUS
// ============================================================================
// Add to KeyboardView.kt - replace findKeyAt() method

/**
 * Find key at position with adaptive detection radius based on swipe velocity
 */
private fun findKeyAtImproved(x: Float, y: Float): KeyBound? {
    if (keyBounds.isEmpty()) return null

    // Exact match first (fastest)
    val exactMatch = keyBounds.find { it.bounds.contains(x, y) }
    if (exactMatch != null) return exactMatch

    // For swipes, use adaptive radius based on velocity
    if (swipeGestureDetector?.isSwiping() == true) {
        val swipeVelocity = calculateSwipeVelocity()
        val baseThreshold = 0.4f // 40% base
        
        var closestKey: KeyBound? = null
        var closestDistance = Float.MAX_VALUE
        var bestProbability = 0f

        keyBounds.forEach { keyBound ->
            if (keyBound.key.type != com.kannada.kavi.core.layout.models.KeyType.CHARACTER) return@forEach
            
            val centerX = keyBound.bounds.centerX()
            val centerY = keyBound.bounds.centerY()
            val distance = kotlin.math.sqrt(
                (x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)
            )

            // Adaptive radius: faster swipes = larger detection area
            val adaptiveRadius = when {
                swipeVelocity > 2000f -> keyBound.bounds.width() * 0.7f  // Very fast: 70%
                swipeVelocity > 1000f -> keyBound.bounds.width() * 0.55f // Fast: 55%
                swipeVelocity > 500f -> keyBound.bounds.width() * baseThreshold // Normal: 40%
                else -> keyBound.bounds.width() * 0.3f // Slow: 30% (more precise)
            }

            // Use probabilistic detection (Gaussian)
            val normalizedDistance = distance / adaptiveRadius
            val probability = kotlin.math.exp(-0.5f * normalizedDistance * normalizedDistance)

            if (probability > 0.3f && probability > bestProbability) {
                closestKey = keyBound
                closestDistance = distance
                bestProbability = probability
            }
        }

        return closestKey
    }

    // For taps, use expanded bounds
    val density = resources.displayMetrics.density
    val touchPadding = KeyboardDesignSystem.Dimensions.TOUCH_PADDING_VERTICAL * density

    return keyBounds.find { keyBound ->
        val expandedBounds = RectF(
            keyBound.bounds.left - touchPadding,
            keyBound.bounds.top - touchPadding,
            keyBound.bounds.right + touchPadding,
            keyBound.bounds.bottom + touchPadding
        )
        expandedBounds.contains(x, y)
    }
}

/**
 * Calculate current swipe velocity in pixels per second
 */
private fun calculateSwipeVelocity(): Float {
    val path = swipeGestureDetector?.getPath() ?: return 0f
    if (path.size < 2) return 0f

    val recentPoints = path.takeLast(5) // Last 5 points
    if (recentPoints.size < 2) return 0f

    var totalDistance = 0f
    var totalTime = 0L

    for (i in 1 until recentPoints.size) {
        val dx = recentPoints[i].x - recentPoints[i-1].x
        val dy = recentPoints[i].y - recentPoints[i-1].y
        totalDistance += kotlin.math.sqrt(dx * dx + dy * dy)
        // Assume ~8ms per point (120 FPS sampling)
        totalTime += 8L
    }

    if (totalTime == 0L) return 0f
    return (totalDistance / totalTime) * 1000f // Convert to px/s
}

// ============================================================================
// 2. KEY CROSSING DETECTION (Track Entry/Exit)
// ============================================================================
// Add to KeyboardView.kt

data class KeyCrossing(
    val key: String,
    val entryPoint: PointF,
    val exitPoint: PointF?,
    val entryTime: Long,
    val duration: Long = 0L,
    val confidence: Float = 1.0f
)

/**
 * Detect all key crossings in swipe path
 * This is more accurate than point-based detection
 */
private fun detectKeyCrossings(path: List<PointF>): List<KeyCrossing> {
    val crossings = mutableListOf<KeyCrossing>()
    val activeKeys = mutableMapOf<String, Pair<PointF, Long>>()
    val keyProbabilities = mutableMapOf<String, MutableList<Float>>()
    
    for ((index, point) in path.withIndex()) {
        val currentTime = System.currentTimeMillis()
        
        // Get all keys with probability > threshold
        val candidateKeys = keyBounds
            .filter { it.key.type == com.kannada.kavi.core.layout.models.KeyType.CHARACTER }
            .mapNotNull { keyBound ->
                val prob = getKeyProbability(point, keyBound)
                if (prob > 0.3f) keyBound.key.output to prob else null
            }
            .sortedByDescending { it.second }
        
        // Track key entries
        for ((keyText, probability) in candidateKeys) {
            if (!activeKeys.containsKey(keyText)) {
                // New key entry
                activeKeys[keyText] = point to currentTime
                keyProbabilities[keyText] = mutableListOf(probability)
            } else {
                // Update probability list
                keyProbabilities.getOrPut(keyText) { mutableListOf() }.add(probability)
            }
        }
        
        // Check for key exits
        val keysToExit = activeKeys.keys.filter { keyText ->
            val keyBound = keyBounds.find { it.key.output == keyText }
            keyBound?.let { 
                val prob = getKeyProbability(point, keyBound)
                prob < 0.2f // Exit if probability drops below 20%
            } ?: true
        }
        
        for (keyText in keysToExit) {
            val (entryPoint, entryTime) = activeKeys.remove(keyText)!!
            val probabilities = keyProbabilities.remove(keyText) ?: emptyList()
            val avgProbability = probabilities.average().toFloat()
            val duration = currentTime - entryTime
            
            crossings.add(KeyCrossing(
                key = keyText,
                entryPoint = entryPoint,
                exitPoint = point,
                entryTime = entryTime,
                duration = duration,
                confidence = avgProbability
            ))
        }
    }
    
    // Add final active keys
    for ((keyText, (entryPoint, entryTime)) in activeKeys) {
        val probabilities = keyProbabilities[keyText] ?: emptyList()
        val avgProbability = probabilities.average().toFloat()
        
        crossings.add(KeyCrossing(
            key = keyText,
            entryPoint = entryPoint,
            exitPoint = path.lastOrNull(),
            entryTime = entryTime,
            duration = System.currentTimeMillis() - entryTime,
            confidence = avgProbability
        ))
    }
    
    return crossings
}

/**
 * Calculate probability that point hits a key (Gaussian distribution)
 */
private fun getKeyProbability(point: PointF, keyBound: KeyBound): Float {
    val centerX = keyBound.bounds.centerX()
    val centerY = keyBound.bounds.centerY()
    val keyWidth = keyBound.bounds.width()
    val keyHeight = keyBound.bounds.height()
    
    // Normalized distance
    val dx = (point.x - centerX) / keyWidth
    val dy = (point.y - centerY) / keyHeight
    val normalizedDistance = kotlin.math.sqrt(dx * dx + dy * dy)
    
    // Gaussian probability (sigma = 0.5)
    val sigma = 0.5f
    return kotlin.math.exp(-0.5f * (normalizedDistance / sigma) * (normalizedDistance / sigma))
        .coerceIn(0f, 1f)
}

/**
 * Build key sequence from crossings (more accurate than point-based)
 */
private fun buildKeySequenceFromCrossings(crossings: List<KeyCrossing>): String {
    if (crossings.isEmpty()) return ""
    
    val sequence = StringBuilder()
    var lastKey: String? = null
    var lastKeyTime: Long = 0L
    
    // Filter by minimum duration and confidence
    val minDurationMs = 15L // Minimum 15ms on key
    val minConfidence = 0.4f // Minimum 40% confidence
    
    for (crossing in crossings.sortedBy { it.entryTime }) {
        val shouldAdd = when {
            // Different key - always add
            crossing.key != lastKey -> true
            
            // Same key repeated - check time gap
            lastKey != null -> {
                val timeGap = crossing.entryTime - lastKeyTime
                timeGap > 80L // 80ms gap for repeated letters
            }
            
            else -> false
        }
        
        if (shouldAdd && 
            crossing.duration >= minDurationMs && 
            crossing.confidence >= minConfidence) {
            sequence.append(crossing.key)
            lastKey = crossing.key
            lastKeyTime = crossing.entryTime
        }
    }
    
    return sequence.toString()
}

// ============================================================================
// 3. IMPROVED PATH SAMPLING
// ============================================================================
// Add to SwipeGestureDetector.kt

/**
 * Adaptive path sampling based on curvature and velocity
 */
private fun adaptivePathSampling(
    path: List<PointF>,
    minSampleDistance: Float
): List<PointF> {
    if (path.size < 2) return path
    
    val sampled = mutableListOf<PointF>()
    sampled.add(path.first())
    
    var lastSampledIndex = 0
    var accumulatedDistance = 0f
    
    for (i in 1 until path.size) {
        val segmentDistance = distance(
            path[i].x, path[i].y,
            path[lastSampledIndex].x, path[lastSampledIndex].y
        )
        accumulatedDistance += segmentDistance
        
        // Sample if distance threshold reached OR significant direction change
        val shouldSample = accumulatedDistance >= minSampleDistance ||
            isSignificantDirectionChange(path, lastSampledIndex, i)
        
        if (shouldSample) {
            sampled.add(path[i])
            lastSampledIndex = i
            accumulatedDistance = 0f
        }
    }
    
    // Always include last point
    if (sampled.last() != path.last()) {
        sampled.add(path.last())
    }
    
    return sampled
}

private fun isSignificantDirectionChange(
    path: List<PointF>,
    startIdx: Int,
    endIdx: Int
): Boolean {
    if (endIdx - startIdx < 2) return false
    
    val p0 = path[startIdx]
    val p1 = path[(startIdx + endIdx) / 2]
    val p2 = path[endIdx]
    
    val v1x = p1.x - p0.x
    val v1y = p1.y - p0.y
    val v2x = p2.x - p1.x
    val v2y = p2.y - p1.y
    
    val dot = v1x * v2x + v1y * v2y
    val mag1 = kotlin.math.sqrt(v1x * v1x + v1y * v1y)
    val mag2 = kotlin.math.sqrt(v2x * v2x + v2y * v2y)
    
    if (mag1 == 0f || mag2 == 0f) return false
    
    val cosAngle = dot / (mag1 * mag2)
    val angle = kotlin.math.acos(cosAngle.coerceIn(-1f, 1f))
    
    // Significant change if angle > 30 degrees
    return angle > (kotlin.math.PI / 6).toFloat()
}

// ============================================================================
// 4. OPTIMIZED SCORING WEIGHTS
// ============================================================================
// Update ImprovedSwipeWordPredictor.kt

companion object {
    // Optimized weights (tuned for better accuracy)
    private const val SHAPE_WEIGHT = 0.35f      // Reduced from 0.4
    private const val LOCATION_WEIGHT = 0.45f   // Increased from 0.4 (more important)
    private const val FREQUENCY_WEIGHT = 0.15f  // Reduced from 0.2
    private const val VELOCITY_WEIGHT = 0.05f   // NEW: Velocity consistency
    
    // Better pruning (more candidates)
    private const val START_KEY_N_CLOSEST = 3   // Increased from 2
    private const val END_KEY_N_CLOSEST = 3      // Increased from 2
    private const val LENGTH_TOLERANCE = 0.18f  // Increased from 0.14 (more forgiving)
}

// ============================================================================
// 5. USAGE IN KEYBOARD VIEW
// ============================================================================
// Update the swipe word handler in KaviInputMethodService.kt

// Replace the current swipe word detection with:
private fun handleSwipeWordImproved(gesture: SwipeGesture) {
    val path = gesture.path
    
    // Method 1: Use key crossings (most accurate)
    val crossings = detectKeyCrossings(path)
    val keySequence = buildKeySequenceFromCrossings(crossings)
    
    // Method 2: Fallback to point-based if crossings fail
    val fallbackSequence = if (keySequence.isEmpty()) {
        buildKeySequenceFromPoints(path)
    } else {
        keySequence
    }
    
    if (fallbackSequence.length >= 2) {
        // Get suggestions with key sequence context
        serviceScope.launch(Dispatchers.Main) {
            val currentLayout = layoutManager.activeLayout.value
            val language = if (currentLayout?.id == Constants.Layouts.LAYOUT_QWERTY) "en" else "kn"
            
            val suggestions = suggestionEngine.getSuggestions(
                currentWord = fallbackSequence,
                language = language
            )
            
            // Boost suggestions that match key sequence better
            val scoredSuggestions = suggestions.map { suggestion ->
                val keyMatchScore = calculateKeySequenceMatch(suggestion.word, fallbackSequence)
                val boostedConfidence = suggestion.confidence * (1.0f + keyMatchScore * 0.2f)
                suggestion.copy(confidence = boostedConfidence.coerceIn(0f, 1f))
            }.sortedByDescending { it.confidence }
            
            val bestWord = scoredSuggestions.firstOrNull()?.word ?: fallbackSequence
            commitSwipeWord(bestWord)
        }
    }
}

private fun calculateKeySequenceMatch(word: String, keySequence: String): Float {
    var matches = 0
    var wordIndex = 0
    
    for (key in keySequence) {
        if (wordIndex < word.length && 
            word[wordIndex].lowercaseChar() == key.lowercaseChar()) {
            matches++
            wordIndex++
        }
    }
    
    return if (keySequence.isEmpty()) 0f else matches.toFloat() / keySequence.length
}

// ============================================================================
// IMPLEMENTATION CHECKLIST
// ============================================================================
/*
1. Copy findKeyAtImproved() to KeyboardView.kt
2. Copy detectKeyCrossings() and related functions to KeyboardView.kt
3. Update SwipeGestureDetector to use adaptivePathSampling()
4. Update ImprovedSwipeWordPredictor scoring weights
5. Update swipe word handler to use key crossings
6. Test with various swipe speeds and patterns
7. Tune thresholds based on user feedback
*/


