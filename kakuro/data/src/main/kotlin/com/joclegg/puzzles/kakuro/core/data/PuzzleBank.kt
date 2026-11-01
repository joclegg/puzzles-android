package com.joclegg.puzzles.kakuro.core.data

import com.joclegg.puzzles.core.data.TextPuzzleBank
import com.joclegg.puzzles.kakuro.engine.Puzzle

class PuzzleBank(
    read: (String) -> String,
) : TextPuzzleBank<Int, Puzzle>(read, { "kakuro/$it.txt" }, Puzzle::decode)
