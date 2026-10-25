package com.joclegg.puzzles.twotone

import com.joclegg.puzzles.twotone.core.data.PuzzleRef
import com.joclegg.puzzles.twotone.engine.Grid
import com.joclegg.puzzles.twotone.engine.Puzzle
import com.joclegg.puzzles.twotone.engine.Tile

object TwoToneFixtures {
    private val solution = Grid.parse(4, "RBBR BRRB RBRB BRBR")

    fun puzzle(name: String?): PuzzleRef? =
        when (name) {
            "two_tone_one_left" -> PuzzleRef("fixture_$name", Puzzle(solution.with(1, Tile.EMPTY), solution))
            "two_tone_two_left" -> PuzzleRef("fixture_$name", Puzzle(solution.with(1, Tile.EMPTY).with(2, Tile.EMPTY), solution))
            else -> null
        }
}
