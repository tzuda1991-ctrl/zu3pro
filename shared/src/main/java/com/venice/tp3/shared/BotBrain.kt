package com.venice.tp3.shared

import kotlin.random.Random

object BotBrain {
    fun decide(g: Game, s: Seat): Act {
        val h = Eval.evaluate(s.cards)
        val power = when (h[0]) {
            6, 5 -> 1.0f
            4 -> 0.85f
            3 -> 0.68f
            2 -> 0.5f
            else -> 0.15f + (h[1] - 2) / 45f
        }
        val r = Random.nextFloat()
        if (!s.seen && power > 0.6f && r > 0.4f) return Act.SEE
        if (power < 0.25f && g.pot > g.boot * 8) return Act.PACK
        if (power < 0.4f && r < 0.22f) return Act.PACK
        if (power > 0.8f && r < 0.12f && g.live.size >= 2) return Act.SHOW
        if (s.seen && g.live.size == 2 && power > 0.5f && r < 0.2f) return Act.SIDESHOW
        return if (s.seen) Act.CHAAL else Act.BLIND
    }
}
