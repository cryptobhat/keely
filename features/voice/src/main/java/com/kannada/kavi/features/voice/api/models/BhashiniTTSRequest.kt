package com.kannada.kavi.features.voice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Bhashini TTS Inference Request
 *
 * This is sent to the inference endpoint (Step 2) to actually
 * convert text to speech.
 */
data class BhashiniTTSRequest(
    @SerializedName("pipelineTasks")
    val pipelineTasks: List<TTSPipelineTask>,

    @SerializedName("inputData")
    val inputData: TTSInputData
)

data class TTSPipelineTask(
    @SerializedName("taskType")
    val taskType: String = "tts",

    @SerializedName("config")
    val config: TTSTaskConfig
)

data class TTSTaskConfig(
    @SerializedName("language")
    val language: LanguageConfig,

    @SerializedName("serviceId")
    val serviceId: String, // From pipeline config response

    @SerializedName("gender")
    val gender: String = "female", // "male" or "female"

    @SerializedName("samplingRate")
    val samplingRate: Int = 8000 // 8000 or 16000 Hz
)

data class TTSInputData(
    @SerializedName("input")
    val input: List<TTSInput>
)

data class TTSInput(
    @SerializedName("source")
    val source: String // The text to convert to speech
)
