package com.kannada.kavi.features.clipboard

import com.kannada.kavi.data.database.dao.ClipboardDao
import com.kannada.kavi.data.database.entities.ClipboardItemEntity
import com.kannada.kavi.features.clipboard.models.ClipboardItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing clipboard history with persistent storage.
 *
 * This repository provides:
 * - Persistent clipboard history across keyboard sessions
 * - In-memory caching for fast access
 * - Reactive updates via Flow
 * - Pin/unpin functionality
 * - Search and filtering
 *
 * The repository bridges the gap between ClipboardManager (in-memory)
 * and ClipboardDao (database), ensuring clipboard history persists even
 * after the keyboard is closed.
 */
@Singleton
class ClipboardRepository @Inject constructor(
    private val clipboardDao: ClipboardDao
) {

    /**
     * Save a clipboard item to persistent storage.
     *
     * @param item The clipboard item to save
     */
    suspend fun saveItem(item: ClipboardItem) {
        clipboardDao.insertItem(item.toEntity())
    }

    /**
     * Save multiple clipboard items (batch operation).
     * Useful for restoring clipboard history.
     *
     * @param items List of clipboard items to save
     */
    suspend fun saveItems(items: List<ClipboardItem>) {
        val entities = items.map { it.toEntity() }
        clipboardDao.insertItems(entities)
    }

    /**
     * Get all clipboard items ordered by pinned status and timestamp.
     * Pinned items appear first, followed by most recent items.
     *
     * @return List of clipboard items
     */
    suspend fun getAllItems(): List<ClipboardItem> {
        return clipboardDao.getAllItems().map { it.toDomainModel() }
    }

    /**
     * Get recent clipboard items with a limit.
     * Useful for displaying in the clipboard popup UI.
     *
     * @param limit Maximum number of items to return (default: 50)
     * @return List of clipboard items
     */
    suspend fun getRecentItems(limit: Int = 50): List<ClipboardItem> {
        return clipboardDao.getRecentItems(limit).map { it.toDomainModel() }
    }

    /**
     * Get only pinned clipboard items.
     * These are items the user wants to keep permanently.
     *
     * @return List of pinned clipboard items
     */
    suspend fun getPinnedItems(): List<ClipboardItem> {
        return clipboardDao.getPinnedItems().map { it.toDomainModel() }
    }

    /**
     * Search clipboard items by text content.
     * Case-insensitive search.
     *
     * @param query Search query string
     * @param limit Maximum number of results (default: 50)
     * @return List of matching clipboard items
     */
    suspend fun searchItems(query: String, limit: Int = 50): List<ClipboardItem> {
        return clipboardDao.searchItems(query, limit).map { it.toDomainModel() }
    }

    /**
     * Observe clipboard items as Flow for reactive UI.
     * UI will automatically update when items are added/removed.
     *
     * @param limit Maximum number of items to observe (default: 50)
     * @return Flow of clipboard items list
     */
    fun observeItems(limit: Int = 50): Flow<List<ClipboardItem>> {
        return clipboardDao.observeItems(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Observe only pinned items as Flow.
     *
     * @return Flow of pinned clipboard items
     */
    fun observePinnedItems(): Flow<List<ClipboardItem>> {
        return clipboardDao.observePinnedItems().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Update the pin status of a clipboard item.
     *
     * @param id Clipboard item ID
     * @param isPinned New pin status
     */
    suspend fun updatePinStatus(id: String, isPinned: Boolean) {
        clipboardDao.updatePinStatus(id, isPinned)
    }

    /**
     * Toggle pin status of a clipboard item.
     *
     * @param item The clipboard item to toggle
     * @return New pin status
     */
    suspend fun togglePin(item: ClipboardItem): Boolean {
        val newPinStatus = !item.isPinned
        clipboardDao.updatePinStatus(item.id, newPinStatus)
        return newPinStatus
    }

    /**
     * Delete a specific clipboard item.
     *
     * @param id Clipboard item ID
     */
    suspend fun deleteItem(id: String) {
        clipboardDao.deleteItem(id)
    }

    /**
     * Delete multiple clipboard items (batch deletion).
     *
     * @param ids List of clipboard item IDs to delete
     */
    suspend fun deleteItems(ids: List<String>) {
        clipboardDao.deleteItems(ids)
    }

    /**
     * Delete all non-pinned items.
     * Useful for "clear history" functionality.
     */
    suspend fun deleteAllUnpinned() {
        clipboardDao.deleteAllUnpinned()
    }

    /**
     * Delete all items including pinned (nuclear option).
     */
    suspend fun clearAll() {
        clipboardDao.clearAll()
    }

    /**
     * Trim clipboard history to maintain maximum capacity.
     * Keeps only the most recent N unpinned items.
     * Pinned items are never deleted.
     *
     * @param maxUnpinned Maximum number of unpinned items to keep (default: 50)
     */
    suspend fun trimHistory(maxUnpinned: Int = 50) {
        clipboardDao.trimOldestUnpinned(maxUnpinned)
    }

    /**
     * Get clipboard statistics.
     *
     * @return Pair of (totalItems, pinnedItems)
     */
    suspend fun getStatistics(): Pair<Int, Int> {
        val total = clipboardDao.getItemCount()
        val pinned = clipboardDao.getPinnedItemCount()
        return Pair(total, pinned)
    }

    /**
     * Check if a specific text already exists in clipboard.
     * Useful to avoid duplicates.
     *
     * @param text The text to check
     * @return True if text exists in clipboard
     */
    suspend fun itemExists(text: String): Boolean {
        return clipboardDao.itemExists(text)
    }

    /**
     * Get a specific clipboard item by ID.
     *
     * @param id Clipboard item ID
     * @return ClipboardItem or null if not found
     */
    suspend fun getItem(id: String): ClipboardItem? {
        return clipboardDao.getItem(id)?.toDomainModel()
    }

    /**
     * Load clipboard history from database on app startup.
     * This restores the clipboard state from the last session.
     *
     * @param limit Maximum number of items to load (default: 50)
     * @return List of clipboard items
     */
    suspend fun loadHistory(limit: Int = 50): List<ClipboardItem> {
        return getRecentItems(limit)
    }

    /**
     * Export clipboard history for backup.
     *
     * @return All clipboard items
     */
    suspend fun exportHistory(): List<ClipboardItem> {
        return getAllItems()
    }

    /**
     * Import clipboard history from backup.
     * Replaces existing clipboard with imported items.
     *
     * @param items List of clipboard items to import
     */
    suspend fun importHistory(items: List<ClipboardItem>) {
        clearAll()
        saveItems(items)
    }
}

/**
 * Extension function to convert database entity to domain model.
 */
private fun ClipboardItemEntity.toDomainModel() = ClipboardItem(
    id = id,
    text = text,
    timestamp = timestamp,
    isPinned = isPinned,
    sourceApp = sourceApp,
    contentType = contentType
)

/**
 * Extension function to convert domain model to database entity.
 */
private fun ClipboardItem.toEntity() = ClipboardItemEntity(
    id = id,
    text = text,
    timestamp = timestamp,
    isPinned = isPinned,
    sourceApp = sourceApp,
    contentType = contentType
)
