package com.joclegg.puzzles.kakuro.engine

import kotlin.random.Random

object Generator {
    private const val BLACK_CHANCE = 0.22
    private const val MAX_RUN = 7
    private const val MIN_WHITE_SHARE = 0.5
    private const val ATTEMPTS = 12
    private const val JITTER = 3.0

    fun generate(
        size: Int,
        seed: Long,
    ): Puzzle {
        val random = Random(seed)
        var best: Puzzle? = null
        var attempts = 0
        while (attempts < ATTEMPTS || best == null) {
            val layout = layout(size, random) ?: continue
            attempts++
            val filled = fill(layout, random) ?: continue
            val puzzle = withUniqueSolution(Puzzle(size, filled), best?.givens?.size)
            if (puzzle != null && (best == null || puzzle.givens.size < best.givens.size)) {
                best = puzzle
            }
            if (best?.givens?.isEmpty() == true) {
                break
            }
        }
        return best!!
    }

    private fun withUniqueSolution(
        puzzle: Puzzle,
        mustBeat: Int?,
    ): Puzzle? {
        var givens = emptySet<Int>()
        while (true) {
            if (mustBeat != null && givens.size >= mustBeat) {
                return null
            }
            val start =
                puzzle.solution.mapIndexed { cell, digit ->
                    if (cell in givens) {
                        digit
                    } else {
                        0
                    }
                }
            val alternative = Solver.solutions(puzzle.copy(givens = givens), start).firstOrNull { it != puzzle.solution } ?: break
            givens = givens + alternative.indices.first { alternative[it] != puzzle.solution[it] }
        }
        return puzzle.copy(givens = givens)
    }

    private fun layout(
        size: Int,
        random: Random,
    ): List<Boolean>? {
        val white = MutableList(size * size) { cell -> cell / size > 0 && cell % size > 0 }
        for (cell in white.indices) {
            if (white[cell] && random.nextDouble() < BLACK_CHANCE) {
                setBlack(white, size, cell)
            }
        }
        var changed = true
        while (changed) {
            changed = false
            for (cell in white.indices) {
                if (white[cell] &&
                    (runLength(white, size, cell, true) !in 2..MAX_RUN || runLength(white, size, cell, false) !in 2..MAX_RUN)
                ) {
                    setBlack(white, size, cell)
                    changed = true
                }
            }
        }
        val whites = white.indices.filter { white[it] }
        val enough = whites.size >= (size - 1) * (size - 1) * MIN_WHITE_SHARE
        return white.takeIf { enough && connected(white, size, whites) }
    }

    private fun setBlack(
        white: MutableList<Boolean>,
        size: Int,
        cell: Int,
    ) {
        white[cell] = false
        val row = cell / size
        val col = cell % size
        white[(size - row) * size + (size - col)] = false
    }

    private fun runLength(
        white: List<Boolean>,
        size: Int,
        cell: Int,
        across: Boolean,
    ): Int {
        val step =
            if (across) {
                1
            } else {
                size
            }

        fun sameLine(other: Int) = !across || other / size == cell / size
        var length = 1
        var next = cell - step
        while (next >= 0 && sameLine(next) && white[next]) {
            length++
            next -= step
        }
        next = cell + step
        while (next < white.size && sameLine(next) && white[next]) {
            length++
            next += step
        }
        return length
    }

    private fun connected(
        white: List<Boolean>,
        size: Int,
        whites: List<Int>,
    ): Boolean {
        val seen = mutableSetOf(whites.first())
        val queue = ArrayDeque(listOf(whites.first()))
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            val neighbours =
                listOfNotNull(
                    (cell - size).takeIf { it >= 0 },
                    (cell + size).takeIf { it < white.size },
                    (cell - 1).takeIf { cell % size > 0 },
                    (cell + 1).takeIf { cell % size < size - 1 },
                )
            neighbours.filter { white[it] && seen.add(it) }.forEach(queue::addLast)
        }
        return seen.size == whites.size
    }

    private fun fill(
        white: List<Boolean>,
        random: Random,
    ): List<Int>? {
        val size = Math.sqrt(white.size.toDouble()).toInt()
        val shape =
            Puzzle(
                size,
                white.map {
                    if (it) {
                        1
                    } else {
                        0
                    }
                },
            )
        val values = MutableList(white.size) { 0 }
        val cells = white.indices.filter { white[it] }
        val leansHigh = shape.runs.associateWith { random.nextBoolean() }
        return if (place(shape, values, cells, 0, random, leansHigh)) {
            values
        } else {
            null
        }
    }

    private fun place(
        shape: Puzzle,
        values: MutableList<Int>,
        cells: List<Int>,
        index: Int,
        random: Random,
        leansHigh: Map<Run, Boolean>,
    ): Boolean {
        if (index == cells.size) {
            return true
        }
        val cell = cells[index]
        val runs = shape.runsOf.getValue(cell)
        val used = runs.flatMap { run -> run.cells.map { values[it] } }.toSet()
        val high = runs.count { leansHigh.getValue(it) }
        val order =
            (1..MAX_DIGIT).sortedBy { digit ->
                val lean =
                    when (high) {
                        2 -> MAX_DIGIT - digit
                        0 -> digit
                        else -> MAX_DIGIT / 2
                    }
                lean + random.nextDouble() * JITTER
            }
        for (digit in order) {
            if (digit !in used) {
                values[cell] = digit
                if (place(shape, values, cells, index + 1, random, leansHigh)) {
                    return true
                }
            }
        }
        values[cell] = 0
        return false
    }
}
