package com.joclegg.puzzles.sightlines.engine

enum class Reason {
    SATISFIED,
    TOO_MANY,
    NEEDS,
    ALONE,
}

data class Deduction(
    val cells: List<Int>,
    val dot: Dot,
    val reason: Reason,
    val source: Int,
)

object Solver {
    fun next(
        grid: Grid,
        numbers: Map<Int, Int>,
    ): Deduction? {
        val unnumbered = grid.dots.indices.filter { grid.dots[it] == Dot.BLUE && it !in numbers }
        val targets = numbers.toList() + unnumbered.map { it to 1 }
        return targets.firstNotNullOfOrNull { (cell, number) -> satisfied(grid, cell, number, cell in numbers) }
            ?: targets.firstNotNullOfOrNull { (cell, number) -> tooMany(grid, cell, number, cell in numbers) }
            ?: targets.firstNotNullOfOrNull { (cell, number) -> needs(grid, cell, number) }
            ?: alone(grid)
    }

    fun solveLogically(
        start: Grid,
        numbers: Map<Int, Int>,
    ): Grid? {
        var grid = start
        while (!grid.isFull) {
            val deduction = next(grid, numbers) ?: return null
            grid = deduction.cells.fold(grid) { next, cell -> next.with(cell, deduction.dot) }
        }
        return grid
    }

    private fun closingEnd(
        grid: Grid,
        cell: Int,
        direction: Direction,
    ): Int? =
        grid
            .ray(cell, direction)
            .drop(grid.seen(cell, direction))
            .firstOrNull()
            ?.takeIf { grid.dots[it] == Dot.EMPTY }

    private fun satisfied(
        grid: Grid,
        cell: Int,
        number: Int,
        exact: Boolean,
    ): Deduction? {
        if (!exact || grid.seen(cell) != number) {
            return null
        }
        val ends = Direction.entries.mapNotNull { closingEnd(grid, cell, it) }
        return ends.takeIf { it.isNotEmpty() }?.let { Deduction(it, Dot.RED, Reason.SATISFIED, cell) }
    }

    private fun tooMany(
        grid: Grid,
        cell: Int,
        number: Int,
        exact: Boolean,
    ): Deduction? {
        if (!exact) {
            return null
        }
        val seen = grid.seen(cell)
        for (direction in Direction.entries) {
            val end = closingEnd(grid, cell, direction) ?: continue
            val beyond = grid.ray(end, direction).takeWhile { grid.dots[it] == Dot.BLUE }.size
            if (seen + 1 + beyond > number) {
                return Deduction(listOf(end), Dot.RED, Reason.TOO_MANY, cell)
            }
        }
        return null
    }

    private fun needs(
        grid: Grid,
        cell: Int,
        number: Int,
    ): Deduction? {
        for (direction in Direction.entries) {
            val others = Direction.entries.filter { it != direction }.sumOf { grid.reach(cell, it) }
            if (number - others > grid.seen(cell, direction)) {
                val end = closingEnd(grid, cell, direction) ?: continue
                return Deduction(listOf(end), Dot.BLUE, Reason.NEEDS, cell)
            }
        }
        return null
    }

    private fun alone(grid: Grid): Deduction? =
        grid.dots.indices
            .firstOrNull { cell ->
                grid.dots[cell] == Dot.EMPTY && Direction.entries.all { grid.reach(cell, it) == 0 }
            }?.let { Deduction(listOf(it), Dot.RED, Reason.ALONE, it) }
}
