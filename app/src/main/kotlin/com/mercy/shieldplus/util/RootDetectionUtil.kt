package com.mercyshieldplus.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

/**
 * RootDetectionUtil — Comprehensive Custom Root/Tampering Scans (2026 Best Practices)
 *
 * Supplements Play Integrity with file, package, and property checks.
 * Now includes dedicated KernelSU detection (kernel-based root, harder to hide).
 * Magisk/KernelSU/APatch share /data/adb, but KernelSU has specific traces.
 *
 * Techniques: rootbeer-inspired + 2026 updates for KernelSU (paths like /data/adb/kernelsu)
 */
object RootDetectionUtil {
    // Common classic root binary/file paths
    private val classicRootFilePaths = listOf(
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
        "/su/bin/su"
    )

    // Magisk-specific indicators
    private val magiskFilePaths = listOf(
        "/sbin/.magisk",
        "/sbin/.core/img",
        "/data/adb/magisk"
    )

    // KernelSU-specific indicators (2026 — kernel-based root traces)
    private val kernelSUFilePaths = listOf(
        "/data/adb/kernelsu",          // Primary KernelSU base directory
        "/data/adb/ksu",               // Alternative/alias
        "/data/adb/kernelsu/modules",  // Modules path
        "/data/adb/kernelsu/ap_list",  // App profile list
        "/proc/kernelsu"               // Proc entry if present
    )

    // Known root management packages
    private val rootPackages = listOf(
        "com.topjohnwu.magisk",        // Magisk
        "eu.chainfire.supersu",
        "me.phh.superuser",
        "com.koushikdutta.superuser"
        // KernelSU manager often separate/no fixed package (user-installed APK)
    )

    /**
     * Scan for all suspicious root files (classic + Magisk + KernelSU)
     */
    fun detectSuspiciousFiles(): List<String> {
        val found = mutableListOf<String>()
        val allPaths = classicRootFilePaths + magiskFilePaths + kernelSUFilePaths

        for (path in allPaths) {
            try {
                if (File(path).exists()) {
                    found.add(path)
                }
            } catch (ignored: Exception) {
                // Permission/SELinux deny — mercy ignore
            }
        }
        return found
    }

    /**
     * Detect installed root management apps (primarily Magisk/etc.; KernelSU manager varies)
     */
    fun detectRootPackages(context: Context): List<String> {
        val pm = context.packageManager
        val found = mutableListOf<String>()
        for (pkg in rootPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                found.add(pkg)
            } catch (ignored: PackageManager.NameNotFoundException) {}
        }
        return found
    }

    /**
     * Detect suspicious build props/tags
     */
    fun detectSuspiciousProps(): List<String> {
        val issues = mutableListOf<String>()
        if (Build.TAGS?.contains("test-keys", ignoreCase = true) == true) {
            issues.add("Build.TAGS contains test-keys")
        }

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
        } catch (ignored: Exception) {}

        return issues
    }

    /**
     * Specific Magisk indicators (files + packages)
     */
    fun isMagiskPresent(context: Context): Boolean {
        if (detectRootPackages(context).any { it == "com.topjohnwu.magisk" }) return true

        for (path in magiskFilePaths) {
            try {
                if (File(path).exists()) return true
            } catch (ignored: Exception) {}
        }
        return false
    }

    /**
     * Specific KernelSU indicators (2026 — checks primary traces)
     */
    fun isKernelSUPresent(): Boolean {
        for (path in kernelSUFilePaths) {
            try {
                if (File(path).exists()) return true
            } catch (ignored: Exception) {}
        }
        return false
    }

    /**
     * Generalized any kernel-level root (Magisk OR KernelSU)
     */
    fun isAnyKernelRootPresent(context: Context): Boolean {
        return isMagiskPresent(context) || isKernelSUPresent()
    }
}        for (pkg in rootPackages) {
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
