package com.kannada.kavi.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a word typed by the user.
 * Used by the suggestion engine to learn user's typing patterns and provide better suggestions.
 *
 * Indexes:
 * - word: For quick lookup of word frequency
 * - lastUsed: For finding recently used words
 * - frequency: For sorting by most frequently used words
 * - language + frequency: Composite index for language-specific word ranking
 */
@Entity(
    tableName = "user_typed_words",
    indices = [
        Index(value = ["word"], unique = true),
        Index(value = ["last_used"]),
        Index(value = ["frequency"]),
        Index(value = ["language", "frequency"])
    ]
)
data class UserTypedWordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /**
     * The word typed by the user (in Kannada or English).
     */
    @ColumnInfo(name = "word")
    val word: String,

    /**
     * Number of times this word has been typed.
     * Higher frequency = higher priority in suggestions.
     */
    @ColumnInfo(name = "frequency")
    val frequency: Int = 1,

    /**
     * Timestamp (milliseconds) of when this word was last used.
     * Used for relevance scoring and cleanup of old entries.
     */
    @ColumnInfo(name = "last_used")
    val lastUsed: Long = System.currentTimeMillis(),

    /**
     * Language of the word: "kannada", "english", or "mixed".
     * Allows language-specific suggestion filtering.
     */
    @ColumnInfo(name = "language")
    val language: String = "kannada"
)
