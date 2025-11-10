package com.kannada.kavi.features.voice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Pipeline Configuration Request
 *
 * This is the first step in using Bhashini API.
 * We request a pipeline configuration which tells us:
 * - Which service ID to use for TTS/ASR
 * - What inference endpoint URL to call
 * - What authentication to use
 */
data class PipelineConfigRequest(
    @SerializedName("pipelineTasks")
    val pipelineTasks: List<PipelineTask>,

    @SerializedName("pipelineRequestConfig")
    val pipelineRequestConfig: PipelineRequestConfig
)

data class PipelineTask(
    @SerializedName("taskType")
    val taskType: String, // "tts" for text-to-speech, "asr" for speech-to-text

    @SerializedName("config")
    val config: PipelineTaskConfig
)

data class PipelineTaskConfig(
    @SerializedName("language")
    val language: LanguageConfig
)

data class LanguageConfig(
    @SerializedName("sourceLanguage")
    val sourceLanguage: String // "kn" for Kannada
)

data class PipelineRequestConfig(
    @SerializedName("pipelineId")
    val pipelineId: String // Fixed pipeline ID from Bhashini
)
