package com.joclegg.puzzles.sudoku.core.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.LocalDate

@RunWith(Parameterized::class)
class DailyIdsTest(
    private val id: String,
    private val expected: Pair<GameMode, LocalDate>?,
) {
    @Test
    fun `given an id when it is parsed then dailies give their mode and date and other ids give nothing`() {
        // Act
        val parsed = DailyIds.parse(id)

        // Assert
        assertEquals(expected, parsed)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() =
            listOf(
                arrayOf<Any?>(DailyIds.of(GameMode.KILLER, LocalDate.of(2026, 10, 24)), GameMode.KILLER to LocalDate.of(2026, 10, 24)),
                arrayOf<Any?>("daily_classic:2026-01-31", GameMode.CLASSIC to LocalDate.of(2026, 1, 31)),
                arrayOf<Any?>("classic_hard:4", null),
                arrayOf<Any?>("daily_classic:not-a-date", null),
            )
    }
}
