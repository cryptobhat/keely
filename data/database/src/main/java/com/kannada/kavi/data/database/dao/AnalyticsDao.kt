package com.kannada.kavi.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kannada.kavi.data.database.entities.AnalyticsEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for analytics events.
 * Provides local queue for analytics events before syncing to Firebase.
 * Ensures no events are lost due to network issues or offline mode.
 */
@Dao
interface AnalyticsDao {

    /**
     * Insert a new analytics event into the queue.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AnalyticsEventEntity)

    /**
     * Insert multiple events at once (batch operation).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<AnalyticsEventEntity>)

    /**
     * Get a specific event by ID.
     */
    @Query("SELECT * FROM analytics_events WHERE event_id = :eventId LIMIT 1")
    suspend fun getEvent(eventId: String): AnalyticsEventEntity?

    /**
     * Get all unsynced events ordered by timestamp (oldest first for FIFO processing).
     */
    @Query("""
        SELECT * FROM analytics_events
        WHERE synced = 0
        ORDER BY timestamp ASC
    """)
    suspend fun getUnsyncedEvents(): List<AnalyticsEventEntity>

    /**
     * Get unsynced events with retry count below threshold.
     * Prevents infinite retry loops for permanently failed events.
     */
    @Query("""
        SELECT * FROM analytics_events
        WHERE synced = 0 AND retry_count < :maxRetries
        ORDER BY timestamp ASC
        LIMIT :limit
    """)
    suspend fun getUnsyncedEventsForRetry(maxRetries: Int = 5, limit: Int = 100): List<AnalyticsEventEntity>

    /**
     * Get all synced events (for debugging or analysis).
     */
    @Query("SELECT * FROM analytics_events WHERE synced = 1 ORDER BY synced_at DESC")
    suspend fun getSyncedEvents(): List<AnalyticsEventEntity>

    /**
     * Observe unsynced events count as Flow.
     * Useful for showing pending sync badge in UI.
     */
    @Query("SELECT COUNT(*) FROM analytics_events WHERE synced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    /**
     * Mark an event as successfully synced.
     */
    @Query("""
        UPDATE analytics_events
        SET synced = 1, synced_at = :syncedAt
        WHERE event_id = :eventId
    """)
    suspend fun markAsSynced(eventId: String, syncedAt: Long = System.currentTimeMillis())

    /**
     * Mark multiple events as synced (batch operation).
     */
    @Query("""
        UPDATE analytics_events
        SET synced = 1, synced_at = :syncedAt
        WHERE event_id IN (:eventIds)
    """)
    suspend fun markMultipleAsSynced(eventIds: List<String>, syncedAt: Long = System.currentTimeMillis())

    /**
     * Increment retry count for a failed sync attempt.
     */
    @Query("""
        UPDATE analytics_events
        SET retry_count = retry_count + 1
        WHERE event_id = :eventId
    """)
    suspend fun incrementRetryCount(eventId: String)

    /**
     * Delete a specific event.
     */
    @Query("DELETE FROM analytics_events WHERE event_id = :eventId")
    suspend fun deleteEvent(eventId: String)

    /**
     * Delete multiple events (batch deletion).
     */
    @Query("DELETE FROM analytics_events WHERE event_id IN (:eventIds)")
    suspend fun deleteEvents(eventIds: List<String>)

    /**
     * Delete all synced events older than a given timestamp (cleanup).
     * Keeps recent synced events for debugging but removes old ones to save space.
     */
    @Query("""
        DELETE FROM analytics_events
        WHERE synced = 1 AND synced_at < :timestamp
    """)
    suspend fun deleteSyncedEventsOlderThan(timestamp: Long)

    /**
     * Delete events that have exceeded max retry count (permanent failures).
     */
    @Query("DELETE FROM analytics_events WHERE retry_count >= :maxRetries")
    suspend fun deleteFailedEvents(maxRetries: Int = 5)

    /**
     * Clear all analytics events (nuclear option for privacy/reset).
     */
    @Query("DELETE FROM analytics_events")
    suspend fun clearAll()

    /**
     * Get total count of events.
     */
    @Query("SELECT COUNT(*) FROM analytics_events")
    suspend fun getEventCount(): Int

    /**
     * Get count of unsynced events.
     */
    @Query("SELECT COUNT(*) FROM analytics_events WHERE synced = 0")
    suspend fun getUnsyncedEventCount(): Int

    /**
     * Get count of synced events.
     */
    @Query("SELECT COUNT(*) FROM analytics_events WHERE synced = 1")
    suspend fun getSyncedEventCount(): Int

    /**
     * Get events by name (for analysis).
     */
    @Query("""
        SELECT * FROM analytics_events
        WHERE event_name = :eventName
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getEventsByName(eventName: String, limit: Int = 100): List<AnalyticsEventEntity>

    /**
     * Get events within a time range (for analysis).
     */
    @Query("""
        SELECT * FROM analytics_events
        WHERE timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp ASC
    """)
    suspend fun getEventsBetween(startTime: Long, endTime: Long): List<AnalyticsEventEntity>
}
