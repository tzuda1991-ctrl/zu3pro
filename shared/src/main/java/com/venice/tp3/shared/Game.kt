package com.venice.tp3.shared

enum class Phase { WAITING, BET, END }
enum class Act { BLIND, CHAAL, PACK, SEE, SIDESHOW, SHOW }

data class Seat(
    val idx: Int,
    val userId: String,
    val name: String,
    val isBot: Boolean
) {
    var cards: List<Card> = emptyList()
    var seen = false
    var packed = false
    var chips = 0L
    var bet = 0L
}

class Game(
    val boot: Long,
    val seatCount: Int,
    val commissionPct: Int
) {
    var version = 0
    val seats = ArrayList<Seat>()
    var pot = 0L
    var stake = boot
    var turn = -1
    var phase = Phase.WAITING
    var winner = -1
    var message = "Waiting..."
    var actions = 0
    var forceAt = seatCount * 4

    val current: Seat? get() = seats.getOrNull(turn)
    val live: List<Seat> get() = seats.filter { !it.packed }

    fun addSeat(s: Seat) { seats.add(s); touch() }

    fun start() {
        val deck = newDeck().shuffled()
        var i = 0
        for (s in seats) {
            s.cards = listOf(deck[i++], deck[i++], deck[i++])
            val b = minOf(boot, s.chips)
            s.chips -= b
            s.bet = b
            pot += b
        }
        phase = Phase.BET
        turn = seats.indexOfFirst { !it.packed }
        message = "Hand shuru"
        touch()
    }

    private fun touch() { version++ }

    fun pay(s: Seat) = if (s.seen) stake * 2 else stake

    fun act(idx: Int, a: Act) {
        if (phase != Phase.BET || turn != idx) return
        val s = seats.getOrNull(idx) ?: return
        if (s.packed) return
        when (a) {
            Act.SEE -> { s.seen = true; message = "${s.name} ne cards dekhe"; touch(); return }
            Act.PACK -> { s.packed = true; message = "${s.name} ne pack kiya"; actions++ }
            Act.BLIND, Act.CHAAL -> {
                if (a == Act.CHAAL) s.seen = true
                val amt = minOf(pay(s), s.chips)
                s.chips -= amt
                s.bet += amt
                pot += amt
                actions++
                message = "${s.name} ne ${amt / 100} daale"
            }
            Act.SIDESHOW -> {
                val other = live.firstOrNull { it.idx != idx }
                if (s.seen && other != null) {
                    val mine = Eval.evaluate(s.cards)
                    val his = Eval.evaluate(other.cards)
                    if (Eval.compare(mine, his) > 0) {
                        other.packed = true
                        message = "Side show: ${s.name} jeeta, ${other.name} pack"
                    } else {
                        s.packed = true
                        message = "Side show: ${other.name} jeeta, ${s.name} pack"
                    }
                }
                actions++
            }
            Act.SHOW -> { forceShow(); return }
        }
        afterAction()
    }

    private fun afterAction() {
        if (live.size <= 1) { settle(live.firstOrNull()); return }
        if (actions >= forceAt) { forceShow(); return }
        next()
        touch()
    }

    private fun next() {
        var n = turn
        repeat(seatCount) {
            n = (n + 1) % seatCount
            val s = seats.getOrNull(n)
            if (s != null && !s.packed) { turn = n; return }
        }
        turn = -1
    }

    private fun forceShow() {
        val best = live.maxWithOrNull { a, b ->
            Eval.compare(Eval.evaluate(a.cards), Eval.evaluate(b.cards))
        }
        settle(best)
    }

    private fun settle(w: Seat?) {
        phase = Phase.END
        turn = -1
        winner = w?.idx ?: -1
        if (w != null) {
            val cut = pot * commissionPct / 100
            w.chips += pot - cut
            message = "${w.name} jeeta ₹${(pot - cut) / 100}"
        }
        touch()
    }

    fun net(idx: Int): Long {
        val s = seats.getOrNull(idx) ?: return 0
        return if (idx == winner) s.chips else s.chips - 0
    }
}
