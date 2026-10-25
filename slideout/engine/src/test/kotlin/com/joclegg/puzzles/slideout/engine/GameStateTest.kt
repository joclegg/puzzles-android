package com.joclegg.puzzles.slideout.engine

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GameStateTest {
    private val puzzle = Puzzle(Board.decode("202H,033V"), par = 2)

    @Test
    fun `given the blocker moved and the red block slid out when the game is checked then it is solved in two moves`() {
        // Act
        val game = GameState(puzzle).slide(1, 3).slide(0, 4)

        // Assert
        assertEquals(true to 2, game.isSolved to game.moves.size)
    }

    @Test
    fun `given one block slid twice in a row when moves are counted then it is one move`() {
        // Act
        val game = GameState(puzzle).slide(1, 2).slide(1, 3)

        // Assert
        assertEquals(listOf(Move(1, 3)), game.moves)
    }

    @Test
    fun `given a block slid away and straight back when moves are counted then it costs nothing`() {
        // Act
        val game = GameState(puzzle).slide(1, 3).slide(1, 0)

        // Assert
        assertEquals(emptyList(), game.moves)
    }

    @Test
    fun `given a slide past another block when it is tried then nothing moves`() {
        // Act
        val game = GameState(puzzle).slide(0, 4)

        // Assert
        assertEquals(emptyList(), game.moves)
    }

    @Test
    fun `given a puzzle when a hint is asked for then it is the first move of a shortest solution`() {
        // Act
        val hint = GameState(puzzle).withHint().hint

        // Assert
        assertEquals(Move(1, 3), hint)
    }

    @Test
    fun `given moves saved and replayed when the board is rebuilt then it matches`() {
        // Arrange
        val game = GameState(puzzle).slide(1, 3)

        // Act
        val replayed = GameState(puzzle, MoveCodec.decode(MoveCodec.encode(game.moves)))

        // Assert
        assertEquals(game.board, replayed.board)
    }
}
