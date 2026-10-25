package com.joclegg.puzzles.wordwheel.engine

import kotlin.random.Random

data class Puzzle(
    val letters: String,
    val crossword: Crossword,
) {
    fun encode() = "$letters;${crossword.encode()}"

    companion object {
        fun decode(text: String): Puzzle {
            val (letters, crossword) = text.split(';')
            return Puzzle(letters, Crossword.decode(crossword))
        }
    }
}

object Generator {
    private const val ATTEMPTS = 40

    fun generate(
        words: WordList,
        level: Level,
        base: String,
        random: Random,
    ): Puzzle? {
        val candidates = words.spellable(base, words.common)
        if (candidates.size < level.words.first) {
            return null
        }
        var best: Crossword? = null
        repeat(ATTEMPTS) {
            val crossword = layout(base, candidates.filter { it != base }.shuffled(random), level.words.last)
            if (crossword.placements.size > (best?.placements?.size ?: 0)) {
                best = crossword
            }
        }
        val chosen = best?.takeIf { it.placements.size >= level.words.first } ?: return null
        val letters = base.toList().shuffled(random).joinToString("")
        return Puzzle(letters, chosen.normalised())
    }

    private fun layout(
        base: String,
        others: List<String>,
        limit: Int,
    ): Crossword {
        var crossword = Crossword(listOf(Placement(base, 0, 0, across = true)))
        for (word in others.sortedByDescending { it.length }) {
            if (crossword.placements.size >= limit) {
                break
            }
            val options =
                crossword.placements.flatMap { placed ->
                    placed.cells.flatMapIndexed { index, (row, col) ->
                        word.indices.filter { word[it] == placed.word[index] }.map { at ->
                            if (placed.across) {
                                Placement(word, row - at, col, across = false)
                            } else {
                                Placement(word, row, col - at, across = true)
                            }
                        }
                    }
                }
            val placement = options.firstOrNull { crossword.canPlace(it) } ?: continue
            crossword = Crossword(crossword.placements + placement)
        }
        return crossword
    }
}
