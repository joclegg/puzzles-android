package com.joclegg.puzzles.twotone.engine

import kotlin.random.Random

data class Puzzle(
    val givens: Grid,
    val solution: Grid,
) {
    val size: Int get() = givens.size

    fun isGiven(cell: Int) = givens.tiles[cell] != Tile.EMPTY

    fun encode() = "$size;${givens.encode()};${solution.encode()}"

    companion object {
        fun decode(text: String): Puzzle {
            val (size, givens, solution) = text.split(';')
            return Puzzle(Grid.parse(size.toInt(), givens), Grid.parse(size.toInt(), solution))
        }
    }
}

object Generator {
    fun generate(
        size: Int,
        seed: Long,
    ): Puzzle {
        val random = Random(seed)
        val solution = fill(Grid.empty(size), 0, random) ?: error("No $size x $size grid could be filled")
        var givens = solution
        for (cell in solution.tiles.indices.shuffled(random)) {
            val without = givens.with(cell, Tile.EMPTY)
            if (Solver.solveLogically(without) == solution) {
                givens = without
            }
        }
        return Puzzle(givens, solution)
    }

    private fun fill(
        grid: Grid,
        cell: Int,
        random: Random,
    ): Grid? {
        if (cell == grid.tiles.size) {
            return grid
        }
        val options = listOf(Tile.RED, Tile.BLUE).shuffled(random)
        for (tile in options) {
            val next = grid.with(cell, tile)
            if (fits(next, cell)) {
                fill(next, cell + 1, random)?.let { return it }
            }
        }
        return null
    }

    private fun fits(
        grid: Grid,
        cell: Int,
    ): Boolean {
        val row = Line(true, cell / grid.size)
        val column = Line(false, cell % grid.size)
        return listOf(row, column).all { line ->
            val tiles = grid.tilesOf(line)
            val noThree =
                (0..grid.size - Grid.RUN).none { start ->
                    val run = tiles.subList(start, start + Grid.RUN)
                    run[0] != Tile.EMPTY && run.all { it == run[0] }
                }
            val balanced = listOf(Tile.RED, Tile.BLUE).all { tile -> tiles.count { it == tile } <= grid.size / 2 }
            val unique =
                Tile.EMPTY in tiles ||
                    grid.lines.none { it != line && it.isRow == line.isRow && grid.tilesOf(it) == tiles }
            noThree && balanced && unique
        }
    }
}
