package com.kannada.kavi.data.repositories.models

/**
 * Domain model representing a word typed by the user.
 * This is the model used by the SuggestionEngine and other features.
 *
 * Separate from the database entity to allow the database schema to evolve
 * independently from the business logic layer.
 */
data class UserTypedWord(
    /**
     * The word typed by the user (in Kannada or English).
     */
    val word: String,

    /**
     * Number of times this word has been typed.
     * Higher frequency = higher priority in suggestions.
     */
    val frequency: Int,

    /**
     * Timestamp (milliseconds) of when this word was last used.
     */
    val lastUsed: Long,

    /**
     * Language of the word: "kannada", "english", or "mixed".
     */
    val language: String
) {
    /**
     * Calculate relevance score based on frequency and recency.
     * More recent words get a boost in the score.
     *
     * @return Relevance score (higher = more relevant)
     */
    fun getRelevanceScore(): Double {
        val daysSinceLastUse = (System.currentTimeMillis() - lastUsed) / (1000.0 * 60 * 60 * 24)
        val recencyFactor = kotlin.math.exp(-daysSinceLastUse / 30.0) // Decay over 30 days
        return frequency * recencyFactor
    }

    /**
     * Check if this word was used recently (within last N days).
     */
    fun wasUsedWithinDays(days: Int): Boolean {
        val millisInDay = 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - lastUsed) < (days * millisInDay)
    }
}
