package com.joclegg.puzzles.sudoku.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals

class GraderTest {
    @ParameterizedTest
    @MethodSource("gradedPuzzles")
    fun `given a puzzle when it is graded then it gets the level of its hardest technique`(
        givens: String,
        expectedLevel: Difficulty?,
    ) {
        // Arrange
        val grader = Grader(digits(givens))

        // Act
        val grade = grader.grade()

        // Assert
        assertEquals(expectedLevel, grade.level)
    }

    @Test
    fun `given an easy puzzle when it is graded then the logical solve finds the solution`() {
        // Arrange
        val grader = Grader(digits(EASY_GIVENS))

        // Act
        grader.grade()

        // Assert
        assertEquals(digits(EASY_SOLUTION), grader.values())
    }

    companion object {
        @JvmStatic
        fun gradedPuzzles() =
            listOf(
                Arguments.of(EASY_GIVENS, Difficulty.EASY),
                Arguments.of("800000000003600000070090000050007000000045700000100030001000068008500000000000400", null),
            )
    }
}
