package com.mercyshieldplus.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IntegrityDao {
    // Existing integrity report methods
    @Insert
    suspend fun insert(report: IntegrityReportEntity)

    @Query("SELECT * FROM integrity_reports ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<IntegrityReportEntity>>

    @Query("DELETE FROM integrity_reports")
    suspend fun clearAll()

    // New logging methods mercy
    @Insert
    suspend fun insertLog(entry: LogEntryEntity)

    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntryEntity>>

    @Query("DELETE FROM app_logs")
    suspend fun clearLogs()
}
