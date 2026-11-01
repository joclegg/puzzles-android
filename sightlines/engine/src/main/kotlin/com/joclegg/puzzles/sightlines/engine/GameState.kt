package com.joclegg.puzzles.sightlines.engine

data class Change(
    val cell: Int,
    val from: Dot,
    val to: Dot,
)

object ChangeCodec {
    fun encode(changes: List<Change>) = changes.joinToString(",") { "${it.cell}${it.from.code}${it.to.code}" }

    fun decode(text: String): List<Change> =
        text.split(',').filter { it.isNotEmpty() }.map {
            Change(it.dropLast(2).toInt(), Dot.of(it[it.length - 2]), Dot.of(it.last()))
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
            val open = grid.dots.indices.count { !puzzle.isGiven(it) }
            val filled = grid.dots.indices.count { !puzzle.isGiven(it) && grid.dots[it] != Dot.EMPTY }
            return if (open == 0) {
                PERCENT
            } else {
                filled * PERCENT / open
            }
        }

    val brokenCells: Set<Int> get() = puzzle.brokenCells(grid)

    fun tap(cell: Int): GameState = set(cell) { it.next }

    fun paint(
        cell: Int,
        colour: Dot,
    ): GameState =
        set(cell) { current ->
            if (current == colour) {
                Dot.EMPTY
            } else {
                colour
            }
        }

    private fun set(
        cell: Int,
        change: (Dot) -> Dot,
    ): GameState {
        if (isSolved || puzzle.isGiven(cell)) {
            return copy(hint = null)
        }
        val from = grid.dots[cell]
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

    fun revealSolution(): GameState = copy(grid = puzzle.solution, hint = null)

    fun withHint(): GameState {
        if (isSolved) {
            return this
        }
        val mistakes = grid.dots.indices.filter { grid.dots[it] != Dot.EMPTY && grid.dots[it] != puzzle.solution.dots[it] }
        if (mistakes.isNotEmpty()) {
            return copy(hint = Hint.Mistake(mistakes))
        }
        val deduction = Solver.next(grid, puzzle.numbers) ?: return copy(hint = null)
        val sight = Direction.entries.flatMap { grid.ray(deduction.source, it) } + deduction.source
        return copy(hint = Hint.Next(deduction, sight))
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
