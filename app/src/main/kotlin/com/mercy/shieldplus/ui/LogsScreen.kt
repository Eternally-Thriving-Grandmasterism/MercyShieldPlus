package com.mercyshieldplus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mercyshieldplus.database.LogEntryEntity
import com.mercyshieldplus.viewmodel.ShieldViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsScreen(viewModel: ShieldViewModel) {
    val logs by viewModel.filteredLogs.collectAsState()
    val selectedFilter by viewModel.logFilter.collectAsState()

    val filterOptions = listOf("All", "INFO", "ANOMALY", "SYNC_SUCCESS", "SYNC_FAILURE", "ERROR")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Logs Eternal ⚡️") },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Filter")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        filterOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.setLogFilter(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                    TextButton(onClick = { viewModel.clearLogs() }) {
                        Text("Clear Logs")
                    }
                }
            )
        }
    ) { padding ->
        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No logs yet — events will appear here", color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(logs, key = { it.id }) { log ->
                    LogEntryCard(log)
                }
            }
        }
    }
}

@Composable
fun LogEntryCard(log: LogEntryEntity) {
    val backgroundColor = when (log.logType) {
        "INFO" -> Color(0xFF1B5E20)
        "ANOMALY" -> Color(0xFF7F0000)
        "SYNC_SUCCESS" -> Color(0xFF006400)
        "SYNC_FAILURE" -> Color(0xFFB71C1C)
        "ERROR" -> Color(0xFFC62828)
        else -> Color(0xFF212121)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "[${log.logType}]",
                    color = Color.Yellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = log.message,
                color = Color.White,
                fontSize = 15.sp
            )
        }
    }
}
