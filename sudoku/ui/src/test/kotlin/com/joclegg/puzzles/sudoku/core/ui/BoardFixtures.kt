package com.joclegg.puzzles.sudoku.core.ui

import com.joclegg.puzzles.sudoku.engine.Puzzle

internal val boardPuzzle =
    Puzzle(
        "530070000600195000098000060800060003400803001700020006060000280000419005000080079".map { it.digitToInt() },
        "534678912672195348198342567859761423426853791713924856961537284287419635345286179".map { it.digitToInt() },
    )
