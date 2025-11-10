package com.kannada.kavi.features.voice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Bhashini TTS Inference Response
 *
 * This is what we get back from the inference endpoint.
 * Contains the audio as base64 encoded string.
 */
data class BhashiniTTSResponse(
    @SerializedName("pipelineResponse")
    val pipelineResponse: List<TTSPipelineResponse>?
)

data class TTSPipelineResponse(
    @SerializedName("taskType")
    val taskType: String?, // Should be "tts"

    @SerializedName("audio")
    val audio: List<AudioContent>? // Audio data
)

data class AudioContent(
    @SerializedName("audioContent")
    val audioContent: String // Base64 encoded audio (WAV format)
)
