package com.joclegg.puzzles.sudoku.core.data

import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import com.joclegg.puzzles.sudoku.engine.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(Parameterized::class)
class DailyResultsTest(
    private val description: String,
    private val games: List<SavedGame>,
    private val expectedMedal: Medal,
    private val expectedDoneLate: Boolean,
) {
    @Test
    fun `given finished and unfinished games when the day's result is worked out then the medal and tick are right`() {
        // Act
        val day = dailyDays(games, ZoneOffset.UTC)[DAY]

        // Assert
        assertEquals(expectedMedal to expectedDoneLate, (day?.medal ?: Medal.NONE) to (day?.doneLate ?: false))
    }

    companion object {
        private val DAY = LocalDate.of(2026, 10, 24)
        private val puzzle = Puzzle(List(81) { 1 }, List(81) { 1 })

        private fun daily(
            mode: GameMode,
            finishedOn: LocalDate? = DAY,
            status: GameStatus = GameStatus.COMPLETED,
            id: String = DailyIds.of(mode, DAY),
        ) = SavedGame(
            id = 0,
            ref = PuzzleRef(id, mode, Difficulty.HARD, puzzle),
            values = puzzle.givens,
            notes = List(81) { 0 },
            elapsedMs = 0,
            mistakes = 0,
            mistakeCheck = MistakeCheck.SHOW_WRONG,
            status = status,
            startedAt = 0,
            finishedAt = finishedOn?.atTime(12, 0)?.toInstant(ZoneOffset.UTC)?.toEpochMilli(),
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() =
            listOf(
                arrayOf<Any?>("classic on the day", listOf(daily(GameMode.CLASSIC)), Medal.SILVER, false),
                arrayOf<Any?>("killer on the day", listOf(daily(GameMode.KILLER)), Medal.SILVER, false),
                arrayOf<Any?>("both on the day", listOf(daily(GameMode.CLASSIC), daily(GameMode.KILLER)), Medal.GOLD, false),
                arrayOf<Any?>("classic two days late", listOf(daily(GameMode.CLASSIC, finishedOn = DAY.plusDays(2))), Medal.NONE, true),
                arrayOf<Any?>(
                    "one late, one on the day",
                    listOf(daily(GameMode.CLASSIC, finishedOn = DAY.plusDays(2)), daily(GameMode.KILLER)),
                    Medal.SILVER,
                    false,
                ),
                arrayOf<Any?>("still in progress", listOf(daily(GameMode.CLASSIC, status = GameStatus.IN_PROGRESS)), Medal.NONE, false),
                arrayOf<Any?>("an ordinary game", listOf(daily(GameMode.CLASSIC, id = "classic_hard:4")), Medal.NONE, false),
            )
    }
}
