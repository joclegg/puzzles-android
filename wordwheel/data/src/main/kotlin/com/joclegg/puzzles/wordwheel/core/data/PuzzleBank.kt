package com.joclegg.puzzles.wordwheel.core.data

import com.joclegg.puzzles.wordwheel.engine.Level
import com.joclegg.puzzles.wordwheel.engine.Puzzle
import com.joclegg.puzzles.wordwheel.engine.WordList
import java.util.concurrent.ConcurrentHashMap

class PuzzleBank(
    private val read: (String) -> String,
) {
    private val loaded = ConcurrentHashMap<Level, List<Puzzle>>()

    val words: WordList by lazy {
        val valid = read("wordwheel/valid.txt").lines().filter { it.isNotBlank() }
        WordList(valid, emptyList())
    }

    fun puzzles(level: Level): List<Puzzle> =
        loaded.getOrPut(level) {
            read("wordwheel/${level.name.lowercase()}.txt").lines().filter { it.isNotBlank() }.map(Puzzle::decode)
        }
}
