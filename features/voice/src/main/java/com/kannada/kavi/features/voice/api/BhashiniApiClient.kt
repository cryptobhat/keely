package com.kannada.kavi.features.voice.api

import com.kannada.kavi.core.common.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Bhashini API Client Factory
 *
 * Creates and configures Retrofit clients for Bhashini API.
 * Uses two different base URLs:
 * 1. Auth endpoint - for pipeline configuration
 * 2. Inference endpoint - for actual TTS/ASR (URL from config)
 *
 * Usage:
 * ```kotlin
 * // Step 1: Get pipeline config
 * val authService = BhashiniApiClient.createAuthService()
 * val pipelineConfig = authService.getPipelineConfig(userId, apiKey, request)
 *
 * // Step 2: Call inference
 * val inferenceService = BhashiniApiClient.createInferenceService()
 * val ttsResponse = inferenceService.textToSpeech(url, authToken, ttsRequest)
 * ```
 */
object BhashiniApiClient {

    /**
     * Create OkHttp client with logging and timeouts
     */
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(Constants.Network.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.Network.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.Network.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Create Retrofit instance with given base URL
     */
    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Lazy-initialized auth service (for pipeline config)
     */
    private val authRetrofit by lazy {
        createRetrofit(Constants.Voice.BHASHINI_AUTH_BASE_URL)
    }

    /**
     * Lazy-initialized inference service
     */
    private val inferenceRetrofit by lazy {
        createRetrofit(Constants.Voice.BHASHINI_INFERENCE_BASE_URL)
    }

    /**
     * Create service for pipeline configuration requests
     *
     * @return BhashiniApiService configured for auth endpoint
     */
    fun createAuthService(): BhashiniApiService {
        return authRetrofit.create(BhashiniApiService::class.java)
    }

    /**
     * Create service for inference requests (TTS/ASR)
     *
     * @return BhashiniApiService configured for inference endpoint
     */
    fun createInferenceService(): BhashiniApiService {
        return inferenceRetrofit.create(BhashiniApiService::class.java)
    }
}

