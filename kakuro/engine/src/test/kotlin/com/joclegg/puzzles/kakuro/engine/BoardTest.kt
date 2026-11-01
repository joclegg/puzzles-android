package com.joclegg.puzzles.kakuro.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.random.Random
import kotlin.test.assertEquals

class BoardTest {
    @ParameterizedTest
    @CsvSource(
        "INCREASING,1 5 9;1 6 8;2 4 9;2 5 8;2 6 7;3 4 8;3 5 7;4 5 6",
        "DECREASING,6 5 4;7 5 3;8 4 3;7 6 2;8 5 2;9 4 2;8 6 1;9 5 1",
    )
    fun `given a run's combinations when they are ordered then digits and combinations follow the order`(
        order: CombinationOrder,
        expected: String,
    ) {
        // Arrange
        val masks = Combinations.masks(3, 15).shuffled(Random(1))

        // Act
        val ordered = Combinations.ordered(masks, order).map { it.joinToString(" ") }

        // Assert
        assertEquals(expected.split(';'), ordered)
    }

    @Test
    fun `given a filled grid when its runs are found then each has its cells, sum and clue cell`() {
        // Act
        val runs = small.runs

        // Assert
        assertEquals(
            listOf(
                Run(listOf(4, 5), 3, true, 3),
                Run(listOf(7, 8), 7, true, 6),
                Run(listOf(4, 7), 4, false, 1),
                Run(listOf(5, 8), 6, false, 2),
            ),
            runs,
        )
    }

    @ParameterizedTest
    @CsvSource("2,3,1 2", "2,17,8 9", "3,6,1 2 3", "2,10,1 9;2 8;3 7;4 6")
    fun `given a run length and sum when combinations are listed then every set of different digits that adds up is there`(
        length: Int,
        sum: Int,
        expected: String,
    ) {
        // Act
        val combos = Combinations.masks(length, sum).map { Combinations.digitsOf(it).joinToString(" ") }

        // Assert
        assertEquals(expected.split(';').toSet(), combos.toSet())
    }

    @Test
    fun `given a puzzle when it is encoded and decoded then it is unchanged`() {
        // Arrange
        val puzzle = small.copy(givens = setOf(4))

        // Act
        val decoded = Puzzle.decode(puzzle.encode())

        // Assert
        assertEquals(puzzle, decoded)
    }
}
