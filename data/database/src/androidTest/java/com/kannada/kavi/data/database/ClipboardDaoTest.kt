package com.kannada.kavi.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kannada.kavi.core.common.ClipboardContentType
import com.kannada.kavi.data.database.dao.ClipboardDao
import com.kannada.kavi.data.database.entities.ClipboardItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ClipboardDaoTest - Comprehensive Tests for ClipboardDao
 *
 * Tests clipboard history persistence with pin support.
 *
 * TEST CATEGORIES:
 * ================
 * 1. Insert Operations
 *    - Single insert
 *    - Batch insert
 *    - Insert with auto-generated ID
 *
 * 2. Query Operations
 *    - Get by ID
 *    - Get all items (sorted by timestamp)
 *    - Get pinned items only
 *    - Get unpinned items only
 *    - Get with limit
 *    - Search by text
 *
 * 3. Update Operations
 *    - Update pin status
 *    - Update entire item
 *
 * 4. Delete Operations
 *    - Delete by ID
 *    - Delete oldest unpinned
 *    - Clear unpinned
 *    - Clear all
 *
 * 5. Count Operations
 *    - Total count
 *    - Pinned count
 *    - Unpinned count
 *
 * 6. Flow/Reactive Queries
 *    - Observe all items
 *    - Observe count
 *    - Real-time updates
 */
@RunWith(AndroidJUnit4::class)
class ClipboardDaoTest {

    private lateinit var database: KaviDatabase
    private lateinit var clipboardDao: ClipboardDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            KaviDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        clipboardDao = database.clipboardDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========================================
    // INSERT TESTS
    // ========================================

    @Test
    fun insertItem_savesToDatabase() = runBlocking {
        // Given
        val item = ClipboardItemEntity(
            id = "test-id-1",
            text = "Hello World",
            timestamp = System.currentTimeMillis(),
            isPinned = false,
            sourceApp = "TestApp",
            contentType = ClipboardContentType.TEXT
        )

        // When
        clipboardDao.insertItem(item)

        // Then
        val retrieved = clipboardDao.getItemById("test-id-1")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.text).isEqualTo("Hello World")
        assertThat(retrieved?.isPinned).isFalse()
    }

    @Test
    fun insertMultipleItems_savesAll() = runBlocking {
        // Given
        val items = listOf(
            ClipboardItemEntity(
                id = "id1",
                text = "Item 1",
                timestamp = 1000,
                isPinned = false,
                contentType = ClipboardContentType.TEXT
            ),
            ClipboardItemEntity(
                id = "id2",
                text = "Item 2",
                timestamp = 2000,
                isPinned = true,
                contentType = ClipboardContentType.TEXT
            ),
            ClipboardItemEntity(
                id = "id3",
                text = "Item 3",
                timestamp = 3000,
                isPinned = false,
                contentType = ClipboardContentType.URL
            )
        )

        // When
        clipboardDao.insertItems(items)

        // Then
        val allItems = clipboardDao.getAllItems()
        assertThat(allItems).hasSize(3)
    }

    @Test
    fun insertDuplicateId_replacesExisting() = runBlocking {
        // Given
        val item1 = ClipboardItemEntity(
            id = "same-id",
            text = "Original text",
            timestamp = 1000,
            isPinned = false,
            contentType = ClipboardContentType.TEXT
        )
        clipboardDao.insertItem(item1)

        // When
        val item2 = ClipboardItemEntity(
            id = "same-id",
            text = "Updated text",
            timestamp = 2000,
            isPinned = true,
            contentType = ClipboardContentType.TEXT
        )
        clipboardDao.insertItem(item2)

        // Then
        val allItems = clipboardDao.getAllItems()
        assertThat(allItems).hasSize(1)
        assertThat(allItems[0].text).isEqualTo("Updated text")
        assertThat(allItems[0].isPinned).isTrue()
    }

    // ========================================
    // QUERY TESTS
    // ========================================

    @Test
    fun getItemById_returnsCorrectItem() = runBlocking {
        // Given
        val items = listOf(
            ClipboardItemEntity(id = "id1", text = "Text 1", timestamp = 1000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id2", text = "Text 2", timestamp = 2000, isPinned = false, contentType = ClipboardContentType.TEXT)
        )
        clipboardDao.insertItems(items)

        // When
        val result = clipboardDao.getItemById("id2")

        // Then
        assertThat(result).isNotNull()
        assertThat(result?.text).isEqualTo("Text 2")
    }

    @Test
    fun getItemById_nonExistent_returnsNull() = runBlocking {
        // When
        val result = clipboardDao.getItemById("nonexistent")

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun getAllItems_returnsSortedByTimestamp() = runBlocking {
        // Given - Insert in random order
        val items = listOf(
            ClipboardItemEntity(id = "id2", text = "Text 2", timestamp = 2000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id1", text = "Text 1", timestamp = 1000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id3", text = "Text 3", timestamp = 3000, isPinned = false, contentType = ClipboardContentType.TEXT)
        )
        clipboardDao.insertItems(items)

        // When
        val result = clipboardDao.getAllItems()

        // Then - Should be sorted by timestamp DESC (newest first)
        assertThat(result).hasSize(3)
        assertThat(result[0].id).isEqualTo("id3") // timestamp 3000
        assertThat(result[1].id).isEqualTo("id2") // timestamp 2000
        assertThat(result[2].id).isEqualTo("id1") // timestamp 1000
    }

    @Test
    fun getAllItemsWithLimit_returnsCorrectCount() = runBlocking {
        // Given - Insert 5 items
        val items = (1..5).map { i ->
            ClipboardItemEntity(
                id = "id$i",
                text = "Text $i",
                timestamp = i.toLong() * 1000,
                isPinned = false,
                contentType = ClipboardContentType.TEXT
            )
        }
        clipboardDao.insertItems(items)

        // When - Request only 3
        val result = clipboardDao.getAllItems(limit = 3)

        // Then
        assertThat(result).hasSize(3)
    }

    @Test
    fun getPinnedItems_returnsOnlyPinned() = runBlocking {
        // Given
        val items = listOf(
            ClipboardItemEntity(id = "id1", text = "Pinned 1", timestamp = 1000, isPinned = true, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id2", text = "Not pinned", timestamp = 2000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id3", text = "Pinned 2", timestamp = 3000, isPinned = true, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id4", text = "Not pinned 2", timestamp = 4000, isPinned = false, contentType = ClipboardContentType.TEXT)
        )
        clipboardDao.insertItems(items)

        // When
        val pinned = clipboardDao.getPinnedItems()

        // Then
        assertThat(pinned).hasSize(2)
        assertThat(pinned.map { it.text }).containsExactly("Pinned 2", "Pinned 1")
    }

    @Test
    fun getUnpinnedItems_returnsOnlyUnpinned() = runBlocking {
        // Given
        val items = listOf(
            ClipboardItemEntity(id = "id1", text = "Pinned", timestamp = 1000, isPinned = true, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id2", text = "Unpinned 1", timestamp = 2000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id3", text = "Unpinned 2", timestamp = 3000, isPinned = false, contentType = ClipboardContentType.TEXT)
        )
        clipboardDao.insertItems(items)

        // When
        val unpinned = clipboardDao.getUnpinnedItems()

        // Then
        assertThat(unpinned).hasSize(2)
        assertThat(unpinned.map { it.text }).containsExactly("Unpinned 2", "Unpinned 1")
    }

    @Test
    fun searchItems_findsMatchingText() = runBlocking {
        // Given
        val items = listOf(
            ClipboardItemEntity(id = "id1", text = "Hello World", timestamp = 1000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id2", text = "Goodbye World", timestamp = 2000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id3", text = "Hello Universe", timestamp = 3000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id4", text = "Nothing here", timestamp = 4000, isPinned = false, contentType = ClipboardContentType.TEXT)
        )
        clipboardDao.insertItems(items)

        // When
        val results = clipboardDao.searchItems("%Hello%")

        // Then
        assertThat(results).hasSize(2)
        assertThat(results.map { it.text }).containsExactly("Hello Universe", "Hello World")
    }

    // ========================================
    // UPDATE TESTS
    // ========================================

    @Test
    fun updatePinStatus_changesFlag() = runBlocking {
        // Given
        val item = ClipboardItemEntity(
            id = "test-id",
            text = "Test text",
            timestamp = 1000,
            isPinned = false,
            contentType = ClipboardContentType.TEXT
        )
        clipboardDao.insertItem(item)

        // When - Pin the item
        clipboardDao.updatePinStatus("test-id", isPinned = true)

        // Then
        val updated = clipboardDao.getItemById("test-id")
        assertThat(updated?.isPinned).isTrue()

        // When - Unpin the item
        clipboardDao.updatePinStatus("test-id", isPinned = false)

        // Then
        val unpinned = clipboardDao.getItemById("test-id")
        assertThat(unpinned?.isPinned).isFalse()
    }

    @Test
    fun updateItem_changesAllFields() = runBlocking {
        // Given
        val original = ClipboardItemEntity(
            id = "test-id",
            text = "Original text",
            timestamp = 1000,
            isPinned = false,
            sourceApp = "App1",
            contentType = ClipboardContentType.TEXT
        )
        clipboardDao.insertItem(original)

        // When
        val updated = ClipboardItemEntity(
            id = "test-id",
            text = "Updated text",
            timestamp = 2000,
            isPinned = true,
            sourceApp = "App2",
            contentType = ClipboardContentType.URL
        )
        clipboardDao.updateItem(updated)

        // Then
        val result = clipboardDao.getItemById("test-id")
        assertThat(result?.text).isEqualTo("Updated text")
        assertThat(result?.timestamp).isEqualTo(2000)
        assertThat(result?.isPinned).isTrue()
        assertThat(result?.sourceApp).isEqualTo("App2")
        assertThat(result?.contentType).isEqualTo(ClipboardContentType.URL)
    }

    // ========================================
    // DELETE TESTS
    // ========================================

    @Test
    fun deleteItemById_removesItem() = runBlocking {
        // Given
        val items = listOf(
            ClipboardItemEntity(id = "id1", text = "Text 1", timestamp = 1000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id2", text = "Text 2", timestamp = 2000, isPinned = false, contentType = ClipboardContentType.TEXT)
        )
        clipboardDao.insertItems(items)

        // When
        clipboardDao.deleteItemById("id1")

        // Then
        assertThat(clipboardDao.getItemById("id1")).isNull()
        assertThat(clipboardDao.getItemById("id2")).isNotNull()
        assertThat(clipboardDao.getAllItems()).hasSize(1)
    }

    @Test
    fun trimOldestUnpinned_removesCorrectItems() = runBlocking {
        // Given - 5 items, 2 pinned, 3 unpinned
        val items = listOf(
            ClipboardItemEntity(id = "old1", text = "Old 1", timestamp = 1000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "pinned1", text = "Pinned 1", timestamp = 2000, isPinned = true, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "old2", text = "Old 2", timestamp = 3000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "pinned2", text = "Pinned 2", timestamp = 4000, isPinned = true, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "recent", text = "Recent", timestamp = 5000, isPinned = false, contentType = ClipboardContentType.TEXT)
        )
        clipboardDao.insertItems(items)

        // When - Keep only 1 unpinned item (delete 2 oldest)
        clipboardDao.trimOldestUnpinned(maxUnpinnedItems = 1)

        // Then - Should keep: pinned1, pinned2, recent (most recent unpinned)
        val remaining = clipboardDao.getAllItems()
        assertThat(remaining).hasSize(3)
        assertThat(remaining.map { it.id }).containsExactly("recent", "pinned2", "pinned1")
    }

    @Test
    fun clearUnpinned_removesOnlyUnpinned() = runBlocking {
        // Given
        val items = listOf(
            ClipboardItemEntity(id = "pinned", text = "Pinned", timestamp = 1000, isPinned = true, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "unpinned1", text = "Unpinned 1", timestamp = 2000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "unpinned2", text = "Unpinned 2", timestamp = 3000, isPinned = false, contentType = ClipboardContentType.TEXT)
        )
        clipboardDao.insertItems(items)

        // When
        clipboardDao.clearUnpinned()

        // Then
        val remaining = clipboardDao.getAllItems()
        assertThat(remaining).hasSize(1)
        assertThat(remaining[0].id).isEqualTo("pinned")
    }

    @Test
    fun clearAll_removesEverything() = runBlocking {
        // Given
        val items = (1..10).map { i ->
            ClipboardItemEntity(
                id = "id$i",
                text = "Text $i",
                timestamp = i.toLong() * 1000,
                isPinned = i % 2 == 0, // Even items are pinned
                contentType = ClipboardContentType.TEXT
            )
        }
        clipboardDao.insertItems(items)
        assertThat(clipboardDao.getAllItems()).hasSize(10)

        // When
        clipboardDao.clearAll()

        // Then
        assertThat(clipboardDao.getAllItems()).isEmpty()
    }

    // ========================================
    // COUNT TESTS
    // ========================================

    @Test
    fun getCount_returnsCorrectTotal() = runBlocking {
        // Given
        val items = (1..7).map { i ->
            ClipboardItemEntity(
                id = "id$i",
                text = "Text $i",
                timestamp = i.toLong() * 1000,
                isPinned = false,
                contentType = ClipboardContentType.TEXT
            )
        }
        clipboardDao.insertItems(items)

        // When
        val count = clipboardDao.getCount()

        // Then
        assertThat(count).isEqualTo(7)
    }

    @Test
    fun getPinnedCount_returnsCorrectCount() = runBlocking {
        // Given
        val items = listOf(
            ClipboardItemEntity(id = "id1", text = "Text 1", timestamp = 1000, isPinned = true, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id2", text = "Text 2", timestamp = 2000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id3", text = "Text 3", timestamp = 3000, isPinned = true, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id4", text = "Text 4", timestamp = 4000, isPinned = true, contentType = ClipboardContentType.TEXT)
        )
        clipboardDao.insertItems(items)

        // When
        val count = clipboardDao.getPinnedCount()

        // Then
        assertThat(count).isEqualTo(3)
    }

    @Test
    fun getUnpinnedCount_returnsCorrectCount() = runBlocking {
        // Given
        val items = listOf(
            ClipboardItemEntity(id = "id1", text = "Text 1", timestamp = 1000, isPinned = true, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id2", text = "Text 2", timestamp = 2000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id3", text = "Text 3", timestamp = 3000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id4", text = "Text 4", timestamp = 4000, isPinned = false, contentType = ClipboardContentType.TEXT)
        )
        clipboardDao.insertItems(items)

        // When
        val count = clipboardDao.getUnpinnedCount()

        // Then
        assertThat(count).isEqualTo(3)
    }

    // ========================================
    // FLOW/REACTIVE TESTS
    // ========================================

    @Test
    fun observeAllItems_emitsUpdates() = runBlocking {
        // Given - Empty database
        var emittedItems = clipboardDao.observeAllItems().first()
        assertThat(emittedItems).isEmpty()

        // When - Insert item
        val item = ClipboardItemEntity(
            id = "test-id",
            text = "Test text",
            timestamp = 1000,
            isPinned = false,
            contentType = ClipboardContentType.TEXT
        )
        clipboardDao.insertItem(item)

        // Then - Flow should emit new data
        emittedItems = clipboardDao.observeAllItems().first()
        assertThat(emittedItems).hasSize(1)
        assertThat(emittedItems[0].text).isEqualTo("Test text")
    }

    @Test
    fun observeCount_emitsCorrectCount() = runBlocking {
        // Given - Empty database
        var count = clipboardDao.observeCount().first()
        assertThat(count).isEqualTo(0)

        // When - Insert 3 items
        val items = (1..3).map { i ->
            ClipboardItemEntity(
                id = "id$i",
                text = "Text $i",
                timestamp = i.toLong() * 1000,
                isPinned = false,
                contentType = ClipboardContentType.TEXT
            )
        }
        clipboardDao.insertItems(items)

        // Then
        count = clipboardDao.observeCount().first()
        assertThat(count).isEqualTo(3)
    }

    // ========================================
    // CONTENT TYPE TESTS
    // ========================================

    @Test
    fun contentType_persistsCorrectly() = runBlocking {
        // Given
        val items = listOf(
            ClipboardItemEntity(id = "text", text = "Plain text", timestamp = 1000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "url", text = "https://example.com", timestamp = 2000, isPinned = false, contentType = ClipboardContentType.URL),
            ClipboardItemEntity(id = "email", text = "test@example.com", timestamp = 3000, isPinned = false, contentType = ClipboardContentType.EMAIL),
            ClipboardItemEntity(id = "phone", text = "+1234567890", timestamp = 4000, isPinned = false, contentType = ClipboardContentType.PHONE),
            ClipboardItemEntity(id = "code", text = "function test() {}", timestamp = 5000, isPinned = false, contentType = ClipboardContentType.CODE)
        )
        clipboardDao.insertItems(items)

        // When
        val retrieved = clipboardDao.getAllItems()

        // Then
        assertThat(retrieved).hasSize(5)
        assertThat(retrieved.find { it.id == "text" }?.contentType).isEqualTo(ClipboardContentType.TEXT)
        assertThat(retrieved.find { it.id == "url" }?.contentType).isEqualTo(ClipboardContentType.URL)
        assertThat(retrieved.find { it.id == "email" }?.contentType).isEqualTo(ClipboardContentType.EMAIL)
        assertThat(retrieved.find { it.id == "phone" }?.contentType).isEqualTo(ClipboardContentType.PHONE)
        assertThat(retrieved.find { it.id == "code" }?.contentType).isEqualTo(ClipboardContentType.CODE)
    }

    // ========================================
    // EDGE CASE TESTS
    // ========================================

    @Test
    fun emptyText_handledCorrectly() = runBlocking {
        // Given
        val item = ClipboardItemEntity(
            id = "empty",
            text = "",
            timestamp = 1000,
            isPinned = false,
            contentType = ClipboardContentType.TEXT
        )

        // When
        clipboardDao.insertItem(item)

        // Then
        val retrieved = clipboardDao.getItemById("empty")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.text).isEmpty()
    }

    @Test
    fun veryLongText_handledCorrectly() = runBlocking {
        // Given - 10,000 character text
        val longText = "x".repeat(10000)
        val item = ClipboardItemEntity(
            id = "long",
            text = longText,
            timestamp = 1000,
            isPinned = false,
            contentType = ClipboardContentType.TEXT
        )

        // When
        clipboardDao.insertItem(item)

        // Then
        val retrieved = clipboardDao.getItemById("long")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.text).hasLength(10000)
    }

    @Test
    fun nullSourceApp_handledCorrectly() = runBlocking {
        // Given
        val item = ClipboardItemEntity(
            id = "no-source",
            text = "Test text",
            timestamp = 1000,
            isPinned = false,
            sourceApp = null,
            contentType = ClipboardContentType.TEXT
        )

        // When
        clipboardDao.insertItem(item)

        // Then
        val retrieved = clipboardDao.getItemById("no-source")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.sourceApp).isNull()
    }
}
