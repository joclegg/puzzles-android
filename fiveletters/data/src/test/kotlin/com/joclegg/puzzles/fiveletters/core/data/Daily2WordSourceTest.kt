package com.joclegg.puzzles.fiveletters.core.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class Daily2WordSourceTest {
    @Test
    fun `given the Daily puzzle 2 word when it is fetched then it is used in capitals`() =
        runTest {
            // Arrange
            var requested = ""
            val source =
                Daily2WordSource { url ->
                    requested = url
                    """{"date":"2026-10-26","word":"geeky"}"""
                }

            // Act
            val word = source.word(LocalDate.of(2026, 10, 26))

            // Assert
            assertEquals("GEEKY" to "https://puzzles-daily.joclegg.dev/word/2026-10-26", word to requested)
        }

    @Test
    fun `given no connection when the daily word is fetched then there is no word`() =
        runTest {
            // Arrange
            val source = Daily2WordSource { error("offline") }

            // Act
            val word = source.word(LocalDate.of(2026, 10, 26))

            // Assert
            assertNull(word)
        }

    @Test
    fun `given an answer that is not five letters when it is read then it is ignored`() {
        // Act
        val word = Daily2WordSource.wordOf("""{"date":"2026-10-26","word":"toolong"}""")

        // Assert
        assertNull(word)
    }

    @Test
    fun `given a game saved under the legacy prefix when it is read then it is Daily puzzle 2 for that date`() {
        // Act
        val ref = WordRef("${WordRef.LEGACY_DAILY2_PREFIX}2026-10-26", "GEEKY")

        // Assert
        assertEquals(LocalDate.of(2026, 10, 26) to ref.slot, ref.daily2Date to ref.id)
    }
}
