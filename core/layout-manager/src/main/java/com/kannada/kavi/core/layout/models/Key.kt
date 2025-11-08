package com.kannada.kavi.core.layout.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Key - Represents a Single Key on the Keyboard
 *
 * Think of this like a button on your keyboard. Each button has:
 * - A label (what you see on the button)
 * - An output (what gets typed when you press it)
 * - A type (is it a letter, delete button, space bar, etc.)
 * - A width (how wide the button is)
 *
 * Example:
 * Regular letter key:  label="ಅ", output="ಅ", type=CHARACTER
 * Space bar:           label="", output=" ", type=SPACE
 * Shift key:           label="⇧", output="", type=SHIFT
 */
@Parcelize
data class Key(
    /**
     * The text displayed on the key
     * Examples: "ಅ", "A", "⇧", "123", "😊"
     */
    val label: String,

    /**
     * What gets typed when this key is pressed
     * Examples: "ಅ", "a", " " (space), "\n" (enter)
     * Can be empty for modifier keys like Shift
     */
    val output: String,

    /**
     * The type of key (determines behavior)
     * See KeyType enum below
     */
    val type: KeyType = KeyType.CHARACTER,

    /**
     * Width of the key (relative to standard key width)
     * 1.0 = normal width
     * 1.5 = 50% wider (like Shift key)
     * 4.0 = 4x width (like Space bar)
     */
    val width: Float = 1.0f,

    /**
     * Long-press alternatives (popup characters)
     * Example: Long-press "a" might show ["ā", "à", "á", "â"]
     */
    val longPressKeys: List<String>? = null,

    /**
     * Should this key show a popup when pressed?
     */
    val showPopup: Boolean = true,

    /**
     * Is this key currently enabled?
     */
    val isEnabled: Boolean = true
) : Parcelable {

    /**
     * Check if this is a character key (types text)
     */
    val isCharacter: Boolean
        get() = type == KeyType.CHARACTER && output.isNotEmpty()

    /**
     * Check if this is a modifier key (Shift, Symbols, etc.)
     */
    val isModifier: Boolean
        get() = type in listOf(KeyType.SHIFT, KeyType.SYMBOLS, KeyType.SYMBOLS_EXTRA,
                               KeyType.SYMBOLS_ALT, KeyType.DEFAULT)

    /**
     * Check if this is an action key (Enter, Delete, etc.)
     */
    val isAction: Boolean
        get() = type in listOf(KeyType.ENTER, KeyType.DELETE, KeyType.SPACE)

    /**
     * Get display width in relative units
     */
    fun getDisplayWidth(): Float = width.coerceIn(0.5f, 10.0f)
}

/**
 * KeyType - Different Types of Keys on the Keyboard
 *
 * Each key can have a different behavior based on its type.
 * Like different tools in a toolbox - each has a specific job!
 */
enum class KeyType {
    /**
     * CHARACTER - Types a character (letters, numbers, symbols)
     * Most common type. When pressed, outputs the character.
     */
    CHARACTER,

    /**
     * SHIFT - Toggles between lowercase and uppercase
     * Changes the keyboard layer (default ↔ shift)
     */
    SHIFT,

    /**
     * DELETE - Deletes the previous character
     * Like the backspace key on your computer
     */
    DELETE,

    /**
     * ENTER - Inserts a newline or submits text
     * Behavior depends on the app (new line in notes, send in messages)
     */
    ENTER,

    /**
     * SPACE - Inserts a space character
     * The biggest key on the keyboard!
     */
    SPACE,

    /**
     * SYMBOLS - Switches to the symbols layer
     * Shows numbers and punctuation (123, @#$, etc.)
     */
    SYMBOLS,

    /**
     * SYMBOLS_EXTRA - Switches to extra symbols layer
     * More special characters (=\<, ~, √, etc.)
     */
    SYMBOLS_EXTRA,

    /**
     * SYMBOLS_ALT - Switches to alternate symbols layer
     * Even more special characters
     */
    SYMBOLS_ALT,

    /**
     * DEFAULT - Returns to the default layer
     * Goes back to letters (ABC)
     */
    DEFAULT,

    /**
     * LANGUAGE - Switches between keyboard layouts
     * Changes from Phonetic → Kavi → QWERTY
     */
    LANGUAGE,

    /**
     * EMOJI - Opens emoji picker
     * Shows emoji selection panel
     */
    EMOJI,

    /**
     * VOICE - Activates voice input
     * Starts speech-to-text
     */
    VOICE,

    /**
     * SETTINGS - Opens keyboard settings
     * Quick access to preferences
     */
    SETTINGS,

    /**
     * CLIPBOARD - Shows clipboard history
     * Opens popup with clipboard items
     */
    CLIPBOARD
}

/**
 * Create a character key quickly
 * Example: characterKey("ಅ") creates a key that types "ಅ"
 */
fun characterKey(
    char: String,
    longPress: List<String>? = null
) = Key(
    label = char,
    output = char,
    type = KeyType.CHARACTER,
    width = 1.0f,
    longPressKeys = longPress
)

/**
 * Create a modifier key quickly
 * Example: modifierKey("⇧", KeyType.SHIFT, 1.5f)
 */
fun modifierKey(
    label: String,
    type: KeyType,
    width: Float = 1.0f
) = Key(
    label = label,
    output = "",
    type = type,
    width = width,
    showPopup = false
)

/**
 * Create a space bar key
 */
fun spaceKey(width: Float = 4.0f) = Key(
    label = "",
    output = " ",
    type = KeyType.SPACE,
    width = width,
    showPopup = false
)

/**
 * Create a delete key
 */
fun deleteKey(width: Float = 1.5f) = Key(
    label = "⌫",
    output = "",
    type = KeyType.DELETE,
    width = width,
    showPopup = false
)

/**
 * Create an enter key
 */
fun enterKey(width: Float = 1.5f) = Key(
    label = "↵",
    output = "\n",
    type = KeyType.ENTER,
    width = width,
    showPopup = false
)
