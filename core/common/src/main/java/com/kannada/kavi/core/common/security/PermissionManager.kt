package com.kannada.kavi.core.common.security

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * PermissionManager - Runtime Permission Handling
 *
 * Manages dangerous permissions that require runtime requests on Android 6.0+.
 *
 * DANGEROUS PERMISSIONS IN KAVI:
 * ==============================
 * 1. RECORD_AUDIO - Voice input feature
 * 2. VIBRATE - Haptic feedback (not dangerous, but managed here)
 * 3. INTERNET - API calls (not dangerous, but validated)
 *
 * WHY RUNTIME PERMISSIONS?
 * ========================
 * Android 6.0 (API 23+) requires apps to request dangerous permissions at runtime.
 * Simply declaring them in AndroidManifest.xml is not enough!
 *
 * User can:
 * - Grant permission
 * - Deny permission
 * - Deny with "Don't ask again"
 *
 * PERMISSION WORKFLOW:
 * ====================
 * 1. Check if permission is granted
 * 2. If not, check if we should show rationale
 * 3. Request permission from user
 * 4. Handle result (granted/denied)
 * 5. Provide fallback if denied
 *
 * USAGE EXAMPLE:
 * ==============
 * ```kotlin
 * // In Activity or Fragment
 * val permissionManager = PermissionManager(context)
 *
 * // Check if permission is granted
 * if (permissionManager.hasRecordAudioPermission()) {
 *     // Start voice input
 *     startVoiceRecording()
 * } else {
 *     // Request permission
 *     permissionManager.requestRecordAudioPermission(activity) { granted ->
 *         if (granted) {
 *             startVoiceRecording()
 *         } else {
 *             showVoiceInputUnavailableMessage()
 *         }
 *     }
 * }
 * ```
 */
class PermissionManager(private val context: Context) {

    companion object {
        // Permission request codes
        const val REQUEST_RECORD_AUDIO = 1001
        const val REQUEST_MULTIPLE_PERMISSIONS = 1002

        // Permission groups
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET
        )

        val OPTIONAL_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )

        // Callbacks for permission results
        private val permissionCallbacks = mutableMapOf<Int, (Boolean) -> Unit>()
    }

    // ========================================
    // PERMISSION CHECKS
    // ========================================

    /**
     * Check if RECORD_AUDIO permission is granted
     */
    fun hasRecordAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Pre-M: Permissions granted at install time
            true
        }
    }

    /**
     * Check if VIBRATE permission is granted
     * Note: This is a normal permission, always granted
     */
    fun hasVibratePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.VIBRATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if INTERNET permission is granted
     * Note: This is a normal permission, always granted
     */
    fun hasInternetPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if a specific permission is granted
     */
    fun hasPermission(permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Check if all required permissions are granted
     */
    fun hasAllRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { hasPermission(it) }
    }

    // ========================================
    // PERMISSION REQUESTS
    // ========================================

    /**
     * Request RECORD_AUDIO permission
     *
     * @param activity Activity to show permission dialog
     * @param callback Called with result (true = granted, false = denied)
     */
    fun requestRecordAudioPermission(
        activity: Activity,
        callback: (Boolean) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            callback(true)
            return
        }

        if (hasRecordAudioPermission()) {
            callback(true)
            return
        }

        // Store callback
        permissionCallbacks[REQUEST_RECORD_AUDIO] = callback

        // Request permission
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_RECORD_AUDIO
        )
    }

    /**
     * Request multiple permissions
     *
     * @param activity Activity to show permission dialog
     * @param permissions Array of permission strings
     * @param callback Called with result (true = all granted, false = any denied)
     */
    fun requestMultiplePermissions(
        activity: Activity,
        permissions: Array<String>,
        callback: (Boolean) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            callback(true)
            return
        }

        // Filter to only permissions that aren't granted yet
        val deniedPermissions = permissions.filter { !hasPermission(it) }

        if (deniedPermissions.isEmpty()) {
            callback(true)
            return
        }

        // Store callback
        permissionCallbacks[REQUEST_MULTIPLE_PERMISSIONS] = callback

        // Request permissions
        ActivityCompat.requestPermissions(
            activity,
            deniedPermissions.toTypedArray(),
            REQUEST_MULTIPLE_PERMISSIONS
        )
    }

    /**
     * Check if we should show rationale for a permission
     *
     * Returns true if:
     * - User previously denied the permission
     * - User didn't check "Don't ask again"
     *
     * Use this to show explanation before requesting permission again.
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        } else {
            false
        }
    }

    /**
     * Check if permission was permanently denied
     *
     * Returns true if user denied with "Don't ask again"
     */
    fun isPermanentlyDenied(activity: Activity, permission: String): Boolean {
        return !hasPermission(permission) && !shouldShowRationale(activity, permission)
    }

    // ========================================
    // RESULT HANDLING
    // ========================================

    /**
     * Handle permission request result
     *
     * Call this from Activity.onRequestPermissionsResult()
     *
     * @param requestCode The request code from onRequestPermissionsResult
     * @param permissions The permissions from onRequestPermissionsResult
     * @param grantResults The grant results from onRequestPermissionsResult
     */
    fun onPermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val callback = permissionCallbacks.remove(requestCode) ?: return

        val allGranted = grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        callback(allGranted)
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Get human-readable rationale for RECORD_AUDIO permission
     */
    fun getRecordAudioRationale(): String {
        return """
            Kavi needs microphone access for voice input feature.

            This allows you to:
            • Dictate text in Kannada
            • Use speech-to-text translation
            • Voice commands (future feature)

            Your voice is only processed when you tap the microphone button.
            Audio is sent to Bhashini API for translation and is not stored.
        """.trimIndent()
    }

    /**
     * Get permission status summary
     */
    fun getPermissionStatusSummary(): Map<String, Boolean> {
        return mapOf(
            "RECORD_AUDIO" to hasRecordAudioPermission(),
            "VIBRATE" to hasVibratePermission(),
            "INTERNET" to hasInternetPermission()
        )
    }

    /**
     * Clear all permission callbacks
     *
     * Call this when Activity is destroyed to avoid memory leaks
     */
    fun clearCallbacks() {
        permissionCallbacks.clear()
    }
}

/**
 * USAGE IN ACTIVITY:
 * ==================
 *
 * ```kotlin
 * class SettingsActivity : AppCompatActivity() {
 *     private lateinit var permissionManager: PermissionManager
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         permissionManager = PermissionManager(this)
 *
 *         // Check permission before using voice input
 *         voiceInputButton.setOnClickListener {
 *             if (permissionManager.hasRecordAudioPermission()) {
 *                 startVoiceInput()
 *             } else {
 *                 requestVoicePermission()
 *             }
 *         }
 *     }
 *
 *     private fun requestVoicePermission() {
 *         // Show rationale if needed
 *         if (permissionManager.shouldShowRationale(this, Manifest.permission.RECORD_AUDIO)) {
 *             showRationaleDialog()
 *             return
 *         }
 *
 *         // Request permission
 *         permissionManager.requestRecordAudioPermission(this) { granted ->
 *             if (granted) {
 *                 startVoiceInput()
 *             } else {
 *                 if (permissionManager.isPermanentlyDenied(this, Manifest.permission.RECORD_AUDIO)) {
 *                     showOpenSettingsDialog()
 *                 } else {
 *                     showPermissionDeniedMessage()
 *                 }
 *             }
 *         }
 *     }
 *
 *     override fun onRequestPermissionsResult(
 *         requestCode: Int,
 *         permissions: Array<out String>,
 *         grantResults: IntArray
 *     ) {
 *         super.onRequestPermissionsResult(requestCode, permissions, grantResults)
 *         permissionManager.onPermissionResult(requestCode, permissions, grantResults)
 *     }
 *
 *     override fun onDestroy() {
 *         super.onDestroy()
 *         permissionManager.clearCallbacks()
 *     }
 * }
 * ```
 *
 * BEST PRACTICES:
 * ===============
 *
 * 1. **Request permissions in context**
 *    - Don't request all permissions on app start
 *    - Request when user tries to use the feature
 *    - Explain why you need it
 *
 * 2. **Handle all cases**
 *    - Permission granted → Enable feature
 *    - Permission denied → Show explanation, request again
 *    - Permanently denied → Guide user to settings
 *
 * 3. **Provide fallbacks**
 *    - If voice input denied → Show manual input option
 *    - If vibration denied → Disable haptic feedback silently
 *
 * 4. **Never assume granted**
 *    - User can revoke permissions anytime
 *    - Always check before using protected API
 *
 * 5. **Clear callbacks**
 *    - Avoid memory leaks
 *    - Clear in onDestroy()
 */
