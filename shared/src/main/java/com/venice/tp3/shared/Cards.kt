package com.venice.tp3.shared

enum class Suit(val sym: String, val red: Boolean) { S("♠", false), H("♥", true), D("♦", true), C("♣", false) }

data class Card(val r: Int, val s: Suit) {
    val label: String get() = when (r) {
        11 -> "J"; 12 -> "Q"; 13 -> "K"; 14 -> "A"; else -> r.toString()
    }
}

fun newDeck(): List<Card> =
    Suit.entries.flatMap { su -> (2..14).map { Card(it, su) } }
