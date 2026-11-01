package com.joclegg.puzzles.wordwheel.core.data

import com.joclegg.puzzles.core.data.TextPuzzleBank
import com.joclegg.puzzles.wordwheel.engine.Level
import com.joclegg.puzzles.wordwheel.engine.Puzzle
import com.joclegg.puzzles.wordwheel.engine.WordList

class PuzzleBank(
    private val read: (String) -> String,
) : TextPuzzleBank<Level, Puzzle>(read, { "wordwheel/${it.name.lowercase()}.txt" }, Puzzle::decode) {
    val words: WordList by lazy {
        val valid = read("wordwheel/valid.txt").lines().filter { it.isNotBlank() }
        WordList(valid, emptyList())
    }
}
