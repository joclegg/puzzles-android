package com.joclegg.puzzles.solitaire.feature.game

import com.joclegg.puzzles.solitaire.engine.BoardCodec
import com.joclegg.puzzles.solitaire.engine.Hint
import com.joclegg.puzzles.solitaire.engine.Pile
import com.joclegg.puzzles.solitaire.engine.Selection
import org.junit.Assert.assertEquals
import org.junit.Test

class HintTextTest {
    @Test
    fun `given a move that turns a card over when the hint is worded then it names the target card and why`() {
        // Arrange
        val board = BoardCodec.decode("AS/7H|/8C;;;|||")

        // Act
        val text = hintText(board, Hint.Transfer(Selection(Pile.Tableau(0), 1), Pile.Tableau(1)))

        // Assert
        assertEquals("Move the 7♥ onto the 8♣ to turn over the card under it.", text)
    }

    @Test
    fun `given a waste card for an empty column when the hint is worded then it names the column and why`() {
        // Arrange
        val board = BoardCodec.decode("/|/;;KS;|||")

        // Act
        val text = hintText(board, Hint.Transfer(Selection(Pile.Waste, 1), Pile.Tableau(1)))

        // Assert
        assertEquals("Move the K♠ from the waste to the empty column 2, so the card under it can be played.", text)
    }
}
