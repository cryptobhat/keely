package com.kannada.kavi.data.repositories

import com.google.common.truth.Truth.assertThat
import com.kannada.kavi.data.database.dao.AnalyticsDao
import com.kannada.kavi.data.database.entities.AnalyticsEventEntity
import com.kannada.kavi.data.repositories.models.AnalyticsEvent
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * AnalyticsRepositoryTest - Unit Tests for AnalyticsRepository
 *
 * Tests repository logic with mocked DAO.
 *
 * TEST CATEGORIES:
 * ================
 * 1. Queue Operations
 *    - Queue new event
 *    - Queue with properties
 *    - Queue returns ID
 *
 * 2. Sync Operations
 *    - Get pending events
 *    - Mark as synced
 *    - Increment retry count
 *
 * 3. Query Operations
 *    - Get by ID
 *    - Get by name
 *    - Get by date range
 *    - Get failed events
 *
 * 4. Delete Operations
 *    - Delete synced events
 *    - Delete old events
 *    - Clear all
 *
 * 5. Domain Model Conversion
 *    - Entity to domain model
 *    - Domain model helpers
 */
class AnalyticsRepositoryTest {

    private lateinit var analyticsDao: AnalyticsDao
    private lateinit var repository: AnalyticsRepository

    @Before
    fun setup() {
        analyticsDao = mockk()
        repository = AnalyticsRepository(analyticsDao)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ========================================
    // QUEUE OPERATIONS
    // ========================================

    @Test
    fun `queueEvent inserts entity and returns ID`() = runBlocking {
        // Given
        val eventName = "key_pressed"
        val properties = mapOf("key" to "ಅ", "layout" to "kavi")
        coEvery { analyticsDao.insertEvent(any()) } returns 42L

        // When
        val id = repository.queueEvent(eventName, properties)

        // Then
        assertThat(id).isEqualTo(42L)
        coVerify {
            analyticsDao.insertEvent(
                match {
                    it.eventName == "key_pressed" &&
                    it.isSynced == false &&
                    it.retryCount == 0 &&
                    it.properties == properties
                }
            )
        }
    }

    @Test
    fun `queueEvent with empty properties`() = runBlocking {
        // Given
        coEvery { analyticsDao.insertEvent(any()) } returns 1L

        // When
        repository.queueEvent("simple_event", emptyMap())

        // Then
        coVerify {
            analyticsDao.insertEvent(
                match { it.properties.isEmpty() }
            )
        }
    }

    @Test
    fun `queueEvent with complex nested properties`() = runBlocking {
        // Given
        val properties = mapOf(
            "simple" to "value",
            "number" to 42,
            "boolean" to true,
            "nested" to mapOf("inner" to "value")
        )
        coEvery { analyticsDao.insertEvent(any()) } returns 1L

        // When
        repository.queueEvent("complex_event", properties)

        // Then
        coVerify {
            analyticsDao.insertEvent(
                match { it.properties == properties }
            )
        }
    }

    // ========================================
    // SYNC OPERATIONS
    // ========================================

    @Test
    fun `getPendingEvents returns domain models`() = runBlocking {
        // Given
        val entities = listOf(
            AnalyticsEventEntity(
                id = 1,
                eventName = "event1",
                timestamp = 1000,
                properties = mapOf("key" to "value"),
                isSynced = false,
                retryCount = 0
            ),
            AnalyticsEventEntity(
                id = 2,
                eventName = "event2",
                timestamp = 2000,
                properties = emptyMap(),
                isSynced = false,
                retryCount = 1
            )
        )
        coEvery { analyticsDao.getUnsyncedEvents(any()) } returns entities

        // When
        val result = repository.getPendingEvents(limit = 100)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].eventName).isEqualTo("event1")
        assertThat(result[0].isSynced).isFalse()
        assertThat(result[1].eventName).isEqualTo("event2")
        assertThat(result[1].retryCount).isEqualTo(1)
    }

    @Test
    fun `getPendingEvents with limit calls dao correctly`() = runBlocking {
        // Given
        coEvery { analyticsDao.getUnsyncedEvents(50) } returns emptyList()

        // When
        repository.getPendingEvents(limit = 50)

        // Then
        coVerify { analyticsDao.getUnsyncedEvents(50) }
    }

    @Test
    fun `markAsSynced calls dao`() = runBlocking {
        // Given
        coEvery { analyticsDao.markAsSynced(42L) } just Runs

        // When
        repository.markAsSynced(42L)

        // Then
        coVerify { analyticsDao.markAsSynced(42L) }
    }

    @Test
    fun `incrementRetryCount calls dao`() = runBlocking {
        // Given
        coEvery { analyticsDao.incrementRetryCount(42L) } just Runs

        // When
        repository.incrementRetryCount(42L)

        // Then
        coVerify { analyticsDao.incrementRetryCount(42L) }
    }

    // ========================================
    // QUERY OPERATIONS
    // ========================================

    @Test
    fun `getEventById returns domain model`() = runBlocking {
        // Given
        val entity = AnalyticsEventEntity(
            id = 42,
            eventName = "test_event",
            timestamp = 12345,
            properties = mapOf("key" to "value"),
            isSynced = true,
            retryCount = 0
        )
        coEvery { analyticsDao.getEventById(42) } returns entity

        // When
        val result = repository.getEventById(42)

        // Then
        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(42)
        assertThat(result?.eventName).isEqualTo("test_event")
        assertThat(result?.isSynced).isTrue()
    }

    @Test
    fun `getEventById nonexistent returns null`() = runBlocking {
        // Given
        coEvery { analyticsDao.getEventById(999) } returns null

        // When
        val result = repository.getEventById(999)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `getEventsByName filters correctly`() = runBlocking {
        // Given
        val entities = listOf(
            AnalyticsEventEntity(id = 1, eventName = "key_pressed", timestamp = 1000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(id = 2, eventName = "key_pressed", timestamp = 2000, properties = emptyMap(), isSynced = false, retryCount = 0)
        )
        coEvery { analyticsDao.getEventsByName("key_pressed") } returns entities

        // When
        val result = repository.getEventsByName("key_pressed")

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.all { it.eventName == "key_pressed" }).isTrue()
    }

    @Test
    fun `getEventsByDateRange filters correctly`() = runBlocking {
        // Given
        val startTime = 1000L
        val endTime = 5000L
        val entities = listOf(
            AnalyticsEventEntity(id = 1, eventName = "event1", timestamp = 2000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(id = 2, eventName = "event2", timestamp = 3000, properties = emptyMap(), isSynced = false, retryCount = 0)
        )
        coEvery { analyticsDao.getEventsByDateRange(startTime, endTime) } returns entities

        // When
        val result = repository.getEventsByDateRange(startTime, endTime)

        // Then
        assertThat(result).hasSize(2)
        coVerify { analyticsDao.getEventsByDateRange(startTime, endTime) }
    }

    @Test
    fun `getFailedEvents returns events with retries`() = runBlocking {
        // Given
        val entities = listOf(
            AnalyticsEventEntity(id = 1, eventName = "failed1", timestamp = 1000, properties = emptyMap(), isSynced = false, retryCount = 3),
            AnalyticsEventEntity(id = 2, eventName = "failed2", timestamp = 2000, properties = emptyMap(), isSynced = false, retryCount = 5)
        )
        coEvery { analyticsDao.getFailedEvents(3) } returns entities

        // When
        val result = repository.getFailedEvents(minRetries = 3)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.all { it.retryCount >= 3 }).isTrue()
    }

    // ========================================
    // DELETE OPERATIONS
    // ========================================

    @Test
    fun `deleteSyncedEvents calls dao`() = runBlocking {
        // Given
        coEvery { analyticsDao.deleteSyncedEvents() } just Runs

        // When
        repository.deleteSyncedEvents()

        // Then
        coVerify { analyticsDao.deleteSyncedEvents() }
    }

    @Test
    fun `deleteOldEvents calculates correct cutoff`() = runBlocking {
        // Given
        val daysToKeep = 7
        coEvery { analyticsDao.deleteOldEvents(any()) } just Runs

        // When
        repository.deleteOldEvents(daysToKeep)

        // Then
        coVerify {
            analyticsDao.deleteOldEvents(
                match { cutoffTime ->
                    // Verify cutoff is approximately 7 days ago (within 1 second tolerance)
                    val expectedCutoff = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
                    Math.abs(cutoffTime - expectedCutoff) < 1000
                }
            )
        }
    }

    @Test
    fun `deleteEvent calls dao`() = runBlocking {
        // Given
        coEvery { analyticsDao.deleteEvent(42) } just Runs

        // When
        repository.deleteEvent(42)

        // Then
        coVerify { analyticsDao.deleteEvent(42) }
    }

    @Test
    fun `clearAll calls dao`() = runBlocking {
        // Given
        coEvery { analyticsDao.clearAll() } just Runs

        // When
        repository.clearAll()

        // Then
        coVerify { analyticsDao.clearAll() }
    }

    // ========================================
    // COUNT OPERATIONS
    // ========================================

    @Test
    fun `getTotalCount returns correct count`() = runBlocking {
        // Given
        coEvery { analyticsDao.getCount() } returns 100

        // When
        val count = repository.getTotalCount()

        // Then
        assertThat(count).isEqualTo(100)
    }

    @Test
    fun `getPendingCount returns unsynced count`() = runBlocking {
        // Given
        coEvery { analyticsDao.getUnsyncedCount() } returns 42

        // When
        val count = repository.getPendingCount()

        // Then
        assertThat(count).isEqualTo(42)
    }

    // ========================================
    // FLOW OPERATIONS
    // ========================================

    @Test
    fun `observePendingEvents emits domain models`() = runBlocking {
        // Given
        val entities = listOf(
            AnalyticsEventEntity(id = 1, eventName = "event1", timestamp = 1000, properties = emptyMap(), isSynced = false, retryCount = 0)
        )
        coEvery { analyticsDao.observeUnsyncedEvents() } returns flowOf(entities)

        // When
        var emittedEvents: List<AnalyticsEvent>? = null
        repository.observePendingEvents().collect { events ->
            emittedEvents = events
        }

        // Then
        assertThat(emittedEvents).isNotNull()
        assertThat(emittedEvents).hasSize(1)
        assertThat(emittedEvents?.get(0)?.eventName).isEqualTo("event1")
    }

    @Test
    fun `observePendingCount emits count`() = runBlocking {
        // Given
        coEvery { analyticsDao.observeUnsyncedCount() } returns flowOf(7)

        // When
        var emittedCount: Int? = null
        repository.observePendingCount().collect { count ->
            emittedCount = count
        }

        // Then
        assertThat(emittedCount).isEqualTo(7)
    }

    // ========================================
    // DOMAIN MODEL CONVERSION
    // ========================================

    @Test
    fun `entity to domain model conversion works correctly`() = runBlocking {
        // Given
        val entity = AnalyticsEventEntity(
            id = 42,
            eventName = "test_event",
            timestamp = 12345,
            properties = mapOf("key" to "value", "count" to 10),
            isSynced = true,
            retryCount = 3
        )
        coEvery { analyticsDao.getEventById(42) } returns entity

        // When
        val result = repository.getEventById(42)

        // Then
        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(42)
        assertThat(result?.eventName).isEqualTo("test_event")
        assertThat(result?.timestamp).isEqualTo(12345)
        assertThat(result?.properties).containsEntry("key", "value")
        assertThat(result?.properties).containsEntry("count", 10)
        assertThat(result?.isSynced).isTrue()
        assertThat(result?.retryCount).isEqualTo(3)
    }

    @Test
    fun `domain model isSynced helper works correctly`() {
        // Given
        val syncedEvent = AnalyticsEvent(
            id = 1,
            eventName = "synced",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = true,
            retryCount = 0
        )
        val unsyncedEvent = AnalyticsEvent(
            id = 2,
            eventName = "unsynced",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = false,
            retryCount = 0
        )

        // Then
        assertThat(syncedEvent.isSynced).isTrue()
        assertThat(unsyncedEvent.isSynced).isFalse()
    }

    @Test
    fun `domain model isPending helper works correctly`() {
        // Given
        val pendingEvent = AnalyticsEvent(
            id = 1,
            eventName = "pending",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = false,
            retryCount = 0
        )
        val syncedEvent = AnalyticsEvent(
            id = 2,
            eventName = "synced",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = true,
            retryCount = 0
        )

        // Then
        assertThat(pendingEvent.isPending).isTrue()
        assertThat(syncedEvent.isPending).isFalse()
    }

    @Test
    fun `domain model hasFailed helper works correctly`() {
        // Given
        val failedEvent = AnalyticsEvent(
            id = 1,
            eventName = "failed",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = false,
            retryCount = 3
        )
        val successEvent = AnalyticsEvent(
            id = 2,
            eventName = "success",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = false,
            retryCount = 0
        )

        // Then
        assertThat(failedEvent.hasFailed).isTrue()
        assertThat(successEvent.hasFailed).isFalse()
    }

    @Test
    fun `domain model shouldRetry helper works correctly`() {
        // Given
        val retryableEvent = AnalyticsEvent(
            id = 1,
            eventName = "retryable",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = false,
            retryCount = 2
        )
        val maxRetriesEvent = AnalyticsEvent(
            id = 2,
            eventName = "max_retries",
            timestamp = 1000,
            properties = emptyMap(),
            isSynced = false,
            retryCount = 5
        )

        // Then
        assertThat(retryableEvent.shouldRetry).isTrue()
        assertThat(maxRetriesEvent.shouldRetry).isFalse()
    }

    // ========================================
    // SYNC QUEUE BEHAVIOR
    // ========================================

    @Test
    fun `queue maintains FIFO order`() = runBlocking {
        // Given - Events in chronological order
        val entities = listOf(
            AnalyticsEventEntity(id = 1, eventName = "first", timestamp = 1000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(id = 2, eventName = "second", timestamp = 2000, properties = emptyMap(), isSynced = false, retryCount = 0),
            AnalyticsEventEntity(id = 3, eventName = "third", timestamp = 3000, properties = emptyMap(), isSynced = false, retryCount = 0)
        )
        coEvery { analyticsDao.getUnsyncedEvents(any()) } returns entities

        // When
        val result = repository.getPendingEvents()

        // Then - Should be in insertion order (FIFO)
        assertThat(result).hasSize(3)
        assertThat(result[0].eventName).isEqualTo("first")
        assertThat(result[1].eventName).isEqualTo("second")
        assertThat(result[2].eventName).isEqualTo("third")
    }

    @Test
    fun `batch processing with limit`() = runBlocking {
        // Given - 100 events, request 10
        val entities = (1..10).map { i ->
            AnalyticsEventEntity(
                id = i.toLong(),
                eventName = "event$i",
                timestamp = i.toLong() * 1000,
                properties = emptyMap(),
                isSynced = false,
                retryCount = 0
            )
        }
        coEvery { analyticsDao.getUnsyncedEvents(10) } returns entities

        // When
        val result = repository.getPendingEvents(limit = 10)

        // Then
        assertThat(result).hasSize(10)
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    fun `empty queue returns empty list`() = runBlocking {
        // Given
        coEvery { analyticsDao.getUnsyncedEvents(any()) } returns emptyList()

        // When
        val result = repository.getPendingEvents()

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `queueEvent with null values in properties is handled`() = runBlocking {
        // Given
        val properties = mapOf<String, Any?>(
            "key1" to "value1",
            "key2" to null
        )
        coEvery { analyticsDao.insertEvent(any()) } returns 1L

        // When
        repository.queueEvent("event_with_null", properties as Map<String, Any>)

        // Then
        coVerify { analyticsDao.insertEvent(any()) }
    }

    @Test
    fun `deleteOldEvents with zero days keeps everything`() = runBlocking {
        // Given
        coEvery { analyticsDao.deleteOldEvents(any()) } just Runs

        // When
        repository.deleteOldEvents(daysToKeep = 0)

        // Then
        coVerify {
            analyticsDao.deleteOldEvents(
                match { cutoff ->
                    // Should be approximately now
                    Math.abs(cutoff - System.currentTimeMillis()) < 1000
                }
            )
        }
    }

    @Test
    fun `retry count increments correctly through multiple calls`() = runBlocking {
        // Given
        coEvery { analyticsDao.incrementRetryCount(42) } just Runs

        // When - Simulate 3 failed attempts
        repository.incrementRetryCount(42)
        repository.incrementRetryCount(42)
        repository.incrementRetryCount(42)

        // Then
        coVerify(exactly = 3) { analyticsDao.incrementRetryCount(42) }
    }
}
