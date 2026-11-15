package com.joclegg.puzzles.crossword

import com.joclegg.puzzles.crossword.core.data.PuzzleRef
import com.joclegg.puzzles.crossword.engine.Puzzle

object CrosswordFixtures {
    private val puzzle = Puzzle(3, "CATA#OBOW")

    fun puzzle(name: String?): PuzzleRef? =
        when (name) {
            "crossword_one_left", "crossword_empty" -> PuzzleRef("fixture_$name", puzzle)
            else -> null
        }

    fun entries(name: String?): String? =
        when (name) {
            "crossword_one_left" -> "CATA#OBO."
            else -> null
        }
}
