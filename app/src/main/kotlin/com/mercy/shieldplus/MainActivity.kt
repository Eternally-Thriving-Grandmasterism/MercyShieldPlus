package com.mercyshieldplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
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
                            // Proceed to main UI — DB open safe post-auth
                        },
                        onAuthFailed = {
                            authFailed.value = true
                            // Mercy: User can retry or exit
                        }
                    )
                } else {
                    MainApp(viewModel)
                }

                if (authFailed.value) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Authentication Failed ⚠", color = Color.Red, fontSize = 24.sp)
                            Spacer(Modifier.height(16.dp))
                            Text("Device credential or biometric required for fortress access", color = Color.White, fontSize = 18.sp)
                            // Optional: Retry button mercy
                        }
                    }
                }
            }
        }
    }
}
