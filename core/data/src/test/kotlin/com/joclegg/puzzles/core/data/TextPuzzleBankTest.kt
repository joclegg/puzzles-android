package com.joclegg.puzzles.core.data

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class TextPuzzleBankTest {
    @Test
    fun `given a bank file with blank lines when puzzles are read then each non-blank line is decoded once`() {
        val reads = mutableListOf<String>()
        val bank =
            TextPuzzleBank<Int, String>(
                read = { path ->
                    reads += path
                    "a\n\nb\n"
                },
                path = { "game/$it.txt" },
                decode = String::uppercase,
            )

        assertEquals(listOf("A", "B"), bank.puzzles(5))
        assertEquals(listOf("A", "B"), bank.puzzles(5))
        assertEquals(listOf("game/5.txt"), reads)
    }

    @Test
    fun `given a bank when its shuffled order is read twice then it matches a fresh shuffle with that seed`() {
        // Arrange
        val bank =
            TextPuzzleBank<Int, String>(
                read = { (1..20).joinToString("\n") },
                path = { "game/$it.txt" },
                decode = { it },
            )

        // Act
        val orders = listOf(bank.shuffledOrder(5, 7), bank.shuffledOrder(5, 7))

        // Assert
        assertEquals(List(2) { (0 until 20).shuffled(Random(7)) }, orders)
    }
}
