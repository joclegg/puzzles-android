package com.joclegg.puzzles.kakuro.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class GameStateTest {
    @Test
    fun `given a selected cell when a digit is tapped then it is placed, and tapping it again clears it`() {
        // Arrange
        val game = GameState(small).tapCell(4)

        // Act
        val placed = game.tapDigit(1)
        val cleared = placed.tapDigit(1)

        // Assert
        assertEquals(1 to 0, placed.values[4] to cleared.values[4])
    }

    @Test
    fun `given notes mode when digits are tapped then they toggle as notes`() {
        // Act
        val game =
            GameState(small)
                .tapCell(4)
                .toggleNotes()
                .tapDigit(1)
                .tapDigit(2)
                .tapDigit(1)

        // Assert
        assertEquals(0 to listOf(2), game.values[4] to Combinations.digitsOf(game.notes[4]))
    }

    @Test
    fun `given a starting digit when a digit is tapped on it then it does not change`() {
        // Act
        val game = GameState(small.copy(givens = setOf(4))).tapCell(4).tapDigit(9)

        // Assert
        assertEquals(1, game.values[4])
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 3])
    fun `given entries when they are all undone then the grid is back to the start`(entries: Int) {
        // Arrange
        val played = (1..entries).fold(GameState(small).tapCell(5)) { state, digit -> state.tapDigit(digit) }

        // Act
        val undone = (1..entries).fold(played) { state, _ -> state.undo() }

        // Assert
        assertEquals(List(9) { 0 }, undone.values)
    }

    @Test
    fun `given a run with a repeated digit when broken runs are found then it is listed`() {
        // Act
        val game =
            GameState(small)
                .tapCell(4)
                .tapDigit(2)
                .tapCell(5)
                .tapDigit(2)

        // Assert
        assertEquals(listOf(Run(listOf(4, 5), 3, true, 3)), game.brokenRuns.filter { it.isAcross })
    }

    @Test
    fun `given every cell filled correctly when it is checked then the puzzle is solved`() {
        // Act
        val game =
            listOf(
                4 to 1,
                5 to 2,
                7 to 3,
                8 to 4,
            ).fold(GameState(small)) { state, (cell, digit) -> state.tapCell(cell).tapDigit(digit) }

        // Assert
        assertEquals(true, game.isSolved)
    }

    @Test
    fun `given changes saved and replayed when the game is rebuilt then values and notes match`() {
        // Arrange
        val played =
            GameState(small)
                .tapCell(4)
                .tapDigit(1)
                .toggleNotes()
                .tapCell(5)
                .tapDigit(2)

        // Act
        val replayed = GameState.replay(small, ChangeCodec.decode(ChangeCodec.encode(played.changes)))

        // Assert
        assertEquals(played.values to played.notes, replayed.values to replayed.notes)
    }

    @Test
    fun `given a selected cell when its combinations are listed then each run shows the digit sets that still fit`() {
        // Act
        val combos =
            GameState(small)
                .tapCell(5)
                .tapDigit(2)
                .tapCell(4)
                .selectedCombinations()

        // Assert
        assertEquals(listOf(listOf(listOf(1, 2)), listOf(listOf(1, 3))), combos.map { (_, masks) -> masks.map(Combinations::digitsOf) })
    }
}
