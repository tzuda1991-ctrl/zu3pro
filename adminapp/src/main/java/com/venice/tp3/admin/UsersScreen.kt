package com.venice.tp3.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun UsersScreen(onBack: () -> Unit) {
    val app = AdminApp.app()
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf<User?>(null) }
    var amt by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun reload() = withContext(Dispatchers.IO) {
        val query = q
        val list = app.repo.allUsers()
            .filter { query.isBlank() || it.name.contains(query, true) || it.phone.contains(query) }
            .sortedByDescending { it.createdAt }
        withContext(Dispatchers.Main) { users = list }
    }

    LaunchedEffect(Unit) {
        while (true) {
            reload()
            delay(4000)
        }
    }
    LaunchedEffect(q) { reload() }

    fun move(id: String, sign: Int) {
        val rupees = amt.toLongOrNull() ?: 0L
        if (rupees <= 0L) {
            msg = "Pehle amount daalo"
            return
        }
        busy = true
        scope.launch {
            val delta = rupees * 100L * sign
            val ok = withContext(Dispatchers.IO) { app.repo.adjust(id, delta, note.ifBlank { "manual" }) }
            msg = if (ok) "${if (sign > 0) "+" else "-"}${money(rupees * 100L)} ho gaya" else "Save fail"
            edit = withContext(Dispatchers.IO) { app.repo.refreshUser(id) }
            amt = ""
            note = ""
            busy = false
            reload()
        }
    }

    fun setExact(id: String) {
        val rupees = amt.toLongOrNull() ?: 0L
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) { app.repo.setBalance(id, rupees * 100L) }
            msg = if (ok) "Balance set: ${money(rupees * 100L)}" else "Save fail"
            edit = withContext(Dispatchers.IO) { app.repo.refreshUser(id) }
            amt = ""
            note = ""
            busy = false
            reload()
        }
    }

    fun toggleBan(id: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                app.repo.refreshUser(id)?.let { app.repo.setStatus(id, if (it.active) "BANNED" else "ACTIVE") }
            }
            edit = withContext(Dispatchers.IO) { app.repo.refreshUser(id) }
            reload()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onBack) { Text("< Back") }
            Text("USERS · ${users.size}", color = Gold, fontWeight = FontWeight.Bold)
        }
        Field(q, "Search naam / phone", { q = it })
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(users, key = { it.id }) { u ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Panel)
                        .clickable {
                            edit = u
                            amt = ""
                            note = ""
                            msg = ""
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(u.name, u.color, 36)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(u.name, fontSize = 13.sp)
                        Text(u.phone, fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(money(u.balance), color = Gold, fontSize = 12.sp)
                        StatusChip(u.status)
                    }
                }
            }
        }
    }

    edit?.let { u ->
        AlertDialog(
            onDismissRequest = { edit = null; msg = "" },
            title = { Text(u.name, fontSize = 15.sp) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(u.phone, fontSize = 11.sp, color = Color(0xFF9AA89F))
                    Text("Balance: ${money(u.balance)}", color = Gold, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(100L, 500L, 1000L).forEach { v -> QuickChip("+$v") { amt = v.toString() } }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(100L, 500L, 1000L).forEach { v -> QuickChip("-$v") { amt = v.toString() } }
                    }
                    Spacer(Modifier.height(10.dp))
                    Field(amt, "Amount ₹", { amt = it.filter(Char::isDigit) }, numeric = true)
                    Spacer(Modifier.height(6.dp))
                    Field(note, "Note (optional)", { note = it })
                    if (msg.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(msg, fontSize = 11.sp, color = Gold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f)) { GoldButton("+ ADD", !busy && amt.isNotBlank()) { move(u.id, 1) } }
                        Box(Modifier.weight(1f)) { GhostButton("− CUT") { if (!busy) move(u.id, -1) } }
                    }
                    Spacer(Modifier.height(6.dp))
                    GhostButton(if (amt.isBlank()) "SET EXACT" else "SET EXACT ₹$amt") { if (!busy) setExact(u.id) }
                    Spacer(Modifier.height(6.dp))
                    GhostButton(if (u.active) "BAN USER" else "UNBAN USER") { toggleBan(u.id) }
                }
            },
            confirmButton = { TextButton({ edit = null; msg = "" }) { Text("Close") } }
        )
    }
}

@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1B3326))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) { Text(label, fontSize = 11.sp, color = Gold) }
}
