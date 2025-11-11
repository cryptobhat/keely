package com.kannada.kavi.features.voice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Request model for Bhashini Speech-to-Text API
 * 
 * This is sent to the Bhashini API to convert audio to text.
 */
data class SpeechToTextRequest(
    /**
     * Base64 encoded audio data
     */
    @SerializedName("audio")
    val audio: String,
    
    /**
     * Language code (e.g., "kn-IN" for Kannada)
     */
    @SerializedName("language")
    val language: String,
    
    /**
     * Audio format (e.g., "wav", "mp3", "flac")
     */
    @SerializedName("format")
    val format: String = "wav",
    
    /**
     * Sample rate in Hz (e.g., 16000)
     */
    @SerializedName("sample_rate")
    val sampleRate: Int = 16000,
    
    /**
     * Number of audio channels (1 = mono, 2 = stereo)
     */
    @SerializedName("channels")
    val channels: Int = 1
)



