package com.kannada.kavi.features.voice.util

import android.content.Context
import java.util.Properties
import java.io.File

/**
 * ApiKeyManager - Manages Bhashini API key
 *
 * Tries to load API key from:
 * 1. BuildConfig (if set during build)
 * 2. SharedPreferences (user-configurable at runtime)
 * 3. Environment variable
 *
 * Usage:
 * ```kotlin
 * val apiKeyManager = ApiKeyManager(context)
 * val apiKey = apiKeyManager.getBhashiniApiKey()
 * if (apiKey == null) {
 *     // Show error: API key not configured
 * }
 * ```
 */
class ApiKeyManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "bhashini_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_INFERENCE_KEY = "inference_key"
    }

    /**
     * Get Bhashini API key
     *
     * Tries multiple sources in order:
     * 1. SharedPreferences (user-configurable)
     * 2. BuildConfig.BHASHINI_API_KEY (if available)
     * 3. Environment variable BHASHINI_API_KEY
     *
     * @return API key or null if not found
     */
    fun getBhashiniApiKey(): String? {
        // Try SharedPreferences first (user-configured)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prefsKey = prefs.getString(KEY_API_KEY, null)
        if (!prefsKey.isNullOrBlank()) {
            return prefsKey
        }

        // Try BuildConfig (build-time configuration)
        try {
            val buildConfigKey = getBuildConfigApiKey()
            if (!buildConfigKey.isNullOrBlank()) {
                return buildConfigKey
            }
        } catch (e: Exception) {
            // BuildConfig not available, continue
        }

        // Try environment variable
        val envKey = System.getenv("BHASHINI_API_KEY")
        if (!envKey.isNullOrBlank()) {
            return envKey
        }

        return null
    }

    /**
     * Set Bhashini API key in SharedPreferences
     * Allows users to configure API key at runtime
     */
    fun setBhashiniApiKey(apiKey: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    /**
     * Get Bhashini User ID
     *
     * Tries multiple sources in order:
     * 1. SharedPreferences (user-configured)
     * 2. BuildConfig.BHASHINI_USER_ID (if available)
     * 3. Environment variable BHASHINI_USER_ID
     *
     * @return User ID or null if not found
     */
    fun getBhashiniUserId(): String? {
        // Try SharedPreferences first
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prefsUserId = prefs.getString(KEY_USER_ID, null)
        if (!prefsUserId.isNullOrBlank()) {
            return prefsUserId
        }

        // Try BuildConfig
        try {
            val buildConfigUserId = getBuildConfigValue("BHASHINI_USER_ID")
            if (!buildConfigUserId.isNullOrBlank()) {
                return buildConfigUserId
            }
        } catch (e: Exception) {
            // BuildConfig not available
        }

        // Try environment variable
        val envUserId = System.getenv("BHASHINI_USER_ID")
        if (!envUserId.isNullOrBlank()) {
            return envUserId
        }

        return null
    }

    /**
     * Set Bhashini User ID in SharedPreferences
     */
    fun setBhashiniUserId(userId: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    /**
     * Get Bhashini Inference API Key (optional - sometimes provided by pipeline config)
     *
     * Tries multiple sources in order:
     * 1. SharedPreferences (user-configured)
     * 2. BuildConfig.BHASHINI_INFERENCE_KEY (if available)
     * 3. Environment variable BHASHINI_INFERENCE_KEY
     *
     * @return Inference API key or null if not found
     */
    fun getBhashiniInferenceKey(): String? {
        // Try SharedPreferences first
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prefsKey = prefs.getString(KEY_INFERENCE_KEY, null)
        if (!prefsKey.isNullOrBlank()) {
            return prefsKey
        }

        // Try BuildConfig
        try {
            val buildConfigKey = getBuildConfigValue("BHASHINI_INFERENCE_KEY")
            if (!buildConfigKey.isNullOrBlank()) {
                return buildConfigKey
            }
        } catch (e: Exception) {
            // BuildConfig not available
        }

        // Try environment variable
        val envKey = System.getenv("BHASHINI_INFERENCE_KEY")
        if (!envKey.isNullOrBlank()) {
            return envKey
        }

        return null
    }

    /**
     * Set Bhashini Inference Key in SharedPreferences
     */
    fun setBhashiniInferenceKey(inferenceKey: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_INFERENCE_KEY, inferenceKey).apply()
    }

    /**
     * Get API key from BuildConfig
     * This reads from app module's BuildConfig
     */
    private fun getBuildConfigApiKey(): String? {
        return getBuildConfigValue("BHASHINI_API_KEY")
    }

    /**
     * Get value from BuildConfig
     * Generic method to read any field from BuildConfig
     */
    private fun getBuildConfigValue(fieldName: String): String? {
        return try {
            val buildConfigClass = Class.forName("com.kannada.kavi.BuildConfig")
            val field = buildConfigClass.getField(fieldName)
            val value = field.get(null) as? String
            if (value.isNullOrBlank()) null else value
        } catch (e: Exception) {
            null
        }
    }
}

