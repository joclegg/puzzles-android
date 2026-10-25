package com.joclegg.puzzles.kakuro.engine

data class Change(
    val cell: Int,
    val fromValue: Int,
    val fromNotes: Int,
    val toValue: Int,
    val toNotes: Int,
)

object ChangeCodec {
    fun encode(changes: List<Change>) =
        changes.joinToString(";") { "${it.cell},${it.fromValue},${it.fromNotes},${it.toValue},${it.toNotes}" }

    fun decode(text: String): List<Change> =
        text.split(';').filter { it.isNotEmpty() }.map {
            val (cell, fromValue, fromNotes, toValue, toNotes) = it.split(',').map(String::toInt)
            Change(cell, fromValue, fromNotes, toValue, toNotes)
        }
}

data class GameState(
    val puzzle: Puzzle,
    val values: List<Int> = startValues(puzzle),
    val notes: List<Int> = List(puzzle.solution.size) { 0 },
    val changes: List<Change> = emptyList(),
    val selected: Int? = null,
    val notesMode: Boolean = false,
) {
    val isSolved: Boolean get() = values == puzzle.solution

    val wrongCells: Set<Int> get() = values.indices.filter { values[it] != 0 && values[it] != puzzle.solution[it] }.toSet()

    val brokenRuns: List<Run>
        get() =
            puzzle.runs.filter { run ->
                val placed = run.cells.map { values[it] }.filter { it != 0 }
                placed.size != placed.toSet().size || placed.sum() > run.sum || (placed.size == run.cells.size && placed.sum() != run.sum)
            }

    fun isGiven(cell: Int) = cell in puzzle.givens

    fun selectedCombinations(): List<Pair<Run, List<Int>>> {
        val cell = selected ?: return emptyList()
        return puzzle.runsOf[cell].orEmpty().map { run ->
            val placed = run.cells.filter { values[it] != 0 }.fold(0) { mask, other -> mask or Combinations.bit(values[other]) }
            run to Combinations.masks(run.cells.size, run.sum).filter { it and placed == placed }
        }
    }

    fun tapCell(cell: Int): GameState =
        if (puzzle.isWhite[cell]) {
            copy(selected = cell)
        } else {
            this
        }

    fun tapDigit(digit: Int): GameState {
        val cell = selected ?: return this
        if (isSolved || isGiven(cell)) {
            return this
        }
        return if (notesMode) {
            set(cell, 0, notes[cell] xor Combinations.bit(digit))
        } else if (values[cell] == digit) {
            set(cell, 0, notes[cell])
        } else {
            set(cell, digit, 0)
        }
    }

    fun erase(): GameState {
        val cell = selected ?: return this
        if (isSolved || isGiven(cell)) {
            return this
        }
        return set(cell, 0, 0)
    }

    fun toggleNotes(): GameState = copy(notesMode = !notesMode)

    fun undo(): GameState {
        val last = changes.lastOrNull() ?: return this
        if (isSolved) {
            return this
        }
        return copy(
            values = values.replaced(last.cell, last.fromValue),
            notes = notes.replaced(last.cell, last.fromNotes),
            changes = changes.dropLast(1),
            selected = last.cell,
        )
    }

    fun restart(): GameState = GameState(puzzle)

    private fun set(
        cell: Int,
        value: Int,
        cellNotes: Int,
    ): GameState {
        if (values[cell] == value && notes[cell] == cellNotes) {
            return this
        }
        val change = Change(cell, values[cell], notes[cell], value, cellNotes)
        return copy(values = values.replaced(cell, value), notes = notes.replaced(cell, cellNotes), changes = changes + change)
    }

    companion object {
        fun startValues(puzzle: Puzzle) =
            puzzle.solution.mapIndexed { cell, digit ->
                if (cell in puzzle.givens) {
                    digit
                } else {
                    0
                }
            }

        fun replay(
            puzzle: Puzzle,
            changes: List<Change>,
        ): GameState =
            changes.fold(GameState(puzzle)) { state, change ->
                state.copy(
                    values = state.values.replaced(change.cell, change.toValue),
                    notes = state.notes.replaced(change.cell, change.toNotes),
                    changes = state.changes + change,
                )
            }
    }
}

private fun List<Int>.replaced(
    index: Int,
    value: Int,
) = toMutableList().also { it[index] = value }

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
