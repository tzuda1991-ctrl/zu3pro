package com.venice.tp3.user

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venice.tp3.shared.*

@Composable
fun SetupScreen(onDone: () -> Unit) {
    val prefs = UserApp.app().prefs
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TEEN PATTI 3", color = Gold, fontWeight = FontWeight.Black, fontSize = 28.sp)
        Spacer(Modifier.height(6.dp))
        Text("Setup code daalo (admin se milega)", fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))
        Field(code, "Setup Code", { code = it })
        if (err.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(err, color = androidx.compose.ui.graphics.Color.Red, fontSize = 12.sp)
        }
        Spacer(Modifier.height(18.dp))
        GoldButton("CONNECT") {
            val c = BotConfig.decode(code)
            if (c == null || !c.ready) { err = "Invalid setup code" }
            else { prefs.config = c; onDone() }
        }
    }
}
