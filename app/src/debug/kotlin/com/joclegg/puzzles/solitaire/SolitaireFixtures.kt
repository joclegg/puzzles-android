package com.joclegg.puzzles.solitaire

import com.joclegg.puzzles.solitaire.core.data.DealRef
import com.joclegg.puzzles.solitaire.engine.BoardCodec
import com.joclegg.puzzles.solitaire.engine.Card
import com.joclegg.puzzles.solitaire.engine.DrawMode
import com.joclegg.puzzles.solitaire.engine.Suit

object SolitaireFixtures {
    private val foundationsMissingKingOfSpades =
        Suit.entries.joinToString("|") { suit ->
            Card.deck.filter { it.suit == suit && (suit != Suit.SPADES || it.rank < Card.KING) }.joinToString("") { it.code }
        }

    fun deal(name: String?): DealRef? =
        when (name) {
            "simple" -> fixture(name, "5C/AH|/KS|/QH|/|/|/|/;2H3C;;|||")
            "one_move_to_win" -> fixture(name, "/KS|/|/|/|/|/|/;;;$foundationsMissingKingOfSpades")
            else -> null
        }

    private fun fixture(
        name: String,
        board: String,
    ) = DealRef("fixture_$name", DrawMode.DRAW_ONE, BoardCodec.decode(board))
}
