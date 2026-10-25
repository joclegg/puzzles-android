package com.joclegg.puzzles.sightlines.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GameStateTest {
    private val solution = Grid.parse(4, "BBRB BRBB RBBR BBRB")
    private val puzzle = Puzzle(solution, numbers = mapOf(0 to 2), walls = setOf(2))

    @Test
    fun `given an empty dot when it is tapped three times then it goes blue, red and empty again`() {
        // Arrange
        val game = GameState(puzzle)

        // Act
        val dots = (1..3).runningFold(game) { state, _ -> state.tap(1) }.map { it.grid.dots[1] }

        // Assert
        assertEquals(listOf(Dot.EMPTY, Dot.BLUE, Dot.RED, Dot.EMPTY), dots)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 2])
    fun `given a starting dot when it is tapped then it does not change`(cell: Int) {
        // Act
        val after = GameState(puzzle).tap(cell)

        // Assert
        assertEquals(puzzle.givens.dots[cell], after.grid.dots[cell])
    }

    @Test
    fun `given taps saved as changes when the game is replayed then the grid matches`() {
        // Arrange
        val played = GameState(puzzle).tap(1).tap(5).tap(5)

        // Act
        val replayed = GameState.replay(puzzle, ChangeCodec.decode(ChangeCodec.encode(played.changes)))

        // Assert
        assertEquals(played.grid, replayed.grid)
    }

    @Test
    fun `given a wrong dot when a hint is asked for then the wrong dot is pointed out`() {
        // Arrange
        val game = GameState(puzzle).tap(1).tap(1)

        // Act
        val hint = game.withHint().hint

        // Assert
        assertEquals(Hint.Mistake(listOf(1)), hint)
    }

    @Test
    fun `given no mistakes when a hint is asked for then the next deduction is given with the number's lines of sight`() {
        // Act
        val hint = GameState(puzzle).withHint().hint

        // Assert
        assertIs<Hint.Next>(hint)
        assertEquals(0, hint.deduction.source)
        assertEquals(true, 0 in hint.cells && 12 in hint.cells)
    }

    @Test
    fun `given a number boxed in by red dots when broken dots are found then it is listed`() {
        // Arrange
        val game =
            GameState(puzzle)
                .tap(1)
                .tap(1)
                .tap(4)
                .tap(4)

        // Act
        val broken = game.brokenCells

        // Assert
        assertEquals(setOf(0), broken)
    }

    @Test
    fun `given the last dot when it is set right then the puzzle is solved`() {
        // Arrange
        val game = GameState(puzzle, solution.with(1, Dot.EMPTY))

        // Act
        val after = game.tap(1)

        // Assert
        assertEquals(true, after.isSolved)
    }

    @ParameterizedTest
    @EnumSource(Dot::class, names = ["RED", "BLUE"])
    fun `given a colour picked when an empty spot is painted twice then it takes the colour and then clears`(colour: Dot) {
        // Arrange
        val game = GameState(puzzle)

        // Act
        val painted = game.paint(1, colour)
        val cleared = painted.paint(1, colour)

        // Assert
        assertEquals(colour to Dot.EMPTY, painted.grid.dots[1] to cleared.grid.dots[1])
    }

    @Test
    fun `given a spot of one colour when it is painted the other colour then it switches in one undoable change`() {
        // Arrange
        val game = GameState(puzzle).paint(1, Dot.BLUE)

        // Act
        val after = game.paint(1, Dot.RED)

        // Assert
        assertEquals(Dot.RED, after.grid.dots[1])
        assertEquals(Dot.BLUE, after.undo().grid.dots[1])
    }

    @Test
    fun `given a starting spot when it is painted then it does not change`() {
        // Act
        val after = GameState(puzzle).paint(0, Dot.RED)

        // Assert
        assertEquals(puzzle.givens, after.grid)
    }
}
