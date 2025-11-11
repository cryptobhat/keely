package com.kannada.kavi.features.suggestion.swipe

/**
 * Simple Edit Distance Calculator
 *
 * Measures how similar two strings are by counting the minimum number of
 * single-character edits (insertions, deletions, substitutions) needed
 * to transform one string into the other.
 *
 * Used for swipe typing to handle cases where the swipe path misses
 * a letter or includes an extra one.
 */
object EditDistance {

    /**
     * Calculate the Levenshtein distance between two strings
     *
     * @param s1 First string (e.g., the swiped key sequence)
     * @param s2 Second string (e.g., a dictionary word)
     * @return The edit distance (lower is better, 0 means exact match)
     */
    fun calculate(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        // Handle empty strings
        if (len1 == 0) return len2
        if (len2 == 0) return len1

        // Create a 2D array for dynamic programming
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        // Initialize first column (delete all from s1)
        for (i in 0..len1) {
            dp[i][0] = i
        }

        // Initialize first row (insert all from s2)
        for (j in 0..len2) {
            dp[0][j] = j
        }

        // Fill the rest of the table
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1

                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Calculate normalized edit distance (0 to 1 range)
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Normalized distance (0 = identical, 1 = completely different)
     */
    fun normalizedDistance(s1: String, s2: String): Float {
        val distance = calculate(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)

        return if (maxLength == 0) 0f else distance.toFloat() / maxLength
    }

    /**
     * Check if two strings are similar enough for swipe typing
     *
     * Rules for swipe typing similarity:
     * - Must start with same letter
     * - Must end with same letter (if length > 2)
     * - Edit distance should be <= 2 for short words, <= 3 for long words
     */
    fun isSimilarForSwipe(swipeSequence: String, word: String): Boolean {
        // Both must have content
        if (swipeSequence.isEmpty() || word.isEmpty()) return false

        // Must start with same letter (critical for accuracy)
        if (swipeSequence.first() != word.first()) return false

        // For words longer than 2, must end with same letter
        if (word.length > 2 && swipeSequence.length > 1) {
            if (swipeSequence.last() != word.last()) return false
        }

        // Calculate allowed edit distance based on word length
        val maxDistance = when (word.length) {
            in 0..3 -> 1   // Very short words: allow 1 edit
            in 4..6 -> 2   // Medium words: allow 2 edits
            else -> 3      // Long words: allow 3 edits
        }

        // Check if edit distance is acceptable
        val distance = calculate(swipeSequence, word)
        return distance <= maxDistance
    }

    /**
     * Calculate similarity score for ranking predictions
     *
     * @param swipeSequence The key sequence from swipe
     * @param word The dictionary word
     * @return Score from 0 to 1 (higher is better)
     */
    fun similarityScore(swipeSequence: String, word: String): Float {
        // Special case: exact match
        if (swipeSequence == word) return 1.0f

        // Check basic requirements
        if (!isSimilarForSwipe(swipeSequence, word)) return 0.0f

        // Calculate normalized distance
        val normalDist = normalizedDistance(swipeSequence, word)

        // Convert distance to similarity (inverse)
        var score = 1.0f - normalDist

        // Bonus for matching start and end
        if (swipeSequence.isNotEmpty() && word.isNotEmpty()) {
            if (swipeSequence.first() == word.first()) score += 0.1f
            if (swipeSequence.last() == word.last()) score += 0.1f
        }

        // Bonus for similar length
        val lengthDiff = kotlin.math.abs(swipeSequence.length - word.length)
        if (lengthDiff == 0) score += 0.2f
        else if (lengthDiff == 1) score += 0.1f

        return score.coerceIn(0f, 1f)
    }

    /**
     * Find the best matching words from a list using edit distance
     *
     * @param swipeSequence The key sequence from swipe
     * @param words List of candidate words
     * @param maxResults Maximum number of results to return
     * @return List of words sorted by similarity
     */
    fun findBestMatches(
        swipeSequence: String,
        words: List<String>,
        maxResults: Int = 5
    ): List<Pair<String, Float>> {
        return words
            .map { word -> word to similarityScore(swipeSequence, word) }
            .filter { it.second > 0.3f } // Minimum threshold
            .sortedByDescending { it.second }
            .take(maxResults)
    }
}