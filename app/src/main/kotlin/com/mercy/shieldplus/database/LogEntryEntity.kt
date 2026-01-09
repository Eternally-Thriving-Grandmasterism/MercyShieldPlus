package com.mercyshieldplus.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_logs")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val logType: String,  // e.g., "INFO", "ANOMALY", "SYNC_SUCCESS", "SYNC_FAILURE", "ERROR"
    val message: String
)
