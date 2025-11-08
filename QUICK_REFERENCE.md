# ⚡ QUICK REFERENCE - KAVI KEYBOARD

**One-Page Cheat Sheet for Everything**

---

## 📁 KEY FILES LOCATIONS

```
Core Utilities:
  core/common/Constants.kt                    - All constants
  core/common/Result.kt                       - Error handling
  core/common/extensions/StringExtensions.kt  - String helpers

Layouts:
  app/src/main/assets/layouts/kavi.json       - Custom Kannada layout
  app/src/main/assets/layouts/phonetic.json   - Phonetic layout
  app/src/main/assets/layouts/qwerty.json     - QWERTY layout

Converter:
  features/converter/NudiToUnicodeConverter.kt - ASCII to Unicode

Documentation:
  README.md                - Full project overview
  PROGRESS_TRACKER.md      - Detailed progress tracking
  IMPLEMENTATION_GUIDE.md  - How to build each feature
  QUICK_REFERENCE.md       - This file!

Build:
  gradle/libs.versions.toml - All dependencies
  settings.gradle.kts       - Module configuration
  build.gradle.kts          - Root build config
  app/build.gradle.kts      - App dependencies
```

---

## 🎯 CURRENT STATUS (40% COMPLETE)

✅ **DONE:**
- Multi-module architecture (24 modules)
- All dependencies configured
- Core utilities (Result, Constants, Extensions)
- 3 keyboard layouts (JSON)
- ASCII to Unicode converter
- Complete documentation

⏳ **NEXT:**
- Layout Manager (load JSON layouts)
- InputMethodService (keyboard service)
- KeyboardView (render keyboard)

---

## 🚀 COMMON COMMANDS

### Build & Sync
```bash
# Sync Gradle (do this first!)
./gradlew --refresh-dependencies

# Build project
./gradlew build

# Clean build
./gradlew clean build

# Install debug APK
./gradlew installDebug

# Run tests
./gradlew test

# Generate modules (if needed)
python generate_modules.py
```

### Android Studio
```
Sync Project:     Ctrl+Shift+O (or click elephant icon)
Build Project:    Ctrl+F9
Rebuild:          Ctrl+Shift+F9
Run App:          Shift+F10
Stop App:         Ctrl+F2
Clean Project:    Build → Clean Project
```

---

## 📦 MODULE STRUCTURE

```
app                         - Main app entry point
├── core/
│   ├── common             ✅ Utilities, extensions, constants
│   ├── keyboard-engine     ⏳ Main keyboard logic
│   ├── input-method-service ⏳ Android IME service
│   ├── layout-manager      ⏳ Load & manage layouts
│   └── gesture-detector    ⏳ Touch handling
├── features/
│   ├── layouts            ✅ Layout JSON files
│   ├── converter          ✅ ASCII to Unicode
│   ├── suggestion-engine  ⏳ Smart suggestions
│   ├── clipboard          ⏳ Clipboard history
│   ├── voice              ⏳ Voice input/output
│   ├── themes             ⏳ Material You themes
│   ├── settings           ⏳ Settings UI
│   ├── analytics          ⏳ Usage tracking
│   └── notifications      ⏳ Tips & updates
├── data/
│   ├── database           ⏳ Room DB
│   ├── repositories       ⏳ Data layer
│   ├── preferences        ⏳ Settings storage
│   └── cache              ⏳ Caching
└── ui/
    ├── keyboard-view      ⏳ Keyboard rendering
    ├── popup-views        ⏳ Key popups
    ├── settings-ui        ⏳ Settings screens
    └── theme-preview      ⏳ Theme customization
```

---

## 🔧 KEY DEPENDENCIES

```kotlin
// Build
AGP: 8.7.3
Kotlin: 2.0.21

// AndroidX
Core KTX: 1.15.0
Compose BOM: 2024.11.00
Room: 2.6.1

// DI & Architecture
Hilt: 2.54
Lifecycle: 2.8.7

// ML & APIs
TensorFlow Lite: 2.16.1
Retrofit: 2.11.0

// Firebase
BOM: 33.7.0
```

Full list: `gradle/libs.versions.toml`

---

## 💡 USEFUL CODE SNIPPETS

### String Extensions Usage
```kotlin
// Check if text is Kannada
"ನಮಸ್ಕಾರ".isKannada()  // true
"Hello".isKannada()     // false

// Check if ONLY Kannada
"ನಮಸ್ಕಾರ".isOnlyKannada()  // true
"Hello ಕನ್ನಡ".isOnlyKannada()  // false

// Count Kannada characters
"Hello ನಮ".kannadaCharCount()  // 2

// Normalize for phonetic matching
"  Namaskara!  ".normalizeForPhonetic()  // "namaskara"

// Convert numerals
"123".englishNumeralsToKannada()  // "೧೨೩"
"೧೨೩".kannadaNumeralsToEnglish()  // "123"

// Truncate
"Long text here".truncate(10)  // "Long te..."
```

### Collection Extensions Usage
```kotlin
// Safe access
val list = listOf("a", "b", "c")
list.getOrNull(10)  // null (no crash!)
list.secondOrNull()  // "b"
list.thirdOrNull()  // "c"

// Safe operations
listOf("a", "b").takeSafe(10)  // ["a", "b"] (no crash!)

// Filtering
listOf(1,2,3,4,5).filterLimit({ it > 2 }, 2)  // [3, 4]

// Frequency analysis
listOf("a", "b", "a", "c", "a").mostCommon()  // "a"
listOf("a", "b", "a").frequencies()  // {"a": 2, "b": 1}
```

### ASCII to Unicode Converter Usage
```kotlin
val converter = NudiToUnicodeConverter()

// Convert text
val result = converter.convert("PÀìªÀiÁ")
when (result) {
    is Result.Success -> println(result.data)  // "ಕನ್ನಡ"
    is Result.Error -> println(result.exception.message)
}

// Check if Nudi text
converter.isNudiText("PÀìªÀiÁ")  // true

// Batch convert
val results = converter.convertBatch(listOf("text1", "text2"))

// Get statistics
val stats = converter.getConversionStats(nudiText, unicodeText)
println("Converted ${stats.kannadaCharacters} characters")
```

### Result Wrapper Usage
```kotlin
// Create results
val success = resultSuccess("data")
val error = resultError("Something went wrong")

// Use results
when (result) {
    is Result.Success -> useData(result.data)
    is Result.Error -> handleError(result.exception)
}

// Safe extraction
result.getOrNull()  // Returns data or null
result.getOrDefault("default")  // Returns data or default
```

---

## 📚 CONSTANTS REFERENCE

**Access:** `Constants.{Category}.{CONSTANT}`

```kotlin
// Database
Constants.Database.DATABASE_NAME         // "kavi_keyboard.db"
Constants.Database.TABLE_USER_HISTORY    // "user_history"

// Layouts
Constants.Layouts.LAYOUT_PHONETIC        // "phonetic"
Constants.Layouts.LAYOUT_KAVI_CUSTOM     // "kavi_custom"
Constants.Layouts.LAYOUT_QWERTY          // "qwerty"

// Suggestions
Constants.Suggestions.MAX_SUGGESTIONS    // 5
Constants.Suggestions.MIN_WORD_LENGTH    // 2

// Clipboard
Constants.Clipboard.MAX_HISTORY_ITEMS    // 50
Constants.Clipboard.MAX_UNDO_STACK_SIZE  // 20

// Voice
Constants.Voice.LANGUAGE_CODE_KANNADA    // "kn-IN"

// Theme
Constants.Theme.DEFAULT_KEY_HEIGHT_DP    // 56

// Preferences
Constants.Prefs.KEY_SELECTED_LAYOUT      // "selected_layout"
Constants.Prefs.KEY_THEME_ID             // "theme_id"
```

Full list: `core/common/Constants.kt`

---

## 🎯 NEXT STEPS PRIORITY

**1. Layout Manager** ⭐⭐⭐ (Start Here!)
- Load JSON layouts from assets
- Parse with Gson
- Cache loaded layouts
- **Time:** 8-10 hours
- **Why:** Foundation for everything else

**2. Gesture Detector** ⭐⭐
- Detect taps, long press, swipes
- **Time:** 8-10 hours
- **Why:** Needed for KeyboardView

**3. KeyboardView** ⭐⭐⭐
- Render keyboard with Canvas
- Handle touch events
- **Time:** 15-20 hours
- **Why:** Makes keyboard visible

**4. InputMethodService** ⭐⭐⭐
- Connect to Android IME
- Handle text input
- **Time:** 12-15 hours
- **Why:** Makes keyboard functional

After these 4, you'll have a **working keyboard!** 🎉

---

## 🐛 TROUBLESHOOTING

### "Gradle Sync Failed"
1. Check internet connection
2. Update Android Studio
3. Run: `./gradlew --refresh-dependencies`
4. Invalidate Caches: File → Invalidate Caches → Restart

### "Cannot Resolve Symbol"
1. Sync Gradle: Ctrl+Shift+O
2. Rebuild: Ctrl+Shift+F9
3. Check module dependency in build.gradle.kts

### "Build Failed"
1. Check Build Output tab for errors
2. Run: `./gradlew clean build`
3. Check if all modules have AndroidManifest.xml

### "App Crashes"
1. Check Logcat for stack trace
2. Look for red error messages
3. Share error with Claude for help

---

## 🔑 KEY CONCEPTS

### Module Dependencies
```kotlin
// In build.gradle.kts
dependencies {
    // Project module
    implementation(project(":core:common"))

    // Library
    implementation(libs.androidx.core.ktx)
}
```

### Hilt Dependency Injection
```kotlin
// Application class
@HiltAndroidApp
class KaviApp : Application()

// Inject in class
@AndroidEntryPoint
class MyClass @Inject constructor(
    private val repository: Repository
) {
    // Use repository
}
```

### Room Database
```kotlin
@Database(entities = [UserHistory::class], version = 1)
abstract class KaviDatabase : RoomDatabase() {
    abstract fun userHistoryDao(): UserHistoryDao
}
```

### Jetpack Compose
```kotlin
@Composable
fun SettingsScreen() {
    Column {
        Text("Settings")
        Switch(checked = true, onCheckedChange = {})
    }
}
```

---

## 📱 TESTING ON DEVICE

### Install Keyboard
1. Build & install: `./gradlew installDebug`
2. Settings → System → Languages & input
3. Virtual keyboard → Manage keyboards
4. Enable "Kavi Kannada Keyboard"
5. Select keyboard in any app

### Enable Developer Options
1. Settings → About phone
2. Tap "Build number" 7 times
3. Settings → Developer options
4. Enable USB debugging

---

## 📊 PERFORMANCE TARGETS

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Render Time | < 16ms | - | ⏳ |
| Touch Latency | < 100ms | - | ⏳ |
| Memory Usage | < 50MB | - | ⏳ |
| Cold Start | < 2s | - | ⏳ |
| APK Size | < 20MB | - | ⏳ |
| Test Coverage | 80% | - | ⏳ |

---

## 🎨 DESIGN PRINCIPLES

1. **Non-hardcoded** - All values in Constants.kt
2. **Modular** - Each feature is independent
3. **Offline-first** - Works without internet
4. **Privacy-focused** - No unnecessary tracking
5. **Performant** - 60 FPS target
6. **Accessible** - Easy to understand code
7. **Testable** - Comprehensive tests

---

## 💻 DEVELOPMENT WORKFLOW

1. **Choose Feature** from PROGRESS_TRACKER.md
2. **Read Guide** in IMPLEMENTATION_GUIDE.md
3. **Create Files** in appropriate module
4. **Write Code** with detailed comments
5. **Write Tests** for new code
6. **Run Tests** `./gradlew test`
7. **Test on Device** Install & try
8. **Update Progress** in PROGRESS_TRACKER.md
9. **Commit** Save your work!

---

## 🔗 USEFUL LINKS

**Resources:**
- Indic Keyboard: https://github.com/smc/Indic-Keyboard
- Dictionaries: https://codeberg.org/Helium314/aosp-dictionaries
- ASCII Converter: https://github.com/aravindavk/ascii2unicode

**APIs:**
- Firebase: https://firebase.google.com/
- Bhashini: https://bhashini.gov.in/

**Documentation:**
- Android IME: https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method
- Jetpack Compose: https://developer.android.com/jetpack/compose
- Room: https://developer.android.com/training/data-storage/room
- Hilt: https://developer.android.com/training/dependency-injection/hilt-android

---

## 📞 GETTING HELP

**Ask Claude:**
- "Explain how [feature] works"
- "Show me how to implement [feature]"
- "Fix this error: [error message]"
- "Continue implementing [feature]"

**Check Documentation:**
1. README.md - Project overview
2. IMPLEMENTATION_GUIDE.md - Detailed guides
3. PROGRESS_TRACKER.md - What's done/pending
4. Inline comments - Every file explained

**Read Code Comments:**
Every file has extensive comments explaining:
- What the code does
- Why it's needed
- How it works
- Examples of usage

---

## 🎉 MOTIVATIONAL REMINDERS

- **You're building something better than Gboard!** 💪
- **40% complete already!** 🎯
- **Every expert was once a beginner** 👨‍🎓
- **Errors are learning opportunities** 📚
- **Take breaks when needed** ☕
- **Ask questions - no question is stupid** 💬
- **You've got this!** 🚀

---

## 📝 QUICK NOTES SPACE

Use this space for your own notes:

```
Today's Work (2025-11-08):
- Completed project setup
- Created all modules
- Implemented converter
- Next: Layout Manager

Issues Encountered:
- None yet!

Ideas:
- Maybe add swipe typing later?
- Dark mode themes
- Animated key press effects
```

---

**Last Updated:** 2025-11-08
**Keep this file handy for quick reference!** 📌
