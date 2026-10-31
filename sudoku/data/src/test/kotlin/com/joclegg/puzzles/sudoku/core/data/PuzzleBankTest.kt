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
    fun `given a date when a level's daily is picked twice then it is the same puzzle, named by mode, level and date`() {
        // Arrange
        val level = mode.levels.first()

        // Act
        val first = bank.daily(mode, level, date)

        // Assert
        assertEquals(
            first to "daily_${mode.name.lowercase()}_${level.name.lowercase()}:2026-10-24",
            bank.daily(mode, level, date) to first.id,
        )
    }

    @Test
    fun `given a date when every level's daily is picked then each has its own level and id`() {
        // Act
        val dailies = mode.levels.map { bank.daily(mode, it, date) }

        // Assert
        assertEquals(mode.levels to mode.levels.size, dailies.map { it.difficulty } to dailies.map { it.id }.toSet().size)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun modes() = GameMode.entries.map { arrayOf<Any>(it) }
    }
}
