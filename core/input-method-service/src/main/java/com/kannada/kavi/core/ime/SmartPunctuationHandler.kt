package com.kannada.kavi.core.ime

/**
 * SmartPunctuationHandler - Automatically convert quotes and dashes
 *
 * Converts:
 * - Straight quotes (" and ') to curly quotes (" " and ' ')
 * - Double dash (--) to em-dash (—)
 * - Three dots (...) to ellipsis (…)
 *
 * This improves typography automatically, making text look more professional.
 */
class SmartPunctuationHandler {

    /**
     * Process text before it's committed to the text field
     * Returns the processed text with smart punctuation applied
     *
     * @param text The text to process
     * @param textBeforeCursor The text before the cursor (for context)
     * @return The processed text with smart punctuation
     */
    fun processPunctuation(text: String, textBeforeCursor: String?): String {
        // Only process single character inputs for performance
        if (text.length != 1) {
            return text
        }

        val char = text[0]
        val contextText = textBeforeCursor ?: ""

        return when (char) {
            '"' -> handleQuote(contextText, isDouble = true)
            '\'' -> handleQuote(contextText, isDouble = false)
            '-' -> handleDash(contextText)
            '.' -> handleEllipsis(contextText)
            else -> text
        }
    }

    /**
     * Handle quote conversion based on context
     * - Opening quote if at start or after whitespace/punctuation
     * - Closing quote if after word character
     */
    private fun handleQuote(contextText: String, isDouble: Boolean): String {
        // Unicode left/right double quotes: \u201C and \u201D
        // Unicode left/right single quotes: \u2018 and \u2019
        val leftDouble = '\u201C'
        val rightDouble = '\u201D'
        val leftSingle = '\u2018'
        val rightSingle = '\u2019'

        if (contextText.isEmpty()) {
            // At the beginning, use opening quote
            return if (isDouble) leftDouble.toString() else leftSingle.toString()
        }

        val lastChar = contextText.last()

        // Check if we should use opening quote
        val shouldBeOpening = lastChar.isWhitespace() ||
                             lastChar in "([{-–—!?:;,." ||
                             lastChar == '"' || lastChar == '\'' ||
                             lastChar == leftDouble || lastChar == rightDouble ||
                             lastChar == leftSingle || lastChar == rightSingle

        return if (isDouble) {
            if (shouldBeOpening) leftDouble.toString() else rightDouble.toString()
        } else {
            if (shouldBeOpening) leftSingle.toString() else rightSingle.toString()
        }
    }

    /**
     * Handle dash conversion
     * - -- becomes — (em-dash)
     * - Single - stays as-is (hyphen)
     */
    private fun handleDash(contextText: String): String {
        // Check if the last character is also a dash
        if (contextText.isNotEmpty() && contextText.last() == '-') {
            // Replace the previous dash with em-dash by returning em-dash
            // The caller should handle deleting the previous dash
            return "—"
        }
        return "-"
    }

    /**
     * Handle ellipsis conversion
     * - ... becomes … (single ellipsis character)
     */
    private fun handleEllipsis(contextText: String): String {
        // Check if the last two characters are dots
        if (contextText.length >= 2) {
            val lastTwo = contextText.substring(contextText.length - 2)
            if (lastTwo == "..") {
                // Replace the previous two dots with ellipsis
                // The caller should handle deleting the previous two dots
                return "…"
            }
        }
        return "."
    }

    /**
     * Check if smart punctuation would apply to the given text
     * Useful for preview/UI feedback
     *
     * @param text The text to check
     * @param textBeforeCursor The context
     * @return True if this text would be transformed
     */
    fun wouldTransform(text: String, textBeforeCursor: String?): Boolean {
        if (text.length != 1) return false

        val char = text[0]
        val processed = processPunctuation(text, textBeforeCursor)
        return processed != text
    }

    /**
     * Get the description of what transformation would occur
     * Useful for showing user hints
     */
    fun getTransformationDescription(text: String, textBeforeCursor: String?): String {
        if (text.length != 1) return ""

        val char = text[0]
        return when (char) {
            '"' -> "Smart quote (opening or closing)"
            '\'' -> "Smart quote (opening or closing)"
            '-' -> "Em-dash if typed after '-'"
            '.' -> "Ellipsis if typed after '..'"
            else -> ""
        }
    }
}
