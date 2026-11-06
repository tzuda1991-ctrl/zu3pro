package com.venice.tp3.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venice.tp3.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun Dashboard(
    onUsers: () -> Unit, onDeps: () -> Unit, onWdr: () -> Unit,
    onCfg: () -> Unit, onSetup: () -> Unit
) {
    val app = AdminApp.app()
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var deps by remember { mutableStateOf<List<Deposit>>(emptyList()) }
    var wdrs by remember { mutableStateOf<List<Withdraw>>(emptyList()) }

    LaunchedEffect(Unit) {
        var tick = 0
        while (true) {
            withContext(Dispatchers.IO) {
                val u = app.repo.allUsers()
                val d = app.repo.allDeposits()
                val w = app.repo.allWithdraws()
                if (tick % 60 == 0) app.repo.snapshotAll()
                withContext(Dispatchers.Main) {
                    users = u
                    deps = d
                    wdrs = w
                }
            }
            tick++
            delay(5000)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("DASHBOARD", color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("USERS", users.size.toString(), onClick = onUsers)
            StatBox("PENDING DEP", deps.count { it.status == "PENDING" }.toString(), Color(0xFF2ECC71), onDeps)
            StatBox("PENDING WDR", wdrs.count { it.status == "PENDING" }.toString(), Color(0xFF3498DB), onWdr)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("TOTAL BAL", money(users.sumOf { it.balance }), Gold)
            StatBox("DEP APPROVED", money(deps.filter { it.status == "APPROVED" }.sumOf { it.amount }), Color(0xFF2ECC71))
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { GoldButton("TABLE CONFIG") { onCfg() } }
            Box(Modifier.weight(1f)) { GhostButton("SETUP") { onSetup() } }
        }
        Spacer(Modifier.height(14.dp))
        PanelCard {
            Text("Connection", fontSize = 12.sp, color = Gold, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Chat: ${app.prefs.config.chatId}", fontSize = 10.sp)
            Text("Snapshot har 5 min auto-push hota hai", fontSize = 10.sp, color = Color(0xFF9AA89F))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    GhostButton("PING") {
                        Thread {
                            android.util.Log.d("ping", "${app.repo.pingAdmin()} | ${app.repo.pingPublic()}")
                        }.start()
                    }
                }
                Box(Modifier.weight(1f)) {
                    GhostButton("RE-PUSH ALL") {
                        Thread {
                            val ok = app.repo.resync()
                            android.util.Log.d("resync", if (ok) "ok" else "fail")
                        }.start()
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Recent users", fontSize = 11.sp, color = Color(0xFF9AA89F))
        users.sortedByDescending { it.createdAt }.take(8).forEach { u ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(u.name, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(money(u.balance), fontSize = 12.sp, color = Gold)
            }
        }
    }
}
