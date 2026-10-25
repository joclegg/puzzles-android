package com.joclegg.puzzles.solitaire.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BoardTest {
    @Test
    fun `given a seed when a deal is made then columns hold one to seven cards with only the top face up`() {
        // Act
        val board = Board.deal(seed = 42)

        // Assert
        assertEquals(
            Triple((1..7).toList(), (0..6).toList(), 24),
            Triple(board.tableau.map { it.cards.size }, board.tableau.map { it.faceUpFrom }, board.stock.size),
        )
    }

    @Test
    fun `given the same seed when two deals are made then they match`() {
        // Act
        val deals = listOf(Board.deal(seed = 7), Board.deal(seed = 7), Board.deal(seed = 8))

        // Assert
        assertEquals(deals[0], deals[1])
        assertNotEquals(deals[0], deals[2])
    }

    @ParameterizedTest
    @MethodSource("draws")
    fun `given a stock when drawing then cards move to the waste or the waste turns over`(
        before: String,
        drawMode: DrawMode,
        after: String,
    ) {
        // Act
        val drawn = board(before).draw(drawMode)

        // Assert
        assertEquals(board(after), drawn)
    }

    @ParameterizedTest
    @MethodSource("moves")
    fun `given a board when a move is checked then only legal moves are allowed`(
        text: String,
        from: Pile,
        count: Int,
        to: Pile,
        legal: Boolean,
    ) {
        // Act
        val result = board(text).canMove(from, count, to)

        // Assert
        assertEquals(legal, result)
    }

    @ParameterizedTest
    @MethodSource("upMoves")
    fun `given cards build up when a move is checked then only legal moves are allowed`(
        text: String,
        from: Pile,
        count: Int,
        to: Pile,
        legal: Boolean,
    ) {
        // Act
        val result = board(text).canMove(from, count, to, BuildOrder.UP)

        // Assert
        assertEquals(legal, result)
    }

    @Test
    fun `given a column with a face down card when its top card moves away then the next card turns face up`() {
        // Arrange
        val before = board("KS/QH|/;;;|||")

        // Act
        val after = before.move(Pile.Tableau(0), 1, Pile.Tableau(1))

        // Assert
        assertEquals(board("/KS|/QH;;;|||"), after)
    }

    @Test
    fun `given four full foundations when the board is checked then it is won`() {
        // Act
        val won = board("/;;;$fullFoundations").isWon

        // Assert
        assertEquals(true, won)
    }

    companion object {
        @JvmStatic
        fun draws() =
            listOf(
                Arguments.of("/;2C3C4C;;|||", DrawMode.DRAW_ONE, "/;2C3C;4C;|||"),
                Arguments.of("/;2C3C4C5C;AC;|||", DrawMode.DRAW_THREE, "/;2C;AC5C4C3C;|||"),
                Arguments.of("/;2C;;|||", DrawMode.DRAW_THREE, "/;;2C;|||"),
                Arguments.of("/;;2C3C4C;|||", DrawMode.DRAW_ONE, "/;4C3C2C;;|||"),
            )

        @JvmStatic
        fun upMoves() =
            listOf(
                Arguments.of("/2H|/AS;;;|||", Pile.Tableau(0), 1, Pile.Tableau(1), true),
                Arguments.of("/QH|/KS;;;|||", Pile.Tableau(0), 1, Pile.Tableau(1), false),
                Arguments.of("/AHQS|/;;;|||", Pile.Tableau(0), 1, Pile.Tableau(1), false),
                Arguments.of("/AH2S|/;;;|||", Pile.Tableau(0), 2, Pile.Tableau(1), true),
                Arguments.of("/KH|/;;;|||", Pile.Tableau(0), 1, Pile.Tableau(1), false),
                Arguments.of("/KS;;;|||", Pile.Tableau(0), 1, Pile.Foundation(3), true),
                Arguments.of("/AS;;;|||", Pile.Tableau(0), 1, Pile.Foundation(3), false),
                Arguments.of("/QS;;;|||KS", Pile.Tableau(0), 1, Pile.Foundation(3), true),
                Arguments.of("/JS;;;|||KS", Pile.Tableau(0), 1, Pile.Foundation(3), false),
            )

        @JvmStatic
        fun moves() =
            listOf(
                Arguments.of("/QH|/KS;;;|||", Pile.Tableau(0), 1, Pile.Tableau(1), true),
                Arguments.of("/QS|/KS;;;|||", Pile.Tableau(0), 1, Pile.Tableau(1), false),
                Arguments.of("/QH|/;;;|||", Pile.Tableau(0), 1, Pile.Tableau(1), false),
                Arguments.of("/KH|/;;;|||", Pile.Tableau(0), 1, Pile.Tableau(1), true),
                Arguments.of("/KHQS|/;;;|||", Pile.Tableau(0), 2, Pile.Tableau(1), true),
                Arguments.of("KH/QS|/;;;|||", Pile.Tableau(0), 2, Pile.Tableau(1), false),
                Arguments.of("/KHQH|/;;;|||", Pile.Tableau(0), 2, Pile.Tableau(1), false),
                Arguments.of("/AS;;;|||", Pile.Tableau(0), 1, Pile.Foundation(3), true),
                Arguments.of("/AS;;;|||", Pile.Tableau(0), 1, Pile.Foundation(0), false),
                Arguments.of("/2S;;;|||AS", Pile.Tableau(0), 1, Pile.Foundation(3), true),
                Arguments.of("/3S;;;|||AS", Pile.Tableau(0), 1, Pile.Foundation(3), false),
                Arguments.of("/;;AH;|||", Pile.Waste, 1, Pile.Foundation(2), true),
                Arguments.of("/2H;;;||AH|", Pile.Foundation(2), 1, Pile.Tableau(0), false),
                Arguments.of("/2S;;;|AD||", Pile.Foundation(1), 1, Pile.Tableau(0), true),
                Arguments.of("/KS;;;|||", Pile.Tableau(0), 1, Pile.Waste, false),
            )
    }
}
