package com.kannada.kavi.features.clipboard

import android.content.ClipData
import android.content.ClipboardManager as AndroidClipboardManager
import android.content.Context
import com.kannada.kavi.core.common.Constants
import com.kannada.kavi.features.clipboard.models.ClipboardItem
import com.kannada.kavi.features.clipboard.models.detectContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ClipboardManager - Advanced Clipboard with History
 *
 * Manages clipboard history with 50-item storage, undo/redo, and more!
 *
 * FEATURES:
 * =========
 * 1. **History**: Stores last 50 copied items
 * 2. **Undo/Redo**: 20-action undo stack
 * 3. **Search**: Find old clipboard items
 * 4. **Pin**: Keep important items forever
 * 5. **Smart Detection**: URLs, emails, phones
 * 6. **Reactive**: Observable with Flow
 *
 * HOW IT WORKS:
 * =============
 * 1. User copies text → addItem()
 * 2. Stored in memory list (max 50)
 * 3. Also stored in undo stack (max 20)
 * 4. UI observes via Flow
 * 5. User can paste any item
 * 6. Undo/redo changes
 *
 * UNDO/REDO SYSTEM:
 * =================
 * - Each action creates a state snapshot
 * - Undo: Go back to previous state
 * - Redo: Go forward to next state
 * - Actions: Add, Delete, Clear, Pin
 *
 * EXAMPLE:
 * ========
 * 1. Copy "Hello" → added to history
 * 2. Copy "World" → added to history
 * 3. Delete "Hello" → action recorded
 * 4. Undo → "Hello" restored!
 * 5. Redo → "Hello" deleted again
 *
 * ARCHITECTURE:
 * =============
 * ```
 * User Action
 *     ↓
 * ClipboardManager.addItem()
 *     ↓
 * Update memory list
 *     ↓
 * Save to undo stack
 *     ↓
 * Emit Flow update
 *     ↓
 * UI updates automatically
 * ```
 */
class ClipboardManager(
    private val context: Context,
    private val clipboardRepository: ClipboardRepository? = null
) {

    // Android system clipboard (for integration)
    private val systemClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as AndroidClipboardManager

    // Clipboard history (in memory)
    private val _items = MutableStateFlow<List<ClipboardItem>>(emptyList())
    val items: StateFlow<List<ClipboardItem>> = _items.asStateFlow()

    // Undo/Redo stacks
    private val undoStack = ArrayDeque<ClipboardAction>()
    private val redoStack = ArrayDeque<ClipboardAction>()

    // Maximum items and undo depth
    private val maxItems = Constants.Clipboard.MAX_HISTORY_ITEMS
    private val maxUndoActions = Constants.Clipboard.MAX_UNDO_STACK_SIZE

    // Coroutine scope for async database operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Track if clipboard has been initialized (loaded from database)
    private var isInitialized = false

    /**
     * Initialize clipboard manager by loading history from database.
     * Call this when keyboard service starts.
     */
    fun initialize() {
        if (isInitialized) return

        scope.launch {
            try {
                // Load clipboard history from database
                if (clipboardRepository != null) {
                    val savedItems = clipboardRepository.loadHistory(limit = maxItems)
                    _items.value = savedItems
                    println("ClipboardManager: Loaded ${savedItems.size} items from database")
                } else {
                    println("ClipboardManager: No repository available, starting with empty clipboard")
                }
                isInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
                println("ClipboardManager: Failed to load clipboard history: ${e.message}")
                isInitialized = true // Mark as initialized even on error to prevent retry loops
            }
        }
    }

    /**
     * Add item to clipboard history
     *
     * @param text Text to add
     * @param sourceApp Optional source app name
     * @return The created ClipboardItem
     */
    fun addItem(text: String, sourceApp: String? = null): ClipboardItem {
        val sanitizedText = text.trim().take(Constants.Clipboard.MAX_CLIP_LENGTH)

        // Don't add empty text
        if (sanitizedText.isBlank()) {
            return _items.value.firstOrNull() ?: createEmptyItem()
        }

        // Don't add duplicates (if same as most recent)
        if (_items.value.firstOrNull()?.text == sanitizedText) {
            return _items.value.first()
        }

        // Create new item
        val item = ClipboardItem(
            id = UUID.randomUUID().toString(),
            text = sanitizedText,
            timestamp = System.currentTimeMillis(),
            isPinned = false,
            sourceApp = sourceApp,
            contentType = detectContentType(sanitizedText)
        )

        // Add to history
        val currentItems = _items.value.toMutableList()
        currentItems.add(0, item) // Add to beginning (newest first)

        // Remove oldest unpinned entries if exceeding limit
        if (currentItems.size > maxItems) {
            var index = currentItems.lastIndex
            while (currentItems.size > maxItems && index >= 0) {
                if (!currentItems[index].isPinned) {
                    currentItems.removeAt(index)
                }
                index--
            }
        }

        // Record action for undo
        recordAction(ClipboardAction.Add(item))

        // Update state
        _items.value = currentItems

        // Save to database
        scope.launch {
            try {
                clipboardRepository?.saveItem(item)
                // Also trim old items in database
                clipboardRepository?.trimHistory(maxUnpinned = maxItems)
            } catch (e: Exception) {
                e.printStackTrace()
                println("ClipboardManager: Failed to save item to database: ${e.message}")
            }
        }

        // Also copy to system clipboard
        copyToSystemClipboard(sanitizedText)

        return item
    }

    /**
     * Delete item from history
     *
     * @param itemId ID of item to delete
     * @return true if deleted, false if not found or pinned
     */
    fun deleteItem(itemId: String): Boolean {
        val currentItems = _items.value.toMutableList()
        val itemIndex = currentItems.indexOfFirst { it.id == itemId }

        if (itemIndex == -1) return false

        val item = currentItems[itemIndex]

        // Can't delete pinned items
        if (item.isPinned) return false

        // Remove item
        currentItems.removeAt(itemIndex)

        // Record action for undo
        recordAction(ClipboardAction.Delete(item, itemIndex))

        // Update state
        _items.value = currentItems

        // Delete from database
        scope.launch {
            try {
                clipboardRepository?.deleteItem(itemId)
            } catch (e: Exception) {
                e.printStackTrace()
                println("ClipboardManager: Failed to delete item from database: ${e.message}")
            }
        }

        return true
    }

    /**
     * Pin/Unpin an item
     *
     * Pinned items never get auto-deleted
     *
     * @param itemId ID of item to pin/unpin
     * @param pinned true to pin, false to unpin
     * @return true if successful
     */
    fun setPinned(itemId: String, pinned: Boolean): Boolean {
        val currentItems = _items.value.toMutableList()
        val itemIndex = currentItems.indexOfFirst { it.id == itemId }

        if (itemIndex == -1) return false

        val oldItem = currentItems[itemIndex]
        val newItem = oldItem.copy(isPinned = pinned)

        // Update item
        currentItems[itemIndex] = newItem

        // Record action for undo
        recordAction(ClipboardAction.Pin(oldItem, newItem, itemIndex))

        // Update state
        _items.value = currentItems

        // Update in database
        scope.launch {
            try {
                clipboardRepository?.updatePinStatus(itemId, pinned)
            } catch (e: Exception) {
                e.printStackTrace()
                println("ClipboardManager: Failed to update pin status in database: ${e.message}")
            }
        }

        return true
    }

    /**
     * Clear all items (except pinned)
     *
     * @return Number of items cleared
     */
    fun clearAll(): Int {
        val currentItems = _items.value.toMutableList()
        val itemsToKeep = currentItems.filter { it.isPinned }
        val clearedCount = currentItems.size - itemsToKeep.size

        if (clearedCount == 0) return 0

        // Record action for undo
        recordAction(ClipboardAction.Clear(currentItems, itemsToKeep))

        // Update state
        _items.value = itemsToKeep

        // Delete from database
        scope.launch {
            try {
                clipboardRepository?.deleteAllUnpinned()
            } catch (e: Exception) {
                e.printStackTrace()
                println("ClipboardManager: Failed to clear clipboard in database: ${e.message}")
            }
        }

        return clearedCount
    }

    /**
     * Clear all items including pinned
     *
     * @return Number of items cleared
     */
    fun clearAllIncludingPinned(): Int {
        val currentItems = _items.value
        val clearedCount = currentItems.size

        if (clearedCount == 0) return 0

        // Record action for undo
        recordAction(ClipboardAction.Clear(currentItems, emptyList()))

        // Update state
        _items.value = emptyList()

        // Delete from database
        scope.launch {
            try {
                clipboardRepository?.clearAll()
            } catch (e: Exception) {
                e.printStackTrace()
                println("ClipboardManager: Failed to clear all clipboard items in database: ${e.message}")
            }
        }

        return clearedCount
    }

    /**
     * Paste an item (copy it to system clipboard)
     *
     * @param itemId ID of item to paste
     * @return The pasted text, or null if not found
     */
    fun pasteItem(itemId: String): String? {
        val item = _items.value.find { it.id == itemId } ?: return null

        // Copy to system clipboard
        copyToSystemClipboard(item.text)

        // Move this item to top of history (mark as recently used)
        val currentItems = _items.value.toMutableList()
        val itemIndex = currentItems.indexOfFirst { it.id == itemId }

        if (itemIndex > 0) {
            // Remove from current position
            currentItems.removeAt(itemIndex)
            // Add to beginning
            currentItems.add(0, item)

            // Update state (no undo needed for this)
            _items.value = currentItems
        }

        return item.text
    }

    /**
     * Search clipboard history
     *
     * @param query Search term
     * @return Matching items
     */
    fun search(query: String): List<ClipboardItem> {
        if (query.isEmpty()) return _items.value

        return _items.value.filter { item ->
            item.matchesSearch(query)
        }
    }

    /**
     * Get item by ID
     *
     * @param itemId Item ID
     * @return ClipboardItem or null
     */
    fun getItem(itemId: String): ClipboardItem? {
        return _items.value.find { it.id == itemId }
    }

    /**
     * Can undo?
     *
     * @return true if undo stack is not empty
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()

    /**
     * Can redo?
     *
     * @return true if redo stack is not empty
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    /**
     * Undo last action
     *
     * @return true if undo successful
     */
    fun undo(): Boolean {
        if (!canUndo()) return false

        val action = undoStack.removeLast()
        action.undo(this)
        redoStack.addLast(action)

        // Trim redo stack if too large
        while (redoStack.size > maxUndoActions) {
            redoStack.removeFirst()
        }

        return true
    }

    /**
     * Redo last undone action
     *
     * @return true if redo successful
     */
    fun redo(): Boolean {
        if (!canRedo()) return false

        val action = redoStack.removeLast()
        action.redo(this)
        undoStack.addLast(action)

        return true
    }

    // ==================== Private Helper Functions ====================

    /**
     * Record an action for undo/redo
     */
    private fun recordAction(action: ClipboardAction) {
        undoStack.addLast(action)

        // Clear redo stack when new action is performed
        redoStack.clear()

        // Trim undo stack if too large
        while (undoStack.size > maxUndoActions) {
            undoStack.removeFirst()
        }
    }

    /**
     * Copy text to system clipboard
     */
    private fun copyToSystemClipboard(text: String) {
        val clip = ClipData.newPlainText("Kavi Clipboard", text)
        systemClipboard.setPrimaryClip(clip)
    }

    /**
     * Create empty item (fallback)
     */
    private fun createEmptyItem(): ClipboardItem {
        return ClipboardItem(
            id = UUID.randomUUID().toString(),
            text = "",
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Internal method to set items directly (used by undo/redo)
     */
    internal fun setItemsInternal(items: List<ClipboardItem>) {
        _items.value = items
    }

    /**
     * ClipboardAction - Represents an undoable action
     *
     * Sealed class for type-safe undo/redo
     */
    private sealed class ClipboardAction {
        abstract fun undo(manager: ClipboardManager)
        abstract fun redo(manager: ClipboardManager)

        /**
         * Add item action
         */
        data class Add(val item: ClipboardItem) : ClipboardAction() {
            override fun undo(manager: ClipboardManager) {
                // Remove the added item
                val items = manager._items.value.toMutableList()
                items.removeAll { it.id == item.id }
                manager.setItemsInternal(items)
            }

            override fun redo(manager: ClipboardManager) {
                // Re-add the item
                val items = manager._items.value.toMutableList()
                items.add(0, item)
                manager.setItemsInternal(items)
            }
        }

        /**
         * Delete item action
         */
        data class Delete(val item: ClipboardItem, val originalIndex: Int) : ClipboardAction() {
            override fun undo(manager: ClipboardManager) {
                // Restore the deleted item at original position
                val items = manager._items.value.toMutableList()
                items.add(originalIndex.coerceAtMost(items.size), item)
                manager.setItemsInternal(items)
            }

            override fun redo(manager: ClipboardManager) {
                // Re-delete the item
                val items = manager._items.value.toMutableList()
                items.removeAll { it.id == item.id }
                manager.setItemsInternal(items)
            }
        }

        /**
         * Pin/Unpin action
         */
        data class Pin(
            val oldItem: ClipboardItem,
            val newItem: ClipboardItem,
            val index: Int
        ) : ClipboardAction() {
            override fun undo(manager: ClipboardManager) {
                // Restore old item
                val items = manager._items.value.toMutableList()
                items[index] = oldItem
                manager.setItemsInternal(items)
            }

            override fun redo(manager: ClipboardManager) {
                // Restore new item
                val items = manager._items.value.toMutableList()
                items[index] = newItem
                manager.setItemsInternal(items)
            }
        }

        /**
         * Clear all action
         */
        data class Clear(
            val oldItems: List<ClipboardItem>,
            val keptItems: List<ClipboardItem>
        ) : ClipboardAction() {
            override fun undo(manager: ClipboardManager) {
                // Restore all items
                manager.setItemsInternal(oldItems)
            }

            override fun redo(manager: ClipboardManager) {
                // Clear again
                manager.setItemsInternal(keptItems)
            }
        }
    }
}

/**
 * USAGE EXAMPLE:
 * ==============
 *
 * ```kotlin
 * val clipboardManager = ClipboardManager(context)
 *
 * // Add items
 * clipboardManager.addItem("Hello World")
 * clipboardManager.addItem("Kannada Keyboard")
 * clipboardManager.addItem("https://github.com")
 *
 * // Observe changes
 * lifecycleScope.launch {
 *     clipboardManager.items.collect { items ->
 *         // Update UI with clipboard items
 *         updateClipboardUI(items)
 *     }
 * }
 *
 * // Paste an item
 * val text = clipboardManager.pasteItem(itemId)
 * inputConnection.commitText(text, 1)
 *
 * // Delete an item
 * clipboardManager.deleteItem(itemId)
 *
 * // Undo
 * if (clipboardManager.canUndo()) {
 *     clipboardManager.undo()
 * }
 *
 * // Redo
 * if (clipboardManager.canRedo()) {
 *     clipboardManager.redo()
 * }
 *
 * // Pin important item
 * clipboardManager.setPinned(itemId, true)
 *
 * // Search
 * val results = clipboardManager.search("hello")
 *
 * // Clear all
 * clipboardManager.clearAll()
 * ```
 */
