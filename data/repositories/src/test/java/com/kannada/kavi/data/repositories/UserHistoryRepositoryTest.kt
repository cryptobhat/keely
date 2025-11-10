package com.kannada.kavi.data.repositories

import com.google.common.truth.Truth.assertThat
import com.kannada.kavi.data.database.dao.UserHistoryDao
import com.kannada.kavi.data.database.entities.UserTypedWordEntity
import com.kannada.kavi.data.repositories.models.UserTypedWord
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * UserHistoryRepositoryTest - Unit Tests for UserHistoryRepository
 *
 * Tests repository logic with mocked DAO.
 *
 * TEST CATEGORIES:
 * ================
 * 1. Record Operations
 *    - Record new word
 *    - Record existing word (increment frequency)
 *    - Batch record
 *
 * 2. Query Operations
 *    - Get all words
 *    - Get by language
 *    - Get sorted by frequency
 *    - Get sorted by recency
 *
 * 3. Delete Operations
 *    - Delete word
 *    - Delete old entries
 *    - Clear all
 *
 * 4. Domain Model Conversion
 *    - Entity to domain model
 *    - Relevance score calculation
 *
 * 5. Flow Operations
 *    - Observe words
 *    - Observe count
 */
class UserHistoryRepositoryTest {

    private lateinit var userHistoryDao: UserHistoryDao
    private lateinit var repository: UserHistoryRepository

    @Before
    fun setup() {
        userHistoryDao = mockk()
        repository = UserHistoryRepository(userHistoryDao)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ========================================
    // RECORD OPERATIONS
    // ========================================

    @Test
    fun `recordWord with new word inserts entity`() = runBlocking {
        // Given
        coEvery { userHistoryDao.getWord("ಕನ್ನಡ") } returns null
        coEvery { userHistoryDao.insertWord(any()) } returns 1L

        // When
        repository.recordWord("ಕನ್ನಡ", "kannada")

        // Then
        coVerify {
            userHistoryDao.insertWord(
                match {
                    it.word == "ಕನ್ನಡ" &&
                    it.frequency == 1 &&
                    it.language == "kannada"
                }
            )
        }
        coVerify(exactly = 0) { userHistoryDao.incrementWordFrequency(any(), any(), any()) }
    }

    @Test
    fun `recordWord with existing word increments frequency`() = runBlocking {
        // Given
        val existingWord = UserTypedWordEntity(
            id = 1,
            word = "ಕನ್ನಡ",
            frequency = 5,
            lastUsed = 1000,
            language = "kannada"
        )
        coEvery { userHistoryDao.getWord("ಕನ್ನಡ") } returns existingWord
        coEvery { userHistoryDao.incrementWordFrequency(any(), any(), any()) } just Runs

        // When
        repository.recordWord("ಕನ್ನಡ", "kannada")

        // Then
        coVerify {
            userHistoryDao.incrementWordFrequency(
                word = "ಕನ್ನಡ",
                increment = 1,
                newLastUsed = any()
            )
        }
        coVerify(exactly = 0) { userHistoryDao.insertWord(any()) }
    }

    @Test
    fun `recordWords batch inserts multiple words`() = runBlocking {
        // Given
        val words = listOf("ಕನ್ನಡ", "ನಮಸ್ಕಾರ", "ಧನ್ಯವಾದ")
        coEvery { userHistoryDao.getWord(any()) } returns null
        coEvery { userHistoryDao.insertWord(any()) } returns 1L

        // When
        repository.recordWords(words, "kannada")

        // Then
        coVerify(exactly = 3) { userHistoryDao.insertWord(any()) }
    }

    @Test
    fun `recordWords with mix of new and existing words`() = runBlocking {
        // Given
        val existingWord = UserTypedWordEntity(
            id = 1,
            word = "ಕನ್ನಡ",
            frequency = 5,
            lastUsed = 1000,
            language = "kannada"
        )
        coEvery { userHistoryDao.getWord("ಕನ್ನಡ") } returns existingWord
        coEvery { userHistoryDao.getWord("ನಮಸ್ಕಾರ") } returns null
        coEvery { userHistoryDao.insertWord(any()) } returns 1L
        coEvery { userHistoryDao.incrementWordFrequency(any(), any(), any()) } just Runs

        // When
        repository.recordWords(listOf("ಕನ್ನಡ", "ನಮಸ್ಕಾರ"), "kannada")

        // Then - One insert, one increment
        coVerify(exactly = 1) { userHistoryDao.insertWord(any()) }
        coVerify(exactly = 1) { userHistoryDao.incrementWordFrequency(any(), any(), any()) }
    }

    // ========================================
    // QUERY OPERATIONS
    // ========================================

    @Test
    fun `getAllWords returns domain models`() = runBlocking {
        // Given
        val entities = listOf(
            UserTypedWordEntity(id = 1, word = "ಕನ್ನಡ", frequency = 10, lastUsed = 2000, language = "kannada"),
            UserTypedWordEntity(id = 2, word = "ನಮಸ್ಕಾರ", frequency = 5, lastUsed = 1000, language = "kannada")
        )
        coEvery { userHistoryDao.getAllWords(any()) } returns entities

        // When
        val result = repository.getAllWords(limit = 100)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].word).isEqualTo("ಕನ್ನಡ")
        assertThat(result[0].frequency).isEqualTo(10)
        assertThat(result[1].word).isEqualTo("ನಮಸ್ಕಾರ")
        assertThat(result[1].frequency).isEqualTo(5)
    }

    @Test
    fun `getWordsByLanguage filters correctly`() = runBlocking {
        // Given
        val kannadaEntities = listOf(
            UserTypedWordEntity(id = 1, word = "ಕನ್ನಡ", frequency = 10, lastUsed = 1000, language = "kannada")
        )
        coEvery { userHistoryDao.getAllWordsByLanguage("kannada", any()) } returns kannadaEntities

        // When
        val result = repository.getWordsByLanguage("kannada", limit = 100)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].language).isEqualTo("kannada")
        coVerify { userHistoryDao.getAllWordsByLanguage("kannada", 100) }
    }

    @Test
    fun `getWordsSortedByFrequency returns correct order`() = runBlocking {
        // Given
        val entities = listOf(
            UserTypedWordEntity(id = 1, word = "most", frequency = 100, lastUsed = 1000, language = "kannada"),
            UserTypedWordEntity(id = 2, word = "middle", frequency = 50, lastUsed = 1000, language = "kannada"),
            UserTypedWordEntity(id = 3, word = "least", frequency = 10, lastUsed = 1000, language = "kannada")
        )
        coEvery { userHistoryDao.getWordsSortedByFrequency(any()) } returns entities

        // When
        val result = repository.getWordsSortedByFrequency(limit = 100)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result[0].word).isEqualTo("most")
        assertThat(result[1].word).isEqualTo("middle")
        assertThat(result[2].word).isEqualTo("least")
    }

    @Test
    fun `getWordsSortedByRecency returns correct order`() = runBlocking {
        // Given
        val entities = listOf(
            UserTypedWordEntity(id = 1, word = "newest", frequency = 1, lastUsed = 3000, language = "kannada"),
            UserTypedWordEntity(id = 2, word = "middle", frequency = 1, lastUsed = 2000, language = "kannada"),
            UserTypedWordEntity(id = 3, word = "oldest", frequency = 1, lastUsed = 1000, language = "kannada")
        )
        coEvery { userHistoryDao.getWordsSortedByRecency(any()) } returns entities

        // When
        val result = repository.getWordsSortedByRecency(limit = 100)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result[0].word).isEqualTo("newest")
        assertThat(result[1].word).isEqualTo("middle")
        assertThat(result[2].word).isEqualTo("oldest")
    }

    @Test
    fun `getWordFrequency returns correct value`() = runBlocking {
        // Given
        coEvery { userHistoryDao.getWordFrequency("ಕನ್ನಡ") } returns 42

        // When
        val frequency = repository.getWordFrequency("ಕನ್ನಡ")

        // Then
        assertThat(frequency).isEqualTo(42)
    }

    @Test
    fun `getWordFrequency for nonexistent word returns zero`() = runBlocking {
        // Given
        coEvery { userHistoryDao.getWordFrequency("nonexistent") } returns 0

        // When
        val frequency = repository.getWordFrequency("nonexistent")

        // Then
        assertThat(frequency).isEqualTo(0)
    }

    // ========================================
    // DELETE OPERATIONS
    // ========================================

    @Test
    fun `deleteWord calls dao`() = runBlocking {
        // Given
        coEvery { userHistoryDao.deleteWord("ಕನ್ನಡ") } just Runs

        // When
        repository.deleteWord("ಕನ್ನಡ")

        // Then
        coVerify { userHistoryDao.deleteWord("ಕನ್ನಡ") }
    }

    @Test
    fun `deleteOldEntries calculates correct cutoff`() = runBlocking {
        // Given
        val daysToKeep = 30
        coEvery { userHistoryDao.deleteOldEntries(any()) } just Runs

        // When
        repository.deleteOldEntries(daysToKeep)

        // Then
        coVerify {
            userHistoryDao.deleteOldEntries(
                match { cutoffTime ->
                    // Verify cutoff is approximately 30 days ago (within 1 second tolerance)
                    val expectedCutoff = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
                    Math.abs(cutoffTime - expectedCutoff) < 1000
                }
            )
        }
    }

    @Test
    fun `clearAll calls dao`() = runBlocking {
        // Given
        coEvery { userHistoryDao.clearAll() } just Runs

        // When
        repository.clearAll()

        // Then
        coVerify { userHistoryDao.clearAll() }
    }

    // ========================================
    // COUNT OPERATIONS
    // ========================================

    @Test
    fun `getWordCount returns correct count`() = runBlocking {
        // Given
        coEvery { userHistoryDao.getWordCount() } returns 100

        // When
        val count = repository.getWordCount()

        // Then
        assertThat(count).isEqualTo(100)
    }

    // ========================================
    // FLOW OPERATIONS
    // ========================================

    @Test
    fun `observeAllWords emits domain models`() = runBlocking {
        // Given
        val entities = listOf(
            UserTypedWordEntity(id = 1, word = "ಕನ್ನಡ", frequency = 10, lastUsed = 1000, language = "kannada")
        )
        coEvery { userHistoryDao.observeAllWords() } returns flowOf(entities)

        // When
        var emittedWords: List<UserTypedWord>? = null
        repository.observeAllWords().collect { words ->
            emittedWords = words
        }

        // Then
        assertThat(emittedWords).isNotNull()
        assertThat(emittedWords).hasSize(1)
        assertThat(emittedWords?.get(0)?.word).isEqualTo("ಕನ್ನಡ")
    }

    @Test
    fun `observeWordCount emits count`() = runBlocking {
        // Given
        coEvery { userHistoryDao.observeWordCount() } returns flowOf(42)

        // When
        var emittedCount: Int? = null
        repository.observeWordCount().collect { count ->
            emittedCount = count
        }

        // Then
        assertThat(emittedCount).isEqualTo(42)
    }

    // ========================================
    // DOMAIN MODEL CONVERSION
    // ========================================

    @Test
    fun `entity to domain model conversion works correctly`() = runBlocking {
        // Given
        val currentTime = System.currentTimeMillis()
        val entity = UserTypedWordEntity(
            id = 1,
            word = "ಕನ್ನಡ",
            frequency = 10,
            lastUsed = currentTime - 1000,
            language = "kannada"
        )
        coEvery { userHistoryDao.getAllWords(any()) } returns listOf(entity)

        // When
        val result = repository.getAllWords(limit = 1)

        // Then
        val domainModel = result[0]
        assertThat(domainModel.word).isEqualTo("ಕನ್ನಡ")
        assertThat(domainModel.frequency).isEqualTo(10)
        assertThat(domainModel.lastUsed).isEqualTo(currentTime - 1000)
        assertThat(domainModel.language).isEqualTo("kannada")
    }

    @Test
    fun `relevance score calculation in domain model`() {
        // Given
        val currentTime = System.currentTimeMillis()
        val recentTime = currentTime - (1 * 24 * 60 * 60 * 1000) // 1 day ago
        val oldTime = currentTime - (30 * 24 * 60 * 60 * 1000) // 30 days ago

        val recentWord = UserTypedWord(
            word = "recent",
            frequency = 10,
            lastUsed = recentTime,
            language = "kannada"
        )
        val oldWord = UserTypedWord(
            word = "old",
            frequency = 10,
            lastUsed = oldTime,
            language = "kannada"
        )

        // When
        val recentScore = recentWord.relevanceScore
        val oldScore = oldWord.relevanceScore

        // Then - Recent word should have higher score
        assertThat(recentScore).isGreaterThan(oldScore)
    }

    @Test
    fun `frequency affects relevance score`() {
        // Given
        val currentTime = System.currentTimeMillis()
        val highFreqWord = UserTypedWord(
            word = "frequent",
            frequency = 100,
            lastUsed = currentTime,
            language = "kannada"
        )
        val lowFreqWord = UserTypedWord(
            word = "rare",
            frequency = 1,
            lastUsed = currentTime,
            language = "kannada"
        )

        // When
        val highScore = highFreqWord.relevanceScore
        val lowScore = lowFreqWord.relevanceScore

        // Then - Higher frequency should have higher score
        assertThat(highScore).isGreaterThan(lowScore)
    }

    @Test
    fun `recency calculation works correctly`() {
        // Given
        val currentTime = System.currentTimeMillis()
        val oneDayAgo = currentTime - (1 * 24 * 60 * 60 * 1000)

        val word = UserTypedWord(
            word = "test",
            frequency = 1,
            lastUsed = oneDayAgo,
            language = "kannada"
        )

        // When
        val daysSinceUsed = word.daysSinceLastUsed

        // Then - Should be approximately 1 day (within tolerance)
        assertThat(daysSinceUsed).isAtLeast(0)
        assertThat(daysSinceUsed).isAtMost(2) // Allow some tolerance
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    fun `recordWord with empty string throws exception`() = runBlocking {
        // When/Then
        try {
            repository.recordWord("", "kannada")
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Word cannot be empty")
        }
    }

    @Test
    fun `getAllWords with zero limit returns empty list`() = runBlocking {
        // Given
        coEvery { userHistoryDao.getAllWords(0) } returns emptyList()

        // When
        val result = repository.getAllWords(limit = 0)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `empty database returns empty list`() = runBlocking {
        // Given
        coEvery { userHistoryDao.getAllWords(any()) } returns emptyList()

        // When
        val result = repository.getAllWords()

        // Then
        assertThat(result).isEmpty()
    }
}
