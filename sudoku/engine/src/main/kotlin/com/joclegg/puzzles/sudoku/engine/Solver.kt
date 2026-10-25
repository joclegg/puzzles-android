package com.joclegg.puzzles.sudoku.engine

import kotlin.random.Random

object CageCombos {
    private val table: Array<Array<IntArray>> =
        Array(10) { size ->
            Array(46) { sum ->
                (0 until 1024 step 2)
                    .filter { mask -> Integer.bitCount(mask) == size && digitsOf(mask).sum() == sum }
                    .toIntArray()
            }
        }

    private val byAllowedDigits = mutableMapOf<Int, Array<Array<IntArray>>>()

    fun of(
        size: Int,
        sum: Int,
        allowedDigits: Int = Grid.STANDARD.allDigits,
    ): IntArray {
        if (size !in 0..9 || sum !in 0..45) {
            return IntArray(0)
        }
        if (allowedDigits == Grid.STANDARD.allDigits) {
            return table[size][sum]
        }
        val restricted =
            synchronized(byAllowedDigits) {
                byAllowedDigits.getOrPut(allowedDigits) {
                    Array(table.size) { s -> Array(table[s].size) { t -> table[s][t].filter { it and allowedDigits == it }.toIntArray() } }
                }
            }
        return restricted[size][sum]
    }
}

class Solver(
    givens: List<Int>,
    private val cages: List<Cage> = emptyList(),
) {
    private val grid = Grid.forCellCount(givens.size)
    private val values = IntArray(grid.cells)
    private val rowUsed = IntArray(grid.size)
    private val colUsed = IntArray(grid.size)
    private val boxUsed = IntArray(grid.size)
    private val cageOf = IntArray(grid.cells) { NO_CAGE }
    private val cageUsed = IntArray(cages.size)
    private val cageRemaining = IntArray(cages.size) { cages[it].sum }
    private val cageEmpty = IntArray(cages.size) { cages[it].cells.size }

    val consistent: Boolean

    init {
        var ok = true
        cages.forEachIndexed { index, cage ->
            cage.cells.forEach { cell ->
                if (cageOf[cell] != NO_CAGE) {
                    ok = false
                }
                cageOf[cell] = index
            }
        }
        for (cell in 0 until grid.cells) {
            val digit = givens[cell]
            if (digit == 0) {
                continue
            }
            if (candidates(cell) and digitBit(digit) == 0) {
                ok = false
            }
            place(cell, digit)
        }
        consistent = ok && cages.indices.all { cageRemaining[it] >= 0 && (cageEmpty[it] > 0 || cageRemaining[it] == 0) }
    }

    fun countSolutions(limit: Int = 2): Int = countSolutionsWithin(limit, Long.MAX_VALUE)!!

    fun countSolutionsWithin(
        limit: Int,
        maxSteps: Long,
    ): Int? {
        if (!consistent) {
            return 0
        }
        stepsLeft = maxSteps
        var count = 0
        search(null) {
            count++
            count >= limit
        }
        return if (stepsLeft < 0) {
            null
        } else {
            count
        }
    }

    private var stepsLeft = Long.MAX_VALUE

    fun solve(random: Random? = null): List<Int>? {
        if (!consistent) {
            return null
        }
        var result: List<Int>? = null
        search(random) {
            result = values.toList()
            true
        }
        return result
    }

    private fun search(
        random: Random?,
        onSolution: () -> Boolean,
    ): Boolean {
        if (--stepsLeft < 0) {
            return true
        }
        var best = -1
        var bestMask = 0
        var bestCount = 10
        for (cell in 0 until grid.cells) {
            if (values[cell] != 0) {
                continue
            }
            val mask = candidates(cell)
            val count = Integer.bitCount(mask)
            if (count == 0) {
                return false
            }
            if (count < bestCount) {
                best = cell
                bestMask = mask
                bestCount = count
                if (count == 1) {
                    break
                }
            }
        }
        if (best == -1) {
            return onSolution()
        }
        val digits =
            digitsOf(bestMask).let {
                if (random != null) {
                    it.shuffled(random)
                } else {
                    it
                }
            }
        for (digit in digits) {
            place(best, digit)
            val stop = search(random, onSolution)
            unplace(best, digit)
            if (stop) {
                return true
            }
        }
        return false
    }

    private fun candidates(cell: Int): Int {
        var mask = grid.allDigits and (rowUsed[grid.rowOf(cell)] or colUsed[grid.colOf(cell)] or boxUsed[grid.boxOf(cell)]).inv()
        val cage = cageOf[cell]
        if (cage != NO_CAGE) {
            var allowed = 0
            for (combo in CageCombos.of(cageEmpty[cage], cageRemaining[cage], grid.allDigits)) {
                if (combo and cageUsed[cage] == 0) {
                    allowed = allowed or combo
                }
            }
            mask = mask and allowed
        }
        return mask
    }

    private fun place(
        cell: Int,
        digit: Int,
    ) {
        val bit = digitBit(digit)
        values[cell] = digit
        rowUsed[grid.rowOf(cell)] = rowUsed[grid.rowOf(cell)] or bit
        colUsed[grid.colOf(cell)] = colUsed[grid.colOf(cell)] or bit
        boxUsed[grid.boxOf(cell)] = boxUsed[grid.boxOf(cell)] or bit
        val cage = cageOf[cell]
        if (cage != NO_CAGE) {
            cageUsed[cage] = cageUsed[cage] or bit
            cageRemaining[cage] -= digit
            cageEmpty[cage]--
        }
    }

    private fun unplace(
        cell: Int,
        digit: Int,
    ) {
        val bit = digitBit(digit).inv()
        values[cell] = 0
        rowUsed[grid.rowOf(cell)] = rowUsed[grid.rowOf(cell)] and bit
        colUsed[grid.colOf(cell)] = colUsed[grid.colOf(cell)] and bit
        boxUsed[grid.boxOf(cell)] = boxUsed[grid.boxOf(cell)] and bit
        val cage = cageOf[cell]
        if (cage != NO_CAGE) {
            cageUsed[cage] = cageUsed[cage] and bit
            cageRemaining[cage] += digit
            cageEmpty[cage]++
        }
    }

    companion object {
        fun randomGrid(
            random: Random,
            grid: Grid = Grid.STANDARD,
        ): List<Int> = Solver(grid.emptyValues()).solve(random)!!
    }
}
