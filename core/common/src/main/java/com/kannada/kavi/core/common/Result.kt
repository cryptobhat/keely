package com.kannada.kavi.core.common

/**
 * Result Class - A Simple Way to Handle Success and Failure
 *
 * Imagine you're asking your friend to bring you a book:
 * - If they bring it successfully = Success (you get the book!)
 * - If something goes wrong = Error (maybe they couldn't find it)
 *
 * This class helps us handle both scenarios gracefully in our code.
 *
 * Example usage:
 * ```
 * val result = getSuggestions("kan")
 * when (result) {
 *     is Result.Success -> showSuggestions(result.data)
 *     is Result.Error -> showError(result.exception.message)
 * }
 * ```
 */
sealed class Result<out T> {
    /**
     * Success - We got the data we wanted!
     * @param data The actual data (like a list of suggestions, user settings, etc.)
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Error - Something went wrong
     * @param exception Contains information about what went wrong
     */
    data class Error(val exception: Exception) : Result<Nothing>()

    /**
     * Check if the result is successful
     * Returns true if we got the data, false if there was an error
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Check if the result is an error
     * Returns true if something went wrong, false if we got the data
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Get the data if successful, or null if there was an error
     * It's like saying "give me the book if you have it, otherwise give me nothing"
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    /**
     * Get the data if successful, or a default value if there was an error
     * Example: getOrDefault(emptyList()) - give me the list, or an empty list if failed
     */
    fun getOrDefault(default: T): T = when (this) {
        is Success -> data
        is Error -> default
    }

    /**
     * Get the exception if there was an error, or null if successful
     */
    fun exceptionOrNull(): Exception? = when (this) {
        is Success -> null
        is Error -> exception
    }
}

/**
 * Helper function to create a Success result
 * Makes code cleaner: resultSuccess(myData) instead of Result.Success(myData)
 */
fun <T> resultSuccess(data: T): Result<T> = Result.Success(data)

/**
 * Helper function to create an Error result
 * Makes code cleaner: resultError(exception) instead of Result.Error(exception)
 */
fun resultError(exception: Exception): Result<Nothing> = Result.Error(exception)

/**
 * Helper function to create an Error result from a message
 * Example: resultError("Network connection failed")
 */
fun resultError(message: String): Result<Nothing> = Result.Error(Exception(message))
