package com.kannada.kavi.data.repositories

import com.kannada.kavi.data.database.dao.AnalyticsDao
import com.kannada.kavi.data.database.entities.AnalyticsEventEntity
import com.kannada.kavi.data.repositories.models.AnalyticsEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing analytics events with offline support.
 *
 * This repository provides:
 * - Local queue for analytics events when offline
 * - Sync coordination with Firebase Analytics
 * - Retry mechanism for failed syncs
 * - Event batching for efficiency
 *
 * Architecture:
 * 1. Event occurs → queue locally first
 * 2. Try to sync to Firebase immediately
 * 3. If sync fails (offline/error) → retry later
 * 4. Background worker periodically syncs pending events
 * 5. Clean up successfully synced events after retention period
 *
 * This ensures zero data loss even when device is offline.
 */
@Singleton
class AnalyticsRepository @Inject constructor(
    private val analyticsDao: AnalyticsDao
) {

    /**
     * Queue an analytics event for eventual sync to Firebase.
     * Event is stored locally first, then synced asynchronously.
     *
     * @param eventName Firebase Analytics event name
     * @param properties Event properties (key-value pairs)
     * @return The created event ID
     */
    suspend fun queueEvent(eventName: String, properties: Map<String, Any> = emptyMap()): String {
        val eventId = UUID.randomUUID().toString()

        val event = AnalyticsEventEntity(
            eventId = eventId,
            eventName = eventName,
            timestamp = System.currentTimeMillis(),
            properties = propertiesToJson(properties),
            synced = false,
            syncedAt = null,
            retryCount = 0
        )

        analyticsDao.insertEvent(event)
        return eventId
    }

    /**
     * Queue multiple events at once (batch operation).
     * Useful for catching up after offline period.
     *
     * @param events List of event name + properties pairs
     */
    suspend fun queueEvents(events: List<Pair<String, Map<String, Any>>>) {
        val entities = events.map { (eventName, properties) ->
            AnalyticsEventEntity(
                eventId = UUID.randomUUID().toString(),
                eventName = eventName,
                timestamp = System.currentTimeMillis(),
                properties = propertiesToJson(properties),
                synced = false,
                syncedAt = null,
                retryCount = 0
            )
        }

        analyticsDao.insertEvents(entities)
    }

    /**
     * Get all unsynced events that need to be sent to Firebase.
     * These are events that failed to sync or were queued while offline.
     *
     * @return List of unsynced analytics events
     */
    suspend fun getUnsyncedEvents(): List<AnalyticsEvent> {
        return analyticsDao.getUnsyncedEvents().map { it.toDomainModel() }
    }

    /**
     * Get unsynced events for retry (excluding permanently failed events).
     * Events that exceeded max retries are excluded.
     *
     * @param maxRetries Maximum retry attempts (default: 5)
     * @param limit Maximum events to fetch (default: 100)
     * @return List of analytics events to retry
     */
    suspend fun getEventsForRetry(maxRetries: Int = 5, limit: Int = 100): List<AnalyticsEvent> {
        return analyticsDao.getUnsyncedEventsForRetry(maxRetries, limit).map { it.toDomainModel() }
    }

    /**
     * Mark an event as successfully synced to Firebase.
     *
     * @param eventId The event ID to mark as synced
     */
    suspend fun markAsSynced(eventId: String) {
        analyticsDao.markAsSynced(eventId, System.currentTimeMillis())
    }

    /**
     * Mark multiple events as synced (batch operation).
     * Efficient for batch sync operations.
     *
     * @param eventIds List of event IDs to mark as synced
     */
    suspend fun markMultipleAsSynced(eventIds: List<String>) {
        analyticsDao.markMultipleAsSynced(eventIds, System.currentTimeMillis())
    }

    /**
     * Increment retry count for an event that failed to sync.
     * Used to implement exponential backoff.
     *
     * @param eventId The event ID to increment retry count
     */
    suspend fun incrementRetryCount(eventId: String) {
        analyticsDao.incrementRetryCount(eventId)
    }

    /**
     * Observe count of unsynced events as Flow.
     * Useful for showing "pending sync" indicator in UI.
     *
     * @return Flow of pending event count
     */
    fun observeUnsyncedCount(): Flow<Int> {
        return analyticsDao.observeUnsyncedCount()
    }

    /**
     * Clean up old synced events to save storage space.
     * Keeps recent synced events for debugging but removes old ones.
     *
     * @param retentionDays Keep synced events for this many days (default: 30)
     */
    suspend fun cleanupOldEvents(retentionDays: Int = 30) {
        val cutoffTimestamp = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        analyticsDao.deleteSyncedEventsOlderThan(cutoffTimestamp)
    }

    /**
     * Delete events that permanently failed (exceeded max retries).
     * These events will never sync successfully.
     *
     * @param maxRetries Threshold for permanent failure (default: 5)
     */
    suspend fun deleteFailedEvents(maxRetries: Int = 5) {
        analyticsDao.deleteFailedEvents(maxRetries)
    }

    /**
     * Clear all analytics events (nuclear option for privacy/testing).
     */
    suspend fun clearAll() {
        analyticsDao.clearAll()
    }

    /**
     * Get analytics statistics.
     *
     * @return Triple of (totalEvents, unsyncedEvents, syncedEvents)
     */
    suspend fun getStatistics(): Triple<Int, Int, Int> {
        val total = analyticsDao.getEventCount()
        val unsynced = analyticsDao.getUnsyncedEventCount()
        val synced = analyticsDao.getSyncedEventCount()
        return Triple(total, unsynced, synced)
    }

    /**
     * Get events by name (for analysis/debugging).
     *
     * @param eventName The event name to search for
     * @param limit Maximum number of events (default: 100)
     * @return List of analytics events
     */
    suspend fun getEventsByName(eventName: String, limit: Int = 100): List<AnalyticsEvent> {
        return analyticsDao.getEventsByName(eventName, limit).map { it.toDomainModel() }
    }

    /**
     * Get events within a time range (for analysis/debugging).
     *
     * @param startTime Start timestamp in milliseconds
     * @param endTime End timestamp in milliseconds
     * @return List of analytics events in the time range
     */
    suspend fun getEventsBetween(startTime: Long, endTime: Long): List<AnalyticsEvent> {
        return analyticsDao.getEventsBetween(startTime, endTime).map { it.toDomainModel() }
    }

    /**
     * Delete a specific event.
     *
     * @param eventId The event ID to delete
     */
    suspend fun deleteEvent(eventId: String) {
        analyticsDao.deleteEvent(eventId)
    }

    /**
     * Convert properties map to JSON string for database storage.
     */
    private fun propertiesToJson(properties: Map<String, Any>): String? {
        if (properties.isEmpty()) return null

        return try {
            val jsonObject = JSONObject()
            properties.forEach { (key, value) ->
                jsonObject.put(key, value)
            }
            jsonObject.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert JSON string back to properties map.
     */
    private fun jsonToProperties(json: String?): Map<String, Any> {
        if (json == null) return emptyMap()

        return try {
            val jsonObject = JSONObject(json)
            val map = mutableMapOf<String, Any>()
            jsonObject.keys().forEach { key ->
                map[key] = jsonObject.get(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

/**
 * Extension function to convert database entity to domain model.
 */
private fun AnalyticsEventEntity.toDomainModel() = AnalyticsEvent(
    eventId = eventId,
    eventName = eventName,
    timestamp = timestamp,
    properties = if (properties != null) {
        try {
            val jsonObject = JSONObject(properties)
            val map = mutableMapOf<String, Any>()
            jsonObject.keys().forEach { key ->
                map[key] = jsonObject.get(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    } else {
        emptyMap()
    },
    synced = synced,
    syncedAt = syncedAt,
    retryCount = retryCount
)
