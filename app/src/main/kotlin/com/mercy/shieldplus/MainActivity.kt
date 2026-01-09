package com.mercyshieldplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.mercyshieldplus.ui.CustomSplashScreen
import com.mercyshieldplus.ui.MainApp
import com.mercyshieldplus.viewmodel.ShieldViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ShieldViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ShieldViewModel(this.application)

        setContent {
            MaterialTheme {
                val authenticated = remember { mutableStateOf(false) }
                val authFailed = remember { mutableStateOf(false) }

                if (!authenticated.value) {
                    CustomSplashScreen(
                        onAuthenticated = {
                            authenticated.value = true
                            // DB open happens in ViewModel init (after auth success mercy)
                        },
                        onAuthFailed = {
                            authFailed.value = true
                            // Show mercy error screen or exit
                        }
                    )
                } else {
                    MainApp(viewModel)
                }

                if (authFailed.value) {
                    // Mercy failure screen
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        Text("Authentication required for fortress access ⚡️", color = Color.Red, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}
