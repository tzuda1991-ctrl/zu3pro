package com.venice.tp3.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venice.tp3.shared.*

@Composable
fun LoginScreen(onIn: () -> Unit, onSetup: () -> Unit) {
    val app = AdminApp.app()
    var pass by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("ADMIN LOGIN", color = Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
        Spacer(Modifier.height(16.dp))
        Field(pass, "Password", { pass = it })
        if (err.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(err, color = Color.Red, fontSize = 11.sp)
        }
        Spacer(Modifier.height(14.dp))
        GoldButton("UNLOCK", pass.isNotBlank()) {
            if (app.repo.sha(pass) == app.prefs.meId) {
                app.prefs.adminOk = true
                app.syncBot()
                onIn()
            } else err = "Galat password"
        }
        TextButton({ onSetup() }, Modifier.align(Alignment.CenterHorizontally)) { Text("Setup badlo") }
    }
}
