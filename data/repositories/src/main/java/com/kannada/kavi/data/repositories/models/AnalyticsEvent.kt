package com.kannada.kavi.data.repositories.models

/**
 * Domain model representing an analytics event.
 * Separate from the database entity to allow independent evolution.
 *
 * This model represents events that are queued locally before being
 * synced to Firebase Analytics.
 */
data class AnalyticsEvent(
    /**
     * Unique identifier for this event (UUID).
     */
    val eventId: String,

    /**
     * Firebase event name (e.g., "key_press", "layout_switch").
     */
    val eventName: String,

    /**
     * Timestamp when the event occurred (milliseconds).
     */
    val timestamp: Long,

    /**
     * Event properties as key-value map.
     * Contains additional metadata about the event.
     */
    val properties: Map<String, Any> = emptyMap(),

    /**
     * Whether this event has been successfully synced to Firebase.
     */
    val synced: Boolean = false,

    /**
     * Timestamp when this event was synced (null if not yet synced).
     */
    val syncedAt: Long? = null,

    /**
     * Number of sync retry attempts.
     */
    val retryCount: Int = 0
) {
    /**
     * Check if this event is pending sync.
     */
    fun isPending(): Boolean = !synced

    /**
     * Check if this event has exceeded max retries.
     */
    fun hasExceededRetries(maxRetries: Int = 5): Boolean = retryCount >= maxRetries

    /**
     * Get age of this event in milliseconds.
     */
    fun getAge(): Long = System.currentTimeMillis() - timestamp

    /**
     * Get time since sync in milliseconds (or null if not synced).
     */
    fun getTimeSinceSync(): Long? = syncedAt?.let { System.currentTimeMillis() - it }
}
