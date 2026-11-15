package com.joclegg.puzzles.crossword.engine

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HintTest {
    @Test
    fun `given a wrong letter and empty cells when a hint is found then it points at the wrong letter`() {
        // Arrange
        val game = GameState(small).typed("CX")

        // Act
        val hint = game.findHint()

        // Assert
        assertEquals(Hint(1, 'A', "This letter is wrong."), hint)
    }

    @Test
    fun `given two wrong letters when a hint is found then it points at the first in reading order`() {
        // Arrange
        val game = GameState(small, entries = "CAX.#..Y.", selected = 6, isAcross = true)

        // Act
        val hint = game.findHint()

        // Assert
        assertEquals(Hint(2, 'T', "This letter is wrong."), hint)
    }

    @Test
    fun `given an empty cell in the current word when a hint is found then it suggests that cell's letter`() {
        // Arrange
        val game = GameState(small).typed("C")

        // Act
        val hint = game.findHint()

        // Assert
        assertEquals(Hint(1, 'A', "Try A here."), hint)
    }

    @Test
    fun `given the current word is full when a hint is found then it suggests the next clue's first empty cell`() {
        // Arrange
        val game = GameState(small, entries = "CAT.#....", selected = 0, isAcross = true)

        // Act
        val hint = game.findHint()

        // Assert
        assertEquals(Hint(6, 'W', "Try W here."), hint)
    }

    @Test
    fun `given the last clue is full when a hint is found then it wraps round to the first clue`() {
        // Arrange
        val game = GameState(small, entries = "..T.#A..T", selected = 2, isAcross = false)

        // Act
        val hint = game.findHint()

        // Assert
        assertEquals(Hint(0, 'C', "Try C here."), hint)
    }

    @Test
    fun `given a full correct grid when a hint is found then there is none`() {
        // Arrange
        val game = GameState(small, entries = small.solution)

        // Act
        val hint = game.findHint()

        // Assert
        assertNull(hint)
    }

    @Test
    fun `given a hint when it points at its cell then the cell is selected and the hints count goes up`() {
        // Arrange
        val game = GameState(small, entries = "CAT.#....", selected = 0, isAcross = true)

        // Act
        val pointed = game.pointAt(Hint(3, 'O', "Try O here."))

        // Assert
        assertEquals(Triple(3, false, 1), Triple(pointed.selected, pointed.isAcross, pointed.reveals))
        assertEquals(game.entries, pointed.entries)
    }

    @Test
    fun `given a wrong letter when a hint fills it then the right letter goes in and the hints count goes up`() {
        // Arrange
        val game = GameState(small).typed("CX")

        // Act
        val filled = game.placeHint(Hint(1, 'A', "This letter is wrong."))

        // Assert
        assertEquals("CA..#....", filled.entries)
        assertEquals(1 to 1, filled.selected to filled.reveals)
    }

    @Test
    fun `given a filled hint when undo is pressed then the previous letter comes back`() {
        // Arrange
        val filled = GameState(small).typed("CX").placeHint(Hint(1, 'A', "This letter is wrong."))

        // Act
        val undone = filled.undo()

        // Assert
        assertEquals("CX..#....", undone.entries)
    }
}
