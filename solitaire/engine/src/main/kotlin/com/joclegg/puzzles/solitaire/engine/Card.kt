package com.joclegg.puzzles.solitaire.engine

enum class Suit(
    val symbol: Char,
    val code: Char,
    val isRed: Boolean,
) {
    CLUBS('♣', 'C', false),
    DIAMONDS('♦', 'D', true),
    HEARTS('♥', 'H', true),
    SPADES('♠', 'S', false),
}

data class Card(
    val rank: Int,
    val suit: Suit,
) {
    init {
        require(rank in ACE..KING) { "Rank $rank is not between $ACE and $KING" }
    }

    val rankLabel: String
        get() =
            when (rank) {
                ACE -> "A"
                JACK -> "J"
                QUEEN -> "Q"
                KING -> "K"
                else -> rank.toString()
            }

    val code: String get() = "${RANK_CODES[rank - 1]}${suit.code}"

    fun stacksOnTableau(
        below: Card,
        order: BuildOrder = BuildOrder.DOWN,
    ) = rank + order.step == below.rank && suit.isRed != below.suit.isRed

    companion object {
        const val ACE = 1
        const val JACK = 11
        const val QUEEN = 12
        const val KING = 13
        private const val RANK_CODES = "A23456789TJQK"

        val deck: List<Card> = Suit.entries.flatMap { suit -> (ACE..KING).map { Card(it, suit) } }

        fun parse(code: String): Card {
            require(code.length == 2) { "Card code '$code' must be two characters" }
            val rank = RANK_CODES.indexOf(code[0]) + 1
            val suit = Suit.entries.first { it.code == code[1] }
            return Card(rank, suit)
        }
    }
}
