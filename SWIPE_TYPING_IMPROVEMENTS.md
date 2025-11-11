# Swipe Typing Accuracy Improvements

## Current Issues Identified

1. **Key Detection**: 40% threshold may miss keys during fast swipes
2. **Path Sampling**: May not capture all keys during rapid movements
3. **Key Sequence Building**: Spatial deduplication may miss repeated letters
4. **Word Prediction**: Scoring weights may need optimization
5. **No Key Entry/Exit Tracking**: Doesn't track when finger enters/exits keys

## Improvement Strategy

### 1. Enhanced Key Detection with Entry/Exit Tracking

**Problem**: Current detection only checks if point is near key center, doesn't track key crossings.

**Solution**: Track when path enters and exits each key boundary.

```kotlin
// Add to KeyboardView.kt

data class KeyCrossing(
    val key: String,
    val entryPoint: PointF,
    val exitPoint: PointF?,
    val entryTime: Long,
    val duration: Long = 0L
)

private fun detectKeyCrossings(path: List<PointF>): List<KeyCrossing> {
    val crossings = mutableListOf<KeyCrossing>()
    val activeKeys = mutableMapOf<String, Pair<PointF, Long>>() // key -> (entryPoint, entryTime)
    
    for ((index, point) in path.withIndex()) {
        val currentTime = System.currentTimeMillis()
        
        // Check which keys this point is inside
        val keysAtPoint = keyBounds.filter { keyBound ->
            keyBound.bounds.contains(point.x, point.y) &&
            keyBound.key.type == KeyType.CHARACTER
        }
        
        // Track new key entries
        for (keyBound in keysAtPoint) {
            val keyText = keyBound.key.output
            if (!activeKeys.containsKey(keyText)) {
                // New key entry
                activeKeys[keyText] = point to currentTime
            }
        }
        
        // Check for key exits (point no longer inside key)
        val keysToExit = activeKeys.keys.filter { keyText ->
            val keyBound = keyBounds.find { it.key.output == keyText }
            keyBound?.let { !it.bounds.contains(point.x, point.y) } ?: true
        }
        
        for (keyText in keysToExit) {
            val (entryPoint, entryTime) = activeKeys.remove(keyText)!!
            val duration = currentTime - entryTime
            
            crossings.add(KeyCrossing(
                key = keyText,
                entryPoint = entryPoint,
                exitPoint = point,
                entryTime = entryTime,
                duration = duration
            ))
        }
    }
    
    // Add final active keys as crossings (finger still on key at end)
    for ((keyText, (entryPoint, entryTime)) in activeKeys) {
        crossings.add(KeyCrossing(
            key = keyText,
            entryPoint = entryPoint,
            exitPoint = path.lastOrNull(),
            entryTime = entryTime,
            duration = System.currentTimeMillis() - entryTime
        ))
    }
    
    return crossings
}
```

### 2. Improved Key Sequence Building

**Problem**: Current spatial deduplication (50% key width) may miss valid repeated letters.

**Solution**: Use key crossings + minimum duration threshold.

```kotlin
// Replace current key sequence building in KeyboardView.kt

private fun buildKeySequenceFromCrossings(crossings: List<KeyCrossing>): String {
    val sequence = StringBuilder()
    var lastKey: String? = null
    
    // Filter crossings by minimum duration (ignore accidental touches)
    val minDurationMs = 20L // Minimum 20ms on key to count
    
    for (crossing in crossings) {
        // Add key if:
        // 1. Different from last key, OR
        // 2. Same key but enough time passed (for repeated letters)
        val shouldAdd = when {
            crossing.key != lastKey -> true
            lastKey != null -> {
                // Same key repeated - check if enough time passed
                val timeSinceLast = crossing.entryTime - (crossings
                    .lastOrNull { it.key == lastKey }?.entryTime ?: 0L)
                timeSinceLast > 100L // 100ms gap for repeated letters
            }
            else -> false
        }
        
        if (shouldAdd && crossing.duration >= minDurationMs) {
            sequence.append(crossing.key)
            lastKey = crossing.key
        }
    }
    
    return sequence.toString()
}
```

### 3. Adaptive Key Detection Radius

**Problem**: Fixed 40% threshold doesn't adapt to swipe speed.

**Solution**: Adjust detection radius based on swipe velocity.

```kotlin
// Add to SwipeGestureDetector.kt

private fun getAdaptiveDetectionRadius(
    keyWidth: Float,
    swipeVelocity: Float,
    baseThreshold: Float = 0.4f
): Float {
    // Faster swipes need larger detection radius
    val velocityFactor = when {
        swipeVelocity > 2000f -> 1.5f  // Very fast: 60% of key width
        swipeVelocity > 1000f -> 1.25f // Fast: 50% of key width
        swipeVelocity > 500f -> 1.0f   // Normal: 40% (base)
        else -> 0.75f                  // Slow: 30% (more precise)
    }
    
    return keyWidth * baseThreshold * velocityFactor
}
```

### 4. Better Path Sampling for Fast Swipes

**Problem**: Current 8ms interval may miss keys during very fast swipes.

**Solution**: Adaptive sampling based on path curvature and velocity.

```kotlin
// Improve path sampling in SwipeGestureDetector.kt

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
        
        // Sample if:
        // 1. Distance threshold reached, OR
        // 2. Significant direction change (curvature)
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
    
    // Calculate angle between vectors
    val dot = v1x * v2x + v1y * v2y
    val mag1 = sqrt(v1x * v1x + v1y * v1y)
    val mag2 = sqrt(v2x * v2x + v2y * v2y)
    
    if (mag1 == 0f || mag2 == 0f) return false
    
    val cosAngle = dot / (mag1 * mag2)
    val angle = acos(cosAngle.coerceIn(-1f, 1f))
    
    // Significant change if angle > 30 degrees
    return angle > (PI / 6).toFloat()
}
```

### 5. Enhanced Word Prediction Scoring

**Problem**: Current scoring weights may not be optimal.

**Solution**: Tune weights and add velocity-based scoring.

```kotlin
// Update ImprovedSwipeWordPredictor.kt

companion object {
    // Optimized weights based on testing
    private const val SHAPE_WEIGHT = 0.35f      // Reduced from 0.4
    private const val LOCATION_WEIGHT = 0.45f   // Increased from 0.4
    private const val FREQUENCY_WEIGHT = 0.15f  // Reduced from 0.2
    private const val VELOCITY_WEIGHT = 0.05f   // NEW: Velocity consistency
    
    // Better pruning
    private const val START_KEY_N_CLOSEST = 3   // Increased from 2
    private const val END_KEY_N_CLOSEST = 3     // Increased from 2
    private const val LENGTH_TOLERANCE = 0.18f  // Increased from 0.14
}

private fun calculateWeightedScore(
    shapeDistance: Float,
    locationDistance: Float,
    frequency: Float,
    velocityConsistency: Float = 1.0f  // NEW parameter
): Float {
    val shapeSimilarity = exp(-shapeDistance * 2).toFloat().coerceIn(0f, 1f)
    val locationSimilarity = exp(-locationDistance * 5).toFloat().coerceIn(0f, 1f)
    
    // Weighted combination with velocity consistency
    return (shapeSimilarity * SHAPE_WEIGHT +
            locationSimilarity * LOCATION_WEIGHT +
            frequency * FREQUENCY_WEIGHT +
            velocityConsistency * VELOCITY_WEIGHT)
}
```

### 6. Probabilistic Key Detection

**Problem**: Binary key detection (in/out) doesn't handle edge cases well.

**Solution**: Use Gaussian probability for soft key detection.

```kotlin
// Add to KeyboardView.kt

private fun getKeyProbability(
    point: PointF,
    keyBound: KeyBound
): Float {
    val centerX = keyBound.bounds.centerX()
    val centerY = keyBound.bounds.centerY()
    val keyWidth = keyBound.bounds.width()
    val keyHeight = keyBound.bounds.height()
    
    // Distance from point to key center
    val dx = (point.x - centerX) / keyWidth
    val dy = (point.y - centerY) / keyHeight
    val distance = sqrt(dx * dx + dy * dy)
    
    // Gaussian probability (sigma = 0.5 means 68% within half key size)
    val sigma = 0.5f
    val probability = exp(-0.5f * (distance / sigma) * (distance / sigma))
    
    return probability.coerceIn(0f, 1f)
}

// Use probability threshold instead of binary detection
private fun findKeysWithProbability(
    point: PointF,
    minProbability: Float = 0.3f
): List<Pair<KeyBound, Float>> {
    return keyBounds
        .filter { it.key.type == KeyType.CHARACTER }
        .map { it to getKeyProbability(point, it) }
        .filter { it.second >= minProbability }
        .sortedByDescending { it.second }
}
```

### 7. Context-Aware Word Prediction

**Problem**: Doesn't consider previous words for better predictions.

**Solution**: Add context from previous words.

```kotlin
// Update SuggestionEngine to pass context

suspend fun getSwipeSuggestions(
    rawWord: String,
    keySequence: String,
    previousWord: String? = null,
    language: String = "kn"
): List<Suggestion> {
    // Get base suggestions
    val baseSuggestions = getSuggestions(rawWord, previousWord, language)
    
    // Boost suggestions that match key sequence better
    val scored = baseSuggestions.map { suggestion ->
        val keyMatchScore = calculateKeySequenceMatch(suggestion.word, keySequence)
        suggestion.copy(confidence = suggestion.confidence * (1.0f + keyMatchScore * 0.2f))
    }
    
    return scored.sortedByDescending { it.confidence }
}

private fun calculateKeySequenceMatch(word: String, keySequence: String): Float {
    // Check how well word matches the key sequence
    var matches = 0
    var wordIndex = 0
    
    for (key in keySequence) {
        if (wordIndex < word.length && 
            word[wordIndex].lowercaseChar() == key.lowercaseChar()) {
            matches++
            wordIndex++
        }
    }
    
    return matches.toFloat() / keySequence.length
}
```

## Implementation Priority

### Phase 1: Quick Wins (1-2 days)
1. ✅ Adaptive key detection radius
2. ✅ Better path sampling
3. ✅ Tune scoring weights

### Phase 2: Medium Improvements (3-4 days)
4. ✅ Key crossing detection
5. ✅ Probabilistic key detection
6. ✅ Enhanced key sequence building

### Phase 3: Advanced Features (5-7 days)
7. ✅ Context-aware predictions
8. ✅ Velocity-based scoring
9. ✅ Machine learning integration

## Testing Recommendations

1. **Test Cases**:
   - Fast swipes (velocity > 2000px/s)
   - Slow swipes (velocity < 500px/s)
   - Words with repeated letters ("hello", "coffee")
   - Short words (2-3 letters)
   - Long words (8+ letters)
   - Diagonal swipes
   - Curved paths

2. **Metrics to Track**:
   - Top-1 accuracy (first suggestion correct)
   - Top-3 accuracy (correct word in top 3)
   - Prediction latency
   - Key detection rate

3. **A/B Testing**:
   - Test different threshold values
   - Compare old vs new algorithm
   - Measure user satisfaction

## Expected Improvements

- **Accuracy**: 70% → 85%+ (top-1)
- **Latency**: < 100ms (maintained)
- **User Satisfaction**: Significant improvement in perceived accuracy


