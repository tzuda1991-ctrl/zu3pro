package com.venice.tp3.user

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venice.tp3.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WalletScreen(onBack: () -> Unit) {
    val app = UserApp.app()
    val ctx = LocalContext.current
    val me = app.me ?: return
    var tab by remember { mutableIntStateOf(0) }
    var cfg by remember { mutableStateOf(TableCfg()) }
    var deps by remember { mutableStateOf<List<Deposit>>(emptyList()) }
    var amt by remember { mutableStateOf("") }
    var utr by remember { mutableStateOf("") }
    var upi by remember { mutableStateOf("") }
    var proof by remember { mutableStateOf<ByteArray?>(null) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scope.launch {
            proof = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(uri)?.readBytes() }.getOrNull()
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) {
                val c = app.repo.tableCfg()
                val d = app.repo.db.load()["dep"]?.values
                    ?.mapNotNull { runCatching { Deposit.from(it) }.getOrNull() }
                    ?.filter { it.userId == me.id } ?: emptyList()
                withContext(Dispatchers.Main) {
                    cfg = c
                    deps = d
                }
            }
            delay(4000)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onBack) { Text("< Back") }
            Text("WALLET", fontWeight = FontWeight.Black, color = Gold)
        }
        PanelCard {
            Text("Balance", fontSize = 11.sp, color = Color(0xFF9AA89F))
            Text(money(me.balance), fontSize = 26.sp, fontWeight = FontWeight.Black, color = Gold)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { GoldButton("DEPOSIT", onClick = { tab = 0 }, enabled = true) }
            Box(Modifier.weight(1f)) { GhostButton("WITHDRAW") { tab = 1 } }
        }
        Spacer(Modifier.height(12.dp))

        if (tab == 0) {
            PanelCard {
                Text("UPI: ${cfg.upi}", fontSize = 12.sp, color = Gold)
                Text("Pay karke UTR + screenshot daalo", fontSize = 10.sp, color = Color(0xFF9AA89F))
                Spacer(Modifier.height(8.dp))
                Field(amt, "Amount (₹)", { amt = it.filter { c -> c.isDigit() } }, numeric = true)
                Spacer(Modifier.height(6.dp))
                Field(utr, "UTR / Ref No", { utr = it })
                Spacer(Modifier.height(8.dp))
                GhostButton(if (proof == null) "Screenshot chuno" else "Screenshot ✓") {
                    picker.launch("image/*")
                }
                Spacer(Modifier.height(8.dp))
                GoldButton("REQUEST", amt.isNotBlank()) {
                    val a = (amt.toLongOrNull() ?: 0) * 100
                    if (a < cfg.minDeposit) {
                        msg = "Min ${money(cfg.minDeposit)}"
                        return@GoldButton
                    }
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { app.repo.requestDeposit(me, a, utr, proof) }
                        msg = if (r.isSuccess) "Request bhej di, admin approval ka wait karo"
                        else r.exceptionOrNull()?.message ?: "fail"
                        amt = ""
                        utr = ""
                        proof = null
                    }
                }
                if (msg.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(msg, fontSize = 11.sp, color = Gold)
                }
            }
        } else {
            PanelCard {
                Field(amt, "Amount (₹)", { amt = it.filter { c -> c.isDigit() } }, numeric = true)
                Spacer(Modifier.height(6.dp))
                Field(upi, "Aapka UPI ID", { upi = it })
                Spacer(Modifier.height(8.dp))
                GoldButton("WITHDRAW", amt.isNotBlank() && upi.isNotBlank()) {
                    val a = (amt.toLongOrNull() ?: 0) * 100
                    if (a < cfg.minWithdraw) {
                        msg = "Min ${money(cfg.minWithdraw)}"
                        return@GoldButton
                    }
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { app.repo.requestWithdraw(me, a, upi) }
                        msg = if (r.isSuccess) "Withdraw request bhej di"
                        else r.exceptionOrNull()?.message ?: "fail"
                        app.repo.refreshUser(me.id)?.let { app.me = it }
                        amt = ""
                    }
                }
                if (msg.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(msg, fontSize = 11.sp, color = Gold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Mere deposits", fontSize = 11.sp, color = Color(0xFF9AA89F))
        LazyColumn(Modifier.weight(1f)) {
            items(deps) { d ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Panel)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(money(d.amount), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("UTR ${d.utr}", fontSize = 9.sp, color = Color(0xFF9AA89F))
                    }
                    StatusChip(d.status)
                }
            }
        }
    }
}
