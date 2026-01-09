package com.mercyshieldplus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AuthLockoutScreen(lockoutEndTime: Long, onTimeUp: () -> Unit) {
    val remainingSeconds = remember { mutableLongStateOf(0L) }

    LaunchedEffect(lockoutEndTime) {
        while (true) {
            val remaining = (lockoutEndTime - System.currentTimeMillis()) / 1000
            if (remaining <= 0) {
                remainingSeconds.longValue = 0
                onTimeUp()
                break
            }
            remainingSeconds.longValue = remaining
            delay(1000)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(
                "Temporary Lockout ⚠",
                color = Color.Red,
                fontSize = 32.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Too many failed attempts — mercy cooldown active",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Text(
                "Retry available in:",
                color = Color.Orange,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                formatRemainingTime(remainingSeconds.longValue),
                color = Color.Cyan,
                fontSize = 48.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

private fun formatRemainingTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
