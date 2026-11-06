package com.venice.tp3.user

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(onLoggedIn: () -> Unit) {
    val app = UserApp.app()
    var mode by remember { mutableStateOf(0) } // 0 login 1 register
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (mode == 0) "LOGIN" else "REGISTER",
            color = Gold, fontWeight = FontWeight.Black, fontSize = 26.sp
        )
        Spacer(Modifier.height(20.dp))
        Field(phone, "Mobile Number", { phone = it.filter { c -> c.isDigit() }.take(10) }, numeric = true)
        Spacer(Modifier.height(10.dp))
        if (mode == 1) {
            Field(name, "Naam", { name = it })
            Spacer(Modifier.height(10.dp))
        }
        Field(pin, "4-digit PIN", { pin = it.filter { c -> c.isDigit() }.take(4) }, numeric = true)
        Spacer(Modifier.height(8.dp))
        if (msg.isNotEmpty()) Text(msg, color = Color(0xFFE74C3C), fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        GoldButton(if (mode == 0) "LOGIN" else "CREATE ACCOUNT", !busy && phone.length == 10 && pin.length == 4) {
            busy = true; msg = ""
            scope.launch {
                val r = withContext(Dispatchers.IO) {
                    if (mode == 0) app.repo.login(phone, pin)
                    else app.repo.register(phone, name.ifBlank { "Player" }, pin)
                }
                busy = false
                r.onSuccess {
                    app.me = it
                    app.prefs.meId = it.id
                    onLoggedIn()
                }.onFailure { msg = it.message ?: "Error" }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton({ mode = 1 - mode; msg = "" }, Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (mode == 0) "Naya account banao" else "Login pe wapas")
        }
    }
}
