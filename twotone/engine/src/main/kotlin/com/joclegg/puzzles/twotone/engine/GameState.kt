package com.joclegg.puzzles.twotone.engine

data class Change(
    val cell: Int,
    val from: Tile,
    val to: Tile,
)

object ChangeCodec {
    fun encode(changes: List<Change>) = changes.joinToString(",") { "${it.cell}${it.from.code}${it.to.code}" }

    fun decode(text: String): List<Change> =
        text.split(',').filter { it.isNotEmpty() }.map {
            Change(it.dropLast(2).toInt(), Tile.of(it[it.length - 2]), Tile.of(it.last()))
        }
}

sealed interface Hint {
    val cells: List<Int>

    data class Mistake(
        override val cells: List<Int>,
    ) : Hint

    data class Next(
        val deduction: Deduction,
        override val cells: List<Int>,
    ) : Hint
}

data class GameState(
    val puzzle: Puzzle,
    val grid: Grid = puzzle.givens,
    val changes: List<Change> = emptyList(),
    val hint: Hint? = null,
) {
    val isSolved: Boolean get() = grid == puzzle.solution

    val progressPercent: Int
        get() {
            val open = puzzle.givens.tiles.count { it == Tile.EMPTY }
            val filled = grid.tiles.indices.count { !puzzle.isGiven(it) && grid.tiles[it] != Tile.EMPTY }
            return if (open == 0) {
                PERCENT
            } else {
                filled * PERCENT / open
            }
        }

    val brokenCells: Set<Int> get() = grid.brokenCells()

    fun tap(cell: Int): GameState = set(cell) { it.next }

    fun paint(
        cell: Int,
        colour: Tile,
    ): GameState =
        set(cell) { current ->
            if (current == colour) {
                Tile.EMPTY
            } else {
                colour
            }
        }

    private fun set(
        cell: Int,
        change: (Tile) -> Tile,
    ): GameState {
        if (isSolved || puzzle.isGiven(cell)) {
            return copy(hint = null)
        }
        val from = grid.tiles[cell]
        val to = change(from)
        if (to == from) {
            return copy(hint = null)
        }
        return copy(grid = grid.with(cell, to), changes = changes + Change(cell, from, to), hint = null)
    }

    fun undo(): GameState {
        val last = changes.lastOrNull()
        if (isSolved || last == null) {
            return copy(hint = null)
        }
        return copy(grid = grid.with(last.cell, last.from), changes = changes.dropLast(1), hint = null)
    }

    fun restart(): GameState = GameState(puzzle)

    fun withHint(): GameState {
        if (isSolved) {
            return this
        }
        val mistakes = grid.tiles.indices.filter { grid.tiles[it] != Tile.EMPTY && grid.tiles[it] != puzzle.solution.tiles[it] }
        if (mistakes.isNotEmpty()) {
            return copy(hint = Hint.Mistake(mistakes))
        }
        val deduction = Solver.next(grid) ?: return copy(hint = null)
        return copy(hint = Hint.Next(deduction, grid.cellsOf(deduction.line)))
    }

    companion object {
        private const val PERCENT = 100

        fun replay(
            puzzle: Puzzle,
            changes: List<Change>,
        ): GameState = GameState(puzzle, changes.fold(puzzle.givens) { grid, change -> grid.with(change.cell, change.to) }, changes)
    }
}

data class GameTimer(
    val accumulatedMs: Long = 0,
    val runningSinceMs: Long? = null,
) {
    fun start(nowMs: Long) =
        if (runningSinceMs != null) {
            this
        } else {
            copy(runningSinceMs = nowMs)
        }

    fun pause(nowMs: Long) = runningSinceMs?.let { GameTimer(accumulatedMs + (nowMs - it), null) } ?: this

    fun elapsedMs(nowMs: Long) = accumulatedMs + (runningSinceMs?.let { nowMs - it } ?: 0)
}
