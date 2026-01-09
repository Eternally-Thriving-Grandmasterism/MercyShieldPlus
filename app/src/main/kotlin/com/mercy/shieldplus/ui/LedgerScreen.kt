package com.mercyshieldplus.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import com.mercyshieldplus.viewmodel.ShieldViewModel
import com.mercyshieldplus.viewmodel.IntegrityReportEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LedgerScreen(viewModel: ShieldViewModel) {
    val history by viewModel.integrityHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Integrity Ledger Eternal ⚡️") })
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No history yet — checks will appear here", color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(history, key = { it.timestamp }) { entry ->
                    LedgerEntryCard(entry)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LedgerEntryCard(entry: IntegrityReportEntry) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .scale(if (entry.isAnomaly) pulseScale else 1f),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isGenuine) Color(0xFF1B5E20) else Color(0xFF7F0000)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (entry.isGenuine) "Genuine Eternal ✓" else "Anomaly Detected ⚠",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp)),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(visible = entry.expanded) {
                Column {
                    entry.details.forEach { detail ->
                        Text("• $detail", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                    }
                    if (entry.riskScore > 0u) {
                        Text("Risk Score: ${entry.riskScore}", color = Color.Yellow, fontWeight = FontWeight.Bold)
                    }
                }
            }

            TextButton(onClick = { entry.onToggleExpand() }) {
                Text(if (entry.expanded) "Hide Details" else "Show Details", color = Color.Cyan)
            }
        }
    }
}
