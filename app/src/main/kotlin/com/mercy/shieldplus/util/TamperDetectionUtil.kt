package com.mercyshieldplus.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.security.MessageDigest

/**
 * TamperDetectionUtil — Self-Tampering / Repack Verification Mercy
 *
 * Compares app signing certificate fingerprint (SHA-256) to expected release value.
 * Detects repacking, debug builds in release, or modified APKs.
 *
 * Expected fingerprint: Generate from release APK
 * adb shell dumpsys package com.mercyshieldplus | grep signature
 * Or keytool -list -printcert -jarfile app-release.apk
 *
 * Supports Signing V1/V2/V3/V4 — uses GET_SIGNING_CERTIFICATES (API 28+)
 * Fallback to GET_SIGNATURES for older.
 */
object TamperDetectionUtil {
    // Replace with your actual release signing cert SHA-256 fingerprint (uppercase hex)
    private const val EXPECTED_RELEASE_SHA256 = "YOUR_RELEASE_SHA256_FINGERPRINT_HERE"  // e.g., "A1:B2:C3:..."

    // Optional debug fingerprint (allow debug builds)
    private const val EXPECTED_DEBUG_SHA256 = "YOUR_DEBUG_SHA256_FINGERPRINT_HERE"  // Android Studio default or custom

    fun isAppTampered(context: Context): Boolean {
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

            if (signatures.isEmpty()) return true  // No signatures — tampered

            // Use first signature (multi-sign typical one)
            val cert = signatures[0].toByteArray()
            val digest = MessageDigest.getInstance("SHA-256").digest(cert)
            val fingerprint = digest.joinToString(":") { "%02X".format(it) }

            // Compare to expected (debug allow in dev)
            if (android.os.BuildConfig.DEBUG) {
                fingerprint != EXPECTED_DEBUG_SHA256
            } else {
                fingerprint != EXPECTED_RELEASE_SHA256
            }
        } catch (e: Exception) {
            Log.e("TamperDetection", "Signature check failed", e)
            true  // Error → assume tampered mercy
        }
    }

    fun getCurrentFingerprint(context: Context): String? {
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

            if (signatures.isEmpty()) return null

            val cert = signatures[0].toByteArray()
            val digest = MessageDigest.getInstance("SHA-256").digest(cert)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
