package com.kannada.kavi.features.clipboard.models

import android.os.Parcelable
import com.kannada.kavi.core.common.ClipboardContentType
import kotlinx.parcelize.Parcelize

/**
 * ClipboardItem - A Copied Text Entry
 *
 * Represents one item in clipboard history.
 * Like a receipt of what you copied!
 *
 * WHAT IS CLIPBOARD?
 * ==================
 * When you copy text (Ctrl+C), it goes to clipboard.
 * Normal clipboard holds only ONE item.
 * Our clipboard history holds FIFTY items!
 *
 * EXAMPLE:
 * ========
 * You copy: "Hello" → saved to clipboard history
 * You copy: "World" → also saved
 * You copy: "Kannada" → also saved
 * Now you have 3 items in history!
 * You can paste any of them anytime.
 *
 * WHY CLIPBOARD HISTORY?
 * ======================
 * - Normal clipboard: Copy new text → lose old text
 * - Our clipboard: Copy new text → keep all old text!
 * - Super useful for:
 *   * Copying multiple items
 *   * Referencing old copies
 *   * Productivity workflows
 *
 * FEATURES:
 * =========
 * - Stores up to 50 items
 * - Each item has:
 *   * The text itself
 *   * When it was copied (timestamp)
 *   * Unique ID
 *   * Preview for long text
 * - Automatically removes oldest when full
 */
@Parcelize
data class ClipboardItem(
    /**
     * Unique identifier
     *
     * Used to track and delete specific items
     */
    val id: String,

    /**
     * The copied text
     *
     * Can be any length, but we'll show preview in UI
     */
    val text: String,

    /**
     * When was this copied?
     *
     * Timestamp in milliseconds (System.currentTimeMillis())
     * Used for sorting (newest first) and display
     */
    val timestamp: Long,

    /**
     * Is this item pinned?
     *
     * Pinned items never get auto-deleted
     * Useful for frequently used snippets
     */
    val isPinned: Boolean = false,

    /**
     * Source app (optional)
     *
     * Which app did the text come from?
     * Example: "Chrome", "WhatsApp", "Notes"
     */
    val sourceApp: String? = null,

    /**
     * Type of content (future feature)
     *
     * - TEXT: Plain text
     * - URL: Web link
     * - EMAIL: Email address
     * - PHONE: Phone number
     */
    val contentType: ClipboardContentType = ClipboardContentType.TEXT

) : Parcelable {

    /**
     * Get short preview of text
     *
     * Long text → truncate to 100 characters
     *
     * @param maxLength Maximum characters (default 100)
     * @return Truncated text with "..." if needed
     */
    fun getPreview(maxLength: Int = 100): String {
        return if (text.length > maxLength) {
            text.substring(0, maxLength) + "..."
        } else {
            text
        }
    }

    /**
     * Get first line only
     *
     * Useful for list display
     *
     * @return First line of text
     */
    fun getFirstLine(): String {
        return text.lines().firstOrNull() ?: ""
    }

    /**
     * How many lines does this text have?
     *
     * @return Line count
     */
    fun getLineCount(): Int {
        return text.lines().size
    }

    /**
     * Is this a long text?
     *
     * @return true if more than 100 characters
     */
    fun isLongText(): Boolean {
        return text.length > 100
    }

    /**
     * Is this a multi-line text?
     *
     * @return true if has newlines
     */
    fun isMultiLine(): Boolean {
        return text.contains('\n')
    }

    /**
     * Get age of this clipboard item
     *
     * @return Milliseconds since copied
     */
    fun getAge(): Long {
        return System.currentTimeMillis() - timestamp
    }

    /**
     * Get human-readable time
     *
     * Examples:
     * - "Just now" (< 1 min)
     * - "5 min ago"
     * - "2 hours ago"
     * - "Yesterday"
     * - "3 days ago"
     *
     * @return Relative time string
     */
    fun getRelativeTime(): String {
        val age = getAge()
        val seconds = age / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            days == 1L -> "Yesterday"
            days < 7 -> "$days days ago"
            else -> {
                val weeks = days / 7
                "$weeks week${if (weeks > 1) "s" else ""} ago"
            }
        }
    }

    /**
     * Does this text match search query?
     *
     * Case-insensitive search
     *
     * @param query Search term
     * @return true if text contains query
     */
    fun matchesSearch(query: String): Boolean {
        if (query.isEmpty()) return true
        return text.lowercase().contains(query.lowercase())
    }
}

/**
 * Helper function to detect content type
 *
 * @param text Text to analyze
 * @return Detected content type
 */
fun detectContentType(text: String): ClipboardContentType {
    val trimmed = text.trim()

    return when {
        // URL detection
        trimmed.startsWith("http://") ||
        trimmed.startsWith("https://") ||
        trimmed.startsWith("www.") -> ClipboardContentType.URL

        // Email detection
        trimmed.contains("@") &&
        trimmed.contains(".") &&
        !trimmed.contains(" ") -> ClipboardContentType.EMAIL

        // Phone detection (basic)
        trimmed.matches(Regex("^[+\\d\\s()-]+$")) &&
        trimmed.replace(Regex("[^\\d]"), "").length >= 10 -> ClipboardContentType.PHONE

        // Code detection (has common code patterns)
        trimmed.contains("{") && trimmed.contains("}") ||
        trimmed.contains("function") ||
        trimmed.contains("class ") ||
        trimmed.contains("import ") -> ClipboardContentType.CODE

        // Default to text
        else -> ClipboardContentType.TEXT
    }
}

/**
 * USAGE EXAMPLE:
 * ==============
 *
 * Creating clipboard items:
 * ```kotlin
 * val item1 = ClipboardItem(
 *     id = UUID.randomUUID().toString(),
 *     text = "Hello World",
 *     timestamp = System.currentTimeMillis(),
 *     isPinned = false
 * )
 *
 * val item2 = ClipboardItem(
 *     id = UUID.randomUUID().toString(),
 *     text = "https://github.com",
 *     timestamp = System.currentTimeMillis(),
 *     contentType = detectContentType("https://github.com") // URL
 * )
 *
 * // Get preview
 * val preview = item1.getPreview(50) // Max 50 chars
 *
 * // Get relative time
 * val time = item1.getRelativeTime() // "Just now", "5 min ago", etc.
 *
 * // Search
 * val matches = item1.matchesSearch("hello") // true
 * ```
 *
 * STORAGE:
 * ========
 * - In-memory: List<ClipboardItem> (fast, lost on close)
 * - Database: Room (persistent, survives restart)
 * - Preference: Last 5 items only (super fast access)
 */
