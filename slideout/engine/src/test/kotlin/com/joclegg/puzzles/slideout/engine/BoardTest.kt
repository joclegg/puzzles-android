package com.joclegg.puzzles.slideout.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BoardTest {
    private val board = Board.decode("202H,033V,412H")

    @ParameterizedTest
    @CsvSource("0,0,1", "1,0,3", "2,0,4")
    fun `given blocks when a block's range is found then it stops at the edges and other blocks`(
        block: Int,
        low: Int,
        high: Int,
    ) {
        // Act
        val range = board.range(block)

        // Assert
        assertEquals(low..high, range)
    }

    @Test
    fun `given a board when it is encoded and decoded then it is unchanged`() {
        // Act
        val decoded = Board.decode(board.encode())

        // Assert
        assertEquals(board, decoded)
    }

    @Test
    fun `given the red block at the right edge when the board is checked then it is solved`() {
        // Act
        val solved = Board.decode("242H").isSolved

        // Assert
        assertEquals(true, solved)
    }

    @Test
    fun `given the same blocks in another order when identified then the identities match`() {
        // Arrange
        val reordered = Board.decode("202H,412H,033V")

        // Act
        val identities = board.identity() to reordered.identity()

        // Assert
        assertEquals(identities.first, identities.second)
    }

    @Test
    fun `given a different red block when identified then the identities differ`() {
        // Arrange
        val other = Board.decode("412H,202H,033V")

        // Act
        val identities = board.identity() to other.identity()

        // Assert
        assertNotEquals(identities.first, identities.second)
    }
}
