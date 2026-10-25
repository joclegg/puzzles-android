package com.joclegg.puzzles.sightlines.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class GeneratorTest {
    @ParameterizedTest
    @ValueSource(ints = [4, 5, 6, 7, 8, 9])
    fun `given a size when a puzzle is generated then numbers are right and at most the grid size, and logic solves it`(size: Int) {
        // Act
        val puzzle = Generator.generate(size, seed = size.toLong())

        // Assert
        val solution = puzzle.solution
        assertEquals(emptySet(), puzzle.brokenCells(solution))
        assertEquals(
            true,
            solution.dots.indices
                .filter { solution.dots[it] == Dot.BLUE }
                .all { solution.seen(it) > 0 },
        )
        assertEquals(true, puzzle.numbers.all { (cell, number) -> solution.seen(cell) == number })
        assertEquals(
            true,
            solution.dots.indices
                .filter { solution.dots[it] == Dot.BLUE }
                .all { solution.seen(it) <= size },
        )
        assertEquals(solution, Solver.solveLogically(puzzle.givens, puzzle.numbers))
    }

    @ParameterizedTest
    @ValueSource(longs = [1, 2, 3])
    fun `given a puzzle when it is encoded and decoded then it is unchanged`(seed: Long) {
        // Arrange
        val puzzle = Generator.generate(9, seed)

        // Act
        val decoded = Puzzle.decode(puzzle.encode())

        // Assert
        assertEquals(puzzle, decoded)
    }
}
