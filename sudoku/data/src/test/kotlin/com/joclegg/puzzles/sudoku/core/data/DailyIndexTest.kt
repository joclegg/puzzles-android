package com.joclegg.puzzles.sudoku.core.data

import com.joclegg.puzzles.sudoku.engine.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.LocalDate

@RunWith(Parameterized::class)
class DailyIndexTest(
    private val mode: GameMode,
    private val difficulty: Difficulty,
    private val expected: Int,
) {
    @Test
    fun `given a date and level when the daily is picked then it is the same line puzzles-web picks`() {
        // Act
        val index = dailyIndex(mode, difficulty, LocalDate.of(2026, 10, 25), BANK_SIZE)

        // Assert
        assertEquals(expected, index)
    }

    companion object {
        private const val BANK_SIZE = 10_000

        @JvmStatic
        @Parameterized.Parameters(name = "{0} {1}")
        fun cases() =
            listOf(
                arrayOf<Any>(GameMode.CLASSIC, Difficulty.FAST, 2737),
                arrayOf<Any>(GameMode.CLASSIC, Difficulty.MEDIUM, 4380),
                arrayOf<Any>(GameMode.CLASSIC, Difficulty.EXPERT, 5377),
                arrayOf<Any>(GameMode.KILLER, Difficulty.FAST, 3934),
                arrayOf<Any>(GameMode.KILLER, Difficulty.EVIL, 5057),
            )
    }
}
