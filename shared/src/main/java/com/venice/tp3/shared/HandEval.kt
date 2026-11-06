package com.venice.tp3.shared

/**
 * cat: 6 Trail, 5 Pure Sequence, 4 Sequence, 3 Color, 2 Pair, 1 High Card
 * Tiebreak lexicographic. A-2-3 sabse upar.
 */
object Eval {
    fun evaluate(cards: List<Card>): IntArray {
        val r = cards.map { it.r }.sortedDescending()
        val sameSuit = cards.map { it.s }.distinct().size == 1
        val trail = r[0] == r[2]
        val seq = isSeq(r)
        return when {
            trail -> intArrayOf(6, r[0], 0, 0)
            seq && sameSuit -> intArrayOf(5) + seqTb(r)
            seq -> intArrayOf(4) + seqTb(r)
            sameSuit -> intArrayOf(3, r[0], r[1], r[2])
            r[0] == r[1] || r[1] == r[2] -> {
                val pair = if (r[0] == r[1]) r[0] else r[1]
                val kick = if (r[0] == r[1]) r[2] else r[0]
                intArrayOf(2, pair, kick, 0)
            }
            else -> intArrayOf(1, r[0], r[1], r[2])
        }
    }

    private fun isSeq(r: List<Int>): Boolean {
        if (r.distinct().size != 3) return false
        if (r[0] - r[1] == 1 && r[1] - r[2] == 1) return true
        return r[0] == 14 && r[1] == 3 && r[2] == 2
    }

    private fun seqTb(r: List<Int>): IntArray =
        if (r[0] == 14 && r[1] == 3 && r[2] == 2) intArrayOf(15, 3, 2)
        else intArrayOf(r[0], r[1], r[2])

    fun compare(a: IntArray, b: IntArray): Int {
        for (i in 0 until 4) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    fun name(cat: Int) = when (cat) {
        6 -> "Trail"; 5 -> "Pure Sequence"; 4 -> "Sequence"
        3 -> "Color"; 2 -> "Pair"; else -> "High Card"
    }
}
