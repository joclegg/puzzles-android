package com.joclegg.puzzles.solitaire.engine

object BoardCodec {
    private const val SECTIONS = ";"
    private const val PILES = "|"
    private const val FACE_UP = "/"

    fun encode(board: Board): String =
        listOf(
            board.tableau.joinToString(PILES) { column ->
                cards(column.cards.take(column.faceUpFrom)) + FACE_UP + cards(column.faceUp)
            },
            cards(board.stock),
            cards(board.waste),
            board.foundations.joinToString(PILES, transform = ::cards),
        ).joinToString(SECTIONS)

    fun decode(text: String): Board {
        val (tableau, stock, waste, foundations) = text.split(SECTIONS)
        return Board(
            tableau =
                tableau.split(PILES).map { column ->
                    val (hidden, shown) = column.split(FACE_UP)
                    val faceDown = cards(hidden)
                    Column(faceDown + cards(shown), faceDown.size)
                },
            stock = cards(stock),
            waste = cards(waste),
            foundations = foundations.split(PILES).map(::cards),
        )
    }

    private fun cards(cards: List<Card>) = cards.joinToString("") { it.code }

    private fun cards(text: String) = text.chunked(2).map(Card::parse)
}

object MoveCodec {
    private const val DRAW = "D"

    fun encode(moves: List<Move>): String =
        moves.joinToString(",") { move ->
            when (move) {
                Move.Draw -> DRAW
                is Move.Transfer -> "${pile(move.from)}${move.count}${pile(move.to)}"
            }
        }

    fun decode(text: String): List<Move> =
        text.split(',').filter { it.isNotEmpty() }.map { move ->
            if (move == DRAW) {
                Move.Draw
            } else {
                Move.Transfer(pile(move.take(2)), move.drop(2).dropLast(2).toInt(), pile(move.takeLast(2)))
            }
        }

    private fun pile(pile: Pile): String =
        when (pile) {
            Pile.Waste -> "W0"
            is Pile.Foundation -> "F${pile.index}"
            is Pile.Tableau -> "T${pile.index}"
        }

    private fun pile(code: String): Pile {
        val index = code[1].digitToInt()
        return when (code[0]) {
            'W' -> Pile.Waste
            'F' -> Pile.Foundation(index)
            else -> Pile.Tableau(index)
        }
    }
}
