package com.kannada.kavi.features.voice.manager

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import androidx.core.content.ContextCompat
import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.core.common.Result
import com.kannada.kavi.features.voice.api.BhashiniApiService
import com.kannada.kavi.features.voice.api.models.SpeechToTextRequest
import com.kannada.kavi.features.voice.recorder.AudioRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException

/**
 * VoiceInputManager - Handles voice input using Bhashini API
 * 
 * This manager orchestrates the entire voice input flow:
 * 1. Check/request microphone permission
 * 2. Record audio from microphone
 * 3. Convert audio to base64
 * 4. Send to Bhashini API
 * 5. Return transcribed text
 * 
 * Usage:
 * ```kotlin
 * val manager = VoiceInputManager(context, apiService, audioRecorder, apiKey)
 * when (val result = manager.startVoiceInput()) {
 *     is Result.Success -> println("Transcribed: ${result.data}")
 *     is Result.Error -> println("Error: ${result.exception.message}")
 * }
 * ```
 */
class VoiceInputManager(
    private val context: Context,
    private val apiService: BhashiniApiService,
    private val audioRecorder: AudioRecorder,
    private val apiKey: String
) {
    
    /**
     * Start voice input and get transcription
     * 
     * Flow:
     * 1. Check RECORD_AUDIO permission
     * 2. Start recording
     * 3. Wait for user to stop (or timeout)
     * 4. Convert audio to base64
     * 5. Send to Bhashini API
     * 6. Return transcribed text
     * 
     * @return Result containing transcribed text or error
     */
    suspend fun startVoiceInput(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Check permission
            if (!hasRecordAudioPermission()) {
                return@withContext Result.Error(
                    SecurityException("RECORD_AUDIO permission not granted")
                )
            }
            
            // 2. Start recording
            audioRecorder.startRecording()
            
            // 3. Wait for recording to complete (with timeout)
            // Note: In actual implementation, you'll need a callback/flow
            // to know when user stops recording. For now, we'll use a timeout.
            // This should be replaced with actual UI interaction.
            kotlinx.coroutines.delay(Constants.Voice.MAX_RECORDING_DURATION_MS)
            
            // 4. Stop recording and get audio bytes
            val audioBytes = audioRecorder.stopRecording()
            
            // Check minimum duration
            val durationMs = (audioBytes.size / (Constants.Voice.AUDIO_SAMPLE_RATE * 2.0) * 1000).toLong()
            if (durationMs < Constants.Voice.MIN_RECORDING_DURATION_MS) {
                return@withContext Result.Error(
                    IllegalArgumentException("Recording too short: ${durationMs}ms")
                )
            }
            
            // 5. Convert audio to base64
            val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            
            // 6. Create API request
            val request = SpeechToTextRequest(
                audio = audioBase64,
                language = Constants.Voice.LANGUAGE_CODE_KANNADA,
                format = "wav",
                sampleRate = Constants.Voice.AUDIO_SAMPLE_RATE,
                channels = Constants.Voice.AUDIO_CHANNELS
            )
            
            // TODO: Update to use two-step flow like TTSManager
            // 7. Call Bhashini API with timeout (currently using old API - needs update)
            // Step 1: Get pipeline config
            // Step 2: Call speechToText with dynamic URL
            Result.Error(UnsupportedOperationException("Speech-to-Text needs to be updated to use new two-step API flow"))
            
        } catch (e: SecurityException) {
            Result.Error(e)
        } catch (e: IllegalStateException) {
            Result.Error(e)
        } catch (e: IOException) {
            Result.Error(e)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.Error(IOException("Request timed out", e))
        } catch (e: Exception) {
            Result.Error(e)
        } finally {
            // Ensure recording is stopped
            if (audioRecorder.isRecording()) {
                audioRecorder.cancelRecording()
            }
        }
    }
    
    /**
     * Stop current recording and get transcription
     * 
     * This should be called when user manually stops recording.
     * 
     * @return Result containing transcribed text or error
     */
    suspend fun stopAndTranscribe(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!audioRecorder.isRecording()) {
                return@withContext Result.Error(
                    IllegalStateException("Not currently recording")
                )
            }
            
            // Stop recording
            val audioBytes = audioRecorder.stopRecording()
            
            // Check minimum duration
            val durationMs = (audioBytes.size / (Constants.Voice.AUDIO_SAMPLE_RATE * 2.0) * 1000).toLong()
            if (durationMs < Constants.Voice.MIN_RECORDING_DURATION_MS) {
                return@withContext Result.Error(
                    IllegalArgumentException("Recording too short: ${durationMs}ms")
                )
            }
            
            // Convert to base64
            val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            
            // Create request
            val request = SpeechToTextRequest(
                audio = audioBase64,
                language = Constants.Voice.LANGUAGE_CODE_KANNADA,
                format = "wav",
                sampleRate = Constants.Voice.AUDIO_SAMPLE_RATE,
                channels = Constants.Voice.AUDIO_CHANNELS
            )
            
            // TODO: Update to use two-step flow like TTSManager
            // Call API (currently using old API - needs update)
            Result.Error(UnsupportedOperationException("Speech-to-Text needs to be updated to use new two-step API flow"))
            
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Check if RECORD_AUDIO permission is granted
     */
    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Release all resources
     */
    fun release() {
        audioRecorder.release()
    }
}



