package com.kannada.kavi.core.ime

import android.view.inputmethod.InputConnection

/**
 * InputConnectionHandler - Manages Text Input and Deletion
 *
 * This class is like a messenger between our keyboard and the text field.
 * When you type "A", this class delivers "A" to the text field.
 * When you press delete, this class removes the last character.
 *
 * WHAT IS InputConnection?
 * ========================
 * InputConnection is Android's way of letting keyboards communicate with text fields.
 * Think of it like a phone line:
 * - Keyboard (us) ← → InputConnection ← → Text Field (app)
 *
 * We can:
 * - Send text to the field (commitText)
 * - Delete text (deleteSurroundingText)
 * - Get text before cursor (getTextBeforeCursor)
 * - Get text after cursor (getTextAfterCursor)
 * - Move cursor (setSelection)
 *
 * WHY A SEPARATE CLASS?
 * =====================
 * We separate this logic so that:
 * 1. InputMethodService doesn't get too complex
 * 2. We can easily test text input/deletion
 * 3. We can add features like undo/redo later
 * 4. Code is cleaner and more organized
 */
class InputConnectionHandler {

    // The current connection to the text field
    // Can be null if no text field is active
    private var inputConnection: InputConnection? = null

    /**
     * Set the active input connection
     * Called when user taps a text field
     */
    fun setInputConnection(ic: InputConnection) {
        this.inputConnection = ic
    }

    /**
     * Clear the input connection
     * Called when user leaves the text field
     */
    fun clearInputConnection() {
        this.inputConnection = null
    }

    /**
     * Commit text to the text field
     *
     * This is like typing a character on a physical keyboard.
     * The text appears at the cursor position.
     *
     * @param text The text to insert (can be single char or multiple chars)
     *
     * Examples:
     * commitText("a") → types "a"
     * commitText("ನಮಸ್ಕಾರ") → types "ನಮಸ್ಕಾರ"
     * commitText("\n") → inserts new line
     */
    fun commitText(text: String) {
        val ic = inputConnection ?: return

        // commitText parameters:
        // 1. The text to insert
        // 2. New cursor position (1 = after the text)
        ic.commitText(text, 1)
    }

    /**
     * Delete text before the cursor
     *
     * This is like pressing the backspace key.
     * Deletes the character immediately before the cursor.
     *
     * @param count Number of characters to delete (default = 1)
     *
     * Examples:
     * deleteText() → deletes 1 character
     * deleteText(5) → deletes 5 characters
     */
    fun deleteText(count: Int = 1) {
        val ic = inputConnection ?: return

        if (count <= 0) return

        // First, check if there's selected text
        val selectedText = getSelectedText()
        if (selectedText != null && selectedText.isNotEmpty()) {
            // Delete the selected text
            ic.commitText("", 1)
            return
        }

        // No selection, delete characters before cursor
        // deleteSurroundingText parameters:
        // 1. Number of characters to delete before cursor
        // 2. Number of characters to delete after cursor
        ic.deleteSurroundingText(count, 0)
    }

    /**
     * Delete word before cursor
     *
     * Like Ctrl+Backspace on a computer keyboard.
     * Deletes the entire word before the cursor.
     *
     * Example:
     * "Hello world|" → deleteWord() → "Hello |"
     */
    fun deleteWord() {
        val ic = inputConnection ?: return

        // Get text before cursor
        val beforeCursor = ic.getTextBeforeCursor(100, 0) ?: return

        if (beforeCursor.isEmpty()) return

        // Find the last word boundary (space, punctuation, etc.)
        var deleteCount = 0
        var foundWord = false

        // Start from the end and work backwards
        for (i in beforeCursor.length - 1 downTo 0) {
            val char = beforeCursor[i]

            if (char.isWhitespace() || char in ".,!?;:") {
                if (foundWord) {
                    // We've found a complete word, stop here
                    break
                }
                // Still in whitespace/punctuation, keep counting
                deleteCount++
            } else {
                // We're in a word
                foundWord = true
                deleteCount++
            }
        }

        if (deleteCount > 0) {
            ic.deleteSurroundingText(deleteCount, 0)
        }
    }

    /**
     * Get text before the cursor
     *
     * Useful for:
     * - Auto-correction (what did user just type?)
     * - Suggestions (what word are they typing?)
     * - Context-aware features
     *
     * @param length Maximum number of characters to get
     * @return The text before cursor, or null if unavailable
     */
    fun getTextBeforeCursor(length: Int = 50): String? {
        val ic = inputConnection ?: return null
        return ic.getTextBeforeCursor(length, 0)?.toString()
    }

    /**
     * Get text after the cursor
     *
     * @param length Maximum number of characters to get
     * @return The text after cursor, or null if unavailable
     */
    fun getTextAfterCursor(length: Int = 50): String? {
        val ic = inputConnection ?: return null
        return ic.getTextAfterCursor(length, 0)?.toString()
    }

    /**
     * Get currently selected text
     *
     * If user has selected text (like you do on a computer with mouse),
     * this returns that selected text.
     *
     * @return The selected text, or null if no selection
     */
    fun getSelectedText(): String? {
        val ic = inputConnection ?: return null
        return ic.getSelectedText(0)?.toString()
    }

    /**
     * Get the word currently being typed
     *
     * Very useful for suggestions!
     * If user typed "hel" in "Hello", this returns "hel"
     *
     * @return The current word, or empty string
     */
    fun getCurrentWord(): String {
        val beforeCursor = getTextBeforeCursor(100) ?: return ""

        if (beforeCursor.isEmpty()) return ""

        // Find the start of the current word
        var wordStart = beforeCursor.length - 1

        while (wordStart >= 0) {
            val char = beforeCursor[wordStart]

            // Stop at word boundaries
            if (char.isWhitespace() || char in ".,!?;:") {
                wordStart++
                break
            }

            wordStart--
        }

        // Handle edge case: reached beginning
        if (wordStart < 0) wordStart = 0

        return beforeCursor.substring(wordStart)
    }

    /**
     * Move cursor to specific position
     *
     * @param position The position to move to
     */
    fun setCursorPosition(position: Int) {
        val ic = inputConnection ?: return
        ic.setSelection(position, position)
    }

    /**
     * Select text
     *
     * @param start Start position
     * @param end End position
     */
    fun selectText(start: Int, end: Int) {
        val ic = inputConnection ?: return
        ic.setSelection(start, end)
    }

    /**
     * Send a key event
     *
     * Some apps expect key events instead of commitText.
     * This is less common but sometimes necessary.
     *
     * @param keyCode The key code (from KeyEvent)
     */
    fun sendKeyEvent(keyCode: Int) {
        val ic = inputConnection ?: return

        // Send key down and key up events
        ic.sendKeyEvent(
            android.view.KeyEvent(
                android.view.KeyEvent.ACTION_DOWN,
                keyCode
            )
        )
        ic.sendKeyEvent(
            android.view.KeyEvent(
                android.view.KeyEvent.ACTION_UP,
                keyCode
            )
        )
    }

    /**
     * Start a batch edit
     *
     * When making multiple changes, wrap them in beginBatchEdit/endBatchEdit
     * for better performance. Think of it like "save changes" at the end
     * instead of saving after each keystroke.
     */
    fun beginBatchEdit() {
        inputConnection?.beginBatchEdit()
    }

    /**
     * End a batch edit
     */
    fun endBatchEdit() {
        inputConnection?.endBatchEdit()
    }

    /**
     * Perform a batch of operations
     *
     * Automatically wraps operations in batch edit.
     *
     * Example:
     * ```
     * performBatchEdit {
     *     commitText("Hello")
     *     commitText(" ")
     *     commitText("World")
     * }
     * ```
     */
    inline fun performBatchEdit(operations: () -> Unit) {
        beginBatchEdit()
        try {
            operations()
        } finally {
            endBatchEdit()
        }
    }

    /**
     * Check if input connection is active
     */
    fun isActive(): Boolean {
        return inputConnection != null
    }

    /**
     * Replace text
     *
     * Deletes old text and inserts new text.
     * Useful for auto-correction.
     *
     * @param oldText The text to replace
     * @param newText The new text
     */
    fun replaceText(oldText: String, newText: String) {
        performBatchEdit {
            // Delete old text
            deleteText(oldText.length)
            // Insert new text
            commitText(newText)
        }
    }

    /**
     * Insert text at cursor and select it
     *
     * Useful for inserting suggestions that user can easily delete.
     *
     * @param text The text to insert
     */
    fun insertAndSelectText(text: String) {
        val ic = inputConnection ?: return

        performBatchEdit {
            val cursorPos = getCursorPosition()
            commitText(text)
            ic.setSelection(cursorPos, cursorPos + text.length)
        }
    }

    /**
     * Get current cursor position
     *
     * @return The cursor position, or -1 if unavailable
     */
    private fun getCursorPosition(): Int {
        val ic = inputConnection ?: return -1

        // Get text before cursor
        val beforeCursor = ic.getTextBeforeCursor(1000, 0) ?: return -1

        return beforeCursor.length
    }
}
