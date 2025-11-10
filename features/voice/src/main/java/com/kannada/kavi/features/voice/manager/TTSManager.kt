package com.kannada.kavi.features.voice.manager

import android.content.ContentValues
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.kannada.kavi.features.voice.api.BhashiniApiClient
import com.kannada.kavi.features.voice.api.models.TTSRequest
import com.kannada.kavi.features.voice.api.models.TTSResponse
import com.kannada.kavi.features.voice.util.ApiKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * TTSManager - Comprehensive Text-to-Speech Manager
 *
 * Supports two TTS modes:
 * 1. **Bhashini API** (Online) - High-quality cloud-based TTS
 *    - Better voice quality
 *    - More natural sounding
 *    - Requires internet connection
 *    - Can download audio files
 *
 * 2. **Android TTS** (Offline) - Device's built-in TTS engine
 *    - Works offline
 *    - Faster response
 *    - Requires Kannada voice data to be installed
 *
 * Features:
 * - Automatic fallback from Bhashini to Android TTS
 * - Audio file download support (save as WAV/MP3)
 * - Speech rate and pitch control
 * - Progress callbacks
 * - Error handling
 *
 * Usage:
 * ```kotlin
 * val ttsManager = TTSManager(context)
 *
 * // Initialize
 * ttsManager.initialize { success ->
 *     if (success) {
 *         // Speak text using Bhashini (online)
 *         ttsManager.speak("ನಮಸ್ಕಾರ", useBhashini = true)
 *
 *         // Download audio file
 *         ttsManager.downloadAudio(
 *             text = "ನಮಸ್ಕಾರ",
 *             fileName = "greeting.wav"
 *         ) { success, filePath ->
 *             if (success) {
 *                 Log.d("TTS", "Saved to: $filePath")
 *             }
 *         }
 *     }
 * }
 *
 * // Clean up when done
 * ttsManager.shutdown()
 * ```
 */
class TTSManager(private val context: Context) {

    companion object {
        private const val TAG = "TTSManager"
        private const val KANNADA_LANGUAGE_CODE = "kn"
        private const val KANNADA_LOCALE = "kn-IN"
    }

    // Android TTS (Offline)
    private var androidTts: TextToSpeech? = null
    private var isAndroidTtsReady = false

    // Bhashini API (Online)
    private val bhashiniClient = BhashiniApiClient.create()
    private val apiKeyManager = ApiKeyManager(context)
    private var isBhashiniAvailable = false

    // Media player for Bhashini audio
    private var mediaPlayer: MediaPlayer? = null

    // Configuration
    private var speechRate = 1.0f
    private var pitch = 1.0f
    private var useBhashiniByDefault = true

    // Coroutine scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Callbacks
    private var onSpeakStart: (() -> Unit)? = null
    private var onSpeakComplete: (() -> Unit)? = null
    private var onSpeakError: ((String) -> Unit)? = null

    /**
     * Initialize TTS Manager
     * Sets up both Android TTS and checks Bhashini API availability
     */
    fun initialize(callback: (success: Boolean) -> Unit) {
        // Initialize Android TTS
        androidTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = androidTts?.setLanguage(Locale(KANNADA_LANGUAGE_CODE, "IN"))
                isAndroidTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED

                if (isAndroidTtsReady) {
                    setupAndroidTtsListeners()
                    Log.d(TAG, "Android TTS initialized successfully")
                } else {
                    Log.w(TAG, "Kannada language not available for Android TTS")
                }
            }

            // Check Bhashini API availability
            checkBhashiniAvailability()

            // Callback with success if either TTS is available
            callback(isAndroidTtsReady || isBhashiniAvailable)
        }
    }

    /**
     * Check if Bhashini API is available (has valid API key)
     */
    private fun checkBhashiniAvailability() {
        val apiKey = apiKeyManager.getBhashiniApiKey()
        isBhashiniAvailable = !apiKey.isNullOrBlank()
        Log.d(TAG, "Bhashini API available: $isBhashiniAvailable")
    }

    /**
     * Setup listeners for Android TTS
     */
    private fun setupAndroidTtsListeners() {
        androidTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeakStart?.invoke()
            }

            override fun onDone(utteranceId: String?) {
                onSpeakComplete?.invoke()
            }

            override fun onError(utteranceId: String?) {
                onSpeakError?.invoke("TTS playback error")
            }
        })
    }

    /**
     * Speak text using TTS
     *
     * @param text Text to speak in Kannada
     * @param useBhashini If true, tries Bhashini first, falls back to Android TTS
     * @param onStart Called when speech starts
     * @param onComplete Called when speech completes
     * @param onError Called if there's an error
     */
    fun speak(
        text: String,
        useBhashini: Boolean = useBhashiniByDefault,
        onStart: (() -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        this.onSpeakStart = onStart
        this.onSpeakComplete = onComplete
        this.onSpeakError = onError

        if (text.isBlank()) {
            onError?.invoke("Text is empty")
            return
        }

        if (useBhashini && isBhashiniAvailable) {
            speakUsingBhashini(text)
        } else if (isAndroidTtsReady) {
            speakUsingAndroidTts(text)
        } else {
            onError?.invoke("No TTS engine available")
        }
    }

    /**
     * Speak using Bhashini API (online)
     */
    private fun speakUsingBhashini(text: String) {
        scope.launch {
            try {
                onSpeakStart?.invoke()

                val apiKey = apiKeyManager.getBhashiniApiKey()
                if (apiKey.isNullOrBlank()) {
                    Log.w(TAG, "Bhashini API key not available, falling back to Android TTS")
                    speakUsingAndroidTts(text)
                    return@launch
                }

                val request = TTSRequest(
                    text = text,
                    language = KANNADA_LOCALE,
                    rate = speechRate,
                    pitch = pitch
                )

                val response = withContext(Dispatchers.IO) {
                    bhashiniClient.textToSpeech("Bearer $apiKey", request)
                }

                if (response.error != null) {
                    throw Exception(response.error)
                }

                // Decode base64 audio and play
                playBhashiniAudio(response)

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
     * Play audio received from Bhashini
     */
    private suspend fun playBhashiniAudio(response: TTSResponse) = withContext(Dispatchers.IO) {
        try {
            // Decode base64 audio
            val audioBytes = android.util.Base64.decode(response.audio, android.util.Base64.DEFAULT)

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

    /**
     * Speak using Android TTS (offline)
     */
    private fun speakUsingAndroidTts(text: String) {
        if (!isAndroidTtsReady) {
            onSpeakError?.invoke("Android TTS not available")
            return
        }

        androidTts?.setSpeechRate(speechRate)
        androidTts?.setPitch(pitch)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "tts-${System.currentTimeMillis()}")
        }

        androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))
    }

    /**
     * Download audio file using Bhashini TTS
     *
     * @param text Text to convert to speech
     * @param fileName Name of the file to save (e.g., "greeting.wav")
     * @param saveToDwonloads If true, saves to Downloads folder, else to app's files directory
     * @param callback Called with (success, filePath) when done
     */
    fun downloadAudio(
        text: String,
        fileName: String = "tts_${System.currentTimeMillis()}.wav",
        saveToDownloads: Boolean = true,
        callback: (success: Boolean, filePath: String?) -> Unit
    ) {
        scope.launch {
            try {
                val apiKey = apiKeyManager.getBhashiniApiKey()
                if (apiKey.isNullOrBlank()) {
                    callback(false, null)
                    return@launch
                }

                val request = TTSRequest(
                    text = text,
                    language = KANNADA_LOCALE,
                    rate = speechRate,
                    pitch = pitch
                )

                val response = withContext(Dispatchers.IO) {
                    bhashiniClient.textToSpeech("Bearer $apiKey", request)
                }

                if (response.error != null) {
                    throw Exception(response.error)
                }

                // Decode base64 audio
                val audioBytes = android.util.Base64.decode(response.audio, android.util.Base64.DEFAULT)

                if (saveToDownloads) {
                    // Use MediaStore for Downloads (Android 10+)
                    val savedUri = withContext(Dispatchers.IO) {
                        saveToMediaStore(audioBytes, fileName)
                    }

                    if (savedUri != null) {
                        Log.d(TAG, "Audio saved to Downloads: $savedUri")
                        callback(true, "Downloads/$fileName")
                    } else {
                        Log.e(TAG, "Failed to save to MediaStore")
                        callback(false, null)
                    }
                } else {
                    // Save to app's private storage
                    val directory = File(context.filesDir, "tts_audio")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }

                    val file = File(directory, fileName)
                    withContext(Dispatchers.IO) {
                        FileOutputStream(file).use { it.write(audioBytes) }
                    }

                    Log.d(TAG, "Audio saved to: ${file.absolutePath}")
                    callback(true, file.absolutePath)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to download audio: ${e.message}", e)
                callback(false, null)
            }
        }
    }

    /**
     * Save audio to MediaStore (Downloads folder)
     * Works on all Android versions, especially Android 10+
     */
    private fun saveToMediaStore(audioBytes: ByteArray, fileName: String): Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")

                // For Android 10+, use relative path
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val contentResolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val uri = contentResolver.insert(collection, contentValues)

            uri?.let { fileUri ->
                contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                    outputStream.write(audioBytes)
                }

                // Mark file as completed (Android 10+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(fileUri, contentValues, null, null)
                }

                fileUri
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to MediaStore: ${e.message}", e)
            null
        }
    }

    /**
     * Stop current speech
     */
    fun stop() {
        androidTts?.stop()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    /**
     * Set speech rate (0.5 to 2.0)
     */
    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
    }

    /**
     * Set pitch (0.5 to 2.0)
     */
    fun setPitch(pitch: Float) {
        this.pitch = pitch.coerceIn(0.5f, 2.0f)
    }

    /**
     * Set whether to use Bhashini by default
     */
    fun setUseBhashini(useBhashini: Boolean) {
        this.useBhashiniByDefault = useBhashini
    }

    /**
     * Check if Bhashini TTS is available
     */
    fun isBhashiniAvailable(): Boolean = isBhashiniAvailable

    /**
     * Check if Android TTS is available
     */
    fun isAndroidTtsAvailable(): Boolean = isAndroidTtsReady

    /**
     * Get list of available voices (Android TTS only)
     */
    fun getAvailableVoices(): List<android.speech.tts.Voice> {
        return androidTts?.voices?.filter { it.locale.language == KANNADA_LANGUAGE_CODE } ?: emptyList()
    }

    /**
     * Set voice (Android TTS only)
     */
    fun setVoice(voice: android.speech.tts.Voice) {
        androidTts?.voice = voice
    }

    /**
     * Clean up resources
     */
    fun shutdown() {
        stop()
        androidTts?.shutdown()
        androidTts = null
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
