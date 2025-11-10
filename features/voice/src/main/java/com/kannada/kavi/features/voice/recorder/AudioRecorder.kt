package com.kannada.kavi.features.voice.recorder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.kannada.kavi.core.common.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * AudioRecorder - Records audio from microphone
 * 
 * Records audio in the format required by Bhashini API:
 * - Sample rate: 16kHz
 * - Channels: Mono (1 channel)
 * - Encoding: PCM 16-bit
 * 
 * Usage:
 * ```kotlin
 * val recorder = AudioRecorder()
 * recorder.startRecording()
 * // ... wait for user to stop ...
 * val audioBytes = recorder.stopRecording()
 * ```
 */
class AudioRecorder {
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val audioBytes = ByteArrayOutputStream()
    
    // Audio configuration matching Bhashini API requirements
    private val sampleRate = Constants.Voice.AUDIO_SAMPLE_RATE
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    
    // Buffer size for reading audio data
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        channelConfig,
        audioFormat
    )
    
    /**
     * Start recording audio from microphone
     * 
     * @throws IllegalStateException if already recording
     * @throws SecurityException if RECORD_AUDIO permission not granted
     */
    fun startRecording() {
        if (isRecording) {
            throw IllegalStateException("Already recording")
        }
        
        // Clear previous recording
        audioBytes.reset()
        
        // Create AudioRecord instance
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize * 2 // Use 2x buffer size for stability
        )
        
        // Check if AudioRecord is initialized properly
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord initialization failed")
        }
        
        // Start recording
        audioRecord?.startRecording()
        isRecording = true
    }
    
    /**
     * Stop recording and return audio data
     * 
     * @return ByteArray of recorded audio in PCM format
     * @throws IllegalStateException if not recording
     */
    suspend fun stopRecording(): ByteArray = withContext(Dispatchers.IO) {
        if (!isRecording || audioRecord == null) {
            throw IllegalStateException("Not recording")
        }
        
        // Stop recording
        audioRecord?.stop()
        isRecording = false
        
        // Read remaining audio data
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int
        
        while (true) {
            bytesRead = audioRecord!!.read(buffer, 0, buffer.size)
            if (bytesRead <= 0) break
            audioBytes.write(buffer, 0, bytesRead)
        }
        
        // Release AudioRecord
        audioRecord?.release()
        audioRecord = null
        
        // Return recorded audio
        audioBytes.toByteArray()
    }
    
    /**
     * Cancel recording without returning data
     */
    fun cancelRecording() {
        if (isRecording) {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            isRecording = false
            audioBytes.reset()
        }
    }
    
    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * Release all resources
     */
    fun release() {
        cancelRecording()
    }
}

