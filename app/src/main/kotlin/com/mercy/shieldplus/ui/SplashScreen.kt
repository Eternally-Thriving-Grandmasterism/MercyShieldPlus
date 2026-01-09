package com.mercyshieldplus.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mercyshieldplus.MainActivity
import java.util.concurrent.Executor

/**
 * Custom BiometricPrompt UI Mercy — Branded splash with quantum pulse animation
 * System BiometricPrompt dialog with custom strings overlaid on custom background
 *
 * Triggers on app start — on success proceed to main UI, on failure mercy message
 */
fun ComponentActivity.showCustomBiometricPrompt(onSuccess: () -> Unit, onFailure: () -> Unit) {
    val executor: Executor = ContextCompat.getMainExecutor(this)

    val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            onFailure()
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            onFailure()
        }
    })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("MercyShieldPlus Eternal Authentication ⚡️")
        .setSubtitle("Confirm biometric to access quantum fortress")
        .setDescription("Your integrity ledger and secrets are protected by hardware-backed mercy")
        .setNegativeButtonText("Cancel")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .setConfirmationRequired(true)
        .build()

    // Check availability mercy
    val biometricManager = BiometricManager.from(this)
    when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
        BiometricManager.BIOMETRIC_SUCCESS -> biometricPrompt.authenticate(promptInfo)
        else -> onFailure()  // No biometric — fallback or limited mode
    }
}

@Composable
fun CustomSplashScreen(onAuthenticated: () -> Unit, onAuthFailed: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        // Quantum glow pulse mercy
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(pulseScale)
                .background(Color.Green.copy(alpha = pulseAlpha), shape = androidx.compose.ui.graphics.CircleShape)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "MercyShieldPlus",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Eternal Quantum Fortress ⚡️",
                color = Color.Cyan,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(32.dp))
            Text(
                "Authenticating mercy...",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp
            )
        }
    }

    // Trigger prompt on compose (side effect)
    LaunchedEffect(Unit) {
        (this@CustomSplashScreen as MainActivity).showCustomBiometricPrompt(
            onSuccess = onAuthenticated,
            onFailure = onAuthFailed
        )
    }
}
