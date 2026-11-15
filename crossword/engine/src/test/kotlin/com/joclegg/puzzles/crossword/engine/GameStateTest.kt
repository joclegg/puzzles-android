package com.joclegg.puzzles.crossword.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameStateTest {
    @Test
    fun `given a new game when it starts then the first across clue's first cell is selected`() {
        // Act
        val game = GameState(small)

        // Assert
        assertEquals(0 to true, game.selected to game.isAcross)
    }

    @Test
    fun `given the start of a word when letters are typed then each goes in and the cursor moves along`() {
        // Act
        val game = GameState(small).typed("ca")

        // Assert
        assertEquals("CA..#....", game.entries)
        assertEquals(2, game.selected)
    }

    @Test
    fun `given a word when its last letter is typed then the cursor moves to the next clue's first empty cell`() {
        // Act
        val game = GameState(small).typed("CAT")

        // Assert
        assertEquals(6 to true, game.selected to game.isAcross)
    }

    @Test
    fun `given CAT just typed when backspace is pressed three times then T, A and C clear in turn leaving the C cell selected`() {
        // Arrange
        val typed = GameState(small).typed("CAT")

        // Act
        val steps = (1..3).runningFold(typed) { state, _ -> state.backspace() }.drop(1)

        // Assert
        assertEquals(
            listOf("CA..#...." to 2, "C...#...." to 1, "....#...." to 0),
            steps.map { it.entries to it.selected },
        )
    }

    @Test
    fun `given a cell with a letter when backspace is pressed then the letter clears and the cursor stays`() {
        // Arrange
        val game = GameState(small).typed("CA").tapCell(1)

        // Act
        val cleared = game.backspace()

        // Assert
        assertEquals("C...#...." to 1, cleared.entries to cleared.selected)
    }

    @Test
    fun `given an empty cell inside a word when backspace is pressed then the previous cell is cleared and selected`() {
        // Arrange
        val game = GameState(small).typed("CA")

        // Act
        val cleared = game.backspace()

        // Assert
        assertEquals("C...#...." to 1, cleared.entries to cleared.selected)
    }

    @Test
    fun `given the empty first cell of a word when backspace is pressed then the previous clue's last cell is cleared and selected`() {
        // Arrange
        val game = GameState(small, entries = "CAT.#....").tapCell(6)

        // Act
        val cleared = game.backspace()

        // Assert
        assertEquals(Triple("CA..#....", 2, true), Triple(cleared.entries, cleared.selected, cleared.isAcross))
    }

    @ParameterizedTest
    @CsvSource("true, 2", "false, 1")
    fun `given the next cell already filled when a letter is typed then skip filled cells decides where the cursor goes`(
        skip: Boolean,
        expected: Int,
    ) {
        // Arrange
        val game = GameState(small, skipFilledCells = skip).tapCell(1).type('A').tapCell(0)

        // Act
        val typed = game.type('C')

        // Assert
        assertEquals(expected, typed.selected)
    }

    @Test
    fun `given skip filled cells and only earlier gaps when a letter is typed then the cursor stays on the word's last cell`() {
        // Arrange
        val game = GameState(small).tapCell(2).type('T').tapCell(1)

        // Act
        val typed = game.type('A')

        // Assert
        assertEquals(".AT.#....", typed.entries)
        assertEquals(2, typed.selected)
    }

    @Test
    fun `given a full puzzle when the last letter is typed then the cursor stays put`() {
        // Arrange
        val game = GameState(small, entries = "CATO#AWE.").tapCell(8)

        // Act
        val typed = game.type('X')

        // Assert
        assertEquals(8, typed.selected)
    }

    @Test
    fun `given digits and punctuation when typed then nothing changes`() {
        // Arrange
        val game = GameState(small)

        // Act
        val typed = game.typed("1-? ")

        // Assert
        assertEquals(game, typed)
    }

    @Test
    fun `given a cell in two words when it is tapped twice then the direction toggles`() {
        // Act
        val game = GameState(small).tapCell(6).tapCell(6)

        // Assert
        assertEquals(6 to false, game.selected to game.isAcross)
    }

    @Test
    fun `given a cell in only an across word when it is tapped again or space is pressed then the direction stays across`() {
        // Act
        val game = GameState(small).tapCell(1).tapCell(1).toggleDirection()

        // Assert
        assertEquals(1 to true, game.selected to game.isAcross)
    }

    @Test
    fun `given an across selection when a cell only in a down word is tapped then the direction becomes down`() {
        // Act
        val game = GameState(small).tapCell(3)

        // Assert
        assertEquals(3 to false, game.selected to game.isAcross)
    }

    @ParameterizedTest
    @CsvSource(
        "0, true, RIGHT, 1, true",
        "0, true, DOWN, 0, false",
        "0, false, DOWN, 3, false",
        "1, true, DOWN, 7, true",
        "2, true, RIGHT, 2, true",
        "8, true, LEFT, 7, true",
        "3, false, UP, 0, false",
    )
    fun `given a selection when an arrow is pressed then it switches direction first or moves to the next white cell`(
        cell: Int,
        across: Boolean,
        arrow: Arrow,
        expectedCell: Int,
        expectedAcross: Boolean,
    ) {
        // Arrange
        val game = GameState(small).copy(selected = cell, isAcross = across)

        // Act
        val moved = game.arrow(arrow)

        // Assert
        assertEquals(expectedCell to expectedAcross, moved.selected to moved.isAcross)
    }

    @Test
    fun `given the first clue when previous and next clue are pressed then they wrap through across and down`() {
        // Arrange
        val game = GameState(small)

        // Act
        val previous = game.previousClue()
        val next = previous.nextClue()

        // Assert
        assertEquals(listOf(2 to false, 0 to true), listOf(previous.selected to previous.isAcross, next.selected to next.isAcross))
    }

    @Test
    fun `given a partly filled clue when it is reached by next clue then its first empty cell is selected`() {
        // Arrange
        val game = GameState(small, entries = "CAT.#.W..")

        // Act
        val next = game.nextClue()

        // Assert
        assertEquals(7, next.selected)
    }

    @Test
    fun `given letters typed when undo and redo are pressed then the grid steps back and forward`() {
        // Arrange
        val game = GameState(small).typed("CA")

        // Act
        val undone = game.undo()
        val redone = undone.redo()

        // Assert
        assertEquals(listOf("C...#....", "CA..#...."), listOf(undone.entries, redone.entries))
    }

    @Test
    fun `given moves when they are encoded, decoded and replayed then the grid is restored`() {
        // Arrange
        val game = GameState(small).typed("CAT").revealWord()

        // Act
        val replayed = GameState.replay(small, ChangeCodec.decode(ChangeCodec.encode(game.changes)))

        // Assert
        assertEquals(game.entries, replayed.entries)
    }

    @Test
    fun `given a saved grid with no moves when it is restored then the entries are kept`() {
        // Act
        val game = GameState.restore(small, "CA..#....", emptyList())

        // Assert
        assertEquals("CA..#....", game.entries)
    }

    @Test
    fun `given a word selected when it is revealed then it is filled in, counted as one reveal and undone a cell at a time`() {
        // Arrange
        val game = GameState(small).typed("CX")

        // Act
        val revealed = game.tapCell(0).revealWord()

        // Assert
        assertEquals(Triple("CAT.#....", 1, "CA..#...."), Triple(revealed.entries, revealed.reveals, revealed.undo().entries))
    }

    @Test
    fun `given a selected cell when its letter is revealed then only that cell is filled`() {
        // Act
        val game = GameState(small).tapCell(7).revealLetter()

        // Assert
        assertEquals("....#..E.", game.entries)
    }

    @Test
    fun `given wrong letters when the grid is checked then they are wrong and the puzzle is not solved`() {
        // Act
        val game = GameState(small, entries = "CAXO#AWET")

        // Assert
        assertEquals(setOf(2) to false, game.wrongCells to game.isSolved)
    }

    @Test
    fun `given every cell right when the grid is checked then the puzzle is solved`() {
        // Act
        val game = GameState(small, entries = "CATO#AWE.").tapCell(8).type('T')

        // Assert
        assertTrue(game.isSolved)
    }

    @Test
    fun `given a solved puzzle when the solution is revealed or restarted then reveal fills the grid and restart empties it`() {
        // Act
        val revealed = GameState(small).revealSolution()
        val restarted = revealed.restart()

        // Assert
        assertEquals(listOf(small.solution, "....#...."), listOf(revealed.entries, restarted.entries))
    }

    @Test
    fun `given moves when they are encoded then each is the cell, old mark and new mark`() {
        // Act
        val text = ChangeCodec.encode(listOf(Change(0, '.', 'C'), Change(2, 'T', '.')))

        // Assert
        assertEquals("0.C,2T.", text)
    }

    @Test
    fun `given a grid whose first clue is full when the game opens then the first clue with an empty cell is selected`() {
        // Act
        val game = GameState.restore(small, "CAT.#....", emptyList())

        // Assert
        assertEquals(6 to true, game.selected to game.isAcross)
    }

    @ParameterizedTest
    @CsvSource("true, 1", "false, 0")
    fun `given a wrong letter when it is typed then it counts as a mistake only while mistakes are shown`(
        count: Boolean,
        expected: Int,
    ) {
        // Act
        val game = GameState(small, countMistakes = count).type('X')

        // Assert
        assertEquals(expected, game.mistakes)
    }
}
