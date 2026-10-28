package com.joclegg.puzzles.twotone.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SolverTest {
    @ParameterizedTest
    @CsvSource(
        "RR.. ........ ....,2,BLUE,PAIR",
        ".BB. ........ ....,0,RED,PAIR",
        "R.R. ........ ....,1,BLUE,GAP",
        "RB.R ........ ....,2,BLUE,BALANCE",
    )
    fun `given a row with one forced tile when the next deduction is found then it names the tile and the rule`(
        text: String,
        cell: Int,
        tile: Tile,
        reason: Reason,
    ) {
        // Arrange
        val grid = Grid.parse(4, text)

        // Act
        val deduction = Solver.next(grid)

        // Assert
        assertEquals(Triple(cell, tile, reason), Triple(deduction?.cells?.first(), deduction?.tile, deduction?.reason))
    }

    @Test
    fun `given a row that would copy a full row when the next deduction is found then it picks the other way round`() {
        // Arrange
        val grid = Grid.parse(6, "RBBRBR ...... ...... ...... ...... R.BRB.")

        // Act
        val deduction = Solver.next(grid)

        // Assert
        assertEquals(Triple(listOf(31), Tile.RED, Reason.UNIQUE), Triple(deduction?.cells, deduction?.tile, deduction?.reason))
        assertEquals(Line(isRow = true, index = 0), deduction?.twin)
    }

    @Test
    fun `given an empty grid when the next deduction is looked for then there is none`() {
        // Act
        val deduction = Solver.next(Grid.empty(4))

        // Assert
        assertNull(deduction)
    }
}
