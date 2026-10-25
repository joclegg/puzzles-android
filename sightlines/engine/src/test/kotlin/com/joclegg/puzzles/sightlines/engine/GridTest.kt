package com.joclegg.puzzles.sightlines.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class GridTest {
    @ParameterizedTest
    @CsvSource(
        "BBB. .... .... ....,0,2,6",
        "BBRB B... B... R...,0,3,3",
        "BRBB .... .... ....,0,0,3",
        "B... B... B... B...,5,1,6",
    )
    fun `given a grid when a cell looks along its row and column then it sees blue dots up to the first red`(
        text: String,
        cell: Int,
        seen: Int,
        reach: Int,
    ) {
        // Arrange
        val grid = Grid.parse(4, text)

        // Act
        val result = grid.seen(cell) to Direction.entries.sumOf { grid.reach(cell, it) }

        // Assert
        assertEquals(seen to reach, result)
    }
}
