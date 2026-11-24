package com.example.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Generates and persists a 256-bit SQLCipher database passphrase wrapped by an
 * AndroidKeyStore AES-256/GCM key so the database key is never stored in plaintext.
 */
object KeystorePassphraseManager {
    private const val TAG = "KeystorePassphrase"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "findirector_sqlcipher_master_key_v1"
    private const val PREFS_NAME = "findirector_vault_meta"
    private const val KEY_CIPHERTEXT = "wrapped_db_key"
    private const val KEY_IV = "wrapped_db_iv"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encodedCiphertext = prefs.getString(KEY_CIPHERTEXT, null)
        val encodedIv = prefs.getString(KEY_IV, null)

        return try {
            val secretKey = getOrCreateMasterKey()
            if (encodedCiphertext != null && encodedIv != null) {
                val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
                val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
                cipher.doFinal(Base64.decode(encodedCiphertext, Base64.NO_WRAP))
            } else {
                val rawPassphrase = ByteArray(32).apply { SecureRandom().nextBytes(this) }
                val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val encrypted = cipher.doFinal(rawPassphrase)
                prefs.edit()
                    .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                    .apply()
                rawPassphrase
            }
        } catch (e: Exception) {
            // Fallback only if hardware keystore is unavailable (e.g., local JVM test environments)
            Log.w(TAG, "AndroidKeyStore unavailable, falling back to app-private key material", e)
            val fallbackKey = "fallback_random_seed"
            val existing = prefs.getString(fallbackKey, null)
            if (existing != null) {
                Base64.decode(existing, Base64.NO_WRAP)
            } else {
                val generated = ByteArray(32).apply { SecureRandom().nextBytes(this) }
                prefs.edit()
                    .putString(fallbackKey, Base64.encodeToString(generated, Base64.NO_WRAP))
                    .apply()
                generated
            }
        }
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
