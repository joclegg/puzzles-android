package com.joclegg.puzzles.kakuro.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class SolverTest {
    @Test
    fun `given a cell in runs adding to 3 and 4 when its candidates are found then only 1 fits both`() {
        // Act
        val mask = Solver.candidates(small, List(9) { 0 }, 4)

        // Assert
        assertEquals(listOf(1), Combinations.digitsOf(mask))
    }

    @ParameterizedTest
    @ValueSource(ints = [6, 8, 10, 12])
    fun `given a size when a puzzle is generated then its runs are valid and it has exactly one solution`(size: Int) {
        // Act
        val puzzle = Generator.generate(size, seed = size.toLong())

        // Assert
        assertEquals(
            true,
            puzzle.runs.all { run ->
                run.cells
                    .map { puzzle.solution[it] }
                    .toSet()
                    .size == run.cells.size
            },
        )
        assertEquals(
            true,
            puzzle.solution.indices
                .filter { puzzle.isWhite[it] }
                .all { puzzle.runsOf[it]?.size == 2 },
        )
        assertEquals(listOf(puzzle.solution), Solver.solutions(puzzle, GameState.startValues(puzzle)))
    }
}
