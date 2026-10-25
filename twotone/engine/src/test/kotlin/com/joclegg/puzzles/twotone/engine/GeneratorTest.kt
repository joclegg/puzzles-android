package com.joclegg.puzzles.twotone.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class GeneratorTest {
    @ParameterizedTest
    @ValueSource(ints = [4, 6, 8, 10, 12])
    fun `given a size when a puzzle is generated then its solution follows every rule and logic alone solves it`(size: Int) {
        // Act
        val puzzle = Generator.generate(size, seed = size.toLong())

        // Assert
        assertEquals(emptySet(), puzzle.solution.brokenCells())
        assertEquals(true, puzzle.solution.isFull)
        assertEquals(puzzle.solution, Solver.solveLogically(puzzle.givens))
    }

    @ParameterizedTest
    @ValueSource(longs = [1, 2, 3])
    fun `given a seed when two puzzles are generated then they match`(seed: Long) {
        // Act
        val puzzles = List(2) { Generator.generate(6, seed) }

        // Assert
        assertEquals(puzzles[0], puzzles[1])
    }
}
