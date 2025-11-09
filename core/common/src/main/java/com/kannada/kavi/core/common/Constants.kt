package com.kannada.kavi.core.common

/**
 * Constants - All Fixed Values Used in the App
 *
 * Think of constants as rules that never change. Like:
 * - The maximum speed limit on a road
 * - The number of players in a cricket team (always 11)
 * - The price of your favorite candy (though this can change in real life!)
 *
 * By putting all these fixed values in one place, we can easily find and update them.
 * If we hardcode values everywhere, updating becomes a nightmare!
 */

object Constants {

    /**
     * Database Constants
     * These define how we store data in the phone's local database
     */
    object Database {
        const val DATABASE_NAME = "kavi_keyboard.db"
        const val DATABASE_VERSION = 1

        // Table names - like different notebooks for different subjects
        const val TABLE_USER_HISTORY = "user_history"
        const val TABLE_CLIPBOARD = "clipboard_history"
        const val TABLE_CUSTOM_WORDS = "custom_words"
        const val TABLE_THEMES = "themes"
    }

    /**
     * Keyboard Layout Constants
     * These define the types of keyboard layouts we support
     */
    object Layouts {
        const val LAYOUT_PHONETIC = "phonetic"
        const val LAYOUT_KAVI_CUSTOM = "kavi_custom"
        const val LAYOUT_QWERTY = "qwerty"

        // Layout file names (stored in assets folder)
        const val LAYOUT_FILE_PHONETIC = "phonetic.json"
        const val LAYOUT_FILE_KAVI = "kavi.json"
        const val LAYOUT_FILE_QWERTY = "qwerty.json"
    }

    /**
     * Suggestion Engine Constants
     * These control how word suggestions work
     */
    object Suggestions {
        const val MAX_SUGGESTIONS = 5              // Show maximum 5 suggestions
        const val MAX_INTERNAL_RESULTS = 10        // Fetch 10 candidates internally before ranking
        const val MIN_WORD_LENGTH = 2              // Start suggesting after 2 characters
        const val SUGGESTION_TIMEOUT_MS = 300L     // Wait 300 milliseconds before suggesting
        const val LEARNING_ENABLED_DEFAULT = true  // Learn from user's typing by default

        // Confidence Scoring Weights (must sum to 1.0)
        const val FREQUENCY_WEIGHT = 0.4f          // How often word is used
        const val RECENCY_WEIGHT = 0.3f            // How recently word was used
        const val DICTIONARY_WEIGHT = 0.3f         // Is it a common dictionary word

        // Base Confidence Ranges
        const val USER_HISTORY_BASE_CONFIDENCE = 0.7f     // User's personal words: 0.7-1.0
        const val USER_HISTORY_MAX_BOOST = 0.3f           // Maximum boost from frequency
        const val DICTIONARY_BASE_CONFIDENCE = 0.5f       // Dictionary words: 0.5-0.7
        const val DICTIONARY_MAX_BOOST = 0.2f             // Maximum boost from frequency
        const val ML_MIN_CONFIDENCE = 0.3f                // ML predictions must be >= 0.3

        // Frequency Assumptions
        const val MAX_USER_FREQUENCY = 100        // Assume max 100 uses for a user word
        const val MAX_DICTIONARY_FREQUENCY = 1000 // Common words have higher frequency
    }

    /**
     * Clipboard Constants
     * These control the clipboard history feature
     */
    object Clipboard {
        const val MAX_HISTORY_ITEMS = 50         // Remember last 50 copied items
        const val MAX_CLIP_LENGTH = 1000         // Maximum characters per clip
        const val MAX_UNDO_STACK_SIZE = 20       // Remember last 20 actions for undo
    }

    /**
     * Voice Input Constants
     * These control voice-to-text and text-to-speech features
     */
    object Voice {
        const val LANGUAGE_CODE_KANNADA = "kn-IN"
        const val SPEECH_TIMEOUT_MS = 5000L      // 5 seconds timeout for voice input
        const val DEFAULT_SPEECH_RATE = 1.0f     // Normal speech speed
        const val DEFAULT_PITCH = 1.0f           // Normal voice pitch
    }

    /**
     * Theme Constants
     * These control the appearance of the keyboard
     */
    object Theme {
        const val DEFAULT_KEY_HEIGHT_DP = 56     // Default height of keyboard keys
        const val MIN_KEY_HEIGHT_DP = 40         // Minimum height
        const val MAX_KEY_HEIGHT_DP = 80         // Maximum height
        const val DEFAULT_KEY_RADIUS_DP = 8      // Rounded corner radius
        const val KEY_POPUP_DURATION_MS = 150L   // How long to show key press popup
    }

    /**
     * Analytics Constants
     * These control usage tracking and analytics
     */
    object Analytics {
        const val EVENT_KEY_PRESS = "key_press"
        const val EVENT_LAYOUT_SWITCH = "layout_switch"
        const val EVENT_SUGGESTION_ACCEPTED = "suggestion_accepted"
        const val EVENT_VOICE_INPUT_USED = "voice_input_used"
        const val EVENT_CLIPBOARD_USED = "clipboard_used"
        const val EVENT_THEME_CHANGED = "theme_changed"

        // Properties for analytics events
        const val PROPERTY_LAYOUT_TYPE = "layout_type"
        const val PROPERTY_SUGGESTION_POSITION = "suggestion_position"
        const val PROPERTY_VOICE_DURATION = "voice_duration_ms"
        const val PROPERTY_THEME_ID = "theme_id"
    }

    /**
     * Preferences Keys
     * These are like labels on storage boxes where we save user settings
     */
    object Prefs {
        const val KEY_SELECTED_LAYOUT = "selected_layout"
        const val KEY_THEME_ID = "theme_id"
        const val KEY_KEY_HEIGHT = "key_height"
        const val KEY_NUMBER_ROW_ENABLED = "number_row_enabled"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_POPUP_ENABLED = "popup_enabled"
        const val KEY_AUTO_CAPITALIZATION = "auto_capitalization"
        const val KEY_AUTO_CORRECTION = "auto_correction"
        const val KEY_LEARNING_ENABLED = "learning_enabled"
    }

    /**
     * Network Constants
     * These control API calls to external services (like Bhashini for voice)
     */
    object Network {
        const val CONNECT_TIMEOUT_SECONDS = 30L
        const val READ_TIMEOUT_SECONDS = 30L
        const val WRITE_TIMEOUT_SECONDS = 30L
    }

    /**
     * File Constants
     * These define file paths and storage locations
     */
    object Files {
        const val DICTIONARY_FOLDER = "dictionaries"
        const val LAYOUTS_FOLDER = "layouts"
        const val THEMES_FOLDER = "themes"
        const val VOICE_CACHE_FOLDER = "voice_cache"
        const val ML_MODELS_FOLDER = "ml_models"
        const val TRANSLITERATION_FOLDER = "transliteration"
    }

    /**
     * Dictionary Constants
     * These control dictionary loading and management
     */
    object Dictionary {
        // Dictionary file paths (in assets folder)
        const val KANNADA_DICT_FILE = "dictionaries/kannada_dictionary.txt"
        const val ENGLISH_DICT_FILE = "dictionaries/english_dictionary.txt"
        const val KANNADA_PHRASES_FILE = "dictionaries/kannada_common_phrases.txt"
        const val DICTIONARY_METADATA_FILE = "dictionaries/dictionary_metadata.json"

        // Dictionary loading limits
        const val MAX_DICTIONARY_SIZE = 100000       // Maximum words to load
        const val MIN_WORD_FREQUENCY = 1             // Minimum frequency to include
        const val CACHE_SIZE = 5000                  // Cache top 5000 frequent words

        // Dictionary format settings
        const val WORD_FREQUENCY_SEPARATOR = " "     // Space separates word from frequency
        const val COMMENT_PREFIX = "#"               // Lines starting with # are comments
        const val ENCODING = "UTF-8"                 // File encoding
    }

    /**
     * Machine Learning Constants
     * These control ML model inference and predictions
     */
    object ML {
        // Model file paths (in assets folder)
        const val NEXT_WORD_MODEL = "ml_models/kannada_next_word_v1.tflite"
        const val AUTOCOMPLETE_MODEL = "ml_models/kannada_autocomplete_v1.tflite"
        const val MODEL_METADATA_FILE = "ml_models/model_metadata.json"

        // Inference settings
        const val MAX_CONTEXT_WORDS = 3              // Use last 3 words for context
        const val INFERENCE_TIMEOUT_MS = 50L         // Max 50ms for ML inference
        const val MIN_PREDICTION_CONFIDENCE = 0.3f   // Only show predictions >= 0.3 confidence
        const val MAX_PREDICTIONS = 10               // Get top 10 predictions from model

        // Model configuration
        const val USE_GPU_ACCELERATION = true        // Use GPU if available
        const val NUM_THREADS = 4                    // Number of CPU threads for inference
        const val ALLOW_FP16 = true                  // Allow 16-bit floating point

        // Vocabulary settings
        const val VOCABULARY_SIZE = 50000            // Model vocabulary size
        const val EMBEDDING_DIM = 300                // Word embedding dimensions
        const val MAX_SEQUENCE_LENGTH = 20           // Maximum input sequence length
        const val UNKNOWN_TOKEN = "<UNK>"            // Token for unknown words
        const val PAD_TOKEN = "<PAD>"                // Padding token
        const val START_TOKEN = "<START>"            // Sentence start token
        const val END_TOKEN = "<END>"                // Sentence end token

        // Model versioning
        const val MODEL_VERSION = "1.0.0"            // Current model version
        const val MIN_SUPPORTED_VERSION = "1.0.0"    // Minimum compatible version

        // Learning settings (for future online learning)
        const val ENABLE_ONLINE_LEARNING = false     // Disabled by default (privacy)
        const val LEARNING_RATE = 0.001f             // Learning rate for updates
        const val BATCH_SIZE = 32                    // Batch size for training
    }

    /**
     * Transliteration Constants
     * These control phonetic transliteration (English → Kannada)
     */
    object Transliteration {
        // Transliteration file paths
        const val PHONETIC_RULES_FILE = "transliteration/phonetic_rules.json"
        const val SPECIAL_CASES_FILE = "transliteration/special_cases.json"
        const val COMMON_MISTAKES_FILE = "transliteration/common_mistakes.json"

        // Transliteration settings
        const val MAX_CACHE_SIZE = 1000              // Cache 1000 frequent conversions
        const val ENABLE_CACHING = true              // Enable transliteration cache
        const val CASE_SENSITIVE = false             // Ignore case when transliterating

        // Phonetic mapping modes
        const val MODE_STRICT = "strict"             // Strict phonetic rules
        const val MODE_RELAXED = "relaxed"           // Allow common variations
        const val DEFAULT_MODE = MODE_RELAXED        // Default to relaxed mode
    }

    /**
     * Typo Correction Constants
     * These control spell checking and typo correction
     */
    object TypoCorrection {
        const val MAX_EDIT_DISTANCE = 2              // Maximum Levenshtein distance
        const val MIN_WORD_LENGTH_FOR_CORRECTION = 3 // Only correct words >= 3 chars
        const val MAX_CORRECTION_CANDIDATES = 5      // Show top 5 corrections
        const val CORRECTION_CONFIDENCE_THRESHOLD = 0.6f // Minimum confidence to suggest

        // Edit distance costs
        const val INSERTION_COST = 1                 // Cost of adding a character
        const val DELETION_COST = 1                  // Cost of removing a character
        const val SUBSTITUTION_COST = 1              // Cost of replacing a character
        const val TRANSPOSITION_COST = 1             // Cost of swapping adjacent chars

        // Kannada-specific typo patterns
        const val ENABLE_KANNADA_TYPO_PATTERNS = true // Use Kannada-specific rules
        const val ENABLE_PHONETIC_MATCHING = true     // Match similar sounding words
    }

    /**
     * Audio/Sound Constants
     * These control sound effects when typing
     */
    object Audio {
        const val MAX_SOUND_STREAMS = 5           // Maximum simultaneous sounds
        const val DEFAULT_SOUND_VOLUME = 0.5f     // 50% volume
        const val MIN_SOUND_VOLUME = 0.0f         // Muted
        const val MAX_SOUND_VOLUME = 1.0f         // Maximum volume
    }

    /**
     * Converter Constants (for ASCII to Unicode conversion)
     */
    object Converter {
        const val ENCODING_NUDI = "nudi"
        const val ENCODING_BARAHA = "baraha"
        const val MAX_INPUT_LENGTH = 10000     // Maximum characters to convert at once
    }

    /**
     * Notification Constants
     */
    object Notifications {
        const val CHANNEL_ID_TIPS = "keyboard_tips"
        const val CHANNEL_ID_UPDATES = "keyboard_updates"
        const val NOTIFICATION_ID_TIP = 1001
        const val NOTIFICATION_ID_UPDATE = 1002
    }
}
