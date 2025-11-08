# 🧪 Kavi Keyboard Testing Guide

This guide helps you test the keyboard thoroughly to ensure everything works perfectly!

## 📋 Table of Contents
1. [Android Studio Setup & Build](#android-studio-setup--build)
2. [Installing on Device/Emulator](#installing-on-deviceemulator)
3. [Enabling the Keyboard](#enabling-the-keyboard)
4. [Core Functionality Tests](#core-functionality-tests)
5. [Layout Tests](#layout-tests)
6. [Sound & Haptic Tests](#sound--haptic-tests)
7. [Advanced Feature Tests](#advanced-feature-tests)
8. [Performance Tests](#performance-tests)
9. [Bug Reporting](#bug-reporting)

---

## 🔧 Android Studio Setup & Build

### Step 1: Open Project
1. Open Android Studio
2. Click **File → Open**
3. Navigate to `C:\Users\Nags\AndroidStudioProjects\Kavi`
4. Click **OK**
5. Wait for Gradle sync to complete

### Step 2: Sync Gradle
- Click **File → Sync Project with Gradle Files**
- Wait for sync to finish (may take 2-5 minutes first time)
- Check **Build** tab at bottom for any errors

### Step 3: Build Project
```bash
# In Android Studio terminal or external terminal:
./gradlew build
```
**Expected result:** Build SUCCESS (no errors)

**Common issues and fixes:**
- **Kotlin version mismatch:** Update `libs.versions.toml` if needed
- **Missing SDK:** Install required SDK version in SDK Manager
- **Gradle timeout:** Increase timeout in `gradle.properties`

---

## 📱 Installing on Device/Emulator

### Option A: Using Android Device (Recommended)
1. Enable **Developer Options** on your phone:
   - Go to **Settings → About Phone**
   - Tap **Build Number** 7 times
   - Go back, you'll see **Developer Options**

2. Enable **USB Debugging**:
   - Go to **Settings → Developer Options**
   - Turn on **USB Debugging**

3. Connect phone via USB
4. Click **Run** (green play button) in Android Studio
5. Select your device
6. App will install automatically

### Option B: Using Emulator
1. Open **Device Manager** in Android Studio
2. Create new device or use existing
3. Start emulator
4. Click **Run** and select emulator
5. App will install automatically

---

## ⌨️ Enabling the Keyboard

After installing the app, you need to enable it:

### Step 1: Enable Input Method
1. Go to **Settings → System → Languages & Input**
2. Tap **On-screen keyboard** or **Virtual keyboard**
3. Tap **Manage keyboards**
4. Find **Kavi Kannada Keyboard**
5. Toggle it **ON**

### Step 2: Select Keyboard
1. Open any app (WhatsApp, Messages, Notes, Chrome)
2. Tap a text field
3. Tap keyboard icon in navigation bar (or notification)
4. Select **Kavi Kannada Keyboard**

### Step 3: Verify It's Working
- You should see the Kavi keyboard appear
- Tap a few keys to test
- You should see text appearing in the text field

**Troubleshooting:**
- **Keyboard not listed?** Reinstall the app
- **Keyboard crashes?** Check Logcat in Android Studio
- **Keys not responding?** Check touch event handling

---

## ✅ Core Functionality Tests

### Test 1: Basic Text Input
**Goal:** Verify characters appear when keys are tapped

**Steps:**
1. Open Notes app
2. Tap a text field
3. Type: `ಕನ್ನಡ` (Kannada)
4. Verify text appears correctly

**Expected result:**
✅ Text "ಕನ್ನಡ" appears in text field
✅ No lag or delay
✅ Characters are correct Unicode

**If it fails:**
- Check InputConnectionHandler.commitText()
- Check KeyboardView touch detection
- Check character mappings in layout JSON

---

### Test 2: Delete Key
**Goal:** Verify backspace deletes characters

**Steps:**
1. Type some text: `ನಮಸ್ತೆ`
2. Tap delete key 3 times
3. Verify last 3 characters are deleted

**Expected result:**
✅ Characters deleted one by one
✅ Text becomes `ನಮ`
✅ Delete sound plays (if enabled)

**If it fails:**
- Check InputConnectionHandler.deleteText()
- Check DELETE key type in layout JSON

---

### Test 3: Space Key
**Goal:** Verify spacebar works

**Steps:**
1. Type: `ಕನ್ನಡ` + SPACE + `ಕೀಬೋರ್ಡ್`
2. Verify space appears between words

**Expected result:**
✅ Space inserted between words
✅ Text is: `ಕನ್ನಡ ಕೀಬೋರ್ಡ್`
✅ Space sound plays

---

### Test 4: Enter Key
**Goal:** Verify enter creates new line

**Steps:**
1. Type: `Line 1`
2. Tap ENTER
3. Type: `Line 2`

**Expected result:**
✅ New line created
✅ Text is:
```
Line 1
Line 2
```
✅ Enter sound plays

**Special case - Messaging apps:**
- In WhatsApp/Telegram, ENTER should SEND message
- Verify message is sent, not new line

---

### Test 5: Shift Key
**Goal:** Verify shift toggles uppercase/lowercase

**Steps:**
1. Tap SHIFT once
2. Type one character
3. Verify it's uppercase
4. Type another character
5. Verify it's lowercase (shift auto-disabled)

**Expected result:**
✅ First character uppercase
✅ Second character lowercase
✅ Shift key visual state changes

---

### Test 6: Caps Lock
**Goal:** Verify double-tap shift enables caps lock

**Steps:**
1. Double-tap SHIFT quickly
2. Type several characters
3. Verify all are uppercase
4. Tap SHIFT once
5. Verify caps lock disabled

**Expected result:**
✅ Caps lock enables on double-tap
✅ All characters uppercase
✅ Single tap disables caps lock

---

## 🎹 Layout Tests

### Test 7: Default Layout (Letters)
**Goal:** Verify default Kannada layout works

**Steps:**
1. Open keyboard
2. Verify you see Kannada letters
3. Type all keys in top row
4. Verify correct output

**Expected result:**
✅ Kavi layout displayed
✅ All keys produce correct Kannada characters
✅ No missing or duplicate keys

---

### Test 8: Symbols Layer
**Goal:** Verify switching to symbols works

**Steps:**
1. Tap **123** key
2. Verify symbols appear (!, @, #, $, etc.)
3. Type some symbols
4. Tap **ABC** key
5. Verify back to letters

**Expected result:**
✅ Symbols layer loads
✅ Symbols are correct
✅ ABC key returns to letters
✅ Modifier sound plays on switch

---

### Test 9: Layout Switching
**Goal:** Verify switching between layouts (Phonetic ↔ Kavi ↔ QWERTY)

**Steps:**
1. Tap **🌐** (language/globe) key
2. Verify layout changes to next one
3. Tap again, verify cycles through all 3 layouts
4. Tap again, verify returns to first layout

**Expected result:**
✅ Cycles: Phonetic → Kavi → QWERTY → Phonetic
✅ Keys change correctly for each layout
✅ No crash or freeze

---

### Test 10: Phonetic Transliteration
**Goal:** Verify English → Kannada conversion

**Steps:**
1. Switch to **Phonetic** layout
2. Type: `namaste`
3. Verify it converts to: `ನಮಸ್ತೆ`
4. Type: `kannada`
5. Verify it converts to: `ಕನ್ನಡ`

**Expected result:**
✅ English typed on keys
✅ Kannada appears in text field
✅ Common words convert correctly

**Test words:**
- `namaste` → `ನಮಸ್ತೆ`
- `kannada` → `ಕನ್ನಡ`
- `bengaluru` → `ಬೆಂಗಳೂರು`

---

## 🔊 Sound & Haptic Tests

### Test 11: Haptic Feedback
**Goal:** Verify vibration on key press

**Steps:**
1. Ensure phone vibration is enabled
2. Tap any key
3. Feel for vibration

**Expected result:**
✅ Short vibration on each key press
✅ Consistent across all keys
✅ Not too strong or too weak

**If it fails:**
- Check KeyboardView.performHapticFeedback()
- Check VIBRATE permission in AndroidManifest.xml

---

### Test 12: Sound Effects
**Goal:** Verify click sounds play

**Steps:**
1. Ensure media volume is up
2. Tap letter key - hear standard click
3. Tap DELETE - hear delete click
4. Tap SPACE - hear space click
5. Tap ENTER - hear enter click
6. Tap SHIFT/123 - hear modifier click

**Expected result:**
✅ Standard click: normal sound
✅ Delete: slightly different sound
✅ Space: distinct sound
✅ Enter: distinct sound
✅ Modifier: distinct sound

**If sounds don't play:**
- Check SoundManager.initialize()
- Falls back to system sounds (should still work)
- Check media volume on device

---

## 🚀 Advanced Feature Tests

### Test 13: Different Apps
**Goal:** Verify keyboard works in all apps

**Apps to test:**
- ✅ WhatsApp
- ✅ Chrome (Google search, address bar)
- ✅ Gmail
- ✅ Notes/Keep
- ✅ Facebook/Instagram
- ✅ Twitter/X
- ✅ SMS/Messages

**Expected result:**
✅ Works in all apps
✅ Text commits correctly
✅ No crashes

---

### Test 14: Special Input Types
**Goal:** Verify keyboard adapts to input type

**Test scenarios:**

**Email field:**
1. Open Chrome, go to Gmail login
2. Tap email field
3. Verify keyboard shows (should work normally)

**Password field:**
1. Tap password field
2. Verify keyboard works (future: could hide suggestions)

**Number field:**
1. Find app with number input (calculator, phone dialer)
2. Tap number field
3. Verify keyboard shows (future: could switch to number layout)

**URL field:**
1. Open Chrome address bar
2. Type a URL
3. Verify keyboard works

---

### Test 15: Orientation Change
**Goal:** Verify keyboard handles screen rotation

**Steps:**
1. Open keyboard in portrait mode
2. Type some text
3. Rotate device to landscape
4. Verify keyboard resizes correctly
5. Type more text
6. Rotate back to portrait

**Expected result:**
✅ Keyboard resizes smoothly
✅ No crash
✅ Text input still works
✅ All keys visible and touchable

---

## ⚡ Performance Tests

### Test 16: Typing Speed
**Goal:** Verify keyboard is responsive at high typing speed

**Steps:**
1. Type very fast (rapid tapping)
2. Verify all characters appear
3. No lag or dropped characters

**Expected result:**
✅ All characters register
✅ < 100ms latency
✅ Smooth, no stuttering

**Measure with:**
- Logcat timestamps
- Visual observation
- User perception

---

### Test 17: Memory Usage
**Goal:** Verify keyboard doesn't use excessive memory

**Steps:**
1. Open keyboard
2. Use it for 5 minutes (type a lot)
3. Check memory in Android Studio Profiler

**Expected result:**
✅ Memory usage < 50MB
✅ No memory leaks
✅ Stable over time

**How to check:**
- Android Studio → Profiler → Memory
- Watch heap allocation graph
- Look for steady increase (= leak)

---

### Test 18: Frame Rate
**Goal:** Verify smooth 60 FPS rendering

**Steps:**
1. Enable GPU rendering profile in Developer Options
2. Open keyboard
3. Observe green bars (should be below 16ms line)

**Expected result:**
✅ Consistent frame time < 16ms (60 FPS)
✅ No dropped frames during typing
✅ Smooth animations

---

## 🐛 Bug Reporting

### How to Report Issues

**Template:**
```
**Issue:** [Brief description]
**Steps to reproduce:**
1. Step 1
2. Step 2
3. Step 3

**Expected behavior:** [What should happen]
**Actual behavior:** [What actually happens]
**Device:** [Phone model, Android version]
**Logs:** [Paste Logcat if available]
**Screenshots:** [If applicable]
```

### Collecting Logs

**In Android Studio:**
1. Open **Logcat** tab at bottom
2. Filter by: `com.kannada.kavi`
3. Reproduce the issue
4. Copy relevant logs
5. Paste in bug report

**Common error patterns:**
- `NullPointerException` → Something not initialized
- `ClassCastException` → Wrong data type
- `ResourceNotFoundException` → Missing file/resource
- `ActivityNotFoundException` → Missing activity in manifest

---

## 📊 Testing Checklist

Before marking any feature as "complete", verify all these:

### Basic Functionality
- [ ] All letter keys work
- [ ] All symbol keys work
- [ ] Delete key works
- [ ] Space key works
- [ ] Enter key works
- [ ] Shift key works
- [ ] Caps lock works
- [ ] Layout switching works (🌐 key)
- [ ] Layer switching works (123 ↔ ABC)

### Layouts
- [ ] Phonetic layout loads
- [ ] Kavi layout loads
- [ ] QWERTY layout loads
- [ ] All 3 layouts have all keys
- [ ] Phonetic transliteration works

### Feedback
- [ ] Haptic feedback on key press
- [ ] Sound effects play (if enabled)
- [ ] Visual feedback (key press animation)

### Apps Compatibility
- [ ] Works in WhatsApp
- [ ] Works in Chrome
- [ ] Works in Gmail
- [ ] Works in Notes
- [ ] Works in SMS app

### Performance
- [ ] No lag when typing fast
- [ ] Memory usage < 50MB
- [ ] Frame rate 60 FPS
- [ ] No crashes after 10 min use

### Edge Cases
- [ ] Works after orientation change
- [ ] Works in split-screen mode
- [ ] Handles rapid layout switching
- [ ] Handles rapid key presses
- [ ] Recovers from interruptions (calls, notifications)

---

## 🎯 Next Steps After Testing

Once core functionality is verified working:

1. **Fix Critical Bugs**
   - Crashes
   - Characters not appearing
   - Keys not responding

2. **Build Suggestion Engine**
   - Word predictions
   - Auto-correction
   - Learning from user

3. **Add Clipboard Manager**
   - 50-item history
   - Undo/redo

4. **Implement Voice Input**
   - Bhashini integration
   - Speech-to-text
   - Text-to-speech

5. **Create Theme System**
   - Material You theming
   - Custom colors
   - Settings UI

---

## 📞 Support

**Issues?** Check:
- Logcat for error messages
- Module build files for missing dependencies
- AndroidManifest.xml for missing permissions
- Layout JSON files for syntax errors

**Still stuck?** Create an issue with:
- Full error message
- Steps to reproduce
- Device/emulator details
- Logcat output

---

**Happy Testing! 🚀**

*Remember: Testing is not just about finding bugs, it's about ensuring users have a delightful typing experience!*
