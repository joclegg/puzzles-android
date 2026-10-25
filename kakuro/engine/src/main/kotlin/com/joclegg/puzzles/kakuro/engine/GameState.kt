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

enum class InputMode { CELL_FIRST, DIGIT_FIRST }

enum class MistakeCheck { OFF, BLOCK_CLASHES, SHOW_WRONG }

data class GameState(
    val puzzle: Puzzle,
    val values: List<Int> = startValues(puzzle),
    val notes: List<Int> = List(puzzle.solution.size) { 0 },
    val changes: List<Change> = emptyList(),
    val selected: Int? = null,
    val notesMode: Boolean = false,
    val inputMode: InputMode = InputMode.CELL_FIRST,
    val mistakeCheck: MistakeCheck = MistakeCheck.SHOW_WRONG,
    val activeDigit: Int? = null,
    val eraserActive: Boolean = false,
    val mistakes: Int = 0,
    val rejected: Int? = null,
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

    val shownWrongCells: Set<Int>
        get() =
            when (mistakeCheck) {
                MistakeCheck.SHOW_WRONG -> wrongCells + brokenRuns.flatMap { run -> run.cells.filter { values[it] != 0 } }
                MistakeCheck.BLOCK_CLASHES -> brokenRuns.flatMap { run -> run.cells.filter { values[it] != 0 } }.toSet()
                MistakeCheck.OFF -> emptySet()
            }

    fun isDigitDone(digit: Int): Boolean {
        val needed = puzzle.solution.count { it == digit }
        return needed > 0 && values.indices.count { values[it] == digit && values[it] == puzzle.solution[it] } == needed
    }

    fun tapCell(cell: Int): GameState {
        if (!puzzle.isWhite[cell]) {
            return this
        }
        val selectedHere = copy(selected = cell, rejected = null)
        if (inputMode == InputMode.CELL_FIRST || isSolved || isGiven(cell)) {
            return selectedHere
        }
        val digit = activeDigit
        return when {
            eraserActive -> selectedHere.set(cell, 0, 0)
            digit != null -> selectedHere.place(cell, digit)
            else -> selectedHere
        }
    }

    fun tapDigit(digit: Int): GameState {
        if (inputMode == InputMode.DIGIT_FIRST) {
            val next =
                if (activeDigit == digit) {
                    null
                } else {
                    digit
                }
            return copy(activeDigit = next, eraserActive = false, rejected = null)
        }
        val cell = selected ?: return this
        if (isSolved || isGiven(cell)) {
            return this
        }
        return copy(rejected = null).place(cell, digit)
    }

    fun erase(): GameState {
        if (inputMode == InputMode.DIGIT_FIRST) {
            return copy(eraserActive = !eraserActive, activeDigit = null, rejected = null)
        }
        val cell = selected ?: return this
        if (isSolved || isGiven(cell)) {
            return this
        }
        return set(cell, 0, 0)
    }

    fun withInputMode(mode: InputMode): GameState = copy(inputMode = mode, activeDigit = null, eraserActive = false, rejected = null)

    private fun place(
        cell: Int,
        digit: Int,
    ): GameState {
        if (notesMode) {
            return set(cell, 0, notes[cell] xor Combinations.bit(digit))
        }
        if (values[cell] == digit) {
            return set(cell, 0, notes[cell])
        }
        val clashes =
            puzzle.runsOf[cell]
                .orEmpty()
                .any { run -> run.cells.any { it != cell && values[it] == digit } }
        return when (mistakeCheck) {
            MistakeCheck.BLOCK_CLASHES -> {
                if (clashes) {
                    copy(rejected = cell, mistakes = mistakes + 1)
                } else {
                    set(cell, digit, 0)
                }
            }

            MistakeCheck.SHOW_WRONG -> {
                val placed = set(cell, digit, 0)
                if (digit != puzzle.solution[cell]) {
                    placed.copy(mistakes = mistakes + 1)
                } else {
                    placed
                }
            }

            MistakeCheck.OFF -> {
                set(cell, digit, 0)
            }
        }
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

    fun restart(): GameState = GameState(puzzle, inputMode = inputMode, mistakeCheck = mistakeCheck)

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
