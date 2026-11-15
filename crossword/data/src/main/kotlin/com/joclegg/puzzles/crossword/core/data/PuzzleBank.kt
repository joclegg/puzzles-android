package com.joclegg.puzzles.crossword.core.data

import com.joclegg.puzzles.core.data.TextPuzzleBank
import com.joclegg.puzzles.crossword.engine.Clues
import com.joclegg.puzzles.crossword.engine.Puzzle

class PuzzleBank(
    private val read: (String) -> String,
) : TextPuzzleBank<Int, Puzzle>(read, { "crossword/${fileName(it)}.txt" }, Puzzle::decode) {
    val clues: Clues by lazy { Clues.parse(read(CLUES)) }

    companion object {
        const val CLUES = "crossword/clues.txt"

        fun fileName(size: Int) =
            when (size) {
                11 -> "small"
                13 -> "medium"
                else -> "large"
            }
    }
}
