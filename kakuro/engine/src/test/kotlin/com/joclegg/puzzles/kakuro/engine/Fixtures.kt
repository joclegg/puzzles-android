package com.joclegg.puzzles.kakuro.engine

fun puzzle(
    size: Int,
    rows: String,
    givens: Set<Int> = emptySet(),
) = Puzzle(size, rows.filterNot { it.isWhitespace() }.map { it.digitToInt() }, givens)

val small = puzzle(3, "000 012 034")
