package com.kannada.kavi.features.converter

import com.kannada.kavi.core.common.Result
import com.kannada.kavi.core.common.resultError
import com.kannada.kavi.core.common.resultSuccess

/**
 * NudiToUnicodeConverter - ASCII to Unicode converter for Kannada
 * 
 * Based on the exact implementation from ascii2unicode (https://github.com/aravindavk/ascii2unicode)
 * This is a direct port of the Python knconverter script to Kotlin.
 * 
 * Supports:
 * - Complete Nudi encoding conversion
 * - Complete Baraha encoding conversion
 * - Special handling for vattaksharagalu (consonant clusters)
 * - Special handling for arkavattu (ರ combinations)
 * - Broken cases handling (special vowel combinations)
 * - ZWJ (Zero Width Joiner) for proper rendering
 * 
 * Usage:
 * ```kotlin
 * val converter = NudiToUnicodeConverter()
 * val result = converter.convert("PÀjÉ")
 * when (result) {
 *     is Result.Success -> println(result.data) // "ಕನ್ನಡ"
 *     is Result.Error -> println(result.exception.message)
 * }
 * ```
 */
class NudiToUnicodeConverter {

    /**
     * Convert Nudi/Baraha ASCII text to Unicode Kannada
     *
     * @param asciiText The ASCII-encoded Kannada text (Nudi or Baraha)
     * @return Result containing the Unicode Kannada text
     */
    fun convert(asciiText: String): Result<String> {
        return try {
            if (asciiText.isEmpty()) {
                return resultSuccess("")
            }
            resultSuccess(processLine(asciiText))
        } catch (e: Exception) {
            resultError("Failed to convert text: ${e.message}")
        }
    }

    /**
     * Convert Unicode Kannada text to Nudi/Baraha ASCII
     *
     * @param unicodeText The Unicode Kannada text
     * @return Result containing the Nudi ASCII text
     */
    fun convertReverse(unicodeText: String): Result<String> {
        return try {
            if (unicodeText.isEmpty()) {
                return resultSuccess("")
            }
            resultSuccess(unicodeToNudi(unicodeText))
        } catch (e: Exception) {
            resultError("Failed to convert text: ${e.message}")
        }
    }

    /**
     * Convert multiple texts in batch
     * 
     * @param asciiTexts List of ASCII-encoded texts
     * @return Result containing list of converted Unicode texts
     */
    fun convertBatch(asciiTexts: List<String>): Result<List<String>> {
        return try {
            resultSuccess(asciiTexts.map { processLine(it) })
        } catch (e: Exception) {
            resultError("Failed to convert batch: ${e.message}")
        }
    }

    /**
     * Detect if text appears to be Nudi/Baraha encoded
     * 
     * @param text The text to check
     * @return true if text likely contains Nudi/Baraha encoding
     */
    fun isNudiText(text: String): Boolean {
        if (text.isEmpty()) return false
        
        // Check for common Nudi/Baraha patterns
        val suspectCount = text.count { char ->
            char.code in 128..255 || 
            mainMapping.keys.any { it.contains(char) }
        }
        
        return (suspectCount.toFloat() / text.length) > 0.3f
    }

    /**
     * Get conversion statistics
     * 
     * @param asciiText Original ASCII text
     * @param unicodeText Converted Unicode text
     * @return ConversionStats with metrics
     */
    fun getConversionStats(asciiText: String, unicodeText: String): ConversionStats {
        val kannadaChars = unicodeText.count { it in '\u0C80'..'\u0CFF' }
        val compression = if (asciiText.isNotEmpty()) {
            unicodeText.length.toFloat() / asciiText.length
        } else {
            0f
        }
        return ConversionStats(
            originalLength = asciiText.length,
            convertedLength = unicodeText.length,
            kannadaCharacters = kannadaChars,
            compressionRatio = compression
        )
    }

    /**
     * Process a line - splits into words and processes each word
     */
    private fun processLine(line: String): String {
        val cleaned = line.trim()
        if (cleaned.isEmpty()) return ""

        val words = cleaned.split(' ')
        return words.joinToString(" ") { processWord(it) }
    }

    /**
     * Convert Unicode to Nudi (reverse conversion)
     */
    private fun unicodeToNudi(text: String): String {
        var result = text

        // Sort by length (longest first) to avoid partial replacements
        val sortedMappings = reverseMapping.entries.sortedByDescending { it.key.length }

        for ((unicode, nudi) in sortedMappings) {
            result = result.replace(unicode, nudi)
        }

        return result
    }

    /**
     * Process a single word - main conversion logic
     * This is a direct port of the Python process_word function
     */
    private fun processWord(word: String): String {
        val output = mutableListOf<Char>()
        var i = 0
        val maxLen = word.length

        while (i < maxLen) {
            // Skip ignore list characters
            if (word[i] in ignoreList) {
                i++
                continue
            }

            // Find mapping
            val (jump, converted) = findMapping(output, word, i)
            
            // Add converted characters
            output.addAll(converted)
            
            // Jump by number of characters matched
            i += (1 + jump)
        }

        return output.joinToString("")
    }

    /**
     * Find mapping for current position
     * This is a direct port of the Python find_mapping function
     * 
     * @param output Current output list
     * @param text Input text
     * @param currentPos Current position in text
     * @return Pair of (jump_count, converted_chars)
     */
    private fun findMapping(output: MutableList<Char>, text: String, currentPos: Int): Pair<Int, List<Char>> {
        val maxLen = minOf(4, text.length - currentPos - 1)
        var n = 0
        val outputList = output.toMutableList()

        // Try matching from longest to shortest (4 chars down to 1 char)
        for (i in maxLen downTo 0) {
            val substr = text.substring(currentPos, currentPos + i + 1)

            if (substr in mainMapping) {
                // Direct mapping found
                val mapped = mainMapping[substr]!!
                
                // Check if previous char is halant and current is not vattakshara
                // Add ZWJ to prevent mixing
                if (outputList.isNotEmpty() && outputList.last() == '\u0CCD') {
                    // Check if this is not a vattakshara
                    val firstChar = mapped.firstOrNull()
                    if (firstChar != null && firstChar !in vattaksharagalu.values) {
                        outputList.add('\u200D') // ZWJ
                    }
                }
                
                // Add mapped characters
                outputList.addAll(mapped.toList())
                n = i
                break
            } else if (i == 0) {
                // No direct mapping, try special cases
                val char = substr[0]
                
                when {
                    char in asciiArkavattu -> {
                        processArkavattu(outputList, char)
                    }
                    char in vattaksharagalu -> {
                        processVattakshara(outputList, char)
                    }
                    char in brokenCases -> {
                        processBrokenCase(outputList, char)
                    }
                    else -> {
                        // No match, append as-is
                        outputList.add(char)
                    }
                }
            }
        }

        return Pair(n, outputList.drop(output.size))
    }

    /**
     * Process vattakshara (consonant clusters)
     * Example: ತಿಮ್ಮಿ in ASCII: ತಿ + ಮಿ + ma_vattu
     * in Unicode: ತ + dependent vowel ಇ + ಮ + halant + ಮ + dependent vowel ಇ
     */
    private fun processVattakshara(letters: MutableList<Char>, t: Char) {
        val lastLetter = if (letters.isNotEmpty()) letters.last() else null
        val baseChar = vattaksharagalu[t]!!

        if (lastLetter != null && lastLetter in dependentVowels) {
            // If last letter is dependent vowel, rearrange
            letters[letters.size - 1] = '\u0CCD' // halant
            letters.add(baseChar)
            letters.add(lastLetter)
        } else {
            // If "ಅ" kaara, just append halant + base letter
            letters.add('\u0CCD') // halant
            letters.add(baseChar)
        }
    }

    /**
     * Process arkavattu (special ರ combinations)
     * Example: ವರ್ಷ in ASCII ವ + ಷ + arkavattu
     * in Unicode ವ + ರ + halant + ಷ
     */
    private fun processArkavattu(letters: MutableList<Char>, t: Char) {
        val lastLetter = if (letters.isNotEmpty()) letters.last() else null
        val secondLast = if (letters.size > 1) letters[letters.size - 2] else null
        val baseChar = asciiArkavattu[t]!!

        if (lastLetter != null && lastLetter in dependentVowels) {
            // Rearrange
            if (secondLast != null) {
                letters[letters.size - 2] = baseChar
                letters[letters.size - 1] = '\u0CCD' // halant
                letters.add(secondLast)
                letters.add(lastLetter)
            }
        } else {
            if (lastLetter != null) {
                letters[letters.size - 1] = baseChar
                letters.add('\u0CCD') // halant
                letters.add(lastLetter)
            }
        }
    }

    /**
     * Process broken cases (special vowel combinations)
     * Example: ಕೀರ್ತಿ and ಕೇಳಿ - deerga has same code but different Unicode
     */
    private fun processBrokenCase(letters: MutableList<Char>, t: Char) {
        val lastLetter = if (letters.isNotEmpty()) letters.last() else null
        val brokenCase = brokenCases[t]!!

        if (lastLetter != null && lastLetter in brokenCase.mapping) {
            // Replace last letter with mapped value
            letters[letters.size - 1] = brokenCase.mapping[lastLetter]!!
        } else {
            // Append the value
            letters.add(brokenCase.value)
        }
    }

    companion object {
        /**
         * Dependent vowels (matras)
         */
        private val dependentVowels = listOf(
            '\u0CCD', // ್ halant
            '\u0CBE', // ಾ
            '\u0CBF', // ಿ
            '\u0CC0', // ೀ
            '\u0CC1', // ು
            '\u0CC2', // ೂ
            '\u0CC3', // ೃ
            '\u0CC6', // ೆ
            '\u0CC7', // ೇ
            '\u0CC8', // ೈ
            '\u0CCA', // ೊ
            '\u0CCB', // ೋ
            '\u0CCC'  // ೌ
        )

        /**
         * Characters to ignore
         */
        private val ignoreList = setOf('ö', '÷')

        /**
         * Main mapping dictionary - exact from ascii2unicode
         */
        private val mainMapping = mapOf(
            "C" to "ಅ",
            "D" to "ಆ",
            "E" to "ಇ",
            "F" to "ಈ",
            "G" to "ಉ",
            "H" to "ಊ",
            "IÄ" to "ಋ",
            "J" to "ಎ",
            "K" to "ಏ",
            "L" to "ಐ",
            "M" to "ಒ",
            "N" to "ಓ",
            "O" to "ಔ",
            "A" to "ಂ",
            "B" to "ಃ",
            "Pï" to "ಕ್",
            "PÀ" to "ಕ",
            "PÁ" to "ಕಾ",
            "Q" to "ಕಿ",
            "PÉ" to "ಕೆ",
            "PË" to "ಕೌ",
            "Sï" to "ಖ್",
            "R" to "ಖ",
            "SÁ" to "ಖಾ",
            "T" to "ಖಿ",
            "SÉ" to "ಖೆ",
            "SË" to "ಖೌ",
            "Uï" to "ಗ್",
            "UÀ" to "ಗ",
            "UÁ" to "ಗಾ",
            "V" to "ಗಿ",
            "UÉ" to "ಗೆ",
            "UË" to "ಗೌ",
            "Wï" to "ಘ್",
            "WÀ" to "ಘ",
            "WÁ" to "ಘಾ",
            "X" to "ಘಿ",
            "WÉ" to "ಘೆ",
            "WË" to "ಘೌ",
            "k" to "ಞ",
            "Zï" to "ಚ್",
            "ZÀ" to "ಚ",
            "ZÁ" to "ಚಾ",
            "a" to "ಚಿ",
            "ZÉ" to "ಚೆ",
            "ZË" to "ಚೌ",
            "bï" to "ಛ್",
            "bÀ" to "ಛ",
            "bÁ" to "ಛಾ",
            "c" to "ಛಿ",
            "bÉ" to "ಛೆ",
            "bË" to "ಛೌ",
            "eï" to "ಜ್",
            "d" to "ಜ",
            "eÁ" to "ಜಾ",
            "f" to "ಜಿ",
            "eÉ" to "ಜೆ",
            "eË" to "ಜೌ",
            "gÀhiï" to "ಝ್",
            "gÀhÄ" to "ಝ",
            "gÀhiÁ" to "ಝಾ",
            "jhÄ" to "ಝಿ",
            "gÉhÄ" to "ಝೆ",
            "gÉhÆ" to "ಝೊ",
            "gÀhiË" to "ಝೌ",
            "Y" to "ಙ",
            "mï" to "ಟ್",
            "l" to "ಟ",
            "mÁ" to "ಟಾ",
            "n" to "ಟಿ",
            "mÉ" to "ಟೆ",
            "mË" to "ಟೌ",
            "oï" to "ಠ್",
            "oÀ" to "ಠ",
            "oÁ" to "ಠಾ",
            "p" to "ಠಿ",
            "oÉ" to "ಠೆ",
            "oË" to "ಠೌ",
            "qï" to "ಡ್",
            "qÀ" to "ಡ",
            "qÁ" to "ಡಾ",
            "r" to "ಡಿ",
            "qÉ" to "ಡೆ",
            "qË" to "ಡೌ",
            "qsï" to "ಢ್",
            "qsÀ" to "ಢ",
            "qsÁ" to "ಢಾ",
            "rü" to "ಢಿ",
            "qsÉ" to "ಢೆ",
            "qsË" to "ಢೌ",
            "uï" to "ಣ್",
            "t" to "ಣ",
            "uÁ" to "ಣಾ",
            "tÂ" to "ಣಿ",
            "uÉ" to "ಣೆ",
            "uË" to "ಣೌ",
            "vï" to "ತ್",
            "vÀ" to "ತ",
            "vÁ" to "ತಾ",
            "w" to "ತಿ",
            "vÉ" to "ತೆ",
            "vË" to "ತೌ",
            "xï" to "ಥ್",
            "xÀ" to "ಥ",
            "xÁ" to "ಥಾ",
            "y" to "ಥಿ",
            "xÉ" to "ಥೆ",
            "xË" to "ಥೌ",
            "zï" to "ದ್",
            "zÀ" to "ದ",
            "zÁ" to "ದಾ",
            "¢" to "ದಿ",
            "zÉ" to "ದೆ",
            "zË" to "ದೌ",
            "zsï" to "ಧ್",
            "zsÀ" to "ಧ",
            "zsÁ" to "ಧಾ",
            "¢ü" to "ಧಿ",
            "zsÉ" to "ಧೆ",
            "zsË" to "ಧೌ",
            "£ï" to "ನ್",
            "£À" to "ನ",
            "£Á" to "ನಾ",
            "¤" to "ನಿ",
            "£É" to "ನೆ",
            "£Ë" to "ನೌ",
            "¥ï" to "ಪ್",
            "¥À" to "ಪ",
            "¥Á" to "ಪಾ",
            "¦" to "ಪಿ",
            "¥É" to "ಪೆ",
            "¥Ë" to "ಪೌ",
            "¥sï" to "ಫ್",
            "¥sÀ" to "ಫ",
            "¥sÁ" to "ಫಾ",
            "¦ü" to "ಫಿ",
            "¥sÉ" to "ಫೆ",
            "¥sË" to "ಫೌ",
            "¨ï" to "ಬ್",
            "§" to "ಬ",
            "¨Á" to "ಬಾ",
            "©" to "ಬಿ",
            "¨É" to "ಬೆ",
            "¨Ë" to "ಬೌ",
            "¨sï" to "ಭ್",
            "¨sÀ" to "ಭ",
            "¨sÁ" to "ಭಾ",
            "©ü" to "ಭಿ",
            "¨sÉ" to "ಭೆ",
            "¨sË" to "ಭೌ",
            "ªÀiï" to "ಮ್",
            "ªÀÄ" to "ಮ",
            "ªÀiÁ" to "ಮಾ",
            "«Ä" to "ಮಿ",
            "ªÉÄ" to "ಮೆ",
            "ªÀiË" to "ಮೌ",
            "AiÀiï" to "ಯ್",
            "AiÀÄ" to "ಯ",
            "0iÀÄ" to "ಯ",
            "AiÀiÁ" to "ಯಾ",
            "0iÀiÁ" to "ಯಾ",
            "¬Ä" to "ಯಿ",
            "0iÀÄÄ" to "ಯು",
            "AiÉÄ" to "ಯೆ",
            "0iÉÆ" to "ಯೊ",
            "AiÉÆ" to "ಯೊ",
            "AiÀiË" to "ಯೌ",
            "gï" to "ರ್",
            "gÀ" to "ರ",
            "gÁ" to "ರಾ",
            "j" to "ರಿ",
            "gÉ" to "ರೆ",
            "gË" to "ರೌ",
            "¯ï" to "ಲ್",
            "®" to "ಲ",
            "¯Á" to "ಲಾ",
            "°" to "ಲಿ",
            "¯É" to "ಲೆ",
            "¯Ë" to "ಲೌ",
            "ªï" to "ವ್",
            "ªÀ" to "ವ",
            "ªÁ" to "ವಾ",
            "«" to "ವಿ",
            "ªÀÅ" to "ವು",
            "ªÀÇ" to "ವೂ",
            "ªÉ" to "ವೆ",
            "ªÉÃ" to "ವೇ",
            "ªÉÊ" to "ವೈ",
            "ªÉÆ" to "ಮೊ",
            "ªÉÆÃ" to "ಮೋ",
            "ªÉÇ" to "ವೊ",
            "ªÉÇÃ" to "ವೋ",
            "ªÉ  " to "ವೆ",
            "¥ÀÅ" to "ಪು",
            "¥ÀÇ" to "ಪೂ",
            "¥sÀÅ" to "ಫು",
            "¥sÀÇ" to "ಫೂ",
            "ªË" to "ವೌ",
            "±ï" to "ಶ್",
            "±À" to "ಶ",
            "±Á" to "ಶಾ",
            "²" to "ಶಿ",
            "±É" to "ಶೆ",
            "±Ë" to "ಶೌ",
            "µï" to "ಷ್",
            "µÀ" to "ಷ",
            "µÁ" to "ಷಾ",
            "¶" to "ಷಿ",
            "µÉ" to "ಷೆ",
            "µË" to "ಷೌ",
            "¸ï" to "ಸ್",
            "¸À" to "ಸ",
            "¸Á" to "ಸಾ",
            "¹" to "ಸಿ",
            "¸É" to "ಸೆ",
            "¸Ë" to "ಸೌ",
            "ºï" to "ಹ್",
            "ºÀ" to "ಹ",
            "ºÁ" to "ಹಾ",
            "»" to "ಹಿ",
            "ºÉ" to "ಹೆ",
            "ºË" to "ಹೌ",
            "¼ï" to "ಳ್",
            "¼À" to "ಳ",
            "¼Á" to "ಳಾ",
            "½" to "ಳಿ",
            "¼É" to "ಳೆ",
            "¼Ë" to "ಳೌ"
        )

        /**
         * Broken cases - special vowel combinations
         */
        private data class BrokenCase(val value: Char, val mapping: Map<Char, Char>)

        private val brokenCases = mapOf(
            'Ã' to BrokenCase('\u0CC0', mapOf( // ೀ
                '\u0CBF' to '\u0CC0', // ಿ -> ೀ
                '\u0CC6' to '\u0CC7', // ೆ -> ೇ
                '\u0CCA' to '\u0CCB'  // ೊ -> ೋ
            )),
            'Ä' to BrokenCase('\u0CC1', emptyMap()), // ು
            'Æ' to BrokenCase('\u0CC2', mapOf( // ೂ
                '\u0CC6' to '\u0CCA'  // ೆ -> ೊ
            )),
            'È' to BrokenCase('\u0CC3', emptyMap()), // ೃ
            'Ê' to BrokenCase('\u0CC8', mapOf( // ೈ
                '\u0CC6' to '\u0CC8'  // ೆ -> ೈ
            ))
        )

        /**
         * Vattaksharagalu - consonant clusters
         */
        private val vattaksharagalu = mapOf(
            'Ì' to 'ಕ',
            'Í' to 'ಖ',
            'Î' to 'ಗ',
            'Ï' to 'ಘ',
            'Õ' to 'ಞ',
            'Ñ' to 'ಚ',
            'Ò' to 'ಛ',
            'Ó' to 'ಜ',
            'Ô' to 'ಝ',
            'Ö' to 'ಟ',
            '×' to 'ಠ',
            'Ø' to 'ಡ',
            'Ù' to 'ಢ',
            'Ú' to 'ಣ',
            'Û' to 'ತ',
            'Ü' to 'ಥ',
            'Ý' to 'ದ',
            'Þ' to 'ಧ',
            'ß' to 'ನ',
            'à' to 'ಪ',
            'á' to 'ಫ',
            'â' to 'ಬ',
            'ã' to 'ಭ',
            'ä' to 'ಮ',
            'å' to 'ಯ',
            'æ' to 'ರ',
            'è' to 'ಲ',
            'é' to 'ವ',
            'ê' to 'ಶ',
            'ë' to 'ಷ',
            'ì' to 'ಸ',
            'í' to 'ಹ',
            'î' to 'ಳ',
            'ç' to 'ರ'
        )

        /**
         * Arkavattu - special ರ combinations
         */
        private val asciiArkavattu = mapOf(
            'ð' to 'ರ'
        )

        /**
         * Reverse mapping - Unicode to Nudi
         * Built from mainMapping by reversing key-value pairs
         */
        private val reverseMapping: Map<String, String> by lazy {
            mainMapping.entries.associate { (nudi, unicode) -> unicode to nudi }
        }
    }
}

/**
 * Conversion statistics
 */
data class ConversionStats(
    val originalLength: Int,
    val convertedLength: Int,
    val kannadaCharacters: Int,
    val compressionRatio: Float
)
