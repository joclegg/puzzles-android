package com.joclegg.puzzles.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ShareTextTest {
    private val date = LocalDate.of(2026, 11, 20)

    @Test
    fun `given a day of dailies when the dailies share is built then it matches the agreed format`() {
        // Arrange
        val body =
            listOf(
                dailyLine(ShareDaily("Easy", 151_000)),
                dailyLine(ShareDaily("Medium", 252_000, best = true)),
                dailyLine(ShareDaily("Hard", 588_000)),
                dailyLine(ShareDaily("Expert", null)),
            )
        val week = listOf(Medal.GOLD, Medal.GOLD, Medal.SILVER, Medal.GOLD, Medal.GOLD, Medal.NONE, Medal.SILVER)

        // Act
        val text = dailiesShareText("Sudoku", date, Medal.SILVER, body, week)

        // Assert
        assertEquals(
            "Sudoku · 20 Nov 🥈\nEasy 2:31\nMedium 4:12 ⭐\nHard 9:48\nExpert ⬛\n\nLast 7 days 🟨🟨⬜🟨🟨⬛⬜\njoclegg.github.io/puzzles-web",
            text,
        )
    }

    @Test
    fun `given no medal when the dailies share is built then the heading has no emoji`() {
        // Act
        val text = dailiesShareText("Solitaire", date, Medal.NONE, listOf(solitaireDailyLine(null)), List(7) { Medal.BRONZE })

        // Assert
        assertEquals("Solitaire · 20 Nov\nNot won\n\nLast 7 days 🟧🟧🟧🟧🟧🟧🟧\njoclegg.github.io/puzzles-web", text)
    }

    @Test
    fun `given moves when a daily line is built then the moves follow the time`() {
        // Act
        val line = dailyLine(ShareDaily("Easy", 65_000, best = true, moves = 12))

        // Assert
        assertEquals("Easy 1:05 · 12 moves ⭐", line)
    }

    @Test
    fun `given a won solitaire daily when its line is built then it shows the time`() {
        // Act
        val line = solitaireDailyLine(3_723_000)

        // Assert
        assertEquals("Won in 1:02:03", line)
    }

    @Test
    fun `given a date when the last seven days are listed then they run oldest first ending on that day`() {
        // Act
        val days = lastSevenDays(date) { mapOf(date.minusDays(6) to Medal.GOLD)[it] ?: Medal.NONE }

        // Assert
        assertEquals(listOf(Medal.GOLD) + List(6) { Medal.NONE }, days)
    }

    @Test
    fun `given records when the records share is built then unsolved sizes are left out`() {
        // Arrange
        val lines = recordLines(listOf(ShareRecord("6×6", 61_000), ShareRecord("8×8", null), ShareRecord("Hard", 90_000, 14)))

        // Act
        val text = recordsShareText("Two Tone", lines, solvedSummary(12, 3, 5))

        // Assert
        assertEquals(
            "Two Tone records 🏆\n6×6 1:01\nHard 1:30 · 14 moves\n\n12 solved · 🔥 3 · longest 🔥 5\njoclegg.github.io/puzzles-web",
            text,
        )
    }

    @Test
    fun `given solitaire stats when its records are built then wins and percent are shown`() {
        // Act
        val lines = solitaireRecordLines(125_000, 98)
        val summary = solitaireSummary(2, 3, 1, 4)

        // Assert
        assertEquals(listOf("Fastest win 2:05", "Fewest moves 98"), lines)
        assertEquals("2 wins · 66% · 🔥 1 · longest 🔥 4", summary)
    }

    @Test
    fun `given guess counts when the chart is built then the largest bucket has eight squares`() {
        // Act
        val chart = guessChart(listOf(0, 1, 4, 16, 8, 2))

        // Assert
        assertEquals(
            listOf("1  0", "2 🟩 1", "3 🟩🟩 4", "4 🟩🟩🟩🟩🟩🟩🟩🟩 16", "5 🟩🟩🟩🟩 8", "6 🟩 2"),
            chart,
        )
        assertEquals("5/7 won · 🔥 2 · longest 🔥 3", wordSummary(5, 7, 2, 3))
    }

    @Test
    fun `given solved days when streaks are counted then today or yesterday ends the current run`() {
        // Arrange
        val days = setOf(date.minusDays(1), date.minusDays(2), date.minusDays(5), date.minusDays(6), date.minusDays(7), date.minusDays(8))

        // Act
        val current = currentStreak(days, date)
        val longest = longestStreak(days)

        // Assert
        assertEquals(2, current)
        assertEquals(4, longest)
    }

    @Test
    fun `given daily ids when the date is read then only dailies have one`() {
        // Assert
        assertEquals(date, dailyDateOf("daily_classic_easy:2026-11-20"))
        assertEquals(date, dailyDateOf("daily_2026-11-20_6"))
        assertEquals(date, dailyDateOf("daily2_2026-11-20"))
        assertEquals(null, dailyDateOf("practice_2026-11-20"))
    }
}
