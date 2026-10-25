package com.joclegg.puzzles.sudoku.core.data

import com.joclegg.puzzles.sudoku.engine.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.LocalDate

@RunWith(Parameterized::class)
class PuzzleBankTest(
    private val mode: GameMode,
) {
    private val line = "0".repeat(81) + "|" + "1".repeat(81) + "|"
    private val bank = PuzzleBank { List(3) { line }.joinToString("\n") }
    private val date = LocalDate.of(2026, 10, 24)

    @Test
    fun `given two puzzles played when the next is picked then it is the unplayed one`() {
        // Arrange
        val prefix = "${mode.name.lowercase()}_easy"
        val played = setOf("$prefix:0", "$prefix:1")

        // Act
        val next = bank.next(mode, Difficulty.EASY, played)

        // Assert
        assertEquals("$prefix:2", next.id)
    }

    @Test
    fun `given a date when the daily is picked twice then it is the same puzzle, named by mode and date`() {
        // Act
        val first = bank.daily(mode, date)

        // Assert
        assertEquals(first to "daily_${mode.name.lowercase()}:2026-10-24", bank.daily(mode, date) to first.id)
    }

    @Test
    fun `given a year of dates when daily levels are picked then every level of the mode comes up`() {
        // Arrange
        val start = LocalDate.of(2026, 1, 1)

        // Act
        val levels = (0L until 365L).map { bank.dailyLevel(mode, start.plusDays(it)) }.toSet()

        // Assert
        assertEquals(mode.levels.toSet(), levels)
    }

    @Test
    fun `given a date when the daily is picked then its level is the day's level`() {
        // Act
        val daily = bank.daily(mode, date)

        // Assert
        assertEquals(bank.dailyLevel(mode, date), daily.difficulty)
    }

    @Test
    fun `given a date when both modes' dailies are picked then they are different games`() {
        // Arrange
        val other = GameMode.entries.first { it != mode }

        // Act
        val dailies = bank.daily(mode, date) to bank.daily(other, date)

        // Assert
        assertNotEquals(dailies.first.id, dailies.second.id)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun modes() = GameMode.entries.map { arrayOf<Any>(it) }
    }
}
