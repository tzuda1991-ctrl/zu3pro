package com.venice.tp3.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venice.tp3.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UsersScreen(onBack: () -> Unit) {
    val app = AdminApp.app()
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf<User?>(null) }
    var delta by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(q) {
        withContext(Dispatchers.IO) {
            val list = app.repo.allUsers()
                .filter { q.isBlank() || it.name.contains(q, true) || it.phone.contains(q) }
            withContext(Dispatchers.Main) { users = list }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onBack) { Text("< Back") }
            Text("USERS", color = Gold)
        }
        Field(q, "Search naam / phone", { q = it })
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(users) { u ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Panel)
                        .clickable { edit = u }
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
            onDismissRequest = { edit = null },
            title = { Text(u.name) },
            text = {
                Column {
                    Text("Balance: ${money(u.balance)}", fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Field(delta, "Amount ₹ (+/-)", { delta = it })
                }
            },
            confirmButton = {
                TextButton({
                    val d = (delta.toLongOrNull() ?: 0) * 100
                    scope.launch {
                        withContext(Dispatchers.IO) { app.repo.adjust(u, d) }
                        edit = null
                        delta = ""
                        users = withContext(Dispatchers.IO) { app.repo.allUsers() }
                    }
                }) { Text("Balance badlo") }
            },
            dismissButton = {
                Row {
                    TextButton({
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val fresh = app.repo.refreshUser(u.id) ?: return@withContext
                                fresh.status = if (fresh.active) "BANNED" else "ACTIVE"
                                app.repo.saveUser(fresh)
                            }
                            edit = null
                            users = withContext(Dispatchers.IO) { app.repo.allUsers() }
                        }
                    }) { Text(if (u.active) "Ban" else "Unban") }
                    TextButton({ edit = null }) { Text("Band") }
                }
            }
        )
    }
}
