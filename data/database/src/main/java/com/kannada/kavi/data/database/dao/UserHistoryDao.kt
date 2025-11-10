package com.kannada.kavi.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kannada.kavi.data.database.entities.UserTypedWordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user typing history.
 * Provides methods to store, retrieve, and manage words typed by the user.
 */
@Dao
interface UserHistoryDao {

    /**
     * Insert a new word or update existing word on conflict.
     * If word already exists, it will be replaced with the new entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: UserTypedWordEntity)

    /**
     * Insert multiple words at once (batch operation for efficiency).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<UserTypedWordEntity>)

    /**
     * Get a specific word by its text.
     * Returns null if word doesn't exist.
     */
    @Query("SELECT * FROM user_typed_words WHERE word = :word LIMIT 1")
    suspend fun getWord(word: String): UserTypedWordEntity?

    /**
     * Get frequency of a specific word.
     * Returns 0 if word doesn't exist.
     */
    @Query("SELECT frequency FROM user_typed_words WHERE word = :word LIMIT 1")
    suspend fun getWordFrequency(word: String): Int?

    /**
     * Get all words ordered by frequency (most frequent first).
     * Limit parameter controls how many words to return.
     */
    @Query("SELECT * FROM user_typed_words ORDER BY frequency DESC LIMIT :limit")
    suspend fun getTopWords(limit: Int = 100): List<UserTypedWordEntity>

    /**
     * Get all words for a specific language, ordered by frequency.
     */
    @Query("SELECT * FROM user_typed_words WHERE language = :language ORDER BY frequency DESC LIMIT :limit")
    suspend fun getTopWordsByLanguage(language: String, limit: Int = 100): List<UserTypedWordEntity>

    /**
     * Get recently used words (within last N days), ordered by last used timestamp.
     */
    @Query("""
        SELECT * FROM user_typed_words
        WHERE last_used > :sinceTimestamp
        ORDER BY last_used DESC
        LIMIT :limit
    """)
    suspend fun getRecentWords(sinceTimestamp: Long, limit: Int = 50): List<UserTypedWordEntity>

    /**
     * Get all words (for bulk export or analysis).
     */
    @Query("SELECT * FROM user_typed_words ORDER BY frequency DESC")
    suspend fun getAllWords(): List<UserTypedWordEntity>

    /**
     * Observe all words as a Flow for reactive UI updates.
     * Useful for live word cloud or statistics screens.
     */
    @Query("SELECT * FROM user_typed_words ORDER BY frequency DESC LIMIT :limit")
    fun observeTopWords(limit: Int = 100): Flow<List<UserTypedWordEntity>>

    /**
     * Increment frequency of a word by a given amount.
     * Also updates the lastUsed timestamp to current time.
     */
    @Query("""
        UPDATE user_typed_words
        SET frequency = frequency + :increment,
            last_used = :timestamp
        WHERE word = :word
    """)
    suspend fun incrementWordFrequency(word: String, increment: Int = 1, timestamp: Long = System.currentTimeMillis())

    /**
     * Update the last used timestamp for a word.
     */
    @Query("UPDATE user_typed_words SET last_used = :timestamp WHERE word = :word")
    suspend fun updateLastUsed(word: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Delete a specific word.
     */
    @Query("DELETE FROM user_typed_words WHERE word = :word")
    suspend fun deleteWord(word: String)

    /**
     * Delete words with frequency below a threshold (cleanup low-frequency words).
     */
    @Query("DELETE FROM user_typed_words WHERE frequency < :threshold")
    suspend fun deleteWordsWithLowFrequency(threshold: Int)

    /**
     * Delete words not used since a given timestamp (cleanup old words).
     */
    @Query("DELETE FROM user_typed_words WHERE last_used < :timestamp")
    suspend fun deleteOldWords(timestamp: Long)

    /**
     * Clear all words (nuclear option for privacy/reset).
     */
    @Query("DELETE FROM user_typed_words")
    suspend fun clearAll()

    /**
     * Get total count of words in the database.
     */
    @Query("SELECT COUNT(*) FROM user_typed_words")
    suspend fun getWordCount(): Int

    /**
     * Get count of words for a specific language.
     */
    @Query("SELECT COUNT(*) FROM user_typed_words WHERE language = :language")
    suspend fun getWordCountByLanguage(language: String): Int
}
