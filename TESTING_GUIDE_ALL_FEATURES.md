# Complete Testing Guide - All 8 Features

This guide walks through testing all 8 newly implemented features in the Kavi keyboard.

---

## Prerequisites

1. Build the APK: `gradlew assembleDebug`
2. Install on device or emulator
3. Set Kavi as the default keyboard
4. Open a text app (Notes, Messages, Chrome)

---

## Feature 1: Clipboard System Sync

**Location:** Settings → Clipboard Manager → "System Clipboard Sync"

### Test Steps:
1. Open Settings app
2. Navigate to Clipboard Manager
3. Toggle "System Clipboard Sync" ON
4. Open Chrome and copy some text
5. Open any text field (Notes, Messages, etc.)
6. Open the keyboard's clipboard strip
7. **Expected:** Copied text appears in clipboard history
8. Toggle off and repeat
9. **Expected:** System clipboard no longer syncs

### Success Criteria:
- ✓ Toggle appears and persists across app restart
- ✓ When enabled, system clipboard items appear in history
- ✓ When disabled, system clipboard no longer captured
- ✓ User can paste any item from history

---

## Feature 2: Number Row Toggle

**Location:** Settings → Keyboard Layout → "Number Row"

### Test Steps:
1. Open Settings
2. Go to Keyboard Layout section
3. Toggle "Number Row" OFF (default)
4. Type normally, verify no number row visible above keyboard
5. Toggle "Number Row" ON
6. **Expected:** Row of keys 0-9 appears above main keyboard
7. Tap each number - verify 0-9 are inserted
8. Switch to Symbols layer
9. **Expected:** Number row disappears (not visible on symbol layer)
10. Switch back to DEFAULT
11. **Expected:** Number row reappears

### Success Criteria:
- ✓ Toggle controls visibility of number row
- ✓ Number row shows 0-9 keys
- ✓ Numbers insert correctly when tapped
- ✓ Number row only visible on DEFAULT/SHIFT layers
- ✓ Setting persists across app restart
- ✓ Works on all layouts (Phonetic, QWERTY, Kavi)

---

## Feature 3: Vibration Duration Feedback

**Location:** Settings → Feedback → "Vibration Duration" slider

### Test Steps:
1. Open Settings
2. Make sure "Vibration on Key Press" is enabled first
3. Scroll down to "Vibration Duration: XXms" slider
4. Move slider all the way left to 10ms
5. Press keys - feel **light, short vibration**
6. Move slider to middle (~55ms)
7. Press keys - feel **medium vibration**
8. Move slider all the way right to 100ms
9. Press keys - feel **strong, long vibration**
10. Switch to another app and back
11. **Expected:** Slider position persists

### Success Criteria:
- ✓ Slider appears when vibration enabled
- ✓ Value range: 10-100ms
- ✓ Slider shows current value
- ✓ Vibration intensity changes with slider
- ✓ Setting persists across sessions

**Note:** If device doesn't have vibration motor, you won't feel the haptic feedback.

---

## Feature 4: One-Handed Mode

**Location:** Settings → Keyboard Layout → "One-Handed Mode" (radio buttons)

### Test Steps:
1. Open Settings
2. Go to Keyboard Layout
3. Find "One-Handed Mode" section with 3 buttons: Off / Left / Right
4. Select "Off"
5. **Expected:** Keyboard is centered (normal position)
6. Select "Left"
7. Open a text field
8. **Expected:** Keyboard shifts to the LEFT side (lots of empty space on right)
   - *Current Status: UI ready, visual shift not yet implemented*
9. Select "Right"
10. **Expected:** Keyboard shifts to the RIGHT side (lots of empty space on left)
    - *Current Status: UI ready, visual shift not yet implemented*

### Success Criteria:
- ✓ Three radio button options visible
- ✓ Selection persists across app restart
- ✓ Can toggle between modes
- ✓ Only one mode selected at a time
- ✓ *Future:* Visual keyboard shift when implemented

---

## Feature 5: Auto-Capitalization Modes

**Location:** Settings → Keyboard Layout → "Auto-Capitalization" + mode selector

### Test Steps:

#### Part A: Enable/Disable Auto-Capitalization
1. Open Settings → Keyboard Layout
2. Toggle "Auto-Capitalization" OFF
3. Type: `hello world`
4. **Expected:** No capitalization: `hello world`
5. Toggle "Auto-Capitalization" ON
6. Mode selector appears with radio buttons

#### Part B: Test "None" Mode
1. Select "None" radio button
2. Type: `hello world. new sentence`
3. **Expected:** All lowercase: `hello world. new sentence`

#### Part C: Test "Sentences" Mode
1. Select "Sentences" radio button
2. Type: `hello world. new sentence! another one? last one.`
3. **Expected:** Caps after `.`, `!`, `?`:
   ```
   Hello world. New sentence! Another one? Last one.
   ```

#### Part D: Test "Words" Mode
1. Select "Words" radio button
2. Type: `hello world test`
3. **Expected:** Caps on every word start:
   ```
   Hello World Test
   ```

### Success Criteria:
- ✓ Toggle controls auto-capitalization
- ✓ "None" mode: no automatic caps
- ✓ "Sentences" mode: caps after `.!?`
- ✓ "Words" mode: caps on word starts
- ✓ Radio buttons show current mode
- ✓ Mode changes take effect immediately
- ✓ Setting persists across sessions

---

## Feature 6: Smart Punctuation

**Location:** Settings → Smart Features → "Smart Punctuation"

### Test Steps:

#### Part A: Curly Quotes
1. Open Settings → Smart Features
2. Toggle "Smart Punctuation" ON (default)
3. Open a text field
4. Type: `"hello" and 'world'`
5. **Expected:** Curly quotes appear:
   ```
   "hello" and 'world'
   ```
   (Not straight quotes: `"hello"`)

#### Part B: Em-Dash
1. Type: `This -- is a test`
2. **Expected:** Double-dash becomes em-dash:
   ```
   This — is a test
   ```

#### Part C: Ellipsis
1. Type: `Wait...` then continue
2. **Expected:** Three dots become ellipsis:
   ```
   Wait… (and continue)
   ```

#### Part D: Context-Aware Quotes
1. Type: `"start` (opening quote)
2. **Expected:** Left curly quote: `"`
3. Type: `end"` (closing quote)
4. **Expected:** Right curly quote: `"`

#### Part E: Disable Smart Punctuation
1. Go back to Settings
2. Toggle "Smart Punctuation" OFF
3. Type: `"test" and -- and ...`
4. **Expected:** Straight quotes and plain punctuation:
   ```
   "test" and -- and ...
   ```

### Success Criteria:
- ✓ Straight double quotes become curly quotes
- ✓ Straight single quotes become curly quotes
- ✓ Double-dash (--) becomes em-dash (—)
- ✓ Three dots (...) become ellipsis (…)
- ✓ Opening vs closing quote context works
- ✓ Can toggle on/off
- ✓ Setting persists across sessions

---

## Feature 7: Spacebar Language Indicator

**Location:** On the spacebar (always visible, no settings)

### Test Steps:
1. Open a text field
2. Look at the spacebar
3. **Expected:** You see the layout name displayed (e.g., "Phonetic")
4. Press the layout switcher button (usually on top-left)
5. Select "QWERTY" layout
6. **Expected:** Spacebar now shows "QWERTY"
7. Switch to "Kavi" layout
8. **Expected:** Spacebar now shows "Kavi"
9. Switch back to "Phonetic"
10. **Expected:** Spacebar shows "Phonetic"

### Success Criteria:
- ✓ Layout name appears on spacebar
- ✓ Name updates immediately when layout changes
- ✓ All layouts display their names
- ✓ Name is centered and readable
- ✓ Works during normal typing

---

## Feature 8: Emoji Skin Tone Selector

**Location:** Long-press any emoji in the emoji board

### Test Steps:

#### Part A: Open Emoji Board
1. Open a text field
2. Look for emoji button on keyboard (usually shows a smiley face 😀)
3. Tap it to open emoji board
4. **Expected:** Emoji board appears with many emojis

#### Part B: Long-Press Emoji with Skin Tones
1. Find an emoji that supports skin tones (e.g., 👋, 👍, 🙏, 💪)
2. Press and hold (long-press) the emoji
3. **Expected:** Popup appears showing:
   ```
   👋 🏻 🏼 🏽 🏾 🏿
   (base + 5 skin tone variants)
   ```
4. Tap one of the variants (e.g., 🏻 light)
5. **Expected:**
   - Emoji with skin tone is inserted
   - Popup disappears
   - Keyboard comes back

#### Part C: Try Different Skin Tones
1. Long-press 👍 (thumbs up)
2. Tap each variant and watch inserted emoji
3. Repeat with 🙏 (praying hands)
4. Repeat with 💪 (flexed biceps)

#### Part D: Long-Press Emoji Without Skin Tones
1. Find an emoji that doesn't support skin tones (e.g., 📱, 🎉, ⭐)
2. Long-press it
3. **Expected:**
   - Either popup doesn't appear
   - OR popup shows only base emoji
   - Emoji is inserted

### Success Criteria:
- ✓ Long-press detected on emoji keys
- ✓ Popup shows base + 5 skin tone variants
- ✓ Each variant displays correctly
- ✓ Tapping variant inserts emoji with tone modifier
- ✓ Popup dismisses after selection
- ✓ Keyboard returns to normal
- ✓ Non-skin-tone emojis handled gracefully

---

## Quick Test Checklist

### ✓ Session 1 Features (Easy)
- [ ] Clipboard sync ON/OFF toggle works
- [ ] Clipboard sync captures system clipboard
- [ ] Number row appears/disappears correctly
- [ ] Number keys (0-9) function properly
- [ ] Vibration slider adjusts feedback
- [ ] One-Handed mode has 3 options
- [ ] Settings persist across restarts

### ✓ Session 2 Features (Medium)
- [ ] Auto-cap "None" mode works
- [ ] Auto-cap "Sentences" mode works
- [ ] Auto-cap "Words" mode works
- [ ] Smart punctuation converts quotes
- [ ] Smart punctuation converts dashes/ellipsis
- [ ] Spacebar shows layout name
- [ ] Spacebar updates on layout change
- [ ] Emoji long-press shows variants popup
- [ ] Emoji variant selection inserts correctly

---

## Troubleshooting

### Number Row Not Appearing
- [ ] Make sure toggle is enabled in settings
- [ ] Make sure you're in DEFAULT or SHIFT layer
- [ ] Try restarting the keyboard app

### Smart Punctuation Not Working
- [ ] Verify toggle is ON in settings
- [ ] Try disabling and re-enabling
- [ ] Make sure you're typing slowly enough
- [ ] Try in a different text field

### Emoji Popup Not Showing
- [ ] Make sure you're long-pressing (not just tapping)
- [ ] Hold for ~400-500ms
- [ ] Try with different emojis
- [ ] Verify emoji is in standard emoji board

### Vibration Not Felt
- [ ] Check if device has vibration motor
- [ ] Make sure vibration is enabled globally in Android settings
- [ ] Toggle vibration on/off in Kavi settings
- [ ] Adjust slider while typing to confirm

---

## Performance Notes

All features should:
- ✓ Load instantly when app starts
- ✓ Toggle settings without lag
- ✓ Not slow down typing
- ✓ Handle rapid key presses
- ✓ Work with all layouts
- ✓ Not crash on edge cases

---

## Build & Installation

```bash
# Build debug APK
gradlew assembleDebug

# APK location
app/build/outputs/apk/debug/app-debug.apk

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Feedback

If any feature doesn't work as expected:
1. Note which feature and what happened
2. Note the device model and Android version
3. Note exact steps to reproduce
4. Check logcat for errors: `adb logcat | grep -i kavi`

---

**Testing Status:** Ready for QA
**Last Updated:** November 2025
