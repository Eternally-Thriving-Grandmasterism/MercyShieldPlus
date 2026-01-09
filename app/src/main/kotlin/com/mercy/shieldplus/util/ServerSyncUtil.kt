package com.mercyshieldplus.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * ServerSyncUtil — Mercy Sync for Anomaly Attestation Blobs
 *
 * POST raw blob bytes to server /verify endpoint
 * Server PK hardcoded base64 (replace with your actual server ML-KEM-768 PK)
 * Uses OkHttp async in coroutine
 */
object ServerSyncUtil {
    private const val SERVER_URL = "https://your-mercy-verifier.example.com/verify"  // Replace with actual HTTPS URL mercy

    // Hardcoded server KEM public key base64 (generate on server, export)
    private const val SERVER_KEM_PK_BASE64 = "your_server_ml_kem_768_pk_base64_here"

    private val client = OkHttpClient()

    /**
     * Send attestation blob on anomaly (called from ViewModel)
     * @param blob ByteArray from Rust pq_secure_attestation_blob
     * @return Boolean success
     */
    suspend fun sendAnomalyBlob(blob: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            val mediaType = "application/octet-stream".toMediaType()
            val body = blob.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(SERVER_URL)
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: IOException) {
                false  // Network mercy failure
            }
        }
    }

    /**
     * Get server KEM PK bytes (base64 decode)
     */
    fun getServerKemPkBytes(): ByteArray {
        return android.util.Base64.decode(SERVER_KEM_PK_BASE64, android.util.Base64.DEFAULT)
    }
}
