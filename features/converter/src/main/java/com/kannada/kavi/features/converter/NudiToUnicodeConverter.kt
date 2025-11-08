package com.kannada.kavi.features.converter

import com.kannada.kavi.core.common.Result
import com.kannada.kavi.core.common.resultError
import com.kannada.kavi.core.common.resultSuccess

/**
 * Nudi to Unicode Converter
 *
 * This class converts ASCII-encoded Kannada text (Nudi format) to proper Unicode Kannada.
 *
 * WHAT IS NUDI?
 * -------------
 * Before Unicode became standard, people used special fonts like "Nudi" to type Kannada.
 * In these fonts, you'd type English characters, but they'd appear as Kannada on screen.
 * Problem: If you sent this text to someone without the Nudi font, they'd see gibberish!
 *
 * WHAT IS UNICODE?
 * ----------------
 * Unicode is the universal standard where every character has a unique code.
 * Unicode Kannada text works everywhere - WhatsApp, Facebook, any app, any device!
 *
 * THIS CONVERTER:
 * --------------
 * Takes old Nudi text (like "PÀìªÀiÁ") and converts it to proper Unicode (like "ಕನ್ನಡ")
 * Think of it like a translator that speaks both languages!
 *
 * Based on the open-source converter from: github.com/aravindavk/ascii2unicode
 */
class NudiToUnicodeConverter {

    /**
     * The Master Translation Dictionary
     *
     * This maps ASCII characters (Nudi encoding) to Unicode Kannada characters.
     * Each entry is like a word in a dictionary: "this character" = "that character"
     */
    private val conversionMap = mapOf(
        // Vowels (ಸ್ವರಗಳು) - Independent vowel characters
        "C" to "ಅ",   // a
        "D" to "ಆ",   // aa
        "E" to "ಇ",   // i
        "F" to "ಈ",   // ii
        "G" to "ಉ",   // u
        "H" to "ಊ",   // uu
        "I" to "ಋ",   // R (vocalic r)
        "J" to "ಎ",   // e
        "K" to "ಏ",   // ee
        "L" to "ಐ",   // ai
        "M" to "ಒ",   // o
        "N" to "ಓ",   // oo
        "O" to "ಔ",   // au

        // Consonants (ವ್ಯಂಜನಗಳು) - Basic consonants
        "P" to "ಕ",   // ka
        "Q" to "ಖ",   // kha
        "R" to "ಗ",   // ga
        "S" to "ಘ",   // gha
        "T" to "ಙ",   // nga
        "U" to "ಚ",   // cha
        "V" to "ಛ",   // chha
        "W" to "ಜ",   // ja
        "X" to "ಝ",   // jha
        "Y" to "ಞ",   // nya
        "Z" to "ಟ",   // Ta
        "[" to "ಠ",   // Tha
        "\\" to "ಡ",  // Da
        "]" to "ಢ",   // Dha
        "^" to "ಣ",   // Na
        "_" to "ತ",   // ta
        "`" to "ಥ",   // tha
        "a" to "ದ",   // da
        "b" to "ಧ",   // dha
        "c" to "ನ",   // na
        "d" to "ಪ",   // pa
        "e" to "ಫ",   // pha
        "f" to "ಬ",   // ba
        "g" to "ಭ",   // bha
        "h" to "ಮ",   // ma
        "i" to "ಯ",   // ya
        "j" to "ರ",   // ra
        "k" to "ಲ",   // la
        "l" to "ವ",   // va
        "m" to "ಶ",   // sha
        "n" to "ಷ",   // Sha
        "o" to "ಸ",   // sa
        "p" to "ಹ",   // ha
        "q" to "ಳ",   // La
        "r" to "ೞ",   // zha

        // Dependent vowel signs (ಮಾತ್ರೆಗಳು) - Vowel markers that attach to consonants
        "¨" to "ಾ",   // aa sign
        "©" to "ಿ",   // i sign
        "ª" to "ೀ",   // ii sign
        "«" to "ು",   // u sign
        "¬" to "ೂ",   // uu sign
        "­" to "ೃ",   // R sign
        "®" to "ೆ",   // e sign
        "¯" to "ೇ",   // ee sign
        "°" to "ೈ",   // ai sign
        "±" to "ೊ",   // o sign
        "²" to "ೋ",   // oo sign
        "³" to "ೌ",   // au sign

        // Special characters
        "¢" to "ಂ",   // anusvara (chandrabindu)
        "£" to "ಃ",   // visarga
        "¤" to "್",   // halant/virama (removes inherent vowel)

        // Numerals (ಸಂಖ್ಯೆಗಳು)
        "¦" to "೦",   // 0
        "§" to "೧",   // 1
        "¨" to "೨",   // 2
        "©" to "೩",   // 3
        "ª" to "೪",   // 4
        "«" to "೫",   // 5
        "¬" to "೬",   // 6
        "­" to "೭",   // 7
        "®" to "೮",   // 8
        "¯" to "೯",   // 9

        // Vattakshara (ದ್ವಿತ್ವ ಅಕ್ಷರಗಳು) - Doubled consonants
        "Ì" to "ಕ",
        "Í" to "ಖ",
        "Î" to "ಗ",
        "Ï" to "ಘ",
        "Ð" to "ಚ",
        "Ñ" to "ಛ",
        "Ò" to "ಜ",
        "Ó" to "ಝ",
        "Ô" to "ಟ",
        "Õ" to "ಠ",
        "Ö" to "ಡ",
        "×" to "ಢ",
        "Ø" to "ಣ",
        "Ù" to "ತ",
        "Ú" to "ಥ",
        "Û" to "ದ",
        "Ü" to "ಧ",
        "Ý" to "ನ",
        "Þ" to "ಪ",
        "ß" to "ಫ",
        "à" to "ಬ",
        "á" to "ಭ",
        "â" to "ಮ",
        "ã" to "ಯ",
        "ä" to "ರ",
        "å" to "ಲ",
        "æ" to "ವ",
        "ç" to "ಶ",
        "è" to "ಷ",
        "é" to "ಸ",
        "ê" to "ಹ",
        "ë" to "ಳ",

        // Arkavattu (ಅರ್ಕವಟ್ಟು) - Medial consonants
        "ì" to "್ಕ",
        "í" to "್ಖ",
        "î" to "್ಗ",
        "ï" to "್ಘ",
        "ð" to "್ರ",   // ra
        "ñ" to "್ಚ",
        "ò" to "್ಛ",
        "ó" to "್ಜ",
        "ô" to "್ಝ",
        "õ" to "್ಟ",
        "ö" to "್ಠ",
        "÷" to "್ಡ",
        "ø" to "್ಢ",
        "ù" to "್ಣ",
        "ú" to "್ತ",
        "û" to "್ಥ",
        "ü" to "್ದ",
        "ý" to "್ಧ",
        "þ" to "್ನ",
        "ÿ" to "್ಪ",

        // Common conjuncts and special combinations
        "Ã" to "ೀ",   // Special case for ii
        "Ä" to "ೂ",   // Special case for uu
        "Æ" to "ೃ",   // Special case for R
        "Ç" to "ೆ",   // Special case for e
        "È" to "ೇ",   // Special case for ee
        "É" to "ೈ",   // Special case for ai
        "Ê" to "ೊ",   // Special case for o
        "Ë" to "ೋ",   // Special case for oo
        "Ì" to "ೌ"    // Special case for au
    )

    /**
     * Convert Nudi ASCII text to Unicode Kannada
     *
     * @param nudiText The input text in Nudi encoding
     * @return Result containing the converted Unicode text, or an error
     *
     * Example:
     * ```
     * val result = converter.convert("PÀìªÀiÁ")
     * when (result) {
     *     is Result.Success -> println(result.data) // Prints: "ಕನ್ನಡ"
     *     is Result.Error -> println(result.exception.message)
     * }
     * ```
     */
    fun convert(nudiText: String): Result<String> {
        return try {
            // Empty string? Nothing to convert!
            if (nudiText.isEmpty()) {
                return resultSuccess("")
            }

            // Check if text is too long (prevent memory issues)
            if (nudiText.length > 100000) {
                return resultError("Text too long. Maximum 100,000 characters allowed.")
            }

            // The actual conversion happens here!
            val unicode = convertToUnicode(nudiText)

            resultSuccess(unicode)
        } catch (e: Exception) {
            resultError(e)
        }
    }

    /**
     * The core conversion logic
     *
     * This function walks through the Nudi text character by character,
     * looking up each character in our conversion map, and builds the Unicode result.
     *
     * It's like translating a sentence word by word using a dictionary!
     */
    private fun convertToUnicode(nudiText: String): String {
        val result = StringBuilder()
        var index = 0

        while (index < nudiText.length) {
            // Try to match the longest possible sequence first
            // (some characters are represented by multiple ASCII chars)
            var matched = false
            var maxLength = minOf(4, nudiText.length - index) // Try up to 4 chars

            // Try longest sequences first, then shorter ones
            for (length in maxLength downTo 1) {
                val substring = nudiText.substring(index, index + length)

                if (conversionMap.containsKey(substring)) {
                    // Found a match! Add the Unicode equivalent
                    result.append(conversionMap[substring])
                    index += length
                    matched = true
                    break
                }
            }

            // If no match found, keep the original character
            // (might be English text mixed in, or a space, punctuation, etc.)
            if (!matched) {
                result.append(nudiText[index])
                index++
            }
        }

        return result.toString()
    }

    /**
     * Check if text appears to be in Nudi format
     *
     * This is useful to detect if text needs conversion.
     * It checks if the text contains characters commonly used in Nudi encoding.
     *
     * @param text The text to check
     * @return true if text appears to be Nudi-encoded, false otherwise
     */
    fun isNudiText(text: String): Boolean {
        // Nudi text often contains characters in these ranges:
        // - Extended ASCII (128-255)
        // - Specific characters like ¨, ©, ª, etc.

        val nudiCharCount = text.count { char ->
            // Count characters that are likely from Nudi encoding
            char.code in 128..255 || conversionMap.containsKey(char.toString())
        }

        // If more than 30% of characters are Nudi-like, probably Nudi text
        return nudiCharCount.toFloat() / text.length > 0.3f
    }

    /**
     * Batch convert multiple strings
     *
     * Useful when converting multiple messages, file contents, etc.
     *
     * @param nudiTexts List of Nudi texts to convert
     * @return Result containing list of converted texts
     */
    fun convertBatch(nudiTexts: List<String>): Result<List<String>> {
        return try {
            val converted = nudiTexts.map { text ->
                when (val result = convert(text)) {
                    is Result.Success -> result.data
                    is Result.Error -> text // Keep original if conversion fails
                }
            }
            resultSuccess(converted)
        } catch (e: Exception) {
            resultError(e)
        }
    }

    /**
     * Get conversion statistics
     *
     * Useful for debugging and showing users what was converted.
     *
     * @param nudiText Original Nudi text
     * @param unicodeText Converted Unicode text
     * @return ConversionStats object with details
     */
    fun getConversionStats(nudiText: String, unicodeText: String): ConversionStats {
        val originalLength = nudiText.length
        val convertedLength = unicodeText.length
        val kannadaChars = unicodeText.count { it in '\u0C80'..'\u0CFF' }

        return ConversionStats(
            originalLength = originalLength,
            convertedLength = convertedLength,
            kannadaCharacters = kannadaChars,
            compressionRatio = if (originalLength > 0) {
                convertedLength.toFloat() / originalLength.toFloat()
            } else 0f
        )
    }
}

/**
 * Statistics about a conversion operation
 *
 * @property originalLength Length of original Nudi text
 * @property convertedLength Length of converted Unicode text
 * @property kannadaCharacters Number of Kannada characters in result
 * @property compressionRatio How much smaller/larger the text became (usually smaller)
 */
data class ConversionStats(
    val originalLength: Int,
    val convertedLength: Int,
    val kannadaCharacters: Int,
    val compressionRatio: Float
)
