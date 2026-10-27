package com.joclegg.puzzles.sudoku.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class HintTest {
    private val game = GameState(easyPuzzle)

    @Test
    fun `given a fresh puzzle when a hint is asked for then it names a cell and its answer`() {
        // Act
        val hint = game.findHint()!!

        // Assert
        assertEquals(easyPuzzle.solution!![hint.cell], hint.digit)
    }

    @Test
    fun `given a wrong digit when a hint is asked for then it points at that cell first`() {
        // Arrange
        val cell = easyPuzzle.givens.indexOfFirst { it == 0 }
        val wrongDigit = easyPuzzle.solution!![cell] % 9 + 1
        val wrong = game.copy(values = game.values.toMutableList().apply { this[cell] = wrongDigit })

        // Act
        val hint = wrong.findHint()

        // Assert
        assertEquals(Hint(cell, easyPuzzle.solution!![cell], "This cell is wrong."), hint)
    }

    @Test
    fun `given a hint when it is placed then the cell holds its answer`() {
        // Arrange
        val hint = game.findHint()!!

        // Act
        val placed = game.placeHint(hint)

        // Assert
        assertEquals(hint.digit, placed.values[hint.cell])
    }
}
