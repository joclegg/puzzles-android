package com.joclegg.puzzles.twotone.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class GridTest {
    @ParameterizedTest
    @CsvSource(
        "RRR. .... .... ....,0 1 2",
        "R... R... R... ....,0 4 8",
        "RRBB BBRR RRBB ....,0 1 2 3 8 9 10 11",
        "RBRR .... .... ....,0 2 3",
        "RB.. BR.. .... ....,''",
    )
    fun `given a grid when broken tiles are found then only tiles breaking a rule are listed`(
        text: String,
        broken: String,
    ) {
        // Act
        val cells = Grid.parse(4, text).brokenCells()

        // Assert
        assertEquals(
            broken
                .split(' ')
                .filter { it.isNotEmpty() }
                .map(String::toInt)
                .toSet(),
            cells,
        )
    }
}
