package com.joclegg.puzzles.slideout

import com.joclegg.puzzles.slideout.core.data.PuzzleRef
import com.joclegg.puzzles.slideout.engine.Board
import com.joclegg.puzzles.slideout.engine.Level
import com.joclegg.puzzles.slideout.engine.Puzzle

object SlideOutFixtures {
    fun puzzle(name: String?): PuzzleRef? =
        when (name) {
            "slide_out_two_moves" -> PuzzleRef("fixture_$name", Level.BEGINNER, Puzzle(Board.decode("202H,033V"), par = 2))
            else -> null
        }
}
