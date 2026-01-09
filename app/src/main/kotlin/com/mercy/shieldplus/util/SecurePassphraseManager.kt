package com.mercyshieldplus.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import kotlin.text.Charsets.UTF_8

/**
 * SecurePassphraseManager — Biometric-Authenticated Keystore MasterKey for SQLCipher Passphrase
 *
 * MasterKey requires user authentication (biometric preferred, fallback PIN/pattern).
 * Validity: 30 seconds after successful auth (adjustable).
 *
 * On first launch: Generate random 32-byte passphrase → store encrypted.
 * Subsequent: Access triggers BiometricPrompt if not recently authenticated.
 *
 * Security: Passphrase only available post-biometric → DB open protected.
 * Library auto-handles prompt on prefs access.
 */
object SecurePassphraseManager {
    private const val PREFS_NAME = "mercyshield_secure_prefs"
    private const val KEY_PASSPHRASE = "sqlcipher_passphrase_hex"

    private const val AUTH_VALIDITY_SECONDS = 30  // Post-auth validity window

    /**
     * Get passphrase char[] — triggers biometric if required
     * Caller (DB init) should handle potential delay/prompt
     */
    fun getPassphrase(context: Context): CharArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(AUTH_VALIDITY_SECONDS)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        var passphraseHex = prefs.getString(KEY_PASSPHRASE, null)

        if (passphraseHex == null) {
            // First launch after biometric setup — generate strong passphrase
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            passphraseHex = randomBytes.joinToString("") { "%02x".format(it) }

            with(prefs.edit()) {
                putString(KEY_PASSPHRASE, passphraseHex)
                apply()  // Sync — may trigger auth if required
            }
        }

        // Convert hex back to bytes → char[] for SQLCipher
        val passphraseBytes = ByteArray(passphraseHex.length / 2)
        for (i in passphraseBytes.indices) {
            val index = i * 2
            passphraseBytes[i] = passphraseHex.substring(index, index + 2).toInt(16).toByte()
        }

        return passphraseBytes.toString(UTF_8).toCharArray()
    }
}
