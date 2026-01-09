package com.mercyshieldplus.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mercyshieldplus.database.LogEntryEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * LogExportUtil — Export Logs to Shareable JSON File Mercy
 *
 * Collects logs (all or filtered), serializes pretty JSON
 * Writes to cache dir as "mercyshield_logs_YYYYMMDD_HHMMSS.json"
 * Returns content URI via FileProvider for secure share
 *
 * Manifest requirement: Add FileProvider
 * <provider
 *     android:name="androidx.core.content.FileProvider"
 *     android:authorities="${applicationId}.provider"
 *     android:exported="false"
 *     android:grantUriPermissions="true">
 *     <meta-data
 *         android:name="android.support.FILE_PROVIDER_PATHS"
 *         android:resource="@xml/file_paths" />
 * </provider>
 *
 * res/xml/file_paths.xml:
 * <?xml version="1.0" encoding="utf-8"?>
 * <paths>
 *     <cache-path name="logs" path="/" />
 * </paths>
 */
object LogExportUtil {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    data class ExportLog(
        val timestamp: Long,
        val date: String,
        val type: String,
        val message: String
    )

    fun exportLogs(context: Context, logs: List<LogEntryEntity>): Uri {
        val exportLogs = logs.map { log ->
            ExportLog(
                timestamp = log.timestamp,
                date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                type = log.logType,
                message = log.message
            )
        }

        val json = gson.toJson(exportLogs)

        val fileName = "mercyshield_logs_${SimpleDateFormat("yyyyMMdd_HHMMss", Locale.getDefault()).format(Date())}.json"
        val file = File(context.cacheDir, fileName)
        file.writeText(json)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    fun shareLogsUri(context: Context, uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "MercyShieldPlus Logs Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Export Logs Mercy")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
