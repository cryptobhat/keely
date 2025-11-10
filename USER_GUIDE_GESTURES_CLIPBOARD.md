# User Guide: Swipe Typing, Gestures & Clipboard

This guide explains how to enable and use the new swipe typing, gesture controls, and clipboard features in Kavi Keyboard.

---

## 📱 Quick Start

### Default Settings (Out of the Box)

When you install Kavi Keyboard, these features have the following defaults:

| Feature | Default State | Description |
|---------|--------------|-------------|
| **Swipe Typing** | ❌ OFF | Glide across keys to type words |
| **Gesture Controls** | ✅ ON | Swipe gestures for quick actions |
| **Swipe to Delete** | ✅ ON | Swipe left on backspace to delete word |
| **Cursor Movement** | ✅ ON | Swipe on spacebar to move cursor |
| **Clipboard History** | ✅ ON | Save last 50 copied items |
| **Swipe Path Visual** | ✅ ON | Show trail when swiping |

---

## 🎯 How to Enable Features

### Method 1: Via Settings App (Recommended)

1. **Open Kavi Settings:**
   - Long press the **Settings key** (⚙️) on the keyboard, OR
   - Open Android Settings → System → Languages & input → Kavi Keyboard → Settings

2. **Navigate to Feature Settings:**
   ```
   Settings
   └── Input Features
       ├── ☑️ Swipe Typing
       ├── ☑️ Gesture Controls
       └── ☑️ Clipboard History
   ```

3. **Toggle Features:**
   - Tap the switch next to each feature to enable/disable
   - Changes take effect immediately

### Method 2: Via Quick Toggle (In Development)

Future versions will support quick toggles directly from the keyboard.

---

## 📝 Feature Details & Usage

### 1. 🌀 Swipe Typing

**What it does:** Type words by gliding your finger across the keys instead of tapping each letter.

#### How to Enable
```
Settings → Input Features → Swipe Typing → ON
```

#### How to Use

1. **Start typing:** Touch the first letter of the word
2. **Glide:** Move your finger across the keyboard, passing over each letter
3. **Lift:** Lift your finger on the last letter
4. **See prediction:** The word appears automatically!

**Example:**
- To type "hello":
  1. Touch "h"
  2. Glide through "e" → "l" → "l" → "o"
  3. Lift finger
  4. Word "hello" is inserted!

#### Tips for Better Accuracy
- Move at a **steady pace** (not too fast or slow)
- Pass **close to the center** of each letter
- The visual trail helps you see your path
- Try the same word a few times - it learns your style!

#### Advanced Settings

```
Settings → Input Features → Swipe Typing → Advanced
```

| Setting | Options | Default | Description |
|---------|---------|---------|-------------|
| **Sensitivity** | Low / Medium / High | Medium | How precise you need to be |
| **Show Path** | ON / OFF | ON | Display visual trail |
| **Auto-space** | ON / OFF | ON | Add space after swipe word |
| **Suggestions** | 1 / 3 / 5 | 3 | Number of word suggestions |

---

### 2. 🎮 Gesture Controls

**What it does:** Quick swipe gestures for common actions.

#### How to Enable
```
Settings → Input Features → Gesture Controls → ON
```

#### Available Gestures

##### A. Swipe to Delete Word
**What:** Quickly delete the last word you typed

**How:**
1. Swipe **left** on the backspace key (⌫)
2. The entire previous word is deleted
3. Faster than tapping backspace multiple times!

**Enable/Disable:**
```
Settings → Input Features → Gesture Controls → Swipe to Delete
```

##### B. Cursor Movement
**What:** Move the text cursor left or right

**How:**
1. Swipe **left or right** on the **spacebar**
2. Cursor moves one character per swipe
3. Long swipe moves multiple characters

**Enable/Disable:**
```
Settings → Input Features → Gesture Controls → Cursor Movement
```

##### C. Quick Capitalize (Coming Soon)
**What:** Swipe up on any letter to capitalize it

**How:**
1. Instead of tapping shift, swipe **up** on the letter
2. Letter is automatically capitalized
3. Great for names and acronyms!

#### Gesture Sensitivity

```
Settings → Input Features → Gesture Controls → Sensitivity
```

| Level | Description | Best For |
|-------|-------------|----------|
| **Low** | Requires larger swipes | Avoid accidental gestures |
| **Medium** | Balanced (default) | Most users |
| **High** | Small swipes trigger | Power users, fast typing |

---

### 3. 📋 Clipboard History

**What it does:** Remembers everything you copy (up to 50 items) so you can paste it later.

#### How to Enable
```
Settings → Input Features → Clipboard History → ON
```

#### How to Use

##### Opening Clipboard
1. Tap the **clipboard icon** (📋) on the keyboard
2. The clipboard popup appears above the keyboard

##### Pasting an Item
**Method 1 (Quick Paste):**
- Tap any item in the list → It pastes immediately

**Method 2 (View First):**
- Long press an item → Preview appears
- Tap "Paste" to insert it

##### Pinning Important Items
1. Open clipboard
2. Tap the **star icon** (⭐) next to an item
3. Pinned items stay at the top and never expire

##### Searching Clipboard
1. Open clipboard
2. Type in the **search box** at the top
3. Items are filtered as you type
4. Supports partial matching

**Example:**
- Search "github" finds "https://github.com/user/repo"

##### Filtering by Category
Tap category chips at the top:

| Category | Icon | Shows |
|----------|------|-------|
| **All** | 📄 | Everything |
| **Pinned** | ⭐ | Only pinned items |
| **Text** | 📝 | Plain text only |
| **Links** | 🔗 | URLs only |
| **Code** | 💻 | Code snippets |

##### Deleting Items
**Method 1 (Swipe):**
- Swipe **left** on any item
- Tap "Delete" to confirm

**Method 2 (Long Press):**
- Long press the item
- Select "Delete" from menu

**Note:** Pinned items cannot be deleted (unpin first)

##### Double-Tap to Paste
- Double-tap any item for instant paste
- Clipboard automatically closes
- Super quick workflow!

#### Clipboard Settings

```
Settings → Input Features → Clipboard History → Advanced
```

| Setting | Options | Default | Description |
|---------|---------|---------|-------------|
| **Max Items** | 20 / 50 / 100 | 50 | How many items to remember |
| **Auto-Delete** | Never / 7 days / 30 days | 30 days | When to remove old items |
| **Sync** | ON / OFF | OFF | Sync across devices (cloud) |
| **Encrypt** | ON / OFF | ON | Encrypt sensitive data |

---

## ⚙️ Settings Quick Reference

### Complete Settings Path

```
Android Settings
└── System
    └── Languages & input
        └── Virtual keyboard
            └── Kavi Keyboard
                └── Settings
                    └── Input Features
                        ├── Swipe Typing
                        │   ├── Enable swipe typing
                        │   ├── Sensitivity
                        │   ├── Show visual path
                        │   ├── Auto-space after swipe
                        │   └── Number of suggestions
                        │
                        ├── Gesture Controls
                        │   ├── Enable gesture controls
                        │   ├── Swipe to delete word
                        │   ├── Cursor movement
                        │   └── Sensitivity
                        │
                        └── Clipboard History
                            ├── Enable clipboard history
                            ├── Maximum items
                            ├── Auto-delete after
                            ├── Cloud sync
                            └── Encryption
```

---

## 🎓 Tutorial Mode (For New Users)

### First-Time Setup Wizard

When you first enable swipe typing or gestures, Kavi will show an interactive tutorial:

1. **Welcome Screen**
   - Explains what the feature does
   - Shows example animations

2. **Interactive Practice**
   - Try swiping "hello"
   - Try gesture controls
   - Practice clipboard operations

3. **Tips & Tricks**
   - Best practices
   - Common mistakes to avoid
   - Pro user shortcuts

4. **Completion**
   - Feature is now enabled
   - Access tutorial anytime from settings

---

## 🔧 Programmatic Access (For Developers)

If you're integrating these features programmatically:

### Enable/Disable via Code

```kotlin
// Get preferences instance
val prefs = KeyboardPreferences(context)

// Enable swipe typing
prefs.setSwipeTyping(true)

// Enable gesture controls
prefs.setGesturesEnabled(true)

// Enable specific gestures
prefs.setSwipeToDeleteEnabled(true)
prefs.setSwipeCursorMoveEnabled(true)

// Enable clipboard history
prefs.setClipboardHistoryEnabled(true)

// Adjust sensitivity (0.0 to 1.0)
prefs.setSwipeTypingSensitivity(0.7f)

// Show/hide swipe path
prefs.setSwipePathVisible(true)
```

### Check if Feature is Enabled

```kotlin
val prefs = KeyboardPreferences(context)

if (prefs.isSwipeTypingEnabled()) {
    // Initialize swipe typing components
    initializeSwipeTyping()
}

if (prefs.isGesturesEnabled()) {
    // Initialize gesture detector
    initializeGestureControls()
}

if (prefs.isClipboardHistoryEnabled()) {
    // Initialize clipboard manager
    initializeClipboardHistory()
}
```

### Listen for Setting Changes

```kotlin
val prefs = KeyboardPreferences(context)

val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
    when (key) {
        "swipe_typing" -> {
            if (prefs.isSwipeTypingEnabled()) {
                enableSwipeTyping()
            } else {
                disableSwipeTyping()
            }
        }
        "gestures_enabled" -> {
            if (prefs.isGesturesEnabled()) {
                enableGestures()
            } else {
                disableGestures()
            }
        }
        "clipboard_history" -> {
            if (prefs.isClipboardHistoryEnabled()) {
                enableClipboardHistory()
            } else {
                disableClipboardHistory()
            }
        }
    }
}

prefs.registerChangeListener(listener)
```

---

## 🎯 Usage Examples

### Example 1: Typing a Message with Swipe

**Scenario:** You want to type "Hello how are you today"

**Traditional Typing:**
- Tap: H-E-L-L-O → Space → H-O-W → Space → A-R-E → Space → Y-O-U → Space → T-O-D-A-Y
- **Total taps:** 25

**With Swipe Typing:**
- Swipe: "hello" → Swipe: "how" → Swipe: "are" → Swipe: "you" → Swipe: "today"
- **Total swipes:** 5
- **Time saved:** ~70%!

### Example 2: Correcting a Typo with Gestures

**Scenario:** You typed "Helllo" (extra 'l') and want to fix it

**Traditional Method:**
1. Tap and hold to position cursor
2. Tap backspace 3 times
3. Type "lo"

**With Gestures:**
1. Swipe left on backspace → deletes "Helllo"
2. Swipe type "hello" → done!
3. **Faster and easier!**

### Example 3: Pasting Multiple Items

**Scenario:** You need to paste your email, phone, and address

**Traditional Method:**
- Can only paste the last copied item
- Have to switch apps to copy each item again

**With Clipboard History:**
1. Open clipboard (all 3 items are there)
2. Tap email → pastes
3. Open clipboard again
4. Tap phone → pastes
5. Open clipboard again
6. Tap address → pastes
7. **Everything available instantly!**

---

## 🐛 Troubleshooting

### Swipe Typing Not Working

**Problem:** Swipe doesn't type words

**Solutions:**
1. ✅ Check if swipe typing is enabled
   - Settings → Input Features → Swipe Typing → ON
2. ✅ Restart the keyboard
   - Switch to another keyboard and back
3. ✅ Adjust sensitivity
   - Try "High" sensitivity for easier detection
4. ✅ Swipe slower
   - Fast swipes may not be detected
5. ✅ Update dictionary
   - Settings → Language → Download dictionary

### Gestures Not Detected

**Problem:** Swipe on backspace/spacebar doesn't work

**Solutions:**
1. ✅ Enable gesture controls
   - Settings → Input Features → Gesture Controls → ON
2. ✅ Swipe longer
   - Gestures need minimum swipe distance
3. ✅ Check individual gesture settings
   - Make sure specific gestures are enabled
4. ✅ Increase sensitivity
   - Settings → Gesture Sensitivity → High

### Clipboard Not Showing Items

**Problem:** Clipboard is empty even after copying

**Solutions:**
1. ✅ Enable clipboard history
   - Settings → Clipboard History → ON
2. ✅ Grant clipboard permission
   - Android Settings → Apps → Kavi → Permissions → Clipboard
3. ✅ Check filter settings
   - Make sure "All" category is selected
4. ✅ Clear search query
   - Empty the search box if filtering
5. ✅ Restart keyboard
   - Close and reopen Kavi keyboard

### Swipe Path Not Visible

**Problem:** Can't see the trail when swiping

**Solutions:**
1. ✅ Enable path visibility
   - Settings → Swipe Typing → Show Path → ON
2. ✅ Check theme compatibility
   - Some themes may hide the path
3. ✅ Update to latest version
   - Older versions may have display bugs

---

## 📊 Performance Tips

### For Best Swipe Typing Experience

1. **Use good quality touchscreen** - Old or damaged screens may not track well
2. **Clean your screen** - Dirt affects swipe detection
3. **Remove screen protector** - Some thick protectors reduce sensitivity
4. **Swipe at medium speed** - Not too fast, not too slow
5. **Practice common words** - Muscle memory improves accuracy

### For Smooth Clipboard

1. **Pin frequently used items** - Keeps them always accessible
2. **Clear old items regularly** - Prevents slowdown
3. **Use search for long lists** - Faster than scrolling
4. **Delete sensitive data** - Don't keep passwords in history
5. **Enable auto-delete** - Automatically removes old items

---

## 🔒 Privacy & Security

### What Data is Stored?

| Feature | Data Stored | Location | Security |
|---------|-------------|----------|----------|
| **Swipe Typing** | Learned words | Device only | Encrypted |
| **Gestures** | None | - | - |
| **Clipboard** | Copied text | Device only | Encrypted |

### Data Deletion

**To clear clipboard history:**
```
Settings → Clipboard History → Clear All
```

**To clear learned words:**
```
Settings → Swipe Typing → Clear Learned Words
```

**To reset all data:**
```
Settings → Advanced → Reset All Data
```

### Cloud Sync (Optional)

- Clipboard sync is **OFF by default**
- When enabled, data is **end-to-end encrypted**
- Only you can decrypt your data
- Syncs across your devices only

---

## 🚀 Pro Tips

### Power User Shortcuts

1. **Quick Clipboard Access**
   - Double-tap clipboard icon → Opens to search

2. **One-Handed Swipe Typing**
   - Swipe with thumb for faster one-handed typing

3. **Gesture Combos**
   - Swipe delete → Swipe type → Super fast corrections!

4. **Pin Templates**
   - Pin email signatures, addresses, common phrases

5. **Category Power Use**
   - Filter by Links when sharing URLs
   - Filter by Code when programming

### Speed Typing Techniques

1. **Chain swipes** - Don't lift between words (experimental)
2. **Partial swipes** - Just the first few letters (learns over time)
3. **Gesture muscle memory** - Practice gestures until automatic
4. **Clipboard favorites** - Pin your top 10 most-used snippets

---

## 📞 Support

### Need Help?

- **Documentation:** See `GESTURES_AND_CLIPBOARD_IMPLEMENTATION.md`
- **Issues:** https://github.com/cryptobhat/keely/issues
- **Discussions:** GitHub Discussions
- **Email:** support@kavikeyboard.com (if available)

### Feature Requests

Have ideas for improving these features? Open an issue on GitHub with the label `enhancement`.

---

## 📅 What's Coming Next?

### Planned Features

- ✨ AI-powered swipe prediction
- ✨ Multilingual swipe typing (Kannada support)
- ✨ Custom gesture creation
- ✨ Clipboard categories and folders
- ✨ OCR for clipboard (copy from images)
- ✨ Voice-to-clipboard
- ✨ Clipboard templates with variables

### Beta Testing

Want to try new features early?
```
Settings → About → Join Beta Program
```

---

## 🎉 Summary

You now know how to:
- ✅ Enable swipe typing, gestures, and clipboard
- ✅ Use each feature effectively
- ✅ Customize settings to your preference
- ✅ Troubleshoot common issues
- ✅ Access advanced features

**Happy typing!** 🎹✨
