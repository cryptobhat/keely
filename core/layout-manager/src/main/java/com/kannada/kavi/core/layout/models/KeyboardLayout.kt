package com.kannada.kavi.core.layout.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * KeyboardLayout - Complete Keyboard Configuration
 *
 * This represents an entire keyboard layout with all its layers.
 * Like a complete instruction manual for how the keyboard should look and behave!
 *
 * A keyboard typically has multiple "layers":
 * - Default layer: lowercase letters (a, b, c...)
 * - Shift layer: uppercase letters (A, B, C...)
 * - Symbols layer: numbers and symbols (1, 2, @, #...)
 * - Extra symbols layer: more special characters
 *
 * Each layer has multiple rows of keys.
 */
@Parcelize
data class KeyboardLayout(
    /**
     * Unique identifier for this layout
     * Examples: "phonetic", "kavi_custom", "qwerty"
     */
    val id: String,

    /**
     * Display name of the layout
     * Example: "Phonetic Kannada Layout"
     */
    val name: String,

    /**
     * Description of what this layout does
     * Example: "Type in English, get Kannada (transliteration-based)"
     */
    val description: String,

    /**
     * Version of this layout
     * Example: "1.0"
     */
    val version: String,

    /**
     * Language code (ISO 639-1)
     * Examples: "kn" (Kannada), "en" (English)
     */
    val language: String,

    /**
     * All the layers in this keyboard
     * Key = layer name ("default", "shift", "symbols")
     * Value = list of rows for that layer
     */
    val layers: Map<String, List<KeyboardRow>>,

    /**
     * Is transliteration enabled for this layout?
     * If true, typing "namaste" converts to "ನಮಸ್ತೆ"
     */
    val transliterationEnabled: Boolean = false,

    /**
     * Transliteration rules (only if transliterationEnabled = true)
     * Maps English text to Kannada
     * Example: "ka" → "ಕ", "namaste" → "ನಮಸ್ತೆ"
     */
    val transliterationRules: Map<String, String>? = null,

    /**
     * Default layer name
     * Usually "default"
     */
    val defaultLayer: String = "default"
) : Parcelable {

    /**
     * Get a specific layer by name
     * Returns null if layer doesn't exist
     */
    fun getLayer(layerName: String): List<KeyboardRow>? {
        return layers[layerName]
    }

    /**
     * Get the default layer rows
     */
    fun getDefaultLayer(): List<KeyboardRow>? {
        return layers[defaultLayer]
    }

    /**
     * Get the shift layer rows
     */
    fun getShiftLayer(): List<KeyboardRow>? {
        return layers["shift"]
    }

    /**
     * Get the symbols layer rows
     */
    fun getSymbolsLayer(): List<KeyboardRow>? {
        return layers["symbols"]
    }

    /**
     * Check if this layout has a specific layer
     */
    fun hasLayer(layerName: String): Boolean {
        return layers.containsKey(layerName)
    }

    /**
     * Get all available layer names
     */
    fun getLayerNames(): List<String> {
        return layers.keys.toList()
    }

    /**
     * Count total number of keys across all layers
     */
    fun getTotalKeyCount(): Int {
        return layers.values.sumOf { rows ->
            rows.sumOf { it.keyCount }
        }
    }

    /**
     * Get transliteration result for input text
     * Returns null if transliteration is not enabled or no rule found
     */
    fun transliterate(input: String): String? {
        if (!transliterationEnabled || transliterationRules == null) {
            return null
        }
        return transliterationRules[input.lowercase()]
    }

    /**
     * Check if this layout supports Kannada
     */
    val isKannada: Boolean
        get() = language == "kn" || language == "kan" || language == "kannada"

    /**
     * Check if this layout supports English
     */
    val isEnglish: Boolean
        get() = language == "en" || language == "eng" || language == "english"
}

/**
 * LayerName - Common layer names used in keyboards
 */
object LayerName {
    const val DEFAULT = "default"
    const val SHIFT = "shift"
    const val SYMBOLS = "symbols"
    const val SYMBOLS_EXTRA = "symbols_extra"
    const val SYMBOLS_ALT = "symbols_alt"
    const val EMOJI = "emoji"
}

/**
 * LayoutType - Predefined layout types
 */
enum class LayoutType(val id: String, val displayName: String) {
    PHONETIC("phonetic", "Phonetic"),
    KAVI_CUSTOM("kavi_custom", "Kavi Custom"),
    QWERTY("qwerty", "QWERTY"),
    INSCRIPT("inscript", "InScript");

    companion object {
        fun fromId(id: String): LayoutType? {
            return values().find { it.id == id }
        }
    }
}
