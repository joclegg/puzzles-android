package com.joclegg.puzzles.kakuro.core.data

import com.joclegg.puzzles.kakuro.engine.Puzzle
import java.util.concurrent.ConcurrentHashMap

class PuzzleBank(
    private val read: (String) -> String,
) {
    private val loaded = ConcurrentHashMap<Int, List<Puzzle>>()

    fun puzzles(size: Int): List<Puzzle> =
        loaded.getOrPut(size) {
            read("kakuro/$size.txt").lines().filter { it.isNotBlank() }.map(Puzzle::decode)
        }
}
