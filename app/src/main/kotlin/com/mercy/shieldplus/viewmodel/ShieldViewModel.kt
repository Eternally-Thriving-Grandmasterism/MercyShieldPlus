package com.mercyshieldplus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
// Import uniFFI generated: com.mercyshieldplus.MercyShieldPlus (assume generated)

class ShieldViewModel : ViewModel() {
    private val _shieldState = MutableStateFlow<ShieldState>(ShieldState.Loading)
    val shieldState: StateFlow<ShieldState> = _shieldState

    init {
        checkIntegrity()
    }

    fun checkIntegrity() {
        viewModelScope.launch {
            try {
                // Kotlin-side: Collect evidences (files, props, Play Integrity)
                val suspiciousFiles = listOf<String>()  // TODO: fs scans
                val suspiciousProps = listOf<String>()  // Build.PROP etc.
                val magisk = false                      // PackageManager check
                val playVerdict = "MEETS_DEVICE_INTEGRITY"  // TODO: Real Play Integrity API

                // Call Rust
                val report = MercyShieldPlus.evaluateIntegrity(
                    suspiciousFiles,
                    suspiciousProps,
                    magisk,
                    playVerdict
                )

                _shieldState.value = if (report.riskScore == 0u) {
                    ShieldState.Genuine(report.details)
                } else {
                    ShieldState.Anomaly(report.verdict.toString(), report.details, report.riskScore)
                }

                // Optional: Sign report with PQ for off-device
            } catch (e: Exception) {
                _shieldState.value = ShieldState.Error(e.message ?: "Integrity check failed")
            }
        }
    }
}

sealed class ShieldState {
    object Loading : ShieldState()
    data class Genuine(val details: List<String>) : ShieldState()
    data class Anomaly(val verdict: String, val details: List<String>, val risk: UByte) : ShieldState()
    data class Error(val message: String) : ShieldState()
}
