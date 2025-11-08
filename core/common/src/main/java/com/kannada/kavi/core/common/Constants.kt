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
        const val MIN_WORD_LENGTH = 2              // Start suggesting after 2 characters
        const val SUGGESTION_TIMEOUT_MS = 300L     // Wait 300 milliseconds before suggesting
        const val LEARNING_ENABLED_DEFAULT = true  // Learn from user's typing by default
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
