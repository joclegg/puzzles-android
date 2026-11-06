package com.joclegg.puzzles.kakuro.engine

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BoardTest {
    @Test
    fun `given a filled grid when its runs are found then each has its cells, sum and clue cell`() {
        // Act
        val runs = small.runs

        // Assert
        assertEquals(
            listOf(
                Run(listOf(4, 5), 3, true, 3),
                Run(listOf(7, 8), 7, true, 6),
                Run(listOf(4, 7), 4, false, 1),
                Run(listOf(5, 8), 6, false, 2),
            ),
            runs,
        )
    }

    @Test
    fun `given a puzzle when it is encoded and decoded then it is unchanged`() {
        // Arrange
        val puzzle = small.copy(givens = setOf(4))

        // Act
        val decoded = Puzzle.decode(puzzle.encode())

        // Assert
        assertEquals(puzzle, decoded)
    }
}
