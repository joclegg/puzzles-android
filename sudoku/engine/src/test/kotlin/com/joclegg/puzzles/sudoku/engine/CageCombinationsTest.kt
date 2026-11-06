package com.joclegg.puzzles.sudoku.engine

import com.joclegg.puzzles.core.data.Combinations
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CageCombinationsTest {
    private fun killer(
        grid: Grid,
        cage: Cage,
    ) = GameState(Puzzle(List(grid.cells) { 0 }, null, listOf(cage)))

    private fun GameState.combinations() = selectedCageCombinations()?.second?.map { Combinations.digitsOf(it).joinToString(" ") }

    @ParameterizedTest
    @CsvSource("2,10,1 9;2 8;3 7;4 6", "2,17,8 9", "3,6,1 2 3")
    fun `given a selected cell in an empty cage when its combinations are listed then every set of different digits that adds up is there`(
        length: Int,
        sum: Int,
        expected: String,
    ) {
        // Arrange
        val game = killer(Grid.STANDARD, Cage((0 until length).toList(), sum)).copy(selected = 0)

        // Act
        val combinations = game.combinations()

        // Assert
        assertEquals(expected.split(';').toSet(), combinations?.toSet())
    }

    @Test
    fun `given a cage with a placed digit when its combinations are listed then only those holding that digit remain`() {
        // Arrange
        val game =
            killer(Grid.STANDARD, Cage(listOf(0, 1), 10))
                .copy(selected = 1)
                .let { it.copy(values = it.values.toMutableList().also { values -> values[0] = 3 }) }

        // Act
        val combinations = game.combinations()

        // Assert
        assertEquals(listOf("3 7"), combinations)
    }

    @Test
    fun `given a six by six grid when a cage's combinations are listed then digits above six are left out`() {
        // Arrange
        val game = killer(Grid.FAST, Cage(listOf(0, 1), 11)).copy(selected = 0)

        // Act
        val combinations = game.combinations()

        // Assert
        assertEquals(listOf("5 6"), combinations)
    }

    @Test
    fun `given a selected cell outside every cage when combinations are listed then there are none`() {
        // Arrange
        val game = killer(Grid.STANDARD, Cage(listOf(0, 1), 10)).copy(selected = 5)

        // Act
        val combinations = game.selectedCageCombinations()

        // Assert
        assertNull(combinations)
    }
}
