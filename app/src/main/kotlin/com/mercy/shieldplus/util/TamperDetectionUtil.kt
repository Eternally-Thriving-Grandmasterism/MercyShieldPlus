package com.mercyshieldplus.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * TamperDetectionUtil — Comprehensive Self-Tampering Detection Mercy
 *
 * Dual layer:
 * 1. Signing certificate fingerprint (SHA-256) — detects re-signing/repacking
 * 2. APK file checksum (SHA-256 of base.apk) — detects code modification
 *
 * Expected values: Generate from official release APK
 * - Signature: keytool -printcert -jarfile app-release.apk
 * - Checksum: sha256sum app-release.apk (or adb pull /data/app/.../base.apk)
 *
 * Debug builds allow debug values or skip checksum (path varies)
 */
object TamperDetectionUtil {
    // Replace with your official release values mercy
    private const val EXPECTED_RELEASE_SIG_SHA256 = "YOUR_RELEASE_CERT_SHA256_HERE"  // e.g., "A1:B2:C3:..."
    private const val EXPECTED_RELEASE_APK_SHA256 = "YOUR_RELEASE_APK_SHA256_HERE"  // Lowercase hex no colons

    // Optional debug values
    private const val EXPECTED_DEBUG_SIG_SHA256 = "YOUR_DEBUG_CERT_SHA256_HERE"
    private const val SKIP_CHECKSUM_IN_DEBUG = true  // Debug APK path varies — optional skip

    /**
     * Check signing certificate fingerprint
     */
    private fun isSignatureTampered(context: Context): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners ?: emptyArray()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isEmpty()) return true

            val cert = signatures[0].toByteArray()
            val digest = MessageDigest.getInstance("SHA-256").digest(cert)
            val fingerprint = digest.joinToString(":") { "%02X".format(it) }

            val expected = if (android.os.BuildConfig.DEBUG) EXPECTED_DEBUG_SIG_SHA256 else EXPECTED_RELEASE_SIG_SHA256
            fingerprint != expected
        } catch (e: Exception) {
            true  // Error → tampered mercy
        }
    }

    /**
     * Check APK file checksum (base.apk SHA-256)
     */
    private fun isApkChecksumTampered(context: Context): Boolean {
        if (android.os.BuildConfig.DEBUG && SKIP_CHECKSUM_IN_DEBUG) return false  // Debug mercy

        return try {
            val apkPath = context.packageCodePath  // /data/app/~~package~~/base.apk
            val file = File(apkPath)
            if (!file.exists()) return true

            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }

            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            hash != EXPECTED_RELEASE_APK_SHA256.lowercase()
        } catch (e: Exception) {
            true  // Error → tampered
        }
    }

    /**
     * Combined tamper detection
     */
    fun isAppTampered(context: Context): Boolean {
        val sigTampered = isSignatureTampered(context)
        val checksumTampered = isApkChecksumTampered(context)
        return sigTampered || checksumTampered
    }

    /**
     * Get details for logging/reporting
     */
    fun getTamperDetails(context: Context): List<String> {
        val details = mutableListOf<String>()
        if (isSignatureTampered(context)) details.add("Signing certificate mismatch — possible re-signing")
        if (isApkChecksumTampered(context)) details.add("APK checksum mismatch — possible code tampering")
        return details
    }
}            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners ?: emptyArray()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isEmpty()) return null

            val cert = signatures[0].toByteArray()
            val digest = MessageDigest.getInstance("SHA-256").digest(cert)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
