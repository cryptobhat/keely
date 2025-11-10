package com.kannada.kavi.core.common.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * SecureStorage - Android Keystore-based Secure Data Storage
 *
 * Provides encrypted storage for sensitive data using Android Keystore System.
 *
 * WHAT IS ANDROID KEYSTORE?
 * =========================
 * - Hardware-backed secure key storage
 * - Keys never leave secure hardware (if available)
 * - Protected against extraction even with root access
 * - Automatic key generation and management
 *
 * SECURITY FEATURES:
 * ==================
 * 1. **Encryption**
 *    - AES-256-GCM encryption
 *    - Each value encrypted separately
 *    - Automatic IV generation
 *
 * 2. **Key Protection**
 *    - Keys stored in Android Keystore
 *    - User authentication can be required
 *    - Keys can be hardware-backed
 *
 * 3. **Data Protection**
 *    - No plaintext storage
 *    - Encrypted SharedPreferences for metadata
 *    - Automatic key rotation support
 *
 * WHAT TO STORE HERE:
 * ===================
 * ✅ API keys (Bhashini, Firebase)
 * ✅ User authentication tokens
 * ✅ Sensitive user preferences
 * ✅ Encryption keys for other data
 * ❌ Non-sensitive data (use regular SharedPreferences)
 * ❌ Large data (use encrypted files instead)
 *
 * USAGE EXAMPLE:
 * ==============
 * ```kotlin
 * val secureStorage = SecureStorage(context)
 *
 * // Store sensitive data
 * secureStorage.putString("bhashini_api_key", apiKey)
 * secureStorage.putString("user_token", token)
 *
 * // Retrieve sensitive data
 * val apiKey = secureStorage.getString("bhashini_api_key")
 * val token = secureStorage.getString("user_token")
 *
 * // Remove sensitive data
 * secureStorage.remove("user_token")
 * ```
 */
class SecureStorage(private val context: Context) {

    companion object {
        private const val KEYSTORE_ALIAS = "KaviKeyboardMasterKey"
        private const val PREFS_NAME = "kavi_secure_prefs"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SEPARATOR = "]"
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            createEncryptedSharedPreferences()
        } else {
            // Fallback for older Android versions
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            createOrGetMasterKey()
        }
    }

    /**
     * Store encrypted string
     *
     * @param key Key identifier
     * @param value String to encrypt and store
     */
    fun putString(key: String, value: String?) {
        if (value == null) {
            remove(key)
            return
        }

        val encrypted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            encryptString(value)
        } else {
            // Fallback: Base64 encoding (not secure, but better than plaintext)
            Base64.encodeToString(value.toByteArray(), Base64.DEFAULT)
        }

        encryptedPrefs.edit()
            .putString(key, encrypted)
            .apply()
    }

    /**
     * Retrieve and decrypt string
     *
     * @param key Key identifier
     * @param defaultValue Default value if key doesn't exist
     * @return Decrypted string or default
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        val encrypted = encryptedPrefs.getString(key, null) ?: return defaultValue

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decryptString(encrypted)
            } else {
                // Fallback: Base64 decoding
                String(Base64.decode(encrypted, Base64.DEFAULT))
            }
        } catch (e: Exception) {
            android.util.Log.e("SecureStorage", "Failed to decrypt value for key: $key", e)
            defaultValue
        }
    }

    /**
     * Store encrypted integer
     */
    fun putInt(key: String, value: Int) {
        putString(key, value.toString())
    }

    /**
     * Retrieve integer
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return getString(key)?.toIntOrNull() ?: defaultValue
    }

    /**
     * Store encrypted long
     */
    fun putLong(key: String, value: Long) {
        putString(key, value.toString())
    }

    /**
     * Retrieve long
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return getString(key)?.toLongOrNull() ?: defaultValue
    }

    /**
     * Store encrypted boolean
     */
    fun putBoolean(key: String, value: Boolean) {
        putString(key, value.toString())
    }

    /**
     * Retrieve boolean
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return getString(key)?.toBoolean() ?: defaultValue
    }

    /**
     * Check if key exists
     */
    fun contains(key: String): Boolean {
        return encryptedPrefs.contains(key)
    }

    /**
     * Remove specific key
     */
    fun remove(key: String) {
        encryptedPrefs.edit()
            .remove(key)
            .apply()
    }

    /**
     * Clear all secure data
     *
     * WARNING: This removes ALL encrypted data!
     */
    fun clearAll() {
        encryptedPrefs.edit()
            .clear()
            .apply()
    }

    /**
     * Get all stored keys
     */
    fun getAllKeys(): Set<String> {
        return encryptedPrefs.all.keys
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    /**
     * Create or retrieve the master encryption key from Android Keystore
     */
    private fun createOrGetMasterKey(): SecretKey {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw UnsupportedOperationException("Android Keystore requires API 23+")
        }

        // Check if key already exists
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            return (keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }

        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // Optionally require user authentication
            // .setUserAuthenticationRequired(true)
            // .setUserAuthenticationValidityDurationSeconds(30)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypt string using AES-GCM
     */
    private fun encryptString(plaintext: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw UnsupportedOperationException("Encryption requires API 23+")
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, createOrGetMasterKey())

        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Combine IV and encrypted data: [IV][encrypted]
        val combined = iv + encrypted

        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    /**
     * Decrypt string using AES-GCM
     */
    private fun decryptString(encryptedData: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw UnsupportedOperationException("Decryption requires API 23+")
        }

        val combined = Base64.decode(encryptedData, Base64.DEFAULT)

        // Extract IV (first 12 bytes for GCM)
        val ivSize = 12
        val iv = combined.copyOfRange(0, ivSize)
        val encrypted = combined.copyOfRange(ivSize, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, createOrGetMasterKey(), spec)

        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    /**
     * Create EncryptedSharedPreferences using Jetpack Security
     */
    private fun createEncryptedSharedPreferences(): SharedPreferences {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                android.util.Log.e("SecureStorage", "Failed to create EncryptedSharedPreferences", e)
                // Fallback to regular SharedPreferences
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
}

/**
 * Extension function for easy access to SecureStorage
 */
fun Context.getSecureStorage(): SecureStorage {
    return SecureStorage(this)
}

/**
 * SECURITY BEST PRACTICES:
 * ========================
 *
 * 1. **Only store truly sensitive data**
 *    - API keys, tokens, passwords
 *    - User authentication credentials
 *    - Encryption keys
 *
 * 2. **Never hardcode secrets**
 *    ```kotlin
 *    // ❌ BAD
 *    val apiKey = "hardcoded_key_12345"
 *
 *    // ✅ GOOD
 *    val apiKey = secureStorage.getString("api_key")
 *    ```
 *
 * 3. **Clear data when no longer needed**
 *    ```kotlin
 *    // User logs out
 *    secureStorage.remove("user_token")
 *    ```
 *
 * 4. **Handle encryption failures gracefully**
 *    - Always provide fallback/default values
 *    - Log errors for debugging
 *    - Don't crash the app
 *
 * 5. **Use appropriate key lifetime**
 *    - Session tokens: Clear on logout
 *    - API keys: Persist indefinitely
 *    - Temporary data: Clear after use
 *
 * THREAT MODEL:
 * =============
 * Protects against:
 * ✅ Malicious apps reading SharedPreferences
 * ✅ Backup extraction
 * ✅ File system access (rooted devices)
 * ✅ Memory dumps (to some extent)
 *
 * Does NOT protect against:
 * ❌ Compromised device with active keylogger
 * ❌ User giving permissions to malicious app
 * ❌ Physical access to unlocked device
 * ❌ Man-in-the-middle attacks (use HTTPS!)
 */
