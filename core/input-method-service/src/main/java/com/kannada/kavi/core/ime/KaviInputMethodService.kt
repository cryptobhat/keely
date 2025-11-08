package com.kannada.kavi.core.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.kannada.kavi.core.layout.LayoutManager
import com.kannada.kavi.core.layout.models.KeyType
import com.kannada.kavi.ui.keyboardview.KeyboardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

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

    // Coroutine scope for async operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // The keyboard view that will be displayed
    private var keyboardView: KeyboardView? = null

    /**
     * onCreate - Called when the service is first created
     *
     * This happens once when Android starts our keyboard service.
     * Like turning on a computer - happens once at startup.
     */
    override fun onCreate() {
        super.onCreate()

        // Initialize the layout manager
        layoutManager = LayoutManager(this)

        // Initialize input connection handler
        inputConnectionHandler = InputConnectionHandler()

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
        // Create our custom KeyboardView
        val view = KeyboardView(this).apply {
            // Set key press listener
            setOnKeyPressListener { key ->
                handleKeyPress(key)
            }
        }

        keyboardView = view

        // Observe layout changes and update keyboard view
        layoutManager.currentRows.onEach { rows ->
            keyboardView?.setKeyboard(rows)
        }.launchIn(serviceScope)

        return view
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

        // Connect to the text field
        val ic = currentInputConnection
        if (ic != null) {
            inputConnectionHandler.setInputConnection(ic)
        }

        // Check what type of field this is
        attribute?.let { info ->
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
    }

    /**
     * onDestroy - Called when the service is destroyed
     *
     * This is like shutting down a computer - happens once.
     * We clean up all resources here.
     */
    override fun onDestroy() {
        super.onDestroy()

        // Cancel all coroutines
        serviceScope.cancel()

        // Clean up keyboard view
        keyboardView = null
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
        when (key.type) {
            KeyType.CHARACTER -> onKeyPressed(key.output)
            KeyType.DELETE -> onDeletePressed()
            KeyType.ENTER -> onEnterPressed()
            KeyType.SPACE -> onSpacePressed()
            KeyType.SHIFT -> onShiftPressed()
            KeyType.SYMBOLS,
            KeyType.SYMBOLS_EXTRA,
            KeyType.SYMBOLS_ALT -> onSymbolsPressed()
            KeyType.DEFAULT -> onDefaultPressed()
            KeyType.LANGUAGE -> onLanguagePressed()
            KeyType.EMOJI -> {
                // TODO: Show emoji picker
            }
            KeyType.VOICE -> {
                // TODO: Start voice input
            }
            KeyType.SETTINGS -> {
                // TODO: Open settings
            }
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

        // Check if transliteration is enabled
        if (layoutManager.isTransliterationEnabled()) {
            // Try to transliterate
            val transliterated = layoutManager.transliterate(keyOutput)
            if (transliterated != null) {
                inputConnectionHandler.commitText(transliterated)
                return
            }
        }

        // Normal character input
        inputConnectionHandler.commitText(keyOutput)

        // Disable shift after typing (if not caps lock)
        layoutManager.disableShiftAfterInput()
    }

    /**
     * Handle delete key press
     */
    fun onDeletePressed() {
        inputConnectionHandler.deleteText()
    }

    /**
     * Handle enter key press
     */
    fun onEnterPressed() {
        val ic = currentInputConnection ?: return

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

            else -> {
                // Default: insert new line
                inputConnectionHandler.commitText("\n")
            }
        }
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
        inputConnectionHandler.commitText(" ")
        layoutManager.disableShiftAfterInput()
    }

    /**
     * Switch to symbols layer
     */
    fun onSymbolsPressed() {
        layoutManager.switchToSymbols()
    }

    /**
     * Switch back to default (ABC) layer
     */
    fun onDefaultPressed() {
        layoutManager.switchToDefault()
    }

    /**
     * Switch to next keyboard layout (Phonetic → Kavi → QWERTY)
     */
    fun onLanguagePressed() {
        layoutManager.switchToNextLayout()
    }

    /**
     * Get the current layout manager
     * Used by KeyboardView to get current keys
     */
    fun getLayoutManager(): LayoutManager {
        return layoutManager
    }
}
