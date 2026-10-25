package com.joclegg.puzzles.wordwheel.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class CrosswordTest {
    private val crossword = Crossword(listOf(Placement("PARADE", 0, 0, across = true)))

    @ParameterizedTest
    @CsvSource(
        "DRAPE,0,5,false,false",
        "DARE,-3,5,false,true",
        "READ,0,3,false,false",
        "AREA,0,1,false,true",
        "PARE,1,0,true,false",
        "DEAR,0,6,false,false",
    )
    fun `given a word on the grid when another is placed then it must cross and not touch side by side`(
        word: String,
        row: Int,
        col: Int,
        across: Boolean,
        allowed: Boolean,
    ) {
        // Act
        val result = crossword.canPlace(Placement(word, row, col, across))

        // Assert
        assertEquals(allowed, result)
    }

    @ParameterizedTest
    @CsvSource("'PARADE@0,0,A|AREA@0,1,D'")
    fun `given a crossword when it is encoded and decoded then it is unchanged`(text: String) {
        // Act
        val decoded = Crossword.decode(text)

        // Assert
        assertEquals(text, decoded.encode())
    }
}
