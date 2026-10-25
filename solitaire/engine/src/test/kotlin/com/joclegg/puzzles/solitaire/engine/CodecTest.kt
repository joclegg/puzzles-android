package com.joclegg.puzzles.solitaire.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class CodecTest {
    @ParameterizedTest
    @ValueSource(longs = [1, 99, 123_456])
    fun `given a deal when it is encoded and decoded then it is unchanged`(seed: Long) {
        // Arrange
        val deal = Board.deal(seed).draw(DrawMode.DRAW_THREE)

        // Act
        val decoded = BoardCodec.decode(BoardCodec.encode(deal))

        // Assert
        assertEquals(deal, decoded)
    }

    @Test
    fun `given every kind of move when they are encoded and decoded then they are unchanged`() {
        // Arrange
        val moves =
            listOf(
                Move.Draw,
                Move.Transfer(Pile.Waste, 1, Pile.Foundation(2)),
                Move.Transfer(Pile.Tableau(6), 12, Pile.Tableau(0)),
                Move.Transfer(Pile.Foundation(3), 1, Pile.Tableau(4)),
            )

        // Act
        val decoded = MoveCodec.decode(MoveCodec.encode(moves))

        // Assert
        assertEquals(moves, decoded)
    }
}
