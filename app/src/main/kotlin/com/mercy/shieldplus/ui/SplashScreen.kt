package com.mercyshieldplus.ui

import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

fun ComponentActivity.showBiometricPromptWithFallback(
    onSuccess: () -> Unit,
    onAuthFailed: (isRecoverable: Boolean) -> Unit  // Distinguish failed vs error
) {
    val executor: Executor = ContextCompat.getMainExecutor(this)

    val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            // Non-recoverable (lockout, no auth, etc.)
            onAuthFailed(false)
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            // Recoverable (wrong biometric) — allow retry mercy
            onAuthFailed(true)
        }
    })

    val biometricManager = BiometricManager.from(this)
    val canAuthenticate = biometricManager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    )

    val promptInfo = when (canAuthenticate) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricPrompt.PromptInfo.Builder()
            .setTitle("MercyShieldPlus Eternal Authentication ⚡️")
            .setSubtitle("Use biometric or device PIN/pattern")
            .setDescription("Hardware-backed mercy protects your quantum fortress")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .setConfirmationRequired(false)
            .build()
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricPrompt.PromptInfo.Builder()
            .setTitle("MercyShieldPlus Device Authentication ⚡️")
            .setSubtitle("Enter device PIN/pattern/password")
            .setDescription("Your integrity ledger requires device credential mercy")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        else -> {
            onAuthFailed(false)
            return
        }
    }

    biometricPrompt.authenticate(promptInfo)
}

// Splash animation unchanged mercy
@Composable
fun CustomSplashScreen(onAuthenticated: () -> Unit, onAuthFailed: (Boolean) -> Unit) {
    // ... (previous animation code)

    LaunchedEffect(Unit) {
        (this@CustomSplashScreen as MainActivity).showBiometricPromptWithFallback(
            onSuccess = onAuthenticated,
            onAuthFailed = onAuthFailed
        )
    }
}
