package com.joclegg.puzzles.sudoku.engine

data class Hint(
    val cell: Int,
    val digit: Int,
    val message: String,
)

fun GameState.findHint(): Hint? {
    val grid = puzzle.grid
    val solution = puzzle.solution
    if (solution != null) {
        val wrong = values.indices.firstOrNull { values[it] != 0 && values[it] != solution[it] }
        if (wrong != null) {
            return Hint(wrong, solution[wrong], "This cell is wrong.")
        }
    }
    val candidates =
        values.indices.map { cell ->
            if (values[cell] != 0) {
                0
            } else {
                grid.peers[cell].fold(grid.allDigits) { mask, peer -> mask and digitBit(values[peer]).inv() }
            }
        }
    val single = candidates.indexOfFirst { Integer.bitCount(it) == 1 }
    if (single != -1) {
        val digit = Integer.numberOfTrailingZeros(candidates[single])
        return Hint(single, digit, "Only $digit fits here.")
    }
    listOf("row" to grid.rows, "column" to grid.cols, "box" to grid.boxes).forEach { (name, houses) ->
        houses.forEach { house ->
            (1..grid.size).forEach { digit ->
                val places = house.filter { candidates[it] and digitBit(digit) != 0 }
                if (places.size == 1) {
                    return Hint(places.single(), digit, "$digit can only go here in this $name.")
                }
            }
        }
    }
    return fewestCandidatesHint()
}

fun GameState.fewestCandidatesHint(): Hint? {
    val grid = puzzle.grid
    val solution = puzzle.solution ?: return null
    val cell =
        values.indices
            .filter { values[it] == 0 }
            .minByOrNull { Integer.bitCount(candidatesOf(it)) } ?: return null
    val digits = (1..grid.size).filter { candidatesOf(cell) and digitBit(it) != 0 }
    return Hint(cell, solution[cell], "This cell can only be ${digits.spoken()}: everything else is in its row, column or box.")
}

private fun GameState.candidatesOf(cell: Int): Int =
    puzzle.grid.peers[cell].fold(puzzle.grid.allDigits) { mask, peer -> mask and digitBit(values[peer]).inv() }

private fun List<Int>.spoken(): String =
    if (size <= 1) {
        joinToString("")
    } else {
        dropLast(1).joinToString(", ") + " or " + last()
    }
