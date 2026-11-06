package com.joclegg.puzzles.kakuro.engine

val SIZES = listOf(6, 8, 10, 12)

const val MAX_DIGIT = 9

data class Run(
    val cells: List<Int>,
    val sum: Int,
    val isAcross: Boolean,
    val clueCell: Int,
)

data class Puzzle(
    val size: Int,
    val solution: List<Int>,
    val givens: Set<Int> = emptySet(),
) {
    val isWhite: List<Boolean> = solution.map { it != 0 }

    val runs: List<Run> by lazy { findRuns() }

    val runsOf: Map<Int, List<Run>> by lazy {
        runs.flatMap { run -> run.cells.map { it to run } }.groupBy({ it.first }, { it.second })
    }

    val acrossClues: Map<Int, Int> by lazy { runs.filter { it.isAcross }.associate { it.clueCell to it.sum } }

    val downClues: Map<Int, Int> by lazy { runs.filter { !it.isAcross }.associate { it.clueCell to it.sum } }

    fun encode() = "$size;${solution.joinToString("")};${givens.sorted().joinToString(",")}"

    private fun findRuns(): List<Run> =
        listOf(true, false).flatMap { across ->
            (0 until size).flatMap { line ->
                val cells = (0 until size).map { position -> cellAt(line, position, across) }
                segments(cells).map { run -> Run(run, run.sumOf { solution[it] }, across, clueBefore(run.first(), across)) }
            }
        }

    private fun segments(cells: List<Int>): List<List<Int>> {
        val found = mutableListOf<List<Int>>()
        var current = mutableListOf<Int>()
        for (cell in cells + -1) {
            if (cell >= 0 && isWhite[cell]) {
                current += cell
            } else {
                if (current.size > 1) {
                    found += current
                }
                current = mutableListOf()
            }
        }
        return found
    }

    private fun cellAt(
        line: Int,
        position: Int,
        across: Boolean,
    ) = if (across) {
        line * size + position
    } else {
        position * size + line
    }

    private fun clueBefore(
        cell: Int,
        across: Boolean,
    ) = if (across) {
        cell - 1
    } else {
        cell - size
    }

    companion object {
        fun decode(text: String): Puzzle {
            val (size, solution, givens) = text.split(';')
            return Puzzle(
                size.toInt(),
                solution.map { it.digitToInt() },
                givens
                    .split(',')
                    .filter { it.isNotEmpty() }
                    .map(String::toInt)
                    .toSet(),
            )
        }
    }
}
