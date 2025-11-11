package com.kannada.kavi.data.repositories

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.kannada.kavi.core.common.Result
import com.kannada.kavi.data.preferences.KeyboardPreferences
import com.kannada.kavi.features.themes.KeyboardTheme
import com.kannada.kavi.features.themes.KeyboardColors
import com.kannada.kavi.features.themes.MaterialYouThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * ThemeRepository - Theme Management and Persistence
 *
 * Manages keyboard themes:
 * - Load built-in themes
 * - Save/load user-selected theme
 * - Generate dynamic themes from wallpaper
 * - Cache themes for performance
 *
 * THEME SOURCES:
 * =============
 * 1. **Built-in themes**: Material You defaults defined in Kotlin
 *    - KeyboardTheme.defaultLight()
 *    - KeyboardTheme.defaultDark()
 *
 * 2. **Dynamic themes**: Generated from wallpaper (Android 12+)
 *    - Extracts colors from wallpaper
 *    - Creates harmonious palette
 *
 * 3. **Default themes**: Hardcoded fallbacks
 *    - Used if assets fail to load
 *    - Always available
 *
 * THEME PERSISTENCE:
 * ==================
 * User's selected theme is saved to SharedPreferences:
 * - Theme ID (e.g., "material_you_light")
 * - Custom theme JSON (if user created custom theme)
 * - Dynamic theme preference
 *
 * USAGE:
 * ======
 * ```kotlin
 * val themeRepo = ThemeRepository(context)
 *
 * // Get current theme
 * val theme = themeRepo.getCurrentTheme()
 *
 * // Change theme
 * themeRepo.setTheme("material_you_dark")
 *
 * // Enable dynamic theming (Android 12+)
 * themeRepo.setDynamicTheme(true)
 * ```
 */
class ThemeRepository(
    private val context: Context,
    private val preferences: KeyboardPreferences
) {
    private val gson = Gson()

    // Theme cache for performance
    private val themeCache = mutableMapOf<String, KeyboardTheme>()

    // Built-in theme IDs
    companion object {
        const val THEME_LIGHT = "material_you_light"
        const val THEME_DARK = "material_you_dark"
        const val THEME_DYNAMIC_LIGHT = "dynamic_light"
        const val THEME_DYNAMIC_DARK = "dynamic_dark"

        // Theme preference keys
        private const val PREF_THEME_ID = "selected_theme_id"
        private const val PREF_DYNAMIC_THEME = "dynamic_theme_enabled"
        private const val PREF_CUSTOM_THEME = "custom_theme_json"
    }

    /**
     * Get current theme based on user preferences
     *
     * Priority:
     * 1. Custom theme (if user created one)
     * 2. Dynamic theme (if enabled and Android 12+)
     * 3. Selected theme ID
     * 4. Default light theme
     *
     * @return Current KeyboardTheme
     */
    suspend fun getCurrentTheme(): Result<KeyboardTheme> = withContext(Dispatchers.IO) {
        try {
            // Check if custom theme exists
            val customThemeJson = preferences.getString(PREF_CUSTOM_THEME, null)
            if (customThemeJson != null) {
                val customTheme = gson.fromJson(customThemeJson, KeyboardTheme::class.java)
                return@withContext Result.Success(customTheme)
            }

            // Check if dynamic theme is enabled
            val dynamicEnabled = preferences.getBoolean(PREF_DYNAMIC_THEME, false)
            if (dynamicEnabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val isDark = isSystemInDarkMode()
                val dynamicTheme = generateDynamicTheme(isDark)
                if (dynamicTheme != null) {
                    return@withContext Result.Success(dynamicTheme)
                }
            }

            // Get selected theme ID
            val themeId = preferences.getString(PREF_THEME_ID, THEME_LIGHT) ?: THEME_LIGHT

            // Load theme
            loadTheme(themeId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Load a theme by ID
     *
     * Checks cache first, then built-in defaults, then optional asset overrides.
     *
     * @param themeId Theme identifier
     * @return Theme or error
     */
    suspend fun loadTheme(themeId: String): Result<KeyboardTheme> = withContext(Dispatchers.IO) {
        try {
            // Check cache
            themeCache[themeId]?.let {
                return@withContext Result.Success(it)
            }

            // Load built-in theme or optional asset override
            val theme = when (themeId) {
                THEME_LIGHT -> loadThemeFromAssets("$themeId.json")
                    ?: KeyboardTheme.defaultLight()
                THEME_DARK -> loadThemeFromAssets("$themeId.json")
                    ?: KeyboardTheme.defaultDark()
                else -> loadThemeFromAssets("$themeId.json")
                    ?: KeyboardTheme.defaultLight()
            }

            // Cache for future use
            themeCache[themeId] = theme

            Result.Success(theme)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Set active theme
     *
     * @param themeId Theme ID to activate
     */
    suspend fun setTheme(themeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            preferences.putString(PREF_THEME_ID, themeId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Enable or disable dynamic theming
     *
     * @param enabled Whether to use wallpaper colors
     */
    suspend fun setDynamicTheme(enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            preferences.putBoolean(PREF_DYNAMIC_THEME, enabled)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Save custom theme
     *
     * @param theme Custom keyboard theme
     */
    suspend fun saveCustomTheme(theme: KeyboardTheme): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(theme)
            preferences.putString(PREF_CUSTOM_THEME, json)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Clear custom theme
     */
    suspend fun clearCustomTheme(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            preferences.putString(PREF_CUSTOM_THEME, null)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Get list of available themes
     *
     * @return List of theme IDs and names
     */
    suspend fun getAvailableThemes(): Result<List<ThemeInfo>> = withContext(Dispatchers.IO) {
        try {
            val themes = mutableListOf<ThemeInfo>()

            // Add built-in themes
            themes.add(ThemeInfo(THEME_LIGHT, "Material You Light", false))
            themes.add(ThemeInfo(THEME_DARK, "Material You Dark", true))

            // Add dynamic themes if supported
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                themes.add(ThemeInfo(THEME_DYNAMIC_LIGHT, "Dynamic Light", false, isDynamic = true))
                themes.add(ThemeInfo(THEME_DYNAMIC_DARK, "Dynamic Dark", true, isDynamic = true))
            }

            // Add custom theme if exists
            val customThemeJson = preferences.getString(PREF_CUSTOM_THEME, null)
            if (customThemeJson != null) {
                themes.add(ThemeInfo("custom", "Custom Theme", false, isCustom = true))
            }

            Result.Success(themes)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Generate dynamic theme from wallpaper
     *
     * @param isDark Whether to generate dark theme
     * @return Generated theme or null
     */
    private fun generateDynamicTheme(isDark: Boolean): KeyboardTheme? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return null
        }

        return runCatching {
            val manager = MaterialYouThemeManager(context)
            manager.setDarkMode(isDark)
            manager.setDynamicColorEnabled(true)
            val materialTheme = manager.currentTheme.value
            val colors = materialTheme.colors
            KeyboardTheme(
                id = if (isDark) THEME_DYNAMIC_DARK else THEME_DYNAMIC_LIGHT,
                name = if (isDark) "Dynamic Dark" else "Dynamic Light",
                isDark = isDark,
                colors = KeyboardColors(
                    background = colors.surfaceContainerHigh,
                    keyBackground = colors.surface,
                    keyForeground = colors.onSurface,
                    primaryKeyBackground = colors.primary,
                    primaryKeyForeground = colors.onPrimary,
                    accent = colors.secondary,
                    suggestion = colors.primary,
                    divider = colors.outlineVariant
                )
            )
        }.getOrNull()
    }

    /**
     * Load theme from assets folder
     *
     * @param filename Theme JSON filename
     * @return KeyboardTheme or null
     */
    private fun loadThemeFromAssets(filename: String): KeyboardTheme? {
        return try {
            val json = context.assets.open("themes/$filename").bufferedReader().use { it.readText() }
            gson.fromJson(json, KeyboardTheme::class.java)
        } catch (e: IOException) {
            null
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /**
     * Check if system is in dark mode
     *
     * @return true if dark mode
     */
    private fun isSystemInDarkMode(): Boolean {
        val uiMode = context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Clear theme cache
     *
     * Use when themes are updated or after loading new themes.
     */
    fun clearCache() {
        themeCache.clear()
    }
}

/**
 * ThemeInfo - Theme metadata
 *
 * Used to display available themes in settings UI.
 *
 * @property id Theme identifier
 * @property name Display name
 * @property mode Light or dark
 * @property isDynamic Whether theme uses wallpaper colors
 * @property isCustom Whether theme is user-created
 */
data class ThemeInfo(
    val id: String,
    val name: String,
    val isDark: Boolean, // Simplified from ThemeMode
    val isDynamic: Boolean = false,
    val isCustom: Boolean = false
)
