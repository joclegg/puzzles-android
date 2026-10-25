package com.joclegg.puzzles.slideout.core.data

import com.joclegg.puzzles.slideout.engine.Level
import com.joclegg.puzzles.slideout.engine.Puzzle
import java.util.concurrent.ConcurrentHashMap

class PuzzleBank(
    private val read: (String) -> String,
) {
    private val loaded = ConcurrentHashMap<Level, List<Puzzle>>()

    fun puzzles(level: Level): List<Puzzle> =
        loaded.getOrPut(level) {
            read("slideout/${level.name.lowercase()}.txt").lines().filter { it.isNotBlank() }.map(Puzzle::decode)
        }
}
