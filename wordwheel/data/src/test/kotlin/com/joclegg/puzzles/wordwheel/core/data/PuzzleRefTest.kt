package com.joclegg.puzzles.wordwheel.core.data

import com.joclegg.puzzles.wordwheel.engine.Level
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PuzzleRefTest {
    @Test
    fun `given a level when the daily id is made then it matches the web's lower case id`() {
        // Act
        val id = PuzzleRef.dailyId(LocalDate.of(2026, 10, 30), Level.FIVE)

        // Assert
        assertEquals("daily_2026-10-30_five", id)
    }
}
