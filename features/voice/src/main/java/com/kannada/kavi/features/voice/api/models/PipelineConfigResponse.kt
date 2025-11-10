package com.kannada.kavi.features.voice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Pipeline Configuration Response
 *
 * This is what we get back from the pipeline config request.
 * It contains:
 * - The inference endpoint URL to call for actual TTS/ASR
 * - Authentication tokens
 * - Service IDs to use
 */
data class PipelineConfigResponse(
    @SerializedName("pipelineInferenceAPIEndPoint")
    val inferenceEndpoint: InferenceEndpoint?,

    @SerializedName("pipelineResponseConfig")
    val responseConfig: List<PipelineResponseConfig>?
)

data class InferenceEndpoint(
    @SerializedName("callbackUrl")
    val callbackUrl: String?, // The URL to call for inference

    @SerializedName("inferenceApiKey")
    val apiKey: InferenceApiKey? // Auth token for inference
)

data class InferenceApiKey(
    @SerializedName("name")
    val name: String?, // Key name (usually "Authorization")

    @SerializedName("value")
    val value: String? // The actual auth token
)

data class PipelineResponseConfig(
    @SerializedName("taskType")
    val taskType: String?, // "tts" or "asr"

    @SerializedName("config")
    val config: ServiceConfig? // Service configuration
)

data class ServiceConfig(
    @SerializedName("serviceId")
    val serviceId: String?, // The service ID to use (e.g., "ai4bharat/indic-tts:v0")

    @SerializedName("modelId")
    val modelId: String?, // Model ID

    @SerializedName("language")
    val language: LanguageConfig? // Language config
)
