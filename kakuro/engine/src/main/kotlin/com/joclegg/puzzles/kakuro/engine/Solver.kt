package com.joclegg.puzzles.kakuro.engine

object Solver {
    fun candidates(
        puzzle: Puzzle,
        values: List<Int>,
        cell: Int,
    ): Int =
        puzzle.runsOf
            .getValue(cell)
            .fold(FULL) { allowed, run ->
                val placed =
                    run.cells.filter { it != cell && values[it] != 0 }.fold(0) { mask, other ->
                        mask or
                            Combinations.bit(values[other])
                    }
                val fits =
                    Combinations
                        .masks(run.cells.size, run.sum)
                        .filter { it and placed == placed }
                        .fold(0) { union, mask -> union or mask }
                allowed and (fits and placed.inv())
            }

    fun solutions(
        puzzle: Puzzle,
        start: List<Int>,
        limit: Int = 2,
    ): List<List<Int>> {
        val found = mutableListOf<List<Int>>()
        search(puzzle, start.toMutableList(), limit, found)
        return found
    }

    private fun search(
        puzzle: Puzzle,
        values: MutableList<Int>,
        limit: Int,
        found: MutableList<List<Int>>,
    ) {
        if (found.size >= limit) {
            return
        }
        val open = values.indices.filter { puzzle.isWhite[it] && values[it] == 0 }
        if (open.isEmpty()) {
            found += values.toList()
            return
        }
        val options = open.map { it to candidates(puzzle, values, it) }
        val (cell, mask) = options.minBy { Integer.bitCount(it.second) }
        for (digit in Combinations.digitsOf(mask)) {
            values[cell] = digit
            search(puzzle, values, limit, found)
            values[cell] = 0
            if (found.size >= limit) {
                return
            }
        }
    }

    private const val FULL = (1 shl MAX_DIGIT) - 1
}
