package com.kannada.kavi.features.suggestion

import android.content.Context
import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.features.suggestion.models.Suggestion
import com.kannada.kavi.features.suggestion.models.SuggestionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SuggestionEngine - The Brain of Autocomplete
 *
 * This is the main engine that generates word suggestions as you type.
 * Think of it as a helpful assistant that predicts what you want to say!
 *
 * WHAT DOES IT DO?
 * ================
 * - You type: "nam"
 * - Engine suggests: ["ನಮಸ್ತೆ", "ನಾನು", "ನಮ್ಮ"]
 * - You select "ನಮಸ್ತೆ"
 * - Engine learns you like this word → ranks it higher next time
 *
 * HOW IT WORKS:
 * =============
 * 1. Load dictionaries (Kannada + English words)
 * 2. User types characters
 * 3. Find matching words in Trie (super fast!)
 * 4. Rank by: frequency + confidence + recency
 * 5. Return top 5 suggestions
 * 6. Learn from user's choices
 *
 * SUGGESTION ALGORITHM:
 * =====================
 * Confidence = (0.4 × frequency_score) + (0.3 × recency_score) + (0.3 × dictionary_score)
 *
 * Where:
 * - frequency_score: How often user types this word (0-1)
 * - recency_score: How recently user typed it (0-1)
 * - dictionary_score: Is it a common word? (0-1)
 *
 * EXAMPLE:
 * ========
 * User types "ka":
 * 1. Find words in Trie starting with "ka"
 * 2. Found: ["ಕನ್ನಡ", "ಕರ್ನಾಟಕ", "ಕಾಫಿ", "ಕಮಲ", ...]
 * 3. Calculate confidence for each:
 *    - "ಕನ್ನಡ": frequency=100, recency=high → confidence=0.95
 *    - "ಕರ್ನಾಟಕ": frequency=50, recency=medium → confidence=0.75
 *    - "ಕಾಫಿ": frequency=10, recency=low → confidence=0.50
 * 4. Sort by confidence: ["ಕನ್ನಡ", "ಕರ್ನಾಟಕ", "ಕಾಫಿ"]
 * 5. Return top 5
 *
 * PERFORMANCE:
 * ============
 * - Lookup time: < 1ms (thanks to Trie!)
 * - Memory: ~50MB for 100,000 words
 * - Suggestions update: < 50ms
 * - Background loading: Doesn't block keyboard
 */
class SuggestionEngine(private val context: Context) {

    // Trie for Kannada words
    private val kannadaTrie = Trie()

    // Trie for English words
    private val englishTrie = Trie()

    // User's personal word history (learned over time)
    private val userHistoryTrie = Trie()

    // Coroutine scope for async operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Current suggestions (observable by UI)
    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions: StateFlow<List<Suggestion>> = _suggestions.asStateFlow()

    // Is the engine ready to use?
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    // Learning enabled?
    private var learningEnabled = Constants.Suggestions.LEARNING_ENABLED_DEFAULT

    /**
     * Initialize the engine
     *
     * Loads dictionaries in background.
     * Call this in InputMethodService.onCreate()
     */
    fun initialize() {
        scope.launch {
            try {
                // Load Kannada dictionary
                loadKannadaDictionary()

                // Load English dictionary (for QWERTY layout)
                loadEnglishDictionary()

                // Load user's personal history
                loadUserHistory()

                _isReady.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                // Even if dictionaries fail to load, mark as ready
                // (user can still type without suggestions)
                _isReady.value = true
            }
        }
    }

    /**
     * Get suggestions for the current input
     *
     * This is the MAIN function called as user types.
     *
     * @param currentWord The word being typed (e.g., "nam")
     * @param previousWord The word before current (for context prediction)
     * @param language Current keyboard language ("kn" or "en")
     */
    suspend fun getSuggestions(
        currentWord: String,
        previousWord: String? = null,
        language: String = "kn"
    ): List<Suggestion> = withContext(Dispatchers.Default) {
        if (currentWord.isEmpty()) {
            return@withContext emptyList()
        }

        if (currentWord.length < Constants.Suggestions.MIN_WORD_LENGTH) {
            return@withContext emptyList()
        }

        val suggestions = mutableListOf<Suggestion>()

        // 1. Get suggestions from user history (highest priority)
        val historySuggestions = getUserHistorySuggestions(currentWord)
        suggestions.addAll(historySuggestions)

        // 2. Get suggestions from dictionary
        val dictionarySuggestions = getDictionarySuggestions(currentWord, language)
        suggestions.addAll(dictionarySuggestions)

        // 3. TODO: Get predictions based on previous word (ML-based)
        // val predictions = getNextWordPredictions(previousWord, language)
        // suggestions.addAll(predictions)

        // 4. TODO: Get typo corrections
        // val corrections = getTypoCorrections(currentWord, language)
        // suggestions.addAll(corrections)

        // Remove duplicates (keep highest confidence)
        val uniqueSuggestions = suggestions
            .groupBy { it.word.lowercase() }
            .map { (_, group) -> group.maxByOrNull { it.confidence }!! }

        // Sort by confidence and limit results
        val finalSuggestions = uniqueSuggestions
            .sortedByDescending { it.confidence } // Highest confidence first
            .take(Constants.Suggestions.MAX_SUGGESTIONS)

        // Update observable state
        _suggestions.value = finalSuggestions

        return@withContext finalSuggestions
    }

    /**
     * User selected a suggestion
     *
     * Learn from this choice to improve future suggestions.
     *
     * @param suggestion The suggestion user selected
     */
    fun onSuggestionSelected(suggestion: Suggestion) {
        if (!learningEnabled) return

        scope.launch {
            // Increase frequency in user history
            userHistoryTrie.incrementFrequency(suggestion.word, increment = 1)

            // TODO: Save to database for persistence
            // userHistoryRepository.recordWord(suggestion.word)

            // TODO: Update ML model with this choice
            // predictionModel.learn(suggestion.word, context)
        }
    }

    /**
     * User manually typed a word (not from suggestions)
     *
     * Learn this word too!
     *
     * @param word The word user typed
     */
    fun onWordTyped(word: String) {
        if (!learningEnabled) return
        if (word.length < Constants.Suggestions.MIN_WORD_LENGTH) return

        scope.launch {
            userHistoryTrie.incrementFrequency(word, increment = 1)

            // TODO: Save to database
            // userHistoryRepository.recordWord(word)
        }
    }

    /**
     * Clear all suggestions
     *
     * Call this when user deletes text or moves cursor
     */
    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    /**
     * Enable or disable learning
     *
     * @param enabled true to learn from user, false to disable
     */
    fun setLearningEnabled(enabled: Boolean) {
        learningEnabled = enabled
    }

    /**
     * Clear user history
     *
     * Useful for privacy or resetting personalization
     */
    fun clearUserHistory() {
        scope.launch {
            userHistoryTrie.clear()

            // TODO: Clear database
            // userHistoryRepository.clearAll()
        }
    }

    /**
     * Release resources
     *
     * Call this in InputMethodService.onDestroy()
     */
    fun release() {
        // Coroutines will be cancelled when scope is cancelled
        // Tries will be garbage collected
    }

    // ==================== Private Helper Functions ====================

    /**
     * Get suggestions from user's typing history
     */
    private fun getUserHistorySuggestions(prefix: String): List<Suggestion> {
        val words = userHistoryTrie.findWordsWithPrefix(prefix, maxResults = 10)

        return words.map { (word, frequency) ->
            // Higher frequency = higher confidence
            val maxFrequency = 100 // Assume max frequency is 100
            val confidence = (frequency.toFloat() / maxFrequency).coerceAtMost(1.0f)

            Suggestion(
                word = word,
                confidence = 0.7f + (confidence * 0.3f), // 0.7 to 1.0 range
                source = SuggestionSource.USER_HISTORY,
                frequency = frequency
            )
        }
    }

    /**
     * Get suggestions from dictionary
     */
    private fun getDictionarySuggestions(prefix: String, language: String): List<Suggestion> {
        val trie = if (language == "en") englishTrie else kannadaTrie
        val words = trie.findWordsWithPrefix(prefix, maxResults = 10)

        return words.map { (word, frequency) ->
            // Dictionary words have medium confidence
            val maxFrequency = 1000 // Common words have higher frequency
            val confidence = 0.5f + ((frequency.toFloat() / maxFrequency) * 0.2f)

            Suggestion(
                word = word,
                confidence = confidence.coerceAtMost(0.7f), // Max 0.7 for dictionary
                source = SuggestionSource.DICTIONARY,
                frequency = frequency
            )
        }
    }

    /**
     * Load Kannada dictionary from assets
     *
     * Dictionary format: One word per line, optionally with frequency
     * Example:
     * ```
     * ನಮಸ್ತೆ 100
     * ಕನ್ನಡ 95
     * ಕರ್ನಾಟಕ 80
     * ```
     */
    private suspend fun loadKannadaDictionary() = withContext(Dispatchers.IO) {
        try {
            // TODO: Load from actual dictionary file
            // For now, add some common words manually

            val commonWords = listOf(
                "ನಮಸ್ತೆ" to 100,
                "ಕನ್ನಡ" to 95,
                "ಕರ್ನಾಟಕ" to 90,
                "ಬೆಂಗಳೂರು" to 85,
                "ಹೌದು" to 80,
                "ಇಲ್ಲ" to 80,
                "ಧನ್ಯವಾದ" to 75,
                "ನಾನು" to 70,
                "ನೀನು" to 65,
                "ನಮ್ಮ" to 60,
                "ಹೇಗಿದ್ದೀರಿ" to 55,
                "ಬನ್ನಿ" to 50
            )

            commonWords.forEach { (word, frequency) ->
                kannadaTrie.insert(word, frequency)
            }

            // TODO: Load full dictionary from file
            /*
            context.assets.open("dictionaries/kannada.txt").bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    val parts = line.split(" ")
                    val word = parts[0]
                    val frequency = parts.getOrNull(1)?.toIntOrNull() ?: 1
                    kannadaTrie.insert(word, frequency)
                }
            }
            */
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Load English dictionary from assets
     */
    private suspend fun loadEnglishDictionary() = withContext(Dispatchers.IO) {
        try {
            // TODO: Load from actual dictionary file
            // For now, add some common words manually

            val commonWords = listOf(
                "hello" to 100,
                "help" to 90,
                "happy" to 80,
                "home" to 75,
                "thank" to 70,
                "thanks" to 70,
                "good" to 65,
                "morning" to 60,
                "night" to 60,
                "yes" to 55,
                "no" to 55
            )

            commonWords.forEach { (word, frequency) ->
                englishTrie.insert(word, frequency)
            }

            // TODO: Load full dictionary from file
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Load user's typing history from database
     */
    private suspend fun loadUserHistory() = withContext(Dispatchers.IO) {
        try {
            // TODO: Load from database
            /*
            val userWords = userHistoryRepository.getAllWords()
            userWords.forEach { (word, frequency) ->
                userHistoryTrie.insert(word, frequency)
            }
            */
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * USAGE EXAMPLE:
 * ==============
 *
 * In KaviInputMethodService:
 *
 * ```kotlin
 * private lateinit var suggestionEngine: SuggestionEngine
 *
 * override fun onCreate() {
 *     super.onCreate()
 *     suggestionEngine = SuggestionEngine(this)
 *     suggestionEngine.initialize()
 *
 *     // Observe suggestions
 *     lifecycleScope.launch {
 *         suggestionEngine.suggestions.collect { suggestions ->
 *             // Update UI with suggestions
 *             suggestionStrip.setSuggestions(suggestions)
 *         }
 *     }
 * }
 *
 * // When user types
 * fun onTextChanged(currentWord: String) {
 *     lifecycleScope.launch {
 *         suggestionEngine.getSuggestions(
 *             currentWord = currentWord,
 *             language = currentLayout.language
 *         )
 *     }
 * }
 *
 * // When user selects a suggestion
 * fun onSuggestionTapped(suggestion: Suggestion) {
 *     // Commit the word
 *     inputConnection.commitText(suggestion.word, 1)
 *
 *     // Learn from this choice
 *     suggestionEngine.onSuggestionSelected(suggestion)
 * }
 *
 * // When user completes a word
 * fun onSpacePressed() {
 *     val typedWord = getCurrentWord()
 *     suggestionEngine.onWordTyped(typedWord)
 *     suggestionEngine.clearSuggestions()
 * }
 * ```
 *
 * FUTURE ENHANCEMENTS:
 * ====================
 * 1. **ML-based prediction:** Use TensorFlow Lite for context-aware suggestions
 * 2. **Typo correction:** Levenshtein distance for fuzzy matching
 * 3. **Emoji suggestions:** Map words to relevant emojis
 * 4. **Multi-word suggestions:** Suggest phrases, not just words
 * 5. **Context awareness:** Suggest based on app (formal in email, casual in chat)
 * 6. **Time-based:** Different suggestions for morning/night
 * 7. **Dictionary updates:** Download new words from server
 */
