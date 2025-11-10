package com.kannada.kavi.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kannada.kavi.data.database.entities.ClipboardItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for clipboard history.
 * Provides methods to store, retrieve, and manage clipboard items.
 */
@Dao
interface ClipboardDao {

    /**
     * Insert a new clipboard item or replace if ID already exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ClipboardItemEntity)

    /**
     * Insert multiple clipboard items at once (batch operation).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ClipboardItemEntity>)

    /**
     * Get a specific clipboard item by ID.
     */
    @Query("SELECT * FROM clipboard_items WHERE id = :id LIMIT 1")
    suspend fun getItem(id: String): ClipboardItemEntity?

    /**
     * Get all clipboard items ordered by pinned status and timestamp.
     * Pinned items appear first, then most recent items.
     */
    @Query("""
        SELECT * FROM clipboard_items
        ORDER BY is_pinned DESC, timestamp DESC
    """)
    suspend fun getAllItems(): List<ClipboardItemEntity>

    /**
     * Get limited number of clipboard items (for UI display).
     * Pinned items first, then most recent.
     */
    @Query("""
        SELECT * FROM clipboard_items
        ORDER BY is_pinned DESC, timestamp DESC
        LIMIT :limit
    """)
    suspend fun getRecentItems(limit: Int = 50): List<ClipboardItemEntity>

    /**
     * Get only pinned items.
     */
    @Query("SELECT * FROM clipboard_items WHERE is_pinned = 1 ORDER BY timestamp DESC")
    suspend fun getPinnedItems(): List<ClipboardItemEntity>

    /**
     * Get non-pinned items (for cleanup when reaching max capacity).
     */
    @Query("SELECT * FROM clipboard_items WHERE is_pinned = 0 ORDER BY timestamp DESC")
    suspend fun getUnpinnedItems(): List<ClipboardItemEntity>

    /**
     * Search clipboard items by text content (case-insensitive).
     */
    @Query("""
        SELECT * FROM clipboard_items
        WHERE text LIKE '%' || :query || '%'
        ORDER BY is_pinned DESC, timestamp DESC
        LIMIT :limit
    """)
    suspend fun searchItems(query: String, limit: Int = 50): List<ClipboardItemEntity>

    /**
     * Observe all clipboard items as Flow for reactive UI.
     * Updates automatically when items are added/removed.
     */
    @Query("""
        SELECT * FROM clipboard_items
        ORDER BY is_pinned DESC, timestamp DESC
        LIMIT :limit
    """)
    fun observeItems(limit: Int = 50): Flow<List<ClipboardItemEntity>>

    /**
     * Observe only pinned items.
     */
    @Query("SELECT * FROM clipboard_items WHERE is_pinned = 1 ORDER BY timestamp DESC")
    fun observePinnedItems(): Flow<List<ClipboardItemEntity>>

    /**
     * Update the pin status of a clipboard item.
     */
    @Query("UPDATE clipboard_items SET is_pinned = :isPinned WHERE id = :id")
    suspend fun updatePinStatus(id: String, isPinned: Boolean)

    /**
     * Delete a specific clipboard item by ID.
     */
    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    /**
     * Delete multiple items by IDs (batch deletion).
     */
    @Query("DELETE FROM clipboard_items WHERE id IN (:ids)")
    suspend fun deleteItems(ids: List<String>)

    /**
     * Delete the oldest non-pinned items to maintain max capacity.
     * Keeps only the most recent N items (excluding pinned).
     */
    @Query("""
        DELETE FROM clipboard_items
        WHERE id IN (
            SELECT id FROM clipboard_items
            WHERE is_pinned = 0
            ORDER BY timestamp ASC
            LIMIT (SELECT COUNT(*) FROM clipboard_items WHERE is_pinned = 0) - :maxUnpinned
        )
    """)
    suspend fun trimOldestUnpinned(maxUnpinned: Int)

    /**
     * Delete all non-pinned items.
     */
    @Query("DELETE FROM clipboard_items WHERE is_pinned = 0")
    suspend fun deleteAllUnpinned()

    /**
     * Delete all items (including pinned) - nuclear option.
     */
    @Query("DELETE FROM clipboard_items")
    suspend fun clearAll()

    /**
     * Get total count of clipboard items.
     */
    @Query("SELECT COUNT(*) FROM clipboard_items")
    suspend fun getItemCount(): Int

    /**
     * Get count of pinned items.
     */
    @Query("SELECT COUNT(*) FROM clipboard_items WHERE is_pinned = 1")
    suspend fun getPinnedItemCount(): Int

    /**
     * Get count of non-pinned items.
     */
    @Query("SELECT COUNT(*) FROM clipboard_items WHERE is_pinned = 0")
    suspend fun getUnpinnedItemCount(): Int

    /**
     * Check if a specific text already exists in clipboard.
     * Useful to avoid duplicates.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM clipboard_items WHERE text = :text LIMIT 1)")
    suspend fun itemExists(text: String): Boolean
}
