package com.kannada.kavi.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kannada.kavi.core.common.ClipboardContentType

/**
 * Entity representing a clipboard item stored in the database.
 * Allows users to access clipboard history even after keyboard restart.
 *
 * Indexes:
 * - timestamp: For chronological ordering (most recent first)
 * - isPinned: For quick filtering of pinned items
 * - isPinned + timestamp: Composite index for showing pinned items first, then by timestamp
 */
@Entity(
    tableName = "clipboard_items",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["is_pinned"]),
        Index(value = ["is_pinned", "timestamp"])
    ]
)
data class ClipboardItemEntity(
    /**
     * Unique identifier for this clipboard item (UUID).
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /**
     * The actual text content of the clipboard item.
     */
    @ColumnInfo(name = "text")
    val text: String,

    /**
     * Timestamp (milliseconds) when this item was copied.
     */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    /**
     * Whether this item is pinned (never auto-deleted).
     * Pinned items persist even when clipboard reaches max capacity.
     */
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    /**
     * Source application package name (if available).
     * Helps users identify where the content came from.
     */
    @ColumnInfo(name = "source_app")
    val sourceApp: String? = null,

    /**
     * Type of content: TEXT, URL, EMAIL, PHONE, or CODE.
     * Used for displaying appropriate icons and formatting.
     */
    @ColumnInfo(name = "content_type")
    val contentType: ClipboardContentType = ClipboardContentType.TEXT
)
