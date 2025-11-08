package com.kannada.kavi.core.common.extensions

/**
 * String Extensions - Making Text Operations Super Easy!
 *
 * Extensions are like giving superpowers to existing things.
 * Imagine if you could teach your bicycle new tricks - that's what extensions do to code!
 *
 * Instead of writing long functions, we can just call: myText.isKannada()
 * Much cleaner and easier to read!
 */

/**
 * Check if a string contains Kannada characters
 *
 * Kannada Unicode range: \u0C80-\u0CFF
 * Think of Unicode as a giant phonebook where every character in every language
 * has a unique number. Kannada characters live in the 0C80-0CFF neighborhood.
 *
 * Example:
 * "Hello".isKannada() -> false
 * "ನಮಸ್ಕಾರ".isKannada() -> true
 * "Hello ನಮಸ್ಕಾರ".isKannada() -> true (has some Kannada)
 */
fun String.isKannada(): Boolean {
    // Check each character - if any character is Kannada, return true
    return this.any { char ->
        char in '\u0C80'..'\u0CFF'
    }
}

/**
 * Check if string contains ONLY Kannada characters (and spaces)
 *
 * Example:
 * "ನಮಸ್ಕಾರ".isOnlyKannada() -> true
 * "Hello ನಮಸ್ಕಾರ".isOnlyKannada() -> false (mixed languages)
 */
fun String.isOnlyKannada(): Boolean {
    if (this.isEmpty()) return false
    return this.all { char ->
        char in '\u0C80'..'\u0CFF' || char.isWhitespace()
    }
}

/**
 * Check if string contains English characters
 *
 * Example:
 * "Hello".isEnglish() -> true
 * "ನಮಸ್ಕಾರ".isEnglish() -> false
 */
fun String.isEnglish(): Boolean {
    return this.any { char ->
        char in 'a'..'z' || char in 'A'..'Z'
    }
}

/**
 * Count how many Kannada characters are in the string
 *
 * Example:
 * "Hello ನಮ".kannadaCharCount() -> 2
 */
fun String.kannadaCharCount(): Int {
    return this.count { char ->
        char in '\u0C80'..'\u0CFF'
    }
}

/**
 * Convert English text to lowercase for phonetic matching
 * Also removes extra spaces and special characters
 *
 * Example:
 * "  Namaskara!  ".normalizeForPhonetic() -> "namaskara"
 */
fun String.normalizeForPhonetic(): String {
    return this.trim()                    // Remove spaces from start and end
        .lowercase()                      // Convert to lowercase
        .replace(Regex("[^a-z]"), "")    // Remove anything that's not a-z
}

/**
 * Truncate (cut) string to maximum length
 * If string is longer, it adds "..." at the end
 *
 * Example:
 * "This is a very long text".truncate(10) -> "This is a..."
 */
fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
    if (this.length <= maxLength) return this
    return this.substring(0, maxLength - ellipsis.length) + ellipsis
}

/**
 * Check if string is empty or contains only whitespace
 *
 * Example:
 * "".isBlank() -> true
 * "   ".isBlank() -> true
 * " a ".isBlank() -> false
 */
fun String?.isBlank(): Boolean {
    return this == null || this.trim().isEmpty()
}

/**
 * Check if string has actual content (opposite of isBlank)
 *
 * Example:
 * "Hello".isNotBlank() -> true
 * "".isNotBlank() -> false
 */
fun String?.isNotBlank(): Boolean {
    return !this.isBlank()
}

/**
 * Get first N characters safely (won't crash if string is shorter)
 *
 * Example:
 * "Hello".firstChars(3) -> "Hel"
 * "Hi".firstChars(5) -> "Hi" (doesn't crash!)
 */
fun String.firstChars(count: Int): String {
    return this.take(count.coerceAtMost(this.length))
}

/**
 * Get last N characters safely
 *
 * Example:
 * "Hello".lastChars(3) -> "llo"
 */
fun String.lastChars(count: Int): String {
    return this.takeLast(count.coerceAtMost(this.length))
}

/**
 * Remove all whitespace from string
 *
 * Example:
 * "Hello World  !".removeWhitespace() -> "HelloWorld!"
 */
fun String.removeWhitespace(): String {
    return this.replace(Regex("\\s+"), "")
}

/**
 * Convert string to title case (First Letter of Each Word Capitalized)
 *
 * Example:
 * "hello world".toTitleCase() -> "Hello World"
 */
fun String.toTitleCase(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }
}

/**
 * Check if string is a valid Kannada word (for dictionary lookup)
 * Valid means: Only Kannada characters, reasonable length, no excessive repetition
 */
fun String.isValidKannadaWord(): Boolean {
    if (this.isEmpty() || this.length > 50) return false
    if (!this.isOnlyKannada()) return false

    // Check for excessive character repetition (like "aaaaaaaa")
    // Count consecutive same characters
    var maxConsecutive = 1
    var currentConsecutive = 1

    for (i in 1 until this.length) {
        if (this[i] == this[i - 1]) {
            currentConsecutive++
            maxConsecutive = maxOf(maxConsecutive, currentConsecutive)
        } else {
            currentConsecutive = 1
        }
    }

    // If more than 4 same characters in a row, probably not a real word
    return maxConsecutive <= 4
}

/**
 * Split text into words (handles both English and Kannada)
 *
 * Example:
 * "Hello ನಮಸ್ಕಾರ World".splitWords() -> ["Hello", "ನಮಸ್ಕಾರ", "World"]
 */
fun String.splitWords(): List<String> {
    return this.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
}

/**
 * Convert Kannada numerals (೦-೯) to English numerals (0-9)
 *
 * Example:
 * "೧೨೩".kannadaNumeralsToEnglish() -> "123"
 */
fun String.kannadaNumeralsToEnglish(): String {
    val kannadaDigits = "೦೧೨೩೪೫೬೭೮೯"
    val englishDigits = "0123456789"

    return this.map { char ->
        val index = kannadaDigits.indexOf(char)
        if (index != -1) englishDigits[index] else char
    }.joinToString("")
}

/**
 * Convert English numerals (0-9) to Kannada numerals (೦-೯)
 *
 * Example:
 * "123".englishNumeralsToKannada() -> "೧೨೩"
 */
fun String.englishNumeralsToKannada(): String {
    val englishDigits = "0123456789"
    val kannadaDigits = "೦೧೨೩೪೫೬೭೮೯"

    return this.map { char ->
        val index = englishDigits.indexOf(char)
        if (index != -1) kannadaDigits[index] else char
    }.joinToString("")
}
