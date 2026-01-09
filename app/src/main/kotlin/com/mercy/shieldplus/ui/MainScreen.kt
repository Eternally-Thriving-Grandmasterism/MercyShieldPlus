package com.mercyshieldplus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mercyshieldplus.viewmodel.ShieldViewModel

@Composable
fun MainApp(viewModel: ShieldViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "shield") {
        composable("shield") { ShieldScreen(viewModel, navController) }
        composable("ledger") { LedgerScreen(viewModel) }
    }
}

// Example ShieldScreen with nav button
@Composable
fun ShieldScreen(viewModel: ShieldViewModel, navController: NavController) {
    // ... previous shield UI mercy

    Button(onClick = { navController.navigate("ledger") }) {
        Text("View Ledger History")
    }
}
