package com.joclegg.puzzles.slideout.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertEquals

class SolverTest {
    @Test
    fun `given a blocker in the way when the shortest solution is found then it moves the blocker then the red block`() {
        // Arrange
        val board = Board.decode("202H,033V")

        // Act
        val moves = Solver.shortest(board)

        // Assert
        assertEquals(listOf(Move(1, 3), Move(0, 4)), moves)
    }

    @Test
    fun `given a red block walled in when it is solved then there is no solution`() {
        // Act
        val moves = Solver.shortest(Board.decode("202H,232H"))

        // Assert
        assertEquals(null, moves)
    }

    @ParameterizedTest
    @EnumSource(Level::class, names = ["BEGINNER", "EASY", "MEDIUM"])
    fun `given a level when a puzzle is generated then its fewest moves are in the level's band`(level: Level) {
        // Act
        val puzzle = Generator.generate(level, seed = 3)

        // Assert
        assertEquals(true, puzzle != null && puzzle.par in level.moves && Solver.shortest(puzzle.board)?.size == puzzle.par)
    }
}
