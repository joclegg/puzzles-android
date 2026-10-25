package com.joclegg.puzzles.sudoku.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

class GridTest {
    @ParameterizedTest
    @MethodSource("boxCases")
    fun `given a grid and a cell when its box is worked out then it matches the grid's box shape`(
        grid: Grid,
        row: Int,
        col: Int,
        expectedBox: Int,
    ) {
        // Act
        val box = grid.boxOf(grid.cellAt(row, col))

        // Assert
        assertEquals(expectedBox, box)
    }

    @ParameterizedTest
    @MethodSource("grids")
    fun `given a grid when its houses and peers are built then every house has size cells and every cell the right peers`(
        grid: Grid,
        expectedPeers: Int,
    ) {
        // Act
        val houseSizes = grid.houses.map { it.size }.toSet()
        val peerCounts = grid.peers.map { it.size }.toSet()

        // Assert
        assertEquals(setOf(grid.size) to setOf(expectedPeers), houseSizes to peerCounts)
    }

    @ParameterizedTest
    @MethodSource("neighbourCases")
    fun `given a cell when its neighbours are listed then only cells sharing an edge are included`(
        grid: Grid,
        cell: Int,
        expected: Set<Int>,
    ) {
        // Act
        val neighbours = grid.neighbours(cell).toSet()

        // Assert
        assertEquals(expected, neighbours)
    }

    companion object {
        @JvmStatic
        fun neighbourCases() =
            listOf(
                Arguments.of(Grid.STANDARD, 0, setOf(1, 9)),
                Arguments.of(Grid.STANDARD, 40, setOf(31, 39, 41, 49)),
                Arguments.of(Grid.STANDARD, 80, setOf(71, 79)),
                Arguments.of(Grid.FAST, 0, setOf(1, 6)),
                Arguments.of(Grid.FAST, 35, setOf(29, 34)),
            )

        @JvmStatic
        fun boxCases() =
            listOf(
                Arguments.of(Grid.STANDARD, 0, 0, 0),
                Arguments.of(Grid.STANDARD, 4, 4, 4),
                Arguments.of(Grid.STANDARD, 8, 8, 8),
                Arguments.of(Grid.FAST, 0, 0, 0),
                Arguments.of(Grid.FAST, 0, 3, 1),
                Arguments.of(Grid.FAST, 2, 0, 2),
                Arguments.of(Grid.FAST, 5, 5, 5),
            )

        @JvmStatic
        fun grids() =
            listOf(
                Arguments.of(Grid.STANDARD, 20),
                Arguments.of(Grid.FAST, 12),
            )
    }
}
