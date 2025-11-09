package com.kannada.kavi.features.suggestion.transliteration

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.core.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * TransliterationEngine - English to Kannada Phonetic Conversion
 *
 * This engine converts English text to Kannada script using phonetic rules.
 * It's perfect for the phonetic keyboard layout!
 *
 * WHAT DOES IT DO?
 * ================
 * User types: "namaste"
 * Engine converts: "ನಮಸ್ತೆ"
 *
 * User types: "kannada"
 * Engine converts: "ಕನ್ನಡ"
 *
 * User types: "bengaluru"
 * Engine converts: "ಬೆಂಗಳೂರು"
 *
 * HOW IT WORKS:
 * =============
 * 1. Load phonetic rules from JSON (vowels, consonants, special cases)
 * 2. User types English text
 * 3. Match longest patterns first (greedy algorithm)
 * 4. Convert each pattern to Kannada
 * 5. Handle special cases (common words, conjuncts)
 * 6. Return transliterated text
 *
 * PHONETIC MAPPING:
 * =================
 * Vowels:
 *   a → ಅ, aa/ā → ಆ, i → ಇ, ii/ī → ಈ
 *   u → ಉ, uu/ū → ಊ, e → ಎ, ee/ē → ಏ
 *   o → ಒ, oo/ō → ಓ, ai → ಐ, au → ಔ
 *
 * Consonants:
 *   ka → ಕ, kha → ಖ, ga → ಗ, gha → ಘ, nga → ಙ
 *   cha → ಚ, chha → ಛ, ja → ಜ, jha → ಝ, nja → ಞ
 *   ta → ತ, tha → ಥ, da → ದ, dha → ಧ, na → ನ
 *   pa → ಪ, pha → ಫ, ba → ಬ, bha → ಭ, ma → ಮ
 *   ya → ಯ, ra → ರ, la → ಲ, va → ವ
 *   sha → ಶ, shha → ಷ, sa → ಸ, ha → ಹ, La → ಳ
 *
 * Special Cases:
 *   namaste → ನಮಸ್ತೆ (direct mapping)
 *   kannada → ಕನ್ನಡ (direct mapping)
 *   bengaluru → ಬೆಂಗಳೂರು (direct mapping)
 *
 * ALGORITHM:
 * ==========
 * Greedy longest-match approach:
 *
 * Input: "namaste"
 * Step 1: Check "namaste" in special cases → Found! Return "ನಮಸ್ತೆ"
 *
 * Input: "shree"
 * Step 1: Check 5 chars "shree" → No match
 * Step 2: Check 4 chars "shre" → No match
 * Step 3: Check 3 chars "shr" → No match
 * Step 4: Check 2 chars "sh" → Match! → "ಶ್"
 * Step 5: Remaining "ree"
 * Step 6: Check 3 chars "ree" → No match
 * Step 7: Check 2 chars "re" → Match! → "ರೇ"
 * Result: "ಶ್ರೀ"
 *
 * CONFIGURATION:
 * ==============
 * ALL settings from Constants.Transliteration - NO hardcoding!
 * - Rules file: Constants.Transliteration.PHONETIC_RULES_FILE
 * - Cache size: Constants.Transliteration.MAX_CACHE_SIZE
 * - Mode: Constants.Transliteration.DEFAULT_MODE
 */
class TransliterationEngine(private val context: Context) {

    // Phonetic rules loaded from JSON
    private val vowelRules = mutableMapOf<String, String>()
    private val consonantRules = mutableMapOf<String, String>()
    private val specialCases = mutableMapOf<String, String>()
    private val conjunctRules = mutableMapOf<String, String>()

    // Cache for frequent conversions (performance optimization)
    private val transliterationCache = mutableMapOf<String, String>()

    // Is the engine ready?
    @Volatile
    private var isInitialized = false

    /**
     * Initialize the transliteration engine
     *
     * Loads phonetic rules from JSON file in assets.
     *
     * @return Result.Success if initialized, Result.Error if failed
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Load phonetic rules from JSON
            val rulesResult = loadPhoneticRules()

            when (rulesResult) {
                is Result.Success -> {
                    val rules = rulesResult.data

                    // Populate rule maps
                    vowelRules.putAll(rules.vowels)
                    consonantRules.putAll(rules.consonants)
                    specialCases.putAll(rules.specialCases)
                    conjunctRules.putAll(rules.conjuncts ?: emptyMap())

                    isInitialized = true

                    println("TransliterationEngine initialized")
                    println("  Vowels: ${vowelRules.size}")
                    println("  Consonants: ${consonantRules.size}")
                    println("  Special cases: ${specialCases.size}")
                    println("  Conjuncts: ${conjunctRules.size}")

                    Result.Success(Unit)
                }
                is Result.Error -> {
                    Result.Error(rulesResult.exception)
                }
            }
        } catch (e: Exception) {
            println("TransliterationEngine initialization failed: ${e.message}")
            e.printStackTrace()
            Result.Error(Exception("Failed to initialize TransliterationEngine: ${e.message}", e))
        }
    }

    /**
     * Transliterate English text to Kannada
     *
     * Converts English phonetic text to Kannada script using loaded rules.
     *
     * @param englishText English text to transliterate
     * @return Kannada text
     *
     * Example:
     * ```kotlin
     * val kannada = engine.transliterate("namaste")
     * // Returns: "ನಮಸ್ತೆ"
     * ```
     */
    fun transliterate(englishText: String): String {
        require(isInitialized) { "TransliterationEngine not initialized" }

        if (englishText.isEmpty()) return ""

        // Check cache first
        if (Constants.Transliteration.ENABLE_CACHING) {
            transliterationCache[englishText]?.let { return it }
        }

        // Normalize input
        val normalized = if (Constants.Transliteration.CASE_SENSITIVE) {
            englishText
        } else {
            englishText.lowercase()
        }

        // Check special cases first (fastest lookup for common words)
        specialCases[normalized]?.let { kannada ->
            cacheResult(englishText, kannada)
            return kannada
        }

        // Transliterate character by character
        val result = transliterateCharacters(normalized)

        // Cache result
        cacheResult(englishText, result)

        return result
    }

    /**
     * Get multiple transliteration suggestions
     *
     * Returns variations of the transliteration (useful for ambiguous inputs)
     *
     * @param englishText English text
     * @return List of possible transliterations
     */
    fun getSuggestions(englishText: String): List<String> {
        require(isInitialized) { "TransliterationEngine not initialized" }

        val suggestions = mutableListOf<String>()

        // Add main transliteration
        val main = transliterate(englishText)
        if (main.isNotEmpty()) {
            suggestions.add(main)
        }

        // TODO: Add variations
        // - Different vowel lengths (a vs aa)
        // - Alternative conjuncts
        // - Regional variations

        return suggestions.distinct()
    }

    /**
     * Check if text is already Kannada
     *
     * @param text Text to check
     * @return true if text contains Kannada characters
     */
    fun isKannada(text: String): Boolean {
        return text.any { char ->
            char.code in 0x0C80..0x0CFF // Kannada Unicode block
        }
    }

    /**
     * Clear transliteration cache
     *
     * Call this when rules are updated or memory needs to be freed
     */
    fun clearCache() {
        transliterationCache.clear()
        println("Transliteration cache cleared")
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            size = transliterationCache.size,
            maxSize = Constants.Transliteration.MAX_CACHE_SIZE,
            hitRate = 0f // TODO: Track hit rate
        )
    }

    // ==================== Private Helper Functions ====================

    /**
     * Transliterate text character by character using greedy longest-match
     */
    private fun transliterateCharacters(text: String): String {
        val result = StringBuilder()
        var i = 0

        while (i < text.length) {
            // Try to match longest pattern first
            var matched = false
            var maxLength = minOf(10, text.length - i) // Check up to 10 chars

            for (length in maxLength downTo 1) {
                val substring = text.substring(i, i + length)

                // Check conjuncts first (higher priority)
                conjunctRules[substring]?.let { kannada ->
                    result.append(kannada)
                    i += length
                    matched = true
                    return@let
                }

                // Check consonants
                consonantRules[substring]?.let { kannada ->
                    result.append(kannada)
                    i += length
                    matched = true
                    return@let
                }

                // Check vowels
                vowelRules[substring]?.let { kannada ->
                    result.append(kannada)
                    i += length
                    matched = true
                    return@let
                }
            }

            // No match found - keep original character
            if (!matched) {
                result.append(text[i])
                i++
            }
        }

        return result.toString()
    }

    /**
     * Cache a transliteration result
     */
    private fun cacheResult(english: String, kannada: String) {
        if (!Constants.Transliteration.ENABLE_CACHING) return

        // Check cache size limit
        if (transliterationCache.size >= Constants.Transliteration.MAX_CACHE_SIZE) {
            // Remove oldest entry (simple LRU approximation)
            val firstKey = transliterationCache.keys.firstOrNull()
            firstKey?.let { transliterationCache.remove(it) }
        }

        transliterationCache[english] = kannada
    }

    /**
     * Load phonetic rules from JSON file
     */
    private suspend fun loadPhoneticRules(): Result<PhoneticRules> = withContext(Dispatchers.IO) {
        try {
            val rulesFile = Constants.Transliteration.PHONETIC_RULES_FILE

            context.assets.open(rulesFile).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    val json = reader.readText()

                    // Parse JSON
                    val gson = Gson()
                    val rules: PhoneticRules = gson.fromJson(json, PhoneticRules::class.java)

                    // Validate rules
                    if (rules.vowels.isEmpty()) {
                        return@withContext Result.Error(
                            Exception("Phonetic rules missing vowels")
                        )
                    }

                    if (rules.consonants.isEmpty()) {
                        return@withContext Result.Error(
                            Exception("Phonetic rules missing consonants")
                        )
                    }

                    Result.Success(rules)
                }
            }
        } catch (e: java.io.FileNotFoundException) {
            println("Phonetic rules file not found: ${Constants.Transliteration.PHONETIC_RULES_FILE}")
            Result.Error(Exception("Phonetic rules file not found", e))

        } catch (e: Exception) {
            println("Failed to load phonetic rules: ${e.message}")
            e.printStackTrace()
            Result.Error(Exception("Failed to load phonetic rules: ${e.message}", e))
        }
    }
}

/**
 * PhoneticRules - Data model for phonetic mapping rules
 */
data class PhoneticRules(
    val vowels: Map<String, String>,
    val consonants: Map<String, String>,
    val specialCases: Map<String, String>,
    val conjuncts: Map<String, String>? = null
)

/**
 * CacheStats - Statistics about transliteration cache
 */
data class CacheStats(
    val size: Int,
    val maxSize: Int,
    val hitRate: Float
)

/**
 * USAGE EXAMPLE:
 * ==============
 *
 * ```kotlin
 * val engine = TransliterationEngine(context)
 *
 * // Initialize
 * val initResult = engine.initialize()
 * if (initResult is Result.Error) {
 *     println("Failed: ${initResult.exception.message}")
 *     return
 * }
 *
 * // Transliterate
 * val kannada = engine.transliterate("namaste")
 * println(kannada)  // "ನಮಸ್ತೆ"
 *
 * // Get suggestions
 * val suggestions = engine.getSuggestions("kannada")
 * println(suggestions)  // ["ಕನ್ನಡ"]
 *
 * // Check if already Kannada
 * val isKannada = engine.isKannada("ನಮಸ್ತೆ")
 * println(isKannada)  // true
 * ```
 *
 * INTEGRATION WITH SUGGESTIONENGINE:
 * ===================================
 *
 * ```kotlin
 * // In SuggestionEngine
 * private val transliterationEngine = TransliterationEngine(context)
 *
 * suspend fun getSuggestions(...): List<Suggestion> {
 *     val suggestions = mutableListOf<Suggestion>()
 *
 *     // ... existing suggestions ...
 *
 *     // Add transliteration if typing in English on phonetic layout
 *     if (layout == "phonetic" && !transliterationEngine.isKannada(currentWord)) {
 *         val transliterated = transliterationEngine.transliterate(currentWord)
 *         if (transliterated.isNotEmpty()) {
 *             suggestions.add(Suggestion(
 *                 word = transliterated,
 *                 confidence = 0.8f,
 *                 source = SuggestionSource.TRANSLITERATION,
 *                 frequency = 0
 *             ))
 *         }
 *     }
 *
 *     return suggestions
 * }
 * ```
 */
