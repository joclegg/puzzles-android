package com.joclegg.puzzles.sudoku.engine

internal const val EASY_GIVENS = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
internal const val EASY_SOLUTION = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"

internal fun digits(text: String) = text.map { it.digitToInt() }

internal val easyPuzzle = Puzzle(digits(EASY_GIVENS), digits(EASY_SOLUTION))
