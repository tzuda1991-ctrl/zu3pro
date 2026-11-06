package com.venice.tp3.user

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun UserNav() {
    val app = UserApp.app()
    val nav = rememberNavController()
    val start = when {
        !app.prefs.config.ready -> "setup"
        app.prefs.meId.isBlank() -> "auth"
        else -> "home"
    }

    NavHost(nav, start) {
        composable("setup") {
            SetupScreen { nav.navigate("auth") { popUpTo("setup") { inclusive = true } } }
        }
        composable("auth") {
            AuthScreen { nav.navigate("home") { popUpTo("auth") { inclusive = true } } }
        }
        composable("home") {
            HomeScreen(
                onPlay = { boot, seats -> nav.navigate("table/$boot/$seats") },
                onWallet = { nav.navigate("wallet") },
                onProfile = { nav.navigate("profile") }
            )
        }
        composable(
            "table/{boot}/{seats}",
            arguments = listOf(navArgument("boot") { type = NavType.LongType }, navArgument("seats") { type = NavType.IntType })
        ) { e ->
            TableScreen(e.arguments!!.getLong("boot"), e.arguments!!.getInt("seats")) { nav.popBackStack() }
        }
        composable("wallet") { WalletScreen { nav.popBackStack() } }
        composable("profile") {
            ProfileScreen(
                onBack = { nav.popBackStack() },
                onLogout = {
                    app.prefs.meId = ""; app.me = null
                    nav.navigate("auth") { popUpTo("home") { inclusive = true } }
                }
            )
        }
    }
}
