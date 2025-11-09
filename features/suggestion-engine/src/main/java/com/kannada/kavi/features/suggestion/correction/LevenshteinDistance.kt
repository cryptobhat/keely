package com.kannada.kavi.features.suggestion.correction

import com.kannada.kavi.core.common.Constants
import kotlin.math.min

/**
 * LevenshteinDistance - String similarity algorithm
 *
 * Calculates the minimum number of single-character edits (insertions, deletions, or substitutions)
 * required to change one word into another. Used for typo correction!
 *
 * WHAT IS IT?
 * ===========
 * Levenshtein distance measures how different two strings are.
 * Lower distance = more similar strings
 *
 * Examples:
 * - "cat" → "cat" = distance 0 (identical)
 * - "cat" → "bat" = distance 1 (substitute c→b)
 * - "cat" → "cats" = distance 1 (insert s)
 * - "cat" → "ca" = distance 1 (delete t)
 * - "cat" → "dog" = distance 3 (substitute all 3)
 *
 * USE CASES:
 * ==========
 * 1. Typo correction:
 *    User types: "namste" (typo)
 *    Find closest: "namaste" (distance=1)
 *
 * 2. Fuzzy search:
 *    User types: "begluru" (typo)
 *    Find closest: "bengaluru" (distance=2)
 *
 * 3. Spell checking:
 *    User types: "recieve" (typo)
 *    Find closest: "receive" (distance=1)
 *
 * ALGORITHM:
 * ==========
 * Dynamic programming approach with O(m×n) time and space complexity.
 *
 * Example: distance("cat", "bat")
 *
 * Matrix:
 *     ""  c  a  t
 * ""   0  1  2  3
 * b    1  1  2  3
 * a    2  2  1  2
 * t    3  3  2  1
 *
 * Result: 1 (bottom-right cell)
 *
 * How to read:
 * - Cell [i,j] = min edits to convert first i chars of "cat" to first j chars of "bat"
 * - Bottom-right = min edits for full strings
 *
 * OPERATIONS:
 * ===========
 * Three operations, each costs 1:
 * 1. Insert: Add a character
 * 2. Delete: Remove a character
 * 3. Substitute: Replace a character
 *
 * Optional: Transposition (swap adjacent chars) also costs 1
 * - Example: "teh" → "the" (swap e and h)
 * - This is Damerau-Levenshtein distance
 *
 * CONFIGURATION:
 * ==============
 * Costs from Constants.TypoCorrection:
 * - INSERTION_COST
 * - DELETION_COST
 * - SUBSTITUTION_COST
 * - TRANSPOSITION_COST (if using Damerau-Levenshtein)
 */
object LevenshteinDistance {

    /**
     * Calculate Levenshtein distance between two strings
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Edit distance (number of operations)
     *
     * Example:
     * ```kotlin
     * val distance = LevenshteinDistance.calculate("cat", "bat")
     * // Returns: 1
     * ```
     */
    fun calculate(s1: String, s2: String): Int {
        // Edge cases
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val m = s1.length
        val n = s2.length

        // Create DP table
        // dp[i][j] = distance between first i chars of s1 and first j chars of s2
        val dp = Array(m + 1) { IntArray(n + 1) }

        // Initialize first row and column
        for (i in 0..m) {
            dp[i][0] = i * Constants.TypoCorrection.DELETION_COST
        }
        for (j in 0..n) {
            dp[0][j] = j * Constants.TypoCorrection.INSERTION_COST
        }

        // Fill DP table
        for (i in 1..m) {
            for (j in 1..n) {
                // If characters match, no operation needed
                if (s1[i - 1] == s2[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1]
                } else {
                    // Take minimum of three operations:
                    // 1. Insert: dp[i][j-1] + insert cost
                    // 2. Delete: dp[i-1][j] + delete cost
                    // 3. Substitute: dp[i-1][j-1] + substitute cost
                    dp[i][j] = minOf(
                        dp[i][j - 1] + Constants.TypoCorrection.INSERTION_COST,
                        dp[i - 1][j] + Constants.TypoCorrection.DELETION_COST,
                        dp[i - 1][j - 1] + Constants.TypoCorrection.SUBSTITUTION_COST
                    )
                }
            }
        }

        return dp[m][n]
    }

    /**
     * Calculate normalized Levenshtein distance (0.0 to 1.0)
     *
     * Normalizes by dividing by the maximum possible distance.
     * - 0.0 = identical strings
     * - 1.0 = completely different
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Normalized distance (0.0 to 1.0)
     *
     * Example:
     * ```kotlin
     * val normalized = LevenshteinDistance.calculateNormalized("cat", "bat")
     * // Returns: 0.333 (1 edit / 3 chars max)
     * ```
     */
    fun calculateNormalized(s1: String, s2: String): Float {
        if (s1 == s2) return 0.0f
        if (s1.isEmpty() || s2.isEmpty()) return 1.0f

        val distance = calculate(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)

        return distance.toFloat() / maxLength.toFloat()
    }

    /**
     * Calculate similarity score (0.0 to 1.0)
     *
     * Inverse of normalized distance.
     * - 1.0 = identical strings
     * - 0.0 = completely different
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Similarity score (0.0 to 1.0)
     *
     * Example:
     * ```kotlin
     * val similarity = LevenshteinDistance.calculateSimilarity("cat", "bat")
     * // Returns: 0.667 (1.0 - 0.333)
     * ```
     */
    fun calculateSimilarity(s1: String, s2: String): Float {
        return 1.0f - calculateNormalized(s1, s2)
    }

    /**
     * Check if two strings are within edit distance threshold
     *
     * More efficient than calculating full distance if you only care about threshold.
     *
     * @param s1 First string
     * @param s2 Second string
     * @param threshold Maximum acceptable distance
     * @return true if distance <= threshold
     *
     * Example:
     * ```kotlin
     * val isClose = LevenshteinDistance.isWithinThreshold("cat", "bat", 1)
     * // Returns: true (distance=1, threshold=1)
     * ```
     */
    fun isWithinThreshold(s1: String, s2: String, threshold: Int): Boolean {
        // Quick rejection
        val lengthDiff = kotlin.math.abs(s1.length - s2.length)
        if (lengthDiff > threshold) return false

        val distance = calculate(s1, s2)
        return distance <= threshold
    }

    /**
     * Calculate Damerau-Levenshtein distance
     *
     * Includes transposition (swap adjacent characters) as an operation.
     * More accurate for typos like "teh" → "the"
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Edit distance including transpositions
     */
    fun calculateWithTransposition(s1: String, s2: String): Int {
        // Edge cases
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val m = s1.length
        val n = s2.length

        // Create DP table (need one extra row/col for transposition)
        val dp = Array(m + 1) { IntArray(n + 1) }

        // Initialize
        for (i in 0..m) {
            dp[i][0] = i * Constants.TypoCorrection.DELETION_COST
        }
        for (j in 0..n) {
            dp[0][j] = j * Constants.TypoCorrection.INSERTION_COST
        }

        // Fill DP table
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else Constants.TypoCorrection.SUBSTITUTION_COST

                dp[i][j] = minOf(
                    dp[i - 1][j] + Constants.TypoCorrection.DELETION_COST,      // Deletion
                    dp[i][j - 1] + Constants.TypoCorrection.INSERTION_COST,      // Insertion
                    dp[i - 1][j - 1] + cost                                      // Substitution
                )

                // Check for transposition
                if (i > 1 && j > 1 &&
                    s1[i - 1] == s2[j - 2] &&
                    s1[i - 2] == s2[j - 1]
                ) {
                    dp[i][j] = min(
                        dp[i][j],
                        dp[i - 2][j - 2] + Constants.TypoCorrection.TRANSPOSITION_COST
                    )
                }
            }
        }

        return dp[m][n]
    }

    /**
     * Find the closest match from a list of candidates
     *
     * Returns the candidate with minimum distance.
     *
     * @param target Target string
     * @param candidates List of candidate strings
     * @return Pair of (closest match, distance) or null if candidates empty
     *
     * Example:
     * ```kotlin
     * val (match, distance) = LevenshteinDistance.findClosest(
     *     "namste",
     *     listOf("namaste", "name", "master")
     * )
     * // Returns: ("namaste", 1)
     * ```
     */
    fun findClosest(target: String, candidates: List<String>): Pair<String, Int>? {
        if (candidates.isEmpty()) return null

        var closestMatch = candidates[0]
        var minDistance = calculate(target, candidates[0])

        for (i in 1 until candidates.size) {
            val distance = calculate(target, candidates[i])
            if (distance < minDistance) {
                minDistance = distance
                closestMatch = candidates[i]
            }
        }

        return Pair(closestMatch, minDistance)
    }

    /**
     * Find all matches within distance threshold
     *
     * Returns all candidates with distance <= threshold.
     *
     * @param target Target string
     * @param candidates List of candidate strings
     * @param threshold Maximum acceptable distance
     * @return List of (match, distance) pairs, sorted by distance
     *
     * Example:
     * ```kotlin
     * val matches = LevenshteinDistance.findMatchesWithinThreshold(
     *     "namste",
     *     listOf("namaste", "name", "master"),
     *     threshold = 2
     * )
     * // Returns: [("namaste", 1)]
     * ```
     */
    fun findMatchesWithinThreshold(
        target: String,
        candidates: List<String>,
        threshold: Int
    ): List<Pair<String, Int>> {
        return candidates
            .map { candidate -> Pair(candidate, calculate(target, candidate)) }
            .filter { (_, distance) -> distance <= threshold }
            .sortedBy { (_, distance) -> distance }
    }
}

/**
 * USAGE EXAMPLES:
 * ==============
 *
 * Example 1: Basic distance calculation
 * ```kotlin
 * val distance = LevenshteinDistance.calculate("cat", "bat")
 * println("Distance: $distance")  // 1
 *
 * val normalized = LevenshteinDistance.calculateNormalized("cat", "bat")
 * println("Normalized: $normalized")  // 0.333
 *
 * val similarity = LevenshteinDistance.calculateSimilarity("cat", "bat")
 * println("Similarity: $similarity")  // 0.667
 * ```
 *
 * Example 2: Typo correction
 * ```kotlin
 * val userInput = "namste"  // User made a typo
 * val dictionary = listOf("namaste", "name", "master", "aste")
 *
 * val (closest, distance) = LevenshteinDistance.findClosest(userInput, dictionary)!!
 * println("Did you mean: $closest? (distance=$distance)")
 * // Output: "Did you mean: namaste? (distance=1)"
 * ```
 *
 * Example 3: Find all similar words
 * ```kotlin
 * val userInput = "begluru"  // User made typos
 * val cities = listOf("bengaluru", "belgaum", "bellary", "bijapur")
 *
 * val matches = LevenshteinDistance.findMatchesWithinThreshold(
 *     userInput,
 *     cities,
 *     threshold = 2
 * )
 *
 * matches.forEach { (city, distance) ->
 *     println("$city (distance=$distance)")
 * }
 * // Output:
 * // bengaluru (distance=2)
 * ```
 *
 * Example 4: With transposition (Damerau-Levenshtein)
 * ```kotlin
 * val normal = LevenshteinDistance.calculate("teh", "the")
 * println("Normal: $normal")  // 2 (delete e, insert e)
 *
 * val withTransposition = LevenshteinDistance.calculateWithTransposition("teh", "the")
 * println("With transposition: $withTransposition")  // 1 (swap e and h)
 * ```
 *
 * Example 5: Threshold check (efficient)
 * ```kotlin
 * val isClose = LevenshteinDistance.isWithinThreshold("namste", "namaste", 1)
 * println("Is close: $isClose")  // true
 *
 * val isFar = LevenshteinDistance.isWithinThreshold("namste", "master", 1)
 * println("Is far: $isFar")  // false (distance=3)
 * ```
 *
 * PERFORMANCE:
 * ============
 * - Time complexity: O(m × n) where m, n = string lengths
 * - Space complexity: O(m × n) for DP table
 * - For strings of length 10: ~100 operations
 * - For strings of length 20: ~400 operations
 * - Very fast for typical word lengths (< 1ms)
 *
 * OPTIMIZATION TIPS:
 * ==================
 * 1. Use isWithinThreshold() if you only care about threshold
 * 2. Pre-filter by length difference before calculating
 * 3. Cache distances for frequent pairs
 * 4. Use normalized distance for similarity ranking
 *
 * COMMON PITFALLS:
 * ================
 * 1. Don't compare very long strings (> 1000 chars) - too slow!
 * 2. Remember: distance is case-sensitive - normalize first if needed
 * 3. Distance 0 = identical, NOT similar
 * 4. Threshold depends on word length (1 for short words, 2-3 for longer)
 */
