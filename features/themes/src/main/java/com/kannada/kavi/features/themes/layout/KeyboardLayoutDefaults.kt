package com.kannada.kavi.features.themes.layout

import com.kannada.kavi.core.layout.models.Key
import com.kannada.kavi.core.layout.models.KeyType
import com.kannada.kavi.core.layout.models.KeyboardRow

/**
 * KeyboardLayoutDefaults - Composable keyboard layout builder
 *
 * Provides a DSL (Domain-Specific Language) for creating keyboard layouts
 * in a declarative, easy-to-read way.
 *
 * USAGE:
 * ```kotlin
 * val layout = keyboardLayout {
 *     row {
 *         key("ಅ") { hint = "1" }
 *         key("ಆ") { hint = "2" }
 *         key("ಇ") { hint = "3" }
 *     }
 *     row {
 *         key("ಕ")
 *         key("ಖ")
 *         key("ಗ")
 *     }
 * }
 * ```
 */

/**
 * KeyboardLayoutBuilder - Top-level layout builder
 */
class KeyboardLayoutBuilder {
    private val rows = mutableListOf<KeyboardRow>()

    /**
     * Add a row to the keyboard
     */
    fun row(builder: KeyboardRowBuilder.() -> Unit) {
        val rowBuilder = KeyboardRowBuilder()
        rowBuilder.builder()
        rows.add(KeyboardRow(rowBuilder.build()))
    }

    fun build(): List<KeyboardRow> = rows
}

/**
 * KeyboardRowBuilder - Row-level builder
 */
class KeyboardRowBuilder {
    private val keys = mutableListOf<Key>()

    /**
     * Add a character key
     */
    fun key(
        label: String,
        output: String = label,
        builder: KeyBuilder.() -> Unit = {}
    ) {
        val keyBuilder = KeyBuilder(label, output, KeyType.CHARACTER)
        keyBuilder.builder()
        keys.add(keyBuilder.build())
    }

    /**
     * Add a shift key
     */
    fun shift(width: Float = 1.5f, builder: KeyBuilder.() -> Unit = {}) {
        val keyBuilder = KeyBuilder("⇧", "", KeyType.SHIFT)
        keyBuilder.width = width
        keyBuilder.builder()
        keys.add(keyBuilder.build())
    }

    /**
     * Add a delete key
     */
    fun delete(width: Float = 1.5f, builder: KeyBuilder.() -> Unit = {}) {
        val keyBuilder = KeyBuilder("⌫", "", KeyType.DELETE)
        keyBuilder.width = width
        keyBuilder.builder()
        keys.add(keyBuilder.build())
    }

    /**
     * Add an enter key
     */
    fun enter(width: Float = 1.5f, builder: KeyBuilder.() -> Unit = {}) {
        val keyBuilder = KeyBuilder("↵", "\n", KeyType.ENTER)
        keyBuilder.width = width
        keyBuilder.builder()
        keys.add(keyBuilder.build())
    }

    /**
     * Add a space key
     */
    fun space(width: Float = 4.0f, builder: KeyBuilder.() -> Unit = {}) {
        val keyBuilder = KeyBuilder("", " ", KeyType.SPACE)
        keyBuilder.width = width
        keyBuilder.builder()
        keys.add(keyBuilder.build())
    }

    /**
     * Add a symbols key (?123)
     */
    fun symbols(label: String = "?123", width: Float = 1.5f, builder: KeyBuilder.() -> Unit = {}) {
        val keyBuilder = KeyBuilder(label, "", KeyType.SYMBOLS)
        keyBuilder.width = width
        keyBuilder.builder()
        keys.add(keyBuilder.build())
    }

    /**
     * Add a language switch key
     */
    fun language(label: String = "🌐", width: Float = 1.0f, builder: KeyBuilder.() -> Unit = {}) {
        val keyBuilder = KeyBuilder(label, "", KeyType.LANGUAGE)
        keyBuilder.width = width
        keyBuilder.builder()
        keys.add(keyBuilder.build())
    }

    /**
     * Add an emoji key
     */
    fun emoji(label: String = "😊", width: Float = 1.0f, builder: KeyBuilder.() -> Unit = {}) {
        val keyBuilder = KeyBuilder(label, "", KeyType.EMOJI)
        keyBuilder.width = width
        keyBuilder.builder()
        keys.add(keyBuilder.build())
    }

    /**
     * Add a clipboard key
     */
    fun clipboard(label: String = "📋", width: Float = 1.0f, builder: KeyBuilder.() -> Unit = {}) {
        val keyBuilder = KeyBuilder(label, "", KeyType.CLIPBOARD)
        keyBuilder.width = width
        keyBuilder.builder()
        keys.add(keyBuilder.build())
    }

    /**
     * Add a default layer key (ABC)
     */
    fun defaultLayer(label: String = "ABC", width: Float = 1.5f, builder: KeyBuilder.() -> Unit = {}) {
        val keyBuilder = KeyBuilder(label, "", KeyType.DEFAULT)
        keyBuilder.width = width
        keyBuilder.builder()
        keys.add(keyBuilder.build())
    }

    fun build(): List<Key> = keys
}

/**
 * KeyBuilder - Individual key builder
 */
class KeyBuilder(
    private val label: String,
    private val output: String,
    private val type: KeyType
) {
    var width: Float = 1.0f
    var hint: String? = null
    var longPress: List<String>? = null
    var showPopup: Boolean = true
    var isEnabled: Boolean = true

    /**
     * Add long-press alternatives
     */
    fun longPressKeys(vararg keys: String) {
        longPress = keys.toList()
    }

    fun build(): Key {
        return Key(
            label = label,
            output = output,
            type = type,
            width = width,
            hint = hint,
            longPressKeys = longPress,
            showPopup = showPopup,
            isEnabled = isEnabled
        )
    }
}

/**
 * Top-level function to create a keyboard layout
 */
fun keyboardLayout(builder: KeyboardLayoutBuilder.() -> Unit): List<KeyboardRow> {
    val layoutBuilder = KeyboardLayoutBuilder()
    layoutBuilder.builder()
    return layoutBuilder.build()
}

/**
 * KeyboardLayoutPresets - Pre-built keyboard layouts
 */
object KeyboardLayoutPresets {

    /**
     * Standard QWERTY layout
     */
    fun qwerty(): List<KeyboardRow> = keyboardLayout {
        row {
            key("q") { hint = "1"; longPressKeys("1") }
            key("w") { hint = "2"; longPressKeys("2") }
            key("e") { hint = "3"; longPressKeys("3") }
            key("r") { hint = "4"; longPressKeys("4") }
            key("t") { hint = "5"; longPressKeys("5") }
            key("y") { hint = "6"; longPressKeys("6") }
            key("u") { hint = "7"; longPressKeys("7") }
            key("i") { hint = "8"; longPressKeys("8") }
            key("o") { hint = "9"; longPressKeys("9") }
            key("p") { hint = "0"; longPressKeys("0") }
        }

        row {
            key("a")
            key("s")
            key("d")
            key("f")
            key("g")
            key("h")
            key("j")
            key("k")
            key("l")
        }

        row {
            shift()
            key("z")
            key("x")
            key("c")
            key("v")
            key("b")
            key("n")
            key("m")
            delete()
        }

        row {
            symbols()
            language()
            emoji()
            space()
            key(",")
            key(".")
            enter()
        }
    }

    /**
     * Number pad layout
     */
    fun numpad(): List<KeyboardRow> = keyboardLayout {
        row {
            key("1")
            key("2")
            key("3")
        }

        row {
            key("4")
            key("5")
            key("6")
        }

        row {
            key("7")
            key("8")
            key("9")
        }

        row {
            key(".")
            key("0")
            delete()
        }
    }

    /**
     * Symbols layout
     */
    fun symbols(): List<KeyboardRow> = keyboardLayout {
        row {
            key("1")
            key("2")
            key("3")
            key("4")
            key("5")
            key("6")
            key("7")
            key("8")
            key("9")
            key("0")
        }

        row {
            key("@")
            key("#")
            key("$")
            key("_")
            key("&")
            key("-")
            key("+")
            key("(")
            key(")")
            key("/")
        }

        row {
            symbols("=\\<", width = 1.5f)
            key("*")
            key("\"")
            key("'")
            key(":")
            key(";")
            key("!")
            key("?")
            delete(width = 1.5f)
        }

        row {
            defaultLayer()
            emoji()
            space()
            key(",")
            key(".")
            enter()
        }
    }
}

/**
 * KeyboardLayoutExtensions - Extension functions for layout manipulation
 */
object KeyboardLayoutExtensions {

    /**
     * Add hints to the first row (numbers 1-0)
     */
    fun List<KeyboardRow>.withNumberHints(): List<KeyboardRow> {
        if (isEmpty()) return this

        val hints = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val firstRow = first()
        val keysWithHints = firstRow.keys.mapIndexed { index, key ->
            if (index < hints.size) {
                key.copy(hint = hints[index])
            } else {
                key
            }
        }

        return listOf(KeyboardRow(keysWithHints)) + drop(1)
    }

    /**
     * Scale all key widths by a factor
     */
    fun List<KeyboardRow>.scaleWidths(scale: Float): List<KeyboardRow> {
        return map { row ->
            KeyboardRow(row.keys.map { key ->
                key.copy(width = key.width * scale)
            })
        }
    }

    /**
     * Filter out disabled keys
     */
    fun List<KeyboardRow>.removeDisabledKeys(): List<KeyboardRow> {
        return map { row ->
            KeyboardRow(row.keys.filter { it.isEnabled })
        }
    }

    /**
     * Get all character keys from layout
     */
    fun List<KeyboardRow>.getCharacterKeys(): List<Key> {
        return flatMap { it.keys }.filter { it.type == KeyType.CHARACTER }
    }

    /**
     * Get all modifier keys from layout
     */
    fun List<KeyboardRow>.getModifierKeys(): List<Key> {
        return flatMap { it.keys }.filter { it.isModifier }
    }
}
