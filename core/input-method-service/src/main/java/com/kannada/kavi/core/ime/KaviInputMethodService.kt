package com.kannada.kavi.core.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.os.SystemClock
import com.kannada.kavi.core.layout.LayoutManager
import com.kannada.kavi.core.layout.models.KeyType
import com.kannada.kavi.core.layout.models.LayerName
import com.kannada.kavi.core.engine.SoundManager
import com.kannada.kavi.core.engine.VibrationManager
import com.kannada.kavi.features.suggestion.SuggestionEngine
import com.kannada.kavi.features.clipboard.ClipboardManager
import com.kannada.kavi.ui.keyboardview.KeyboardView
import com.kannada.kavi.ui.keyboardview.SuggestionStripView
import com.kannada.kavi.ui.keyboardview.ClipboardPopupView
import com.kannada.kavi.ui.popupviews.EmojiBoardView
// Design system removed
import android.widget.PopupWindow
import com.kannada.kavi.features.themes.DynamicThemeManager
import com.kannada.kavi.features.themes.KeyboardDesignSystem
import android.view.Gravity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.kannada.kavi.features.suggestion.transliteration.TransliterationEngine
import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.core.common.Result
import com.kannada.kavi.data.preferences.KeyboardPreferences
import com.kannada.kavi.features.voice.api.BhashiniApiClient
import com.kannada.kavi.features.voice.manager.VoiceInputManager
import com.kannada.kavi.features.voice.recorder.AudioRecorder
import com.kannada.kavi.features.voice.util.ApiKeyManager
import com.kannada.kavi.features.analytics.AnalyticsManager
import kotlin.jvm.Volatile

/**
 * KaviInputMethodService - The Heart of Our Keyboard
 *
 * This is the main Android service that powers our keyboard.
 * Think of it as the conductor of an orchestra - it coordinates everything:
 * - When to show/hide the keyboard
 * - What keys to display
 * - Where to send typed text
 * - How to handle special keys (delete, enter, etc.)
 *
 * WHAT IS AN INPUT METHOD SERVICE (IME)?
 * ======================================
 * In Android, a keyboard is called an "Input Method".
 * When you tap a text field, Android asks "which keyboard should I show?"
 * Our InputMethodService answers that question and provides the keyboard UI.
 *
 * HOW IT WORKS:
 * =============
 * 1. User taps a text field in any app (WhatsApp, Chrome, Notes, etc.)
 * 2. Android calls our onCreateInputView() to create the keyboard UI
 * 3. We show our custom keyboard view
 * 4. User types keys
 * 5. We send characters to the text field using InputConnection
 * 6. User is done, keyboard hides
 * 7. Android calls onDestroy() to clean up
 *
 * LIFECYCLE:
 * ==========
 * onCreate() → onCreateInputView() → onStartInput() → [User types] →
 * onFinishInput() → onDestroy()
 */
class KaviInputMethodService : InputMethodService() {

    // LayoutManager handles keyboard layouts (Phonetic, Kavi, QWERTY)
    private lateinit var layoutManager: LayoutManager

    // InputConnectionHandler sends text to the app
    private lateinit var inputConnectionHandler: InputConnectionHandler

    // SoundManager handles key press sound effects
    private lateinit var soundManager: SoundManager

    // VibrationManager for haptic feedback
    private lateinit var vibrationManager: VibrationManager

    // Preferences for settings
    private lateinit var preferences: KeyboardPreferences

    // Preference change listener for dynamic updates
    private val preferenceChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "keyboard_height" -> {
                // Update keyboard height dynamically
                val newHeight = preferences.getKeyboardHeightPercentage()
                keyboardView?.setKeyboardHeightPercentage(newHeight)
            }
            "key_press_sound" -> {
                // Update sound setting
                if (preferences.isKeyPressSoundEnabled()) {
                    soundManager.enable()
                } else {
                    soundManager.disable()
                }
            }
            "key_press_vibration" -> {
                // Update vibration setting
                if (preferences.isKeyPressVibrationEnabled()) {
                    vibrationManager.enable()
                } else {
                    vibrationManager.disable()
                }
            }
            "dynamic_theme" -> {
                // Update dynamic color setting
                val isDynamicEnabled = preferences.isDynamicThemeEnabled()
                dynamicThemeManager.setDynamicColorEnabled(isDynamicEnabled)
                
                // Track theme change for analytics
                if (::analyticsManager.isInitialized) {
                    val isDark = dynamicThemeManager.isDarkMode.value
                    analyticsManager.trackThemeChanged("dynamic", isDark, isDynamicEnabled)
                }
                
                // Force update color scheme to apply changes immediately
                serviceScope.launch(Dispatchers.Main) {
                    dynamicThemeManager.updateColorSchemeAsync()
                }
            }
        }
    }

    // SuggestionEngine provides word predictions
    private lateinit var suggestionEngine: SuggestionEngine

    // ClipboardManager handles clipboard history
    private lateinit var clipboardManager: ClipboardManager

    // VoiceInputManager handles voice input using Bhashini API
    private var voiceInputManager: VoiceInputManager? = null

    // Coroutine scope for async operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // The keyboard view that will be displayed
    private var keyboardView: KeyboardView? = null

    // The suggestion strip view above the keyboard
    private var suggestionStripView: SuggestionStripView? = null

    // Clipboard popup window
    private var clipboardPopup: PopupWindow? = null

    // Emoji board view
    private var emojiBoardView: EmojiBoardView? = null
    private var isEmojiBoardVisible = false

    // Dynamic theme manager for Material You support
    private lateinit var dynamicThemeManager: DynamicThemeManager

    // Analytics manager for tracking events
    private lateinit var analyticsManager: AnalyticsManager

    // Transliteration engine for phonetic buffering
    private lateinit var transliterationEngine: TransliterationEngine
    private val phoneticRomanBuffer = StringBuilder()
    private var lastInsertedKannadaLength: Int = 0
    @Volatile private var isTransliterationReady = false

    // Debounce job for suggestions
    private var suggestionsJob: Job? = null
    private var layoutRowsJob: Job? = null
    private var layoutInfoJob: Job? = null
    private var suggestionStreamJob: Job? = null

    // Track recent IME-driven edits so cursor moves from user can be detected
    private var lastInternalEditTimestamp: Long = 0L
    
    // Session tracking for analytics
    private var sessionStartTime: Long = 0L
    private var sessionKeyPressCount: Int = 0

    private fun isPhoneticLayoutActive(): Boolean {
        return ::layoutManager.isInitialized &&
            layoutManager.activeLayout.value?.id == Constants.Layouts.LAYOUT_PHONETIC
    }

    private fun resetPhoneticState() {
        phoneticRomanBuffer.clear()
        lastInsertedKannadaLength = 0
    }

    private fun markInternalEdit() {
        lastInternalEditTimestamp = SystemClock.uptimeMillis()
    }

    /**
     * Finalize the current phonetic buffer and optionally append trailing text (space, etc.)
     *
     * @return true if the buffer was processed, false if nothing was done
     */
    private fun finalizePhoneticBuffer(trailingText: String? = null): Boolean {
        if (!isPhoneticLayoutActive() || phoneticRomanBuffer.isEmpty()) {
            return false
        }

        val roman = phoneticRomanBuffer.toString()
        val trailing = trailingText ?: ""

        val commitAsTyped = {
            inputConnectionHandler.performBatchEdit {
                if (lastInsertedKannadaLength > 0) {
                    inputConnectionHandler.deleteText(lastInsertedKannadaLength)
                }
                inputConnectionHandler.commitText(roman + trailing)
            }
            markInternalEdit()
            resetPhoneticState()
            true
        }

        if (!isTransliterationReady) {
            return commitAsTyped()
        }

        return try {
            val kannada = transliterationEngine.transliterate(roman)
            inputConnectionHandler.performBatchEdit {
                if (lastInsertedKannadaLength > 0) {
                    inputConnectionHandler.deleteText(lastInsertedKannadaLength)
                }
                inputConnectionHandler.commitText(kannada)
                if (trailingText != null) {
                    inputConnectionHandler.commitText(trailingText)
                }
            }
            markInternalEdit()
            resetPhoneticState()
            true
        } catch (e: Exception) {
            commitAsTyped()
        }
    }

    /**
     * onCreate - Called when the service is first created
     *
     * This happens once when Android starts our keyboard service.
     * Like turning on a computer - happens once at startup.
     */
    override fun onCreate() {
        super.onCreate()

        // Initialize preferences first (needed by other components)
        preferences = KeyboardPreferences(this)

        // Register preference change listener for dynamic updates
        preferences.registerChangeListener(preferenceChangeListener)

        // Initialize the layout manager
        layoutManager = LayoutManager(this)

        // Initialize input connection handler
        inputConnectionHandler = InputConnectionHandler()

        // Initialize sound manager and apply preferences
        soundManager = SoundManager(this)
        soundManager.initialize()
        if (preferences.isKeyPressSoundEnabled()) {
            soundManager.enable()
        } else {
            soundManager.disable()
        }

        // Initialize vibration manager and apply preferences
        vibrationManager = VibrationManager(this)
        if (preferences.isKeyPressVibrationEnabled()) {
            vibrationManager.enable()
        } else {
            vibrationManager.disable()
        }

        // Initialize suggestion engine
        suggestionEngine = SuggestionEngine(this)
        suggestionEngine.initialize()

        // Initialize clipboard manager
        clipboardManager = ClipboardManager(this)

        // Initialize analytics manager
        analyticsManager = AnalyticsManager.getInstance(this)

        // Initialize voice input manager (if API key is available)
        initializeVoiceInputManager()

        // Initialize dynamic theme manager for Material You support
        dynamicThemeManager = DynamicThemeManager(this)

        // Load dynamic color setting from preferences
        val isDynamicEnabled = preferences.isDynamicThemeEnabled()
        dynamicThemeManager.setDynamicColorEnabled(isDynamicEnabled)
        
        // Force initial color scheme update
        serviceScope.launch(Dispatchers.Main) {
            try {
                dynamicThemeManager.updateColorSchemeAsync()
            } catch (e: Exception) {
                // Fallback to static colors if dynamic colors fail
                if (::analyticsManager.isInitialized) {
                    analyticsManager.trackError("dynamic_theme_init", "Failed to initialize dynamic theme", e)
                }
                e.printStackTrace()
            }
        }

        // Observe dynamic color scheme changes and update keyboard
        serviceScope.launch(Dispatchers.Main) {
            try {
                dynamicThemeManager.keyboardColorScheme.collect { colorScheme ->
                    colorScheme?.let {
                        // Update design system with new colors
                        KeyboardDesignSystem.setDynamicColorScheme(it)
                        // Refresh keyboard view colors (if view exists)
                        keyboardView?.refreshColors()
                        // Refresh emoji board colors
                        emojiBoardView?.refreshColors()
                    }
                }
            } catch (e: Exception) {
                // If theme updates fail, just continue with static colors
                // This prevents crashes
                if (::analyticsManager.isInitialized) {
                    analyticsManager.trackError("theme_update", "Failed to update theme colors", e)
                }
                e.printStackTrace()
            }
        }

        // Initialize transliteration engine (used for phonetic inline typing)
        transliterationEngine = TransliterationEngine(this)
        serviceScope.launch(Dispatchers.IO) {
            try {
                val result = transliterationEngine.initialize()
                isTransliterationReady = result is Result.Success
                if (result is Result.Error && ::analyticsManager.isInitialized) {
                    analyticsManager.trackError("transliteration_init", "Failed to initialize transliteration engine", result.exception)
                }
            } catch (e: Exception) {
                if (::analyticsManager.isInitialized) {
                    analyticsManager.trackError("transliteration_init", "Exception during transliteration initialization", e)
                }
                isTransliterationReady = false
            }
        }

        // Load all keyboard layouts asynchronously
        serviceScope.launch {
            layoutManager.initialize()
        }
    }

    /**
     * onCreateInputView - Create the keyboard UI
     *
     * Android calls this to ask: "What should the keyboard look like?"
     * We return our custom keyboard view here.
     *
     * Think of it like Android asking "Show me your keyboard!"
     * and we hand over our beautiful custom keyboard UI.
     *
     * @return The View that represents our keyboard
     */
    override fun onCreateInputView(): View {
        // Create container to hold suggestion strip + keyboard
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF5F5F5.toInt())  // Light gray background
            val density = resources.displayMetrics.density
            setPadding(0, 0, 0, (8 * density).toInt()) // 8dp bottom padding like Gboard
        }

        // Create suggestion strip with top margin for gap
        val stripView = SuggestionStripView(this).apply {
            setOnSuggestionClickListener { suggestion ->
                onSuggestionClicked(suggestion)
            }
        }
        suggestionStripView = stripView
        
        // Add margin layout params to create gap before suggestion bar
        val stripLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            val density = resources.displayMetrics.density
            topMargin = (8 * density).toInt() // 8dp gap before suggestion bar
        }
        container.addView(stripView, stripLayoutParams)

        // Add separator between suggestion bar and keyboard
        val separatorView = View(this).apply {
            val density = resources.displayMetrics.density
            val separatorHeight = (1 * density).toInt() // 1dp separator height
            val separatorMargin = (4 * density).toInt() // 4dp margin above separator
            
            setBackgroundColor(0xFFE0E0E0.toInt()) // Light gray separator
            
            val separatorParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                separatorHeight
            ).apply {
                topMargin = separatorMargin
            }
            layoutParams = separatorParams
        }
        container.addView(separatorView)

        // Create emoji board view (initially hidden)
        val emojiBoard = EmojiBoardView(this).apply {
            visibility = View.GONE
            setOnEmojiSelectedListener { emoji ->
                onEmojiSelected(emoji)
            }
        }
        emojiBoardView = emojiBoard
        val emojiLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (200 * resources.displayMetrics.density).toInt() // 200dp height
        )
        container.addView(emojiBoard, emojiLayoutParams)

        // Create keyboard view
        val view = KeyboardView(this).apply {
            setOnKeyPressListener { key ->
                handleKeyPress(key)
            }
            setBackgroundColor(0xFFF5F5F5.toInt())  // Light gray background

            // Apply keyboard height adjustment from preferences
            val heightPercentage = preferences.getKeyboardHeightPercentage()
            setKeyboardHeightPercentage(heightPercentage)

            // Configure swipe typing and gestures
            val swipeEnabled = preferences.isSwipeTypingEnabled()
            val gesturesEnabled = preferences.isGesturesEnabled()
            setSwipeTypingEnabled(swipeEnabled)
            setGesturesEnabled(gesturesEnabled)

            // Set callback for swipe words
            setOnSwipeWordListener { word ->
                // Insert the swiped word
                currentInputConnection?.commitText(word, 1)
                // Add space after word
                currentInputConnection?.commitText(" ", 1)
            }
        }
        keyboardView = view

        // Create a FrameLayout to hold keyboard + swipe path overlay
        val keyboardContainer = android.widget.FrameLayout(this).apply {
            addView(view, android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            ))

            // Add swipe path view as overlay (if swipe typing is enabled)
            if (preferences.isSwipeTypingEnabled() && preferences.isSwipePathVisible()) {
                val swipePathView = com.kannada.kavi.ui.keyboardview.SwipePathView(this@KaviInputMethodService)
                addView(swipePathView, android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                ))
                view.setSwipePathView(swipePathView)
            }
        }

        container.addView(keyboardContainer)
        
        // Now that we have a window, try to update dynamic colors
        // This is the best time since we have a proper window context
        // Use the container's context which has the window
        if (preferences.isDynamicThemeEnabled()) {
            dynamicThemeManager.updateColorSchemeWithWindow(container.context)
        }

        layoutRowsJob?.cancel()
        layoutInfoJob?.cancel()
        suggestionStreamJob?.cancel()

        // DIRECT CALLBACK - Bypass StateFlow for immediate updates
        layoutManager.onLayoutChanged = { layout, rows ->
            android.util.Log.e("KaviIME", "===== DIRECT CALLBACK RECEIVED: ${layout.id}, ${rows.size} rows =====")
            serviceScope.launch(Dispatchers.Main) {
                android.util.Log.e("KaviIME", "===== UPDATING VIEW ON MAIN THREAD =====")
                keyboardView?.setKeyboard(rows)
                val layoutName = layout.name.split(" ").firstOrNull() ?: layout.name
                keyboardView?.setLayoutName(layoutName)
                android.util.Log.e("KaviIME", "===== VIEW UPDATE COMPLETE =====")
            }
        }

        // Observe layout changes and update keyboard view
        // Use Dispatchers.Main to ensure UI updates happen on main thread
        layoutRowsJob = layoutManager.currentRows
            .onEach { rows ->
                android.util.Log.d("KaviIME", "Flow: currentRows changed, ${rows.size} rows received")
                keyboardView?.setKeyboard(rows)
            }
            .launchIn(serviceScope)

        // Observe layout changes and update spacebar label
        layoutInfoJob = layoutManager.activeLayout
            .onEach { layout ->
                android.util.Log.d("KaviIME", "Flow: activeLayout changed to ${layout?.id}")
                layout?.let {
                    val layoutName = it.name.split(" ").firstOrNull() ?: it.name
                    keyboardView?.setLayoutName(layoutName)
                }
            }
            .launchIn(serviceScope)

        // Observe suggestions from engine and update strip
        suggestionStreamJob = suggestionEngine.suggestions.onEach { suggestions ->
            suggestionStripView?.setSuggestions(suggestions)
        }.launchIn(serviceScope)

        return container
    }

    /**
     * onStartInput - Called when user taps a text field
     *
     * This is called every time the user focuses a text field.
     * We can check what type of field it is (email, password, number, etc.)
     * and adjust the keyboard accordingly.
     *
     * @param attribute Information about the text field
     * @param restarting Is this a restart of the same field?
     */
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)

        // Reset keyboard state
        layoutManager.reset()

        // Reset phonetic buffer state
        resetPhoneticState()

        // Connect to the text field
        val ic = currentInputConnection
        if (ic != null) {
            inputConnectionHandler.setInputConnection(ic)
        }

        // Track session start
        sessionStartTime = System.currentTimeMillis()
        sessionKeyPressCount = 0
        if (::analyticsManager.isInitialized) {
            analyticsManager.trackKeyboardSessionStart()
        }

        // Check what type of field this is
        attribute?.let { info ->
            // Update enter key icon based on IME action
            val imeAction = info.imeOptions and EditorInfo.IME_MASK_ACTION
            keyboardView?.setEnterAction(imeAction)
            
            handleInputType(info)
        }
    }

    /**
     * onFinishInput - Called when user leaves the text field
     *
     * This is like saying goodbye to the text field.
     * We clean up and prepare for the next field.
     */
    override fun onFinishInput() {
        super.onFinishInput()

        // Disconnect from the text field
        inputConnectionHandler.clearInputConnection()

        // Clear phonetic state
        resetPhoneticState()

        // Track session end
        if (::analyticsManager.isInitialized && sessionStartTime > 0) {
            val sessionDuration = System.currentTimeMillis() - sessionStartTime
            analyticsManager.trackKeyboardSessionEnd(sessionDuration, sessionKeyPressCount)
            sessionStartTime = 0
            sessionKeyPressCount = 0
        }

        suggestionsJob?.cancel()
        suggestionsJob = null
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd
        )

        if (!isPhoneticLayoutActive() || phoneticRomanBuffer.isEmpty()) {
            return
        }

        val now = SystemClock.uptimeMillis()
        if (now - lastInternalEditTimestamp > INTERNAL_SELECTION_GRACE_MS) {
            resetPhoneticState()
        }
    }

    /**
     * onDestroy - Called when the service is destroyed
     *
     * This is like shutting down a computer - happens once.
     * We clean up all resources here.
     */
    override fun onDestroy() {
        super.onDestroy()

        // Unregister preference change listener
        preferences.unregisterChangeListener(preferenceChangeListener)

        // Unregister theme change receiver
        if (::dynamicThemeManager.isInitialized) {
            dynamicThemeManager.unregisterThemeChangeReceiver()
        }

        // Cancel all coroutines
        serviceScope.cancel()
        suggestionsJob?.cancel()
        layoutRowsJob?.cancel()
        layoutInfoJob?.cancel()
        suggestionStreamJob?.cancel()

        // Release sound resources
        soundManager.release()
        suggestionEngine.release()
        
        // Release voice input manager
        voiceInputManager?.release()
        voiceInputManager = null

        // Clean up keyboard view
        keyboardView = null
        clipboardPopup = null
        emojiBoardView = null
    }

    /**
     * Handle different input types
     *
     * Different text fields need different keyboards:
     * - Email field → show @ and .com
     * - Number field → show number pad
     * - Password field → hide suggestions
     * - URL field → show / and .com
     *
     * @param info Information about the text field
     */
    private fun handleInputType(info: EditorInfo) {
        val inputType = info.inputType

        // Extract the type category
        val variation = inputType and EditorInfo.TYPE_MASK_VARIATION
        val typeClass = inputType and EditorInfo.TYPE_MASK_CLASS

        when (typeClass) {
            EditorInfo.TYPE_CLASS_NUMBER -> {
                // Number field - could switch to number layout
                // layoutManager.switchToLayer("symbols")
            }

            EditorInfo.TYPE_CLASS_PHONE -> {
                // Phone number field
                // layoutManager.switchToLayer("symbols")
            }

            EditorInfo.TYPE_CLASS_DATETIME -> {
                // Date/time field
                // layoutManager.switchToLayer("symbols")
            }

            EditorInfo.TYPE_CLASS_TEXT -> {
                // Text field - check variation
                when (variation) {
                    EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> {
                        // Email field - might want to show @ easily
                    }

                    EditorInfo.TYPE_TEXT_VARIATION_URI -> {
                        // URL field - might want to show / easily
                    }

                    EditorInfo.TYPE_TEXT_VARIATION_PASSWORD -> {
                        // Password field - disable suggestions
                    }
                }
            }
        }
    }

    /**
     * Handle key press from KeyboardView
     * Routes the key to the appropriate handler based on its type
     *
     * @param key The key that was pressed
     */
    private fun handleKeyPress(key: com.kannada.kavi.core.layout.models.Key) {
        android.util.Log.d("KaviIME", "handleKeyPress: key.type=${key.type}, key.label='${key.label}'")

        // Track key press for analytics
        if (::analyticsManager.isInitialized) {
            val keyType = when (key.type) {
                KeyType.CHARACTER -> "character"
                KeyType.DELETE -> "delete"
                KeyType.ENTER -> "enter"
                KeyType.SPACE -> "space"
                KeyType.SHIFT -> "shift"
                KeyType.SYMBOLS -> "symbols"
                KeyType.EMOJI -> "emoji"
                KeyType.LANGUAGE -> "language"
                KeyType.CLIPBOARD -> "clipboard"
                else -> "other"
            }
            analyticsManager.trackKeyPress(keyType, if (key.type == KeyType.CHARACTER) key.output else null)
            sessionKeyPressCount++
        }

        // Play appropriate sound effect and vibration feedback
        when (key.type) {
            KeyType.CHARACTER -> {
                soundManager.playStandardClick()
                vibrationManager.vibrateStandardKey()
            }
            KeyType.DELETE -> {
                soundManager.playDeleteClick()
                vibrationManager.vibrateDeleteKey()
            }
            KeyType.ENTER -> {
                soundManager.playEnterClick()
                vibrationManager.vibrateEnterKey()
            }
            KeyType.SPACE -> {
                soundManager.playSpaceClick()
                vibrationManager.vibrateSpaceKey()
            }
            KeyType.SHIFT,
            KeyType.SYMBOLS,
            KeyType.SYMBOLS_EXTRA,
            KeyType.SYMBOLS_ALT,
            KeyType.DEFAULT,
            KeyType.LANGUAGE -> {
                soundManager.playModifierClick()
                vibrationManager.vibrateModifierKey()
            }
            else -> {
                soundManager.playStandardClick()
                vibrationManager.vibrateStandardKey()
            }
        }

        // Handle the key action
        when (key.type) {
            KeyType.CHARACTER -> onKeyPressed(key.output)
            KeyType.DELETE -> onDeletePressed()
            KeyType.ENTER -> onEnterPressed()
            KeyType.SPACE -> onSpacePressed()
            KeyType.SHIFT -> onShiftPressed()
            KeyType.SYMBOLS -> onSymbolsPressed()
            KeyType.SYMBOLS_EXTRA -> onSymbolsExtraPressed()
            KeyType.SYMBOLS_ALT -> onSymbolsAltPressed()
            KeyType.DEFAULT -> onDefaultPressed()
            KeyType.LANGUAGE -> onLanguagePressed()
            KeyType.EMOJI -> {
                onEmojiPressed()
            }
            KeyType.VOICE -> {
                onVoicePressed()
            }
            KeyType.SETTINGS -> {
                // TODO: Open settings
            }
            KeyType.CLIPBOARD -> onClipboardPressed()
        }
    }

    /**
     * Public function to handle key presses
     * This will be called by the KeyboardView when user taps a key
     *
     * @param keyOutput The character to type
     */
    private fun onKeyPressed(keyOutput: String) {
        val ic = currentInputConnection ?: return

        // If phonetic layout is active, buffer roman input and replace inline with Kannada
        val activeLayoutId = layoutManager.activeLayout.value?.id
        val isPhonetic = activeLayoutId == Constants.Layouts.LAYOUT_PHONETIC


        if (isPhonetic && isTransliterationReady && keyOutput.isNotEmpty()) {
            // Check if this is a letter that should be transliterated (a-z, A-Z)
            val isLetter = keyOutput.length == 1 && keyOutput[0].isLetter()

            if (isLetter) {
                // Treat letters as part of roman buffer for transliteration
                phoneticRomanBuffer.append(keyOutput)
                val roman = phoneticRomanBuffer.toString()
                try {
                    val kannada = transliterationEngine.transliterate(roman)
                    inputConnectionHandler.performBatchEdit {
                        if (lastInsertedKannadaLength > 0) {
                            inputConnectionHandler.deleteText(lastInsertedKannadaLength)
                        }
                        inputConnectionHandler.commitText(kannada)
                    }
                    markInternalEdit()
                    lastInsertedKannadaLength = kannada.length
                } catch (e: Exception) {
                    // Fallback to normal commit if engine not ready
                    if (::analyticsManager.isInitialized) {
                        analyticsManager.trackError("transliteration_error", "Failed to transliterate: $roman", e)
                    }
                    inputConnectionHandler.commitText(keyOutput)
                    markInternalEdit()
                    resetPhoneticState()
                }
            } else {
                // Non-letter character (space, punctuation, etc.) - finalize current word first
                if (phoneticRomanBuffer.isNotEmpty()) {
                    // Finalize the buffered word
                    val roman = phoneticRomanBuffer.toString()
                    try {
                        val kannada = transliterationEngine.transliterate(roman)
                        inputConnectionHandler.performBatchEdit {
                            if (lastInsertedKannadaLength > 0) {
                                inputConnectionHandler.deleteText(lastInsertedKannadaLength)
                            }
                            inputConnectionHandler.commitText(kannada)
                            // Then commit the non-letter character
                            inputConnectionHandler.commitText(keyOutput)
                        }
                        markInternalEdit()
                        resetPhoneticState()
                    } catch (e: Exception) {
                        // Fallback: commit buffer as-is, then the character
                        inputConnectionHandler.commitText(roman + keyOutput)
                        markInternalEdit()
                        resetPhoneticState()
                    }
                } else {
                    // No buffer, just commit the character
                    inputConnectionHandler.commitText(keyOutput)
                    markInternalEdit()
                    resetPhoneticState()
                }
            }
        } else {
            // Kavi layout or other layouts: commit character directly
            // For Kavi, Kannada characters are typed directly without transliteration
            // But we need to handle matra combining: consonant + matra should combine
            handleKaviKeyPress(keyOutput)
        }

        // Disable shift after typing (if not caps lock)
        layoutManager.disableShiftAfterInput()

        // Update suggestions based on current word (debounced)
        updateSuggestions()
    }

    /**
     * Handle key press for Kavi layout with matra combining support
     * When a matra or full vowel is typed after a consonant, they should combine properly
     * Example: ಹ + ಊ → ಹೂ, ಕ + ಆ → ಕಾ, ಕ + ಾ → ಕಾ
     */
    private fun handleKaviKeyPress(keyOutput: String) {
        if (keyOutput.isEmpty()) return
        
        val char = keyOutput[0]
        
        // Check if this is a Kannada matra (vowel sign) or full vowel
        // Kannada matras: 0CBE-0CCC (ಾ, ಿ, ೀ, ು, ೂ, ೃ, ೄ, ೆ, ೇ, ೈ, ೊ, ೋ, ೌ)
        // Kannada full vowels: 0C85-0C94 (ಅ, ಆ, ಇ, ಈ, ಉ, ಊ, ಋ, ೠ, ಎ, ಏ, ಐ, ಒ, ಓ, ಔ)
        val isMatra = char in '\u0CBE'..'\u0CCC'
        val isFullVowel = char in '\u0C85'..'\u0C94'
        
        if (isMatra || isFullVowel) {
            // Get the text before cursor to check if last char is a consonant
            val ic = currentInputConnection ?: return
            val textBeforeCursor = ic.getTextBeforeCursor(10, 0)?.toString() ?: ""
            
            if (textBeforeCursor.isNotEmpty()) {
                val lastChar = textBeforeCursor.last()
                // Check if last character is a Kannada consonant
                // Kannada consonants: 0C95-0CB9, 0C9E-0C9F, 0CA0-0CA1, 0CA3-0CA4, 0CA6-0CA7, 0CAA-0CAC, 0CAE-0CAF, 0CB0-0CB1, 0CB5-0CB9
                val isConsonant = (lastChar in '\u0C95'..'\u0CB9') || 
                                 (lastChar in '\u0C9E'..'\u0C9F') ||
                                 (lastChar == '\u0CA0' || lastChar == '\u0CA1') ||
                                 (lastChar == '\u0CA3' || lastChar == '\u0CA4') ||
                                 (lastChar == '\u0CA6' || lastChar == '\u0CA7') ||
                                 (lastChar in '\u0CAA'..'\u0CAC') ||
                                 (lastChar in '\u0CAE'..'\u0CAF') ||
                                 (lastChar == '\u0CB0' || lastChar == '\u0CB1') ||
                                 (lastChar in '\u0CB5'..'\u0CB9')
                
                if (isConsonant) {
                    // Convert full vowel to matra if needed, or use matra directly
                    val matra = if (isFullVowel) {
                        // Map full vowels to their matras
                        when (char) {
                            '\u0C86' -> '\u0CBE' // ಆ → ಾ
                            '\u0C87' -> '\u0CBF' // ಇ → ಿ
                            '\u0C88' -> '\u0CC0' // ಈ → ೀ
                            '\u0C89' -> '\u0CC1' // ಉ → ು
                            '\u0C8A' -> '\u0CC2' // ಊ → ೂ
                            '\u0C8B' -> '\u0CC3' // ಋ → ೃ
                            '\u0C8C' -> '\u0CC4' // ೠ → ೄ
                            '\u0C8E' -> '\u0CC6' // ಎ → ೆ
                            '\u0C8F' -> '\u0CC7' // ಏ → ೇ
                            '\u0C90' -> '\u0CC8' // ಐ → ೈ
                            '\u0C92' -> '\u0CCA' // ಒ → ೊ
                            '\u0C93' -> '\u0CCB' // ಓ → ೋ
                            '\u0C94' -> '\u0CCC' // ಔ → ೌ
                            else -> char // ಅ stays as ಅ (inherent vowel, no matra)
                        }
                    } else {
                        char // Already a matra
                    }
                    
                    // If we got a matra (not inherent vowel), combine with consonant
                    if (matra != '\u0C85') { // Not ಅ (inherent vowel)
                        inputConnectionHandler.performBatchEdit {
                            inputConnectionHandler.deleteText(1)
                            inputConnectionHandler.commitText("$lastChar$matra")
                        }
                        markInternalEdit()
                        resetPhoneticState()
                        return
                    }
                }
            }
        }
        
        // Not a matra/vowel, or no consonant before it - commit normally
        inputConnectionHandler.commitText(keyOutput)
        markInternalEdit()
        resetPhoneticState()
    }

    /**
     * Handle delete key press
     */
    fun onDeletePressed() {
        val activeLayoutId = layoutManager.activeLayout.value?.id
        val isPhonetic = activeLayoutId == Constants.Layouts.LAYOUT_PHONETIC

        if (isPhonetic && isTransliterationReady && phoneticRomanBuffer.isNotEmpty()) {
            // Remove last roman char and re-render Kannada inline
            phoneticRomanBuffer.deleteCharAt(phoneticRomanBuffer.length - 1)
            val roman = phoneticRomanBuffer.toString()
            try {
                val kannada = transliterationEngine.transliterate(roman)
                inputConnectionHandler.performBatchEdit {
                    if (lastInsertedKannadaLength > 0) {
                        inputConnectionHandler.deleteText(lastInsertedKannadaLength)
                    }
                    if (kannada.isNotEmpty()) {
                        inputConnectionHandler.commitText(kannada)
                        lastInsertedKannadaLength = kannada.length
                    } else {
                        lastInsertedKannadaLength = 0
                    }
                }
                markInternalEdit()
            } catch (e: Exception) {
                // Fallback to normal delete
                inputConnectionHandler.deleteText()
                markInternalEdit()
                resetPhoneticState()
            }
        } else {
            // For Kavi layout or when buffer is empty, delete normally
            inputConnectionHandler.deleteText()
            markInternalEdit()
            // If phonetic buffer exists but is empty, clear the tracking
            if (isPhonetic) {
                resetPhoneticState()
            }
        }

        // Update suggestions after deletion (debounced)
        updateSuggestions()
    }

    /**
     * Handle enter key press
     */
    fun onEnterPressed() {
        val ic = currentInputConnection ?: return

        // Finalize any pending phonetic buffer before performing actions
        finalizePhoneticBuffer()

        // Check if this is a "send" action or "new line"
        val editorInfo = currentInputEditorInfo
        val imeAction = editorInfo?.imeOptions ?: EditorInfo.IME_NULL

        when (imeAction and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_SEND -> {
                // Send button in messaging apps
                ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
            }

            EditorInfo.IME_ACTION_SEARCH -> {
                // Search button
                ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
            }

            EditorInfo.IME_ACTION_GO -> {
                // Go button in browsers
                ic.performEditorAction(EditorInfo.IME_ACTION_GO)
            }
            
            EditorInfo.IME_ACTION_NEXT -> {
                // Next button (move to next field)
                ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
            }
            
            EditorInfo.IME_ACTION_DONE -> {
                // Done button (close keyboard)
                ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
            }

            else -> {
                // Default: insert new line
                inputConnectionHandler.commitText("\n")
                markInternalEdit()
            }
        }

        resetPhoneticState()
    }

    /**
     * Handle shift key press
     */
    fun onShiftPressed() {
        layoutManager.toggleShift()
    }

    /**
     * Handle space key press
     */
    fun onSpacePressed() {
        val wordBeforeSpace = inputConnectionHandler.getCurrentWord()
        val handledByPhonetic = finalizePhoneticBuffer(trailingText = " ")
        if (!handledByPhonetic) {
            inputConnectionHandler.commitText(" ")
            markInternalEdit()
            resetPhoneticState()
        }

        layoutManager.disableShiftAfterInput()

        // Learn from the typed word
        if (wordBeforeSpace.isNotEmpty()) {
            suggestionEngine.onWordTyped(wordBeforeSpace)
        }

        // Clear suggestions
        suggestionStripView?.clear()
    }

    /**
     * Switch to symbols layer (cycles through symbol layers)
     */
    fun onSymbolsPressed() {
        android.util.Log.d("KaviIME", "onSymbolsPressed: Symbols button pressed!")

        // Clear any phonetic buffer before switching to symbols
        if (phoneticRomanBuffer.isNotEmpty()) {
            finalizePhoneticBuffer()
        }
        resetPhoneticState()

        android.util.Log.d("KaviIME", "onSymbolsPressed: Calling switchToSymbols...")
        layoutManager.switchToSymbols()
        android.util.Log.d("KaviIME", "onSymbolsPressed: After switch, active layer = ${layoutManager.activeLayer.value}")

        // Force keyboard view update
        val view = keyboardView
        if (view != null) {
            serviceScope.launch(Dispatchers.Main) {
                try {
                    val rows = layoutManager.currentRows.value
                    android.util.Log.d("KaviIME", "onSymbolsPressed: Updating view with ${rows.size} rows")
                    view.setKeyboard(rows)
                    view.invalidate()
                    view.requestLayout()
                    android.util.Log.d("KaviIME", "onSymbolsPressed: View updated successfully")
                } catch (e: Exception) {
                    android.util.Log.e("KaviIME", "onSymbolsPressed: Error updating view", e)
                    e.printStackTrace()
                }
            }
        } else {
            android.util.Log.w("KaviIME", "onSymbolsPressed: KeyboardView is null!")
        }
    }
    
    /**
     * Switch to symbols_extra layer
     */
    fun onSymbolsExtraPressed() {
        layoutManager.switchToLayer(LayerName.SYMBOLS_EXTRA)
    }
    
    /**
     * Switch to symbols_alt layer
     */
    fun onSymbolsAltPressed() {
        layoutManager.switchToLayer(LayerName.SYMBOLS_ALT)
    }

    /**
     * Switch back to default (ABC) layer
     */
    fun onDefaultPressed() {
        android.util.Log.d("KaviIME", "onDefaultPressed: ABC/Default button pressed!")

        // Check if layout manager is initialized
        if (!::layoutManager.isInitialized) {
            android.util.Log.e("KaviIME", "onDefaultPressed: Layout manager not initialized!")
            return
        }

        // Clear any phonetic buffer before switching to default
        if (phoneticRomanBuffer.isNotEmpty()) {
            finalizePhoneticBuffer()
        }
        resetPhoneticState()

        android.util.Log.d("KaviIME", "onDefaultPressed: Calling switchToDefault...")
        // Switch to default layer
        val result = layoutManager.switchToDefault()
        android.util.Log.d("KaviIME", "onDefaultPressed: After switch, active layer = ${layoutManager.activeLayer.value}, result = $result")

        // Force keyboard view update to ensure layer change is reflected
        val view = keyboardView
        if (view != null) {
            serviceScope.launch(Dispatchers.Main) {
                try {
                    // Directly update rows for immediate feedback
                    val rows = layoutManager.currentRows.value
                    android.util.Log.d("KaviIME", "onDefaultPressed: Updating view with ${rows.size} rows")
                    view.setKeyboard(rows)
                    view.invalidate()
                    view.requestLayout()
                    android.util.Log.d("KaviIME", "onDefaultPressed: View updated successfully")
                } catch (e: Exception) {
                    android.util.Log.e("KaviIME", "onDefaultPressed: Error updating view", e)
                    e.printStackTrace()
                    view.invalidate()
                }
            }
        } else {
            android.util.Log.w("KaviIME", "onDefaultPressed: KeyboardView is null!")
        }

        // If switching failed, try direct layer switch as fallback
        if (result is com.kannada.kavi.core.common.Result.Error) {
            android.util.Log.w("KaviIME", "onDefaultPressed: switchToDefault failed, trying direct layer switch")
            try {
                layoutManager.switchToLayer(LayerName.DEFAULT)
            } catch (e: Exception) {
                android.util.Log.e("KaviIME", "onDefaultPressed: Direct layer switch also failed", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * Switch to next keyboard layout (Phonetic → Kavi → QWERTY)
     * Properly updates the keyboard view when switching
     */
    fun onLanguagePressed() {
        android.util.Log.e("KaviIME", "========== LANGUAGE BUTTON PRESSED ==========")
        android.util.Log.d("KaviIME", "onLanguagePressed: Language button pressed!")

        // Clear any phonetic buffer before switching
        if (phoneticRomanBuffer.isNotEmpty()) {
            finalizePhoneticBuffer()
        }
        resetPhoneticState()

        // Check if layout manager is initialized
        if (!::layoutManager.isInitialized) {
            android.util.Log.e("KaviIME", "onLanguagePressed: Layout manager not initialized!")
            return
        }

        android.util.Log.d("KaviIME", "onLanguagePressed: Calling switchToNextLayout...")
        // Switch to next layout
        layoutManager.switchToNextLayout()

        android.util.Log.d("KaviIME", "onLanguagePressed: After switch, active layout = ${layoutManager.activeLayout.value?.id}")

        // Track layout switch for analytics
        layoutManager.activeLayout.value?.let { layout ->
            if (::analyticsManager.isInitialized) {
                analyticsManager.trackLayoutSwitch(layout.id, layout.name)
            }
        }

        // Force immediate update by directly getting current rows and updating view
        // This ensures the UI updates even if StateFlow hasn't emitted yet
        val view = keyboardView
        if (view != null) {
            serviceScope.launch(Dispatchers.Main) {
                try {
                    val rows = layoutManager.currentRows.value
                    android.util.Log.d("KaviIME", "onLanguagePressed: Updating view with ${rows.size} rows")
                    view.setKeyboard(rows)

                    val layout = layoutManager.activeLayout.value
                    layout?.let {
                        val layoutName = it.name.split(" ").firstOrNull() ?: it.name
                        android.util.Log.d("KaviIME", "onLanguagePressed: Setting layout name = $layoutName")
                        view.setLayoutName(layoutName)
                    }

                    // Force redraw
                    view.invalidate()
                    view.requestLayout()
                    android.util.Log.d("KaviIME", "onLanguagePressed: View updated successfully")
                } catch (e: Exception) {
                    // Silently handle any errors to prevent crashes
                    android.util.Log.e("KaviIME", "onLanguagePressed: Error updating view", e)
                    e.printStackTrace()
                }
            }
        } else {
            android.util.Log.w("KaviIME", "onLanguagePressed: KeyboardView is null!")
        }
    }
    
    /**
     * Handle emoji key press - toggle emoji board
     */
    private fun onEmojiPressed() {
        if (isEmojiBoardVisible) {
            // Hide emoji board, show keyboard
            hideEmojiBoard()
        } else {
            // Show emoji board, hide keyboard
            showEmojiBoard()
        }
    }
    
    /**
     * Show emoji board
     */
    private fun showEmojiBoard() {
        isEmojiBoardVisible = true
        emojiBoardView?.visibility = View.VISIBLE
        keyboardView?.visibility = View.GONE
        suggestionStripView?.visibility = View.GONE
        // Update emoji icon to show keyboard icon
        keyboardView?.setEmojiBoardVisible(true)
    }
    
    /**
     * Hide emoji board
     */
    private fun hideEmojiBoard() {
        isEmojiBoardVisible = false
        emojiBoardView?.visibility = View.GONE
        keyboardView?.visibility = View.VISIBLE
        suggestionStripView?.visibility = View.VISIBLE
        // Update emoji icon to show emoji icon
        keyboardView?.setEmojiBoardVisible(false)
    }
    
    /**
     * Handle emoji selection - insert emoji and hide board
     */
    private fun onEmojiSelected(emoji: String) {
        val ic = currentInputConnection ?: return

        finalizePhoneticBuffer()
        resetPhoneticState()

        // Insert emoji
        inputConnectionHandler.commitText(emoji)
        markInternalEdit()
        
        // Hide emoji board and show keyboard
        hideEmojiBoard()
    }
    
    /**
     * Handle back button - close emoji board if visible
     */
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK && isEmojiBoardVisible) {
            hideEmojiBoard()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * Get the current layout manager
     * Used by KeyboardView to get current keys
     */
    fun getLayoutManager(): LayoutManager {
        return layoutManager
    }

    /**
     * Handle suggestion click
     *
     * Called when user taps a suggestion in the strip
     *
     * @param suggestion The tapped suggestion
     */
    private fun onSuggestionClicked(suggestion: com.kannada.kavi.features.suggestion.models.Suggestion) {
        resetPhoneticState()

        // Get current word to delete it
        val currentWord = inputConnectionHandler.getCurrentWord()

        // Delete the current word
        if (currentWord.isNotEmpty()) {
            inputConnectionHandler.deleteText(currentWord.length)
            markInternalEdit()
        }

        // Insert the suggestion with a space
        inputConnectionHandler.commitText(suggestion.word + " ")
        markInternalEdit()

        // Track suggestion acceptance for analytics
        if (::analyticsManager.isInitialized) {
            // Find position of suggestion in the list
            val suggestions = suggestionEngine.suggestions.value
            val position = suggestions.indexOfFirst { it.word == suggestion.word }
            analyticsManager.trackSuggestionAccepted(suggestion.word, if (position >= 0) position else 0)
        }

        // Learn from user's choice
        suggestionEngine.onSuggestionSelected(suggestion)

        // Clear suggestions
        suggestionStripView?.clear()
    }

    /**
     * Update suggestions based on current word
     *
     * Called after each character input or deletion
     */
    private fun updateSuggestions() {
        suggestionsJob?.cancel()
        suggestionsJob = serviceScope.launch {
            delay(Constants.Suggestions.SUGGESTION_TIMEOUT_MS)
            val currentWord = inputConnectionHandler.getCurrentWord()

            if (currentWord.isEmpty()) {
                suggestionStripView?.clear()
                return@launch
            }

            val currentLayout = layoutManager.activeLayout.value
            val language = if (currentLayout?.id == Constants.Layouts.LAYOUT_QWERTY) "en" else "kn"

            suggestionEngine.getSuggestions(
                currentWord = currentWord,
                language = language
            )
        }
    }

    /**
     * Show clipboard history popup
     *
     * Displays a popup window with clipboard history above the keyboard
     */
    private fun showClipboardPopup() {
        // Create popup view if needed
        val popupView = ClipboardPopupView(this).apply {
            // Set clipboard items
            setItems(clipboardManager.items.value)

            // Handle item click (paste)
            setOnItemClickListener { item ->
                // Paste the clipboard item
                val text = clipboardManager.pasteItem(item.id)
                if (text != null) {
                    finalizePhoneticBuffer()
                    resetPhoneticState()
                    inputConnectionHandler.commitText(text)
                    markInternalEdit()
                }
                hideClipboardPopup()
            }

            // Handle close button
            setOnCloseListener {
                hideClipboardPopup()
            }

            // Handle pin toggle
            setOnPinToggleListener { item ->
                clipboardManager.setPinned(item.id, !item.isPinned)
                // Update popup with new data
                setItems(clipboardManager.items.value)
            }

            // Handle delete
            setOnDeleteListener { item ->
                clipboardManager.deleteItem(item.id)
                // Update popup with new data
                setItems(clipboardManager.items.value)
            }
        }

        // Create popup window with responsive dimensions
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val popupWidth = (screenWidth * 0.9f).toInt()  // 90% of screen width
        val popupHeight = (400f * density).toInt()  // 400dp height

        clipboardPopup = PopupWindow(
            popupView,
            popupWidth,
            popupHeight,
            true
        ).apply {
            // Show popup above keyboard
            val keyboardView = keyboardView
            if (keyboardView != null) {
                showAtLocation(keyboardView, Gravity.BOTTOM, 0, keyboardView.height)
            }
        }
    }

    /**
     * Hide clipboard popup
     */
    private fun hideClipboardPopup() {
        clipboardPopup?.dismiss()
        clipboardPopup = null
    }

    /**
     * Handle clipboard button press
     *
     * Shows clipboard history popup
     */
    private fun onClipboardPressed() {
        // Track clipboard usage for analytics
        if (::analyticsManager.isInitialized) {
            val itemCount = clipboardManager.items.value.size
            analyticsManager.trackClipboardUsed(itemCount)
        }
        showClipboardPopup()
    }

    /**
     * Initialize voice input manager
     * 
     * Only initializes if API key is available.
     * If API key is not found, voice input will be disabled.
     */
    private fun initializeVoiceInputManager() {
        try {
            val apiKeyManager = ApiKeyManager(this)
            val apiKey = apiKeyManager.getBhashiniApiKey()
            if (apiKey.isNullOrBlank()) {
                android.util.Log.w("KaviIME", "Bhashini API key not found. Voice input disabled.")
                android.util.Log.w("KaviIME", "To enable voice input, add BHASHINI_API_KEY to local.properties")
                return
            }

            // Create API service (using inference service for ASR)
            val apiService = BhashiniApiClient.createInferenceService()

            // Create audio recorder
            val audioRecorder = AudioRecorder()

            // Create voice input manager
            voiceInputManager = VoiceInputManager(
                context = this,
                apiService = apiService,
                audioRecorder = audioRecorder,
                apiKey = apiKey!!
            )
            
            android.util.Log.d("KaviIME", "Voice input manager initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("KaviIME", "Failed to initialize voice input manager", e)
            if (::analyticsManager.isInitialized) {
                analyticsManager.trackError("voice_init", "Failed to initialize voice input manager", e)
            }
            voiceInputManager = null
        }
    }

    /**
     * Handle voice key press
     * 
     * Starts voice input recording and transcribes to text.
     * The transcribed text is then inserted into the current text field.
     */
    private fun onVoicePressed() {
        val manager = voiceInputManager
        if (manager == null) {
            android.util.Log.w("KaviIME", "Voice input not available. API key may be missing.")
            // TODO: Show user-friendly error message
            return
        }

        // Play sound feedback
        soundManager.playModifierClick()
        vibrationManager.vibrateModifierKey()

        // Start voice input in background
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Finalize any pending phonetic buffer before voice input
                finalizePhoneticBuffer()
                resetPhoneticState()

                // Start voice input
                when (val result = manager.startVoiceInput()) {
                    is Result.Success -> {
                        val transcribedText = result.data
                        
                        // Insert transcribed text on main thread
                        serviceScope.launch(Dispatchers.Main) {
                            inputConnectionHandler.commitText(transcribedText)
                            markInternalEdit()
                            android.util.Log.d("KaviIME", "Voice input successful: $transcribedText")
                        }
                    }
                    is Result.Error -> {
                        android.util.Log.e("KaviIME", "Voice input failed: ${result.exception.message}", result.exception)
                        if (::analyticsManager.isInitialized) {
                            analyticsManager.trackError("voice_input_error", "Voice input failed", result.exception)
                        }
                        // TODO: Show user-friendly error message
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("KaviIME", "Error in voice input", e)
                if (::analyticsManager.isInitialized) {
                    analyticsManager.trackError("voice_input_exception", "Exception during voice input", e)
                }
            }
        }
    }

    companion object {
        private const val INTERNAL_SELECTION_GRACE_MS = 150L
    }
}
