package com.joclegg.puzzles.crossword.ui

import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SentinelInputsTest(
    private val text: String,
    private val expected: List<BoardInput>,
) {
    @org.junit.Test
    fun `given the field's new text when it is read then it becomes letter, backspace or toggle inputs`() {
        // Act
        val inputs = sentinelInputs(text)

        // Assert
        assertEquals(expected, inputs)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun cases() =
            listOf(
                arrayOf("", listOf(BoardInput.Backspace)),
                arrayOf(" a", listOf(BoardInput.Letter('A'))),
                arrayOf(" Ab", listOf(BoardInput.Letter('A'), BoardInput.Letter('B'))),
                arrayOf("  ", listOf(BoardInput.ToggleDirection)),
                arrayOf(" 4.", emptyList<BoardInput>()),
                arrayOf("Q", listOf(BoardInput.Letter('Q'))),
            )
    }
}
