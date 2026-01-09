package com.mercyshieldplus.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mercyshieldplus.database.LogEntryEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import com.mercyshieldplus.util.SecurePassphraseManager
import zeroize.Zeroize.zeroize

/**
 * LogExportUtil — Plain + Encrypted Log Export Mercy
 *
 * Plain: Pretty JSON for easy viewing
 * Encrypted: AES-256-GCM using device Keystore-derived passphrase (hardware-backed)
 * Encrypted format: Base64(nonce (12 bytes) + ciphertext + tag (16 bytes))
 * File: .json for plain, .enc.txt for encrypted (text safe share)
 *
 * Decryption requires same device passphrase (advanced/user tool)
 */
object LogExportUtil {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    data class ExportLog(
        val timestamp: Long,
        val date: String,
        val type: String,
        val message: String
    )

    // Plain JSON export mercy
    fun exportPlainLogs(context: Context, logs: List<LogEntryEntity>): Uri {
        val exportLogs = logs.map { log ->
            ExportLog(
                timestamp = log.timestamp,
                date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                type = log.logType,
                message = log.message
            )
        }

        val json = gson.toJson(exportLogs)

        val fileName = "mercyshield_logs_${SimpleDateFormat("yyyyMMdd_HHMMss", Locale.getDefault()).format(Date())}.json"
        val file = File(context.cacheDir, fileName)
        file.writeText(json)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    // Encrypted export mercy (AES-256-GCM with device passphrase)
    fun exportEncryptedLogs(context: Context, logs: List<LogEntryEntity>): Uri {
        val exportLogs = logs.map { log ->
            ExportLog(
                timestamp = log.timestamp,
                date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                type = log.logType,
                message = log.message
            )
        }

        val json = gson.toJson(exportLogs).toByteArray(Charsets.UTF_8)

        // Device passphrase as AES-256 key mercy
        val passphraseChars = SecurePassphraseManager.getPassphrase(context)
        val keyBytes = String(passphraseChars).toByteArray(Charsets.UTF_8)  // 32 bytes direct key
        passphraseChars.fill('\u0000')  // Zeroize chars

        val key = SecretKeySpec(keyBytes, "AES")

        // Generate nonce
        val nonce = ByteArray(12)
        java.security.SecureRandom().nextBytes(nonce)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        val ciphertext = cipher.doFinal(json)

        // Combine nonce + ciphertext + tag (tag appended by doFinal)
        val encryptedData = nonce + ciphertext

        // Base64 for text-safe file
        val base64Encrypted = Base64.encodeToString(encryptedData, Base64.NO_WRAP)

        val fileName = "mercyshield_logs_encrypted_${SimpleDateFormat("yyyyMMdd_HHMMss", Locale.getDefault()).format(Date())}.enc.txt"
        val file = File(context.cacheDir, fileName)
        file.writeText(base64Encrypted)

        // Zeroize key bytes mercy
        keyBytes.fill(0)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    fun shareLogsUri(context: Context, uri: Uri, isEncrypted: Boolean = false) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (isEncrypted) "text/plain" else "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, if (isEncrypted) "MercyShieldPlus Encrypted Logs" else "MercyShieldPlus Logs Export")
            putExtra(Intent.EXTRA_TEXT, if (isEncrypted) "Encrypted with device key — secure mercy" else "")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Export Logs Mercy")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
