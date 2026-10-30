package com.joclegg.puzzles.solitaire.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class LostMoveSearchTest {
    @ParameterizedTest
    @CsvSource("0, 40, 20", "10, 13, 11", "12, 13, ")
    fun `given a winnable and a lost move when probing then it checks the move between them`(
        winnable: Int,
        lost: Int,
        probe: Int?,
    ) {
        // Act
        val next = LostMoveSearch(winnable, lost).probe

        // Assert
        assertEquals(probe, next)
    }

    @ParameterizedTest
    @CsvSource("WINNABLE, 20, 40", "UNWINNABLE, 0, 20", "UNKNOWN, 0, 20")
    fun `given a verdict at the probe when narrowing then it keeps a proven range`(
        verdict: Verdict,
        winnable: Int,
        lost: Int,
    ) {
        // Act
        val narrowed = LostMoveSearch(0, 40).narrow(20, verdict)

        // Assert
        assertEquals(LostMoveSearch(winnable, lost), narrowed)
    }
}
