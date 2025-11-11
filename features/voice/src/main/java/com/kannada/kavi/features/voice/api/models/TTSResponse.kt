package com.kannada.kavi.features.voice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Response model from Bhashini Text-to-Speech API
 * 
 * This is received from the Bhashini API after converting text to speech.
 */
data class TTSResponse(
    /**
     * Base64 encoded audio data
     */
    @SerializedName("audio")
    val audio: String,
    
    /**
     * Audio format (e.g., "wav", "mp3")
     */
    @SerializedName("format")
    val format: String? = null,
    
    /**
     * Sample rate in Hz
     */
    @SerializedName("sample_rate")
    val sampleRate: Int? = null,
    
    /**
     * Processing time in milliseconds
     */
    @SerializedName("processing_time_ms")
    val processingTimeMs: Long? = null,
    
    /**
     * Error message if conversion failed
     */
    @SerializedName("error")
    val error: String? = null
)




