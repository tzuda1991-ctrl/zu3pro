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
fun SetupScreen(onSaved: () -> Unit) {
    val app = AdminApp.app()
    val cfg = app.prefs.config

    var adminToken by remember { mutableStateOf(cfg.adminToken) }
    var publicToken by remember { mutableStateOf(cfg.publicToken) }
    var chatId by remember { mutableStateOf(cfg.chatId) }
    var dbUrl by remember { mutableStateOf(cfg.dbUrl) }
    var pass by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())
    ) {
        Text("ADMIN SETUP", color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "DB URL zaroori hai. Firebase Realtime DB banao, rules public karo, URL paste karo.",
            fontSize = 11.sp, color = Color(0xFF9AA89F)
        )
        Spacer(Modifier.height(12.dp))

        Field(dbUrl, "DB URL (https://xxx.firebaseio.com)", { dbUrl = it.trim() })
        Spacer(Modifier.height(8.dp))
        Field(adminToken, "Admin Bot Token", { adminToken = it.trim() })
        Spacer(Modifier.height(8.dp))
        Field(publicToken, "Public Bot Token (optional)", { publicToken = it.trim() })
        Spacer(Modifier.height(8.dp))
        Field(chatId, "Group Chat ID (-100xxxxxxxxxx)", { chatId = it.trim() })
        Spacer(Modifier.height(8.dp))
        Field(pass, "Admin Password", { pass = it })

        if (msg.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(msg, fontSize = 11.sp, color = Gold)
        }

        Spacer(Modifier.height(14.dp))
        GhostButton("PING TEST") {
            busy = true
            scope.launch {
                msg = withContext(Dispatchers.IO) {
                    "DB: ${Rtdb.ping(dbUrl)} | BOT: ${TgApi.ping(adminToken)}"
                }
                busy = false
            }
        }

        Spacer(Modifier.height(8.dp))
        GoldButton("SAVE + LOGIN", !busy && pass.isNotBlank() && dbUrl.isNotBlank()) {
            if (adminToken.isBlank()) { msg = "Admin bot token daalo"; return@GoldButton }
            if (chatId.isBlank()) { msg = "Group chat id daalo"; return@GoldButton }
            if (dbUrl.isBlank()) { msg = "DB URL daalo"; return@GoldButton }
            app.prefs.config = BotConfig(
                adminToken = adminToken.trim(),
                publicToken = publicToken.trim(),
                chatId = chatId.trim(),
                dbUrl = dbUrl.trim()
            )
            app.prefs.meId = app.repo.sha(pass)
            app.prefs.adminOk = true
            app.syncBot()
            onSaved()
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Firebase → Realtime Database → Rules:\n{ \"rules\": { \".read\": true, \".write\": true } }",
            fontSize = 10.sp, color = Color(0xFF6F8579)
        )
        Spacer(Modifier.height(10.dp))
        Text("Save ke baad apne admin bot ko Telegram me /start bhejo.", fontSize = 10.sp, color = Gold)
    }
}
