package com.kannada.kavi.core.common.security

import android.util.Patterns
import java.util.regex.Pattern

/**
 * InputValidator - Input Validation and Sanitization
 *
 * Provides security-focused validation for all user inputs.
 *
 * WHY INPUT VALIDATION?
 * =====================
 * Prevents:
 * - SQL Injection
 * - XSS (Cross-Site Scripting)
 * - Path Traversal
 * - Command Injection
 * - Buffer Overflow
 * - Malicious file uploads
 *
 * VALIDATION PRINCIPLES:
 * ======================
 * 1. **Whitelist over Blacklist**
 *    - Define what IS allowed
 *    - Reject everything else
 *
 * 2. **Fail Securely**
 *    - Invalid input → Reject
 *    - Don't try to "fix" malicious input
 *
 * 3. **Length Limits**
 *    - Prevent buffer overflow
 *    - Prevent DoS attacks
 *
 * 4. **Type Checking**
 *    - Ensure data is expected type
 *    - Validate format
 *
 * USAGE EXAMPLE:
 * ==============
 * ```kotlin
 * // Validate user input before processing
 * val userText = editText.text.toString()
 *
 * if (!InputValidator.isValidText(userText)) {
 *     showError("Invalid input")
 *     return
 * }
 *
 * // Sanitize before storing
 * val sanitized = InputValidator.sanitizeText(userText)
 * saveToDatabase(sanitized)
 * ```
 */
object InputValidator {

    // ========================================
    // CONSTANTS
    // ========================================

    // Maximum lengths to prevent DoS
    const val MAX_TEXT_LENGTH = 10000 // 10k chars for typed text
    const val MAX_WORD_LENGTH = 100 // 100 chars per word
    const val MAX_URL_LENGTH = 2000
    const val MAX_EMAIL_LENGTH = 254 // RFC 5321
    const val MAX_PHONE_LENGTH = 20
    const val MAX_API_KEY_LENGTH = 256
    const val MAX_FILENAME_LENGTH = 255

    // Patterns for validation
    private val SAFE_TEXT_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}\\p{S}]+$")
    private val ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$")
    private val NUMERIC_PATTERN = Pattern.compile("^[0-9]+$")
    private val KANNADA_PATTERN = Pattern.compile("^[\\u0C80-\\u0CFF\\s]+$")

    // SQL Injection patterns (blacklist for extra safety)
    private val SQL_INJECTION_PATTERNS = listOf(
        Pattern.compile("('|(\\-\\-)|(;)|(\\|\\|)|(\\*))"),
        Pattern.compile("\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\bOR\\b.*=|\\bAND\\b.*=)", Pattern.CASE_INSENSITIVE)
    )

    // XSS patterns (blacklist for extra safety)
    private val XSS_PATTERNS = listOf(
        Pattern.compile("<script", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onerror\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onclick\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<iframe", Pattern.CASE_INSENSITIVE)
    )

    // Path traversal patterns
    private val PATH_TRAVERSAL_PATTERNS = listOf(
        Pattern.compile("\\.\\./"),
        Pattern.compile("\\.\\.\\\\"),
        Pattern.compile("^/"),
        Pattern.compile("^[A-Za-z]:\\\\")
    )

    // ========================================
    // TEXT VALIDATION
    // ========================================

    /**
     * Validate general text input
     *
     * Checks:
     * - Not null or blank
     * - Within length limit
     * - No SQL injection patterns
     * - No XSS patterns
     *
     * @param text Text to validate
     * @param maxLength Maximum allowed length (default: MAX_TEXT_LENGTH)
     * @return true if valid
     */
    fun isValidText(text: String?, maxLength: Int = MAX_TEXT_LENGTH): Boolean {
        if (text.isNullOrBlank()) return false
        if (text.length > maxLength) return false

        // Check for SQL injection
        if (containsSqlInjection(text)) return false

        // Check for XSS
        if (containsXss(text)) return false

        return true
    }

    /**
     * Validate Kannada text
     *
     * Only allows Kannada Unicode characters and whitespace
     */
    fun isValidKannadaText(text: String?, maxLength: Int = MAX_TEXT_LENGTH): Boolean {
        if (text.isNullOrBlank()) return false
        if (text.length > maxLength) return false

        return KANNADA_PATTERN.matcher(text).matches()
    }

    /**
     * Validate word (single word, no spaces)
     */
    fun isValidWord(word: String?, maxLength: Int = MAX_WORD_LENGTH): Boolean {
        if (word.isNullOrBlank()) return false
        if (word.length > maxLength) return false
        if (word.contains(" ")) return false

        return isValidText(word, maxLength)
    }

    /**
     * Validate alphanumeric string
     */
    fun isAlphanumeric(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return ALPHANUMERIC_PATTERN.matcher(text).matches()
    }

    /**
     * Validate numeric string
     */
    fun isNumeric(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return NUMERIC_PATTERN.matcher(text).matches()
    }

    // ========================================
    // SPECIFIC FORMAT VALIDATION
    // ========================================

    /**
     * Validate email address
     */
    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        if (email.length > MAX_EMAIL_LENGTH) return false

        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validate URL
     */
    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        if (url.length > MAX_URL_LENGTH) return false

        return Patterns.WEB_URL.matcher(url).matches()
    }

    /**
     * Validate phone number
     */
    fun isValidPhoneNumber(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false
        if (phone.length > MAX_PHONE_LENGTH) return false

        return Patterns.PHONE.matcher(phone).matches()
    }

    /**
     * Validate API key format
     *
     * Only alphanumeric and hyphens/underscores
     */
    fun isValidApiKey(apiKey: String?): Boolean {
        if (apiKey.isNullOrBlank()) return false
        if (apiKey.length > MAX_API_KEY_LENGTH) return false

        return Pattern.compile("^[a-zA-Z0-9_-]+$").matcher(apiKey).matches()
    }

    /**
     * Validate filename
     *
     * No path traversal, special characters
     */
    fun isValidFilename(filename: String?): Boolean {
        if (filename.isNullOrBlank()) return false
        if (filename.length > MAX_FILENAME_LENGTH) return false

        // Check for path traversal
        if (containsPathTraversal(filename)) return false

        // Only allow safe characters
        return Pattern.compile("^[a-zA-Z0-9._-]+$").matcher(filename).matches()
    }

    /**
     * Validate integer within range
     */
    fun isValidInteger(value: Int, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Boolean {
        return value in min..max
    }

    /**
     * Validate long within range
     */
    fun isValidLong(value: Long, min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE): Boolean {
        return value in min..max
    }

    // ========================================
    // SANITIZATION
    // ========================================

    /**
     * Sanitize text for safe storage
     *
     * - Trims whitespace
     * - Removes null characters
     * - Limits length
     *
     * @param text Text to sanitize
     * @param maxLength Maximum length to enforce
     * @return Sanitized text
     */
    fun sanitizeText(text: String?, maxLength: Int = MAX_TEXT_LENGTH): String {
        if (text == null) return ""

        return text
            .trim()
            .replace("\u0000", "") // Remove null bytes
            .take(maxLength) // Enforce max length
    }

    /**
     * Sanitize filename
     *
     * Removes dangerous characters and path traversal
     */
    fun sanitizeFilename(filename: String?): String {
        if (filename == null) return ""

        return filename
            .replace(Regex("[^a-zA-Z0-9._-]"), "_") // Replace unsafe chars
            .take(MAX_FILENAME_LENGTH)
    }

    /**
     * Sanitize for HTML display
     *
     * Escapes HTML special characters
     */
    fun sanitizeHtml(text: String?): String {
        if (text == null) return ""

        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }

    /**
     * Sanitize for SQL (escape single quotes)
     *
     * NOTE: Prefer parameterized queries over manual escaping!
     */
    fun sanitizeSql(text: String?): String {
        if (text == null) return ""

        return text.replace("'", "''")
    }

    // ========================================
    // SECURITY CHECKS
    // ========================================

    /**
     * Check for SQL injection patterns
     */
    fun containsSqlInjection(text: String): Boolean {
        return SQL_INJECTION_PATTERNS.any { pattern ->
            pattern.matcher(text).find()
        }
    }

    /**
     * Check for XSS patterns
     */
    fun containsXss(text: String): Boolean {
        return XSS_PATTERNS.any { pattern ->
            pattern.matcher(text).find()
        }
    }

    /**
     * Check for path traversal attempts
     */
    fun containsPathTraversal(text: String): Boolean {
        return PATH_TRAVERSAL_PATTERNS.any { pattern ->
            pattern.matcher(text).find()
        }
    }

    /**
     * Check if text contains only safe characters
     */
    fun isSafeText(text: String): Boolean {
        return SAFE_TEXT_PATTERN.matcher(text).matches()
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Validate and sanitize in one call
     *
     * Returns sanitized text if valid, null otherwise
     */
    fun validateAndSanitize(
        text: String?,
        maxLength: Int = MAX_TEXT_LENGTH
    ): String? {
        if (!isValidText(text, maxLength)) return null
        return sanitizeText(text, maxLength)
    }

    /**
     * Get validation error message
     */
    fun getValidationError(text: String?, maxLength: Int = MAX_TEXT_LENGTH): String? {
        return when {
            text.isNullOrBlank() -> "Input cannot be empty"
            text.length > maxLength -> "Input too long (max $maxLength characters)"
            containsSqlInjection(text) -> "Invalid characters detected"
            containsXss(text) -> "Invalid characters detected"
            else -> null // Valid
        }
    }
}

/**
 * Extension functions for easy validation
 */

fun String?.isValidText(maxLength: Int = InputValidator.MAX_TEXT_LENGTH): Boolean {
    return InputValidator.isValidText(this, maxLength)
}

fun String?.isValidEmail(): Boolean {
    return InputValidator.isValidEmail(this)
}

fun String?.isValidUrl(): Boolean {
    return InputValidator.isValidUrl(this)
}

fun String?.sanitize(maxLength: Int = InputValidator.MAX_TEXT_LENGTH): String {
    return InputValidator.sanitizeText(this, maxLength)
}

/**
 * USAGE EXAMPLES:
 * ===============
 *
 * 1. **Validate user-typed text:**
 * ```kotlin
 * val userText = editText.text.toString()
 *
 * if (!userText.isValidText()) {
 *     showError("Invalid input")
 *     return
 * }
 *
 * saveWord(userText.sanitize())
 * ```
 *
 * 2. **Validate API key:**
 * ```kotlin
 * val apiKey = apiKeyInput.text.toString()
 *
 * if (!InputValidator.isValidApiKey(apiKey)) {
 *     showError("Invalid API key format")
 *     return
 * }
 *
 * secureStorage.putString("api_key", apiKey)
 * ```
 *
 * 3. **Validate clipboard data:**
 * ```kotlin
 * fun addToClipboard(text: String) {
 *     val error = InputValidator.getValidationError(text, maxLength = 10000)
 *     if (error != null) {
 *         Log.w("Clipboard", "Invalid input: $error")
 *         return
 *     }
 *
 *     val sanitized = text.sanitize()
 *     clipboardManager.addItem(sanitized)
 * }
 * ```
 *
 * 4. **Validate database query parameters:**
 * ```kotlin
 * fun searchWords(query: String): List<Word> {
 *     // Validate before using in query
 *     if (!query.isValidText(maxLength = 100)) {
 *         return emptyList()
 *     }
 *
 *     // Room handles parameterized queries, but still validate
 *     return wordDao.search(query.sanitize())
 * }
 * ```
 *
 * BEST PRACTICES:
 * ===============
 *
 * 1. **Validate all inputs**
 *    - User-typed text
 *    - API responses
 *    - File contents
 *    - Database results (if from untrusted source)
 *
 * 2. **Validate early**
 *    - At input boundary (UI layer)
 *    - Before processing
 *    - Before storage
 *
 * 3. **Use parameterized queries**
 *    - Room automatically uses prepared statements
 *    - Never concatenate SQL queries
 *
 * 4. **Fail securely**
 *    - Reject invalid input
 *    - Don't try to "fix" malicious input
 *    - Log security violations
 *
 * 5. **Defense in depth**
 *    - Validate at multiple layers
 *    - UI validation (user feedback)
 *    - Business logic validation (security)
 *    - Database constraints (last line of defense)
 */
