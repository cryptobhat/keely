pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Kavi"

// Main application module
include(":app")

// Core modules - These are the foundation of the keyboard engine
// They contain the basic functionality that other modules depend on
include(":core:keyboard-engine")        // Main keyboard service and IME implementation
include(":core:input-method-service")   // Android InputMethodService wrapper
include(":core:layout-manager")         // Layout loading and switching system
include(":core:gesture-detector")       // Touch, swipe, and gesture handling
include(":core:common")                 // Shared utilities, extensions, and constants

// Feature modules - Each feature is independent and can be added/removed easily
// This modular approach makes the codebase easy to understand and maintain
include(":features:layouts")            // Keyboard layouts (Phonetic, Kavi, QWERTY)
include(":features:suggestion-engine")  // Smart suggestions and transliteration
include(":features:clipboard")          // Clipboard manager with undo/redo
include(":features:voice")              // Voice input and text-to-speech
include(":features:converter")          // ASCII to Unicode converter (Nudi/Baraha)
include(":features:themes")             // Theming system with Material You
include(":features:settings")           // Settings and preferences UI
include(":features:analytics")          // Analytics, DAU/MAU tracking
include(":features:notifications")      // Notification system

// Data layer modules - Handle database, network, and storage
include(":data:database")               // Room database for user history
include(":data:repositories")           // Data repositories and data sources
include(":data:preferences")            // DataStore preferences manager
include(":data:cache")                  // Caching layer for fast access

// UI modules - Reusable UI components
include(":ui:keyboard-view")            // Custom keyboard rendering engine
include(":ui:popup-views")              // Key popups and long-press menus
include(":ui:settings-ui")              // Settings screens using Compose
include(":ui:theme-preview")            // Theme customization and preview
