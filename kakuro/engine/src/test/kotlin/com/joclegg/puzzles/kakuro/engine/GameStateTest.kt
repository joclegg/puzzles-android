package com.joclegg.puzzles.kakuro.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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

    @Test
    fun `given number first with a digit picked when cells are tapped then the digit goes in, and tapping again clears it`() {
        // Arrange
        val game = GameState(small, inputMode = InputMode.DIGIT_FIRST).tapDigit(1)

        // Act
        val placed = game.tapCell(4)
        val cleared = placed.tapCell(4)

        // Assert
        assertEquals(1 to 0, placed.values[4] to cleared.values[4])
    }

    @Test
    fun `given number first with erase on when a filled cell is tapped then it is cleared`() {
        // Arrange
        val game = GameState(small, inputMode = InputMode.DIGIT_FIRST).tapDigit(2).tapCell(5).erase()

        // Act
        val after = game.tapCell(5)

        // Assert
        assertEquals(0, after.values[5])
    }

    @Test
    fun `given block clashes when a digit already in the run is entered then it is refused and counted`() {
        // Arrange
        val game = GameState(small, mistakeCheck = MistakeCheck.BLOCK_CLASHES).tapCell(5).tapDigit(2).tapCell(4)

        // Act
        val after = game.tapDigit(2)

        // Assert
        assertEquals(Triple(0, 4, 1), Triple(after.values[4], after.rejected, after.mistakes))
    }

    @ParameterizedTest
    @EnumSource(MistakeCheck::class, names = ["OFF", "SHOW_WRONG"])
    fun `given a mistake check level when a wrong digit is entered then it is shown and counted only when showing wrong`(
        level: MistakeCheck,
    ) {
        // Arrange
        val game = GameState(small, mistakeCheck = level).tapCell(4)

        // Act
        val after = game.tapDigit(9)

        // Assert
        val showing = level == MistakeCheck.SHOW_WRONG
        assertEquals(showing to showing, (4 in after.shownWrongCells) to (after.mistakes == 1))
    }

    @Test
    fun `given every one of a digit placed correctly when it is checked then that digit is done`() {
        // Act
        val game = GameState(small).tapCell(4).tapDigit(1)

        // Assert
        assertEquals(true to false, game.isDigitDone(1) to game.isDigitDone(2))
    }

    @Test
    fun `given pencil marks in the same runs when a digit is placed then those marks are removed, and undo puts them back`() {
        // Arrange
        val noted =
            GameState(small)
                .toggleNotes()
                .tapCell(5)
                .tapDigit(1)
                .tapCell(7)
                .tapDigit(1)
                .tapCell(8)
                .tapDigit(1)
                .toggleNotes()

        // Act
        val placed = noted.tapCell(4).tapDigit(1)
        val undone = placed.undo()

        // Assert
        assertEquals(listOf(0, 0, 1), listOf(5, 7, 8).map { placed.notes[it] })
        assertEquals(noted.notes, undone.notes)
    }

    @Test
    fun `given auto remove turned off when a digit is placed then other pencil marks stay`() {
        // Arrange
        val noted =
            GameState(small, autoRemoveNotes = false)
                .toggleNotes()
                .tapCell(5)
                .tapDigit(1)
                .toggleNotes()

        // Act
        val placed = noted.tapCell(4).tapDigit(1)

        // Assert
        assertEquals(1, placed.notes[5])
    }

    @Test
    fun `given changes that cleared pencil marks when they are saved and replayed then the marks match`() {
        // Arrange
        val played =
            GameState(small)
                .toggleNotes()
                .tapCell(5)
                .tapDigit(1)
                .toggleNotes()
                .tapCell(4)
                .tapDigit(1)

        // Act
        val replayed = GameState.replay(small, ChangeCodec.decode(ChangeCodec.encode(played.changes)))

        // Assert
        assertEquals(played.notes, replayed.notes)
    }

    @Test
    fun `given two undone taps when redo is used twice then both are back, pencil marks included`() {
        // Arrange
        val played =
            GameState(small)
                .toggleNotes()
                .tapCell(5)
                .tapDigit(1)
                .toggleNotes()
                .tapCell(4)
                .tapDigit(1)

        // Act
        val redone =
            played
                .undo()
                .undo()
                .redo()
                .redo()

        // Assert
        assertEquals(played.values to played.notes, redone.values to redone.notes)
        assertEquals(played.changes, redone.changes)
    }

    @Test
    fun `given an undone tap when a new digit is placed then there is nothing to redo`() {
        // Arrange
        val undone = GameState(small).tapCell(4).tapDigit(1).undo()

        // Act
        val next = undone.tapDigit(2)

        // Assert
        assertEquals(next, next.redo())
    }

    @Test
    fun `given a selected cell when mark is used twice then it is marked and then unmarked`() {
        // Arrange
        val selected = GameState(small).tapCell(4)

        // Act
        val marked = selected.mark()

        // Assert
        assertEquals(setOf(4), marked.marked)
        assertEquals(emptySet(), marked.mark().marked)
    }

    @Test
    fun `given number first with mark on when cells are tapped then they are marked and nothing is entered`() {
        // Arrange
        val marking = GameState(small, inputMode = InputMode.DIGIT_FIRST).mark()

        // Act
        val next = marking.tapCell(4).tapCell(5)

        // Assert
        assertEquals(setOf(4, 5), next.marked)
        assertEquals(marking.values, next.values)
    }
}
