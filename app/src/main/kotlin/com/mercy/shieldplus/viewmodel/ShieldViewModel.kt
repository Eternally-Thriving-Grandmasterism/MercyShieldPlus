package com.mercyshieldplus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mercyshieldplus.database.AppDatabase
import com.mercyshieldplus.database.IntegrityReportEntity
import com.mercyshieldplus.database.LogEntryEntity
import com.mercyshieldplus.util.PlayIntegrityUtil
import com.mercyshieldplus.util.RootDetectionUtil
import com.mercyshieldplus.util.ServerSyncUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// uniFFI mercy
import com.mercyshieldplus.MercyShieldPlus

data class IntegrityReportEntry(
    val timestamp: Long,
    val isGenuine: Boolean,
    val isAnomaly: Boolean = !isGenuine,
    val details: List<String>,
    val riskScore: UByte,
    val verdict: String,
    var expanded: Boolean = false,
    val onToggleExpand: () -> Unit
)

class ShieldViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.integrityDao()

    private val _shieldState = MutableStateFlow<ShieldState>(ShieldState.Loading)
    val shieldState: StateFlow<ShieldState> = _shieldState.asStateFlow()

    private val _integrityHistory = MutableStateFlow<List<IntegrityReportEntry>>(emptyList())
    val integrityHistory: StateFlow<List<IntegrityReportEntry>> = _integrityHistory.asStateFlow()

    private val gson = Gson()

    init {
        PlayIntegrityUtil.init(getApplication())
        loadHistoryFromDb()
        logEvent("INFO", "App started — fortress awakening")
        checkIntegrity()
    }

    fun refreshIntegrity() {
        logEvent("INFO", "Manual integrity refresh triggered")
        checkIntegrity()
    }

    private fun logEvent(type: String, message: String) {
        viewModelScope.launch {
            dao.insertLog(LogEntryEntity(logType = type, message = message))
        }
    }

    private fun loadHistoryFromDb() {
        viewModelScope.launch {
            dao.getAllHistory().collect { entities ->
                val entries = entities.map { entity ->
                    val details = gson.fromJson<List<String>>(
                        entity.detailsJson,
                        object : TypeToken<List<String>>() {}.type
                    ) ?: emptyList()

                    IntegrityReportEntry(
                        timestamp = entity.timestamp,
                        isGenuine = entity.isGenuine,
                        details = details,
                        riskScore = entity.riskScore.toUByte(),
                        verdict = entity.verdict,
                        onToggleExpand = {
                            toggleExpanded(entity.timestamp)
                        }
                    )
                }
                _integrityHistory.value = entries
            }
        }
    }

    private fun toggleExpanded(timestamp: Long) {
        _integrityHistory.value = _integrityHistory.value.map {
            if (it.timestamp == timestamp) it.copy(expanded = !it.expanded) else it
        }
    }

    private fun checkIntegrity() {
        viewModelScope.launch {
            try {
                logEvent("INFO", "Integrity check started")

                val suspiciousFiles = RootDetectionUtil.detectSuspiciousFiles()
                val suspiciousProps = RootDetectionUtil.detectSuspiciousProps()
                val anyKernelRoot = RootDetectionUtil.isAnyKernelRootPresent(getApplication())

                val bindData = "integrity_check_${System.currentTimeMillis()}"
                val cloudProjectNumber = 123456789012L
                val playToken = PlayIntegrityUtil.requestIntegrityToken(cloudProjectNumber, bindData)

                val report = MercyShieldPlus.evaluateIntegrity(
                    suspiciousFiles,
                    suspiciousProps,
                    anyKernelRoot,
                    playToken
                )

                val jsonReport = MercyShieldPlus.reportToJson(report)
                val detailsJson = gson.toJson(report.details)

                // Persist report
                val entity = IntegrityReportEntity(
                    isGenuine = report.riskScore == 0u,
                    detailsJson = detailsJson,
                    riskScore = report.riskScore.toInt(),
                    verdict = when {
                        report.riskScore == 0u -> "Genuine"
                        report.riskScore < 50u -> "Suspicious"
                        else -> "Compromised"
                    }
                )
                dao.insert(entity)

                logEvent(if (report.riskScore == 0u) "INFO" else "ANOMALY", "Integrity check completed — risk: ${report.riskScore}")

                // Anomaly sync mercy
                if (report.riskScore > 0u) {
                    val serverPkBytes = ServerSyncUtil.getServerKemPkBytes()
                    val blob = MercyShieldPlus.pqSecureAttestationBlob(
                        jsonReport.toByteArray(),
                        serverPkBytes,
                        null  // Add local SK when persisted
                    )

                    val syncSuccess = ServerSyncUtil.sendAnomalyBlob(blob)
                    logEvent(if (syncSuccess) "SYNC_SUCCESS" else "SYNC_FAILURE", "Anomaly blob sync attempt — ${if (syncSuccess) "success" else "failed"}")
                }

                _shieldState.value = when {
                    report.riskScore == 0u -> ShieldState.Genuine(report.details)
                    else -> ShieldState.Anomaly(report.verdict.toString(), report.details, report.riskScore)
                }
            } catch (e: Exception) {
                logEvent("ERROR", "Integrity check failed: ${e.message}")
                _shieldState.value = ShieldState.Error(e.message ?: "Fortress anomaly")
            }
        }
    }
}

// ShieldState unchanged
