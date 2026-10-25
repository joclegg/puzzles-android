package com.joclegg.puzzles.solitaire.engine

fun board(text: String) = BoardCodec.decode(text)

fun state(
    text: String,
    drawMode: DrawMode = DrawMode.DRAW_ONE,
    oneTapMove: Boolean = true,
) = GameState(board(text), drawMode, oneTapMove = oneTapMove)

val fullFoundations: String = Suit.entries.joinToString("|") { suit -> Card.deck.filter { it.suit == suit }.joinToString("") { it.code } }

fun foundationsMissingKings(): String =
    Suit.entries.joinToString("|") { suit -> Card.deck.filter { it.suit == suit && it.rank < Card.KING }.joinToString("") { it.code } }
