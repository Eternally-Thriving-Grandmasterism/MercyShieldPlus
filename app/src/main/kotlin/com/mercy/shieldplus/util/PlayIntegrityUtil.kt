package com.mercyshieldplus.util

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

/**
 * PlayIntegrityUtil — Eternal Mercy Wrapper for Play Integrity Standard API (2026 v1.6.0)
 *
 * Standard API: Low latency, on-demand, automatic protections (no nonce needed).
 * Token fetched client-side, raw token passed for server verification (full verdicts).
 * Optional requestHash for binding to action/data (anti-tampering).
 *
 * Requirements:
 * - Play Integrity API enabled in Google Cloud project.
 * - Cloud project linked in Play Console (App integrity).
 * - Project number (long) configured.
 */
object PlayIntegrityUtil {
    private var integrityManager: IntegrityManager? = null

    /**
     * Initialize with app Context (call once early, e.g., Application.onCreate)
     */
    fun init(context: Context) {
        if (integrityManager == null) {
            integrityManager = IntegrityManagerFactory.create(context.applicationContext)
        }
    }

    /**
     * Optional: Generate binding hash (SHA256 of action data string)
     * Use to bind token to specific request/context.
     */
    private fun generateRequestHash(data: String): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
    }

    /**
     * Request Standard integrity token asynchronously
     *
     * @param cloudProjectNumber Your linked Google Cloud project number (long)
     * @param bindData Optional string to bind token (e.g., "integrity_check_${System.currentTimeMillis()}")
     * @return Raw token string on success, "null_token" on failure
     */
    suspend fun requestIntegrityToken(
        cloudProjectNumber: Long,
        bindData: String? = null
    ): String {
        val manager = integrityManager ?: return "null_token".also { /* Mercy log: "Manager not initialized" */ }

        return try {
            val builder = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(cloudProjectNumber)

            bindData?.let {
                builder.setRequestHash(generateRequestHash(it))
            }

            val request = builder.build()

            val response: IntegrityTokenResponse = manager.requestIntegrityToken(request).await()
            response.token() ?: "null_token"
        } catch (e: Exception) {
            // Mercy handling: network/API errors
            "null_token"
        }
    }
}
