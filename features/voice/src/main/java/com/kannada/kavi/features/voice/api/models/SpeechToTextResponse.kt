package com.kannada.kavi.features.voice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Response model from Bhashini Speech-to-Text API
 * 
 * This is received from the Bhashini API after processing audio.
 */
data class SpeechToTextResponse(
    /**
     * Transcribed text in the requested language
     */
    @SerializedName("text")
    val text: String,
    
    /**
     * Confidence score (0.0 to 1.0)
     * Higher value means more confident transcription
     */
    @SerializedName("confidence")
    val confidence: Float? = null,
    
    /**
     * Language detected (may differ from requested)
     */
    @SerializedName("language")
    val language: String? = null,
    
    /**
     * Processing time in milliseconds
     */
    @SerializedName("processing_time_ms")
    val processingTimeMs: Long? = null,
    
    /**
     * Error message if transcription failed
     */
    @SerializedName("error")
    val error: String? = null
)




