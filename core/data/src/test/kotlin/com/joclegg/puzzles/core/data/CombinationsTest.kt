package com.joclegg.puzzles.core.data

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.random.Random
import kotlin.test.assertEquals

class CombinationsTest {
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
}
