package com.joclegg.puzzles.slideout.core.data

import com.joclegg.puzzles.core.data.TextPuzzleBank
import com.joclegg.puzzles.slideout.engine.Level
import com.joclegg.puzzles.slideout.engine.Puzzle

class PuzzleBank(
    read: (String) -> String,
) : TextPuzzleBank<Level, Puzzle>(read, { "slideout/${it.name.lowercase()}.txt" }, Puzzle::decode)
