package com.venice.tp3.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Felt = Color(0xFF0E3B24)
val FeltDark = Color(0xFF082516)
val Gold = Color(0xFFFFC64B)
val Chip = Color(0xFFE6B23A)
val Bg = Color(0xFF08130D)
val Panel = Color(0xFF12211A)

private val scheme = darkColorScheme(
    primary = Gold,
    onPrimary = Color.Black,
    secondary = Color(0xFF4CD787),
    background = Bg,
    surface = Panel,
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED)
)

@Composable
fun Tp3Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

fun money(paise: Long): String {
    val neg = paise < 0
    val p = kotlin.math.abs(paise)
    val s = "₹%d.%02d".format(p / 100, p % 100)
    return if (neg) "-$s" else s
}
