# Bhashini API Integration Issues and Fixes

## 🔍 Problem Identified

The current Bhashini API integration has several critical issues:

### Issue 1: Incorrect API Endpoints ❌

**Current Implementation:**
```kotlin
const val BHASHINI_API_BASE_URL = "https://api.bhashini.gov.in/"
const val BHASHINI_API_ENDPOINT_STT = "v1/speech-to-text"
const val BHASHINI_API_ENDPOINT_TTS = "v1/text-to-speech"
```

**Problem:** These endpoints don't exist! Bhashini uses a different API structure.

### Issue 2: Incorrect Request/Response Models ❌

The current models don't match the actual Bhashini API specification.

### Issue 3: Missing Pipeline Configuration ❌

Bhashini requires a pipeline configuration step before making TTS/STT requests.

---

## ✅ Correct Bhashini API Structure

### How Bhashini API Actually Works

Bhashini API has a multi-step process:

#### Step 1: Get Pipeline Configuration
**Endpoint:** `https://meity-auth.ulcacontrib.org/ulca/apis/v0/model/getModelsPipeline`

**Request:**
```json
{
  "pipelineTasks": [
    {
      "taskType": "tts",
      "config": {
        "language": {
          "sourceLanguage": "kn"
        }
      }
    }
  ],
  "pipelineRequestConfig": {
    "pipelineId": "64392f96daac500b55c543cd"
  }
}
```

**Response:** Returns pipeline config with service IDs and inference API URL.

#### Step 2: Call Inference API (TTS)
**Endpoint:** (from Step 1 response) `https://dhruva-api.bhashini.gov.in/services/inference/pipeline`

**Request:**
```json
{
  "pipelineTasks": [
    {
      "taskType": "tts",
      "config": {
        "language": {
          "sourceLanguage": "kn"
        },
        "serviceId": "ai4bharat/indic-tts:v0",
        "gender": "female",
        "samplingRate": 8000
      }
    }
  ],
  "inputData": {
    "input": [
      {
        "source": "ನಮಸ್ಕಾರ"
      }
    ]
  }
}
```

**Response:**
```json
{
  "pipelineResponse": [
    {
      "taskType": "tts",
      "audio": [
        {
          "audioContent": "base64_encoded_audio_here"
        }
      ]
    }
  ]
}
```

---

## 🔧 Required Fixes

### Fix 1: Update Constants.kt

**File:** `core/common/src/main/java/.../Constants.kt`

**Replace:**
```kotlin
object Voice {
    // Network timeouts
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 60L
    const val WRITE_TIMEOUT_SECONDS = 60L

    // OLD - INCORRECT
    const val BHASHINI_API_BASE_URL = "https://api.bhashini.gov.in/"
    const val BHASHINI_API_ENDPOINT_STT = "v1/speech-to-text"
    const val BHASHINI_API_ENDPOINT_TTS = "v1/text-to-speech"
}
```

**With:**
```kotlin
object Voice {
    // Network timeouts
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 60L
    const val WRITE_TIMEOUT_SECONDS = 60L

    // Bhashini API - CORRECT
    const val BHASHINI_AUTH_BASE_URL = "https://meity-auth.ulcacontrib.org/"
    const val BHASHINI_INFERENCE_BASE_URL = "https://dhruva-api.bhashini.gov.in/"

    const val BHASHINI_ENDPOINT_PIPELINE = "ulca/apis/v0/model/getModelsPipeline"
    const val BHASHINI_ENDPOINT_INFERENCE = "services/inference/pipeline"

    // Pipeline IDs (from Bhashini documentation)
    const val BHASHINI_PIPELINE_ID = "64392f96daac500b55c543cd"

    // Service IDs
    const val BHASHINI_TTS_SERVICE_ID = "ai4bharat/indic-tts:v0"
    const val BHASHINI_ASR_SERVICE_ID = "ai4bharat/conformer-hi-gpu--t4"
}
```

### Fix 2: Create New Request/Response Models

#### A. Pipeline Configuration Models

**Create:** `features/voice/.../models/PipelineConfigRequest.kt`

```kotlin
package com.kannada.kavi.features.voice.api.models

import com.google.gson.annotations.SerializedName

data class PipelineConfigRequest(
    @SerializedName("pipelineTasks")
    val pipelineTasks: List<PipelineTask>,

    @SerializedName("pipelineRequestConfig")
    val pipelineRequestConfig: PipelineRequestConfig
)

data class PipelineTask(
    @SerializedName("taskType")
    val taskType: String, // "tts" or "asr"

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
    val pipelineId: String
)
```

**Create:** `features/voice/.../models/PipelineConfigResponse.kt`

```kotlin
package com.kannada.kavi.features.voice.api.models

import com.google.gson.annotations.SerializedName

data class PipelineConfigResponse(
    @SerializedName("pipelineInferenceAPIEndPoint")
    val inferenceEndpoint: InferenceEndpoint?,

    @SerializedName("pipelineResponseConfig")
    val responseConfig: List<PipelineResponseConfig>?
)

data class InferenceEndpoint(
    @SerializedName("callbackUrl")
    val callbackUrl: String?,

    @SerializedName("inferenceApiKey")
    val apiKey: InferenceApiKey?
)

data class InferenceApiKey(
    @SerializedName("name")
    val name: String?,

    @SerializedName("value")
    val value: String?
)

data class PipelineResponseConfig(
    @SerializedName("taskType")
    val taskType: String?,

    @SerializedName("config")
    val config: ServiceConfig?
)

data class ServiceConfig(
    @SerializedName("serviceId")
    val serviceId: String?,

    @SerializedName("modelId")
    val modelId: String?,

    @SerializedName("language")
    val language: LanguageConfig?
)
```

#### B. TTS Inference Models

**Create:** `features/voice/.../models/BhashiniTTSRequest.kt`

```kotlin
package com.kannada.kavi.features.voice.api.models

import com.google.gson.annotations.SerializedName

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
    val serviceId: String,

    @SerializedName("gender")
    val gender: String = "female", // "male" or "female"

    @SerializedName("samplingRate")
    val samplingRate: Int = 8000 // 8000 or 16000
)

data class TTSInputData(
    @SerializedName("input")
    val input: List<TTSInput>
)

data class TTSInput(
    @SerializedName("source")
    val source: String // The text to convert to speech
)
```

**Create:** `features/voice/.../models/BhashiniTTSResponse.kt`

```kotlin
package com.kannada.kavi.features.voice.api.models

import com.google.gson.annotations.SerializedName

data class BhashiniTTSResponse(
    @SerializedName("pipelineResponse")
    val pipelineResponse: List<TTSPipelineResponse>?
)

data class TTSPipelineResponse(
    @SerializedName("taskType")
    val taskType: String?,

    @SerializedName("audio")
    val audio: List<AudioContent>?
)

data class AudioContent(
    @SerializedName("audioContent")
    val audioContent: String // Base64 encoded audio
)
```

### Fix 3: Update BhashiniApiService

**File:** `features/voice/.../BhashiniApiService.kt`

```kotlin
package com.kannada.kavi.features.voice.api

import com.kannada.kavi.features.voice.api.models.*
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface BhashiniApiService {

    /**
     * Get pipeline configuration
     * This must be called first to get the inference endpoint and service IDs
     */
    @POST("ulca/apis/v0/model/getModelsPipeline")
    suspend fun getPipelineConfig(
        @Header("userID") userId: String,
        @Header("ulcaApiKey") apiKey: String,
        @Body request: PipelineConfigRequest
    ): PipelineConfigResponse

    /**
     * Text-to-Speech inference
     * Uses dynamic URL from pipeline config response
     */
    @POST
    suspend fun textToSpeech(
        @Url url: String,
        @Header("Authorization") authToken: String,
        @Body request: BhashiniTTSRequest
    ): BhashiniTTSResponse
}
```

### Fix 4: Update BhashiniApiClient

**File:** `features/voice/.../BhashiniApiClient.kt`

```kotlin
package com.kannada.kavi.features.voice.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object BhashiniApiClient {

    // Client for pipeline config endpoint
    private val authRetrofit by lazy {
        createRetrofit("https://meity-auth.ulcacontrib.org/")
    }

    // Client for inference endpoint
    private val inferenceRetrofit by lazy {
        createRetrofit("https://dhruva-api.bhashini.gov.in/")
    }

    private fun createRetrofit(baseUrl: String): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createAuthService(): BhashiniApiService {
        return authRetrofit.create(BhashiniApiService::class.java)
    }

    fun createInferenceService(): BhashiniApiService {
        return inferenceRetrofit.create(BhashiniApiService::class.java)
    }
}
```

### Fix 5: Update TTSManager to Use Correct API

**File:** `features/voice/.../TTSManager.kt`

Add these methods:

```kotlin
// Cache for pipeline config (avoid repeated calls)
private var cachedPipelineConfig: PipelineConfigResponse? = null
private var cachedServiceId: String? = null

/**
 * Get pipeline configuration (cached)
 */
private suspend fun getPipelineConfig(): PipelineConfigResponse? {
    if (cachedPipelineConfig != null) {
        return cachedPipelineConfig
    }

    return try {
        val apiKey = apiKeyManager.getBhashiniApiKey() ?: return null
        val userId = apiKeyManager.getBhashiniUserId() ?: "kavi-keyboard-user"

        val request = PipelineConfigRequest(
            pipelineTasks = listOf(
                PipelineTask(
                    taskType = "tts",
                    config = PipelineTaskConfig(
                        language = LanguageConfig(sourceLanguage = "kn")
                    )
                )
            ),
            pipelineRequestConfig = PipelineRequestConfig(
                pipelineId = Constants.Voice.BHASHINI_PIPELINE_ID
            )
        )

        val authService = BhashiniApiClient.createAuthService()
        val response = authService.getPipelineConfig(
            userId = userId,
            apiKey = apiKey,
            request = request
        )

        // Extract service ID from response
        cachedServiceId = response.responseConfig
            ?.firstOrNull { it.taskType == "tts" }
            ?.config?.serviceId

        cachedPipelineConfig = response
        response
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get pipeline config: ${e.message}", e)
        null
    }
}

/**
 * Speak using Bhashini API (CORRECTED)
 */
private fun speakUsingBhashini(text: String) {
    scope.launch {
        try {
            onSpeakStart?.invoke()

            // Step 1: Get pipeline config
            val pipelineConfig = getPipelineConfig()
            if (pipelineConfig == null) {
                Log.e(TAG, "Failed to get pipeline config")
                speakUsingAndroidTts(text)
                return@launch
            }

            val serviceId = cachedServiceId ?: Constants.Voice.BHASHINI_TTS_SERVICE_ID
            val inferenceUrl = pipelineConfig.inferenceEndpoint?.callbackUrl
                ?: "https://dhruva-api.bhashini.gov.in/services/inference/pipeline"

            // Step 2: Make TTS request
            val ttsRequest = BhashiniTTSRequest(
                pipelineTasks = listOf(
                    TTSPipelineTask(
                        taskType = "tts",
                        config = TTSTaskConfig(
                            language = LanguageConfig(sourceLanguage = "kn"),
                            serviceId = serviceId,
                            gender = "female",
                            samplingRate = 8000
                        )
                    )
                ),
                inputData = TTSInputData(
                    input = listOf(TTSInput(source = text))
                )
            )

            val apiKey = apiKeyManager.getBhashiniApiKey() ?: ""
            val authToken = pipelineConfig.inferenceEndpoint?.apiKey?.value ?: apiKey

            val inferenceService = BhashiniApiClient.createInferenceService()
            val response = withContext(Dispatchers.IO) {
                inferenceService.textToSpeech(
                    url = inferenceUrl,
                    authToken = authToken,
                    request = ttsRequest
                )
            }

            // Extract audio from response
            val audioContent = response.pipelineResponse
                ?.firstOrNull { it.taskType == "tts" }
                ?.audio
                ?.firstOrNull()
                ?.audioContent

            if (audioContent != null) {
                playBhashiniAudioContent(audioContent)
            } else {
                throw Exception("No audio content in response")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Bhashini TTS failed: ${e.message}", e)
            onSpeakError?.invoke("Failed to synthesize speech: ${e.message}")

            // Fallback to Android TTS
            if (isAndroidTtsReady) {
                Log.d(TAG, "Falling back to Android TTS")
                speakUsingAndroidTts(text)
            }
        }
    }
}

/**
 * Play audio content from Bhashini
 */
private suspend fun playBhashiniAudioContent(base64Audio: String) = withContext(Dispatchers.IO) {
    try {
        // Decode base64 audio
        val audioBytes = android.util.Base64.decode(base64Audio, android.util.Base64.DEFAULT)

        // Save to temporary file
        val tempFile = File.createTempFile("tts_temp", ".wav", context.cacheDir)
        FileOutputStream(tempFile).use { it.write(audioBytes) }

        // Play using MediaPlayer
        withContext(Dispatchers.Main) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener {
                    tempFile.delete()
                    onSpeakComplete?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    tempFile.delete()
                    onSpeakError?.invoke("Playback error")
                    true
                }
                prepare()
                start()
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to play Bhashini audio: ${e.message}", e)
        onSpeakError?.invoke("Failed to play audio")
    }
}
```

---

## 🔑 API Key Setup

### Where to Get Bhashini API Keys

1. **Visit:** https://bhashini.gov.in/ulca/user/register
2. **Register** for a developer account
3. **Navigate to:** API Keys section
4. **Generate** new API key

You'll get:
- **ulcaApiKey** - Main API key
- **userID** - Your user ID

### Add to local.properties

```properties
BHASHINI_API_KEY=your_ulca_api_key_here
BHASHINI_USER_ID=your_user_id_here
```

### Update ApiKeyManager

**File:** `features/voice/.../util/ApiKeyManager.kt`

Add method:
```kotlin
fun getBhashiniUserId(): String? {
    return sharedPreferences.getString("bhashini_user_id", null)
        ?: System.getenv("BHASHINI_USER_ID")
}
```

---

## 🧪 Testing the Fix

### Test 1: Pipeline Configuration

```kotlin
val apiKey = "your_ulca_api_key"
val userId = "your_user_id"

val request = PipelineConfigRequest(
    pipelineTasks = listOf(
        PipelineTask(
            taskType = "tts",
            config = PipelineTaskConfig(
                language = LanguageConfig(sourceLanguage = "kn")
            )
        )
    ),
    pipelineRequestConfig = PipelineRequestConfig(
        pipelineId = "64392f96daac500b55c543cd"
    )
)

val service = BhashiniApiClient.createAuthService()
val response = service.getPipelineConfig(userId, apiKey, request)
Log.d("Bhashini", "Pipeline config: $response")
```

### Test 2: TTS Request

```kotlin
val ttsRequest = BhashiniTTSRequest(
    pipelineTasks = listOf(
        TTSPipelineTask(
            taskType = "tts",
            config = TTSTaskConfig(
                language = LanguageConfig(sourceLanguage = "kn"),
                serviceId = "ai4bharat/indic-tts:v0",
                gender = "female",
                samplingRate = 8000
            )
        )
    ),
    inputData = TTSInputData(
        input = listOf(TTSInput(source = "ನಮಸ್ಕಾರ"))
    )
)

val service = BhashiniApiClient.createInferenceService()
val response = service.textToSpeech(
    url = "https://dhruva-api.bhashini.gov.in/services/inference/pipeline",
    authToken = authToken,
    request = ttsRequest
)
Log.d("Bhashini", "TTS response: ${response.pipelineResponse}")
```

---

## 📋 Implementation Checklist

- [ ] Update Constants.kt with correct endpoints
- [ ] Create PipelineConfigRequest/Response models
- [ ] Create BhashiniTTSRequest/Response models
- [ ] Update BhashiniApiService interface
- [ ] Update BhashiniApiClient
- [ ] Update TTSManager with correct API flow
- [ ] Add getBhashiniUserId() to ApiKeyManager
- [ ] Test pipeline configuration API
- [ ] Test TTS inference API
- [ ] Update BHASHINI_SETUP.md documentation
- [ ] Test end-to-end TTS flow
- [ ] Test audio download feature
- [ ] Handle error cases and fallbacks

---

## 🎯 Expected Results After Fix

✅ Pipeline config call succeeds
✅ Service IDs are retrieved correctly
✅ TTS inference returns audio
✅ Audio plays correctly
✅ Download feature works
✅ Fallback to Android TTS works on errors

---

## 📚 References

- **Bhashini Documentation:** https://bhashini.gitbook.io/bhashini-apis/
- **API Playground:** https://bhashini.gov.in/ulca/model/explore-models
- **ULCA Portal:** https://bhashini.gov.in/ulca/

---

## 🐛 Common Issues After Fix

### Issue: "Invalid API Key"
**Solution:** Make sure you're using the ulcaApiKey from Bhashini portal

### Issue: "Pipeline not found"
**Solution:** Use the correct pipeline ID: `64392f96daac500b55c543cd`

### Issue: "Service not available"
**Solution:** Check if Kannada TTS service is available in the pipeline config response

### Issue: "Audio is corrupted"
**Solution:** Make sure sampling rate is 8000 or 16000 (not custom values)

---

**This fix should resolve all Bhashini API integration issues!** 🎉
