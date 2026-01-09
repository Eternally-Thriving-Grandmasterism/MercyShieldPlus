package com.mercyshieldplus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mercyshieldplus.util.PlayIntegrityUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// uniFFI generated mercy (adjust import)
import com.mercyshieldplus.MercyShieldPlus

class ShieldViewModel(application: Application) : AndroidViewModel(application) {
    private val _shieldState = MutableStateFlow<ShieldState>(ShieldState.Loading)
    val shieldState: StateFlow<ShieldState> = _shieldState

    init {
        PlayIntegrityUtil.init(getApplication())
        checkIntegrity()
    }

    fun refreshIntegrity() {
        checkIntegrity()  // Public for manual refresh mercy
    }

    private fun checkIntegrity() {
        viewModelScope.launch {
            try {
                // Evidences (expand: fs scans, props, packages eternal)
                val suspiciousFiles = emptyList<String>()  // TODO: Root file checks mercy
                val suspiciousProps = emptyList<String>()  // Build.PROPS etc.
                val magisk = false                         // PackageManager Magisk detect

                // Play Integrity token eternal
                val nonce = PlayIntegrityUtil.generateNonce()
                val cloudProjectNumber = 123456789012L  // Replace with your actual (or BuildConfig)
                val playToken = PlayIntegrityUtil.requestIntegrityToken(nonce, cloudProjectNumber) ?: "null_token"

                // Rust evaluate with token
                val report = MercyShieldPlus.evaluateIntegrity(
                    suspiciousFiles,
                    suspiciousProps,
                    magisk,
                    playToken
                )

                // JSON for optional PQ signing/blob
                val jsonReport = MercyShieldPlus.reportToJson(report)

                // UI state mercy
                _shieldState.value = when {
                    report.riskScore == 0u -> ShieldState.Genuine(report.details)
                    report.riskScore < 50u -> ShieldState.Suspicious(report.verdict.toString(), report.details, report.riskScore)
                    else -> ShieldState.Compromised(report.verdict.toString(), report.details, report.riskScore)
                }
            } catch (e: Exception) {
                _shieldState.value = ShieldState.Error(e.message ?: "Integrity fortress anomaly")
            }
        }
    }
}

sealed class ShieldState {
    object Loading : ShieldState()
    data class Genuine(val details: List<String>) : ShieldState()
    data class Suspicious(val verdict: String, val details: List<String>, val risk: UByte) : ShieldState()
    data class Compromised(val verdict: String, val details: List<String>, val risk: UByte) : ShieldState()
    data class Error(val message: String) : ShieldState()
}
