# 🤖 ML-BASED SUGGESTION ENGINE - IMPLEMENTATION PLAN

**Status:** Phase 1 & 2 Complete ✅ | Phase 3-5 Pending ⏳
**Branch:** `ui/keyboard-layout`
**Last Updated:** 2025-11-09

---

## 🎯 Project Goal

**Replace all hardcoded suggestion logic with an intelligent ML-based system that:**
- ✅ Loads dictionaries from configurable asset files (NO hardcoding)
- ✅ Uses constants for all parameters (NO magic numbers)
- ⏳ Predicts next words using TensorFlow Lite LSTM model
- ⏳ Corrects typos using Levenshtein distance algorithm
- ⏳ Provides phonetic transliteration (English → Kannada)
- ⏳ Learns from user typing patterns (privacy-safe)

---

## ✅ COMPLETED: Phase 1 & 2

### Phase 1: Configuration Infrastructure ✅

**Files Modified:**
1. `core/common/Constants.kt` (+170 lines)
   - Added `Constants.Dictionary` - 11 parameters
   - Added `Constants.ML` - 22 parameters
   - Added `Constants.Transliteration` - 8 parameters
   - Added `Constants.TypoCorrection` - 9 parameters
   - Updated `Constants.Suggestions` - 12 parameters

2. `features/suggestion-engine/build.gradle.kts`
   - Added TensorFlow Lite dependencies (3 libraries)
   - Added Gson for JSON parsing
   - Added testing dependencies

**Key Achievement**: Zero hardcoded values - all configuration in Constants.kt!

### Phase 2: Dictionary System ✅

**Files Created:**
1. `DictionaryLoader.kt` (250 lines)
   - Loads dictionaries from assets
   - Parses "word frequency" format
   - UTF-8 encoding for Kannada
   - Error handling with Result wrapper
   - Statistics tracking

2. Dictionary Assets (3 files):
   - `kannada_dictionary.txt` - 100+ words
   - `english_dictionary.txt` - 100+ words
   - `kannada_common_phrases.txt` - 40+ phrases

**Files Modified:**
1. `SuggestionEngine.kt`
   - Integrated DictionaryLoader
   - Removed hardcoded word lists
   - All scoring uses Constants
   - Phrase support added

**Key Achievement**: Dictionary-driven suggestions - expandable to 100k+ words!

---

## ⏳ PENDING: Phase 3-5

### Phase 3: ML Model Infrastructure (6-8 hours)

**Goal:** Create TensorFlow Lite integration for next-word prediction

**Files to Create:**

1. **`features/suggestion-engine/ml/MLPredictor.kt`** (Main ML Engine)
```kotlin
class MLPredictor(private val context: Context) {
    fun initialize()  // Load model from assets
    suspend fun predict(context: List<String>): Result<List<Prediction>>
    fun release()     // Cleanup resources
}
```

2. **`features/suggestion-engine/ml/ModelLoader.kt`**
```kotlin
object ModelLoader {
    fun loadModel(context: Context, path: String): Interpreter
    fun getModelMetadata(context: Context): ModelMetadata
}
```

3. **`features/suggestion-engine/ml/InputPreprocessor.kt`**
```kotlin
class InputPreprocessor {
    fun encodeWords(words: List<String>): FloatArray
    fun padSequence(sequence: FloatArray): FloatArray
}
```

4. **`features/suggestion-engine/ml/OutputPostprocessor.kt`**
```kotlin
class OutputPostprocessor {
    fun decodeOutput(tensor: FloatArray): List<Prediction>
    fun filterByConfidence(predictions: List<Prediction>): List<Prediction>
}
```

5. **`features/suggestion-engine/ml/ContextManager.kt`**
```kotlin
class ContextManager {
    fun trackTypingContext(word: String)
    fun getContext(maxWords: Int): List<String>
    fun clearContext()
}
```

6. **`features/suggestion-engine/models/PredictionResult.kt`**
```kotlin
data class PredictionResult(
    val word: String,
    val confidence: Float,
    val contextUsed: List<String>
)
```

**Model File Needed:**
- Train or obtain a Kannada LSTM model
- Convert to `.tflite` format
- Place in `app/src/main/assets/ml_models/kannada_next_word_v1.tflite`

**Model Architecture Recommendation:**
```
Input Layer: Previous 3 words → [batch_size, sequence_length, vocab_size]
Embedding: 300 dimensions
LSTM Layer 1: 128 units, return sequences
LSTM Layer 2: 128 units
Dense Layer: vocab_size outputs
Softmax: Probability distribution
Output: Top 10 predictions
```

**Training Data Sources:**
- Kannada Wikipedia corpus
- Kannada news articles
- User-generated content (with permission)
- Kannada literature (public domain)

---

### Phase 4: Integration with SuggestionEngine (3-4 hours)

**Goal:** Combine ML predictions with existing Trie-based suggestions

**Modify `SuggestionEngine.kt`:**

```kotlin
class SuggestionEngine(private val context: Context) {
    // ... existing code ...

    private val mlPredictor = MLPredictor(context)  // ADD THIS
    private val contextManager = ContextManager()   // ADD THIS

    override fun initialize() {
        scope.launch {
            // ... existing initialization ...
            mlPredictor.initialize()  // ADD THIS
        }
    }

    suspend fun getSuggestions(...): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()

        // 1. User history (existing)
        suggestions.addAll(getUserHistorySuggestions(currentWord))

        // 2. Dictionary (existing)
        suggestions.addAll(getDictionarySuggestions(currentWord, language))

        // 3. ML predictions (NEW!)
        if (currentWord.isNotEmpty()) {
            val context = contextManager.getContext(Constants.ML.MAX_CONTEXT_WORDS)
            val mlResult = mlPredictor.predict(context)

            if (mlResult is Result.Success) {
                val mlSuggestions = mlResult.data.map { prediction ->
                    Suggestion(
                        word = prediction.word,
                        confidence = prediction.confidence,
                        source = SuggestionSource.PREDICTION,
                        frequency = 0
                    )
                }
                suggestions.addAll(mlSuggestions)
            }
        }

        // Combine, rank, and return top N
        return suggestions
            .sortedByDescending { it.confidence }
            .take(Constants.Suggestions.MAX_SUGGESTIONS)
    }

    fun onWordTyped(word: String) {
        contextManager.trackTypingContext(word)  // ADD THIS
        // ... existing code ...
    }
}
```

**Update Flow:**
```
User types "I am going to"
    ↓
1. Trie lookup for partial word → ["to", "today", "tomorrow"]
2. User history → ["town", "tokyo"]
3. ML prediction with context ["I", "am", "going"] → ["the", "be", "have"]
    ↓
Combine all sources
    ↓
Rank by confidence
    ↓
Return top 5: ["the", "town", "be", "today", "tomorrow"]
```

---

### Phase 5: Advanced Features (8-10 hours)

#### 5.1 Transliteration Engine (4-5 hours)

**Files to Create:**

1. **`transliteration/TransliterationEngine.kt`**
```kotlin
class TransliterationEngine(context: Context) {
    fun transliterate(englishText: String): String
    fun getRules(): Map<String, String>
}
```

2. **`transliteration/KannadaPhoneticRules.kt`**
```kotlin
object KannadaPhoneticRules {
    val VOWELS: Map<String, String>
    val CONSONANTS: Map<String, String>
    val CONJUNCTS: Map<String, String>
}
```

3. **Asset: `transliteration/phonetic_rules.json`**
```json
{
  "vowels": {
    "a": "ಅ", "aa": "ಆ", "i": "ಇ", "ii": "ಈ",
    "u": "ಉ", "uu": "ಊ", "e": "ಎ", "ee": "ಏ",
    "o": "ಒ", "oo": "ಓ"
  },
  "consonants": {
    "ka": "ಕ", "kha": "ಖ", "ga": "ಗ", "gha": "ಘ",
    "cha": "ಚ", "ja": "ಜ", "ta": "ತ", "da": "ದ",
    "na": "ನ", "pa": "ಪ", "ba": "ಬ", "ma": "ಮ",
    "ya": "ಯ", "ra": "ರ", "la": "ಲ", "va": "ವ",
    "sha": "ಶ", "sa": "ಸ", "ha": "ಹ"
  },
  "special_cases": {
    "namaste": "ನಮಸ್ತೆ",
    "kannada": "ಕನ್ನಡ",
    "bengaluru": "ಬೆಂಗಳೂರು"
  }
}
```

**Integration:**
```kotlin
// In SuggestionEngine
if (layout == "phonetic" && currentWord.isKannada().not()) {
    val transliterated = transliterationEngine.transliterate(currentWord)
    suggestions.add(Suggestion(
        word = transliterated,
        confidence = 0.8f,
        source = SuggestionSource.TRANSLITERATION
    ))
}
```

#### 5.2 Typo Correction (4-5 hours)

**Files to Create:**

1. **`correction/TypoCorrector.kt`**
```kotlin
class TypoCorrector(
    private val kannadaTrie: Trie,
    private val englishTrie: Trie
) {
    fun findCorrections(
        misspelled: String,
        language: String
    ): List<Correction>
}
```

2. **`correction/LevenshteinDistance.kt`**
```kotlin
object LevenshteinDistance {
    fun calculate(s1: String, s2: String): Int
    fun calculateNormalized(s1: String, s2: String): Float
}
```

**Algorithm:**
```kotlin
fun findCorrections(word: String): List<Correction> {
    val trie = selectTrie(language)
    val candidates = mutableListOf<Correction>()

    // Find all words within edit distance 2
    trie.getAllWords().forEach { dictionaryWord ->
        val distance = LevenshteinDistance.calculate(word, dictionaryWord)

        if (distance <= Constants.TypoCorrection.MAX_EDIT_DISTANCE) {
            val confidence = 1.0f - (distance.toFloat() / word.length)

            if (confidence >= Constants.TypoCorrection.CORRECTION_CONFIDENCE_THRESHOLD) {
                candidates.add(Correction(
                    original = word,
                    correction = dictionaryWord,
                    editDistance = distance,
                    confidence = confidence
                ))
            }
        }
    }

    return candidates
        .sortedByDescending { it.confidence }
        .take(Constants.TypoCorrection.MAX_CORRECTION_CANDIDATES)
}
```

---

## 📊 Implementation Statistics

### Completed:
- **Constants Added:** 50+ parameters
- **Hardcoded Values Removed:** 20+ magic numbers
- **Dictionary Words:** 200+ (expandable to 100k+)
- **Files Created:** 4 files
- **Lines Added:** ~700 lines

### Remaining:
- **ML Files to Create:** 6 files (~800 lines)
- **Transliteration Files:** 3 files (~400 lines)
- **Typo Correction Files:** 2 files (~300 lines)
- **Total Remaining:** ~1,500 lines of code

### Timeline:
- **Phase 1-2:** ✅ Complete (4 hours)
- **Phase 3:** ⏳ Pending (6-8 hours)
- **Phase 4:** ⏳ Pending (3-4 hours)
- **Phase 5:** ⏳ Pending (8-10 hours)
- **Total:** 21-26 hours

---

## 🔧 Testing Plan

### Unit Tests to Write:

1. **DictionaryLoader Tests:**
```kotlin
@Test fun loadDictionary_validFile_returnsWords()
@Test fun loadDictionary_invalidFile_returnsError()
@Test fun loadDictionary_withComments_skipsComments()
@Test fun getDictionaryStats_validWords_returnsCorrectStats()
```

2. **MLPredictor Tests:**
```kotlin
@Test fun predict_validContext_returnsPredictions()
@Test fun predict_emptyContext_returnsError()
@Test fun predict_invalidModel_returnsError()
```

3. **TransliterationEngine Tests:**
```kotlin
@Test fun transliterate_namaste_returnsKannada()
@Test fun transliterate_multipleWords_returnsCorrect()
```

4. **TypoCorrector Tests:**
```kotlin
@Test fun findCorrections_singleTypo_returnsCorrection()
@Test fun findCorrections_multipleTypos_rankedByConfidence()
```

### Integration Tests:
```kotlin
@Test fun suggestionEngine_withML_returnsRankedSuggestions()
@Test fun suggestionEngine_transliteration_worksInPhoneticLayout()
@Test fun suggestionEngine_typoCorrection_suggestsCorrectWord()
```

---

## 📝 Current Status Summary

### ✅ What's Working:
- Zero hardcoding - all configuration-driven
- Dictionary loading from assets
- Configurable scoring weights
- Trie-based autocomplete
- User history tracking
- Multi-word phrase support

### ⏳ What's Pending:
- TensorFlow Lite model integration
- Next-word prediction with context
- Phonetic transliteration (English → Kannada)
- Typo correction with Levenshtein distance
- ML model training/acquisition

### 🎯 Next Immediate Steps:
1. Create ML infrastructure skeleton (6 files)
2. Train or obtain Kannada LSTM model
3. Integrate ML predictions with existing flow
4. Add transliteration for phonetic layout
5. Implement typo correction
6. Write comprehensive tests

---

## 🚀 How to Continue

### Option A: Train Custom Model
1. Collect Kannada corpus (Wikipedia, news, literature)
2. Preprocess and tokenize text
3. Train LSTM model in Python (TensorFlow/Keras)
4. Convert to TensorFlow Lite (.tflite)
5. Place in `app/src/main/assets/ml_models/`

### Option B: Use Pre-trained Model
1. Search for existing Kannada language models
2. Convert to TensorFlow Lite if needed
3. Test and validate predictions
4. Integrate with MLPredictor

### Option C: Hybrid Approach (Recommended)
1. Start with rule-based transliteration (Phase 5.1)
2. Add typo correction (Phase 5.2)
3. Meanwhile, train ML model in parallel
4. Integrate ML when ready (Phase 3-4)

---

**Status:** 🟢 On Track
**Architecture:** ✅ Clean, Non-hardcoded, Testable
**Next Milestone:** ML Model Integration

*Last Updated: 2025-11-09 by Claude Code*
