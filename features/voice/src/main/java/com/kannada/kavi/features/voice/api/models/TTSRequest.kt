package com.kannada.kavi.features.voice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Request model for Bhashini Text-to-Speech API
 * 
 * This is sent to the Bhashini API to convert text to speech audio.
 */
data class TTSRequest(
    /**
     * Text to convert to speech
     */
    @SerializedName("text")
    val text: String,
    
    /**
     * Language code (e.g., "kn-IN" for Kannada)
     */
    @SerializedName("language")
    val language: String,
    
    /**
     * Voice ID (optional, uses default if not specified)
     */
    @SerializedName("voice_id")
    val voiceId: String? = null,
    
    /**
     * Speech rate (0.5 to 2.0, default 1.0)
     */
    @SerializedName("rate")
    val rate: Float = 1.0f,
    
    /**
     * Voice pitch (0.5 to 2.0, default 1.0)
     */
    @SerializedName("pitch")
    val pitch: Float = 1.0f
)



