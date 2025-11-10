package com.kannada.kavi.data.repositories

import com.kannada.kavi.data.database.dao.UserHistoryDao
import com.kannada.kavi.data.database.entities.UserTypedWordEntity
import com.kannada.kavi.data.repositories.models.UserTypedWord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user typing history.
 *
 * This repository provides a clean API for the SuggestionEngine to:
 * - Record words typed by the user
 * - Retrieve word frequencies for ranking suggestions
 * - Observe typing patterns for adaptive learning
 * - Clear history for privacy
 *
 * The repository handles conversion between database entities and domain models,
 * and provides coroutine-based async operations.
 */
@Singleton
class UserHistoryRepository @Inject constructor(
    private val userHistoryDao: UserHistoryDao
) {

    /**
     * Record a word typed by the user.
     * If the word already exists, increments its frequency.
     * If it's a new word, inserts it with frequency = 1.
     *
     * @param word The word to record (Kannada or English)
     * @param language Language identifier: "kannada", "english", or "mixed"
     */
    suspend fun recordWord(word: String, language: String = "kannada") {
        val existingWord = userHistoryDao.getWord(word)

        if (existingWord != null) {
            // Word exists - increment frequency and update timestamp
            userHistoryDao.incrementWordFrequency(
                word = word,
                increment = 1,
                timestamp = System.currentTimeMillis()
            )
        } else {
            // New word - insert with frequency = 1
            userHistoryDao.insertWord(
                UserTypedWordEntity(
                    word = word,
                    frequency = 1,
                    lastUsed = System.currentTimeMillis(),
                    language = language
                )
            )
        }
    }

    /**
     * Record multiple words at once (batch operation for efficiency).
     * Useful when importing user history or processing a complete sentence.
     */
    suspend fun recordWords(words: List<Pair<String, String>>) {
        words.forEach { (word, language) ->
            recordWord(word, language)
        }
    }

    /**
     * Get the frequency of a specific word.
     * Returns 0 if the word has never been typed.
     *
     * @param word The word to look up
     * @return Frequency count (0 if not found)
     */
    suspend fun getWordFrequency(word: String): Int {
        return userHistoryDao.getWordFrequency(word) ?: 0
    }

    /**
     * Get all words ordered by frequency (most frequent first).
     * Returns domain models instead of database entities.
     *
     * @param limit Maximum number of words to return
     * @return List of user typed words with frequencies
     */
    suspend fun getAllWords(limit: Int = 1000): List<UserTypedWord> {
        return userHistoryDao.getTopWords(limit).map { it.toDomainModel() }
    }

    /**
     * Get top words for a specific language.
     * Useful for language-specific suggestion ranking.
     *
     * @param language Language identifier: "kannada", "english", or "mixed"
     * @param limit Maximum number of words to return
     */
    suspend fun getTopWordsByLanguage(language: String, limit: Int = 100): List<UserTypedWord> {
        return userHistoryDao.getTopWordsByLanguage(language, limit).map { it.toDomainModel() }
    }

    /**
     * Get recently used words (within the last N days).
     * Useful for temporal suggestion ranking.
     *
     * @param daysAgo How many days back to search
     * @param limit Maximum number of words to return
     */
    suspend fun getRecentWords(daysAgo: Int = 7, limit: Int = 50): List<UserTypedWord> {
        val sinceTimestamp = System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000L)
        return userHistoryDao.getRecentWords(sinceTimestamp, limit).map { it.toDomainModel() }
    }

    /**
     * Observe top words as a Flow for reactive UI updates.
     * Automatically updates when new words are typed.
     *
     * @param limit Maximum number of words to observe
     */
    fun observeTopWords(limit: Int = 100): Flow<List<UserTypedWord>> {
        return userHistoryDao.observeTopWords(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Increment the frequency of a specific word.
     * Useful for adjusting word rankings based on user selection.
     *
     * @param word The word to boost
     * @param increment How much to increase frequency (default: 1)
     */
    suspend fun incrementWordFrequency(word: String, increment: Int = 1) {
        userHistoryDao.incrementWordFrequency(word, increment, System.currentTimeMillis())
    }

    /**
     * Delete a specific word from history.
     * Useful for removing accidentally typed words or profanity.
     *
     * @param word The word to delete
     */
    suspend fun deleteWord(word: String) {
        userHistoryDao.deleteWord(word)
    }

    /**
     * Clear all user typing history.
     * Nuclear option for privacy/reset.
     */
    suspend fun clearAll() {
        userHistoryDao.clearAll()
    }

    /**
     * Clean up old or low-frequency words to save space.
     * Removes words with frequency < threshold or not used in the last N days.
     *
     * @param frequencyThreshold Minimum frequency to keep (default: 2)
     * @param daysOld Delete words not used in this many days (default: 90)
     */
    suspend fun cleanupOldWords(frequencyThreshold: Int = 2, daysOld: Int = 90) {
        userHistoryDao.deleteWordsWithLowFrequency(frequencyThreshold)

        val oldTimestamp = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        userHistoryDao.deleteOldWords(oldTimestamp)
    }

    /**
     * Get statistics about user typing history.
     *
     * @return Pair of (totalWords, kannadaWords)
     */
    suspend fun getStatistics(): Pair<Int, Int> {
        val totalWords = userHistoryDao.getWordCount()
        val kannadaWords = userHistoryDao.getWordCountByLanguage("kannada")
        return Pair(totalWords, kannadaWords)
    }

    /**
     * Check if a word exists in user history.
     *
     * @param word The word to check
     * @return True if the word has been typed before
     */
    suspend fun hasWord(word: String): Boolean {
        return userHistoryDao.getWord(word) != null
    }

    /**
     * Import user history from a list of words (for data restoration).
     * Useful for backup/restore functionality.
     *
     * @param words List of UserTypedWord to import
     */
    suspend fun importHistory(words: List<UserTypedWord>) {
        val entities = words.map { it.toEntity() }
        userHistoryDao.insertWords(entities)
    }
}

/**
 * Extension function to convert database entity to domain model.
 */
private fun UserTypedWordEntity.toDomainModel() = UserTypedWord(
    word = word,
    frequency = frequency,
    lastUsed = lastUsed,
    language = language
)

/**
 * Extension function to convert domain model to database entity.
 */
private fun UserTypedWord.toEntity() = UserTypedWordEntity(
    word = word,
    frequency = frequency,
    lastUsed = lastUsed,
    language = language
)
