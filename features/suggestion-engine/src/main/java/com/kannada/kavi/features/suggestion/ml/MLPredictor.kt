package com.kannada.kavi.features.suggestion.ml

import android.content.Context
import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.core.common.Result
import com.kannada.kavi.features.suggestion.models.Prediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer

/**
 * MLPredictor - Machine Learning Prediction Engine
 *
 * This is the brain that uses TensorFlow Lite to predict what word you'll type next!
 * It's like having a crystal ball for your keyboard.
 *
 * WHAT DOES IT DO?
 * ================
 * - You type: "I am going to"
 * - MLPredictor looks at context: ["I", "am", "going"]
 * - Predicts next word: ["the", "be", "have", "sleep", "work"]
 * - Each prediction has a confidence score (0-1)
 *
 * HOW IT WORKS:
 * =============
 * 1. Load TensorFlow Lite model from assets (.tflite file)
 * 2. User types words → ContextManager tracks last 3 words
 * 3. Convert words to numbers (encoding) → InputPreprocessor
 * 4. Feed numbers into neural network → TensorFlow Lite
 * 5. Network outputs probabilities for 50,000 words
 * 6. Pick top 10 predictions → OutputPostprocessor
 * 7. Return as Suggestion objects
 *
 * MODEL ARCHITECTURE:
 * ===================
 * Input: [batch_size=1, sequence_length=3, vocab_size=50000]
 *   ↓
 * Embedding Layer (300 dimensions)
 *   ↓
 * LSTM Layer 1 (128 units) → remembers context
 *   ↓
 * LSTM Layer 2 (128 units) → deeper understanding
 *   ↓
 * Dense Layer (50000 outputs)
 *   ↓
 * Softmax → probabilities sum to 1.0
 *   ↓
 * Output: [word1: 0.45, word2: 0.23, word3: 0.15, ...]
 *
 * EXAMPLE:
 * ========
 * Input context: ["ನಾನು", "ಮನೆಗೆ", "ಹೋಗುತ್ತಿದ್ದೇನೆ"]
 * Predictions:
 *   1. "ನಿನ್ನೆ" (confidence: 0.78) - "I am going home yesterday" ✗ (doesn't make sense)
 *   2. "ಈಗ" (confidence: 0.65) - "I am going home now" ✓
 *   3. "ನಾಳೆ" (confidence: 0.52) - "I am going home tomorrow" ✓
 *
 * PERFORMANCE:
 * ============
 * - Inference time: < 50ms (configured via Constants.ML.INFERENCE_TIMEOUT_MS)
 * - Memory: ~20MB for model
 * - Accuracy: ~75% for top-3 predictions (depends on training data)
 * - GPU acceleration: Optional (configured via Constants.ML.USE_GPU_ACCELERATION)
 *
 * CONFIGURATION:
 * ==============
 * ALL parameters come from Constants.ML - NO hardcoding!
 * - Model path: Constants.ML.NEXT_WORD_MODEL
 * - Max context words: Constants.ML.MAX_CONTEXT_WORDS
 * - Min confidence: Constants.ML.MIN_PREDICTION_CONFIDENCE
 * - Timeout: Constants.ML.INFERENCE_TIMEOUT_MS
 * - GPU enabled: Constants.ML.USE_GPU_ACCELERATION
 * - Threads: Constants.ML.NUM_THREADS
 */
class MLPredictor(private val context: Context) {

    // TensorFlow Lite interpreter (the ML brain)
    private var interpreter: Interpreter? = null

    // Helper classes for data processing
    private lateinit var inputPreprocessor: InputPreprocessor
    private lateinit var outputPostprocessor: OutputPostprocessor

    // Is the predictor ready to use?
    @Volatile
    private var isInitialized = false

    /**
     * Initialize the ML predictor
     *
     * Loads the TensorFlow Lite model from assets.
     * Call this once during app startup (background thread).
     *
     * @return Result.Success if initialized, Result.Error if failed
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Load the model
            val modelResult = ModelLoader.loadModel(context, Constants.ML.NEXT_WORD_MODEL)

            when (modelResult) {
                is Result.Success -> {
                    interpreter = modelResult.data

                    // Initialize preprocessor and postprocessor
                    inputPreprocessor = InputPreprocessor(context)
                    outputPostprocessor = OutputPostprocessor(context)

                    // Initialize their vocabularies
                    val preprocessorInit = inputPreprocessor.initialize()
                    if (preprocessorInit is Result.Error) {
                        return@withContext Result.Error(preprocessorInit.exception)
                    }

                    val postprocessorInit = outputPostprocessor.initialize()
                    if (postprocessorInit is Result.Error) {
                        return@withContext Result.Error(postprocessorInit.exception)
                    }

                    isInitialized = true

                    println("MLPredictor initialized successfully")
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    println("Failed to load ML model: ${modelResult.exception.message}")
                    Result.Error(modelResult.exception)
                }
            }
        } catch (e: Exception) {
            println("MLPredictor initialization failed: ${e.message}")
            e.printStackTrace()
            Result.Error(Exception("Failed to initialize MLPredictor: ${e.message}", e))
        }
    }

    /**
     * Predict next words based on typing context
     *
     * This is the MAIN function called to get ML predictions.
     *
     * @param context List of previous words (e.g., ["I", "am", "going"])
     *                Maximum length = Constants.ML.MAX_CONTEXT_WORDS
     * @return Result.Success with list of predictions, or Result.Error
     *
     * Example:
     * ```kotlin
     * val predictions = mlPredictor.predict(listOf("ನಾನು", "ಮನೆಗೆ", "ಹೋಗುತ್ತಿದ್ದೇನೆ"))
     * // Returns: [
     * //   Prediction("ಈಗ", 0.65, ["ನಾನು", "ಮನೆಗೆ", "ಹೋಗುತ್ತಿದ್ದೇನೆ"]),
     * //   Prediction("ನಾಳೆ", 0.52, ["ನಾನು", "ಮನೆಗೆ", "ಹೋಗುತ್ತಿದ್ದೇನೆ"]),
     * //   ...
     * // ]
     * ```
     */
    suspend fun predict(context: List<String>): Result<List<Prediction>> = withContext(Dispatchers.Default) {
        try {
            // Validate state
            if (!isInitialized || interpreter == null) {
                return@withContext Result.Error(
                    Exception("MLPredictor not initialized. Call initialize() first.")
                )
            }

            // Validate input
            if (context.isEmpty()) {
                return@withContext Result.Error(
                    Exception("Context cannot be empty")
                )
            }

            if (context.size > Constants.ML.MAX_CONTEXT_WORDS) {
                return@withContext Result.Error(
                    Exception("Context exceeds max size: ${context.size} > ${Constants.ML.MAX_CONTEXT_WORDS}")
                )
            }

            // Run inference with timeout
            val predictions = withTimeout(Constants.ML.INFERENCE_TIMEOUT_MS) {
                runInference(context)
            }

            Result.Success(predictions)

        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            println("ML prediction timed out after ${Constants.ML.INFERENCE_TIMEOUT_MS}ms")
            Result.Error(Exception("Prediction timeout", e))
        } catch (e: Exception) {
            println("ML prediction failed: ${e.message}")
            e.printStackTrace()
            Result.Error(Exception("Prediction failed: ${e.message}", e))
        }
    }

    /**
     * Release resources
     *
     * Call this when the keyboard is destroyed.
     * Frees up ~20MB of memory.
     */
    fun release() {
        try {
            interpreter?.close()
            interpreter = null
            isInitialized = false
            println("MLPredictor released")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Check if predictor is ready
     */
    fun isReady(): Boolean = isInitialized && interpreter != null

    // ==================== Private Helper Functions ====================

    /**
     * Run the actual neural network inference
     *
     * Steps:
     * 1. Convert words to numbers (encoding)
     * 2. Pad sequence to fixed length
     * 3. Feed into TensorFlow Lite
     * 4. Get output tensor
     * 5. Convert numbers back to words (decoding)
     * 6. Filter by confidence threshold
     * 7. Return top N predictions
     */
    private suspend fun runInference(context: List<String>): List<Prediction> {
        // Step 1: Encode context words to numbers
        val encodedContext = inputPreprocessor.encodeWords(context)

        // Step 2: Pad sequence to fixed length (e.g., [1, 3, 50000])
        val paddedInput = inputPreprocessor.padSequence(
            encodedContext,
            maxLength = Constants.ML.MAX_CONTEXT_WORDS
        )

        // Step 3: Prepare input buffer for TensorFlow Lite
        val inputBuffer = inputPreprocessor.prepareInputBuffer(paddedInput)

        // Step 4: Prepare output buffer (will hold probabilities for all 50k words)
        val outputBuffer = ByteBuffer.allocateDirect(
            Constants.ML.VOCABULARY_SIZE * 4  // 4 bytes per float
        ).apply {
            order(java.nio.ByteOrder.nativeOrder())
        }

        // Step 5: Run inference (the magic happens here!)
        interpreter?.run(inputBuffer, outputBuffer)

        // Step 6: Decode output tensor to predictions
        val allPredictions = outputPostprocessor.decodeOutput(
            outputBuffer,
            contextUsed = context
        )

        // Step 7: Filter by confidence threshold
        val filteredPredictions = outputPostprocessor.filterByConfidence(
            allPredictions,
            minConfidence = Constants.ML.MIN_PREDICTION_CONFIDENCE
        )

        // Step 8: Return top N predictions
        return filteredPredictions.take(Constants.ML.MAX_PREDICTIONS)
    }
}

/**
 * USAGE EXAMPLE:
 * ==============
 *
 * In SuggestionEngine:
 *
 * ```kotlin
 * private val mlPredictor = MLPredictor(context)
 * private val contextManager = ContextManager()
 *
 * override fun initialize() {
 *     scope.launch {
 *         // ... existing initialization ...
 *         mlPredictor.initialize()
 *     }
 * }
 *
 * suspend fun getSuggestions(...): List<Suggestion> {
 *     val suggestions = mutableListOf<Suggestion>()
 *
 *     // 1. Trie-based suggestions (existing)
 *     suggestions.addAll(getDictionarySuggestions(currentWord))
 *
 *     // 2. ML-based predictions (NEW!)
 *     if (mlPredictor.isReady()) {
 *         val context = contextManager.getContext(Constants.ML.MAX_CONTEXT_WORDS)
 *         val mlResult = mlPredictor.predict(context)
 *
 *         if (mlResult is Result.Success) {
 *             val mlSuggestions = mlResult.data.map { prediction ->
 *                 Suggestion(
 *                     word = prediction.word,
 *                     confidence = prediction.confidence,
 *                     source = SuggestionSource.PREDICTION,
 *                     frequency = 0
 *                 )
 *             }
 *             suggestions.addAll(mlSuggestions)
 *         }
 *     }
 *
 *     return suggestions
 *         .sortedByDescending { it.confidence }
 *         .take(Constants.Suggestions.MAX_SUGGESTIONS)
 * }
 *
 * fun onWordTyped(word: String) {
 *     contextManager.trackTypingContext(word)  // Track for ML
 *     // ... existing code ...
 * }
 *
 * override fun release() {
 *     mlPredictor.release()
 * }
 * ```
 *
 * IMPORTANT NOTES:
 * ================
 * 1. **Model Required**: This code requires a .tflite model file.
 *    - Train your own LSTM model in Python (TensorFlow/Keras)
 *    - Convert to TensorFlow Lite format
 *    - Place at: app/src/main/assets/ml_models/kannada_next_word_v1.tflite
 *
 * 2. **Vocabulary Required**: Model needs a vocabulary file (word → number mapping)
 *    - Create during training: vocabulary.json
 *    - Place at: app/src/main/assets/ml_models/vocabulary.json
 *
 * 3. **Performance**: Test on real device for accurate timing
 *    - Emulator is 5-10x slower than real hardware
 *    - GPU acceleration helps on high-end devices
 *
 * 4. **Error Handling**: Gracefully degrades if model fails to load
 *    - Keyboard still works with Trie-based suggestions
 *    - ML is an enhancement, not a requirement
 */
