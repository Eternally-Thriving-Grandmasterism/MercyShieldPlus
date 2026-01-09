package com.mercyshieldplus.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

/**
 * RootDetectionUtil — Comprehensive Custom Root/Tampering Scans (2026 Best Practices)
 *
 * Supplements Play Integrity with file, package, and property checks.
 * Magisk/KernelSU/APatch hide many, but multi-layer catches advanced.
 * Returns lists for feeding to Rust evaluate_integrity.
 *
 * Techniques inspired by rootbeer lib + 2026 practices:
 * - Common su/busybox file paths existence
 * - Root management packages (Magisk, SuperSU, etc.)
 * - Build.TAGS test-keys
 * - Dangerous props (ro.debuggable=1, ro.secure=0)
 */
object RootDetectionUtil {
    // Common root binary/file paths (updated 2026 — includes Magisk remnants)
    private val rootFilePaths = listOf(
        "/system/app/Superuser.apk",
        "/system/app/SuperSU.apk",
        "/system/xbin/su",
        "/system/xbin/daemonsu",
        "/system/xbin/busybox",
        "/system/bin/su",
        "/system/bin/.ext",
        "/system/bin/failsafe/su",
        "/system/sd/xbin/su",
        "/system/usr/we-need-root/su",
        "/system/usr/su",
        "/sbin/su",
        "/vendor/bin/su",
        "/cache/su",
        "/data/local/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/dev/su",
        "/su/bin/su",
        // Magisk/KernelSU indicators (some hidden, but remnants possible)
        "/sbin/.magisk",
        "/sbin/.core/img",
        "/data/adb/magisk"
    )

    // Known root management packages (Magisk, etc.)
    private val rootPackages = listOf(
        "com.topjohnwu.magisk",
        "com.noshufou.android.su",
        "eu.chainfire.supersu",
        "me.phh.superuser",
        "com.koushikdutta.superuser"
    )

    /**
     * Scan for suspicious root files/directories
     * Returns list of found paths (empty if none)
     */
    fun detectSuspiciousFiles(): List<String> {
        val found = mutableListOf<String>()
        for (path in rootFilePaths) {
            try {
                if (File(path).exists()) {
                    found.add(path)
                }
            } catch (ignored: Exception) {
                // Access denied or security exception — ignore mercy
            }
        }
        return found
    }

    /**
     * Detect installed root management apps
     * Returns list of found package names
     */
    fun detectRootPackages(context: Context): List<String> {
        val pm = context.packageManager
        val found = mutableListOf<String>()
        for (pkg in rootPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                found.add(pkg)
            } catch (ignored: PackageManager.NameNotFoundException) {
                // Not installed — mercy
            }
        }
        return found
    }

    /**
     * Detect suspicious build props/tags
     * Returns list of issues (e.g., "test-keys", "ro.debuggable=1")
     */
    fun detectSuspiciousProps(): List<String> {
        val issues = mutableListOf<String>()
        if (Build.TAGS?.contains("test-keys", ignoreCase = true) == true) {
            issues.add("Build.TAGS contains test-keys")
        }

        // ro.debuggable and ro.secure (reflection mercy)
        try {
            val clazz = Class.forName("android.os.SystemProperties")
            val get = clazz.getMethod("get", String::class.java)
            val debuggable = get.invoke(null, "ro.debuggable") as String
            val secure = get.invoke(null, "ro.secure") as String

            if (debuggable == "1") {
                issues.add("ro.debuggable=1")
            }
            if (secure == "0") {
                issues.add("ro.secure=0")
            }
        } catch (ignored: Exception) {
            // Reflection failed — ignore
        }

        return issues
    }

    /**
     * Detect Magisk indicators (package + files combined)
     * Returns true if any Magisk trace found
     */
    fun isMagiskPresent(context: Context): Boolean {
        val magiskPackages = listOf("com.topjohnwu.magisk")
        val magiskFiles = listOf("/sbin/.magisk", "/data/adb/magisk")

        if (detectRootPackages(context).any { it in magiskPackages }) return true

        for (path in magiskFiles) {
            try {
                if (File(path).exists()) return true
            } catch (ignored: Exception) {}
        }

        return false
    }
}
