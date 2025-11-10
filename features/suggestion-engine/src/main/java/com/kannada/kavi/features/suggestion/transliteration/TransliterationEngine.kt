package com.kannada.kavi.features.suggestion.transliteration

import android.content.Context
import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.core.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * TransliterationEngine - high fidelity wrapper around [FastPhoneticEngine].
 *
 * The original JSON-driven state machine was brittle and the rules file was
 * corrupted in the current workspace. We now rely on the battle-tested
 * `FastPhoneticEngine` implementation that ships with the upstream Kavi project.
 * This gives us Gboard-class accuracy while keeping the public API identical
 * for the rest of the app.
 */
class TransliterationEngine(@Suppress("UNUSED_PARAMETER") context: Context) {

    private val fastEngine = FastPhoneticEngine.getInstance()

    private val transliterationCache =
        object : LinkedHashMap<String, String>(Constants.Transliteration.MAX_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
                return size > Constants.Transliteration.MAX_CACHE_SIZE
            }
        }

    @Volatile
    private var isInitialized = false

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.Default) {
        isInitialized = true
        Result.Success(Unit)
    }

    fun transliterate(englishText: String): String {
        ensureInitialized()
        val normalized = normalizeInput(englishText)
        if (normalized.isEmpty()) return ""

        val cacheKey = cacheKeyFor(normalized)
        synchronized(transliterationCache) {
            transliterationCache[cacheKey]?.let { return it }
        }

        val kannada = fastEngine.transliterate(normalized)

        if (kannada.isNotEmpty()) {
            cacheResult(cacheKey, kannada)
        }

        return kannada
    }

    fun isKannada(text: String): Boolean = text.any { it in '\u0C80'..'\u0CFF' }

    fun containsEnglish(text: String): Boolean = text.any { it in 'a'..'z' || it in 'A'..'Z' }

    fun isLikelyTransliterable(text: String): Boolean {
        val normalized = normalizeInput(text)
        if (normalized.isEmpty()) return false
        return normalized.all { ch ->
            ch.isLetter() || ch == ' ' || ch == '\'' || ch == '-'
        }
    }

    fun resetCache() {
        synchronized(transliterationCache) { transliterationCache.clear() }
    }

    fun learnPattern(roman: String, correction: String) {
        fastEngine.learnPattern(roman, correction)
        cacheResult(cacheKeyFor(roman), correction)
    }

    fun clearLearnedPatterns() {
        fastEngine.clearUserLearning()
        resetCache()
    }

    fun getCacheStats(): CacheStats = CacheStats(
        size = transliterationCache.size,
        maxSize = Constants.Transliteration.MAX_CACHE_SIZE,
        hitRate = 0f
    )

    private fun cacheResult(key: String, value: String) {
        if (!Constants.Transliteration.ENABLE_CACHING || key.isEmpty()) return
        synchronized(transliterationCache) { transliterationCache[key] = value }
    }

    private fun cacheKeyFor(input: String): String {
        return if (Constants.Transliteration.CASE_SENSITIVE) {
            input
        } else {
            input.lowercase(Locale.ROOT)
        }
    }

    private fun normalizeInput(input: String): String {
        return input.trim()
            .replace("\\s+".toRegex(), " ")
            .take(Constants.Converter.MAX_INPUT_LENGTH)
    }

    private fun ensureInitialized() {
        check(isInitialized) { "TransliterationEngine not initialized" }
    }
}

data class CacheStats(
    val size: Int,
    val maxSize: Int,
    val hitRate: Float
)
