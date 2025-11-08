package com.kannada.kavi.core.layout

import android.content.Context
import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.core.common.Result
import com.kannada.kavi.core.common.resultError
import com.kannada.kavi.core.common.resultSuccess
import com.kannada.kavi.core.layout.models.KeyboardLayout
import com.kannada.kavi.core.layout.models.KeyboardRow
import com.kannada.kavi.core.layout.models.LayerName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * LayoutManager - Manages Keyboard Layouts and Active State
 *
 * This is the brain that controls which layout is active, which layer is visible,
 * and how to switch between them.
 *
 * Think of it like a TV remote control:
 * - Loads different channels (layouts)
 * - Switches between channels
 * - Remembers your favorite channel
 * - Changes volume (layers within a layout)
 *
 * Responsibilities:
 * 1. Load all available layouts
 * 2. Manage active layout
 * 3. Manage active layer (default, shift, symbols)
 * 4. Handle layout switching
 * 5. Handle layer switching
 * 6. Provide current keyboard state to UI
 */
class LayoutManager(context: Context) {

    private val layoutLoader = LayoutLoader(context)

    // All loaded layouts (Phonetic, Kavi, QWERTY)
    private val _availableLayouts = MutableStateFlow<List<KeyboardLayout>>(emptyList())
    val availableLayouts: StateFlow<List<KeyboardLayout>> = _availableLayouts.asStateFlow()

    // Currently active layout
    private val _activeLayout = MutableStateFlow<KeyboardLayout?>(null)
    val activeLayout: StateFlow<KeyboardLayout?> = _activeLayout.asStateFlow()

    // Currently active layer (default, shift, symbols, etc.)
    private val _activeLayer = MutableStateFlow(LayerName.DEFAULT)
    val activeLayer: StateFlow<String> = _activeLayer.asStateFlow()

    // Current rows being displayed
    private val _currentRows = MutableStateFlow<List<KeyboardRow>>(emptyList())
    val currentRows: StateFlow<List<KeyboardRow>> = _currentRows.asStateFlow()

    // Is shift key pressed? (for sticky shift)
    private val _isShiftActive = MutableStateFlow(false)
    val isShiftActive: StateFlow<Boolean> = _isShiftActive.asStateFlow()

    // Is caps lock active? (double-tap shift)
    private val _isCapsLockActive = MutableStateFlow(false)
    val isCapsLockActive: StateFlow<Boolean> = _isCapsLockActive.asStateFlow()

    /**
     * Initialize the Layout Manager
     * Loads all layouts and sets the first one as active
     *
     * Call this when the keyboard starts!
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            // Load all layouts from assets
            val result = layoutLoader.loadAllLayouts()

            when (result) {
                is Result.Success -> {
                    _availableLayouts.value = result.data

                    // Set first layout as active (or load from preferences)
                    if (result.data.isNotEmpty()) {
                        setActiveLayout(result.data[0])
                    }

                    resultSuccess(Unit)
                }
                is Result.Error -> result
            }
        } catch (e: Exception) {
            resultError("Failed to initialize LayoutManager: ${e.message}")
        }
    }

    /**
     * Set a layout as the active layout
     *
     * @param layout The layout to activate
     */
    fun setActiveLayout(layout: KeyboardLayout) {
        _activeLayout.value = layout
        _activeLayer.value = LayerName.DEFAULT
        updateCurrentRows()
    }

    /**
     * Set active layout by ID
     *
     * @param layoutId The ID of the layout to activate ("phonetic", "kavi_custom", etc.)
     * @return Result indicating success or failure
     */
    fun setActiveLayoutById(layoutId: String): Result<Unit> {
        val layout = _availableLayouts.value.find { it.id == layoutId }

        return if (layout != null) {
            setActiveLayout(layout)
            resultSuccess(Unit)
        } else {
            resultError("Layout with ID '$layoutId' not found")
        }
    }

    /**
     * Switch to the next available layout
     * Cycles through: Phonetic → Kavi → QWERTY → Phonetic → ...
     */
    fun switchToNextLayout() {
        val layouts = _availableLayouts.value
        if (layouts.isEmpty()) return

        val currentLayout = _activeLayout.value
        val currentIndex = layouts.indexOf(currentLayout)
        val nextIndex = (currentIndex + 1) % layouts.size

        setActiveLayout(layouts[nextIndex])
    }

    /**
     * Switch to a specific layer
     *
     * @param layerName The name of the layer ("default", "shift", "symbols", etc.)
     * @return Result indicating success or failure
     */
    fun switchToLayer(layerName: String): Result<Unit> {
        val layout = _activeLayout.value ?: return resultError("No active layout")

        return if (layout.hasLayer(layerName)) {
            _activeLayer.value = layerName
            updateCurrentRows()
            resultSuccess(Unit)
        } else {
            resultError("Layer '$layerName' not found in layout '${layout.name}'")
        }
    }

    /**
     * Toggle shift (lowercase ↔ uppercase)
     *
     * First tap: Enable shift (one character)
     * Second tap (quick): Enable caps lock (all characters)
     * Third tap: Disable
     */
    fun toggleShift() {
        when {
            // Currently in caps lock → Disable everything
            _isCapsLockActive.value -> {
                _isCapsLockActive.value = false
                _isShiftActive.value = false
                switchToLayer(LayerName.DEFAULT)
            }
            // Currently in shift → Enable caps lock
            _isShiftActive.value -> {
                _isCapsLockActive.value = true
                _isShiftActive.value = true
                // Stay in shift layer
            }
            // Currently normal → Enable shift
            else -> {
                _isShiftActive.value = true
                _isCapsLockActive.value = false
                switchToLayer(LayerName.SHIFT)
            }
        }
    }

    /**
     * Disable shift after typing one character
     * (Only if caps lock is not active)
     */
    fun disableShiftAfterInput() {
        if (!_isCapsLockActive.value && _isShiftActive.value) {
            _isShiftActive.value = false
            switchToLayer(LayerName.DEFAULT)
        }
    }

    /**
     * Switch to symbols layer
     */
    fun switchToSymbols() {
        switchToLayer(LayerName.SYMBOLS)
    }

    /**
     * Switch back to default layer (ABC)
     */
    fun switchToDefault() {
        _isShiftActive.value = false
        _isCapsLockActive.value = false
        switchToLayer(LayerName.DEFAULT)
    }

    /**
     * Get the current keyboard rows based on active layout and layer
     */
    private fun updateCurrentRows() {
        val layout = _activeLayout.value
        val layer = _activeLayer.value

        val rows = layout?.getLayer(layer) ?: emptyList()
        _currentRows.value = rows
    }

    /**
     * Get layout by ID
     */
    fun getLayoutById(id: String): KeyboardLayout? {
        return _availableLayouts.value.find { it.id == id }
    }

    /**
     * Check if transliteration is enabled for current layout
     */
    fun isTransliterationEnabled(): Boolean {
        return _activeLayout.value?.transliterationEnabled ?: false
    }

    /**
     * Transliterate input text using current layout's rules
     *
     * @param input The text to transliterate
     * @return The transliterated text, or null if not available
     */
    fun transliterate(input: String): String? {
        return _activeLayout.value?.transliterate(input)
    }

    /**
     * Get current layout info as string
     * Useful for debugging and logging
     */
    fun getCurrentLayoutInfo(): String {
        val layout = _activeLayout.value ?: return "No active layout"
        val layer = _activeLayer.value
        return "Layout: ${layout.name}, Layer: $layer, Rows: ${_currentRows.value.size}"
    }

    /**
     * Reset to default state
     * Useful when keyboard is hidden and shown again
     */
    fun reset() {
        _isShiftActive.value = false
        _isCapsLockActive.value = false
        _activeLayer.value = LayerName.DEFAULT
        updateCurrentRows()
    }
}
