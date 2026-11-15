package com.joclegg.puzzles.crossword.engine

val SIZES = listOf(11, 13, 15)

const val BLOCK = '#'

const val EMPTY = '.'

data class Word(
    val number: Int,
    val isAcross: Boolean,
    val cells: List<Int>,
) {
    val length: Int get() = cells.size
}

data class Puzzle(
    val size: Int,
    val solution: String,
) {
    val isWhite: List<Boolean> = solution.map { it != BLOCK }

    val numbers: Map<Int, Int> by lazy { findNumbers() }

    val across: List<Word> by lazy { findWords(true) }

    val down: List<Word> by lazy { findWords(false) }

    val words: List<Word> by lazy { across + down }

    private val acrossOf: Map<Int, Word> by lazy { across.flatMap { word -> word.cells.map { it to word } }.toMap() }

    private val downOf: Map<Int, Word> by lazy { down.flatMap { word -> word.cells.map { it to word } }.toMap() }

    fun wordAt(
        cell: Int,
        isAcross: Boolean,
    ): Word? =
        if (isAcross) {
            acrossOf[cell]
        } else {
            downOf[cell]
        }

    fun encode() = "$size;$solution"

    private fun startsWord(
        cell: Int,
        isAcross: Boolean,
    ): Boolean {
        if (!isWhite[cell]) {
            return false
        }
        val row = cell / size
        val col = cell % size
        return if (isAcross) {
            (col == 0 || !isWhite[cell - 1]) && col + 1 < size && isWhite[cell + 1]
        } else {
            (row == 0 || !isWhite[cell - size]) && row + 1 < size && isWhite[cell + size]
        }
    }

    private fun findNumbers(): Map<Int, Int> {
        val found = linkedMapOf<Int, Int>()
        for (cell in solution.indices) {
            if (startsWord(cell, true) || startsWord(cell, false)) {
                found[cell] = found.size + 1
            }
        }
        return found
    }

    private fun findWords(isAcross: Boolean): List<Word> =
        numbers.filterKeys { startsWord(it, isAcross) }.map { (start, number) ->
            val step =
                if (isAcross) {
                    1
                } else {
                    size
                }
            val cells = mutableListOf(start)
            while (true) {
                val next = cells.last() + step
                val wraps = isAcross && next % size == 0
                if (next >= solution.length || wraps || !isWhite[next]) {
                    break
                }
                cells += next
            }
            Word(number, isAcross, cells)
        }

    companion object {
        fun decode(text: String): Puzzle {
            val (size, solution) = text.trim().split(';')
            return Puzzle(size.toInt(), solution.uppercase())
        }
    }
}

fun enumeration(word: Word) = "(${word.length})"

class Clues(
    private val clues: Map<String, String>,
) {
    fun clueFor(answer: String): String = clues[answer].orEmpty()

    companion object {
        fun parse(text: String): Clues =
            Clues(
                text
                    .lines()
                    .filter { ';' in it }
                    .associate { line -> line.substringBefore(';').trim().uppercase() to line.substringAfter(';').trim() },
            )
    }
}
