package com.mercyshieldplus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mercyshieldplus.viewmodel.ShieldState
import com.mercyshieldplus.viewmodel.ShieldViewModel

@Composable
fun LimitedShieldScreen(viewModel: ShieldViewModel, onRetryAuth: () -> Unit) {
    val shieldState by viewModel.shieldState

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(
                "Limited Mode ⚠",
                color = Color.Yellow,
                fontSize = 32.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Full ledger history and persistence require authentication",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            // Live current status mercy (no history access)
            when (shieldState) {
                is ShieldState.Loading -> Text("Checking integrity...", color = Color.Cyan, fontSize = 20.sp)
                is ShieldState.Genuine -> Text("Current Status: Genuine Eternal ✓", color = Color.Green, fontSize = 24.sp)
                is ShieldState.Anomaly -> {
                    Text("Current Status: Anomaly Detected ⚠", color = Color.Red, fontSize = 24.sp)
                    Spacer(Modifier.height(8.dp))
                    Text((shieldState as ShieldState.Anomaly).verdict, color = Color.Orange, fontSize = 18.sp)
                }
                is ShieldState.Error -> Text("Check Failed: ${(shieldState as ShieldState.Error).message}", color = Color.Red, fontSize = 20.sp)
            }

            Spacer(Modifier.height(48.dp))
            Button(onClick = onRetryAuth) {
                Text("Authenticate for Full Fortress Access")
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.refreshIntegrity() }) {
                Text("Refresh Current Check")
            }
        }
    }

    // Auto-refresh on enter mercy
    LaunchedEffect(Unit) {
        viewModel.refreshIntegrity()
    }
}
