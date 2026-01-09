package com.mercyshieldplus.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import kotlin.text.Charsets.UTF_8

/**
 * SecurePassphraseManager — Keystore-Backed Secure Storage for SQLCipher Passphrase
 *
 * Uses EncryptedSharedPreferences (AndroidX Security Crypto) which automatically:
 * - Creates/generates a Keystore-backed AES-256-GCM MasterKey (hardware-backed on capable devices)
 * - Encrypts values at rest
 *
 * On first launch: Generates strong random 32-byte passphrase → hex string → store encrypted
 * Subsequent: Retrieve decrypted passphrase string → toCharArray() for SQLCipher
 *
 * Passphrase never hardcoded or plain — protected by Keystore (tamper-resistant, biometric optional later)
 */
object SecurePassphraseManager {
    private const val PREFS_NAME = "mercyshield_secure_prefs"
    private const val KEY_PASSPHRASE = "sqlcipher_passphrase_hex"

    /**
     * Initialize and get passphrase char[] (zeroizes after use recommended)
     */
    fun getPassphrase(context: Context): CharArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
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
            // First launch — generate strong random 32-byte passphrase
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            passphraseHex = randomBytes.joinToString("") { "%02x".format(it) }

            with(prefs.edit()) {
                putString(KEY_PASSPHRASE, passphraseHex)
                apply()
            }
        }

        // Convert hex string back to char[] for SQLCipher
        val passphraseBytes = ByteArray(passphraseHex.length / 2)
        for (i in passphraseBytes.indices) {
            val index = i * 2
            passphraseBytes[i] = passphraseHex.substring(index, index + 2).toInt(16).toByte()
        }

        return passphraseBytes.toString(UTF_8).toCharArray()
    }
}
