package com.venice.tp3.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venice.tp3.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(onPlay: (Long, Int) -> Unit, onWallet: () -> Unit, onProfile: () -> Unit) {
    val app = UserApp.app()
    var me by remember { mutableStateOf(app.me) }
    var cfg by remember { mutableStateOf(TableCfg()) }
    var bump by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<Pair<Long, Int>?>(null) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(bump) {
        while (true) {
            val id = app.prefs.meId
            withContext(Dispatchers.IO) {
                val fresh = app.repo.refreshUser(id)
                val c = app.repo.tableCfg()
                withContext(Dispatchers.Main) {
                    if (fresh != null) {
                        app.me = fresh
                        me = fresh
                    }
                    cfg = c
                }
            }
            delay(4000)
        }
    }

    fun start(boot: Long, seats: Int) {
        if ((me?.balance ?: 0L) >= boot * 10) onPlay(boot, seats)
        else pending = boot to seats
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            me?.let { Avatar(it.name, it.color, 42) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(me?.name ?: "-", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("ID ${me?.refCode ?: ""}", fontSize = 10.sp, color = Color(0xFF8FA396))
            }
            Column(
                Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF14261C))
                    .clickable { onWallet() }.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(money(me?.balance ?: 0), color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("+ Add", fontSize = 9.sp, color = Color(0xFF8FA396))
            }
        }

        Box(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.radialGradient(listOf(Felt, FeltDark)))
        ) {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("TEEN PATTI 3", color = Gold, fontWeight = FontWeight.Black, fontSize = 30.sp)
                Text("Octro style · Real chips", color = Color(0xFFA9C7B4), fontSize = 12.sp)
                Spacer(Modifier.height(26.dp))

                listOf(
                    Triple("CLASSIC", 5, cfg),
                    Triple("QUICK", 3, cfg),
                    Triple("HIGH STAKES", 6, cfg)
                ).forEachIndexed { i, (label, seats, c) ->
                    val boot = c.boot * (i + 1)
                    Box(
                        Modifier.fillMaxWidth(0.78f).padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0B2417))
                            .clickable { start(boot, seats) }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(label, color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Boot ${money(boot)} · $seats seats", fontSize = 11.sp, color = Color(0xFF8FA396))
                            }
                            Text("▶", color = Gold, fontSize = 18.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Min balance ${money(cfg.boot * 10)} chahiye", fontSize = 10.sp, color = Color(0xFF6F8579))
                if (msg.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(msg, fontSize = 11.sp, color = Gold)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Panel)
                    .clickable { onWallet() }.padding(14.dp),
                contentAlignment = Alignment.Center
            ) { Text("💰  Wallet", fontSize = 13.sp) }
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Panel)
                    .clickable { onProfile() }.padding(14.dp),
                contentAlignment = Alignment.Center
            ) { Text("👤  Profile", fontSize = 13.sp) }
        }
    }

    pending?.let { (boot, seats) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Balance kam hai") },
            text = {
                Column {
                    Text("Is table ke liye ${money(boot * 10)} chahiye.", fontSize = 12.sp)
                    Text("Aapka balance: ${money(me?.balance ?: 0)}", fontSize = 12.sp, color = Gold)
                    if (cfg.demoOn && me?.demoGiven != true) {
                        Spacer(Modifier.height(8.dp))
                        Text("Ek baar free demo stack ${money(cfg.demoTopUp)} mil sakta hai.", fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton({
                    val id = app.prefs.meId
                    scope.launch {
                        val fresh = withContext(Dispatchers.IO) { app.repo.ensurePlayable(id, boot) }
                        pending = null
                        if (fresh != null) {
                            app.me = fresh
                            me = fresh
                            onPlay(boot, seats)
                        } else {
                            msg = "Pehle paise add karo"
                            onWallet()
                        }
                    }
                }) { Text("DEMO KHELO") }
            },
            dismissButton = {
                Row {
                    TextButton({ pending = null; onWallet() }) { Text("ADD MONEY") }
                    TextButton({ pending = null }) { Text("Band") }
                }
            }
        )
    }
}
