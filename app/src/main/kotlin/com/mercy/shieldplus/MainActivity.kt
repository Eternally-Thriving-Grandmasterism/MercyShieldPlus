package com.mercyshieldplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.mercyshieldplus.ui.AuthRetryScreen
import com.mercyshieldplus.ui.CustomSplashScreen
import com.mercyshieldplus.ui.LimitedShieldScreen
import com.mercyshieldplus.ui.MainApp
import com.mercyshieldplus.viewmodel.ShieldViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ShieldViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ShieldViewModel(this.application)

        setContent {
            MaterialTheme {
                val showSplash = remember { mutableStateOf(true) }
                val showRetry = remember { mutableStateOf(false) }
                val limitedMode = remember { mutableStateOf(false) }
                val remainingAttempts = remember { mutableIntStateOf(3) }  // Mercy: 3 attempts before limited

                if (showSplash.value) {
                    CustomSplashScreen(
                        onAuthenticated = {
                            showSplash.value = false
                            showRetry.value = false
                            limitedMode.value = false
                        },
                        onAuthFailed = { isRecoverable ->
                            if (isRecoverable && remainingAttempts.intValue > 1) {
                                remainingAttempts.intValue -= 1
                                showRetry.value = true
                            } else {
                                showSplash.value = false
                                limitedMode.value = true
                            }
                        }
                    )
                } else if (showRetry.value) {
                    AuthRetryScreen(
                        remainingAttempts = remainingAttempts.intValue,
                        onRetry = { showSplash.value = true },
                        onCancel = {
                            showRetry.value = false
                            limitedMode.value = true
                        }
                    )
                } else if (limitedMode.value) {
                    LimitedShieldScreen(
                        viewModel = viewModel,
                        onRetryAuth = {
                            remainingAttempts.intValue = 3  // Reset mercy
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
