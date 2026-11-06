package com.venice.tp3.shared

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlayingCard(card: Card?, faceDown: Boolean, rot: Float = 0f) {
    val shape = RoundedCornerShape(7.dp)
    if (faceDown || card == null) {
        Box(
            Modifier.size(42.dp, 60.dp).rotate(rot).clip(shape)
                .background(Brush.linearGradient(listOf(Color(0xFF1B4D8F), Color(0xFF0D2B52))))
                .border(1.dp, Color(0xFF3E7BB8), shape),
            contentAlignment = Alignment.Center
        ) {
            Text("3", color = Color(0xFF7FB2E0), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    } else {
        Box(
            Modifier.size(42.dp, 60.dp).rotate(rot).clip(shape)
                .background(Color(0xFFF7F7F2))
                .border(1.dp, Color(0xFFCFCFC6), shape)
                .padding(4.dp)
        ) {
            Text(
                card.label,
                color = if (card.s.red) Color(0xFFC0392B) else Color(0xFF111111),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                card.s.sym,
                color = if (card.s.red) Color(0xFFC0392B) else Color(0xFF111111),
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
