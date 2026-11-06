package com.venice.tp3.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venice.tp3.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

private val botNames = listOf("Ravi", "Sonu", "Aarti", "Vikram", "Pooja", "Rahul", "Neha", "Amit")

@Composable
fun TableScreen(boot: Long, seats: Int, onExit: () -> Unit) {
    val app = UserApp.app()
    val me = app.me
    var game by remember { mutableStateOf<Game?>(null) }
    var v by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (me == null) { onExit(); return@LaunchedEffect }
        val g = Game(boot, seats, app.repo.tableCfg().commissionPct)
        val buyIn = boot * 40
        g.addSeat(Seat(0, me.id, me.name, false).apply { chips = buyIn })
        val names = botNames.shuffled().take(seats - 1)
        names.forEachIndexed { i, n ->
            g.addSeat(Seat(i + 1, "bot$i", n, true).apply { chips = buyIn })
        }
        g.start()
        game = g
        withContext(Dispatchers.IO) { app.repo.adjust(me, -buyIn) }

        while (true) {
            delay(1100)
            val cur = g
            if (cur.phase == Phase.END) break
            val seat = cur.current
            if (seat != null && seat.isBot) {
                cur.act(seat.idx, BotBrain.decide(cur, seat))
                v++
            }
        }
        // settle
        val w = g.seats.firstOrNull { it.idx == g.winner }
        val delta = if (w != null && w.userId == me.id) w.chips else 0L
        withContext(Dispatchers.IO) {
            app.repo.adjust(me, delta)
            app.repo.pushHistory(me.id, "table-${boot}", delta - buyIn)
            app.repo.refreshUser(me.id)?.let { app.me = it }
        }
        result = if (w != null && w.userId == me.id) "JEET GAYE ${money(delta)}" else "HAAR GAYE"
        done = true
    }

    val g = game
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(FeltDark, Color(0xFF04120B))))
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Boot ${money(boot)}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("POT ${money(g?.pot ?: 0)}", color = Gold, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))

        // Opponents
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            g?.seats?.filter { it.idx != 0 }?.forEach { s ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        Avatar(s.name, s.idx * 2, 44)
                        if (s.packed) Box(
                            Modifier.matchParentSize().clip(CircleShape)
                                .background(Color.Black.copy(0.6f))
                        )
                    }
                    Text(s.name, fontSize = 10.sp)
                    Text(money(s.chips), fontSize = 9.sp, color = Gold)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        s.cards.forEach { c ->
                            PlayingCard(c, faceDown = !s.packed && s.cards.isNotEmpty() && !(s.packed))
                        }
                    }
                    if (s.seen) Text("SEEN", fontSize = 8.sp, color = Color(0xFF4CD787))
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // My area
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val mySeat = g?.seats?.firstOrNull { it.idx == 0 }
            if (mySeat != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    mySeat.cards.forEach { c -> PlayingCard(c, faceDown = !mySeat.seen) }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (mySeat.seen) Eval.name(Eval.evaluate(mySeat.cards)[0]) else "Cards dekhne ke liye SEE dabao",
                    fontSize = 11.sp, color = Color(0xFF9FB8AA)
                )
                Spacer(Modifier.height(6.dp))
                Text("Aapka stack: ${money(mySeat.chips)}", fontSize = 12.sp, color = Gold)
            }
            Spacer(Modifier.height(10.dp))
            Text(g?.message ?: "", fontSize = 12.sp, color = Color(0xFFD7E7DD))
        }

        Spacer(Modifier.height(10.dp))

        if (done) {
            PanelCard {
                Text(result, color = Gold, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(10.dp))
                GoldButton("WAPAS") { onExit() }
            }
        } else if (g != null && g.phase == Phase.BET && g.turn == 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionBtn("PACK", Color(0xFFE74C3C)) { g.act(0, Act.PACK); v++ }
                ActionBtn("SEE", Color(0xFF3498DB)) { g.act(0, Act.SEE); v++ }
                if (g.seats[0].seen) ActionBtn("SHOW", Color(0xFF9B59B6)) { g.act(0, Act.SHOW); v++ }
                ActionBtn(
                    if (g.seats[0].seen) "CHAAL ${money(g.pay(g.seats[0]))}" else "BLIND ${money(g.pay(g.seats[0]))}",
                    Gold, dark = true
                ) { g.act(0, if (g.seats[0].seen) Act.CHAAL else Act.BLIND); v++ }
            }
        } else {
            Text("Bot ka turn...", fontSize = 11.sp, color = Color(0xFF7E9587), modifier = Modifier.padding(12.dp))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ActionBtn(label: String, color: Color, dark: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(12.dp))
            .background(color)
            .androidx.compose.foundation.clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (dark) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
