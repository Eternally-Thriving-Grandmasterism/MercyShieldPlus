package org.tolc.mercyshieldplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    external fun mercy_shield_status(): String

    companion object {
        init {
            System.loadLibrary("mercyshieldplus")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MercyShieldPlusTheme {
                MercyShieldScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MercyShieldScreen() {
    val status = mercy_shield_status() // Rust proprietary call eternal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MercyShieldPlus ⚡️") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.Cyan)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = status,
                color = if (status.contains("Green")) Color.Green else Color.Red,
                fontSize = 32.sp,
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = "Quantum Fortress Active Proprietary Eternal ⚡️",
                color = Color.Cyan,
                fontSize = 24.sp
            )
        }
    }
}

@Composable
fun MercyShieldPlusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.Cyan,
            background = Color.Black,
            surface = Color(0xFF1E1E1E)
        ),
        content = content
    )
}
