package com.kannada.kavi.features.suggestion.ml

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.core.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * InputPreprocessor - Converts text to tensors for ML model
 *
 * The neural network doesn't understand words - it only understands numbers!
 * This class converts words → numbers (encoding) so TensorFlow Lite can process them.
 *
 * WHAT DOES IT DO?
 * ================
 * 1. Loads vocabulary (word → number mapping) from JSON file
 * 2. Converts words to numbers using vocabulary
 * 3. Pads sequences to fixed length (all inputs must be same size)
 * 4. Converts to ByteBuffer format (required by TensorFlow Lite)
 *
 * HOW ENCODING WORKS:
 * ===================
 * Vocabulary:
 * {
 *   "ನಮಸ್ತೆ": 1,
 *   "ಕನ್ನಡ": 2,
 *   "ನಾನು": 3,
 *   "<UNK>": 0,   // Unknown word
 *   "<PAD>": 9999 // Padding token
 * }
 *
 * Example conversion:
 * Input words: ["ನಮಸ್ತೆ", "ಕನ್ನಡ"]
 *    ↓
 * Encoded: [1, 2]
 *    ↓
 * Padded to length 3: [1, 2, 9999]
 *    ↓
 * ByteBuffer: [00 00 00 01] [00 00 00 02] [00 00 27 0F]
 *
 * UNKNOWN WORDS:
 * ==============
 * If a word is not in vocabulary, it's replaced with <UNK> token (ID = 0)
 *
 * Example:
 * Input: ["ಹಲೋ", "xyz", "ನಮಸ್ತೆ"]  // "xyz" not in vocabulary
 * Encoded: [123, 0, 1]           // "xyz" → 0 (unknown)
 *
 * PADDING:
 * ========
 * Neural networks need fixed-size inputs. We pad short sequences.
 *
 * Example with maxLength=5:
 * Input: ["word1", "word2"]      // Length 2
 * Padded: [45, 67, 9999, 9999, 9999]  // Padded to 5
 *
 * CONFIGURATION:
 * ==============
 * ALL settings from Constants.ML - NO hardcoding!
 * - Vocabulary file: Constants.ML.VOCABULARY_FILE
 * - Vocabulary size: Constants.ML.VOCABULARY_SIZE
 * - Unknown token: Constants.ML.UNKNOWN_TOKEN
 * - Pad token: Constants.ML.PAD_TOKEN
 * - Max sequence length: Constants.ML.MAX_SEQUENCE_LENGTH
 */
class InputPreprocessor(private val context: Context) {

    // Word → Number mapping (loaded from vocabulary.json)
    private val wordToId = mutableMapOf<String, Int>()

    // Number → Word mapping (for debugging)
    private val idToWord = mutableMapOf<Int, String>()

    // Special token IDs
    private var unknownTokenId: Int = 0
    private var padTokenId: Int = 0

    // Is the preprocessor ready?
    @Volatile
    private var isInitialized = false

    /**
     * Initialize the preprocessor
     *
     * Loads vocabulary from assets/ml_models/vocabulary.json
     *
     * @return Result.Success if loaded, Result.Error if failed
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Load vocabulary from JSON
            val vocabularyResult = loadVocabulary()

            when (vocabularyResult) {
                is Result.Success -> {
                    val vocabulary = vocabularyResult.data

                    // Populate maps
                    vocabulary.forEach { (word, id) ->
                        wordToId[word] = id
                        idToWord[id] = word
                    }

                    // Get special token IDs
                    unknownTokenId = wordToId[Constants.ML.UNKNOWN_TOKEN] ?: 0
                    padTokenId = wordToId[Constants.ML.PAD_TOKEN] ?: 0

                    isInitialized = true

                    println("InputPreprocessor initialized with ${wordToId.size} words")
                    println("Unknown token ID: $unknownTokenId")
                    println("Pad token ID: $padTokenId")

                    Result.Success(Unit)
                }
                is Result.Error -> {
                    Result.Error(vocabularyResult.exception)
                }
            }
        } catch (e: Exception) {
            println("InputPreprocessor initialization failed: ${e.message}")
            e.printStackTrace()
            Result.Error(Exception("Failed to initialize InputPreprocessor: ${e.message}", e))
        }
    }

    /**
     * Encode words to numbers
     *
     * Converts a list of words to their corresponding IDs in the vocabulary.
     * Unknown words are replaced with unknownTokenId.
     *
     * @param words List of words to encode
     * @return IntArray of encoded IDs
     *
     * Example:
     * ```kotlin
     * val words = listOf("ನಮಸ್ತೆ", "ಕನ್ನಡ", "xyz")
     * val encoded = preprocessor.encodeWords(words)
     * // Returns: [1, 2, 0]  // "xyz" → 0 (unknown)
     * ```
     */
    fun encodeWords(words: List<String>): IntArray {
        require(isInitialized) { "InputPreprocessor not initialized" }

        return words.map { word ->
            wordToId.getOrDefault(word, unknownTokenId)
        }.toIntArray()
    }

    /**
     * Encode a single word
     *
     * @param word Word to encode
     * @return Word ID, or unknownTokenId if not in vocabulary
     */
    fun encodeWord(word: String): Int {
        require(isInitialized) { "InputPreprocessor not initialized" }
        return wordToId.getOrDefault(word, unknownTokenId)
    }

    /**
     * Pad sequence to fixed length
     *
     * Neural networks need fixed-size inputs. This pads or truncates
     * sequences to the specified length.
     *
     * @param sequence Array of encoded word IDs
     * @param maxLength Target length (default: Constants.ML.MAX_SEQUENCE_LENGTH)
     * @return Padded IntArray
     *
     * Example:
     * ```kotlin
     * val sequence = intArrayOf(1, 2)
     * val padded = preprocessor.padSequence(sequence, maxLength = 5)
     * // Returns: [1, 2, 9999, 9999, 9999]
     * ```
     */
    fun padSequence(
        sequence: IntArray,
        maxLength: Int = Constants.ML.MAX_SEQUENCE_LENGTH
    ): IntArray {
        require(isInitialized) { "InputPreprocessor not initialized" }

        return when {
            // Sequence is already the right length
            sequence.size == maxLength -> sequence

            // Sequence is too short - pad with padTokenId
            sequence.size < maxLength -> {
                IntArray(maxLength) { i ->
                    if (i < sequence.size) sequence[i] else padTokenId
                }
            }

            // Sequence is too long - truncate (take last N words)
            else -> sequence.takeLast(maxLength).toIntArray()
        }
    }

    /**
     * Prepare input buffer for TensorFlow Lite
     *
     * Converts IntArray to ByteBuffer in the format expected by the model.
     * TensorFlow Lite requires input as ByteBuffer with native byte order.
     *
     * @param paddedSequence Padded sequence of word IDs
     * @return ByteBuffer ready for inference
     *
     * Example:
     * ```kotlin
     * val sequence = intArrayOf(1, 2, 0)
     * val buffer = preprocessor.prepareInputBuffer(sequence)
     * interpreter.run(buffer, outputBuffer)
     * ```
     */
    fun prepareInputBuffer(paddedSequence: IntArray): ByteBuffer {
        require(isInitialized) { "InputPreprocessor not initialized" }

        // Allocate buffer (4 bytes per int)
        val buffer = ByteBuffer.allocateDirect(paddedSequence.size * 4)
        buffer.order(ByteOrder.nativeOrder())

        // Write each integer
        paddedSequence.forEach { id ->
            buffer.putInt(id)
        }

        // Rewind to beginning for reading
        buffer.rewind()

        return buffer
    }

    /**
     * Complete preprocessing pipeline
     *
     * Convenience method that does: encode → pad → convert to buffer
     *
     * @param words List of words (context)
     * @param maxLength Maximum sequence length
     * @return ByteBuffer ready for inference
     *
     * Example:
     * ```kotlin
     * val context = listOf("ನಾನು", "ಮನೆಗೆ", "ಹೋಗುತ್ತಿದ್ದೇನೆ")
     * val buffer = preprocessor.preprocessInput(context, maxLength = 3)
     * interpreter.run(buffer, outputBuffer)
     * ```
     */
    fun preprocessInput(
        words: List<String>,
        maxLength: Int = Constants.ML.MAX_CONTEXT_WORDS
    ): ByteBuffer {
        val encoded = encodeWords(words)
        val padded = padSequence(encoded, maxLength)
        return prepareInputBuffer(padded)
    }

    /**
     * Decode word ID back to word (for debugging)
     *
     * @param id Word ID
     * @return Word string, or "<UNK>" if not found
     */
    fun decodeWord(id: Int): String {
        return idToWord.getOrDefault(id, Constants.ML.UNKNOWN_TOKEN)
    }

    /**
     * Decode sequence of IDs back to words (for debugging)
     */
    fun decodeSequence(ids: IntArray): List<String> {
        return ids.map { decodeWord(it) }
    }

    /**
     * Check if word is in vocabulary
     */
    fun hasWord(word: String): Boolean {
        return wordToId.containsKey(word)
    }

    /**
     * Get vocabulary size
     */
    fun getVocabularySize(): Int {
        return wordToId.size
    }

    // ==================== Private Helper Functions ====================

    /**
     * Load vocabulary from JSON file
     *
     * Expected format:
     * {
     *   "word1": 1,
     *   "word2": 2,
     *   ...
     *   "<UNK>": 0,
     *   "<PAD>": 9999
     * }
     */
    private suspend fun loadVocabulary(): Result<Map<String, Int>> = withContext(Dispatchers.IO) {
        try {
            val vocabularyFile = Constants.ML.VOCABULARY_FILE

            context.assets.open(vocabularyFile).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    val json = reader.readText()

                    // Parse JSON
                    val gson = Gson()
                    val type = object : TypeToken<Map<String, Int>>() {}.type
                    val vocabulary: Map<String, Int> = gson.fromJson(json, type)

                    // Validate vocabulary
                    if (vocabulary.isEmpty()) {
                        return@withContext Result.Error(
                            Exception("Vocabulary is empty")
                        )
                    }

                    if (!vocabulary.containsKey(Constants.ML.UNKNOWN_TOKEN)) {
                        return@withContext Result.Error(
                            Exception("Vocabulary missing ${Constants.ML.UNKNOWN_TOKEN} token")
                        )
                    }

                    if (!vocabulary.containsKey(Constants.ML.PAD_TOKEN)) {
                        return@withContext Result.Error(
                            Exception("Vocabulary missing ${Constants.ML.PAD_TOKEN} token")
                        )
                    }

                    Result.Success(vocabulary)
                }
            }
        } catch (e: java.io.FileNotFoundException) {
            println("Vocabulary file not found: ${Constants.ML.VOCABULARY_FILE}")
            Result.Error(Exception("Vocabulary file not found", e))

        } catch (e: Exception) {
            println("Failed to load vocabulary: ${e.message}")
            e.printStackTrace()
            Result.Error(Exception("Failed to load vocabulary: ${e.message}", e))
        }
    }
}

/**
 * USAGE EXAMPLE:
 * ==============
 *
 * ```kotlin
 * val preprocessor = InputPreprocessor(context)
 *
 * // Initialize (load vocabulary)
 * val initResult = preprocessor.initialize()
 * if (initResult is Result.Error) {
 *     println("Failed to initialize: ${initResult.exception.message}")
 *     return
 * }
 *
 * // Encode words
 * val words = listOf("ನಮಸ್ತೆ", "ಕನ್ನಡ", "ಬೆಂಗಳೂರು")
 * val encoded = preprocessor.encodeWords(words)
 * println(encoded.contentToString())  // [1, 2, 3]
 *
 * // Pad sequence
 * val padded = preprocessor.padSequence(encoded, maxLength = 5)
 * println(padded.contentToString())  // [1, 2, 3, 9999, 9999]
 *
 * // Complete pipeline
 * val buffer = preprocessor.preprocessInput(words, maxLength = 3)
 * interpreter.run(buffer, outputBuffer)
 *
 * // Debugging: decode back to words
 * val decoded = preprocessor.decodeSequence(encoded)
 * println(decoded)  // [ನಮಸ್ತೆ, ಕನ್ನಡ, ಬೆಂಗಳೂರು]
 * ```
 *
 * VOCABULARY FILE FORMAT:
 * =======================
 *
 * File: app/src/main/assets/ml_models/vocabulary.json
 *
 * ```json
 * {
 *   "<UNK>": 0,
 *   "<PAD>": 49999,
 *   "<START>": 1,
 *   "<END>": 2,
 *   "ನಮಸ್ತೆ": 3,
 *   "ಕನ್ನಡ": 4,
 *   "ಬೆಂಗಳೂರು": 5,
 *   "the": 6,
 *   "be": 7,
 *   "to": 8,
 *   ...
 *   (50,000 total words)
 * }
 * ```
 *
 * HOW TO CREATE VOCABULARY:
 * ==========================
 *
 * During model training in Python:
 *
 * ```python
 * from tensorflow.keras.preprocessing.text import Tokenizer
 *
 * # Train tokenizer
 * tokenizer = Tokenizer(num_words=50000, oov_token="<UNK>")
 * tokenizer.fit_on_texts(training_sentences)
 *
 * # Add special tokens
 * tokenizer.word_index["<PAD>"] = 49999
 * tokenizer.word_index["<START>"] = 1
 * tokenizer.word_index["<END>"] = 2
 *
 * # Save as JSON
 * import json
 * with open('vocabulary.json', 'w', encoding='utf-8') as f:
 *     json.dump(tokenizer.word_index, f, ensure_ascii=False, indent=2)
 * ```
 *
 * IMPORTANT NOTES:
 * ================
 * 1. **Vocabulary Required**: Must create vocabulary.json during training
 *    - Same tokenizer must be used for training and inference
 *    - Vocabulary must match model's expected input
 *
 * 2. **UTF-8 Encoding**: Essential for Kannada text
 *    - Use charset="UTF-8" when reading JSON
 *    - Ensure JSON file is saved as UTF-8
 *
 * 3. **Special Tokens**: Must include:
 *    - <UNK>: For unknown words
 *    - <PAD>: For padding sequences
 *
 * 4. **Performance**:
 *    - Vocabulary loaded once at startup
 *    - Encoding is O(n) where n = number of words
 *    - Very fast: < 1ms for typical inputs
 */
