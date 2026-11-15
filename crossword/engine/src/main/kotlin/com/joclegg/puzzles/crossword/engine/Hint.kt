package com.joclegg.puzzles.crossword.engine

data class Hint(
    val cell: Int,
    val letter: Char,
    val message: String,
)

fun GameState.findHint(): Hint? {
    val wrong = wrongCells.minOrNull()
    if (wrong != null) {
        return Hint(wrong, puzzle.solution[wrong], "This letter is wrong.")
    }
    val cell = nextEmptyCell() ?: return null
    val letter = puzzle.solution[cell]
    return Hint(cell, letter, "Try $letter here.")
}

private fun GameState.nextEmptyCell(): Int? {
    val words = puzzle.words
    if (words.isEmpty()) {
        return null
    }
    val start = currentWord?.let { words.indexOf(it) }?.coerceAtLeast(0) ?: 0
    val ordered = words.indices.map { words[(start + it).mod(words.size)] }
    return ordered.firstNotNullOfOrNull { word -> word.cells.firstOrNull { entries[it] == EMPTY } }
}
