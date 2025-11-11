package com.kannada.kavi.features.voice.api

import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.features.voice.api.models.*
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Bhashini API Service Interface
 *
 * Retrofit interface for communicating with Bhashini API.
 * Bhashini is an Indian government initiative for AI-powered language services.
 *
 * Documentation: https://bhashini.gitbook.io/bhashini-apis/
 *
 * HOW IT WORKS:
 * =============
 * Step 1: Get Pipeline Configuration (tells us which service to use)
 * Step 2: Call Inference API with the service ID (actual TTS/ASR)
 *
 * Features:
 * - Speech-to-Text (ASR) - Convert spoken Kannada to text
 * - Text-to-Speech (TTS) - Convert text to spoken Kannada
 */
interface BhashiniApiService {

    /**
     * Get Pipeline Configuration (Step 1)
     *
     * This must be called first to get:
     * - Service ID for TTS/ASR
     * - Inference endpoint URL
     * - Authentication token
     *
     * @param userId Your Bhashini user ID
     * @param apiKey Your Bhashini ulcaApiKey
     * @param request Pipeline configuration request
     * @return Pipeline configuration with service details
     */
    @POST(Constants.Voice.BHASHINI_ENDPOINT_PIPELINE)
    suspend fun getPipelineConfig(
        @Header("userID") userId: String,
        @Header("ulcaApiKey") apiKey: String,
        @Body request: PipelineConfigRequest
    ): PipelineConfigResponse

    /**
     * Text-to-Speech Inference (Step 2)
     *
     * Converts Kannada text to audio.
     * Uses dynamic URL from pipeline config response.
     *
     * @param url Inference endpoint URL from pipeline config
     * @param authToken Authorization token from pipeline config
     * @param request TTS request with text and configuration
     * @return TTS response with base64 encoded audio
     */
    @POST
    suspend fun textToSpeech(
        @Url url: String,
        @Header("Authorization") authToken: String,
        @Body request: BhashiniTTSRequest
    ): BhashiniTTSResponse

    /**
     * Speech-to-Text Inference (Step 2)
     *
     * Converts spoken Kannada audio to text.
     * Uses dynamic URL from pipeline config response.
     *
     * @param url Inference endpoint URL from pipeline config
     * @param authToken Authorization token from pipeline config
     * @param request ASR request with audio data
     * @return ASR response with transcribed text
     */
    @POST
    suspend fun speechToText(
        @Url url: String,
        @Header("Authorization") authToken: String,
        @Body request: SpeechToTextRequest
    ): SpeechToTextResponse
}



