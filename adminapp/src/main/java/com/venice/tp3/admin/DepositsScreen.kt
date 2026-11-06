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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.venice.tp3.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DepositsScreen(onBack: () -> Unit) {
    val app = AdminApp.app()
    var list by remember { mutableStateOf<List<Deposit>>(emptyList()) }
    var urls by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) {
                val l = app.repo.allDeposits().sortedByDescending { it.ts }
                val u = l.mapNotNull { d ->
                    if (d.proofFileId.isNotBlank()) d.id to (app.repo.db.proofUrl(d.proofFileId) ?: "") else null
                }.toMap()
                withContext(Dispatchers.Main) { list = l; urls = u }
            }
            delay(5000)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onBack) { Text("< Back") }
            Text("DEPOSITS", color = Gold)
        }
        LazyColumn(Modifier.weight(1f)) {
            items(list) { d ->
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Panel).padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${d.userName} · ${money(d.amount)}", fontSize = 13.sp)
                            Text("UTR ${d.utr}", fontSize = 10.sp)
                        }
                        StatusChip(d.status)
                    }
                    urls[d.id]?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(6.dp))
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    if (d.status == "PENDING") {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) {
                                GoldButton("APPROVE") {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { app.repo.setDeposit(d, "APPROVED", "ok") }
                                        list = app.repo.allDeposits().sortedByDescending { it.ts }
                                    }
                                }
                            }
                            Box(Modifier.weight(1f)) {
                                GhostButton("REJECT") {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { app.repo.setDeposit(d, "REJECTED", "rejected") }
                                        list = app.repo.allDeposits().sortedByDescending { it.ts }
                                    }
                                }
                            }
                        }
                    }
                    if (d.note.isNotBlank()) Text(d.note, fontSize = 9.sp, color = Color(0xFF9AA89F))
                }
            }
        }
    }
}
