package com.kannada.kavi.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an analytics event stored locally before syncing to Firebase.
 * Ensures events are not lost when device is offline or Firebase is temporarily unavailable.
 *
 * Indexes:
 * - synced: For quick filtering of unsynced events
 * - timestamp: For chronological processing
 * - synced + timestamp: Composite index for efficient sync queue queries
 */
@Entity(
    tableName = "analytics_events",
    indices = [
        Index(value = ["synced"]),
        Index(value = ["timestamp"]),
        Index(value = ["synced", "timestamp"])
    ]
)
data class AnalyticsEventEntity(
    /**
     * Unique identifier for this event (UUID).
     */
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,

    /**
     * Firebase event name (e.g., "key_press", "layout_switch", "suggestion_accepted").
     */
    @ColumnInfo(name = "event_name")
    val eventName: String,

    /**
     * Timestamp (milliseconds) when the event occurred.
     */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    /**
     * Event properties as JSON string.
     * Contains additional metadata like layout_type, key_type, etc.
     * Stored as JSON to handle dynamic properties.
     */
    @ColumnInfo(name = "properties")
    val properties: String? = null,

    /**
     * Whether this event has been successfully synced to Firebase.
     * false = pending sync, true = synced successfully
     */
    @ColumnInfo(name = "synced")
    val synced: Boolean = false,

    /**
     * Timestamp (milliseconds) when this event was synced to Firebase.
     * Null if not yet synced.
     */
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null,

    /**
     * Number of sync attempts for this event.
     * Used to implement exponential backoff and give up after max retries.
     */
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0
)
