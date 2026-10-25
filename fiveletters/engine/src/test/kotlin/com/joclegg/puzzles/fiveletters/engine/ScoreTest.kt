package com.joclegg.puzzles.fiveletters.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class ScoreTest {
    @ParameterizedTest
    @CsvSource(
        "CRANE,CRANE,CCCCC",
        "TRACE,CRANE,ACCPC",
        "SPEED,ABIDE,AAPAP",
        "EERIE,THEME,PAAAC",
        "LLAMA,HELLO,PPAAA",
        "ROBOT,FLOOR,PPACA",
    )
    fun `given a guess and an answer when it is scored then repeated letters are only marked as often as the answer has them`(
        guess: String,
        answer: String,
        expected: String,
    ) {
        // Act
        val marks = score(guess, answer)

        // Assert
        assertEquals(expected, marks.joinToString("") { it.name.first().toString() })
    }
}
