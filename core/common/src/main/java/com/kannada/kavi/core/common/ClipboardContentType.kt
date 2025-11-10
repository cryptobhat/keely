package com.kannada.kavi.core.common

/**
 * ClipboardContentType - Type of Clipboard Content
 *
 * Different content types can have different behaviors:
 * - URLs → Show "Open in browser" action
 * - Emails → Show "Compose email" action
 * - Phone → Show "Call" action
 */
enum class ClipboardContentType {
    /**
     * Plain text
     */
    TEXT,

    /**
     * URL/Web link
     *
     * Detected if starts with http://, https://, www.
     */
    URL,

    /**
     * Email address
     *
     * Detected if contains @ and .
     */
    EMAIL,

    /**
     * Phone number
     *
     * Detected if only digits, +, -, (, )
     */
    PHONE,

    /**
     * Code snippet
     *
     * For developers - syntax highlighting in future
     */
    CODE
}
