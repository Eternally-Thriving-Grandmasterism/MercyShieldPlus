package com.mercyshieldplus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthRetryScreen(remainingAttempts: Int, onRetry: () -> Unit, onCancel: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(
                "Authentication Failed ⚠",
                color = Color.Red,
                fontSize = 28.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Remaining attempts: $remainingAttempts",
                color = Color.Orange,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Wrong biometric or credential — please try again",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))
            Button(onClick = onRetry) {
                Text("Retry Authentication")
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCancel) {
                Text("Enter Limited Mode")
            }
        }
    }
}
