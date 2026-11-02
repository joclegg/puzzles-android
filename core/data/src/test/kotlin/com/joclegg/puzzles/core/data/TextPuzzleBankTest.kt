package com.joclegg.puzzles.core.data

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
}
