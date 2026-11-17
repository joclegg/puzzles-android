package com.joclegg.puzzles.crossword.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class BoardScrollTest {
    @Test
    fun `given a board that fits the height when sized then it takes the smaller of width and height`() {
        // Act
        val side = boardSide(maxWidth = 600.dp, maxHeight = 500.dp, size = 15)

        // Assert
        assertEquals(500.dp, side)
    }

    @Test
    fun `given too little height for a large board when sized then it keeps the full width`() {
        // Act
        val side = boardSide(maxWidth = 360.dp, maxHeight = 220.dp, size = 15)

        // Assert
        assertEquals(360.dp, side)
    }

    @Test
    fun `given too little height for a small board when sized then its cells stay comfortable`() {
        // Act
        val side = boardSide(maxWidth = 360.dp, maxHeight = 100.dp, size = 5)

        // Assert
        assertEquals(160.dp, side)
    }

    @Test
    fun `given the word is in view when scrolling then the board stays where it is`() {
        // Act
        val scroll = boardScroll(current = 100, viewport = 300, side = 600, size = 15, selectedRow = 4, wordRows = 4..4)

        // Assert
        assertEquals(100, scroll)
    }

    @Test
    fun `given the word is below the view when scrolling then its last row comes into view with a row to spare`() {
        // Act
        val scroll = boardScroll(current = 0, viewport = 300, side = 600, size = 15, selectedRow = 9, wordRows = 9..12)

        // Assert
        assertEquals(260, scroll)
    }

    @Test
    fun `given the word is above the view when scrolling then its first row comes into view with a row to spare`() {
        // Act
        val scroll = boardScroll(current = 300, viewport = 300, side = 600, size = 15, selectedRow = 2, wordRows = 2..2)

        // Assert
        assertEquals(40, scroll)
    }

    @Test
    fun `given a word taller than the view when scrolling then the selected row is centred`() {
        // Act
        val scroll = boardScroll(current = 0, viewport = 200, side = 600, size = 15, selectedRow = 7, wordRows = 0..14)

        // Assert
        assertEquals(200, scroll)
    }

    @Test
    fun `given a word taller than the view when its last row is selected then the scroll stops at the end`() {
        // Act
        val scroll = boardScroll(current = 0, viewport = 200, side = 600, size = 15, selectedRow = 14, wordRows = 0..14)

        // Assert
        assertEquals(400, scroll)
    }
}
