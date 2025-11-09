package com.kannada.kavi.features.suggestion.ml

import com.kannada.kavi.core.common.Constants
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * ContextManager - Tracks typing context for ML predictions
 *
 * The ML model needs context (previous words) to predict what you'll type next.
 * This class tracks your recent typing history in a sliding window.
 *
 * WHAT DOES IT DO?
 * ================
 * - Remembers the last N words you typed
 * - Provides context for ML predictions
 * - Clears context when appropriate (sentence boundaries, long pauses)
 * - Thread-safe (multiple threads can access safely)
 *
 * HOW IT WORKS:
 * =============
 * Sliding window of recent words:
 *
 * User types: "I am going to the"
 *
 * After "I":        ["I"]
 * After "am":       ["I", "am"]
 * After "going":    ["I", "am", "going"]
 * After "to":       ["am", "going", "to"]      ← Window slides! "I" dropped
 * After "the":      ["going", "to", "the"]     ← Window slides! "am" dropped
 *
 * Context is always the most recent N words (default N=3).
 *
 * WHY CONTEXT MATTERS:
 * ====================
 * Without context:
 *   Predict next word → could be anything! Too many possibilities.
 *
 * With context "I am going":
 *   Most likely next words: ["to", "home", "there", "now"]
 *   Less likely: ["apple", "computer", "yesterday"]
 *
 * Context makes predictions MUCH more accurate!
 *
 * SENTENCE BOUNDARIES:
 * ====================
 * Context is cleared when:
 * - User types punctuation: . ! ?
 * - User presses Enter/Return
 * - Long pause (>5 seconds between words)
 * - User manually clears (e.g., deletes all text)
 *
 * Example:
 * User types: "Hello world. How are you?"
 *                           ↑
 *                     Context cleared here!
 *
 * After "Hello":     ["Hello"]
 * After "world":     ["Hello", "world"]
 * After ".":         []                        ← Cleared!
 * After "How":       ["How"]
 * After "are":       ["How", "are"]
 * After "you":       ["How", "are", "you"]
 *
 * CONFIGURATION:
 * ==============
 * ALL settings from Constants.ML - NO hardcoding!
 * - Max context words: Constants.ML.MAX_CONTEXT_WORDS (default: 3)
 */
class ContextManager {

    // Sliding window of recent words
    private val contextWindow = mutableListOf<String>()

    // Read-write lock for thread safety
    private val lock = ReentrantReadWriteLock()

    // Timestamp of last word typed (for detecting long pauses)
    private var lastWordTimestamp = 0L

    // Maximum time between words before context is cleared (milliseconds)
    private val maxPauseDuration = 5000L  // 5 seconds

    /**
     * Track a typed word
     *
     * Adds a word to the context window. If window is full, oldest word is removed.
     * If long pause detected, context is cleared first.
     *
     * @param word The word that was just typed
     *
     * Example:
     * ```kotlin
     * val manager = ContextManager()
     *
     * manager.trackTypingContext("I")
     * manager.trackTypingContext("am")
     * manager.trackTypingContext("going")
     * manager.trackTypingContext("to")     // "I" dropped from window
     *
     * val context = manager.getContext(3)
     * // Returns: ["am", "going", "to"]
     * ```
     */
    fun trackTypingContext(word: String) {
        // Ignore empty/whitespace words
        if (word.isBlank()) return

        lock.write {
            val currentTime = System.currentTimeMillis()

            // Check for long pause - clear context if detected
            if (lastWordTimestamp > 0 && (currentTime - lastWordTimestamp) > maxPauseDuration) {
                contextWindow.clear()
                println("Context cleared due to long pause")
            }

            // Check for sentence boundary - clear context
            if (isSentenceBoundary(word)) {
                contextWindow.clear()
                println("Context cleared at sentence boundary: $word")
                lastWordTimestamp = currentTime
                return
            }

            // Add word to window
            contextWindow.add(word)

            // If window exceeds max size, remove oldest word (sliding window)
            while (contextWindow.size > Constants.ML.MAX_CONTEXT_WORDS) {
                val removed = contextWindow.removeAt(0)
                println("Context window full, dropped: $removed")
            }

            lastWordTimestamp = currentTime
        }
    }

    /**
     * Get current typing context
     *
     * Returns the most recent N words for ML prediction.
     * Returns fewer words if not enough context available yet.
     *
     * @param maxWords Maximum number of words to return (default: MAX_CONTEXT_WORDS)
     * @return List of recent words, oldest first
     *
     * Example:
     * ```kotlin
     * // User has typed: "I am going to the store"
     * // Context window: ["to", "the", "store"]
     *
     * val context = manager.getContext(3)
     * // Returns: ["to", "the", "store"]
     *
     * val smallerContext = manager.getContext(2)
     * // Returns: ["the", "store"]  ← Last 2 words
     * ```
     */
    fun getContext(maxWords: Int = Constants.ML.MAX_CONTEXT_WORDS): List<String> {
        return lock.read {
            // Return last N words (or all if less than N)
            if (contextWindow.size <= maxWords) {
                contextWindow.toList()  // Copy to prevent external modification
            } else {
                contextWindow.takeLast(maxWords)
            }
        }
    }

    /**
     * Get context size
     *
     * @return Number of words currently in context
     */
    fun getContextSize(): Int {
        return lock.read {
            contextWindow.size
        }
    }

    /**
     * Check if context is empty
     *
     * @return true if no context available
     */
    fun isEmpty(): Boolean {
        return lock.read {
            contextWindow.isEmpty()
        }
    }

    /**
     * Check if context is full
     *
     * @return true if context window has reached maximum size
     */
    fun isFull(): Boolean {
        return lock.read {
            contextWindow.size >= Constants.ML.MAX_CONTEXT_WORDS
        }
    }

    /**
     * Clear all context
     *
     * Removes all words from the context window.
     * Called when:
     * - User starts a new sentence
     * - User deletes all text
     * - User switches to a different text field
     *
     * Example:
     * ```kotlin
     * manager.clearContext()
     * println(manager.isEmpty())  // true
     * ```
     */
    fun clearContext() {
        lock.write {
            contextWindow.clear()
            lastWordTimestamp = 0L
            println("Context manually cleared")
        }
    }

    /**
     * Remove last word from context
     *
     * Called when user presses backspace to delete a word.
     *
     * Example:
     * ```kotlin
     * // Context: ["I", "am", "going"]
     * manager.removeLastWord()
     * // Context: ["I", "am"]
     * ```
     */
    fun removeLastWord() {
        lock.write {
            if (contextWindow.isNotEmpty()) {
                val removed = contextWindow.removeAt(contextWindow.size - 1)
                println("Removed last word from context: $removed")
            }
        }
    }

    /**
     * Get context as string (for debugging/logging)
     *
     * @return Context words joined by spaces
     *
     * Example:
     * ```kotlin
     * // Context: ["I", "am", "going"]
     * println(manager.getContextString())
     * // Output: "I am going"
     * ```
     */
    fun getContextString(): String {
        return lock.read {
            contextWindow.joinToString(" ")
        }
    }

    /**
     * Reset context manager
     *
     * Clears context and resets timestamp.
     * Called when keyboard is destroyed or user switches apps.
     */
    fun reset() {
        lock.write {
            contextWindow.clear()
            lastWordTimestamp = 0L
        }
    }

    // ==================== Private Helper Functions ====================

    /**
     * Check if word is a sentence boundary
     *
     * Sentence boundaries are:
     * - Period (.)
     * - Exclamation mark (!)
     * - Question mark (?)
     * - Newline characters
     *
     * Context should be cleared after these.
     *
     * @param word Word to check
     * @return true if word ends a sentence
     */
    private fun isSentenceBoundary(word: String): Boolean {
        if (word.isBlank()) return false

        // Check last character
        val lastChar = word.last()

        return when (lastChar) {
            '.', '!', '?' -> true
            '।', '॥' -> true  // Kannada sentence terminators (purna virama, deerga virama)
            '\n', '\r' -> true
            else -> false
        }
    }

    /**
     * Get statistics (for debugging/analytics)
     */
    fun getStats(): ContextStats {
        return lock.read {
            ContextStats(
                currentSize = contextWindow.size,
                maxSize = Constants.ML.MAX_CONTEXT_WORDS,
                isFull = contextWindow.size >= Constants.ML.MAX_CONTEXT_WORDS,
                timeSinceLastWord = if (lastWordTimestamp > 0) {
                    System.currentTimeMillis() - lastWordTimestamp
                } else 0L,
                currentContext = contextWindow.toList()
            )
        }
    }
}

/**
 * ContextStats - Statistics about current context
 *
 * Used for debugging, analytics, and testing.
 *
 * @property currentSize Number of words in context
 * @property maxSize Maximum context size
 * @property isFull Is context window full?
 * @property timeSinceLastWord Milliseconds since last word was typed
 * @property currentContext Copy of current context words
 */
data class ContextStats(
    val currentSize: Int,
    val maxSize: Int,
    val isFull: Boolean,
    val timeSinceLastWord: Long,
    val currentContext: List<String>
) {
    override fun toString(): String {
        return """
            ContextStats(
                size=$currentSize/$maxSize,
                full=$isFull,
                idleTime=${timeSinceLastWord}ms,
                context=${currentContext.joinToString(" ")}
            )
        """.trimIndent()
    }
}

/**
 * USAGE EXAMPLES:
 * ==============
 *
 * Example 1: Basic usage in SuggestionEngine
 * ```kotlin
 * class SuggestionEngine(context: Context) {
 *     private val contextManager = ContextManager()
 *     private val mlPredictor = MLPredictor(context)
 *
 *     fun onWordTyped(word: String) {
 *         // Track word for ML context
 *         contextManager.trackTypingContext(word)
 *
 *         // ... existing code (save to history, etc.) ...
 *     }
 *
 *     suspend fun getSuggestions(currentWord: String): List<Suggestion> {
 *         val suggestions = mutableListOf<Suggestion>()
 *
 *         // Get dictionary suggestions
 *         suggestions.addAll(getDictionarySuggestions(currentWord))
 *
 *         // Get ML predictions using context
 *         if (mlPredictor.isReady() && !contextManager.isEmpty()) {
 *             val context = contextManager.getContext(3)
 *             val mlResult = mlPredictor.predict(context)
 *
 *             if (mlResult is Result.Success) {
 *                 suggestions.addAll(mlResult.data.map { it.toSuggestion() })
 *             }
 *         }
 *
 *         return suggestions
 *             .sortedByDescending { it.confidence }
 *             .take(5)
 *     }
 *
 *     fun onSentenceEnd() {
 *         contextManager.clearContext()
 *     }
 * }
 * ```
 *
 * Example 2: Handling backspace
 * ```kotlin
 * fun onBackspacePressed() {
 *     val currentWord = getCurrentWord()
 *
 *     if (currentWord.isEmpty()) {
 *         // User deleted entire word - remove from context
 *         contextManager.removeLastWord()
 *     }
 * }
 * ```
 *
 * Example 3: Debugging context
 * ```kotlin
 * val stats = contextManager.getStats()
 * println(stats)
 * // Output:
 * // ContextStats(
 * //     size=3/3,
 * //     full=true,
 * //     idleTime=1234ms,
 * //     context=I am going
 * // )
 * ```
 *
 * Example 4: Context behavior with sentences
 * ```kotlin
 * val manager = ContextManager()
 *
 * // Type first sentence
 * manager.trackTypingContext("Hello")
 * manager.trackTypingContext("world")
 * println(manager.getContext())  // ["Hello", "world"]
 *
 * // End sentence with period
 * manager.trackTypingContext(".")
 * println(manager.getContext())  // []  ← Cleared!
 *
 * // Type second sentence
 * manager.trackTypingContext("How")
 * manager.trackTypingContext("are")
 * manager.trackTypingContext("you")
 * println(manager.getContext())  // ["How", "are", "you"]
 * ```
 *
 * Example 5: Long pause detection
 * ```kotlin
 * val manager = ContextManager()
 *
 * manager.trackTypingContext("Hello")
 * manager.trackTypingContext("world")
 *
 * // Wait 6 seconds (> 5 second threshold)
 * Thread.sleep(6000)
 *
 * manager.trackTypingContext("New")
 * // Context automatically cleared due to long pause
 * println(manager.getContext())  // ["New"]  ← Only latest word
 * ```
 *
 * IMPORTANT NOTES:
 * ================
 * 1. **Thread Safety**: Uses ReentrantReadWriteLock
 *    - Multiple threads can read simultaneously
 *    - Only one thread can write at a time
 *    - Safe to use from UI thread and background threads
 *
 * 2. **Sliding Window**: Automatically removes old words
 *    - Always maintains most recent N words
 *    - Memory-efficient (max N words stored)
 *
 * 3. **Automatic Clearing**: Context cleared at:
 *    - Sentence boundaries (. ! ? ।)
 *    - Long pauses (>5 seconds)
 *    - Manual clear() calls
 *
 * 4. **Performance**:
 *    - trackTypingContext(): O(1) amortized
 *    - getContext(): O(n) where n = context size (typically 3)
 *    - All operations are very fast (< 1ms)
 *
 * 5. **Memory Usage**: Minimal
 *    - Stores max 3 strings (~100 bytes)
 *    - No background threads
 *    - No heavy allocations
 */
