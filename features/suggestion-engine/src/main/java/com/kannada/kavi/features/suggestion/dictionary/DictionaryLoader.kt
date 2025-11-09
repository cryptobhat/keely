package com.kannada.kavi.features.suggestion.dictionary

import android.content.Context
import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.core.common.Result
import com.kannada.kavi.core.common.resultError
import com.kannada.kavi.core.common.resultSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * DictionaryLoader - Loads Word Dictionaries from Assets
 *
 * This class loads Kannada and English dictionaries from text files stored in the app's assets folder.
 *
 * DICTIONARY FILE FORMAT:
 * ======================
 * Each line contains:
 * word frequency
 *
 * Example (kannada_dictionary.txt):
 * ```
 * # Kannada Dictionary
 * # Format: word frequency
 * ನಮಸ್ತೆ 1000
 * ಕನ್ನಡ 950
 * ಕರ್ನಾಟಕ 800
 * ಬೆಂಗಳೂರು 750
 * ```
 *
 * HOW IT WORKS:
 * =============
 * 1. Open dictionary file from assets folder
 * 2. Read line by line
 * 3. Skip comments (lines starting with #)
 * 4. Parse each line: "word frequency"
 * 5. Return list of (word, frequency) pairs
 *
 * FEATURES:
 * =========
 * - Skip comment lines (# prefix)
 * - Skip empty lines
 * - Handle malformed lines gracefully
 * - Limit dictionary size for memory efficiency
 * - UTF-8 encoding support for Kannada
 * - Error handling for missing files
 *
 * USAGE:
 * ======
 * ```kotlin
 * val loader = DictionaryLoader(context)
 *
 * // Load Kannada dictionary
 * val kannadaResult = loader.loadDictionary(Constants.Dictionary.KANNADA_DICT_FILE)
 * when (kannadaResult) {
 *     is Result.Success -> {
 *         val words = kannadaResult.data  // List<Pair<String, Int>>
 *         words.forEach { (word, frequency) ->
 *             kannadaTrie.insert(word, frequency)
 *         }
 *     }
 *     is Result.Error -> {
 *         // Handle error
 *     }
 * }
 * ```
 */
class DictionaryLoader(private val context: Context) {

    /**
     * Load dictionary from assets file
     *
     * @param filePath Path to dictionary file in assets (e.g., "dictionaries/kannada_dictionary.txt")
     * @return Result containing list of (word, frequency) pairs
     */
    suspend fun loadDictionary(filePath: String): Result<List<Pair<String, Int>>> = withContext(Dispatchers.IO) {
        try {
            val words = mutableListOf<Pair<String, Int>>()
            var lineNumber = 0

            context.assets.open(filePath).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Constants.Dictionary.ENCODING)).use { reader ->
                    reader.forEachLine { line ->
                        lineNumber++

                        // Skip empty lines
                        if (line.isBlank()) return@forEachLine

                        // Skip comments
                        if (line.trimStart().startsWith(Constants.Dictionary.COMMENT_PREFIX)) {
                            return@forEachLine
                        }

                        // Parse line: "word frequency"
                        val parts = line.trim().split(Constants.Dictionary.WORD_FREQUENCY_SEPARATOR, limit = 2)

                        if (parts.isNotEmpty()) {
                            val word = parts[0].trim()
                            val frequency = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1

                            // Validate word is not empty and frequency is positive
                            if (word.isNotEmpty() && frequency >= Constants.Dictionary.MIN_WORD_FREQUENCY) {
                                words.add(word to frequency)

                                // Stop if we've reached max dictionary size
                                if (words.size >= Constants.Dictionary.MAX_DICTIONARY_SIZE) {
                                    return@use  // Exit reader.use
                                }
                            }
                        }
                    }
                }
            }

            resultSuccess(words)

        } catch (e: java.io.FileNotFoundException) {
            resultError("Dictionary file not found: $filePath")
        } catch (e: Exception) {
            resultError("Failed to load dictionary from $filePath: ${e.message}")
        }
    }

    /**
     * Load Kannada dictionary
     *
     * Convenience method to load the Kannada dictionary using the configured path
     *
     * @return Result containing list of (word, frequency) pairs
     */
    suspend fun loadKannadaDictionary(): Result<List<Pair<String, Int>>> {
        return loadDictionary(Constants.Dictionary.KANNADA_DICT_FILE)
    }

    /**
     * Load English dictionary
     *
     * Convenience method to load the English dictionary using the configured path
     *
     * @return Result containing list of (word, frequency) pairs
     */
    suspend fun loadEnglishDictionary(): Result<List<Pair<String, Int>>> {
        return loadDictionary(Constants.Dictionary.ENGLISH_DICT_FILE)
    }

    /**
     * Load common Kannada phrases
     *
     * Loads multi-word phrases for better suggestions
     *
     * @return Result containing list of (phrase, frequency) pairs
     */
    suspend fun loadKannadaPhrases(): Result<List<Pair<String, Int>>> {
        return loadDictionary(Constants.Dictionary.KANNADA_PHRASES_FILE)
    }

    /**
     * Get dictionary statistics
     *
     * Useful for debugging and analytics
     *
     * @param words List of (word, frequency) pairs
     * @return Statistics about the dictionary
     */
    fun getDictionaryStats(words: List<Pair<String, Int>>): DictionaryStats {
        val totalWords = words.size
        val totalFrequency = words.sumOf { it.second }
        val avgFrequency = if (totalWords > 0) totalFrequency / totalWords else 0
        val maxFrequency = words.maxOfOrNull { it.second } ?: 0
        val minFrequency = words.minOfOrNull { it.second } ?: 0

        return DictionaryStats(
            totalWords = totalWords,
            totalFrequency = totalFrequency,
            avgFrequency = avgFrequency,
            maxFrequency = maxFrequency,
            minFrequency = minFrequency
        )
    }
}

/**
 * Dictionary Statistics
 *
 * Contains metadata about a loaded dictionary
 */
data class DictionaryStats(
    val totalWords: Int,
    val totalFrequency: Int,
    val avgFrequency: Int,
    val maxFrequency: Int,
    val minFrequency: Int
) {
    override fun toString(): String {
        return """
            Dictionary Statistics:
            - Total Words: $totalWords
            - Total Frequency: $totalFrequency
            - Average Frequency: $avgFrequency
            - Max Frequency: $maxFrequency
            - Min Frequency: $minFrequency
        """.trimIndent()
    }
}

/**
 * FUTURE ENHANCEMENTS:
 * ====================
 * 1. **Compressed Dictionaries**: Support .gz files for smaller APK size
 * 2. **Incremental Loading**: Load dictionary in chunks for huge files
 * 3. **Dictionary Updates**: Download updated dictionaries from server
 * 4. **Multiple Formats**: Support different dictionary formats (ASPELL, Hunspell, etc.)
 * 5. **Metadata Loading**: Load dictionary metadata (language, version, author)
 * 6. **Validation**: Verify dictionary integrity with checksums
 * 7. **Custom Dictionaries**: Allow users to import their own dictionaries
 * 8. **Binary Format**: Convert to binary format for faster loading
 */
