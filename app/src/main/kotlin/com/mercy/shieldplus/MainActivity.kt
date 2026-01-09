package com.mercyshieldplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // uniFFI init (generated bindings)
        // MercyShieldPlus.init() or similar

        setContent {
            MaterialTheme {
                MercyShieldScreen()
            }
        }
    }
}

@Composable
fun MercyShieldScreen() {
    var shieldActive by remember { mutableStateOf(true) }
    val infiniteTransition = rememberInfiniteTransition()
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        // Quantum Glow Pulse
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(glowScale)
                .background(if (shieldActive) Color.Green else Color.Red, shape = CircleShape)
                .alpha(0.8f)
        )

        Text(
            text = if (shieldActive) "Shield Eternal Active ⚡️" else "Anomaly Burst!",
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge
        )

        // Anomaly burst example trigger
        if (!shieldActive) {
            AnimatedVisibility(visible = true, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                // Red explosion mercy
            }
        }
    }

    // Call Rust: shieldActive = MercyShieldPlus.check_device_integrity()
}
