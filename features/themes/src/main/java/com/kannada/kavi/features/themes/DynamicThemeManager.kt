package com.kannada.kavi.features.themes

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

/**
 * DynamicThemeManager - Manages Material You Dynamic Colors
 * 
 * Observes system theme changes and provides dynamic color schemes
 * for the keyboard. Works on Android 12+ (API 31+) with Material You.
 * 
 * For devices without dynamic color support, falls back to static colors.
 */
class DynamicThemeManager(private val context: Context) {
    
    private val _keyboardColorScheme = MutableStateFlow<KeyboardColorScheme?>(null)
    val keyboardColorScheme: StateFlow<KeyboardColorScheme?> = _keyboardColorScheme.asStateFlow()
    
    private val _isDarkMode = MutableStateFlow(isSystemDarkMode(context))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    // Enable dynamic colors by default if available, will fallback gracefully
    // Dynamic colors are pre-loaded with static fallbacks for faster initialization
    private val _isDynamicColorEnabled = MutableStateFlow(false)
    val isDynamicColorEnabled: StateFlow<Boolean> = _isDynamicColorEnabled.asStateFlow()

    private val wallpaperManager: WallpaperManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) WallpaperManager.getInstance(context) else null

    private val wallpaperColorsListener =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) WallpaperManager.OnColorsChangedListener { _, _ ->
            android.util.Log.i("DynamicThemeManager", "Wallpaper colors changed")
            if (_isDynamicColorEnabled.value) {
                updateColorScheme()
            }
        } else null

    private var isWallpaperListenerRegistered = false
    
    // Broadcast receiver for system theme changes
    private val themeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_CONFIGURATION_CHANGED) {
                // System theme or configuration changed
                updateDarkMode()
            }
        }
    }
    
    private var isReceiverRegistered = false
    
    init {
        // Try to extract dynamic colors immediately (synchronous)
        val initialColors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                extractDynamicColorsDirectly(context, _isDarkMode.value)
            } catch (e: Exception) {
                android.util.Log.w("DynamicThemeManager", "Failed initial dynamic color extraction: ${e.message}")
                getStaticColorScheme(_isDarkMode.value)
            }
        } else {
            getStaticColorScheme(_isDarkMode.value)
        }

        // Apply colors immediately to both slots
        _keyboardColorScheme.value = initialColors
        KeyboardDesignSystem.setDynamicColorScheme(initialColors)
        KeyboardDesignSystem.setFallbackColorScheme(initialColors)

        // Register receiver for system theme changes
        registerThemeChangeReceiver()
        registerWallpaperChangeListener()

        android.util.Log.i("DynamicThemeManager", "Initialized with colors: isDark=${_isDarkMode.value}, dynamic available=${Build.VERSION.SDK_INT >= Build.VERSION_CODES.S}")
    }
    
    /**
     * Register broadcast receiver for system theme changes
     */
    private fun registerThemeChangeReceiver() {
        if (!isReceiverRegistered) {
            try {
                val filter = IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)
                context.registerReceiver(themeChangeReceiver, filter)
                isReceiverRegistered = true
            } catch (e: Exception) {
                android.util.Log.w("DynamicThemeManager", "Failed to register theme change receiver: ${e.message}")
            }
        }
    }
    
    private fun registerWallpaperChangeListener() {
        if (isWallpaperListenerRegistered || wallpaperColorsListener == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }
        try {
            wallpaperManager?.addOnColorsChangedListener(
                wallpaperColorsListener,
                Handler(Looper.getMainLooper())
            )
            isWallpaperListenerRegistered = true
        } catch (e: Exception) {
            android.util.Log.w("DynamicThemeManager", "Failed to register wallpaper colors listener: ${e.message}")
        }
    }
    
    /**
     * Unregister broadcast receiver
     * Call this when the manager is no longer needed
     */
    fun unregisterThemeChangeReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(themeChangeReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                android.util.Log.w("DynamicThemeManager", "Failed to unregister theme change receiver: ${e.message}")
            }
        }
        unregisterWallpaperChangeListener()
    }

    private fun unregisterWallpaperChangeListener() {
        if (!isWallpaperListenerRegistered || wallpaperColorsListener == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }
        try {
            wallpaperManager?.removeOnColorsChangedListener(wallpaperColorsListener)
        } catch (e: Exception) {
            android.util.Log.w("DynamicThemeManager", "Failed to unregister wallpaper colors listener: ${e.message}")
        } finally {
            isWallpaperListenerRegistered = false
        }
    }
    
    /**
     * Update color scheme based on current system theme (async)
     */
    suspend fun updateColorSchemeAsync() {
        // Always update dark mode state first
        updateDarkModeState()

        if (!_isDynamicColorEnabled.value) {
            // Fallback to static colors (properly respecting dark mode)
            val staticScheme = getStaticColorScheme(_isDarkMode.value)
            _keyboardColorScheme.value = staticScheme
            KeyboardDesignSystem.setDynamicColorScheme(null)
            return
        }

        // Extract dynamic colors from Material 3 using a temporary ComposeView
        // This is a workaround since dynamic color schemes require Compose context
        // May fail in IME service context - that's okay, we'll use static colors
        try {
            val colorScheme = withContext(Dispatchers.Main) {
                extractDynamicColorScheme(context, _isDarkMode.value)
            }
            _keyboardColorScheme.value = DynamicColorScheme.extractKeyboardColors(colorScheme)
        } catch (e: Exception) {
            // Fallback to static colors if dynamic colors fail (but with correct dark mode)
            // Don't disable - allow retry when window becomes available
            _keyboardColorScheme.value = getStaticColorScheme(_isDarkMode.value)
            // Log but don't disable - we'll retry when window is available
            android.util.Log.w("DynamicThemeManager", "Failed to extract dynamic colors: ${e.message}. Using static colors in ${if (_isDarkMode.value) "dark" else "light"} mode.")
        }
    }
    
    /**
     * Update color scheme synchronously (for immediate updates)
     */
    fun updateColorScheme() {
        // Always update dark mode state first
        updateDarkModeState()

        android.util.Log.i("DynamicThemeManager", "updateColorScheme() called")
        android.util.Log.i("DynamicThemeManager", "Dynamic enabled: ${_isDynamicColorEnabled.value}, Dark mode: ${_isDarkMode.value}")

        if (!_isDynamicColorEnabled.value) {
            // Fallback to static colors (with correct dark mode)
            android.util.Log.i("DynamicThemeManager", "Using STATIC colors (dynamic disabled) in ${if (_isDarkMode.value) "DARK" else "LIGHT"} mode")
            _keyboardColorScheme.value = getStaticColorScheme(_isDarkMode.value)
            return
        }
        // For sync updates, use static colors immediately with correct dark mode
        android.util.Log.i("DynamicThemeManager", "Setting STATIC colors temporarily, then extracting DYNAMIC colors...")
        _keyboardColorScheme.value = getStaticColorScheme(_isDarkMode.value)
        // Trigger async update to get dynamic colors
        kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.i("DynamicThemeManager", "Starting async dynamic color extraction...")
                updateColorSchemeAsync()
                android.util.Log.i("DynamicThemeManager", "✓ Async dynamic color extraction completed")
            } catch (e: Exception) {
                // If dynamic colors fail, keep static colors but don't disable
                // This allows retry when window becomes available
                android.util.Log.e("DynamicThemeManager", "✗ Failed to extract dynamic colors: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Update color scheme when window is available (call from onCreateInputView)
     * Now uses direct extraction which is more reliable
     */
    fun updateColorSchemeWithWindow(windowContext: android.content.Context) {
        // Always update dark mode state first
        updateDarkModeState()

        android.util.Log.i("DynamicThemeManager", "updateColorSchemeWithWindow called - Dark mode: ${_isDarkMode.value}, Dynamic enabled: ${_isDynamicColorEnabled.value}")

        val colorScheme = if (_isDynamicColorEnabled.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Try direct extraction first (synchronous and reliable)
                val directColors = extractDynamicColorsDirectly(windowContext, _isDarkMode.value)
                android.util.Log.i("DynamicThemeManager", "✓ Dynamic colors extracted directly")
                directColors
            } catch (e: Exception) {
                android.util.Log.w("DynamicThemeManager", "Direct extraction failed, using static: ${e.message}")
                getStaticColorScheme(_isDarkMode.value)
            }
        } else {
            getStaticColorScheme(_isDarkMode.value)
        }

        // Apply immediately to all slots
        _keyboardColorScheme.value = colorScheme
        KeyboardDesignSystem.setDynamicColorScheme(colorScheme)
        KeyboardDesignSystem.setFallbackColorScheme(colorScheme)

        android.util.Log.i("DynamicThemeManager", "Color scheme applied: isDynamic=${_isDynamicColorEnabled.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S}")
    }
    
    /**
     * Extract dynamic colors directly from system theme resources
     * More reliable than ComposeView for InputMethodService context
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun extractDynamicColorsDirectly(context: Context, isDark: Boolean): KeyboardColorScheme {
        return try {
            val resources = context.resources
            val theme = context.theme

            // Get Material You color resources (API 31+)
            val colorPrimary = if (isDark) {
                resources.getColor(android.R.color.system_accent1_600, theme)
            } else {
                resources.getColor(android.R.color.system_accent1_500, theme)
            }

            val colorSurface = if (isDark) {
                resources.getColor(android.R.color.system_neutral1_900, theme)
            } else {
                resources.getColor(android.R.color.system_neutral1_50, theme)
            }

            val colorBackground = if (isDark) {
                resources.getColor(android.R.color.system_neutral1_1000, theme)
            } else {
                resources.getColor(android.R.color.system_neutral1_10, theme)
            }

            val colorOnSurface = if (isDark) {
                resources.getColor(android.R.color.system_neutral1_100, theme)
            } else {
                resources.getColor(android.R.color.system_neutral1_900, theme)
            }

            // Build keyboard color scheme
            KeyboardColorScheme(
                keyBackground = colorSurface,
                keyPressed = ColorUtils.blendARGB(colorSurface, colorPrimary, if (isDark) 0.2f else 0.1f),
                keyText = colorOnSurface,
                specialKeyBackground = ColorUtils.blendARGB(colorSurface, colorPrimary, if (isDark) 0.15f else 0.08f),
                specialKeyPressed = ColorUtils.blendARGB(colorSurface, colorPrimary, 0.3f),
                specialKeyText = colorOnSurface,
                specialKeyIcon = colorOnSurface,
                actionKeyBackground = colorPrimary,
                actionKeyPressed = ColorUtils.blendARGB(colorPrimary, colorBackground, 0.2f),
                actionKeyText = if (isDark) {
                    resources.getColor(android.R.color.system_neutral1_900, theme)
                } else {
                    resources.getColor(android.R.color.system_neutral1_10, theme)
                },
                actionKeyIcon = if (isDark) {
                    resources.getColor(android.R.color.system_neutral1_900, theme)
                } else {
                    resources.getColor(android.R.color.system_neutral1_10, theme)
                },
                spacebarBackground = colorSurface,
                spacebarText = ColorUtils.setAlphaComponent(colorOnSurface, (255 * 0.6).toInt()),
                keyHintText = ColorUtils.setAlphaComponent(colorOnSurface, (255 * 0.5).toInt()),
                keyboardBackground = colorBackground,
                emojiFill = resources.getColor(android.R.color.system_accent1_200, theme),
                emojiOutline = colorPrimary,
                emojiEyes = colorOnSurface,
                emojiSmile = colorOnSurface
            )
        } catch (e: Exception) {
            android.util.Log.e("DynamicThemeManager", "Failed to extract dynamic colors directly: ${e.message}")
            getStaticColorScheme(isDark)
        }
    }

    /**
     * Extract dynamic color scheme using a temporary ComposeView
     * This is necessary because dynamic color schemes require Compose context
     *
     * Works best when called with a windowed context (like from onCreateInputView)
     */
    private suspend fun extractDynamicColorScheme(context: Context, isDark: Boolean): ColorScheme {
        android.util.Log.i("DynamicThemeManager", "extractDynamicColorScheme() called, isDark=$isDark")
        android.util.Log.i("DynamicThemeManager", "Context type: ${context.javaClass.simpleName}")

        var colorScheme: ColorScheme? = null
        var compositionError: Exception? = null

        // Check if context has a window
        val windowContext = if (context is android.app.Activity) {
            android.util.Log.i("DynamicThemeManager", "Using Activity context")
            context
        } else if (context is android.view.ContextThemeWrapper) {
            android.util.Log.i("DynamicThemeManager", "Using ContextThemeWrapper base context")
            context.baseContext
        } else {
            android.util.Log.i("DynamicThemeManager", "Using provided context directly")
            // For IME service, try to get window from the context
            // If it's a windowed context, it should work
            context
        }
        
        try {
            android.util.Log.i("DynamicThemeManager", "Creating ComposeView to extract colors...")

            // Try to create ComposeView and extract colors
            val view = ComposeView(windowContext).apply {
                setContent {
                    try {
                        android.util.Log.i("DynamicThemeManager", "Inside setContent, extracting color scheme...")
                        val scheme = if (isDark) {
                            android.util.Log.i("DynamicThemeManager", "Calling dynamicDarkColorScheme()")
                            dynamicDarkColorScheme(windowContext)
                        } else {
                            android.util.Log.i("DynamicThemeManager", "Calling dynamicLightColorScheme()")
                            dynamicLightColorScheme(windowContext)
                        }
                        colorScheme = scheme
                        android.util.Log.i("DynamicThemeManager", "✓ Color scheme extracted successfully!")
                    } catch (e: Exception) {
                        android.util.Log.e("DynamicThemeManager", "✗ Exception in setContent: ${e.message}", e)
                        compositionError = e
                    }
                    // Empty content, we just need the color scheme
                    Box {}
                }
            }
            android.util.Log.i("DynamicThemeManager", "ComposeView created")
            
            // Try to trigger composition
            try {
                // Post to main looper to ensure we're on UI thread
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                val latch = java.util.concurrent.CountDownLatch(1)
                
                handler.post {
                    try {
                        // Create a parent to attach the view (needed for composition)
                        val parent = android.widget.FrameLayout(windowContext).apply {
                            addView(view)
                            visibility = android.view.View.INVISIBLE
                        }
                        
                        // Force measure and layout to trigger composition
                        parent.measure(
                            android.view.View.MeasureSpec.makeMeasureSpec(100, android.view.View.MeasureSpec.EXACTLY),
                            android.view.View.MeasureSpec.makeMeasureSpec(100, android.view.View.MeasureSpec.EXACTLY)
                        )
                        parent.layout(0, 0, 100, 100)
                        
                        // Give composition a moment
                        handler.postDelayed({
                            latch.countDown()
                        }, 50)
                    } catch (e: Exception) {
                        compositionError = e
                        latch.countDown()
                    }
                }
                
                // Wait for composition (with timeout)
                android.util.Log.i("DynamicThemeManager", "Waiting for composition to complete...")
                var attempts = 0
                while (colorScheme == null && compositionError == null && attempts < 50) {
                    if (latch.await(10, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                        break
                    }
                    attempts++
                }
                android.util.Log.i("DynamicThemeManager", "Wait complete after $attempts attempts")
            } catch (e: Exception) {
                android.util.Log.e("DynamicThemeManager", "✗ Exception during composition trigger: ${e.message}", e)
                compositionError = e
            }
        } catch (e: Exception) {
            android.util.Log.e("DynamicThemeManager", "✗ Exception creating ComposeView: ${e.message}", e)
            compositionError = e
        }

        // If we got a color scheme, return it
        colorScheme?.let {
            android.util.Log.i("DynamicThemeManager", "✓✓✓ Successfully extracted dynamic colors!")
            return it
        }

        // Otherwise throw with the error
        val errorMsg = if (compositionError != null) {
            "Failed to extract color scheme: ${compositionError.message}"
        } else {
            "Failed to extract color scheme: No window context available"
        }
        android.util.Log.e("DynamicThemeManager", "✗✗✗ $errorMsg")
        throw compositionError ?: IllegalStateException(errorMsg)
    }
    
    /**
     * Check if system is in dark mode
     */
    private fun isSystemDarkMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }
    
    /**
     * Set dark mode manually (from settings)
     * This overrides system dark mode setting
     */
    fun setDarkMode(isDark: Boolean) {
        android.util.Log.i("DynamicThemeManager", "setDarkMode: current=${_isDarkMode.value}, new=$isDark")
        
        if (_isDarkMode.value != isDark) {
            _isDarkMode.value = isDark
            // Update color scheme - will use dynamic colors if enabled
            updateColorScheme()
        }
    }
    
    /**
     * Update dark mode state (call when system theme changes)
     * Also updates color scheme if dynamic colors are enabled
     */
    fun updateDarkMode() {
        val newDarkMode = isSystemDarkMode(context)

        android.util.Log.i("DynamicThemeManager", "updateDarkMode: current=${_isDarkMode.value}, new=$newDarkMode")

        if (_isDarkMode.value != newDarkMode) {
            _isDarkMode.value = newDarkMode
            // Update color scheme - will use dynamic colors if enabled
            updateColorScheme()
        } else if (_isDynamicColorEnabled.value) {
            // Even if dark mode didn't change, update colors in case wallpaper changed
            // This ensures dynamic colors stay in sync with system
            updateColorScheme()
        }
    }

    /**
     * Update dark mode state without triggering color scheme update
     */
    private fun updateDarkModeState() {
        _isDarkMode.value = isSystemDarkMode(context)
        android.util.Log.i("DynamicThemeManager", "Dark mode state updated: ${_isDarkMode.value}")
    }
    
    /**
     * Enable or disable dynamic colors
     */
    fun setDynamicColorEnabled(enabled: Boolean) {
        android.util.Log.i("DynamicThemeManager", "========== setDynamicColorEnabled($enabled) ==========")
        android.util.Log.i("DynamicThemeManager", "Device SDK: ${Build.VERSION.SDK_INT}")
        android.util.Log.i("DynamicThemeManager", "Dynamic color available: ${DynamicColorScheme.isDynamicColorAvailable(context)}")

        if (enabled && !DynamicColorScheme.isDynamicColorAvailable(context)) {
            // Can't enable on unsupported devices
            android.util.Log.w("DynamicThemeManager", "Dynamic colors not available on this device")
            _isDynamicColorEnabled.value = false
            val staticScheme = getStaticColorScheme(_isDarkMode.value)
            _keyboardColorScheme.value = staticScheme
            KeyboardDesignSystem.setDynamicColorScheme(null)
            return
        }
        val wasEnabled = _isDynamicColorEnabled.value
        _isDynamicColorEnabled.value = enabled

        android.util.Log.i("DynamicThemeManager", "Dynamic color enabled: $wasEnabled -> $enabled")
        android.util.Log.i("DynamicThemeManager", "Current dark mode: ${_isDarkMode.value}")

        // Force update if state changed or if enabling
        if (wasEnabled != enabled || enabled) {
            android.util.Log.i("DynamicThemeManager", "Triggering color scheme update...")
            updateColorScheme()
        } else {
            android.util.Log.i("DynamicThemeManager", "No update needed (state unchanged)")
        }
    }
    
    /**
     * Get static color scheme (fallback for devices without dynamic colors)
     */
    private fun getStaticColorScheme(isDark: Boolean): KeyboardColorScheme {
        val scheme = if (isDark) {
            KeyboardColorScheme(
                keyBackground = 0xFF1E1E1E.toInt(),
                keyPressed = 0xFF2E2E2E.toInt(),
                keyText = 0xFFFFFFFF.toInt(),
                specialKeyBackground = 0xFF2D2D2D.toInt(),
                specialKeyPressed = 0xFF3D3D3D.toInt(),
                specialKeyText = 0xFFFFFFFF.toInt(),
                specialKeyIcon = 0xFFFFFFFF.toInt(),
                actionKeyBackground = 0xFF3F8C80.toInt(),
                actionKeyPressed = 0xFF2F6C60.toInt(),
                actionKeyText = 0xFFFFFFFF.toInt(),
                actionKeyIcon = 0xFFFFFFFF.toInt(),
                spacebarBackground = 0xFF1E1E1E.toInt(),
                spacebarText = 0xFFAAAAAA.toInt(),
                keyHintText = 0xFF888888.toInt(),
                keyboardBackground = 0xFF121212.toInt(),
                emojiFill = 0xFFFFEB3B.toInt(),
                emojiOutline = 0xFFF9A825.toInt(),
                emojiEyes = 0xFFFFFFFF.toInt(),
                emojiSmile = 0xFFFFFFFF.toInt()
            )
        } else {
            KeyboardDesignSystem.getFallbackColorScheme()
        }
        return scheme
    }
}

