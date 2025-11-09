package com.kannada.kavi.features.suggestion.correction

import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.features.suggestion.Trie

/**
 * TypoCorrector - Intelligent typo correction using Levenshtein distance
 *
 * This corrector finds and suggests corrections for misspelled words by
 * finding dictionary words with minimum edit distance.
 *
 * WHAT DOES IT DO?
 * ================
 * User types: "namste" (typo - missing 'a')
 * Corrector suggests: "namaste" (distance=1)
 *
 * User types: "begluru" (typo - swapped 'n' and missing 'n')
 * Corrector suggests: "bengaluru" (distance=2)
 *
 * User types: "recieve" (common typo - i before e)
 * Corrector suggests: "receive" (distance=2)
 *
 * HOW IT WORKS:
 * =============
 * 1. User types a word
 * 2. Check if word exists in dictionary → if yes, no correction needed
 * 3. If not, search dictionary for similar words
 * 4. Calculate Levenshtein distance for each candidate
 * 5. Filter by maximum edit distance (default: 2)
 * 6. Filter by minimum confidence threshold (default: 0.6)
 * 7. Return top N corrections, sorted by distance
 *
 * ALGORITHM:
 * ==========
 * Two-pass approach for efficiency:
 *
 * Pass 1: Length-based filtering (fast!)
 * - If word length = 5, only check dictionary words of length 3-7
 * - Why? Distance can't be > length difference
 * - Eliminates 90% of candidates instantly
 *
 * Pass 2: Calculate distance for remaining candidates
 * - Use Levenshtein distance algorithm
 * - Keep only candidates with distance <= MAX_EDIT_DISTANCE
 * - Calculate confidence score
 * - Return top candidates
 *
 * CONFIDENCE SCORE:
 * =================
 * confidence = 1.0 - (editDistance / wordLength)
 *
 * Examples:
 * - "namste" → "namaste": distance=1, length=7 → confidence=0.857
 * - "begluru" → "bengaluru": distance=2, length=9 → confidence=0.778
 * - "cat" → "dog": distance=3, length=3 → confidence=0.0
 *
 * Only show corrections with confidence >= threshold (default: 0.6)
 *
 * CONFIGURATION:
 * ==============
 * ALL settings from Constants.TypoCorrection - NO hardcoding!
 * - MAX_EDIT_DISTANCE: Maximum edit distance to consider (default: 2)
 * - MIN_WORD_LENGTH_FOR_CORRECTION: Only correct words >= 3 chars
 * - MAX_CORRECTION_CANDIDATES: Show top 5 corrections
 * - CORRECTION_CONFIDENCE_THRESHOLD: Minimum confidence (default: 0.6)
 */
class TypoCorrector(
    private val kannadaTrie: Trie,
    private val englishTrie: Trie
) {

    /**
     * Find typo corrections for a misspelled word
     *
     * @param misspelled The word to correct
     * @param language Language code ("kn" or "en")
     * @return List of Correction objects, sorted by confidence (highest first)
     *
     * Example:
     * ```kotlin
     * val corrections = corrector.findCorrections("namste", "kn")
     * // Returns: [Correction("namaste", 1, 0.857)]
     * ```
     */
    fun findCorrections(
        misspelled: String,
        language: String = "kn"
    ): List<Correction> {
        // Skip if word is too short
        if (misspelled.length < Constants.TypoCorrection.MIN_WORD_LENGTH_FOR_CORRECTION) {
            return emptyList()
        }

        // Select appropriate dictionary
        val trie = if (language == "en") englishTrie else kannadaTrie

        // Get all words from dictionary (use empty prefix to get all)
        // TODO: Optimize by only getting words within length range
        val dictionaryWords = trie.findWordsWithPrefix("", maxResults = 10000).map { it.first }

        // Check if word already exists (no correction needed)
        if (dictionaryWords.contains(misspelled)) {
            return emptyList()
        }

        // Find corrections
        return findCorrectionsInDictionary(misspelled, dictionaryWords)
    }

    /**
     * Find corrections with custom dictionary
     *
     * Useful for testing or using a custom word list.
     *
     * @param misspelled The word to correct
     * @param dictionary List of correct words
     * @return List of corrections
     */
    fun findCorrectionsInDictionary(
        misspelled: String,
        dictionary: List<String>
    ): List<Correction> {
        val candidates = mutableListOf<Correction>()
        val misspelledLower = misspelled.lowercase()
        val maxDistance = Constants.TypoCorrection.MAX_EDIT_DISTANCE

        // Pass 1: Length-based filtering
        val lengthMin = (misspelled.length - maxDistance).coerceAtLeast(1)
        val lengthMax = misspelled.length + maxDistance

        val filteredByLength = dictionary.filter { word ->
            word.length in lengthMin..lengthMax
        }

        // Pass 2: Calculate edit distance
        for (dictionaryWord in filteredByLength) {
            val dictionaryWordLower = dictionaryWord.lowercase()

            // Calculate distance
            val distance = LevenshteinDistance.calculate(misspelledLower, dictionaryWordLower)

            // Check if within threshold
            if (distance <= maxDistance && distance > 0) {
                // Calculate confidence
                val confidence = calculateConfidence(misspelled, dictionaryWord, distance)

                // Check confidence threshold
                if (confidence >= Constants.TypoCorrection.CORRECTION_CONFIDENCE_THRESHOLD) {
                    candidates.add(
                        Correction(
                            original = misspelled,
                            correction = dictionaryWord,
                            editDistance = distance,
                            confidence = confidence
                        )
                    )
                }
            }
        }

        // Sort by confidence (highest first) and limit results
        return candidates
            .sortedByDescending { it.confidence }
            .take(Constants.TypoCorrection.MAX_CORRECTION_CANDIDATES)
    }

    /**
     * Check if a word is likely misspelled
     *
     * @param word Word to check
     * @param language Language code
     * @return true if word not in dictionary
     */
    fun isMisspelled(word: String, language: String = "kn"): Boolean {
        if (word.length < Constants.TypoCorrection.MIN_WORD_LENGTH_FOR_CORRECTION) {
            return false
        }

        val trie = if (language == "en") englishTrie else kannadaTrie
        val words = trie.findWordsWithPrefix("", maxResults = 10000).map { it.first }

        return !words.contains(word) && !words.contains(word.lowercase())
    }

    /**
     * Get correction suggestions with reasons
     *
     * Returns detailed information about why each correction was suggested.
     *
     * @param misspelled The word to correct
     * @param language Language code
     * @return List of detailed corrections
     */
    fun findCorrectionsWithReasons(
        misspelled: String,
        language: String = "kn"
    ): List<DetailedCorrection> {
        val corrections = findCorrections(misspelled, language)

        return corrections.map { correction ->
            val reason = determineTypoReason(correction.original, correction.correction, correction.editDistance)

            DetailedCorrection(
                correction = correction,
                reason = reason
            )
        }
    }

    // ==================== Private Helper Functions ====================

    /**
     * Calculate confidence score for a correction
     *
     * confidence = 1.0 - (editDistance / wordLength)
     *
     * Adjustments:
     * - Boost if lengths match exactly
     * - Boost if first char matches (important for autocomplete)
     * - Penalize if distance is high relative to length
     */
    private fun calculateConfidence(
        original: String,
        correction: String,
        editDistance: Int
    ): Float {
        val maxLength = maxOf(original.length, correction.length)

        // Base confidence
        var confidence = 1.0f - (editDistance.toFloat() / maxLength.toFloat())

        // Boost if lengths match
        if (original.length == correction.length) {
            confidence += 0.1f
        }

        // Boost if first character matches (important for prefixes)
        if (original.isNotEmpty() && correction.isNotEmpty() &&
            original[0].lowercaseChar() == correction[0].lowercaseChar()
        ) {
            confidence += 0.05f
        }

        // Clamp to [0.0, 1.0]
        return confidence.coerceIn(0.0f, 1.0f)
    }

    /**
     * Determine the type of typo based on edit distance and patterns
     *
     * Helps explain to user what kind of mistake they made.
     */
    private fun determineTypoReason(original: String, correction: String, distance: Int): String {
        return when (distance) {
            1 -> {
                when {
                    original.length < correction.length -> "Missing letter"
                    original.length > correction.length -> "Extra letter"
                    else -> "Wrong letter"
                }
            }
            2 -> "Multiple mistakes"
            else -> "Significant difference"
        }
    }
}

/**
 * Correction - A typo correction suggestion
 *
 * @property original The misspelled word
 * @property correction The suggested correct word
 * @property editDistance Levenshtein distance
 * @property confidence Confidence score (0.0 to 1.0)
 */
data class Correction(
    val original: String,
    val correction: String,
    val editDistance: Int,
    val confidence: Float
) {
    override fun toString(): String {
        return "Correction($original → $correction, distance=$editDistance, confidence=${"%.2f".format(confidence)})"
    }
}

/**
 * DetailedCorrection - Correction with explanation
 *
 * @property correction The correction
 * @property reason Explanation of the typo
 */
data class DetailedCorrection(
    val correction: Correction,
    val reason: String
) {
    override fun toString(): String {
        return "${correction.correction} ($reason, confidence=${"%.2f".format(correction.confidence)})"
    }
}

/**
 * USAGE EXAMPLES:
 * ==============
 *
 * Example 1: Basic typo correction
 * ```kotlin
 * val corrector = TypoCorrector(kannadaTrie, englishTrie)
 *
 * val corrections = corrector.findCorrections("namste", "kn")
 * corrections.forEach { correction ->
 *     println(correction)
 * }
 * // Output: Correction(namste → namaste, distance=1, confidence=0.86)
 * ```
 *
 * Example 2: With reasons
 * ```kotlin
 * val detailed = corrector.findCorrectionsWithReasons("begluru", "kn")
 * detailed.forEach { detail ->
 *     println(detail)
 * }
 * // Output: bengaluru (Multiple mistakes, confidence=0.78)
 * ```
 *
 * Example 3: Check if misspelled
 * ```kotlin
 * val isMisspelled = corrector.isMisspelled("namste", "kn")
 * println("Is misspelled: $isMisspelled")  // true
 *
 * val isCorrect = corrector.isMisspelled("namaste", "kn")
 * println("Is correct: $isCorrect")  // false
 * ```
 *
 * Example 4: Integration with SuggestionEngine
 * ```kotlin
 * // In SuggestionEngine.getSuggestions()
 * val suggestions = mutableListOf<Suggestion>()
 *
 * // ... add dictionary suggestions ...
 *
 * // Add typo corrections if word is misspelled
 * if (typoCorrector.isMisspelled(currentWord, language)) {
 *     val corrections = typoCorrector.findCorrections(currentWord, language)
 *
 *     corrections.forEach { correction ->
 *         suggestions.add(Suggestion(
 *             word = correction.correction,
 *             confidence = correction.confidence,
 *             source = SuggestionSource.TYPO_CORRECTION,
 *             frequency = 0
 *         ))
 *     }
 * }
 *
 * return suggestions
 *     .sortedByDescending { it.confidence }
 *     .take(5)
 * ```
 *
 * PERFORMANCE:
 * ============
 * - Length filtering eliminates ~90% of candidates
 * - For 10,000 word dictionary: ~1,000 comparisons after filtering
 * - Levenshtein calculation: < 1ms per word
 * - Total time: ~100-200ms for 10,000 words
 * - Can optimize further with BK-trees or other data structures
 *
 * OPTIMIZATION IDEAS:
 * ===================
 * 1. **BK-Tree**: Pre-build metric tree for O(log n) lookup
 * 2. **N-gram indexing**: Pre-filter by character n-grams
 * 3. **Caching**: Cache frequent typos and their corrections
 * 4. **Early termination**: Stop if found correction with distance=1
 * 5. **Parallel processing**: Calculate distances in parallel for large dictionaries
 *
 * LIMITATIONS:
 * ============
 * 1. Only finds corrections within MAX_EDIT_DISTANCE (default: 2)
 * 2. Doesn't handle:
 *    - Split words ("cannot" → "can not")
 *    - Merged words ("can not" → "cannot")
 *    - Phonetic errors ("night" → "nite")
 * 3. Performance degrades with very large dictionaries (> 100k words)
 * 4. Case-insensitive (treats "Cat" and "cat" as same)
 *
 * FUTURE ENHANCEMENTS:
 * ====================
 * 1. Phonetic matching (Soundex, Metaphone)
 * 2. Context-aware corrections (use previous words)
 * 3. Learn from user corrections
 * 4. Language-specific patterns (Kannada-specific typos)
 * 5. Keyboard layout awareness (QWERTY vs Phonetic nearby keys)
 */
