package com.venice.tp3.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venice.tp3.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SetupScreen(onDone: () -> Unit) {
    val app = AdminApp.app()
    val p = app.prefs
    val c0 = p.config
    var adminTok by remember { mutableStateOf(c0.adminToken) }
    var publicTok by remember { mutableStateOf(c0.publicToken) }
    var chat by remember { mutableStateOf(c0.chatId) }
    var pass by remember { mutableStateOf("") }
    var log by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)
    ) {
        Text("ADMIN SETUP", color = Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
        Text("Teeno field bharo, Ping se test karo, phir Save.", fontSize = 11.sp, color = Color(0xFF9AA89F))
        Spacer(Modifier.height(16.dp))
        Field(adminTok, "Admin Bot Token", { adminTok = it })
        Spacer(Modifier.height(8.dp))
        Field(publicTok, "Public Bot Token", { publicTok = it })
        Spacer(Modifier.height(8.dp))
        Field(chat, "Group Chat ID (-100...)", { chat = it })
        Spacer(Modifier.height(8.dp))
        Field(pass, "Admin Password", { pass = it })
        Spacer(Modifier.height(14.dp))
        GhostButton("PING TEST") {
            scope.launch {
                p.config = BotConfig(adminTok, publicTok, chat)
                val a = withContext(Dispatchers.IO) { app.repo.pingAdmin() }
                val b = withContext(Dispatchers.IO) { app.repo.pingPublic() }
                log = "admin: $a\npublic: $b"
            }
        }
        Spacer(Modifier.height(8.dp))
        GoldButton("SAVE", adminTok.isNotBlank() && publicTok.isNotBlank() && chat.isNotBlank() && pass.isNotBlank()) {
            p.config = BotConfig(adminTok, publicTok, chat)
            p.adminOk = false
            p.meId = app.repo.sha(pass)
            onDone()
        }
        if (log.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(log, fontSize = 10.sp, color = Gold)
        }
        Spacer(Modifier.height(20.dp))
        PanelCard {
            Text("Zaroori steps", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Gold)
            Text(
                "1. BotFather me dono bot banao\n2. Dono bot ka privacy mode OFF karo (/setprivacy)\n3. Dono bot ko group me add karo\n4. Bot ko admin banao\n5. Group link se -100... id nikaalo\n6. Ping karke OK dekho",
                fontSize = 11.sp
            )
        }
    }
}
