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

    // Direct callback for immediate updates (bypasses StateFlow)
    var onLayoutChanged: ((KeyboardLayout, List<KeyboardRow>) -> Unit)? = null

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
        // Always update the StateFlow to ensure observers are notified
        // Even if it's the same layout, we want to ensure the UI updates
        _activeLayout.value = layout
        // Reset to default layer when switching layouts
        _activeLayer.value = LayerName.DEFAULT
        // Reset shift states when switching layouts
        _isShiftActive.value = false
        _isCapsLockActive.value = false
        // Force update rows to ensure StateFlow emits
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
     * Handles edge cases where current layout might not be found
     */
    fun switchToNextLayout() {
        android.util.Log.e("LayoutManager", "===== SWITCH TO NEXT LAYOUT CALLED =====")
        val layouts = _availableLayouts.value
        android.util.Log.d("LayoutManager", "switchToNextLayout: Available layouts count = ${layouts.size}")

        if (layouts.isEmpty()) {
            android.util.Log.w("LayoutManager", "switchToNextLayout: No layouts available!")
            return
        }

        // If only one layout, nothing to switch
        if (layouts.size == 1) {
            android.util.Log.d("LayoutManager", "switchToNextLayout: Only one layout, nothing to switch")
            return
        }

        val currentLayout = _activeLayout.value
        val currentLayoutId = currentLayout?.id
        android.util.Log.d("LayoutManager", "switchToNextLayout: Current layout = $currentLayoutId")

        // Find current index by ID (more reliable than object reference)
        val currentIndex = if (currentLayoutId != null) {
            val found = layouts.indexOfFirst { it.id == currentLayoutId }
            if (found < 0) 0 else found
        } else {
            0
        }

        // Calculate next index with proper wrapping
        val nextIndex = if (currentIndex < layouts.size - 1) {
            currentIndex + 1
        } else {
            0 // Wrap around to first
        }

        // Get the next layout and always switch to it
        // This ensures StateFlow emits and observers are notified
        val nextLayout = layouts[nextIndex]
        android.util.Log.d("LayoutManager", "switchToNextLayout: Switching from index $currentIndex to $nextIndex, layout = ${nextLayout.id}")
        setActiveLayout(nextLayout)
        android.util.Log.d("LayoutManager", "switchToNextLayout: Switched to ${_activeLayout.value?.id}, layer = ${_activeLayer.value}, rows = ${_currentRows.value.size}")
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
            // Update layer state
            _activeLayer.value = layerName
            // Force update rows immediately
            updateCurrentRows()
            resultSuccess(Unit)
        } else {
            // If layer not found, fallback to default layer if available
            if (layerName != LayerName.DEFAULT && layout.hasLayer(LayerName.DEFAULT)) {
                _activeLayer.value = LayerName.DEFAULT
                updateCurrentRows()
                resultSuccess(Unit)
            } else {
                resultError("Layer '$layerName' not found in layout '${layout.name}'")
            }
        }
    }

    /**
     * Toggle shift (lowercase ↔ uppercase)
     *
     * First tap: Enable shift (one character)
     * Second tap (quick): Enable caps lock (all characters)
     * Third tap: Disable
     * 
     * Works smoothly from any layer (symbols, etc.) - switches to shift layer
     */
    fun toggleShift() {
        val layout = _activeLayout.value ?: return
        
        when {
            // Currently in caps lock → Disable everything
            _isCapsLockActive.value -> {
                _isCapsLockActive.value = false
                _isShiftActive.value = false
                // Return to default layer (not symbols)
                if (layout.hasLayer(LayerName.DEFAULT)) {
                    switchToLayer(LayerName.DEFAULT)
                }
            }
            // Currently in shift → Enable caps lock
            _isShiftActive.value -> {
                _isCapsLockActive.value = true
                _isShiftActive.value = true
                // Stay in shift layer
                if (layout.hasLayer(LayerName.SHIFT)) {
                    switchToLayer(LayerName.SHIFT)
                }
            }
            // Currently normal or in symbols → Enable shift
            else -> {
                _isShiftActive.value = true
                _isCapsLockActive.value = false
                // Switch to shift layer if available, otherwise stay on current
                if (layout.hasLayer(LayerName.SHIFT)) {
                    switchToLayer(LayerName.SHIFT)
                }
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
     * Cycles through: SYMBOLS -> SYMBOLS_EXTRA -> SYMBOLS_ALT -> SYMBOLS
     */
    fun switchToSymbols() {
        val layout = _activeLayout.value ?: return
        val currentLayer = _activeLayer.value
        
        // Cycle through symbol layers
        when (currentLayer) {
            LayerName.SYMBOLS -> {
                // Try to go to SYMBOLS_EXTRA, fallback to SYMBOLS_ALT if not available
                when {
                    layout.hasLayer(LayerName.SYMBOLS_EXTRA) -> switchToLayer(LayerName.SYMBOLS_EXTRA)
                    layout.hasLayer(LayerName.SYMBOLS_ALT) -> switchToLayer(LayerName.SYMBOLS_ALT)
                    else -> switchToLayer(LayerName.SYMBOLS) // Stay on SYMBOLS if no other layers
                }
            }
            LayerName.SYMBOLS_EXTRA -> {
                // Try to go to SYMBOLS_ALT, fallback to SYMBOLS
                when {
                    layout.hasLayer(LayerName.SYMBOLS_ALT) -> switchToLayer(LayerName.SYMBOLS_ALT)
                    else -> switchToLayer(LayerName.SYMBOLS)
                }
            }
            LayerName.SYMBOLS_ALT -> {
                // Cycle back to SYMBOLS
                switchToLayer(LayerName.SYMBOLS)
            }
            else -> {
                // From default/shift, go to first available symbol layer
                when {
                    layout.hasLayer(LayerName.SYMBOLS) -> switchToLayer(LayerName.SYMBOLS)
                    layout.hasLayer(LayerName.SYMBOLS_EXTRA) -> switchToLayer(LayerName.SYMBOLS_EXTRA)
                    layout.hasLayer(LayerName.SYMBOLS_ALT) -> switchToLayer(LayerName.SYMBOLS_ALT)
                    else -> {} // No symbol layers available
                }
            }
        }
    }

    /**
     * Switch back to default layer (ABC)
     * Resets shift state and returns to default layer
     * Works from any layer (symbols, shift, etc.)
     * 
     * @return Result indicating success or failure
     */
    fun switchToDefault(): Result<Unit> {
        _isShiftActive.value = false
        _isCapsLockActive.value = false
        val layout = _activeLayout.value ?: return resultError("No active layout")
        
        // Always try to switch to default layer
        if (layout.hasLayer(LayerName.DEFAULT)) {
            return switchToLayer(LayerName.DEFAULT)
        } else {
            // Fallback: try to get the first available layer
            val firstLayer = layout.getLayerNames().firstOrNull()
            return if (firstLayer != null) {
                switchToLayer(firstLayer)
            } else {
                resultError("No layers available in layout '${layout.name}'")
            }
        }
    }

    /**
     * Get the current keyboard rows based on active layout and layer
     * This is called whenever layout or layer changes
     */
    private fun updateCurrentRows() {
        val layout = _activeLayout.value
        val layer = _activeLayer.value

        android.util.Log.d("LayoutManager", "updateCurrentRows: layout=${layout?.id}, layer=$layer")
        // CRITICAL FIX: Use .toMutableList() instead of .toList() to GUARANTEE a new instance
        // This creates a new ArrayList that is ALWAYS a different reference
        // toList() might return the original list if it's already a List
        // toMutableList() ALWAYS creates a new ArrayList
        val rows = layout?.getLayer(layer)?.toMutableList() ?: mutableListOf()
        android.util.Log.d("LayoutManager", "updateCurrentRows: Got ${rows.size} rows (NEW MUTABLELIST created)")

        // Always update the StateFlow, even if rows are empty
        // This ensures observers are notified of changes
        val oldRows = _currentRows.value
        _currentRows.value = rows
        android.util.Log.d("LayoutManager", "updateCurrentRows: StateFlow updated, same reference? ${oldRows === rows}, oldSize=${oldRows.size}, newSize=${rows.size}")

        // DIRECT CALLBACK - Bypass StateFlow completely for immediate update
        layout?.let {
            android.util.Log.e("LayoutManager", "===== FIRING DIRECT CALLBACK: layout=${it.id}, rows=${rows.size} =====")
            onLayoutChanged?.invoke(it, rows)
        }
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
