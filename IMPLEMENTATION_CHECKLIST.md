# Implementation Checklist: Swipe Typing & Gestures

This checklist helps you integrate the new gesture and clipboard features into the Kavi keyboard.

## ✅ Integration Steps

### Phase 1: Preferences Setup (COMPLETED)

- [x] Add swipe typing preferences
- [x] Add gesture control preferences
- [x] Add clipboard preferences
- [x] Add sensitivity settings
- [x] Add visibility toggles

**Files Modified:**
- `data/preferences/src/main/java/.../KeyboardPreferences.kt`

---

### Phase 2: KeyboardView Integration (TODO)

#### Step 1: Add Gesture Components to KeyboardView

**File:** `ui/keyboard-view/src/main/java/.../KeyboardView.kt`

```kotlin
class KeyboardView : View {
    // Add these fields
    private val swipeGestureDetector = SwipeGestureDetector()
    private var swipePathView: SwipePathView? = null
    private val swipeWordPredictor = SwipeWordPredictor()

    // Preferences
    private lateinit var preferences: KeyboardPreferences

    init {
        // Initialize preferences
        preferences = KeyboardPreferences(context)

        // Setup gesture detector
        swipeGestureDetector.setDensity(resources.displayMetrics.density)

        // Create swipe path overlay (if enabled)
        if (preferences.isSwipePathVisible()) {
            swipePathView = SwipePathView(context)
        }
    }
}
```

**Checklist:**
- [ ] Add SwipeGestureDetector field
- [ ] Add SwipePathView field (nullable)
- [ ] Add SwipeWordPredictor field
- [ ] Add KeyboardPreferences field
- [ ] Initialize in constructor
- [ ] Set density for gesture detector

---

#### Step 2: Setup Gesture Listener

**File:** `ui/keyboard-view/src/main/java/.../KeyboardView.kt`

```kotlin
init {
    // ... previous init code

    swipeGestureDetector.setListener(object : SwipeGestureDetector.GestureListener {
        override fun onTap(x: Float, y: Float) {
            // Check if swipe typing is disabled or user prefers taps
            if (!preferences.isSwipeTypingEnabled()) {
                handleNormalTap(x, y)
            }
        }

        override fun onLongPress(x: Float, y: Float) {
            handleLongPress(x, y)
        }

        override fun onSwipeStart(x: Float, y: Float) {
            if (preferences.isSwipeTypingEnabled() &&
                preferences.isSwipePathVisible()) {
                swipePathView?.startSwipe(x, y)
            }
        }

        override fun onSwipeMove(x: Float, y: Float, path: List<PointF>) {
            if (preferences.isSwipeTypingEnabled()) {
                swipePathView?.updatePath(x, y)

                // Track keys being swiped over
                val key = findKeyAt(x, y)
                if (key != null && key.label.length == 1) {
                    swipeGestureDetector.addKeyToSequence(key.label)
                }
            }
        }

        override fun onSwipeEnd(gesture: SwipeGesture) {
            swipePathView?.endSwipe()

            if (!preferences.isGesturesEnabled()) {
                return // Gestures disabled
            }

            when (gesture.type) {
                SwipeType.SWIPE_TYPE -> {
                    if (preferences.isSwipeTypingEnabled()) {
                        handleSwipeType(gesture)
                    }
                }
                SwipeType.SWIPE_DELETE -> {
                    if (preferences.isSwipeToDeleteEnabled()) {
                        handleSwipeDelete()
                    }
                }
                SwipeType.SWIPE_CURSOR -> {
                    if (preferences.isSwipeCursorMoveEnabled()) {
                        handleCursorMove(gesture)
                    }
                }
                else -> {}
            }
        }

        override fun onSwipeCancel() {
            swipePathView?.cancelSwipe()
        }
    })
}
```

**Checklist:**
- [ ] Implement GestureListener interface
- [ ] Handle onTap (check if swipe typing enabled)
- [ ] Handle onSwipeStart (start path visualization)
- [ ] Handle onSwipeMove (update path, track keys)
- [ ] Handle onSwipeEnd (predict word, handle gestures)
- [ ] Handle onSwipeCancel (clean up)
- [ ] Check preferences before each action

---

#### Step 3: Implement Gesture Handlers

**File:** `ui/keyboard-view/src/main/java/.../KeyboardView.kt`

```kotlin
/**
 * Handle swipe-to-type gesture
 */
private fun handleSwipeType(gesture: SwipeGesture) {
    // Get predictions from path
    val predictions = swipeWordPredictor.predictWord(gesture.path)

    if (predictions.isEmpty()) {
        return // No prediction found
    }

    // Take best prediction
    val bestWord = predictions.first().word

    // Create a text key with the predicted word
    val wordKey = Key(
        label = bestWord,
        output = bestWord,
        type = KeyType.CHARACTER
    )

    // Send to listener
    keyPressListener?.invoke(wordKey)

    // Optional: Show suggestions strip with other predictions
    if (predictions.size > 1) {
        showSuggestions(predictions.map { it.word })
    }
}

/**
 * Handle swipe-to-delete gesture
 */
private fun handleSwipeDelete() {
    // Create delete word key
    val deleteWordKey = Key(
        label = "delete_word",
        output = "",
        type = KeyType.DELETE_WORD // You may need to add this type
    )

    keyPressListener?.invoke(deleteWordKey)
}

/**
 * Handle cursor movement gesture
 */
private fun handleCursorMove(gesture: SwipeGesture) {
    val distance = when (gesture.direction) {
        SwipeDirection.LEFT -> -1
        SwipeDirection.RIGHT -> 1
        else -> 0
    }

    if (distance != 0) {
        val cursorKey = Key(
            label = "cursor_move",
            output = "",
            type = KeyType.CURSOR_MOVE // You may need to add this type
        )

        // You might want to pass distance in metadata
        keyPressListener?.invoke(cursorKey)
    }
}
```

**Checklist:**
- [ ] Implement handleSwipeType()
- [ ] Implement handleSwipeDelete()
- [ ] Implement handleCursorMove()
- [ ] Add DELETE_WORD KeyType (if needed)
- [ ] Add CURSOR_MOVE KeyType (if needed)

---

#### Step 4: Route Touch Events

**File:** `ui/keyboard-view/src/main/java/.../KeyboardView.kt`

```kotlin
override fun onTouchEvent(event: MotionEvent): Boolean {
    // Check if gestures are enabled
    if (preferences.isGesturesEnabled() || preferences.isSwipeTypingEnabled()) {
        // Route through gesture detector
        val handled = swipeGestureDetector.onTouchEvent(event)
        if (handled) {
            return true
        }
    }

    // Fall back to existing touch handling
    return super.onTouchEvent(event)
}
```

**Checklist:**
- [ ] Override onTouchEvent
- [ ] Check if gestures/swipe enabled
- [ ] Route to gesture detector
- [ ] Fall back to original handling

---

#### Step 5: Update Key Bounds for Prediction

**File:** `ui/keyboard-view/src/main/java/.../KeyboardView.kt`

```kotlin
override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)

    // Update predictor with key bounds
    if (preferences.isSwipeTypingEnabled()) {
        val boundsMap = mutableMapOf<String, RectF>()

        keyBounds.forEach { keyBound ->
            // Only add single-character keys for prediction
            if (keyBound.key.label.length == 1) {
                boundsMap[keyBound.key.label] = keyBound.bounds
            }
        }

        swipeWordPredictor.setKeyBounds(boundsMap)
    }
}
```

**Checklist:**
- [ ] Override onLayout (or use existing)
- [ ] Build key bounds map
- [ ] Update swipeWordPredictor.setKeyBounds()

---

#### Step 6: Load Dictionary

**File:** `ui/keyboard-view/src/main/java/.../KeyboardView.kt`

```kotlin
/**
 * Load dictionary for swipe word prediction
 */
private fun loadDictionary() {
    if (!preferences.isSwipeTypingEnabled()) {
        return
    }

    // Load dictionary words (implement based on your dictionary source)
    // This could be from assets, database, or downloaded
    val words = setOf(
        // Common English words for now
        "hello", "world", "how", "are", "you", "the", "and",
        "for", "this", "that", "with", "from", "have", "what",
        "when", "where", "which", "will", "would", "could",
        // Add more words...
    )

    swipeWordPredictor.setDictionary(words)
}
```

**Checklist:**
- [ ] Create loadDictionary() method
- [ ] Load words from assets/database
- [ ] Call setDictionary() on predictor
- [ ] Call loadDictionary() in init

---

### Phase 3: InputMethodService Integration (TODO)

**File:** `core/input-method-service/.../KaviInputMethodService.kt`

#### Step 1: Handle New Key Types

```kotlin
private fun handleKeyPress(key: Key) {
    when (key.type) {
        KeyType.DELETE_WORD -> {
            // Delete entire previous word
            deleteLastWord()
        }

        KeyType.CURSOR_MOVE -> {
            // Move cursor (you might encode direction in key.output)
            moveCursor(/* direction */)
        }

        // ... existing key handling

        else -> {
            // Existing handling
        }
    }
}

/**
 * Delete the last word
 */
private fun deleteLastWord() {
    val ic = currentInputConnection ?: return

    // Get text before cursor
    val textBeforeCursor = ic.getTextBeforeCursor(100, 0) ?: return

    // Find last word boundary
    val text = textBeforeCursor.toString()
    val lastSpaceIndex = text.lastIndexOf(' ')

    val charsToDelete = if (lastSpaceIndex >= 0) {
        text.length - lastSpaceIndex - 1
    } else {
        text.length
    }

    // Delete the word
    if (charsToDelete > 0) {
        ic.deleteSurroundingText(charsToDelete, 0)
    }
}

/**
 * Move cursor left or right
 */
private fun moveCursor(distance: Int) {
    val ic = currentInputConnection ?: return

    if (distance < 0) {
        // Move left
        ic.setSelection(
            ic.getCursorCapsMode(0) + distance,
            ic.getCursorCapsMode(0) + distance
        )
    } else {
        // Move right (similar)
    }
}
```

**Checklist:**
- [ ] Handle DELETE_WORD key type
- [ ] Handle CURSOR_MOVE key type
- [ ] Implement deleteLastWord()
- [ ] Implement moveCursor()

---

#### Step 2: Listen to Preference Changes

```kotlin
override fun onCreate() {
    super.onCreate()

    // ... existing onCreate code

    // Listen for preference changes
    preferences.registerChangeListener { _, key ->
        when (key) {
            "swipe_typing" -> {
                keyboardView?.updateSwipeTypingState(
                    preferences.isSwipeTypingEnabled()
                )
            }
            "gestures_enabled" -> {
                keyboardView?.updateGesturesState(
                    preferences.isGesturesEnabled()
                )
            }
            "swipe_path_visible" -> {
                keyboardView?.updateSwipePathVisibility(
                    preferences.isSwipePathVisible()
                )
            }
        }
    }
}

override fun onDestroy() {
    preferences.unregisterChangeListener(preferenceChangeListener)
    super.onDestroy()
}
```

**Checklist:**
- [ ] Register preference change listener in onCreate
- [ ] Handle swipe_typing changes
- [ ] Handle gestures_enabled changes
- [ ] Handle swipe_path_visible changes
- [ ] Unregister in onDestroy

---

### Phase 4: Settings UI (TODO)

**File:** `features/settings/src/main/java/.../SettingsActivity.kt`

#### Add Settings Screen

```kotlin
// In your settings screen
class InputFeaturesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.input_features_preferences, rootKey)

        // Swipe typing preference
        findPreference<SwitchPreference>("swipe_typing")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                // Update state
                true
            }
        }

        // Gesture controls preference
        findPreference<SwitchPreference>("gestures_enabled")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                // Update state
                true
            }
        }

        // Add more preference listeners...
    }
}
```

**Checklist:**
- [ ] Create settings XML resource
- [ ] Add swipe typing switch
- [ ] Add gesture controls switch
- [ ] Add sensitivity seekbar
- [ ] Add path visibility switch
- [ ] Wire up preference listeners

---

### Phase 5: Testing (TODO)

#### Unit Tests

**File:** Create test files

```kotlin
class SwipeGestureDetectorTest {
    @Test
    fun testTapDetection() {
        // Test that taps are detected correctly
    }

    @Test
    fun testSwipeDetection() {
        // Test that swipes are classified correctly
    }

    @Test
    fun testGestureVelocity() {
        // Test velocity calculations
    }
}

class SwipeWordPredictorTest {
    @Test
    fun testWordPrediction() {
        // Test word prediction from paths
    }

    @Test
    fun testDictionaryLookup() {
        // Test dictionary matching
    }
}
```

**Checklist:**
- [ ] Create SwipeGestureDetectorTest
- [ ] Create SwipeWordPredictorTest
- [ ] Test tap vs swipe detection
- [ ] Test word prediction accuracy
- [ ] Test dictionary lookup
- [ ] Test preference integration

---

#### Integration Tests

```kotlin
@Test
fun testSwipeTypingEndToEnd() {
    // 1. Enable swipe typing
    // 2. Simulate swipe gesture
    // 3. Verify word is predicted
    // 4. Verify word is inserted
}

@Test
fun testSwipeDeleteGesture() {
    // 1. Type some text
    // 2. Simulate swipe delete gesture
    // 3. Verify last word is deleted
}

@Test
fun testClipboardHistory() {
    // 1. Copy some items
    // 2. Open clipboard
    // 3. Verify items are shown
    // 4. Select item
    // 5. Verify it pastes
}
```

**Checklist:**
- [ ] Test swipe typing end-to-end
- [ ] Test swipe delete gesture
- [ ] Test cursor movement gesture
- [ ] Test clipboard history
- [ ] Test clipboard search
- [ ] Test preference changes

---

#### Manual Testing

**Checklist:**
- [ ] Swipe across keys forms correct words
- [ ] Visual path follows finger smoothly
- [ ] Swipe left on backspace deletes word
- [ ] Swipe on spacebar moves cursor
- [ ] Clipboard saves copied items
- [ ] Clipboard search filters correctly
- [ ] Settings enable/disable features
- [ ] Works with different keyboard themes
- [ ] Performance is smooth (60 FPS)
- [ ] No crashes or memory leaks

---

### Phase 6: Documentation (COMPLETED)

- [x] Create implementation guide
- [x] Create user guide
- [x] Create API reference
- [x] Create this checklist

**Files Created:**
- `GESTURES_AND_CLIPBOARD_IMPLEMENTATION.md`
- `USER_GUIDE_GESTURES_CLIPBOARD.md`
- `IMPLEMENTATION_CHECKLIST.md`

---

## 📋 Summary Checklist

### Core Components
- [x] SwipeGestureDetector created
- [x] SwipePathView created
- [x] SwipeWordPredictor created
- [x] ClipboardPopupView enhanced
- [x] KeyboardPreferences updated

### Integration
- [ ] Integrate gesture detector into KeyboardView
- [ ] Integrate swipe path view
- [ ] Integrate word predictor
- [ ] Handle new key types in InputMethodService
- [ ] Add preference listeners

### UI
- [ ] Create settings screen
- [ ] Add feature toggles
- [ ] Add sensitivity controls
- [ ] Add tutorial mode

### Testing
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Perform manual testing
- [ ] Test on multiple devices
- [ ] Performance testing

### Documentation
- [x] Implementation guide
- [x] User guide
- [x] API reference
- [x] Integration checklist

---

## 🚀 Next Steps

1. **Start with KeyboardView integration** (Phase 2)
2. **Add InputMethodService handlers** (Phase 3)
3. **Create settings UI** (Phase 4)
4. **Test thoroughly** (Phase 5)
5. **Deploy and gather feedback**

---

## 📞 Need Help?

If you get stuck during implementation:
- Review `GESTURES_AND_CLIPBOARD_IMPLEMENTATION.md` for detailed examples
- Check existing KeyboardView code for patterns
- Open an issue on GitHub

Good luck with the integration! 🎉
