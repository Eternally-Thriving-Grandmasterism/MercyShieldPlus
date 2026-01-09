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
import com.mercyshieldplus.util.TamperDetectionUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// uniFFI generated bindings mercy
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

sealed class ShieldState {
 object Loading : ShieldState()
 data class Genuine(val details: List<String>) : ShieldState()
 data class Anomaly(val verdict: String, val details: List<String>, val risk: UByte) : ShieldState()
 data class Error(val message: String) : ShieldState()
}

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
 logEvent("INFO", "MercyShieldPlus fortress awakening eternal ⚡️")
 checkIntegrity()
 }

 fun refreshIntegrity() {
 logEvent("INFO", "Manual integrity refresh triggered by user mercy")
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
 _integrityHistory.value = entries.sortedByDescending { it.timestamp }
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
 logEvent("INFO", "Integrity check initiated — scanning fortress")

 val suspiciousFiles = RootDetectionUtil.detectSuspiciousFiles()
 val suspiciousProps = RootDetectionUtil.detectSuspiciousProps()
 val anyKernelRoot = RootDetectionUtil.isAnyKernelRootPresent(getApplication())
 val tamperDetails = TamperDetectionUtil.getTamperDetails(getApplication())

 val allDetails = suspiciousFiles + suspiciousProps + tamperDetails + if (anyKernelRoot) listOf("Kernel root indicators detected") else emptyList()

 val bindData = "integrity_check_${System.currentTimeMillis()}"
 val cloudProjectNumber = 123456789012L // Your Google Cloud project number mercy
 val playToken = PlayIntegrityUtil.requestIntegrityToken(cloudProjectNumber, bindData)

 // PQ key generation mercy (persist SK production)
 val (kemPkB64, dsaPkB64) = MercyShieldPlus.generatePqKeypair()
 logEvent ("INFO", "PQ keys generated — KEM PK: ${kemPkB64.take(20)}..., DSA PK: ${dsaPkB64.take(20)}...")

 // Build report JSON with all details + token + DSA PK
 val reportMap = mapOf(
 "timestamp" to System.currentTimeMillis(),
 "details" to allDetails,
 "play_token" to playToken,
 "dsa_pk_base64" to dsaPkB64 // Bootstrap server trust
 )
 val jsonReport = gson.toJson(reportMap)

 // On anomaly — sign report + generate blob mercy
 if (allDetails.isNotEmpty()) {
 // Sign with persisted DSA SK (future Keystore) — here fresh for demo
 val sigB64 = MercyShieldPlus.pqSignData(dsaPkB64, jsonReport.toByteArray())
 logEvent("INFO", "Report signed — signature: ${sigB64.take(20)}...")

 val serverPkB64 = ServerSyncUtil.getServerKemPkB64() // Config mercy
 val blobB64 = MercyShieldPlus.pqSecureAttestationBlob(
 jsonReport.toByteArray(),
 serverPkB64,
 dsaPkB64 // Local SK base64 for sign
 )

 val blobBytes = BASE64.decode(blobB64)
 val syncSuccess = ServerSyncUtil.sendAnomalyBlob(blobBytes)
 logEvent(if (syncSuccess) "SYNC_SUCCESS" else "SYNC_FAILURE", "Anomaly blob sync — ${if (syncSuccess) "delivered eternal" else "offline mercy"}")
 }

 // Persist report mercy
 val entity = IntegrityReportEntity(
 isGenuine = allDetails.isEmpty(),
 detailsJson = gson.toJson(allDetails),
 riskScore = if (allDetails.isEmpty()) 0 else 80,
 verdict = if (allDetails.isEmpty()) "Genuine" else "Anomaly"
 )
 dao.insert(entity)

 // UI state mercy
 _shieldState.value = if (allDetails.isEmpty()) {
 ShieldState.Genuine(allDetails)
 } else {
 ShieldState.Anomaly("Anomaly Detected", allDetails, 80u)
 }

 logEvent("INFO", "Integrity check completed — status: ${if (allDetails.isEmpty()) "Genuine Eternal" else "Anomaly Detected"}")
 } catch (e: Exception) {
 logEvent("ERROR", "Integrity check failed: ${e.message}")
 _shieldState.value = ShieldState.Error(e.message ?: "Unknown fortress anomaly")
 }
 }
 }
}
