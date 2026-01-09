package com.mercyshieldplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
                val limitedMode = remember { mutableStateOf(false) }

                if (showSplash.value) {
                    CustomSplashScreen(
                        onAuthenticated = {
                            showSplash.value = false
                            limitedMode.value = false
                        },
                        onAuthFailed = {
                            showSplash.value = false
                            limitedMode.value = true  // Enter limited mode mercy
                        }
                    )
                } else if (limitedMode.value) {
                    LimitedShieldScreen(
                        viewModel = viewModel,
                        onRetryAuth = {
                            showSplash.value = true  // Back to splash for retry
                        }
                    )
                } else {
                    MainApp(viewModel)
                }
            }
        }
    }
}
