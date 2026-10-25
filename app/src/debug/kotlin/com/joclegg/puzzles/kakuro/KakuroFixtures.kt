package com.joclegg.puzzles.kakuro

import com.joclegg.puzzles.kakuro.core.data.PuzzleRef
import com.joclegg.puzzles.kakuro.engine.Puzzle

object KakuroFixtures {
    private val solution = "000" + "012" + "034"

    fun puzzle(name: String?): PuzzleRef? =
        when (name) {
            "kakuro_one_left" -> PuzzleRef("fixture_$name", Puzzle(3, solution.map { it.digitToInt() }, givens = setOf(4, 5, 7)))
            "kakuro_empty" -> PuzzleRef("fixture_$name", Puzzle(3, solution.map { it.digitToInt() }))
            else -> null
        }
}
