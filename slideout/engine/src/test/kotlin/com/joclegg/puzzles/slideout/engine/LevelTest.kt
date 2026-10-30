package com.joclegg.puzzles.slideout.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class LevelTest {
    @ParameterizedTest
    @CsvSource("HARD, HARD", "EXPERT, EXPERT", "EVIL, EXPERT")
    fun `given a saved level name when it is read then retired levels become Expert`(
        name: String,
        expected: Level,
    ) {
        // Act
        val level = Level.named(name)

        // Assert
        assertEquals(expected, level)
    }

    @ParameterizedTest
    @CsvSource("0, BEGINNER", "4, EXPERT", "5, EXPERT")
    fun `given a synced size when it is read then retired levels become Expert`(
        size: Int,
        expected: Level,
    ) {
        // Act
        val level = Level.atSize(size)

        // Assert
        assertEquals(expected, level)
    }
}
