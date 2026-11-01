package com.joclegg.puzzles.twotone.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GameStateTest {
    private val solution = Grid.parse(4, "RBBR BRRB RBRB BRBR")
    private val puzzle = Puzzle(Grid.parse(4, "R... .... .... ...."), solution)

    @Test
    fun `given an empty tile when it is tapped three times then it goes red, blue and empty again`() {
        // Arrange
        val game = GameState(puzzle)

        // Act
        val tiles = (1..3).runningFold(game) { state, _ -> state.tap(1) }.map { it.grid.tiles[1] }

        // Assert
        assertEquals(listOf(Tile.EMPTY, Tile.RED, Tile.BLUE, Tile.EMPTY), tiles)
    }

    @Test
    fun `given a starting tile when it is tapped then it does not change`() {
        // Act
        val after = GameState(puzzle).tap(0)

        // Assert
        assertEquals(Tile.RED, after.grid.tiles[0])
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 4, 9])
    fun `given several taps when they are all undone then the grid is back to the start`(taps: Int) {
        // Arrange
        val played = (1..taps).fold(GameState(puzzle)) { state, tap -> state.tap(tap % 15 + 1) }

        // Act
        val undone = (1..taps).fold(played) { state, _ -> state.undo() }

        // Assert
        assertEquals(puzzle.givens, undone.grid)
    }

    @Test
    fun `given taps saved as changes when the game is replayed then the grid matches`() {
        // Arrange
        val played = GameState(puzzle).tap(1).tap(1).tap(5)

        // Act
        val replayed = GameState.replay(puzzle, ChangeCodec.decode(ChangeCodec.encode(played.changes)))

        // Assert
        assertEquals(played.grid, replayed.grid)
    }

    @Test
    fun `given a wrong tile when a hint is asked for then the wrong tile is pointed out`() {
        // Arrange
        val game = GameState(puzzle).tap(1)

        // Act
        val hint = game.withHint().hint

        // Assert
        assertEquals(Hint.Mistake(listOf(1)), hint)
    }

    @Test
    fun `given no mistakes when a hint is asked for then the next deduction is given`() {
        // Arrange
        val game = GameState(Puzzle(Grid.parse(4, "RR.. .... .... ...."), Grid.parse(4, "RRBB BBRR RBRB BRBR")))

        // Act
        val hint = game.withHint().hint

        // Assert
        assertIs<Hint.Next>(hint)
        assertEquals(listOf(2), hint.deduction.cells)
    }

    @Test
    fun `given the last tile when it is filled correctly then the puzzle is solved`() {
        // Arrange
        val game = GameState(puzzle, solution.with(1, Tile.EMPTY))

        // Act
        val after = game.tap(1).tap(1)

        // Assert
        assertEquals(true to 100, after.isSolved to after.progressPercent)
    }

    @Test
    fun `given a starting spot when it is tapped then it does not change`() {
        // Act
        val after = GameState(puzzle).tap(0)

        // Assert
        assertEquals(puzzle.givens, after.grid)
    }
}
