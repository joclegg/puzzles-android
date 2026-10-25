package com.joclegg.puzzles.sightlines

import com.joclegg.puzzles.sightlines.core.data.PuzzleRef
import com.joclegg.puzzles.sightlines.engine.Dot
import com.joclegg.puzzles.sightlines.engine.Grid
import com.joclegg.puzzles.sightlines.engine.Puzzle

object SightlinesFixtures {
    private val solution = Grid.parse(4, "BBRB BRBB RBBR BBRR")

    fun puzzle(name: String?): PuzzleRef? =
        when (name) {
            "sightlines_one_left" -> ref(name, setOf(1))
            "sightlines_two_left" -> ref(name, setOf(1, 2))
            else -> null
        }

    private fun ref(
        name: String,
        open: Set<Int>,
    ): PuzzleRef {
        val cells = solution.dots.indices.filter { it !in open }
        val numbers = cells.filter { solution.dots[it] == Dot.BLUE }.associateWith { solution.seen(it) }
        val walls = cells.filter { solution.dots[it] == Dot.RED }.toSet()
        return PuzzleRef("fixture_$name", Puzzle(solution, numbers, walls))
    }
}
