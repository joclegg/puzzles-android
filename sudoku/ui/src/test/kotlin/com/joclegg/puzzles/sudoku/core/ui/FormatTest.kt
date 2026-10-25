package com.joclegg.puzzles.sudoku.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FormatTest(
    private val text: String,
    private val expectedMs: Long?,
) {
    @Test
    fun `given typed text when it is read as a duration then minutes and seconds become milliseconds`() {
        // Act
        val ms = parseDuration(text)

        // Assert
        assertEquals(expectedMs, ms)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() =
            listOf(
                arrayOf<Any?>("0:25", 25_000L),
                arrayOf<Any?>("7:54", 474_000L),
                arrayOf<Any?>(" 1:02 ", 62_000L),
                arrayOf<Any?>("1:02:03", 3_723_000L),
                arrayOf<Any?>("0:60", null),
                arrayOf<Any?>("25", null),
                arrayOf<Any?>("0:00", null),
                arrayOf<Any?>("a:10", null),
                arrayOf<Any?>("", null),
            )
    }
}
