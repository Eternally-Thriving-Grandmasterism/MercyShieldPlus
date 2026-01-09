package com.mercyshieldplus.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "integrity_reports")
data class IntegrityReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isGenuine: Boolean,
    val detailsJson: String,  // Serialized List<String> to JSON
    val riskScore: Int = 0,
    val verdict: String = ""
)
