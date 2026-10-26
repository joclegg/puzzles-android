package com.joclegg.puzzles.sudoku.engine

enum class InputMode { CELL_FIRST, DIGIT_FIRST }

enum class Highlight { SELECTED, SAME_DIGIT, NOTE_MATCH, PEER, NONE }

enum class HighlightStyle { CELL, NUMBER, OFF }

data class CellChange(
    val cell: Int,
    val value: Int,
    val notes: Int,
)

typealias Move = List<CellChange>

object MoveCodec {
    fun encode(moves: List<Move>): String =
        moves.joinToString(";") { move -> move.joinToString(",") { "${it.cell}:${it.value}:${it.notes}" } }

    fun decode(text: String): List<Move> =
        text.split(';').filter { it.isNotEmpty() }.map { move ->
            move.split(',').map { change ->
                val (cell, value, notes) = change.split(':').map(String::toInt)
                CellChange(cell, value, notes)
            }
        }
}

enum class MistakeCheck { OFF, BLOCK_CLASHES, SHOW_WRONG }

data class Rejection(
    val cell: Int,
    val clashes: Set<Int>,
)

data class PlayOptions(
    val highlightHouses: Boolean = true,
    val highlightCage: Boolean = true,
    val sameNumberStyle: HighlightStyle = HighlightStyle.CELL,
    val notesStyle: HighlightStyle = HighlightStyle.CELL,
    val autoRemoveNotes: Boolean = true,
    val highlightLastPlaced: Boolean = true,
)

data class GameState(
    val puzzle: Puzzle,
    val values: List<Int> = puzzle.givens,
    val notes: List<Int> = List(puzzle.givens.size) { 0 },
    val selected: Int? = null,
    val activeDigit: Int? = null,
    val inputMode: InputMode = InputMode.CELL_FIRST,
    val notesMode: Boolean = false,
    val mistakeCheck: MistakeCheck = MistakeCheck.SHOW_WRONG,
    val mistakes: Int = 0,
    val undoStack: List<Move> = emptyList(),
    val redoStack: List<Move> = emptyList(),
    val options: PlayOptions = PlayOptions(),
    val rejected: Rejection? = null,
    val eraserActive: Boolean = false,
    val lastPlaced: Int? = null,
    val marked: Set<Int> = emptySet(),
    val markActive: Boolean = false,
) {
    val effectiveMistakeCheck: MistakeCheck
        get() =
            if (mistakeCheck == MistakeCheck.SHOW_WRONG && puzzle.solution == null) {
                MistakeCheck.BLOCK_CLASHES
            } else {
                mistakeCheck
            }

    val checking get() = effectiveMistakeCheck == MistakeCheck.SHOW_WRONG

    val countsMistakes get() = effectiveMistakeCheck != MistakeCheck.OFF

    val isComplete: Boolean
        get() =
            values.none { it == 0 } &&
                (puzzle.solution?.let { it == values } ?: conflicts(values, puzzle.cages).isEmpty())

    fun isGiven(cell: Int) = puzzle.givens[cell] != 0

    fun isWrong(cell: Int): Boolean {
        val solution = puzzle.solution ?: return false
        return checking && !isGiven(cell) && values[cell] != 0 && values[cell] != solution[cell]
    }

    fun isCageWrong(cage: Cage): Boolean {
        if (!checking || cage.cells.any { values[it] == 0 }) {
            return false
        }
        val digits = cage.cells.map { values[it] }
        return digits.sum() != cage.sum || digits.toSet().size != digits.size
    }

    val highlightDigit: Int?
        get() =
            when (inputMode) {
                InputMode.DIGIT_FIRST -> activeDigit
                InputMode.CELL_FIRST -> selected?.let { values[it] }?.takeIf { it != 0 }
            }

    fun highlight(cell: Int): Highlight {
        val digit = highlightDigit
        val shownSelection = selected?.takeUnless { !options.highlightLastPlaced && it == lastPlaced }
        return when {
            cell == shownSelection -> {
                Highlight.SELECTED
            }

            options.sameNumberStyle == HighlightStyle.CELL && digit != null && values[cell] == digit -> {
                Highlight.SAME_DIGIT
            }

            options.notesStyle == HighlightStyle.CELL && digit != null && values[cell] == 0 && notes[cell] and digitBit(digit) != 0 -> {
                Highlight.NOTE_MATCH
            }

            shownSelection != null && isHighlightedPeer(shownSelection, cell) -> {
                Highlight.PEER
            }

            else -> {
                Highlight.NONE
            }
        }
    }

    fun isNumberHighlighted(cell: Int): Boolean =
        options.sameNumberStyle == HighlightStyle.NUMBER && highlightDigit != null && values[cell] == highlightDigit

    val noteHighlightDigit: Int?
        get() = highlightDigit.takeIf { options.notesStyle == HighlightStyle.NUMBER }

    fun digitCount(digit: Int): Int = values.count { it == digit }

    fun isDigitDone(digit: Int): Boolean = values.indices.count { values[it] == digit && !isWrong(it) } >= puzzle.grid.size

    fun tapCell(cell: Int): GameState {
        if (isComplete) {
            return this
        }
        val next = copy(selected = cell, rejected = null)
        val digit = activeDigit
        return when {
            inputMode != InputMode.DIGIT_FIRST -> next
            markActive -> next.toggleMark(cell)
            eraserActive -> next.clear(cell)
            digit != null -> next.enter(cell, digit)
            else -> next
        }
    }

    fun tapDigit(digit: Int): GameState {
        if (isComplete) {
            return this
        }
        return when (inputMode) {
            InputMode.DIGIT_FIRST -> {
                copy(
                    markActive = false,
                    activeDigit =
                        if (activeDigit == digit) {
                            null
                        } else {
                            digit
                        },
                    eraserActive = false,
                    rejected = null,
                )
            }

            InputMode.CELL_FIRST -> {
                selected?.let { copy(rejected = null).enter(it, digit) } ?: this
            }
        }
    }

    fun erase(): GameState {
        if (isComplete) {
            return this
        }
        if (inputMode == InputMode.DIGIT_FIRST) {
            return copy(eraserActive = !eraserActive, markActive = false, activeDigit = null, rejected = null)
        }
        return selected?.let(::clear) ?: this
    }

    fun mark(): GameState {
        if (isComplete) {
            return this
        }
        if (inputMode == InputMode.DIGIT_FIRST) {
            return copy(markActive = !markActive, eraserActive = false, activeDigit = null, rejected = null)
        }
        return selected?.let(::toggleMark) ?: this
    }

    private fun toggleMark(cell: Int) =
        copy(
            marked =
                if (cell in marked) {
                    marked - cell
                } else {
                    marked + cell
                },
        )

    private fun clear(cell: Int): GameState {
        if (isGiven(cell) || (values[cell] == 0 && notes[cell] == 0)) {
            return this
        }
        return record(copy(values = values.with(cell, 0), notes = notes.with(cell, 0), rejected = null))
    }

    fun undo(): GameState {
        if (isComplete) {
            return this
        }
        val last = undoStack.lastOrNull() ?: return this
        return applied(last).copy(undoStack = undoStack.dropLast(1), redoStack = redoStack + listOf(snapshot(last)))
    }

    fun redo(): GameState {
        if (isComplete) {
            return this
        }
        val next = redoStack.lastOrNull() ?: return this
        return applied(next).copy(undoStack = undoStack + listOf(snapshot(next)), redoStack = redoStack.dropLast(1))
    }

    private fun snapshot(move: Move): Move = move.map { CellChange(it.cell, values[it.cell], notes[it.cell]) }

    private fun applied(move: Move): GameState {
        val restoredValues = values.toMutableList()
        val restoredNotes = notes.toMutableList()
        for (change in move) {
            restoredValues[change.cell] = change.value
            restoredNotes[change.cell] = change.notes
        }
        return copy(values = restoredValues, notes = restoredNotes, rejected = null, lastPlaced = null)
    }

    fun restart(): GameState =
        GameState(
            puzzle = puzzle,
            inputMode = inputMode,
            mistakeCheck = mistakeCheck,
            options = options,
        )

    fun revealSolution(): GameState {
        val solution = puzzle.solution ?: return this
        return copy(values = solution, notes = notes.map { 0 }, selected = null, activeDigit = null, rejected = null)
    }

    fun toggleNotesMode() = copy(notesMode = !notesMode)

    fun withInputMode(mode: InputMode) = copy(inputMode = mode, activeDigit = null, eraserActive = false)

    private fun enter(
        cell: Int,
        digit: Int,
    ): GameState {
        if (isGiven(cell)) {
            return this
        }
        if (notesMode) {
            if (values[cell] != 0) {
                return this
            }
            return record(copy(notes = notes.with(cell, notes[cell] xor digitBit(digit)), lastPlaced = cell))
        }
        if (values[cell] == digit) {
            return record(copy(values = values.with(cell, 0), lastPlaced = null))
        }
        if (effectiveMistakeCheck == MistakeCheck.BLOCK_CLASHES) {
            val clashes = values.indices.filter { isPeer(cell, it) && values[it] == digit }.toSet()
            if (clashes.isNotEmpty()) {
                return copy(rejected = Rejection(cell, clashes), mistakes = mistakes + 1)
            }
        }
        val wrong = checking && puzzle.solution!![cell] != digit
        val clearBit = digitBit(digit).inv()
        val cleaned =
            notes.mapIndexed { i, mask ->
                if (i == cell) {
                    0
                } else if (options.autoRemoveNotes && isPeer(cell, i)) {
                    mask and clearBit
                } else {
                    mask
                }
            }
        return record(
            copy(
                values = values.with(cell, digit),
                notes = cleaned,
                mistakes =
                    mistakes +
                        if (wrong) {
                            1
                        } else {
                            0
                        },
                lastPlaced = cell,
            ),
        )
    }

    private fun record(next: GameState): GameState {
        val changed = values.indices.filter { values[it] != next.values[it] || notes[it] != next.notes[it] }
        return next.copy(undoStack = undoStack + listOf(changed.map { CellChange(it, values[it], notes[it]) }), redoStack = emptyList())
    }

    private fun isPeer(
        a: Int,
        b: Int,
    ): Boolean = a != b && (sharesHouse(a, b) || sharesCage(a, b))

    private fun isHighlightedPeer(
        a: Int,
        b: Int,
    ): Boolean = a != b && ((options.highlightHouses && sharesHouse(a, b)) || (options.highlightCage && sharesCage(a, b)))

    private fun sharesHouse(
        a: Int,
        b: Int,
    ) = puzzle.grid.sharesHouse(a, b)

    private fun sharesCage(
        a: Int,
        b: Int,
    ) = puzzle.cageIndexByCell[a] != NO_CAGE && puzzle.cageIndexByCell[a] == puzzle.cageIndexByCell[b]
}

private fun List<Int>.with(
    index: Int,
    value: Int,
): List<Int> = toMutableList().also { it[index] = value }

fun conflicts(
    values: List<Int>,
    cages: List<Cage> = emptyList(),
): Set<Int> {
    val bad = mutableSetOf<Int>()
    val groups = Grid.forCellCount(values.size).houses.map { it.toList() } + cages.map { it.cells }
    for (group in groups) {
        group
            .filter { values[it] != 0 }
            .groupBy { values[it] }
            .values
            .filter { it.size > 1 }
            .forEach { bad += it }
    }
    for (cage in cages) {
        val filled = cage.cells.filter { values[it] != 0 }
        val total = filled.sumOf { values[it] }
        if (total > cage.sum || (filled.size == cage.cells.size && total != cage.sum)) {
            bad += filled
        }
    }
    return bad
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
