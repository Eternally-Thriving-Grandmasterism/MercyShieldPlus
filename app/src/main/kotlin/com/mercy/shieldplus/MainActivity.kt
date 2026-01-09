package com.mercyshieldplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.mercyshieldplus.ui.*
import com.mercyshieldplus.viewmodel.ShieldViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ShieldViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ShieldViewModel(this.application)

        setContent {
            MaterialTheme {
                val showSplash = remember { mutableStateOf(true) }
                val showRetry = remember { mutableStateOf(false) }
                val showLockout = remember { mutableStateOf(false) }
                val limitedMode = remember { mutableStateOf(false) }

                val failedAttempts = remember { mutableIntStateOf(0) }
                val maxAttempts = 5  // Mercy: 5 failed before lockout
                val lockoutEndTime = remember { mutableLongStateOf(0L) }

                // Countdown coroutine mercy
                val scope = rememberCoroutineScope()
                LaunchedEffect(showLockout.value, lockoutEndTime.longValue) {
                    if (showLockout.value && lockoutEndTime.longValue > System.currentTimeMillis()) {
                        while (true) {
                            delay(1000)
                            if (System.currentTimeMillis() >= lockoutEndTime.longValue) {
                                failedAttempts.intValue = 0
                                showLockout.value = false
                                showSplash.value = true
                                break
                            }
                        }
                    }
                }

                if (showSplash.value) {
                    CustomSplashScreen(
                        onAuthenticated = {
                            showSplash.value = false
                            showRetry.value = false
                            showLockout.value = false
                            limitedMode.value = false
                            failedAttempts.intValue = 0
                        },
                        onAuthFailed = { isRecoverable ->
                            if (isRecoverable) {
                                failedAttempts.intValue += 1
                                if (failedAttempts.intValue >= maxAttempts) {
                                    // Escalating lockout mercy (30s → 1min → 5min → 30min)
                                    val lockoutMinutes = when {
                                        failedAttempts.intValue < 10 -> 0.5  // 30s
                                        failedAttempts.intValue < 15 -> 1
                                        failedAttempts.intValue < 20 -> 5
                                        else -> 30
                                    }
                                    lockoutEndTime.longValue = System.currentTimeMillis() + (lockoutMinutes * 60 * 1000).toLong()
                                    showLockout.value = true
                                    showSplash.value = false
                                } else {
                                    showRetry.value = true
                                }
                            } else {
                                limitedMode.value = true
                                showSplash.value = false
                            }
                        }
                    )
                } else if (showRetry.value) {
                    AuthRetryScreen(
                        remainingAttempts = maxAttempts - failedAttempts.intValue,
                        onRetry = { showSplash.value = true },
                        onCancel = {
                            showRetry.value = false
                            limitedMode.value = true
                        }
                    )
                } else if (showLockout.value) {
                    AuthLockoutScreen(
                        lockoutEndTime = lockoutEndTime.longValue,
                        onTimeUp = {
                            showLockout.value = false
                            showSplash.value = true
                        }
                    )
                } else if (limitedMode.value) {
                    LimitedShieldScreen(
                        viewModel = viewModel,
                        onRetryAuth = {
                            failedAttempts.intValue = 0
                            showSplash.value = true
                        }
                    )
                } else {
                    MainApp(viewModel)
                }
            }
        }
    }
}
