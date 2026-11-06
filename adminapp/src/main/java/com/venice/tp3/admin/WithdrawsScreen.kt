package com.venice.tp3.admin

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venice.tp3.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WithdrawsScreen(onBack: () -> Unit) {
    val app = AdminApp.app()
    var list by remember { mutableStateOf<List<Withdraw>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) {
                val l = app.repo.allWithdraws().sortedByDescending { it.ts }
                withContext(Dispatchers.Main) { list = l }
            }
            delay(5000)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onBack) { Text("< Back") }
            Text("WITHDRAWS", color = Gold)
        }
        LazyColumn(Modifier.weight(1f)) {
            items(list) { w ->
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Panel).padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${w.userName} · ${money(w.amount)}", fontSize = 13.sp)
                            Text(w.upi, fontSize = 10.sp, color = Gold)
                        }
                        StatusChip(w.status)
                    }
                    if (w.status == "PENDING") {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) {
                                GoldButton("PAID") {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { app.repo.setWithdraw(w, "APPROVED", "paid") }
                                        list = app.repo.allWithdraws().sortedByDescending { it.ts }
                                    }
                                }
                            }
                            Box(Modifier.weight(1f)) {
                                GhostButton("REJECT") {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { app.repo.setWithdraw(w, "REJECTED", "refund") }
                                        list = app.repo.allWithdraws().sortedByDescending { it.ts }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
