package com.kannada.kavi.features.suggestion.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Suggestion - A Word Prediction
 *
 * When you type "hel", the keyboard suggests "hello", "help", "helicopter".
 * Each of these suggestions is represented by this class.
 *
 * WHAT ARE SUGGESTIONS?
 * =====================
 * Suggestions are predicted words that appear above the keyboard.
 * They help you type faster by completing words or correcting typos.
 *
 * EXAMPLE:
 * User types: "nam"
 * Suggestions: ["ನಮಸ್ತೆ", "ನಾನು", "ನಮ್ಮ"]
 *
 * HOW SUGGESTIONS ARE RANKED:
 * ===========================
 * Suggestions have a confidence score (0.0 to 1.0) that determines order:
 * - 1.0 = Perfect match (user typed this exact word before)
 * - 0.8 = Very likely (common word, frequently used)
 * - 0.5 = Possible (word exists in dictionary)
 * - 0.2 = Unlikely (rare word or typo correction)
 *
 * SUGGESTION SOURCES:
 * ===================
 * Suggestions can come from different places:
 * - USER_HISTORY: Words the user typed before (most personalized)
 * - DICTIONARY: Standard Kannada words (most common)
 * - PREDICTION: ML-based next word prediction
 * - CORRECTION: Typo corrections (user typed "hlelo" → suggests "hello")
 * - TRANSLITERATION: Phonetic conversion (user types "namaste" → "ನಮಸ್ತೆ")
 */
@Parcelize
data class Suggestion(
    /**
     * The word being suggested
     *
     * Examples:
     * - "ನಮಸ್ತೆ" (Kannada word)
     * - "hello" (English word)
     * - "Bengaluru" (proper noun)
     */
    val word: String,

    /**
     * Confidence score (0.0 to 1.0)
     *
     * Higher score = more confident suggestion
     * Determines the order in which suggestions are shown.
     *
     * Scoring examples:
     * - 1.0: User just typed this word 5 seconds ago
     * - 0.9: User types this word frequently
     * - 0.7: Common word in dictionary, matches prefix exactly
     * - 0.5: Common word, partial match
     * - 0.3: Rare word, matches prefix
     * - 0.1: Typo correction with low confidence
     */
    val confidence: Float,

    /**
     * Where did this suggestion come from?
     *
     * Helps us understand why we suggested this word.
     * Useful for analytics and debugging.
     */
    val source: SuggestionSource,

    /**
     * Frequency - how often the user has typed this word
     *
     * Higher frequency = more personalized suggestion
     * Used to boost suggestions the user types often.
     *
     * Examples:
     * - 100: User types this word all the time (favorite word)
     * - 10: User has typed this a few times
     * - 1: User typed this once
     * - 0: Word from dictionary (user never typed it)
     */
    val frequency: Int = 0,

    /**
     * Is this the original typed text?
     *
     * Sometimes we show what the user typed as the first suggestion.
     * This allows them to keep their exact input without corrections.
     *
     * Example:
     * User types: "hlelo"
     * Suggestions: ["hlelo" (original), "hello" (correction), "hotel" (correction)]
     */
    val isOriginal: Boolean = false,

    /**
     * Optional metadata
     *
     * Extra information about this suggestion.
     * Can include:
     * - Part of speech (noun, verb, etc.)
     * - Definition
     * - Translation
     * - Usage examples
     *
     * Future feature: Show definitions in suggestion strip!
     */
    val metadata: Map<String, String>? = null

) : Parcelable {

    /**
     * Compare suggestions by confidence
     *
     * Used for sorting: highest confidence first
     */
    operator fun compareTo(other: Suggestion): Int {
        return other.confidence.compareTo(this.confidence)
    }

    /**
     * Is this a high-confidence suggestion?
     *
     * High-confidence suggestions might be shown differently:
     * - Larger font
     * - Bold text
     * - Different color
     */
    fun isHighConfidence(): Boolean = confidence >= 0.7f

    /**
     * Is this a medium-confidence suggestion?
     *
     * Medium suggestions are shown normally
     */
    fun isMediumConfidence(): Boolean = confidence >= 0.4f && confidence < 0.7f

    /**
     * Is this a low-confidence suggestion?
     *
     * Low-confidence suggestions might be:
     * - Shown in gray
     * - Smaller font
     * - Marked with "?"
     */
    fun isLowConfidence(): Boolean = confidence < 0.4f
}

/**
 * SuggestionSource - Where Did This Suggestion Come From?
 *
 * Different sources have different priorities:
 * 1. USER_HISTORY: Highest priority (personalized)
 * 2. PREDICTION: High priority (smart, context-aware)
 * 3. DICTIONARY: Medium priority (standard words)
 * 4. CORRECTION: Medium priority (fixing typos)
 * 5. TRANSLITERATION: High priority for phonetic layout
 */
enum class SuggestionSource {
    /**
     * From user's typing history
     *
     * Words the user has typed before.
     * Most personalized and relevant.
     *
     * Example: User frequently types "Bengaluru" → suggest it first
     */
    USER_HISTORY,

    /**
     * From built-in dictionary
     *
     * Standard Kannada/English words from dictionary file.
     * Common words everyone uses.
     *
     * Example: Dictionary contains "ನಮಸ್ತೆ", "ಕನ್ನಡ", etc.
     */
    DICTIONARY,

    /**
     * ML-based next word prediction
     *
     * Predicts what word comes next based on context.
     * Uses machine learning (TensorFlow Lite).
     *
     * Example: User types "Good" → predict "morning", "night", "day"
     */
    PREDICTION,

    /**
     * Typo correction
     *
     * Suggests corrections for misspelled words.
     *
     * Example: User types "hlelo" → suggest "hello"
     */
    CORRECTION,

    /**
     * Phonetic transliteration
     *
     * Converts English letters to Kannada script.
     * Only for phonetic layout.
     *
     * Example: User types "namaste" → suggest "ನಮಸ್ತೆ"
     */
    TRANSLITERATION
}

/**
 * USAGE EXAMPLE:
 * ==============
 *
 * Creating suggestions:
 * ```kotlin
 * val suggestion1 = Suggestion(
 *     word = "ನಮಸ್ತೆ",
 *     confidence = 0.95f,
 *     source = SuggestionSource.USER_HISTORY,
 *     frequency = 50
 * )
 *
 * val suggestion2 = Suggestion(
 *     word = "ನಾನು",
 *     confidence = 0.80f,
 *     source = SuggestionSource.DICTIONARY,
 *     frequency = 0
 * )
 *
 * val suggestion3 = Suggestion(
 *     word = "ನಮ್ಮ",
 *     confidence = 0.60f,
 *     source = SuggestionSource.PREDICTION,
 *     frequency = 5
 * )
 * ```
 *
 * Sorting suggestions:
 * ```kotlin
 * val suggestions = listOf(suggestion1, suggestion2, suggestion3)
 * val sorted = suggestions.sorted() // Highest confidence first
 * // Result: [suggestion1, suggestion2, suggestion3]
 * ```
 *
 * Checking confidence level:
 * ```kotlin
 * if (suggestion1.isHighConfidence()) {
 *     // Show in bold
 * } else if (suggestion1.isMediumConfidence()) {
 *     // Show normally
 * } else {
 *     // Show in gray
 * }
 * ```
 */
