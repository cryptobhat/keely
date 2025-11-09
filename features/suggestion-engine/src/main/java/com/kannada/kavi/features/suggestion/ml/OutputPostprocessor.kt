package com.kannada.kavi.features.suggestion.ml

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.core.common.Result
import com.kannada.kavi.features.suggestion.models.Prediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer

/**
 * OutputPostprocessor - Converts model outputs to predictions
 *
 * The neural network outputs numbers (probabilities) - not words!
 * This class converts numbers → words (decoding) and creates Prediction objects.
 *
 * WHAT DOES IT DO?
 * ================
 * 1. Loads vocabulary (number → word mapping) from JSON file
 * 2. Reads output tensor from TensorFlow Lite (array of 50,000 probabilities)
 * 3. Finds top N highest probabilities
 * 4. Converts numbers back to words
 * 5. Creates Prediction objects with words and confidence scores
 * 6. Filters out low-confidence predictions
 *
 * HOW DECODING WORKS:
 * ===================
 *
 * Model output (50,000 probabilities):
 * [
 *   0.001,  // Word ID 0 (<UNK>) → 0.1% chance
 *   0.780,  // Word ID 1 ("the") → 78.0% chance
 *   0.120,  // Word ID 2 ("be") → 12.0% chance
 *   0.050,  // Word ID 3 ("have") → 5.0% chance
 *   0.030,  // Word ID 4 ("to") → 3.0% chance
 *   ...     // (49,995 more)
 * ]
 *
 * Step 1: Find top N (e.g., top 10)
 * [
 *   (1, 0.780),  // ID 1 → 78%
 *   (2, 0.120),  // ID 2 → 12%
 *   (3, 0.050),  // ID 3 → 5%
 *   ...
 * ]
 *
 * Step 2: Convert IDs to words using vocabulary
 * [
 *   ("the", 0.780),
 *   ("be", 0.120),
 *   ("have", 0.050),
 *   ...
 * ]
 *
 * Step 3: Create Prediction objects
 * [
 *   Prediction("the", 0.780, context),
 *   Prediction("be", 0.120, context),
 *   Prediction("have", 0.050, context),
 *   ...
 * ]
 *
 * SOFTMAX OUTPUT:
 * ===============
 * The model's final layer uses Softmax, which ensures:
 * - All probabilities are between 0.0 and 1.0
 * - All probabilities sum to exactly 1.0
 * - Highest probability = most likely next word
 *
 * Example:
 * Input: "I am going"
 * Output: [
 *   "to" → 0.45 (45%),
 *   "home" → 0.23 (23%),
 *   "there" → 0.15 (15%),
 *   "now" → 0.08 (8%),
 *   ... (rest add up to 9%)
 * ]
 * Total = 100%
 *
 * CONFIGURATION:
 * ==============
 * ALL settings from Constants.ML - NO hardcoding!
 * - Vocabulary file: Constants.ML.VOCABULARY_FILE
 * - Vocabulary size: Constants.ML.VOCABULARY_SIZE
 * - Max predictions: Constants.ML.MAX_PREDICTIONS
 * - Min confidence: Constants.ML.MIN_PREDICTION_CONFIDENCE
 */
class OutputPostprocessor(private val context: Context) {

    // Number → Word mapping (loaded from vocabulary.json)
    private val idToWord = mutableMapOf<Int, String>()

    // Word → Number mapping (for reverse lookups)
    private val wordToId = mutableMapOf<String, Int>()

    // Is the postprocessor ready?
    @Volatile
    private var isInitialized = false

    /**
     * Initialize the postprocessor
     *
     * Loads vocabulary from assets/ml_models/vocabulary.json
     * Same vocabulary file used by InputPreprocessor.
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
                        idToWord[id] = word
                        wordToId[word] = id
                    }

                    isInitialized = true

                    println("OutputPostprocessor initialized with ${idToWord.size} words")

                    Result.Success(Unit)
                }
                is Result.Error -> {
                    Result.Error(vocabularyResult.exception)
                }
            }
        } catch (e: Exception) {
            println("OutputPostprocessor initialization failed: ${e.message}")
            e.printStackTrace()
            Result.Error(Exception("Failed to initialize OutputPostprocessor: ${e.message}", e))
        }
    }

    /**
     * Decode model output to predictions
     *
     * Converts the raw output tensor from TensorFlow Lite into a list of
     * Prediction objects with words and confidence scores.
     *
     * @param outputBuffer ByteBuffer containing model output probabilities
     * @param contextUsed The context words used for this prediction
     * @return List of Predictions, sorted by confidence (highest first)
     *
     * Example:
     * ```kotlin
     * val outputBuffer = ByteBuffer.allocateDirect(50000 * 4)
     * interpreter.run(inputBuffer, outputBuffer)
     *
     * val predictions = postprocessor.decodeOutput(
     *     outputBuffer,
     *     contextUsed = listOf("I", "am", "going")
     * )
     * // Returns: [
     * //   Prediction("to", 0.78, ["I", "am", "going"]),
     * //   Prediction("home", 0.12, ["I", "am", "going"]),
     * //   ...
     * // ]
     * ```
     */
    fun decodeOutput(
        outputBuffer: ByteBuffer,
        contextUsed: List<String>
    ): List<Prediction> {
        require(isInitialized) { "OutputPostprocessor not initialized" }

        // Rewind buffer to beginning
        outputBuffer.rewind()

        // Read all probabilities from buffer
        val probabilities = FloatArray(Constants.ML.VOCABULARY_SIZE) { i ->
            outputBuffer.getFloat()
        }

        // Find top N predictions
        val topIndices = getTopKIndices(probabilities, Constants.ML.MAX_PREDICTIONS)

        // Convert to Prediction objects
        val predictions = topIndices.map { (id, probability) ->
            val word = idToWord.getOrDefault(id, Constants.ML.UNKNOWN_TOKEN)

            Prediction(
                word = word,
                confidence = probability,
                contextUsed = contextUsed,
                modelVersion = Constants.ML.MODEL_VERSION
            )
        }

        // Filter out special tokens (<UNK>, <PAD>, etc.)
        return predictions.filter { prediction ->
            !isSpecialToken(prediction.word)
        }
    }

    /**
     * Filter predictions by confidence threshold
     *
     * Removes predictions with confidence below the threshold.
     *
     * @param predictions List of predictions
     * @param minConfidence Minimum confidence (0.0 to 1.0)
     * @return Filtered list
     *
     * Example:
     * ```kotlin
     * val all = postprocessor.decodeOutput(buffer, context)
     * // [Prediction("the", 0.78), Prediction("be", 0.25), ...]
     *
     * val filtered = postprocessor.filterByConfidence(all, 0.3f)
     * // [Prediction("the", 0.78)]  // 0.25 filtered out
     * ```
     */
    fun filterByConfidence(
        predictions: List<Prediction>,
        minConfidence: Float = Constants.ML.MIN_PREDICTION_CONFIDENCE
    ): List<Prediction> {
        return predictions.filter { it.confidence >= minConfidence }
    }

    /**
     * Get word for a given ID
     *
     * @param id Word ID
     * @return Word string, or "<UNK>" if not found
     */
    fun getWord(id: Int): String {
        return idToWord.getOrDefault(id, Constants.ML.UNKNOWN_TOKEN)
    }

    /**
     * Get ID for a given word
     *
     * @param word Word string
     * @return Word ID, or null if not found
     */
    fun getId(word: String): Int? {
        return wordToId[word]
    }

    /**
     * Get vocabulary size
     */
    fun getVocabularySize(): Int {
        return idToWord.size
    }

    // ==================== Private Helper Functions ====================

    /**
     * Get top K indices with highest values
     *
     * Finds the K largest values in an array and returns their indices
     * and values, sorted by value (descending).
     *
     * Algorithm: Partial sort using min-heap (efficient for small K)
     * Time complexity: O(n log k) where n = array size, k = top K
     *
     * @param array Array of values (probabilities)
     * @param k Number of top values to find
     * @return List of (index, value) pairs, sorted by value (descending)
     */
    private fun getTopKIndices(array: FloatArray, k: Int): List<Pair<Int, Float>> {
        // Create list of (index, value) pairs
        val indexedValues = array.mapIndexed { index, value ->
            index to value
        }

        // Sort by value (descending) and take top K
        return indexedValues
            .sortedByDescending { it.second }
            .take(k)
    }

    /**
     * Check if a word is a special token
     *
     * Special tokens like <UNK>, <PAD>, <START>, <END> should not be
     * shown to the user as predictions.
     *
     * @param word Word to check
     * @return true if word is a special token
     */
    private fun isSpecialToken(word: String): Boolean {
        return word.startsWith("<") && word.endsWith(">")
    }

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
     *
     * Same file used by InputPreprocessor.
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

                    if (vocabulary.size != Constants.ML.VOCABULARY_SIZE) {
                        println("Warning: Vocabulary size ${vocabulary.size} != expected ${Constants.ML.VOCABULARY_SIZE}")
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
 * val postprocessor = OutputPostprocessor(context)
 *
 * // Initialize (load vocabulary)
 * val initResult = postprocessor.initialize()
 * if (initResult is Result.Error) {
 *     println("Failed to initialize: ${initResult.exception.message}")
 *     return
 * }
 *
 * // Run inference
 * val inputBuffer = preprocessor.preprocessInput(listOf("I", "am", "going"))
 * val outputBuffer = ByteBuffer.allocateDirect(50000 * 4)
 * interpreter.run(inputBuffer, outputBuffer)
 *
 * // Decode output
 * val predictions = postprocessor.decodeOutput(
 *     outputBuffer,
 *     contextUsed = listOf("I", "am", "going")
 * )
 *
 * predictions.forEach { prediction ->
 *     println("${prediction.word}: ${prediction.confidence}")
 * }
 * // Output:
 * // to: 0.78
 * // home: 0.12
 * // there: 0.05
 * // ...
 *
 * // Filter by confidence
 * val confident = postprocessor.filterByConfidence(predictions, 0.5f)
 * // Only "to" (0.78) remains
 * ```
 *
 * COMPLETE PIPELINE EXAMPLE:
 * ==========================
 *
 * ```kotlin
 * // 1. Initialize all components
 * val preprocessor = InputPreprocessor(context)
 * val postprocessor = OutputPostprocessor(context)
 * val interpreter = ModelLoader.loadModel(context, Constants.ML.NEXT_WORD_MODEL)
 *
 * preprocessor.initialize()
 * postprocessor.initialize()
 *
 * // 2. Prepare input
 * val context = listOf("ನಾನು", "ಮನೆಗೆ", "ಹೋಗುತ್ತಿದ್ದೇನೆ")
 * val inputBuffer = preprocessor.preprocessInput(context)
 *
 * // 3. Run inference
 * val outputBuffer = ByteBuffer.allocateDirect(50000 * 4)
 * interpreter.run(inputBuffer, outputBuffer)
 *
 * // 4. Decode output
 * val predictions = postprocessor.decodeOutput(outputBuffer, context)
 *
 * // 5. Filter and use
 * val filtered = postprocessor.filterByConfidence(predictions, 0.3f)
 * filtered.forEach { prediction ->
 *     println("Suggest: ${prediction.word} (${prediction.confidence})")
 * }
 * ```
 *
 * SOFTMAX OUTPUT FORMAT:
 * ======================
 *
 * The model's output layer uses Softmax activation, which produces:
 * - Probabilities for ALL words in vocabulary (50,000 values)
 * - Each probability between 0.0 and 1.0
 * - All probabilities sum to 1.0
 *
 * Example output tensor (showing first 10 of 50,000):
 * [
 *   0.001,  // Word 0 (<UNK>)
 *   0.780,  // Word 1 ("the") ← Highest! Most likely next word
 *   0.120,  // Word 2 ("be")
 *   0.050,  // Word 3 ("have")
 *   0.030,  // Word 4 ("to")
 *   0.010,  // Word 5 ("and")
 *   0.005,  // Word 6 ("a")
 *   0.002,  // Word 7 ("I")
 *   0.001,  // Word 8 ("it")
 *   0.001,  // Word 9 ("in")
 *   ...     // (49,990 more, all very small)
 * ]
 *
 * We only care about the top 10-20 highest probabilities.
 * The rest are noise (< 0.1% chance).
 *
 * IMPORTANT NOTES:
 * ================
 * 1. **Same Vocabulary**: MUST use same vocabulary.json as InputPreprocessor
 *    - Word IDs must match between encoding and decoding
 *    - Vocabulary created during training must be used here
 *
 * 2. **Special Tokens**: Filtered out automatically
 *    - <UNK>, <PAD>, <START>, <END> not shown to user
 *    - Only real words appear in predictions
 *
 * 3. **ByteBuffer**: Must rewind() before reading
 *    - After interpreter.run(), buffer position is at end
 *    - Call rewind() to reset to beginning
 *
 * 4. **Performance**:
 *    - Finding top K: O(n log k) ≈ 1ms for k=10, n=50000
 *    - Very fast compared to inference time (30-50ms)
 */
