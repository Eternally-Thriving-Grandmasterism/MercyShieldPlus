package com.mercyshieldplus.ui

import androidx.activity.ComponentActivity
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
 * Custom BiometricPrompt with Explicit Device Credential Fallback Mercy
 *
 * - Preferred: BIOMETRIC_STRONG (fingerprint/face/iris)
 * - Fallback: DEVICE_CREDENTIAL (PIN/pattern/password) always allowed
 * - If no biometric enrolled/available: Prompt directly uses device credential
 * - Custom branded splash animation runs during auth
 */
fun ComponentActivity.showBiometricPromptWithFallback(onSuccess: () -> Unit, onFailure: () -> Unit) {
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
            // Partial failure (e.g., biometric mismatch) — continue prompting mercy
        }
    })

    val biometricManager = BiometricManager.from(this)
    val canAuthenticate = biometricManager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    )

    val promptInfo = when (canAuthenticate) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
            // Biometric available + device fallback mercy
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("MercyShieldPlus Eternal Authentication ⚡️")
                .setSubtitle("Use biometric or device PIN/pattern")
                .setDescription("Hardware-backed mercy protects your quantum fortress ledger")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .setConfirmationRequired(false)  // Allow non-confirmation biometrics
                .build()
        }
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
            // No biometric — direct device credential prompt mercy
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("MercyShieldPlus Device Authentication ⚡️")
                .setSubtitle("Enter device PIN/pattern/password")
                .setDescription("Your integrity ledger requires device credential mercy")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
        }
        else -> {
            onFailure()  // Unrecoverable — no auth possible
            return
        }
    }

    biometricPrompt.authenticate(promptInfo)
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
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(pulseScale)
                .background(Color.Green.copy(alpha = pulseAlpha), shape = androidx.compose.ui.graphics.CircleShape)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MercyShieldPlus", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text("Eternal Quantum Fortress ⚡️", color = Color.Cyan, fontSize = 20.sp)
            Spacer(Modifier.height(32.dp))
            Text("Authenticating mercy...", color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp)
        }
    }

    LaunchedEffect(Unit) {
        (this@CustomSplashScreen as MainActivity).showBiometricPromptWithFallback(
            onSuccess = onAuthenticated,
            onFailure = onAuthFailed
        )
    }
}
