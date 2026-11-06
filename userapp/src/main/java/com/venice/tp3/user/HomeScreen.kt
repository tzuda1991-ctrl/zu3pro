package com.venice.tp3.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(onPlay: (Long, Int) -> Unit, onWallet: () -> Unit, onProfile: () -> Unit) {
    val app = UserApp.app()
    var me by remember { mutableStateOf(app.me) }
    var cfg by remember { mutableStateOf(TableCfg()) }
    var bump by remember { mutableIntStateOf(0) }

    LaunchedEffect(bump) {
        while (true) {
            val id = app.prefs.meId
            withContext(Dispatchers.IO) {
                val fresh = app.repo.refreshUser(id)
                val c = app.repo.tableCfg()
                withContext(Dispatchers.Main) {
                    if (fresh != null) { app.me = fresh; me = fresh }
                    cfg = c
                }
            }
            delay(4000)
        }
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
                            .clickable {
                                if ((me?.balance ?: 0) < boot * 10)
                                    onWallet()
                                else onPlay(boot, seats)
                            }
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
}
