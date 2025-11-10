package com.kannada.kavi.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kannada.kavi.data.database.dao.UserHistoryDao
import com.kannada.kavi.data.database.entities.UserTypedWordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UserHistoryDaoTest - Comprehensive Tests for UserHistoryDao
 *
 * Tests all CRUD operations and query methods for user typing history.
 *
 * TEST CATEGORIES:
 * ================
 * 1. Insert Operations
 *    - Single insert
 *    - Batch insert
 *    - Insert with conflict (unique constraint)
 *
 * 2. Query Operations
 *    - Get by word
 *    - Get by ID
 *    - Get all words
 *    - Get with limit
 *    - Get with language filter
 *    - Get sorted by frequency
 *    - Get sorted by recency
 *
 * 3. Update Operations
 *    - Increment frequency
 *    - Update last used timestamp
 *    - Update language
 *
 * 4. Delete Operations
 *    - Delete by word
 *    - Delete by ID
 *    - Delete old entries
 *    - Clear all
 *
 * 5. Flow/Reactive Queries
 *    - Observe all words
 *    - Real-time updates
 */
@RunWith(AndroidJUnit4::class)
class UserHistoryDaoTest {

    private lateinit var database: KaviDatabase
    private lateinit var userHistoryDao: UserHistoryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Use in-memory database for testing (data cleared after test)
        database = Room.inMemoryDatabaseBuilder(
            context,
            KaviDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        userHistoryDao = database.userHistoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========================================
    // INSERT TESTS
    // ========================================

    @Test
    fun insertWord_savesWordToDatabase() = runBlocking {
        // Given
        val word = UserTypedWordEntity(
            word = "ಕನ್ನಡ",
            frequency = 5,
            lastUsed = System.currentTimeMillis(),
            language = "kannada"
        )

        // When
        val id = userHistoryDao.insertWord(word)

        // Then
        assertThat(id).isGreaterThan(0)
        val retrieved = userHistoryDao.getWord("ಕನ್ನಡ")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.word).isEqualTo("ಕನ್ನಡ")
        assertThat(retrieved?.frequency).isEqualTo(5)
    }

    @Test
    fun insertMultipleWords_savesAllWords() = runBlocking {
        // Given
        val words = listOf(
            UserTypedWordEntity(word = "ಕನ್ನಡ", frequency = 5, lastUsed = 1000, language = "kannada"),
            UserTypedWordEntity(word = "ನಮಸ್ಕಾರ", frequency = 3, lastUsed = 2000, language = "kannada"),
            UserTypedWordEntity(word = "ಧನ್ಯವಾದ", frequency = 2, lastUsed = 3000, language = "kannada")
        )

        // When
        userHistoryDao.insertWords(words)

        // Then
        val allWords = userHistoryDao.getAllWords()
        assertThat(allWords).hasSize(3)
        assertThat(allWords.map { it.word }).containsExactly("ಕನ್ನಡ", "ನಮಸ್ಕಾರ", "ಧನ್ಯವಾದ")
    }

    @Test
    fun insertDuplicateWord_replacesExisting() = runBlocking {
        // Given - Insert first word
        val word1 = UserTypedWordEntity(word = "ಕನ್ನಡ", frequency = 5, lastUsed = 1000, language = "kannada")
        userHistoryDao.insertWord(word1)

        // When - Insert same word with different frequency
        val word2 = UserTypedWordEntity(word = "ಕನ್ನಡ", frequency = 10, lastUsed = 2000, language = "kannada")
        userHistoryDao.insertWord(word2)

        // Then - Should replace (REPLACE strategy)
        val allWords = userHistoryDao.getAllWords()
        assertThat(allWords).hasSize(1)
        assertThat(allWords[0].frequency).isEqualTo(10)
        assertThat(allWords[0].lastUsed).isEqualTo(2000)
    }

    // ========================================
    // QUERY TESTS
    // ========================================

    @Test
    fun getWord_returnsCorrectWord() = runBlocking {
        // Given
        val words = listOf(
            UserTypedWordEntity(word = "ಕನ್ನಡ", frequency = 5, lastUsed = 1000, language = "kannada"),
            UserTypedWordEntity(word = "ನಮಸ್ಕಾರ", frequency = 3, lastUsed = 2000, language = "kannada")
        )
        userHistoryDao.insertWords(words)

        // When
        val result = userHistoryDao.getWord("ನಮಸ್ಕಾರ")

        // Then
        assertThat(result).isNotNull()
        assertThat(result?.word).isEqualTo("ನಮಸ್ಕಾರ")
        assertThat(result?.frequency).isEqualTo(3)
    }

    @Test
    fun getWord_nonExistent_returnsNull() = runBlocking {
        // When
        val result = userHistoryDao.getWord("nonexistent")

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun getAllWords_returnsAllInDatabase() = runBlocking {
        // Given
        val words = listOf(
            UserTypedWordEntity(word = "word1", frequency = 5, lastUsed = 1000, language = "kannada"),
            UserTypedWordEntity(word = "word2", frequency = 3, lastUsed = 2000, language = "kannada"),
            UserTypedWordEntity(word = "word3", frequency = 7, lastUsed = 3000, language = "kannada")
        )
        userHistoryDao.insertWords(words)

        // When
        val result = userHistoryDao.getAllWords()

        // Then
        assertThat(result).hasSize(3)
    }

    @Test
    fun getAllWordsWithLimit_returnsCorrectCount() = runBlocking {
        // Given - Insert 5 words
        val words = (1..5).map { i ->
            UserTypedWordEntity(
                word = "word$i",
                frequency = i,
                lastUsed = i.toLong() * 1000,
                language = "kannada"
            )
        }
        userHistoryDao.insertWords(words)

        // When - Request only 3
        val result = userHistoryDao.getAllWords(limit = 3)

        // Then
        assertThat(result).hasSize(3)
    }

    @Test
    fun getAllWordsByLanguage_filtersCorrectly() = runBlocking {
        // Given - Mix of languages
        val words = listOf(
            UserTypedWordEntity(word = "ಕನ್ನಡ", frequency = 5, lastUsed = 1000, language = "kannada"),
            UserTypedWordEntity(word = "hello", frequency = 3, lastUsed = 2000, language = "english"),
            UserTypedWordEntity(word = "ನಮಸ್ಕಾರ", frequency = 2, lastUsed = 3000, language = "kannada"),
            UserTypedWordEntity(word = "world", frequency = 4, lastUsed = 4000, language = "english")
        )
        userHistoryDao.insertWords(words)

        // When
        val kannadaWords = userHistoryDao.getAllWordsByLanguage("kannada")
        val englishWords = userHistoryDao.getAllWordsByLanguage("english")

        // Then
        assertThat(kannadaWords).hasSize(2)
        assertThat(englishWords).hasSize(2)
        assertThat(kannadaWords.map { it.word }).containsExactly("ಕನ್ನಡ", "ನಮಸ್ಕಾರ")
        assertThat(englishWords.map { it.word }).containsExactly("hello", "world")
    }

    @Test
    fun getWordsSortedByFrequency_returnsInCorrectOrder() = runBlocking {
        // Given - Words with different frequencies
        val words = listOf(
            UserTypedWordEntity(word = "word1", frequency = 3, lastUsed = 1000, language = "kannada"),
            UserTypedWordEntity(word = "word2", frequency = 10, lastUsed = 2000, language = "kannada"),
            UserTypedWordEntity(word = "word3", frequency = 5, lastUsed = 3000, language = "kannada")
        )
        userHistoryDao.insertWords(words)

        // When
        val result = userHistoryDao.getWordsSortedByFrequency()

        // Then - Should be sorted: 10, 5, 3
        assertThat(result).hasSize(3)
        assertThat(result[0].word).isEqualTo("word2") // frequency 10
        assertThat(result[1].word).isEqualTo("word3") // frequency 5
        assertThat(result[2].word).isEqualTo("word1") // frequency 3
    }

    @Test
    fun getWordsSortedByRecency_returnsInCorrectOrder() = runBlocking {
        // Given - Words with different timestamps
        val words = listOf(
            UserTypedWordEntity(word = "word1", frequency = 1, lastUsed = 1000, language = "kannada"),
            UserTypedWordEntity(word = "word2", frequency = 1, lastUsed = 3000, language = "kannada"),
            UserTypedWordEntity(word = "word3", frequency = 1, lastUsed = 2000, language = "kannada")
        )
        userHistoryDao.insertWords(words)

        // When
        val result = userHistoryDao.getWordsSortedByRecency()

        // Then - Should be sorted: 3000, 2000, 1000 (most recent first)
        assertThat(result).hasSize(3)
        assertThat(result[0].word).isEqualTo("word2") // lastUsed 3000
        assertThat(result[1].word).isEqualTo("word3") // lastUsed 2000
        assertThat(result[2].word).isEqualTo("word1") // lastUsed 1000
    }

    // ========================================
    // UPDATE TESTS
    // ========================================

    @Test
    fun incrementWordFrequency_increasesCount() = runBlocking {
        // Given
        val word = UserTypedWordEntity(word = "ಕನ್ನಡ", frequency = 5, lastUsed = 1000, language = "kannada")
        userHistoryDao.insertWord(word)

        // When
        userHistoryDao.incrementWordFrequency("ಕನ್ನಡ", increment = 3, newLastUsed = 2000)

        // Then
        val updated = userHistoryDao.getWord("ಕನ್ನಡ")
        assertThat(updated?.frequency).isEqualTo(8) // 5 + 3
        assertThat(updated?.lastUsed).isEqualTo(2000)
    }

    @Test
    fun updateWord_changesAllFields() = runBlocking {
        // Given
        val word = UserTypedWordEntity(
            id = 0,
            word = "ಕನ್ನಡ",
            frequency = 5,
            lastUsed = 1000,
            language = "kannada"
        )
        val id = userHistoryDao.insertWord(word)

        // When
        val updated = UserTypedWordEntity(
            id = id,
            word = "ಕನ್ನಡ",
            frequency = 10,
            lastUsed = 2000,
            language = "kannada"
        )
        userHistoryDao.updateWord(updated)

        // Then
        val result = userHistoryDao.getWord("ಕನ್ನಡ")
        assertThat(result?.frequency).isEqualTo(10)
        assertThat(result?.lastUsed).isEqualTo(2000)
    }

    // ========================================
    // DELETE TESTS
    // ========================================

    @Test
    fun deleteWord_removesFromDatabase() = runBlocking {
        // Given
        val words = listOf(
            UserTypedWordEntity(word = "word1", frequency = 5, lastUsed = 1000, language = "kannada"),
            UserTypedWordEntity(word = "word2", frequency = 3, lastUsed = 2000, language = "kannada")
        )
        userHistoryDao.insertWords(words)

        // When
        userHistoryDao.deleteWord("word1")

        // Then
        assertThat(userHistoryDao.getWord("word1")).isNull()
        assertThat(userHistoryDao.getWord("word2")).isNotNull()
        assertThat(userHistoryDao.getAllWords()).hasSize(1)
    }

    @Test
    fun deleteWordById_removesCorrectWord() = runBlocking {
        // Given
        val word = UserTypedWordEntity(word = "ಕನ್ನಡ", frequency = 5, lastUsed = 1000, language = "kannada")
        val id = userHistoryDao.insertWord(word)

        // When
        userHistoryDao.deleteWordById(id)

        // Then
        assertThat(userHistoryDao.getWordById(id)).isNull()
    }

    @Test
    fun deleteOldEntries_removesOnlyOldWords() = runBlocking {
        // Given
        val currentTime = System.currentTimeMillis()
        val oldTime = currentTime - (31L * 24 * 60 * 60 * 1000) // 31 days ago
        val recentTime = currentTime - (5L * 24 * 60 * 60 * 1000) // 5 days ago

        val words = listOf(
            UserTypedWordEntity(word = "old1", frequency = 1, lastUsed = oldTime, language = "kannada"),
            UserTypedWordEntity(word = "old2", frequency = 1, lastUsed = oldTime - 1000, language = "kannada"),
            UserTypedWordEntity(word = "recent", frequency = 1, lastUsed = recentTime, language = "kannada"),
            UserTypedWordEntity(word = "new", frequency = 1, lastUsed = currentTime, language = "kannada")
        )
        userHistoryDao.insertWords(words)

        // When - Delete entries older than 30 days
        val thirtyDaysAgo = currentTime - (30L * 24 * 60 * 60 * 1000)
        userHistoryDao.deleteOldEntries(thirtyDaysAgo)

        // Then - Only old entries should be deleted
        val remaining = userHistoryDao.getAllWords()
        assertThat(remaining).hasSize(2)
        assertThat(remaining.map { it.word }).containsExactly("recent", "new")
    }

    @Test
    fun clearAll_removesAllWords() = runBlocking {
        // Given
        val words = (1..10).map { i ->
            UserTypedWordEntity(
                word = "word$i",
                frequency = i,
                lastUsed = i.toLong() * 1000,
                language = "kannada"
            )
        }
        userHistoryDao.insertWords(words)
        assertThat(userHistoryDao.getAllWords()).hasSize(10)

        // When
        userHistoryDao.clearAll()

        // Then
        assertThat(userHistoryDao.getAllWords()).isEmpty()
    }

    // ========================================
    // FLOW/REACTIVE TESTS
    // ========================================

    @Test
    fun observeAllWords_emitsUpdates() = runBlocking {
        // Given - Empty database
        var emittedWords = userHistoryDao.observeAllWords().first()
        assertThat(emittedWords).isEmpty()

        // When - Insert word
        val word = UserTypedWordEntity(word = "ಕನ್ನಡ", frequency = 5, lastUsed = 1000, language = "kannada")
        userHistoryDao.insertWord(word)

        // Then - Flow should emit new data
        emittedWords = userHistoryDao.observeAllWords().first()
        assertThat(emittedWords).hasSize(1)
        assertThat(emittedWords[0].word).isEqualTo("ಕನ್ನಡ")
    }

    @Test
    fun observeWordCount_emitsCorrectCount() = runBlocking {
        // Given - Empty database
        var count = userHistoryDao.observeWordCount().first()
        assertThat(count).isEqualTo(0)

        // When - Insert 3 words
        val words = listOf(
            UserTypedWordEntity(word = "word1", frequency = 1, lastUsed = 1000, language = "kannada"),
            UserTypedWordEntity(word = "word2", frequency = 1, lastUsed = 2000, language = "kannada"),
            UserTypedWordEntity(word = "word3", frequency = 1, lastUsed = 3000, language = "kannada")
        )
        userHistoryDao.insertWords(words)

        // Then
        count = userHistoryDao.observeWordCount().first()
        assertThat(count).isEqualTo(3)
    }

    // ========================================
    // COMPLEX QUERY TESTS
    // ========================================

    @Test
    fun getTopWordsByLanguageAndFrequency_returnsCorrectResults() = runBlocking {
        // Given - Mix of languages and frequencies
        val words = listOf(
            UserTypedWordEntity(word = "kan1", frequency = 10, lastUsed = 1000, language = "kannada"),
            UserTypedWordEntity(word = "kan2", frequency = 5, lastUsed = 2000, language = "kannada"),
            UserTypedWordEntity(word = "eng1", frequency = 8, lastUsed = 3000, language = "english"),
            UserTypedWordEntity(word = "kan3", frequency = 15, lastUsed = 4000, language = "kannada"),
            UserTypedWordEntity(word = "eng2", frequency = 3, lastUsed = 5000, language = "english")
        )
        userHistoryDao.insertWords(words)

        // When - Get top 2 Kannada words by frequency
        val topKannada = userHistoryDao.getAllWordsByLanguage("kannada", limit = 2)

        // Then - Should return kan3 (15) and kan1 (10)
        assertThat(topKannada).hasSize(2)
        assertThat(topKannada[0].word).isEqualTo("kan3")
        assertThat(topKannada[1].word).isEqualTo("kan1")
    }

    @Test
    fun getWordFrequency_returnsCorrectValue() = runBlocking {
        // Given
        val word = UserTypedWordEntity(word = "ಕನ್ನಡ", frequency = 42, lastUsed = 1000, language = "kannada")
        userHistoryDao.insertWord(word)

        // When
        val frequency = userHistoryDao.getWordFrequency("ಕನ್ನಡ")

        // Then
        assertThat(frequency).isEqualTo(42)
    }

    @Test
    fun getWordFrequency_nonExistent_returnsZero() = runBlocking {
        // When
        val frequency = userHistoryDao.getWordFrequency("nonexistent")

        // Then
        assertThat(frequency).isEqualTo(0)
    }
}
