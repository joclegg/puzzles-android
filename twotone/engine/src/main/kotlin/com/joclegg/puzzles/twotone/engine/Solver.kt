package com.joclegg.puzzles.twotone.engine

enum class Reason {
    PAIR,
    GAP,
    BALANCE,
    UNIQUE,
}

data class Deduction(
    val cells: List<Int>,
    val tile: Tile,
    val reason: Reason,
    val line: Line,
    val twin: Line? = null,
)

object Solver {
    fun next(grid: Grid): Deduction? =
        grid.lines.firstNotNullOfOrNull { pairOrGap(grid, it) } ?: grid.lines.firstNotNullOfOrNull { balance(grid, it) }
            ?: grid.lines.firstNotNullOfOrNull { unique(grid, it) }

    fun solveLogically(grid: Grid): Grid? {
        var current = grid
        while (!current.isFull) {
            val deduction = next(current) ?: return null
            current = deduction.cells.fold(current) { next, cell -> next.with(cell, deduction.tile) }
            if (current.brokenCells().isNotEmpty()) {
                return null
            }
        }
        return current
    }

    private fun pairOrGap(
        grid: Grid,
        line: Line,
    ): Deduction? {
        val cells = grid.cellsOf(line)
        val tiles = grid.tilesOf(line)
        for (start in 0..grid.size - Grid.RUN) {
            val (a, b, c) = tiles.subList(start, start + Grid.RUN)
            val target =
                when {
                    a == Tile.EMPTY && b != Tile.EMPTY && b == c -> Pair(start, Reason.PAIR)
                    c == Tile.EMPTY && a != Tile.EMPTY && a == b -> Pair(start + 2, Reason.PAIR)
                    b == Tile.EMPTY && a != Tile.EMPTY && a == c -> Pair(start + 1, Reason.GAP)
                    else -> null
                }
            if (target != null) {
                val filled = tiles.subList(start, start + Grid.RUN).first { it != Tile.EMPTY }
                return Deduction(listOf(cells[target.first]), filled.other, target.second, line)
            }
        }
        return null
    }

    private fun balance(
        grid: Grid,
        line: Line,
    ): Deduction? {
        val tiles = grid.tilesOf(line)
        if (Tile.EMPTY !in tiles) {
            return null
        }
        val full = listOf(Tile.RED, Tile.BLUE).firstOrNull { tile -> tiles.count { it == tile } == grid.size / 2 } ?: return null
        val empties = grid.cellsOf(line).filter { grid.tiles[it] == Tile.EMPTY }
        return Deduction(empties, full.other, Reason.BALANCE, line)
    }

    private fun unique(
        grid: Grid,
        line: Line,
    ): Deduction? {
        val cells = grid.cellsOf(line)
        val tiles = grid.tilesOf(line)
        val empties = cells.filter { grid.tiles[it] == Tile.EMPTY }
        if (empties.size != 2) {
            return null
        }
        val missingRed = grid.size / 2 - tiles.count { it == Tile.RED }
        if (missingRed != 1) {
            return null
        }
        val twins = grid.lines.filter { it != line && it.isRow == line.isRow && Tile.EMPTY !in grid.tilesOf(it) }
        for (twin in twins) {
            val twinTiles = grid.tilesOf(twin)
            val matchesFilled = tiles.indices.all { tiles[it] == Tile.EMPTY || tiles[it] == twinTiles[it] }
            if (matchesFilled) {
                val first = cells.indexOf(empties[0])
                return Deduction(listOf(empties[0]), twinTiles[first].other, Reason.UNIQUE, line, twin)
            }
        }
        return null
    }
}
