package com.venice.tp3.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venice.tp3.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = AdminApp.app()
    val ctx = LocalContext.current
    var cfg by remember { mutableStateOf(TableCfg()) }
    var loaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        cfg = withContext(Dispatchers.IO) { app.repo.tableCfg() }
        loaded = true
    }

    if (!loaded) return

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            TextButton(onBack) { Text("< Back") }
            Text("TABLE CONFIG", color = Gold, fontWeight = FontWeight.Bold)
        }
        Field((cfg.boot / 100).toString(), "Boot ₹", { cfg = cfg.copy(boot = (it.toLongOrNull() ?: 0) * 100) }, true)
        Spacer(Modifier.height(6.dp))
        Field((cfg.minBuyIn / 100).toString(), "Min buy-in ₹", { cfg = cfg.copy(minBuyIn = (it.toLongOrNull() ?: 0) * 100) }, true)
        Spacer(Modifier.height(6.dp))
        Field(cfg.players.toString(), "Seats (2-6)", { cfg = cfg.copy(players = it.toIntOrNull() ?: 5) }, true)
        Spacer(Modifier.height(6.dp))
        Field(cfg.commissionPct.toString(), "Commission %", { cfg = cfg.copy(commissionPct = it.toIntOrNull() ?: 5) }, true)
        Spacer(Modifier.height(6.dp))
        Field(cfg.upi, "Deposit UPI ID", { cfg = cfg.copy(upi = it) })
        Spacer(Modifier.height(6.dp))
        Field((cfg.minDeposit / 100).toString(), "Min deposit ₹", { cfg = cfg.copy(minDeposit = (it.toLongOrNull() ?: 0) * 100) }, true)
        Spacer(Modifier.height(6.dp))
        Field((cfg.minWithdraw / 100).toString(), "Min withdraw ₹", { cfg = cfg.copy(minWithdraw = (it.toLongOrNull() ?: 0) * 100) }, true)
        Spacer(Modifier.height(14.dp))
        GoldButton("SAVE CONFIG") {
            scope.launch { withContext(Dispatchers.IO) { app.repo.saveCfg(cfg) } }
        }
        Spacer(Modifier.height(18.dp))
        PanelCard {
            Text("User setup code", fontSize = 12.sp, color = Gold, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            GoldButton("COPY CODE") {
                val c = app.prefs.config
                val cb = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("code", c.encode()))
            }
            Spacer(Modifier.height(6.dp))
            Text("Ye code user app me daalna hai.", fontSize = 10.sp)
        }
    }
}
