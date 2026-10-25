package com.joclegg.puzzles.wordwheel.engine

enum class Guess { FOUND, BONUS, ALREADY, NOT_A_WORD, TOO_SHORT }

data class GameState(
    val puzzle: Puzzle,
    val found: List<String> = emptyList(),
    val bonus: List<String> = emptyList(),
    val revealed: Set<Pair<Int, Int>> = emptySet(),
    val wheel: String = puzzle.letters,
    val last: Guess? = null,
) {
    val isSolved: Boolean get() = puzzle.crossword.words.all { it in found }

    val shownCells: Set<Pair<Int, Int>>
        get() =
            puzzle.crossword.placements
                .filter { it.word in found }
                .flatMap { it.cells }
                .toSet() + revealed

    fun submit(
        word: String,
        words: WordList,
    ): GameState {
        val guess = word.uppercase()
        val result =
            when {
                isSolved -> return this
                guess.length < MIN_WORD -> Guess.TOO_SHORT
                guess in found || guess in bonus -> Guess.ALREADY
                guess in puzzle.crossword.words -> Guess.FOUND
                guess in words.valid && WordList.fits(guess, puzzle.letters) -> Guess.BONUS
                else -> Guess.NOT_A_WORD
            }
        return when (result) {
            Guess.FOUND -> copy(found = found + guess, last = result)
            Guess.BONUS -> copy(bonus = bonus + guess, last = result)
            else -> copy(last = result)
        }
    }

    fun shuffle(order: List<Int>): GameState = copy(wheel = order.map { wheel[it] }.joinToString(""), last = null)

    fun reveal(pick: (List<Pair<Int, Int>>) -> Pair<Int, Int>?): GameState {
        val hidden =
            puzzle.crossword.letters.keys
                .filter { it !in shownCells }
                .sortedWith(compareBy({ it.first }, { it.second }))
        val cell = pick(hidden) ?: return this
        val withCell = copy(revealed = revealed + cell, last = null)
        val completed =
            puzzle.crossword.placements
                .filter { it.word !in found && it.cells.all { cell -> cell in withCell.shownCells } }
                .map { it.word }
        return withCell.copy(found = found + completed)
    }
}
