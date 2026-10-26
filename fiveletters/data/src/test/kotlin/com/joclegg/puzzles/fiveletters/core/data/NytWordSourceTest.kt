package com.joclegg.puzzles.fiveletters.core.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class NytWordSourceTest {
    @Test
    fun `given the NYT answer when the daily word is fetched then its solution is used in capitals`() =
        runTest {
            // Arrange
            var requested = ""
            val source =
                NytWordSource { url ->
                    requested = url
                    """{"id":921,"solution":"geeky","print_date":"2026-10-26"}"""
                }

            // Act
            val word = source.word(LocalDate.of(2026, 10, 26))

            // Assert
            assertEquals("GEEKY" to "https://www.nytimes.com/svc/wordle/v2/2026-10-26.json", word to requested)
        }

    @Test
    fun `given no connection when the daily word is fetched then there is no word`() =
        runTest {
            // Arrange
            val source = NytWordSource { error("offline") }

            // Act
            val word = source.word(LocalDate.of(2026, 10, 26))

            // Assert
            assertNull(word)
        }

    @Test
    fun `given an answer that is not five letters when it is read then it is ignored`() {
        // Act
        val word = NytWordSource.solutionOf("""{"solution":"toolong"}""")

        // Assert
        assertNull(word)
    }
}
