package com.joclegg.puzzles.wordwheel.engine

import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals

class GeneratorTest {
    @Test
    fun `given a base word with enough smaller words when a puzzle is made then words cross and use only wheel letters`() {
        // Arrange
        val all = listOf("parade", "drape", "dear", "read", "dare", "area", "pear", "reap", "rap", "par", "ape", "era", "red")
        val words = WordList(all, all)

        // Act
        val puzzle = Generator.generate(words, Level.FIVE, "PARADE", Random(1))!!

        // Assert
        val crossword = puzzle.crossword
        assertEquals(true, crossword.placements.size >= Level.FIVE.words.first)
        assertEquals(true, crossword.words.all { WordList.fits(it, puzzle.letters) })
        assertEquals(
            crossword.letters.size,
            crossword.placements.sumOf { it.word.length } - (crossword.placements.size - 1) - extraCrossings(crossword),
        )
    }

    private fun extraCrossings(crossword: Crossword): Int {
        val counts =
            crossword.placements
                .flatMap { it.cells }
                .groupingBy { it }
                .eachCount()
        return counts.values.sumOf { it - 1 } - (crossword.placements.size - 1)
    }
}
