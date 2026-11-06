package com.venice.tp3.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AdminNav() {
    val app = AdminApp.app()
    val nav = rememberNavController()
    LaunchedEffect(Unit) { app.syncBot() }
    val start = when {
        !app.prefs.config.ready -> "setup"
        !app.prefs.adminOk -> "login"
        else -> "dash"
    }
    NavHost(nav, start) {
        composable("setup") {
            SetupScreen { nav.navigate("login") { popUpTo("setup") { inclusive = true } } }
        }
        composable("login") {
            LoginScreen(
                onIn = { nav.navigate("dash") { popUpTo("login") { inclusive = true } } },
                onSetup = { nav.navigate("setup") }
            )
        }
        composable("dash") {
            Dashboard(
                onUsers = { nav.navigate("users") },
                onDeps = { nav.navigate("deps") },
                onWdr = { nav.navigate("wdr") },
                onCfg = { nav.navigate("cfg") },
                onSetup = { nav.navigate("setup") }
            )
        }
        composable("users") { UsersScreen { nav.popBackStack() } }
        composable("deps") { DepositsScreen { nav.popBackStack() } }
        composable("wdr") { WithdrawsScreen { nav.popBackStack() } }
        composable("cfg") { SettingsScreen { nav.popBackStack() } }
    }
}
