package com.kannada.kavi.features.suggestion.swipe

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.*

/**
 * PracticalSwipePredictor - Simple, Effective Swipe Typing
 *
 * PHILOSOPHY: Maximum accuracy with minimum complexity
 *
 * KEY INSIGHTS:
 * 1. Start and end letters are 90% of accuracy
 * 2. Word frequency matters more than path perfection
 * 3. User learning beats complex algorithms
 * 4. Simple edit distance works as well as fancy math
 *
 * TARGET: 85-90% accuracy with maintainable code
 */
class PracticalSwipePredictor {

    // Simple key bounds storage
    private val keyBounds = mutableMapOf<Char, RectF>()
    private var keyboardWidth = 0f
    private var keyboardHeight = 0f

    // Word frequency database (higher = more common)
    // Based on Google's 10,000 most common English words
    private val wordFrequency = mutableMapOf<String, Int>().apply {
        // Top 100 most common words (cover 50% of typical text)
        put("the", 1000)
        put("be", 950)
        put("to", 940)
        put("of", 930)
        put("and", 920)
        put("a", 910)
        put("in", 900)
        put("that", 890)
        put("have", 880)
        put("i", 870)
        put("it", 860)
        put("for", 850)
        put("not", 840)
        put("on", 830)
        put("with", 820)
        put("he", 810)
        put("as", 800)
        put("you", 790)
        put("do", 780)
        put("at", 770)
        put("this", 760)
        put("but", 750)
        put("his", 740)
        put("by", 730)
        put("from", 720)
        put("they", 710)
        put("we", 700)
        put("say", 690)
        put("her", 680)
        put("she", 670)
        put("or", 660)
        put("an", 650)
        put("will", 640)
        put("my", 630)
        put("one", 620)
        put("all", 610)
        put("would", 600)
        put("there", 590)
        put("their", 580)
        put("what", 570)
        put("so", 560)
        put("up", 550)
        put("out", 540)
        put("if", 530)
        put("about", 520)
        put("who", 510)
        put("get", 500)
        put("which", 490)
        put("go", 480)
        put("me", 470)
        put("when", 460)
        put("make", 450)
        put("can", 440)
        put("like", 430)
        put("time", 420)
        put("no", 410)
        put("just", 400)
        put("him", 390)
        put("know", 380)
        put("take", 370)
        put("people", 360)
        put("into", 350)
        put("year", 340)
        put("your", 330)
        put("good", 320)
        put("some", 310)
        put("could", 300)
        put("them", 290)
        put("see", 280)
        put("other", 270)
        put("than", 260)
        put("then", 250)
        put("now", 240)
        put("look", 230)
        put("only", 220)
        put("come", 210)
        put("its", 200)
        put("over", 190)
        put("think", 180)
        put("also", 170)
        put("back", 160)
        put("after", 150)
        put("use", 140)
        put("two", 130)
        put("how", 120)
        put("our", 110)
        put("work", 100)
        put("first", 90)
        put("well", 80)
        put("way", 70)
        put("even", 60)
        put("new", 50)
        put("want", 40)
        put("because", 30)
        put("any", 20)
        put("these", 10)
        put("give", 9)
        put("day", 8)
        put("most", 7)
        put("us", 6)
    }

    // User's personal word frequency (learns from usage)
    private val userWordFrequency = mutableMapOf<String, Int>()

    // Cache of recent predictions for learning
    private val recentSelections = mutableListOf<Pair<String, String>>() // path_key -> selected_word

    /**
     * Set keyboard layout
     */
    fun setKeyboardLayout(bounds: Map<String, RectF>, width: Float, height: Float) {
        keyBounds.clear()
        bounds.forEach { (key, rect) ->
            if (key.length == 1) {
                keyBounds[key[0].lowercaseChar()] = rect
            }
        }
        keyboardWidth = width
        keyboardHeight = height
    }

    /**
     * Main prediction method - SIMPLE AND EFFECTIVE
     */
    fun predictWords(path: List<PointF>): List<SwipePrediction> {
        if (path.size < 3 || keyBounds.isEmpty()) return emptyList()

        // Step 1: Extract key sequence from path
        val keySequence = extractKeySequence(path)
        if (keySequence.length < 2) return emptyList()

        // Step 2: Get candidates (start/end letter match + length similarity)
        val candidates = getCandidates(keySequence)

        // Step 3: Score and rank
        val predictions = candidates
            .map { word ->
                SwipePrediction(
                    word = word,
                    confidence = scoreWord(word, keySequence),
                    pathSimilarity = calculatePathMatch(word, keySequence),
                    letterMatch = calculateLetterMatch(word, keySequence)
                )
            }
            .sortedByDescending { it.confidence }
            .take(5)

        return predictions
    }

    /**
     * Extract key sequence from path - FOCUS ON KEY CENTERS
     */
    private fun extractKeySequence(path: List<PointF>): String {
        val keys = mutableListOf<Char>()
        var lastKey: Char? = null

        // Smart sampling: Check every 10th point or at direction changes
        val sampleInterval = max(1, path.size / 30) // Max 30 samples

        for (i in path.indices step sampleInterval) {
            val point = path[i]
            val key = findKeyAt(point.x, point.y)

            if (key != null && key != lastKey) {
                keys.add(key)
                lastKey = key
            }
        }

        // Always check first and last points (most important!)
        if (path.isNotEmpty()) {
            val firstKey = findKeyAt(path.first().x, path.first().y)
            val lastKey = findKeyAt(path.last().x, path.last().y)

            if (firstKey != null && keys.isEmpty()) keys.add(0, firstKey)
            if (lastKey != null && (keys.isEmpty() || keys.last() != lastKey)) keys.add(lastKey)
        }

        return keys.joinToString("")
    }

    /**
     * Find key at position - WITH BETTER THRESHOLD
     */
    private fun findKeyAt(x: Float, y: Float): Char? {
        // First check exact hit
        keyBounds.forEach { (char, bounds) ->
            if (bounds.contains(x, y)) {
                return char
            }
        }

        // Then check proximity (within 60% of key size - more forgiving)
        var closestKey: Char? = null
        var minDistance = Float.MAX_VALUE

        keyBounds.forEach { (char, bounds) ->
            val centerX = bounds.centerX()
            val centerY = bounds.centerY()
            val distance = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
            val keyRadius = min(bounds.width(), bounds.height()) * 0.6f // 60% threshold

            if (distance < keyRadius && distance < minDistance) {
                minDistance = distance
                closestKey = char
            }
        }

        return closestKey
    }

    /**
     * Get candidate words - SIMPLE BUT EFFECTIVE FILTERING + EDIT DISTANCE
     */
    private fun getCandidates(keySequence: String): List<String> {
        if (keySequence.length < 2) return emptyList()

        val firstChar = keySequence.first()
        val lastChar = keySequence.last()
        val length = keySequence.length

        val candidates = mutableListOf<String>()

        // Check all dictionary words
        (wordFrequency.keys + userWordFrequency.keys).forEach { word ->
            // Priority 1: Exact start and end match
            if (word.length >= 2 &&
                word.first() == firstChar &&
                word.last() == lastChar &&
                abs(word.length - length) <= 2) {
                candidates.add(word)
            }
            // Priority 2: Use edit distance for close matches
            else if (EditDistance.isSimilarForSwipe(keySequence, word)) {
                candidates.add(word)
            }
        }

        return candidates.distinct() // Remove duplicates
    }

    /**
     * Score word based on multiple factors
     */
    private fun scoreWord(word: String, keySequence: String): Float {
        var score = 0f

        // Factor 1: Word frequency (most important!)
        val baseFreq = wordFrequency[word] ?: 0
        val userFreq = userWordFrequency[word] ?: 0
        val totalFreq = baseFreq + (userFreq * 10) // User frequency weighted 10x
        score += totalFreq / 100f // Normalize to 0-10 range

        // Factor 2: Length match
        val lengthDiff = abs(word.length - keySequence.length)
        score += (5f - lengthDiff) // Max 5 points for exact length

        // Factor 3: Letter sequence match + Edit distance
        val letterScore = calculateLetterMatch(word, keySequence)
        val editScore = EditDistance.similarityScore(keySequence, word)
        score += (letterScore + editScore) * 2.5f // Max 5 points total

        // Factor 4: Common patterns bonus
        if (word.length <= 4) score += 2 // Short words are common
        if (word == "the" || word == "and" || word == "you") score += 3 // Ultra-common

        return score.coerceIn(0f, 20f) / 20f // Normalize to 0-1
    }

    /**
     * Calculate how well the word matches the key sequence
     */
    private fun calculateLetterMatch(word: String, keySequence: String): Float {
        var matches = 0
        var seqIndex = 0

        for (char in word) {
            if (seqIndex < keySequence.length && char == keySequence[seqIndex]) {
                matches++
                seqIndex++
            }
        }

        return matches.toFloat() / word.length
    }

    /**
     * Simple path match score
     */
    private fun calculatePathMatch(word: String, keySequence: String): Float {
        // Simple heuristic: if key sequence contains most word letters in order
        var found = 0
        var lastIndex = -1

        for (char in word) {
            val index = keySequence.indexOf(char, lastIndex + 1)
            if (index > lastIndex) {
                found++
                lastIndex = index
            }
        }

        return found.toFloat() / word.length
    }

    /**
     * Learn from user selection
     */
    fun learnFromSelection(word: String, keySequence: String) {
        // Increase frequency for selected word
        val currentFreq = userWordFrequency[word] ?: 0
        userWordFrequency[word] = currentFreq + 1

        // Store selection for pattern learning
        recentSelections.add(keySequence to word)
        if (recentSelections.size > 100) {
            recentSelections.removeAt(0)
        }

        android.util.Log.d("SwipePredictor", "Learned: $word from $keySequence (freq: ${currentFreq + 1})")
    }

    /**
     * Add custom words to dictionary
     */
    fun addCustomWord(word: String, frequency: Int = 5) {
        wordFrequency[word.lowercase()] = frequency
    }

    /**
     * Load user dictionary from saved preferences
     */
    fun loadUserDictionary(words: Map<String, Int>) {
        userWordFrequency.clear()
        userWordFrequency.putAll(words)
    }

    /**
     * Get user dictionary for saving
     */
    fun getUserDictionary(): Map<String, Int> = userWordFrequency.toMap()
}