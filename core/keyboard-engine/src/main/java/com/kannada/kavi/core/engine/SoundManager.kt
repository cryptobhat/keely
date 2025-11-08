package com.kannada.kavi.core.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.kannada.kavi.core.common.Constants

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
 * 3. When key pressed → play(soundId)
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

    // Volume control
    private var volume: Float = 0.5f // 0.0 to 1.0 (default 50%)

    // Is sound enabled?
    private var enabled: Boolean = true

    /**
     * Initialize SoundPool and load sounds
     *
     * Call this in onCreate() or when keyboard starts
     */
    fun initialize() {
        // Create SoundPool with modern AudioAttributes
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(Constants.Audio.MAX_SOUND_STREAMS) // How many sounds can play simultaneously
            .setAudioAttributes(audioAttributes)
            .build()

        // Load sound files from res/raw/
        // Note: Sound files need to be added to app/src/main/res/raw/
        // For now, we'll use system sounds as fallback
        loadSounds()
    }

    /**
     * Load all sound files into memory
     *
     * SOUND FILE REQUIREMENTS:
     * - Format: .ogg or .wav (OGG preferred for smaller size)
     * - Duration: < 100ms (very short!)
     * - Size: < 50KB each
     * - Location: app/src/main/res/raw/
     *
     * Example files:
     * - key_click.ogg: Standard key press
     * - key_delete.ogg: Backspace sound
     * - key_space.ogg: Spacebar sound
     * - key_enter.ogg: Enter key sound
     * - key_modifier.ogg: Shift/symbols sound
     */
    private fun loadSounds() {
        soundPool?.let { pool ->
            // Try to load custom sounds from res/raw/
            // If not found, sound IDs will be -1 and we'll use system sounds

            try {
                // Load standard click sound
                val standardResId = context.resources.getIdentifier(
                    "key_click", "raw", context.packageName
                )
                if (standardResId != 0) {
                    standardClickId = pool.load(context, standardResId, 1)
                }

                // Load delete sound
                val deleteResId = context.resources.getIdentifier(
                    "key_delete", "raw", context.packageName
                )
                if (deleteResId != 0) {
                    deleteClickId = pool.load(context, deleteResId, 1)
                }

                // Load space sound
                val spaceResId = context.resources.getIdentifier(
                    "key_space", "raw", context.packageName
                )
                if (spaceResId != 0) {
                    spaceClickId = pool.load(context, spaceResId, 1)
                }

                // Load enter sound
                val enterResId = context.resources.getIdentifier(
                    "key_enter", "raw", context.packageName
                )
                if (enterResId != 0) {
                    enterClickId = pool.load(context, enterResId, 1)
                }

                // Load modifier sound
                val modifierResId = context.resources.getIdentifier(
                    "key_modifier", "raw", context.packageName
                )
                if (modifierResId != 0) {
                    modifierClickId = pool.load(context, modifierResId, 1)
                }

            } catch (e: Exception) {
                // Failed to load sounds - will use system sounds as fallback
                e.printStackTrace()
            }
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
        if (!enabled) return

        if (soundId > 0) {
            // Play custom sound
            soundPool?.play(
                soundId,
                volume,    // Left volume
                volume,    // Right volume
                1,         // Priority
                0,         // Loop (0 = no loop)
                1.0f       // Playback rate (1.0 = normal speed)
            )
        } else {
            // Fallback to system sound
            playSystemSound(fallback)
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
        if (!enabled) return

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                // Get the system sound effect code
                val soundEffect = when (effect) {
                    SoundEffect.STANDARD -> AudioManager.FX_KEYPRESS_STANDARD
                    SoundEffect.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                    SoundEffect.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                    SoundEffect.ENTER -> AudioManager.FX_KEYPRESS_RETURN
                    SoundEffect.MODIFIER -> AudioManager.FX_KEYPRESS_STANDARD
                }

                // Play the system sound
                // Volume is controlled by user's media volume
                am.playSoundEffect(soundEffect, volume)
            }
        } catch (e: Exception) {
            // Silently fail - sound is not critical
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
