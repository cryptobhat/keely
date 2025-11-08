# 🔧 Build & Test Checklist

## Pre-Build Checks

### 1. Gradle Sync
- [ ] Open project in Android Studio
- [ ] Wait for Gradle sync to complete
- [ ] Check **Build** tab for errors
- [ ] If sync fails, check error messages

### 2. Common Issues & Fixes

**Issue: "SDK not found"**
- Fix: Tools → SDK Manager → Install required SDK version (API 24+)

**Issue: "Kotlin version mismatch"**
- Fix: Check `libs.versions.toml` matches installed Kotlin plugin

**Issue: "Dependency resolution failed"**
- Fix: File → Invalidate Caches → Restart

**Issue: "Module not found"**
- Fix: File → Sync Project with Gradle Files

### 3. Build the Project
```bash
# In Android Studio terminal:
./gradlew build
```

Expected: **BUILD SUCCESSFUL**

If build fails, check:
- [ ] All module `build.gradle.kts` files exist
- [ ] Dependencies are correct in `libs.versions.toml`
- [ ] No syntax errors in Kotlin files

---

## Device Setup

### 1. Enable Developer Options
1. Go to **Settings → About Phone**
2. Tap **Build Number** 7 times
3. Message: "You are now a developer!"

### 2. Enable USB Debugging
1. Go to **Settings → System → Developer Options**
2. Toggle **USB Debugging** ON
3. Connect phone via USB
4. Trust the computer (popup on phone)

### 3. Verify Connection
In Android Studio:
- Top toolbar: Click device dropdown
- Should see your phone listed
- If not, check USB cable/drivers

---

## Install & Run

### 1. Run the App
- Click green **Run** button (▶️) in Android Studio
- Or press **Shift + F10**
- Select your device
- Wait for installation (~30 seconds)

### 2. Watch Logcat
- Open **Logcat** tab at bottom
- Filter by: `com.kannada.kavi`
- Watch for errors during installation

---

## Enable Keyboard

### 1. Enable Input Method
1. Open **Settings** on phone
2. Go to **System → Languages & Input → On-screen keyboard**
3. Tap **Manage keyboards**
4. Find **Kavi Kannada Keyboard**
5. Toggle it **ON**

### 2. Select Keyboard
1. Open any app (Notes, WhatsApp, Chrome)
2. Tap a text field
3. Tap keyboard icon in navigation bar
4. Select **Kavi Kannada Keyboard**

---

## Test Features

### Core Functionality
- [ ] Keyboard appears
- [ ] Keys are visible
- [ ] Tap letter key → character appears
- [ ] Tap delete → character deleted
- [ ] Tap space → space inserted
- [ ] Tap enter → new line or send

### Layouts
- [ ] Default Kavi layout loads
- [ ] Tap 🌐 → switches layouts
- [ ] All 3 layouts work (Phonetic, Kavi, QWERTY)
- [ ] Tap 123 → symbols appear
- [ ] Tap ABC → back to letters

### Shift/Caps
- [ ] Tap Shift → next letter uppercase
- [ ] Double-tap Shift → Caps Lock
- [ ] Shift auto-disables after one character

### Feedback
- [ ] Keys vibrate when pressed (if vibration enabled)
- [ ] Sound plays when typing (if volume up)
- [ ] Keys show pressed state (visual feedback)

### Suggestions
- [ ] Suggestion strip appears above keyboard
- [ ] Type "nam" → shows suggestions
- [ ] Tap suggestion → inserts word
- [ ] Suggestions update as you type

### Clipboard (if clipboard button added)
- [ ] Tap clipboard button → popup appears
- [ ] Copy text → appears in history
- [ ] Tap item → pastes text
- [ ] Scroll works smoothly

---

## Common Runtime Errors

### Keyboard doesn't appear
**Check:**
1. Keyboard enabled in Settings?
2. Selected as active keyboard?
3. Check Logcat for crash logs

### Keyboard crashes on open
**Check:**
1. Logcat for stack trace
2. Likely NullPointerException
3. Check initialization in `onCreate()`

### Keys don't respond
**Check:**
1. Touch detection working?
2. Check `KeyboardView.onTouchEvent()`
3. Check `handleKeyPress()` is called

### No suggestions
**Check:**
1. SuggestionEngine initialized?
2. Check Logcat for initialization errors
3. Dictionary loaded?

### App won't install
**Check:**
1. USB debugging enabled?
2. Phone trusted?
3. Sufficient storage on phone?
4. Try: `adb uninstall com.kannada.kavi` then reinstall

---

## Debug Commands

```bash
# View installed packages
adb shell pm list packages | grep kavi

# Uninstall app
adb uninstall com.kannada.kavi

# View logs
adb logcat | grep Kavi

# Clear app data
adb shell pm clear com.kannada.kavi

# Force stop
adb shell am force-stop com.kannada.kavi
```

---

## Performance Checks

### Frame Rate
- Enable **Developer Options → Profile GPU Rendering**
- Green bars should be below 16ms line (60 FPS)

### Memory
- Android Studio → Profiler → Memory
- Should be < 50MB for keyboard

### Touch Latency
- Should feel instant (< 100ms)
- If laggy, check `onDraw()` performance

---

## Report Issues

If you find bugs, note:
1. **What happened:** Describe the bug
2. **Steps to reproduce:** Exact steps
3. **Expected:** What should happen
4. **Actual:** What actually happened
5. **Logcat:** Copy relevant error messages
6. **Device:** Phone model, Android version

---

## Success Criteria ✅

The keyboard is working if:
- ✅ All keys respond to touch
- ✅ Text appears in apps correctly
- ✅ Delete/Space/Enter work
- ✅ Layout switching works
- ✅ Suggestions appear
- ✅ No crashes
- ✅ Smooth performance (no lag)

If all above pass → **Keyboard is ready! 🎉**

---

## Next Steps After Testing

1. Fix any bugs found
2. Add more dictionary words
3. Test in multiple apps (WhatsApp, Chrome, Gmail, etc.)
4. Test edge cases (long text, special characters, etc.)
5. Performance optimization if needed
6. Add remaining features (voice input, themes)
7. Publish to Play Store! 🚀
