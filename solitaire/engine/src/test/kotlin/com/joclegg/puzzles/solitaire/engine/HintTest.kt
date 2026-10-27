package com.joclegg.puzzles.solitaire.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HintTest {
    @Test
    fun `given an ace on a column when a hint is asked for then it sends the ace up`() {
        // Arrange
        val game = GameState(board("/AH|/|/|/|/|/|/;;;|||"), DrawMode.DRAW_ONE)

        // Act
        val hint = game.findHint()

        // Assert
        val transfer = assertIs<Hint.Transfer>(hint)
        assertEquals(Selection(Pile.Tableau(0), 1) to true, transfer.selection to (transfer.to is Pile.Foundation))
    }

    @Test
    fun `given no card can move when a hint is asked for then it says to draw`() {
        // Arrange
        val game = GameState(board("/|/|/|/|/|/|/;2C;;|||"), DrawMode.DRAW_ONE)

        // Act
        val hint = game.findHint()

        // Assert
        assertEquals(Hint.Draw, hint)
    }
}
