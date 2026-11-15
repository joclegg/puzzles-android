package com.joclegg.puzzles.crossword.engine

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BoardTest {
    @Test
    fun `given a grid when it is numbered then each word start gets the next number in reading order`() {
        // Act
        val numbers = small.numbers

        // Assert
        assertEquals(mapOf(0 to 1, 2 to 2, 6 to 3), numbers)
    }

    @Test
    fun `given a grid when its words are found then across come first, then down, each in number order`() {
        // Act
        val words = small.words

        // Assert
        assertEquals(
            listOf(
                Word(1, true, listOf(0, 1, 2)),
                Word(3, true, listOf(6, 7, 8)),
                Word(1, false, listOf(0, 3, 6)),
                Word(2, false, listOf(2, 5, 8)),
            ),
            words,
        )
    }

    @Test
    fun `given a grid whose rows end in white cells when words are found then no word wraps onto the next row`() {
        // Arrange
        val grid = puzzle(4, "AB#C DEFG #HIJ KL#M")

        // Act
        val across = grid.across.map { it.cells }

        // Assert
        assertEquals(listOf(listOf(0, 1), listOf(4, 5, 6, 7), listOf(9, 10, 11), listOf(12, 13)), across)
    }

    @Test
    fun `given a puzzle when it is encoded and decoded then it is unchanged`() {
        // Act
        val decoded = Puzzle.decode(small.encode())

        // Assert
        assertEquals(small, decoded)
    }

    @Test
    fun `given a clue list when an answer is looked up then its clue and enumeration are found`() {
        // Arrange
        val clues = Clues.parse("CAT;Pet that purrs\nCOW;Farm animal\n")

        // Act
        val shown = "${clues.clueFor("COW")} ${enumeration(small.down.first())}"

        // Assert
        assertEquals("Farm animal (3)", shown)
    }
}
