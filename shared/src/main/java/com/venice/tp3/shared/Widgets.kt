package com.venice.tp3.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val palette = listOf(
    Color(0xFF6C5CE7), Color(0xFF00B894), Color(0xFFE17055),
    Color(0xFF0984E3), Color(0xFFD63031), Color(0xFFFDCB6E),
    Color(0xFF00CEC9), Color(0xFFB71540)
)

@Composable
fun Avatar(name: String, color: Int, size: Int = 48) {
    val base = palette[color % palette.size]
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(base, base.copy(alpha = 0.5f)))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 2.2).sp
        )
    }
}

@Composable
fun GoldButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) { Text(text) }
}

@Composable
fun Field(
    value: String,
    label: String,
    onChange: (String) -> Unit,
    numeric: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number)
        else KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(1.dp, Color(0xFF1E3A2B), RoundedCornerShape(18.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun StatBox(label: String, value: String, accent: Color = Gold, onClick: (() -> Unit)? = null) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14261C))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, color = Color(0xFF9AA89F), fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun StatusChip(status: String) {
    val c = when (status) {
        "APPROVED", "ACTIVE" -> Color(0xFF2ECC71)
        "REJECTED", "BANNED" -> Color(0xFFE74C3C)
        else -> Color(0xFFF39C12)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(status, color = c, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
