package com.joclegg.puzzles.wordwheel.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class GameStateTest {
    private val words = WordList(valid = listOf("parade", "area", "pare", "drape", "rap", "dear"), common = listOf("parade", "area"))
    private val puzzle = Puzzle("APARED", Crossword(listOf(Placement("PARADE", 0, 0, true), Placement("AREA", 0, 1, false))))

    @ParameterizedTest
    @CsvSource("area,FOUND", "pare,BONUS", "drape,BONUS", "zebra,NOT_A_WORD", "pa,TOO_SHORT", "dread,NOT_A_WORD")
    fun `given a guess when it is submitted then it is found, a bonus or refused`(
        word: String,
        expected: Guess,
    ) {
        // Act
        val after = GameState(puzzle).submit(word, words)

        // Assert
        assertEquals(expected, after.last)
    }

    @Test
    fun `given a word already found when it is guessed again then it is marked as already found`() {
        // Act
        val after = GameState(puzzle).submit("area", words).submit("AREA", words)

        // Assert
        assertEquals(Guess.ALREADY to listOf("AREA"), after.last to after.found)
    }

    @Test
    fun `given every crossword word found when the game is checked then it is solved`() {
        // Act
        val after = GameState(puzzle).submit("parade", words).submit("area", words)

        // Assert
        assertEquals(true, after.isSolved)
    }

    @Test
    fun `given a hint when a letter is revealed then the first hidden cell shows, and a word completed by hints counts as found`() {
        // Arrange
        val game = GameState(puzzle).submit("parade", words)

        // Act
        val after = (1..3).fold(game) { state, _ -> state.reveal { it.firstOrNull() } }

        // Assert
        assertEquals(true to listOf("PARADE", "AREA"), after.isSolved to after.found)
    }

    @Test
    fun `given a word list when American only spellings are checked then they are left out of the common words`() {
        // Act
        val list = WordList(valid = listOf("color", "colour", "center", "centre", "tree"), common = listOf("color", "center", "tree"))

        // Assert
        assertEquals(setOf("TREE"), list.common)
    }
}
