package com.kannada.kavi.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kannada.kavi.data.database.dao.AnalyticsDao
import com.kannada.kavi.data.database.entities.AnalyticsEventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * AnalyticsDaoTest - Comprehensive Tests for AnalyticsDao
 *
 * Tests analytics event queue with sync status and retry logic.
 *
 * TEST CATEGORIES:
 * ================
 * 1. Insert Operations
 *    - Single insert
 *    - Batch insert
 *    - Auto-generated ID
 *
 * 2. Query Operations
 *    - Get by ID
 *    - Get unsynced events
 *    - Get failed events (with retries)
 *    - Get events by name
 *    - Get events by date range
 *
 * 3. Update Operations
 *    - Mark as synced
 *    - Increment retry count
 *    - Update sync status
 *
 * 4. Delete Operations
 *    - Delete by ID
 *    - Delete synced events
 *    - Delete old events
 *    - Clear all
 *
 * 5. Count/Stats Operations
 *    - Total count
 *    - Unsynced count
 *    - Failed count
 *
 * 6. Sync Queue Tests
 *    - FIFO queue behavior
 *    - Retry logic
 *    - Batch processing
 */
@RunWith(AndroidJUnit4::class)
class AnalyticsDaoTest {

    private lateinit var database: KaviDatabase
    private lateinit var analyticsDao: AnalyticsDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            KaviDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        analyticsDao = database.analyticsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========================================
    // INSERT TESTS
    // ========================================

    @Test
    fun insertEvent_savesToDatabase() = runBlocking {
        // Given
        val event = AnalyticsEventEntity(
            eventName = "key_pressed",
            timestamp = System.currentTimeMillis(),
            properties = mapOf("key" to "ಅ", "layout" to "kavi"),
            isSynced = false,
            retryCount = 0
        )

        // When
        val id = analyticsDao.insertEvent(event)

        // Then
        assertThat(id).isGreaterThan(0)
        val retrieved = analyticsDao.getEventById(id)
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.eventName).isEqualTo("key_pressed")
        assertThat(retrieved?.isSynced).isFalse()
    }

    @Test
    fun insertMultipleEvents_savesAll() = runBlocking {
        // Given
        val events = listOf(
            AnalyticsEventEntity(
                eventName = "key_pressed",
                timestamp = 1000,
                properties = mapOf("key" to "ಅ"),
                isSynced = false,
                retryCount = 0
            ),
            AnalyticsEventEntity(
                eventName = "layout_changed",
                timestamp = 2000,
                properties = mapOf("from" to "qwerty", "to" to "kavi"),
                isSynced = false,
                retryCount = 0
            ),
            AnalyticsEventEntity(
                eventName = "suggestion_selected",
                timestamp = 3000,
                properties = mapOf("suggestion" to "ನಮಸ್ಕಾರ"),
                isSynced = true,
                retryCount = 0
            )
        )

        // When
        analyticsDao.insertEvents(events)

        // Then
        val allEvents = analyticsDao.getAllEvents()
        assertThat(allEvents).hasSize(3)
    }

    @Test
    fun insertEvent_autoGeneratesId() = runBlocking {
        // Given
        val event1 = AnalyticsEventEntity(
            eventName = "event1",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = false,
            retryCount = 0
        )
        val event2 = AnalyticsEventEntity(
            eventName = "event2",
            timestamp = 2000,
            properties = emptyMap(),
            isSynced = false,
            retryCount = 0
        )

        // When
        val id1 = analyticsDao.insertEvent(event1)
        val id2 = analyticsDao.insertEvent(event2)

        // Then - IDs should be different and auto-incrementing
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id2).isGreaterThan(id1)
    }

    // ========================================
    // QUERY TESTS
    // ========================================

    @Test
    fun getEventById_returnsCorrectEvent() = runBlocking {
        // Given
        val event = AnalyticsEventEntity(
            eventName = "test_event",
            timestamp = 1000,
            properties = mapOf("key" to "value"),
            isSynced = false,
            retryCount = 0
        )
        val id = analyticsDao.insertEvent(event)

        // When
        val retrieved = analyticsDao.getEventById(id)

        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.eventName).isEqualTo("test_event")
        assertThat(retrieved?.properties).containsEntry("key", "value")
    }

    @Test
    fun getEventById_nonExistent_returnsNull() = runBlocking {
        // When
        val result = analyticsDao.getEventById(999999)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun getUnsyncedEvents_returnsOnlyUnsynced() = runBlocking {
        // Given - Mix of synced and unsynced
        val events = listOf(
            AnalyticsEventEntity(eventName = "synced1", timestamp = 1000, properties = emptyMap(), isSynced = true, retryCount = 0),
            AnalyticsEventEntity(eventName = "unsynced1", timestamp = 2000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "synced2", timestamp = 3000, properties = emptyMap(), isSynced = true, retryCount = 0),
            AnalyticsEventEntity(eventName = "unsynced2", timestamp = 4000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "unsynced3", timestamp = 5000, properties = emptyMap(), isSynced = false, retryCount = 0)
        )
        analyticsDao.insertEvents(events)

        // When
        val unsynced = analyticsDao.getUnsyncedEvents()

        // Then
        assertThat(unsynced).hasSize(3)
        assertThat(unsynced.map { it.eventName }).containsExactly("unsynced1", "unsynced2", "unsynced3")
    }

    @Test
    fun getUnsyncedEvents_sortedByTimestamp() = runBlocking {
        // Given - Insert in random order
        val events = listOf(
            AnalyticsEventEntity(eventName = "event3", timestamp = 3000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "event1", timestamp = 1000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "event2", timestamp = 2000, properties = emptyMap(), isSynced = false, retryCount = 0)
        )
        analyticsDao.insertEvents(events)

        // When
        val unsynced = analyticsDao.getUnsyncedEvents()

        // Then - Should be sorted by timestamp ASC (FIFO)
        assertThat(unsynced).hasSize(3)
        assertThat(unsynced[0].eventName).isEqualTo("event1")
        assertThat(unsynced[1].eventName).isEqualTo("event2")
        assertThat(unsynced[2].eventName).isEqualTo("event3")
    }

    @Test
    fun getUnsyncedEventsWithLimit_returnsCorrectCount() = runBlocking {
        // Given - 5 unsynced events
        val events = (1..5).map { i ->
            AnalyticsEventEntity(
                eventName = "event$i",
                timestamp = i.toLong() * 1000,
                properties = emptyMap(),
                isSynced = false,
                retryCount = 0
            )
        }
        analyticsDao.insertEvents(events)

        // When - Request only 3
        val result = analyticsDao.getUnsyncedEvents(limit = 3)

        // Then
        assertThat(result).hasSize(3)
    }

    @Test
    fun getFailedEvents_returnsOnlyFailed() = runBlocking {
        // Given
        val events = listOf(
            AnalyticsEventEntity(eventName = "success", timestamp = 1000, properties = emptyMap(), isSynced = true, retryCount = 0),
            AnalyticsEventEntity(eventName = "failed1", timestamp = 2000, properties = emptyMap(), isSynced = false, retryCount = 3),
            AnalyticsEventEntity(eventName = "pending", timestamp = 3000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "failed2", timestamp = 4000, properties = emptyMap(), isSynced = false, retryCount = 5)
        )
        analyticsDao.insertEvents(events)

        // When - Get events with retries > 0
        val failed = analyticsDao.getFailedEvents(minRetries = 1)

        // Then
        assertThat(failed).hasSize(2)
        assertThat(failed.map { it.eventName }).containsExactly("failed1", "failed2")
    }

    @Test
    fun getEventsByName_filtersCorrectly() = runBlocking {
        // Given
        val events = listOf(
            AnalyticsEventEntity(eventName = "key_pressed", timestamp = 1000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "layout_changed", timestamp = 2000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "key_pressed", timestamp = 3000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "key_pressed", timestamp = 4000, properties = emptyMap(), isSynced = true, retryCount = 0)
        )
        analyticsDao.insertEvents(events)

        // When
        val keyPressedEvents = analyticsDao.getEventsByName("key_pressed")

        // Then
        assertThat(keyPressedEvents).hasSize(3)
        assertThat(keyPressedEvents.all { it.eventName == "key_pressed" }).isTrue()
    }

    @Test
    fun getEventsByDateRange_filtersCorrectly() = runBlocking {
        // Given
        val events = listOf(
            AnalyticsEventEntity(eventName = "event1", timestamp = 1000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "event2", timestamp = 2000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "event3", timestamp = 3000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "event4", timestamp = 4000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "event5", timestamp = 5000, properties = emptyMap(), isSynced = false, retryCount = 0)
        )
        analyticsDao.insertEvents(events)

        // When - Get events between 2000 and 4000
        val rangeEvents = analyticsDao.getEventsByDateRange(startTime = 2000, endTime = 4000)

        // Then
        assertThat(rangeEvents).hasSize(3) // event2, event3, event4
        assertThat(rangeEvents.map { it.eventName }).containsExactly("event2", "event3", "event4")
    }

    // ========================================
    // UPDATE TESTS
    // ========================================

    @Test
    fun markAsSynced_updatesFlag() = runBlocking {
        // Given
        val event = AnalyticsEventEntity(
            eventName = "test_event",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = false,
            retryCount = 0
        )
        val id = analyticsDao.insertEvent(event)

        // When
        analyticsDao.markAsSynced(id)

        // Then
        val updated = analyticsDao.getEventById(id)
        assertThat(updated?.isSynced).isTrue()
    }

    @Test
    fun incrementRetryCount_increasesCount() = runBlocking {
        // Given
        val event = AnalyticsEventEntity(
            eventName = "test_event",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = false,
            retryCount = 0
        )
        val id = analyticsDao.insertEvent(event)

        // When - Increment 3 times
        analyticsDao.incrementRetryCount(id)
        analyticsDao.incrementRetryCount(id)
        analyticsDao.incrementRetryCount(id)

        // Then
        val updated = analyticsDao.getEventById(id)
        assertThat(updated?.retryCount).isEqualTo(3)
    }

    @Test
    fun updateSyncStatus_changesStatus() = runBlocking {
        // Given
        val event = AnalyticsEventEntity(
            eventName = "test_event",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = false,
            retryCount = 0
        )
        val id = analyticsDao.insertEvent(event)

        // When
        analyticsDao.updateSyncStatus(id, isSynced = true)

        // Then
        val updated = analyticsDao.getEventById(id)
        assertThat(updated?.isSynced).isTrue()

        // When - Change back
        analyticsDao.updateSyncStatus(id, isSynced = false)

        // Then
        val reverted = analyticsDao.getEventById(id)
        assertThat(reverted?.isSynced).isFalse()
    }

    // ========================================
    // DELETE TESTS
    // ========================================

    @Test
    fun deleteEvent_removesFromDatabase() = runBlocking {
        // Given
        val events = listOf(
            AnalyticsEventEntity(eventName = "event1", timestamp = 1000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "event2", timestamp = 2000, properties = emptyMap(), isSynced = false, retryCount = 0)
        )
        val ids = events.map { analyticsDao.insertEvent(it) }

        // When
        analyticsDao.deleteEvent(ids[0])

        // Then
        assertThat(analyticsDao.getEventById(ids[0])).isNull()
        assertThat(analyticsDao.getEventById(ids[1])).isNotNull()
        assertThat(analyticsDao.getAllEvents()).hasSize(1)
    }

    @Test
    fun deleteSyncedEvents_removesOnlySynced() = runBlocking {
        // Given
        val events = listOf(
            AnalyticsEventEntity(eventName = "synced1", timestamp = 1000, properties = emptyMap(), isSynced = true, retryCount = 0),
            AnalyticsEventEntity(eventName = "unsynced", timestamp = 2000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "synced2", timestamp = 3000, properties = emptyMap(), isSynced = true, retryCount = 0)
        )
        analyticsDao.insertEvents(events)

        // When
        analyticsDao.deleteSyncedEvents()

        // Then
        val remaining = analyticsDao.getAllEvents()
        assertThat(remaining).hasSize(1)
        assertThat(remaining[0].eventName).isEqualTo("unsynced")
    }

    @Test
    fun deleteOldEvents_removesOnlyOldEvents() = runBlocking {
        // Given
        val currentTime = System.currentTimeMillis()
        val oldTime = currentTime - (8L * 24 * 60 * 60 * 1000) // 8 days ago
        val recentTime = currentTime - (2L * 24 * 60 * 60 * 1000) // 2 days ago

        val events = listOf(
            AnalyticsEventEntity(eventName = "old1", timestamp = oldTime, properties = emptyMap(), isSynced = true, retryCount = 0),
            AnalyticsEventEntity(eventName = "old2", timestamp = oldTime - 1000, properties = emptyMap(), isSynced = true, retryCount = 0),
            AnalyticsEventEntity(eventName = "recent", timestamp = recentTime, properties = emptyMap(), isSynced = true, retryCount = 0),
            AnalyticsEventEntity(eventName = "new", timestamp = currentTime, properties = emptyMap(), isSynced = false, retryCount = 0)
        )
        analyticsDao.insertEvents(events)

        // When - Delete events older than 7 days
        val sevenDaysAgo = currentTime - (7L * 24 * 60 * 60 * 1000)
        analyticsDao.deleteOldEvents(sevenDaysAgo)

        // Then
        val remaining = analyticsDao.getAllEvents()
        assertThat(remaining).hasSize(2)
        assertThat(remaining.map { it.eventName }).containsExactly("recent", "new")
    }

    @Test
    fun clearAll_removesAllEvents() = runBlocking {
        // Given
        val events = (1..10).map { i ->
            AnalyticsEventEntity(
                eventName = "event$i",
                timestamp = i.toLong() * 1000,
                properties = emptyMap(),
                isSynced = i % 2 == 0,
                retryCount = 0
            )
        }
        analyticsDao.insertEvents(events)
        assertThat(analyticsDao.getAllEvents()).hasSize(10)

        // When
        analyticsDao.clearAll()

        // Then
        assertThat(analyticsDao.getAllEvents()).isEmpty()
    }

    // ========================================
    // COUNT TESTS
    // ========================================

    @Test
    fun getCount_returnsCorrectTotal() = runBlocking {
        // Given
        val events = (1..7).map { i ->
            AnalyticsEventEntity(
                eventName = "event$i",
                timestamp = i.toLong() * 1000,
                properties = emptyMap(),
                isSynced = false,
                retryCount = 0
            )
        }
        analyticsDao.insertEvents(events)

        // When
        val count = analyticsDao.getCount()

        // Then
        assertThat(count).isEqualTo(7)
    }

    @Test
    fun getUnsyncedCount_returnsCorrectCount() = runBlocking {
        // Given
        val events = listOf(
            AnalyticsEventEntity(eventName = "synced", timestamp = 1000, properties = emptyMap(), isSynced = true, retryCount = 0),
            AnalyticsEventEntity(eventName = "unsynced1", timestamp = 2000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "unsynced2", timestamp = 3000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "unsynced3", timestamp = 4000, properties = emptyMap(), isSynced = false, retryCount = 0)
        )
        analyticsDao.insertEvents(events)

        // When
        val count = analyticsDao.getUnsyncedCount()

        // Then
        assertThat(count).isEqualTo(3)
    }

    // ========================================
    // FLOW/REACTIVE TESTS
    // ========================================

    @Test
    fun observeUnsyncedEvents_emitsUpdates() = runBlocking {
        // Given - Empty database
        var emittedEvents = analyticsDao.observeUnsyncedEvents().first()
        assertThat(emittedEvents).isEmpty()

        // When - Insert unsynced event
        val event = AnalyticsEventEntity(
            eventName = "test_event",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = false,
            retryCount = 0
        )
        analyticsDao.insertEvent(event)

        // Then - Flow should emit new data
        emittedEvents = analyticsDao.observeUnsyncedEvents().first()
        assertThat(emittedEvents).hasSize(1)
        assertThat(emittedEvents[0].eventName).isEqualTo("test_event")
    }

    @Test
    fun observeUnsyncedCount_emitsCorrectCount() = runBlocking {
        // Given - Empty database
        var count = analyticsDao.observeUnsyncedCount().first()
        assertThat(count).isEqualTo(0)

        // When - Insert 3 unsynced events
        val events = (1..3).map { i ->
            AnalyticsEventEntity(
                eventName = "event$i",
                timestamp = i.toLong() * 1000,
                properties = emptyMap(),
                isSynced = false,
                retryCount = 0
            )
        }
        analyticsDao.insertEvents(events)

        // Then
        count = analyticsDao.observeUnsyncedCount().first()
        assertThat(count).isEqualTo(3)
    }

    // ========================================
    // PROPERTIES/JSON TESTS
    // ========================================

    @Test
    fun properties_persistCorrectly() = runBlocking {
        // Given
        val properties = mapOf(
            "key" to "ಅ",
            "layout" to "kavi",
            "count" to 5,
            "enabled" to true,
            "nested" to mapOf("inner" to "value")
        )
        val event = AnalyticsEventEntity(
            eventName = "test_event",
            timestamp = 1000,
            properties = properties,
            isSynced = false,
            retryCount = 0
        )

        // When
        val id = analyticsDao.insertEvent(event)

        // Then
        val retrieved = analyticsDao.getEventById(id)
        assertThat(retrieved?.properties).isEqualTo(properties)
    }

    @Test
    fun emptyProperties_handledCorrectly() = runBlocking {
        // Given
        val event = AnalyticsEventEntity(
            eventName = "test_event",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = false,
            retryCount = 0
        )

        // When
        val id = analyticsDao.insertEvent(event)

        // Then
        val retrieved = analyticsDao.getEventById(id)
        assertThat(retrieved?.properties).isEmpty()
    }

    // ========================================
    // SYNC QUEUE BEHAVIOR TESTS
    // ========================================

    @Test
    fun syncQueue_maintainsFIFOOrder() = runBlocking {
        // Given - Insert events at different times
        val events = listOf(
            AnalyticsEventEntity(eventName = "first", timestamp = 1000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "second", timestamp = 2000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(eventName = "third", timestamp = 3000, properties = emptyMap(), isSynced = false, retryCount = 0)
        )
        analyticsDao.insertEvents(events)

        // When - Get unsynced (should be FIFO)
        val queue = analyticsDao.getUnsyncedEvents()

        // Then - Should be in insertion order
        assertThat(queue).hasSize(3)
        assertThat(queue[0].eventName).isEqualTo("first")
        assertThat(queue[1].eventName).isEqualTo("second")
        assertThat(queue[2].eventName).isEqualTo("third")
    }

    @Test
    fun batchSync_processesCorrectly() = runBlocking {
        // Given - 10 unsynced events
        val events = (1..10).map { i ->
            AnalyticsEventEntity(
                eventName = "event$i",
                timestamp = i.toLong() * 1000,
                properties = emptyMap(),
                isSynced = false,
                retryCount = 0
            )
        }
        analyticsDao.insertEvents(events)

        // When - Process first 5
        val batch = analyticsDao.getUnsyncedEvents(limit = 5)
        batch.forEach { event ->
            analyticsDao.markAsSynced(event.id)
        }

        // Then - 5 should remain unsynced
        val remaining = analyticsDao.getUnsyncedCount()
        assertThat(remaining).isEqualTo(5)
    }

    @Test
    fun retryLogic_tracksFailures() = runBlocking {
        // Given
        val event = AnalyticsEventEntity(
            eventName = "failing_event",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = false,
            retryCount = 0
        )
        val id = analyticsDao.insertEvent(event)

        // When - Simulate 3 failed attempts
        repeat(3) {
            analyticsDao.incrementRetryCount(id)
        }

        // Then - Should have 3 retries
        val updated = analyticsDao.getEventById(id)
        assertThat(updated?.retryCount).isEqualTo(3)

        // And - Should appear in failed events
        val failed = analyticsDao.getFailedEvents(minRetries = 3)
        assertThat(failed).hasSize(1)
        assertThat(failed[0].id).isEqualTo(id)
    }
}
