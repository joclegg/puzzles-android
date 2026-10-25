package com.joclegg.puzzles.wordwheel

import com.joclegg.puzzles.wordwheel.core.data.PuzzleRef
import com.joclegg.puzzles.wordwheel.engine.Crossword
import com.joclegg.puzzles.wordwheel.engine.Level
import com.joclegg.puzzles.wordwheel.engine.Puzzle

object WordWheelFixtures {
    fun puzzle(name: String?): PuzzleRef? =
        when (name) {
            "word_wheel_two_words" -> {
                PuzzleRef("fixture_$name", Level.FIVE, Puzzle("TRACE", Crossword.decode("CRATE@0,0,A|TRACE@0,1,D")))
            }

            else -> {
                null
            }
        }
}
