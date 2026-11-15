package com.joclegg.puzzles.crossword.engine

fun puzzle(
    size: Int,
    rows: String,
) = Puzzle(size, rows.filterNot { it.isWhitespace() })

val small = puzzle(3, "CAT O#A WET")

fun GameState.typed(letters: String): GameState = letters.fold(this) { state, letter -> state.type(letter) }

fun GameState.backspaced(times: Int): GameState = (1..times).fold(this) { state, _ -> state.backspace() }
