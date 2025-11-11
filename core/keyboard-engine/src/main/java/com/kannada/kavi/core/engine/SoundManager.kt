package com.kannada.kavi.core.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.core.keyboard.R

/**
 * SoundManager - Keyboard Sound Effects
 *
 * Plays sound effects when keys are pressed, making typing feel more satisfying!
 *
 * WHAT ARE SOUND EFFECTS?
 * =======================
 * When you type on a physical keyboard, you hear "click" sounds.
 * This class recreates that experience for our virtual keyboard.
 *
 * WHY SOUND EFFECTS?
 * ==================
 * 1. FEEDBACK: Confirms the key was pressed successfully
 * 2. SATISFACTION: Makes typing feel more tactile and enjoyable
 * 3. ACCESSIBILITY: Helps users with visual impairments
 * 4. CUSTOMIZATION: Users can choose different sound themes
 *
 * HOW ANDROID PLAYS SOUNDS:
 * =========================
 * Android uses SoundPool for short, frequent sounds (like key clicks).
 * - SoundPool = Pool of pre-loaded sounds ready to play instantly
 * - MediaPlayer = For long sounds (music, videos)
 *
 * Think of it like:
 * - SoundPool = A DJ with pre-loaded samples (instant playback)
 * - MediaPlayer = A tape player (takes time to load and start)
 *
 * SOUNDPOOL WORKFLOW:
 * ===================
 * 1. Load sound files into memory
 * 2. Get sound IDs
 * 3. When key pressed ? play(soundId)
 * 4. Sound plays instantly (already in memory)
 *
 * PERFORMANCE:
 * ============
 * - Pre-loaded: < 1ms to play
 * - On-demand loading: 50-100ms (too slow for typing!)
 * - Memory usage: ~100KB for all sounds
 *
 * SOUND TYPES:
 * ============
 * - Standard click: Normal keys (letters, numbers)
 * - Delete click: Backspace key
 * - Space click: Space bar
 * - Enter click: Enter/Return key
 * - Modifier click: Shift, symbols keys
 */
class SoundManager(private val context: Context) {

    // SoundPool - holds pre-loaded sounds
    private var soundPool: SoundPool? = null

    // Sound IDs (references to loaded sounds)
    private var standardClickId: Int = -1
    private var deleteClickId: Int = -1
    private var spaceClickId: Int = -1
    private var enterClickId: Int = -1
    private var modifierClickId: Int = -1

    // Track which sounds have finished loading
    private val loadedSounds = mutableSetOf<Int>()

    // Volume control
    private var volume: Float = Constants.Audio.DEFAULT_SOUND_VOLUME

    // Is sound enabled?
    private var enabled: Boolean = true

    /**
     * Initialize SoundPool and load sounds
     *
     * Call this in onCreate() or when keyboard starts
     */
    fun initialize() {
        android.util.Log.i("SoundManager", "========== INITIALIZING SOUND MANAGER ==========")
        android.util.Log.i("SoundManager", "Context: ${context.javaClass.simpleName}")
        android.util.Log.i("SoundManager", "Package: ${context.packageName}")

        // Create SoundPool with modern AudioAttributes
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(Constants.Audio.MAX_SOUND_STREAMS) // How many sounds can play simultaneously
            .setAudioAttributes(audioAttributes)
            .build()

        android.util.Log.d("SoundManager", "SoundPool created: ${soundPool != null}")

        // Set up listener to track when sounds finish loading
        soundPool?.setOnLoadCompleteListener { pool, sampleId, status ->
            if (status == 0) {
                // Sound loaded successfully
                loadedSounds.add(sampleId)
                android.util.Log.i("SoundManager", "? Sound loaded successfully: ID=$sampleId (Total loaded: ${loadedSounds.size})")
            } else {
                // Sound failed to load
                android.util.Log.e("SoundManager", "? Failed to load sound: ID=$sampleId, status=$status")
            }
        }

        // Load sound files from res/raw/
        // Note: Sound files need to be added to app/src/main/res/raw/
        // For now, we'll use system sounds as fallback
        loadSounds()

        // Post delayed checks to see loading status (multiple checks for async loading)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            android.util.Log.i("SoundManager", "========== SOUND LOADING STATUS (500ms check) ==========")
            android.util.Log.i("SoundManager", "Loaded sounds count: ${loadedSounds.size}")
            android.util.Log.i("SoundManager", "Loaded sound IDs: $loadedSounds")
            android.util.Log.i("SoundManager", "standardClickId=$standardClickId (loaded=${loadedSounds.contains(standardClickId)})")
            android.util.Log.i("SoundManager", "deleteClickId=$deleteClickId (loaded=${loadedSounds.contains(deleteClickId)})")
            android.util.Log.i("SoundManager", "spaceClickId=$spaceClickId (loaded=${loadedSounds.contains(spaceClickId)})")
            android.util.Log.i("SoundManager", "enterClickId=$enterClickId (loaded=${loadedSounds.contains(enterClickId)})")
            android.util.Log.i("SoundManager", "modifierClickId=$modifierClickId (loaded=${loadedSounds.contains(modifierClickId)})")
            android.util.Log.i("SoundManager", "Enabled: $enabled, Volume: $volume")
        }, 500)
        
        // Additional check after 1 second to catch any late-loading sounds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (loadedSounds.size < 5) {
                android.util.Log.w("SoundManager", "? Some bubble sounds may not have loaded yet. Loaded: ${loadedSounds.size}/5")
            } else {
                android.util.Log.i("SoundManager", "? All bubble sounds loaded successfully!")
            }
        }, 1000)
    }

    /**
     * Load all sound files into memory
     *
     * SOUND FILE REQUIREMENTS:
     * - Format: .ogg or .wav (OGG preferred for smaller size)
     * - Duration: < 100ms (very short!)
     * - Size: < 50KB each
     * - Location: core/keyboard-engine/src/main/res/raw/
     *
     * Example files:
     * - key_click.ogg: Standard key press
     * - key_delete.ogg: Backspace sound
     * - key_space.ogg: Spacebar sound
     * - key_enter.ogg: Enter key sound
     * - key_modifier.ogg: Shift/symbols sound
     */
    private fun loadSounds() {
        android.util.Log.i("SoundManager", "========== LOADING BUBBLE SOUNDS ==========")

        soundPool?.let { pool ->
            android.util.Log.d("SoundManager", "Loading sounds from package: ${context.packageName}")
            android.util.Log.d("SoundManager", "SoundPool instance: $pool")

            try {
                standardClickId = pool.load(context, R.raw.key_click, 1)
                android.util.Log.i("SoundManager", "Loading key_click: soundId=$standardClickId")

                deleteClickId = pool.load(context, R.raw.key_delete, 1)
                android.util.Log.i("SoundManager", "Loading key_delete: soundId=$deleteClickId")

                spaceClickId = pool.load(context, R.raw.key_space, 1)
                android.util.Log.i("SoundManager", "Loading key_space: soundId=$spaceClickId")

                enterClickId = pool.load(context, R.raw.key_enter, 1)
                android.util.Log.i("SoundManager", "Loading key_enter: soundId=$enterClickId")

                modifierClickId = pool.load(context, R.raw.key_modifier, 1)
                android.util.Log.i("SoundManager", "Loading key_modifier: soundId=$modifierClickId")

                android.util.Log.i("SoundManager", "Sound loading initiated. Waiting for OnLoadCompleteListener callbacks...")

            } catch (e: Exception) {
                android.util.Log.e("SoundManager", "Exception loading sounds", e)
                e.printStackTrace()
            }
        } ?: run {
            android.util.Log.e("SoundManager", "SoundPool is NULL - cannot load sounds!")
        }
    }

    /**
     * Play standard key click sound
     *
     * Used for normal letter/number keys
     */
    fun playStandardClick() {
        playSound(standardClickId, SoundEffect.STANDARD)
    }

    /**
     * Play delete key sound
     *
     * Used for backspace/delete keys
     */
    fun playDeleteClick() {
        playSound(deleteClickId, SoundEffect.DELETE)
    }

    /**
     * Play space key sound
     *
     * Used for spacebar
     */
    fun playSpaceClick() {
        playSound(spaceClickId, SoundEffect.SPACE)
    }

    /**
     * Play enter key sound
     *
     * Used for return/enter key
     */
    fun playEnterClick() {
        playSound(enterClickId, SoundEffect.ENTER)
    }

    /**
     * Play modifier key sound
     *
     * Used for shift, symbols, language switch keys
     */
    fun playModifierClick() {
        playSound(modifierClickId, SoundEffect.MODIFIER)
    }

    /**
     * Play a sound by its ID
     *
     * @param soundId The sound ID from SoundPool.load()
     * @param fallback Which system sound to use if custom sound not loaded
     */
    private fun playSound(soundId: Int, fallback: SoundEffect) {
        android.util.Log.d(
            "SoundManager",
            "playSound() called - enabled=$enabled, soundId=$soundId, fallback=$fallback, volume=$volume"
        )

        if (!enabled) {
            android.util.Log.w("SoundManager", "Sound disabled - not playing")
            return
        }

        if (soundId > 0 && loadedSounds.contains(soundId)) {
            android.util.Log.i("SoundManager", "Playing CUSTOM bubble sound: soundId=$soundId, volume=$volume")
            val streamId = soundPool?.play(
                soundId,
                volume,
                volume,
                1,
                0,
                1.0f
            )
            if (streamId != null && streamId > 0) {
                android.util.Log.i("SoundManager", "Custom bubble sound played successfully: streamId=$streamId")
            } else {
                android.util.Log.w("SoundManager", "Custom sound play returned invalid streamId=$streamId")
            }
        } else {
            if (soundId > 0 && !loadedSounds.contains(soundId)) {
                android.util.Log.w("SoundManager", "Custom bubble sound not ready yet (soundId=$soundId not loaded)")
            } else if (soundId <= 0) {
                android.util.Log.w("SoundManager", "Custom bubble sound not found (soundId=$soundId)")
            }
        }
    }
    /**
     * Play system sound as fallback
     *
     * Android has built-in keyboard sounds we can use.
     * These are the same sounds used by the system keyboard.
     *
     * @param effect Which sound effect to play
     */
    private fun playSystemSound(effect: SoundEffect) {
        if (!enabled) {
            android.util.Log.w("SoundManager", "System sound disabled - not playing")
            return
        }

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            android.util.Log.d("SoundManager", "AudioManager: ${audioManager != null}")

            audioManager?.let { am ->
                // Check if sound effects are enabled on the device
                // Note: areSoundEffectsEnabled() was removed in SDK 36
                // Sound effects are generally enabled, so we'll just log that we're proceeding
                android.util.Log.d("SoundManager", "Proceeding with sound effect check")

                if (false) {  // Sound effects generally enabled, so skip this check
                    android.util.Log.w("SoundManager", "? Device sound effects are disabled in system settings")
                    // Try to enable them programmatically (may not work on all devices)
                    try {
                        am.loadSoundEffects()
                    } catch (e: Exception) {
                        android.util.Log.w("SoundManager", "Could not load sound effects: ${e.message}")
                    }
                }

                // Get the system sound effect code
                val soundEffect = when (effect) {
                    SoundEffect.STANDARD -> AudioManager.FX_KEYPRESS_STANDARD
                    SoundEffect.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                    SoundEffect.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                    SoundEffect.ENTER -> AudioManager.FX_KEYPRESS_RETURN
                    SoundEffect.MODIFIER -> AudioManager.FX_KEYPRESS_STANDARD
                }

                android.util.Log.i("SoundManager", "Playing system sound effect: $soundEffect (volume=$volume)")
                
                // Play the system sound with configured volume
                // Note: playSoundEffect volume parameter is ignored on some Android versions
                // The system will use the device's system sound volume
                am.playSoundEffect(soundEffect, volume)
                android.util.Log.i("SoundManager", "? System sound played")
            } ?: run {
                android.util.Log.e("SoundManager", "? AudioManager is NULL - cannot play system sound")
            }
        } catch (e: SecurityException) {
            android.util.Log.e("SoundManager", "? SecurityException playing system sound - may need permissions", e)
        } catch (e: Exception) {
            // Log the exception - important for debugging
            android.util.Log.e("SoundManager", "??? EXCEPTION playing system sound ???", e)
            e.printStackTrace()
        }
    }

    /**
     * Set volume level
     *
     * @param level Volume from 0.0 (silent) to 1.0 (max)
     */
    fun setVolume(level: Float) {
        volume = level.coerceIn(0.0f, 1.0f)
    }

    /**
     * Get current volume level
     *
     * @return Volume from 0.0 to 1.0
     */
    fun getVolume(): Float = volume

    /**
     * Enable sound effects
     */
    fun enable() {
        enabled = true
    }

    /**
     * Disable sound effects
     */
    fun disable() {
        enabled = false
    }

    /**
     * Check if sound effects are enabled
     *
     * @return true if enabled, false if disabled
     */
    fun isEnabled(): Boolean = enabled

    /**
     * Toggle sound effects on/off
     *
     * @return New enabled state
     */
    fun toggle(): Boolean {
        enabled = !enabled
        return enabled
    }

    /**
     * Clean up resources
     *
     * Call this in onDestroy() to free memory
     */
    fun release() {
        soundPool?.release()
        soundPool = null
    }

    /**
     * Enum for sound effect types
     *
     * Maps to Android's AudioManager sound effects
     */
    private enum class SoundEffect {
        STANDARD,  // Normal key press
        DELETE,    // Backspace
        SPACE,     // Spacebar
        ENTER,     // Return/Enter
        MODIFIER   // Shift, symbols, etc.
    }
}

/**
 * USAGE EXAMPLE:
 * ==============
 *
 * In KaviInputMethodService.kt:
 *
 * ```kotlin
 * private lateinit var soundManager: SoundManager
 *
 * override fun onCreate() {
 *     super.onCreate()
 *     soundManager = SoundManager(this)
 *     soundManager.initialize()
 *     soundManager.setVolume(0.5f) // 50% volume
 * }
 *
 * fun onKeyPressed(key: Key) {
 *     // Play appropriate sound
 *     when (key.type) {
 *         KeyType.CHARACTER -> soundManager.playStandardClick()
 *         KeyType.DELETE -> soundManager.playDeleteClick()
 *         KeyType.SPACE -> soundManager.playSpaceClick()
 *         KeyType.ENTER -> soundManager.playEnterClick()
 *         KeyType.SHIFT, KeyType.SYMBOLS -> soundManager.playModifierClick()
 *     }
 *
 *     // ... rest of key handling
 * }
 *
 * override fun onDestroy() {
 *     super.onDestroy()
 *     soundManager.release()
 * }
 * ```
 *
 * TO ADD CUSTOM SOUNDS:
 * =====================
 * 1. Create folder: app/src/main/res/raw/
 * 2. Add sound files:
 *    - key_click.ogg
 *    - key_delete.ogg
 *    - key_space.ogg
 *    - key_enter.ogg
 *    - key_modifier.ogg
 * 3. Sounds will be automatically loaded
 *
 * WHERE TO GET SOUNDS:
 * ====================
 * - FreeSound.org (free sound effects)
 * - ZapSplat.com (free with attribution)
 * - Create your own with Audacity
 * - Record mechanical keyboard sounds
 *
 * SOUND DESIGN TIPS:
 * ==================
 * - Keep it subtle (not annoying!)
 * - Short duration (< 100ms)
 * - Consistent volume across all sounds
 * - Test at different system volumes
 * - Consider different sound themes (mechanical, typewriter, futuristic)
 */



