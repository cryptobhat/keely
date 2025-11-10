package com.kannada.kavi.features.clipboard

import com.google.common.truth.Truth.assertThat
import com.kannada.kavi.core.common.ClipboardContentType
import com.kannada.kavi.data.database.dao.ClipboardDao
import com.kannada.kavi.data.database.entities.ClipboardItemEntity
import com.kannada.kavi.features.clipboard.models.ClipboardItem
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * ClipboardRepositoryTest - Unit Tests for ClipboardRepository
 *
 * Tests repository logic with mocked DAO.
 *
 * TEST CATEGORIES:
 * ================
 * 1. Save Operations
 *    - Save new item
 *    - Save with auto-trim
 *    - Batch save
 *
 * 2. Load Operations
 *    - Load history
 *    - Load with limit
 *    - Load pinned only
 *
 * 3. Pin Operations
 *    - Pin item
 *    - Unpin item
 *
 * 4. Delete Operations
 *    - Delete by ID
 *    - Delete old entries
 *    - Clear unpinned
 *    - Clear all
 *
 * 5. Domain Model Conversion
 *    - Entity to domain model
 *    - Domain model to entity
 */
class ClipboardRepositoryTest {

    private lateinit var clipboardDao: ClipboardDao
    private lateinit var repository: ClipboardRepository

    @Before
    fun setup() {
        clipboardDao = mockk()
        repository = ClipboardRepository(clipboardDao)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ========================================
    // SAVE OPERATIONS
    // ========================================

    @Test
    fun `saveItem inserts entity`() = runBlocking {
        // Given
        val item = ClipboardItem(
            id = "test-id",
            text = "Test text",
            timestamp = 1000,
            isPinned = false,
            sourceApp = "TestApp",
            contentType = ClipboardContentType.TEXT
        )
        coEvery { clipboardDao.insertItem(any()) } just Runs

        // When
        repository.saveItem(item)

        // Then
        coVerify {
            clipboardDao.insertItem(
                match {
                    it.id == "test-id" &&
                    it.text == "Test text" &&
                    it.isPinned == false &&
                    it.sourceApp == "TestApp"
                }
            )
        }
    }

    @Test
    fun `saveItem with auto-trim when exceeds max limit`() = runBlocking {
        // Given
        val item = ClipboardItem(
            id = "new-item",
            text = "New text",
            timestamp = System.currentTimeMillis(),
            isPinned = false,
            contentType = ClipboardContentType.TEXT
        )
        val maxItems = 50
        coEvery { clipboardDao.getUnpinnedCount() } returns maxItems + 10 // Exceeds limit
        coEvery { clipboardDao.insertItem(any()) } just Runs
        coEvery { clipboardDao.trimOldestUnpinned(any()) } just Runs

        // When
        repository.saveItem(item)

        // Then - Should trim to max limit
        coVerify { clipboardDao.trimOldestUnpinned(maxItems) }
        coVerify { clipboardDao.insertItem(any()) }
    }

    @Test
    fun `saveItems batch inserts multiple items`() = runBlocking {
        // Given
        val items = listOf(
            ClipboardItem(id = "id1", text = "Text 1", timestamp = 1000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItem(id = "id2", text = "Text 2", timestamp = 2000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItem(id = "id3", text = "Text 3", timestamp = 3000, isPinned = false, contentType = ClipboardContentType.TEXT)
        )
        coEvery { clipboardDao.insertItems(any()) } just Runs

        // When
        repository.saveItems(items)

        // Then
        coVerify {
            clipboardDao.insertItems(
                match { entities ->
                    entities.size == 3 &&
                    entities[0].id == "id1" &&
                    entities[1].id == "id2" &&
                    entities[2].id == "id3"
                }
            )
        }
    }

    // ========================================
    // LOAD OPERATIONS
    // ========================================

    @Test
    fun `loadHistory returns domain models`() = runBlocking {
        // Given
        val entities = listOf(
            ClipboardItemEntity(
                id = "id1",
                text = "Text 1",
                timestamp = 1000,
                isPinned = false,
                sourceApp = "App1",
                contentType = ClipboardContentType.TEXT
            ),
            ClipboardItemEntity(
                id = "id2",
                text = "Text 2",
                timestamp = 2000,
                isPinned = true,
                sourceApp = "App2",
                contentType = ClipboardContentType.URL
            )
        )
        coEvery { clipboardDao.getAllItems(any()) } returns entities

        // When
        val result = repository.loadHistory(limit = 100)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo("id1")
        assertThat(result[0].text).isEqualTo("Text 1")
        assertThat(result[0].isPinned).isFalse()
        assertThat(result[1].id).isEqualTo("id2")
        assertThat(result[1].text).isEqualTo("Text 2")
        assertThat(result[1].isPinned).isTrue()
    }

    @Test
    fun `loadHistory with limit calls dao correctly`() = runBlocking {
        // Given
        coEvery { clipboardDao.getAllItems(50) } returns emptyList()

        // When
        repository.loadHistory(limit = 50)

        // Then
        coVerify { clipboardDao.getAllItems(50) }
    }

    @Test
    fun `getItemById returns correct item`() = runBlocking {
        // Given
        val entity = ClipboardItemEntity(
            id = "test-id",
            text = "Test text",
            timestamp = 1000,
            isPinned = false,
            sourceApp = null,
            contentType = ClipboardContentType.TEXT
        )
        coEvery { clipboardDao.getItemById("test-id") } returns entity

        // When
        val result = repository.getItemById("test-id")

        // Then
        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("test-id")
        assertThat(result?.text).isEqualTo("Test text")
    }

    @Test
    fun `getItemById nonexistent returns null`() = runBlocking {
        // Given
        coEvery { clipboardDao.getItemById("nonexistent") } returns null

        // When
        val result = repository.getItemById("nonexistent")

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `getPinnedItems returns only pinned`() = runBlocking {
        // Given
        val entities = listOf(
            ClipboardItemEntity(id = "pinned1", text = "Pinned 1", timestamp = 1000, isPinned = true, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "pinned2", text = "Pinned 2", timestamp = 2000, isPinned = true, contentType = ClipboardContentType.TEXT)
        )
        coEvery { clipboardDao.getPinnedItems() } returns entities

        // When
        val result = repository.getPinnedItems()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.all { it.isPinned }).isTrue()
    }

    @Test
    fun `searchItems returns matching results`() = runBlocking {
        // Given
        val entities = listOf(
            ClipboardItemEntity(id = "id1", text = "Hello World", timestamp = 1000, isPinned = false, contentType = ClipboardContentType.TEXT),
            ClipboardItemEntity(id = "id2", text = "Hello Universe", timestamp = 2000, isPinned = false, contentType = ClipboardContentType.TEXT)
        )
        coEvery { clipboardDao.searchItems("%Hello%") } returns entities

        // When
        val result = repository.searchItems("Hello")

        // Then
        assertThat(result).hasSize(2)
        coVerify { clipboardDao.searchItems("%Hello%") }
    }

    // ========================================
    // PIN OPERATIONS
    // ========================================

    @Test
    fun `setPinned updates pin status`() = runBlocking {
        // Given
        coEvery { clipboardDao.updatePinStatus("test-id", true) } just Runs

        // When
        repository.setPinned("test-id", isPinned = true)

        // Then
        coVerify { clipboardDao.updatePinStatus("test-id", true) }
    }

    @Test
    fun `setPinned to false unpins item`() = runBlocking {
        // Given
        coEvery { clipboardDao.updatePinStatus("test-id", false) } just Runs

        // When
        repository.setPinned("test-id", isPinned = false)

        // Then
        coVerify { clipboardDao.updatePinStatus("test-id", false) }
    }

    // ========================================
    // DELETE OPERATIONS
    // ========================================

    @Test
    fun `deleteItem calls dao`() = runBlocking {
        // Given
        coEvery { clipboardDao.deleteItemById("test-id") } just Runs

        // When
        repository.deleteItem("test-id")

        // Then
        coVerify { clipboardDao.deleteItemById("test-id") }
    }

    @Test
    fun `clearUnpinned calls dao`() = runBlocking {
        // Given
        coEvery { clipboardDao.clearUnpinned() } just Runs

        // When
        repository.clearUnpinned()

        // Then
        coVerify { clipboardDao.clearUnpinned() }
    }

    @Test
    fun `clearAll calls dao`() = runBlocking {
        // Given
        coEvery { clipboardDao.clearAll() } just Runs

        // When
        repository.clearAll()

        // Then
        coVerify { clipboardDao.clearAll() }
    }

    @Test
    fun `trimOldItems keeps only specified count`() = runBlocking {
        // Given
        coEvery { clipboardDao.trimOldestUnpinned(30) } just Runs

        // When
        repository.trimOldItems(maxUnpinnedItems = 30)

        // Then
        coVerify { clipboardDao.trimOldestUnpinned(30) }
    }

    // ========================================
    // COUNT OPERATIONS
    // ========================================

    @Test
    fun `getCount returns total count`() = runBlocking {
        // Given
        coEvery { clipboardDao.getCount() } returns 42

        // When
        val count = repository.getCount()

        // Then
        assertThat(count).isEqualTo(42)
    }

    @Test
    fun `getPinnedCount returns pinned count`() = runBlocking {
        // Given
        coEvery { clipboardDao.getPinnedCount() } returns 5

        // When
        val count = repository.getPinnedCount()

        // Then
        assertThat(count).isEqualTo(5)
    }

    @Test
    fun `getUnpinnedCount returns unpinned count`() = runBlocking {
        // Given
        coEvery { clipboardDao.getUnpinnedCount() } returns 37

        // When
        val count = repository.getUnpinnedCount()

        // Then
        assertThat(count).isEqualTo(37)
    }

    // ========================================
    // FLOW OPERATIONS
    // ========================================

    @Test
    fun `observeHistory emits domain models`() = runBlocking {
        // Given
        val entities = listOf(
            ClipboardItemEntity(id = "id1", text = "Text 1", timestamp = 1000, isPinned = false, contentType = ClipboardContentType.TEXT)
        )
        coEvery { clipboardDao.observeAllItems() } returns flowOf(entities)

        // When
        var emittedItems: List<ClipboardItem>? = null
        repository.observeHistory().collect { items ->
            emittedItems = items
        }

        // Then
        assertThat(emittedItems).isNotNull()
        assertThat(emittedItems).hasSize(1)
        assertThat(emittedItems?.get(0)?.text).isEqualTo("Text 1")
    }

    @Test
    fun `observeCount emits count`() = runBlocking {
        // Given
        coEvery { clipboardDao.observeCount() } returns flowOf(10)

        // When
        var emittedCount: Int? = null
        repository.observeCount().collect { count ->
            emittedCount = count
        }

        // Then
        assertThat(emittedCount).isEqualTo(10)
    }

    // ========================================
    // DOMAIN MODEL CONVERSION
    // ========================================

    @Test
    fun `entity to domain model conversion works correctly`() = runBlocking {
        // Given
        val entity = ClipboardItemEntity(
            id = "test-id",
            text = "Test text",
            timestamp = 12345,
            isPinned = true,
            sourceApp = "TestApp",
            contentType = ClipboardContentType.URL
        )
        coEvery { clipboardDao.getAllItems(any()) } returns listOf(entity)

        // When
        val result = repository.loadHistory(limit = 1)

        // Then
        val domainModel = result[0]
        assertThat(domainModel.id).isEqualTo("test-id")
        assertThat(domainModel.text).isEqualTo("Test text")
        assertThat(domainModel.timestamp).isEqualTo(12345)
        assertThat(domainModel.isPinned).isTrue()
        assertThat(domainModel.sourceApp).isEqualTo("TestApp")
        assertThat(domainModel.contentType).isEqualTo(ClipboardContentType.URL)
    }

    @Test
    fun `domain model to entity conversion works correctly`() = runBlocking {
        // Given
        val domainModel = ClipboardItem(
            id = "domain-id",
            text = "Domain text",
            timestamp = 99999,
            isPinned = false,
            sourceApp = "DomainApp",
            contentType = ClipboardContentType.EMAIL
        )
        coEvery { clipboardDao.insertItem(any()) } just Runs

        // When
        repository.saveItem(domainModel)

        // Then
        coVerify {
            clipboardDao.insertItem(
                match { entity ->
                    entity.id == "domain-id" &&
                    entity.text == "Domain text" &&
                    entity.timestamp == 99999L &&
                    entity.isPinned == false &&
                    entity.sourceApp == "DomainApp" &&
                    entity.contentType == ClipboardContentType.EMAIL
                }
            )
        }
    }

    @Test
    fun `all content types convert correctly`() = runBlocking {
        // Given
        val contentTypes = ClipboardContentType.values()
        coEvery { clipboardDao.insertItem(any()) } just Runs

        // When - Save items with each content type
        contentTypes.forEach { type ->
            val item = ClipboardItem(
                id = "id-$type",
                text = "Text",
                timestamp = 1000,
                isPinned = false,
                contentType = type
            )
            repository.saveItem(item)
        }

        // Then - Verify all content types were preserved
        coVerify(exactly = contentTypes.size) {
            clipboardDao.insertItem(
                match { entity ->
                    entity.contentType in contentTypes
                }
            )
        }
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    fun `empty history returns empty list`() = runBlocking {
        // Given
        coEvery { clipboardDao.getAllItems(any()) } returns emptyList()

        // When
        val result = repository.loadHistory()

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `saveItem with empty text is allowed`() = runBlocking {
        // Given
        val item = ClipboardItem(
            id = "empty-text",
            text = "",
            timestamp = 1000,
            isPinned = false,
            contentType = ClipboardContentType.TEXT
        )
        coEvery { clipboardDao.insertItem(any()) } just Runs
        coEvery { clipboardDao.getUnpinnedCount() } returns 0

        // When
        repository.saveItem(item)

        // Then
        coVerify {
            clipboardDao.insertItem(
                match { it.text == "" }
            )
        }
    }

    @Test
    fun `saveItem with very long text is allowed`() = runBlocking {
        // Given
        val longText = "x".repeat(10000)
        val item = ClipboardItem(
            id = "long-text",
            text = longText,
            timestamp = 1000,
            isPinned = false,
            contentType = ClipboardContentType.TEXT
        )
        coEvery { clipboardDao.insertItem(any()) } just Runs
        coEvery { clipboardDao.getUnpinnedCount() } returns 0

        // When
        repository.saveItem(item)

        // Then
        coVerify {
            clipboardDao.insertItem(
                match { it.text.length == 10000 }
            )
        }
    }

    @Test
    fun `null sourceApp is preserved`() = runBlocking {
        // Given
        val item = ClipboardItem(
            id = "no-source",
            text = "Text",
            timestamp = 1000,
            isPinned = false,
            sourceApp = null,
            contentType = ClipboardContentType.TEXT
        )
        coEvery { clipboardDao.insertItem(any()) } just Runs
        coEvery { clipboardDao.getUnpinnedCount() } returns 0

        // When
        repository.saveItem(item)

        // Then
        coVerify {
            clipboardDao.insertItem(
                match { it.sourceApp == null }
            )
        }
    }

    @Test
    fun `pinned items are not trimmed`() = runBlocking {
        // Given - 60 items total, 10 pinned, 50 unpinned
        val item = ClipboardItem(
            id = "new-item",
            text = "New",
            timestamp = System.currentTimeMillis(),
            isPinned = false,
            contentType = ClipboardContentType.TEXT
        )
        coEvery { clipboardDao.getUnpinnedCount() } returns 50
        coEvery { clipboardDao.getPinnedCount() } returns 10
        coEvery { clipboardDao.insertItem(any()) } just Runs
        coEvery { clipboardDao.trimOldestUnpinned(50) } just Runs

        // When
        repository.saveItem(item)

        // Then - Should only trim unpinned, not affect pinned count
        coVerify { clipboardDao.trimOldestUnpinned(50) }
        coVerify(exactly = 0) { clipboardDao.clearAll() }
    }
}
