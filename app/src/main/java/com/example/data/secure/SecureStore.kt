package com.example.data.secure

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed secure storage using Android Keystore and AES-256-GCM encryption.
 * Encrypted values (IV + ciphertext in Base64) are stored in private SharedPreferences "riso_secure".
 */
class SecureStore(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    companion object {
        private const val TAG = "SecureStore"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "riso_master_key"
        private const val PREFS_NAME = "riso_secure"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }

    @Synchronized
    private fun getOrCreateMasterKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            val keyGenSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keyGenSpec)
            return keyGenerator.generateKey()
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: (keyStore.getKey(KEY_ALIAS, null) as SecretKey)
    }

    /**
     * Encrypts and stores a secret string in private SharedPreferences "riso_secure".
     */
    @Synchronized
    fun putSecret(key: String, value: String) {
        if (key.isBlank()) return
        if (value.isEmpty()) {
            deleteSecret(key)
            return
        }

        try {
            val secretKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv // 12 bytes for GCM
            val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

            val base64 = Base64.encodeToString(combined, Base64.NO_WRAP)
            prefs.edit().putString(key, base64).apply()
        } catch (e: Exception) {
            if (com.example.BuildConfig.DEBUG) {
                Log.e(TAG, "putSecret error", e)
            }
        }
    }

    /**
     * Retrieves and decrypts a secret string from private SharedPreferences "riso_secure".
     */
    @Synchronized
    fun getSecret(key: String): String? {
        if (key.isBlank()) return null
        val base64 = prefs.getString(key, null) ?: return null

        return try {
            val combined = Base64.decode(base64, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH_BYTES) {
                return null
            }

            val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val ciphertext = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)

            val secretKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val plaintext = cipher.doFinal(ciphertext)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            if (com.example.BuildConfig.DEBUG) {
                Log.e(TAG, "getSecret error", e)
            }
            null
        }
    }

    /**
     * Deletes a secret from SharedPreferences "riso_secure".
     */
    @Synchronized
    fun deleteSecret(key: String) {
        prefs.edit().remove(key).apply()
    }

    /**
     * Wipes all stored secrets from SharedPreferences and deletes the master key from Android Keystore.
     */
    @Synchronized
    fun wipeAll() {
        try {
            prefs.edit().clear().apply()
            if (com.example.BuildConfig.DEBUG) {
                Log.i(TAG, "wipeAll: cleared SharedPreferences")
            }
        } catch (e: Exception) {
            if (com.example.BuildConfig.DEBUG) {
                Log.e(TAG, "Error clearing SharedPreferences", e)
            }
        }

        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
                if (com.example.BuildConfig.DEBUG) {
                    Log.i(TAG, "wipeAll: deleted master key from AndroidKeyStore")
                }
            }
        } catch (e: Exception) {
            if (com.example.BuildConfig.DEBUG) {
                Log.e(TAG, "Error deleting master key from AndroidKeyStore", e)
            }
        }
    }

    /**
     * Returns true if a secret with the given key exists in SharedPreferences.
     */
    fun containsSecret(key: String): Boolean {
        return prefs.contains(key)
    }
}
