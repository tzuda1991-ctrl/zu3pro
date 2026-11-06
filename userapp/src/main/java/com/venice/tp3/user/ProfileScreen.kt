package com.venice.tp3.user

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venice.tp3.shared.*

@Composable
fun ProfileScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val app = UserApp.app()
    val me = app.me
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onBack) { Text("< Back") }
        Spacer(Modifier.height(10.dp))
        if (me != null) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(me.name, me.color, 84)
                Spacer(Modifier.height(10.dp))
                Text(me.name, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(me.phone, fontSize = 12.sp, color = androidx.compose.ui.graphics.Color(0xFF9AA89F))
                Spacer(Modifier.height(14.dp))
                PanelCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Referral code", fontSize = 11.sp)
                        Text(me.refCode, color = Gold, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(20.dp))
                GhostButton("LOGOUT") { onLogout() }
            }
        }
    }
}
