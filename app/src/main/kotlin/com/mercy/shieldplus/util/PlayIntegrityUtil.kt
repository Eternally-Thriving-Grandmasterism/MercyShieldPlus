package com.mercyshieldplus.util

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Base64

/**
 * PlayIntegrityUtil — Eternal Mercy Wrapper for Standard Play Integrity API (2026)
 * 
 * Handles nonce generation + async token request.
 * Init once in Application or ViewModel.
 * 
 * Requirements:
 * - Linked Google Cloud project (Play Console → App Integrity → Play Integrity API enabled)
 * - Cloud project number in request (long)
 * - Server-side verification recommended (full resilience)
 */
object PlayIntegrityUtil {
    private var integrityManager: IntegrityManager? = null

    /**
     * Initialize with app Context (call once, e.g., in Application.onCreate or ViewModel init)
     */
    fun init(context: Context) {
        if (integrityManager == null) {
            integrityManager = IntegrityManagerFactory.create(context.applicationContext)
        }
    }

    /**
     * Generate secure random nonce (24 bytes → URL-safe Base64, no padding)
     */
    fun generateNonce(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Request integrity token asynchronously
     * Returns token string on success, null on failure (with mercy logging possible)
     *
     * @param nonce Client-generated nonce (use generateNonce())
     * @param cloudProjectNumber Your Google Cloud project number (long) — from Play Console
     */
    suspend fun requestIntegrityToken(nonce: String, cloudProjectNumber: Long = YOUR_CLOUD_PROJECT_NUMBER_HERE): String? {
        val manager = integrityManager ?: return null.also { /* Mercy log: "IntegrityManager not initialized" */ }

        return try {
            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .setCloudProjectNumber(cloudProjectNumber)
                .build()

            val response = manager.requestIntegrityToken(request).await()
            response.token()
        } catch (e: Exception) {
            // Mercy handling: e.message (API error, network, etc.)
            null
        }
    }
}
