package com.kannada.kavi.core.layout.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * KeyboardRow - Represents One Row of Keys
 *
 * A keyboard is made up of multiple rows, like shelves on a bookcase.
 * Each row contains a list of keys.
 *
 * Example keyboard:
 * Row 1: [q] [w] [e] [r] [t] [y] [u] [i] [o] [p]
 * Row 2: [a] [s] [d] [f] [g] [h] [j] [k] [l]
 * Row 3: [shift] [z] [x] [c] [v] [b] [n] [m] [delete]
 * Row 4: [123] [globe] [emoji] [    space    ] [enter]
 */
@Parcelize
data class KeyboardRow(
    /**
     * List of keys in this row, from left to right
     */
    val keys: List<Key>,

    /**
     * Optional: Row height multiplier
     * 1.0 = normal height
     * 1.2 = 20% taller
     */
    val heightMultiplier: Float = 1.0f,

    /**
     * Optional: Horizontal padding for this row
     * Useful for centering shorter rows
     */
    val horizontalPadding: Float = 0.0f
) : Parcelable {

    /**
     * Total width of all keys in this row
     * Useful for layout calculations
     */
    val totalWidth: Float
        get() = keys.sumOf { it.width.toDouble() }.toFloat()

    /**
     * Number of keys in this row
     */
    val keyCount: Int
        get() = keys.size

    /**
     * Check if row is empty
     */
    val isEmpty: Boolean
        get() = keys.isEmpty()

    /**
     * Check if row has keys
     */
    val isNotEmpty: Boolean
        get() = keys.isNotEmpty()

    /**
     * Get key at specific index safely
     * Returns null if index is out of bounds
     */
    fun getKeyOrNull(index: Int): Key? {
        return keys.getOrNull(index)
    }

    /**
     * Get all character keys in this row
     * Filters out modifiers and action keys
     */
    fun getCharacterKeys(): List<Key> {
        return keys.filter { it.isCharacter }
    }

    /**
     * Get all modifier keys in this row
     * (Shift, Symbols, etc.)
     */
    fun getModifierKeys(): List<Key> {
        return keys.filter { it.isModifier }
    }

    /**
     * Find key by label
     */
    fun findKeyByLabel(label: String): Key? {
        return keys.find { it.label == label }
    }

    /**
     * Find key by output
     */
    fun findKeyByOutput(output: String): Key? {
        return keys.find { it.output == output }
    }
}

/**
 * Create a keyboard row easily
 * Example: keyboardRow(listOf(key1, key2, key3))
 */
fun keyboardRow(
    keys: List<Key>,
    heightMultiplier: Float = 1.0f,
    horizontalPadding: Float = 0.0f
) = KeyboardRow(
    keys = keys,
    heightMultiplier = heightMultiplier,
    horizontalPadding = horizontalPadding
)

/**
 * Create a keyboard row from character strings
 * Example: keyboardRow("qwertyuiop") creates a row with those letters
 */
fun keyboardRow(chars: String): KeyboardRow {
    val keys = chars.map { char ->
        characterKey(char.toString())
    }
    return KeyboardRow(keys = keys)
}
