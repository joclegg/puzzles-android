package com.joclegg.puzzles.crossword.engine

data class Change(
    val cell: Int,
    val from: Char,
    val to: Char,
)

object ChangeCodec {
    fun encode(changes: List<Change>) = changes.joinToString(",") { "${it.cell}${it.from}${it.to}" }

    fun decode(text: String): List<Change> =
        text.split(',').filter { it.length > 2 }.map {
            Change(it.dropLast(2).toInt(), it[it.length - 2], it[it.length - 1])
        }
}

enum class Arrow(
    val isAcross: Boolean,
    val step: Int,
) {
    LEFT(true, -1),
    RIGHT(true, 1),
    UP(false, -1),
    DOWN(false, 1),
}

data class GameState(
    val puzzle: Puzzle,
    val entries: String = emptyEntries(puzzle),
    val changes: List<Change> = emptyList(),
    val undone: List<Change> = emptyList(),
    val selected: Int? = firstOpen(puzzle, entries)?.let { word -> word.cells.firstOrNull { entries[it] == EMPTY } ?: word.cells.first() },
    val isAcross: Boolean = firstOpen(puzzle, entries)?.isAcross ?: true,
    val skipFilledCells: Boolean = true,
    val countMistakes: Boolean = false,
    val mistakes: Int = 0,
    val reveals: Int = 0,
    val jumpedFrom: Int? = null,
    val jumpedAcross: Boolean = true,
) {
    val isSolved: Boolean get() = entries == puzzle.solution

    val isFull: Boolean get() = EMPTY !in entries

    val wrongCells: Set<Int>
        get() = entries.indices.filter { entries[it] != EMPTY && entries[it] != puzzle.solution[it] }.toSet()

    val currentWord: Word?
        get() {
            val cell = selected ?: return null
            return puzzle.wordAt(cell, isAcross) ?: puzzle.wordAt(cell, !isAcross)
        }

    fun isWordFilled(word: Word) = word.cells.all { entries[it] != EMPTY }

    fun tapCell(cell: Int): GameState =
        if (jumpedFrom != null) {
            copy(jumpedFrom = null).tapCell(cell)
        } else {
            tapCellNow(cell)
        }

    private fun tapCellNow(cell: Int): GameState {
        if (!puzzle.isWhite[cell]) {
            return this
        }
        if (cell == selected) {
            return toggleDirection()
        }
        val direction =
            if (puzzle.wordAt(cell, isAcross) != null) {
                isAcross
            } else {
                !isAcross
            }
        return copy(selected = cell, isAcross = direction)
    }

    fun toggleDirection(): GameState =
        if (jumpedFrom != null) {
            copy(jumpedFrom = null).toggleDirection()
        } else {
            toggleDirectionNow()
        }

    private fun toggleDirectionNow(): GameState {
        val cell = selected ?: return this
        if (puzzle.wordAt(cell, !isAcross) == null) {
            return this
        }
        return copy(isAcross = !isAcross)
    }

    fun selectWord(word: Word): GameState = copy(selected = firstEmptyOrFirst(word), isAcross = word.isAcross, jumpedFrom = null)

    fun arrow(arrow: Arrow): GameState =
        if (jumpedFrom != null) {
            copy(jumpedFrom = null).arrow(arrow)
        } else {
            arrowNow(arrow)
        }

    private fun arrowNow(arrow: Arrow): GameState {
        val cell = selected ?: return this
        if (arrow.isAcross != isAcross && puzzle.wordAt(cell, arrow.isAcross) != null) {
            return copy(isAcross = arrow.isAcross)
        }
        val step =
            if (arrow.isAcross) {
                arrow.step
            } else {
                arrow.step * puzzle.size
            }
        var next = cell + step
        while (inLine(cell, next, arrow.isAcross) && !puzzle.isWhite[next]) {
            next += step
        }
        if (!inLine(cell, next, arrow.isAcross)) {
            return this
        }
        val direction =
            if (puzzle.wordAt(next, arrow.isAcross) != null) {
                arrow.isAcross
            } else {
                !arrow.isAcross
            }
        return copy(selected = next, isAcross = direction)
    }

    fun nextClue(): GameState = copy(jumpedFrom = null).moveClue(1)

    fun previousClue(): GameState = copy(jumpedFrom = null).moveClue(-1)

    fun type(letter: Char): GameState =
        if (jumpedFrom != null) {
            copy(jumpedFrom = null).type(letter)
        } else {
            typeNow(letter)
        }

    private fun typeNow(letter: Char): GameState {
        val cell = selected ?: return this
        val upper = letter.uppercaseChar()
        if (isSolved || upper !in 'A'..'Z') {
            return this
        }
        val word = currentWord ?: return this
        val wrong =
            if (countMistakes && upper != puzzle.solution[cell] && entries[cell] != upper) {
                1
            } else {
                0
            }
        val typed = record(listOf(Change(cell, entries[cell], upper))).copy(mistakes = mistakes + wrong, isAcross = word.isAcross)
        return typed.advanceAfterTyping(word, cell)
    }

    fun backspace(): GameState {
        val cell = selected ?: return this
        if (isSolved) {
            return this
        }
        val jumped = jumpedFrom
        if (jumped != null) {
            return copy(selected = jumped, isAcross = jumpedAcross, jumpedFrom = null).clear(jumped)
        }
        val word = currentWord ?: return this
        if (entries[cell] != EMPTY) {
            return clear(cell)
        }
        val index = word.cells.indexOf(cell)
        if (index > 0) {
            return copy(selected = word.cells[index - 1], isAcross = word.isAcross).clear(word.cells[index - 1])
        }
        val previous = puzzle.words[(puzzle.words.indexOf(word) - 1).mod(puzzle.words.size)]
        return copy(selected = previous.cells.last(), isAcross = previous.isAcross).clear(previous.cells.last())
    }

    fun revealLetter(): GameState =
        if (jumpedFrom != null) {
            copy(jumpedFrom = null).revealLetter()
        } else {
            revealLetterNow()
        }

    private fun revealLetterNow(): GameState {
        val cell = selected ?: return this
        return reveal(listOf(cell))
    }

    fun revealWord(): GameState =
        if (jumpedFrom != null) {
            copy(jumpedFrom = null).revealWord()
        } else {
            revealWordNow()
        }

    private fun revealWordNow(): GameState {
        val word = currentWord ?: return this
        return reveal(word.cells)
    }

    fun revealSolution(): GameState = copy(entries = puzzle.solution, undone = emptyList())

    fun undo(): GameState =
        if (jumpedFrom != null) {
            copy(jumpedFrom = null).undo()
        } else {
            undoNow()
        }

    private fun undoNow(): GameState {
        val last = changes.lastOrNull() ?: return this
        if (isSolved) {
            return this
        }
        return copy(
            entries = apply(listOf(Change(last.cell, last.to, last.from))),
            changes = changes.dropLast(1),
            undone = undone + last,
            selected = last.cell,
        ).keepDirection()
    }

    fun redo(): GameState =
        if (jumpedFrom != null) {
            copy(jumpedFrom = null).redo()
        } else {
            redoNow()
        }

    private fun redoNow(): GameState {
        val next = undone.lastOrNull() ?: return this
        if (isSolved) {
            return this
        }
        return copy(
            entries = apply(listOf(next)),
            changes = changes + next,
            undone = undone.dropLast(1),
            selected = next.cell,
        ).keepDirection()
    }

    fun restart(): GameState = GameState(puzzle, skipFilledCells = skipFilledCells, countMistakes = countMistakes)

    private fun reveal(cells: List<Int>): GameState {
        if (isSolved) {
            return this
        }
        val edits = cells.filter { entries[it] != puzzle.solution[it] }.map { Change(it, entries[it], puzzle.solution[it]) }
        if (edits.isEmpty()) {
            return this
        }
        return record(edits).copy(reveals = reveals + 1)
    }

    private fun advanceAfterTyping(
        word: Word,
        cell: Int,
    ): GameState {
        val after = word.cells.drop(word.cells.indexOf(cell) + 1)
        val nextCell =
            if (skipFilledCells) {
                after.firstOrNull { entries[it] == EMPTY }
            } else {
                after.firstOrNull()
            }
        if (nextCell != null) {
            return copy(selected = nextCell)
        }
        if (isWordFilled(word) && !isFull) {
            val start = puzzle.words.indexOf(word)
            val next =
                (1 until puzzle.words.size)
                    .map { puzzle.words[(start + it).mod(puzzle.words.size)] }
                    .firstOrNull { !isWordFilled(it) }
            if (next != null) {
                return selectWord(next).copy(jumpedFrom = cell, jumpedAcross = word.isAcross)
            }
        }
        return copy(selected = word.cells.last())
    }

    private fun moveClue(step: Int): GameState {
        val word = currentWord ?: return this
        val next = puzzle.words[(puzzle.words.indexOf(word) + step).mod(puzzle.words.size)]
        return selectWord(next)
    }

    private fun firstEmptyOrFirst(word: Word) = word.cells.firstOrNull { entries[it] == EMPTY } ?: word.cells.first()

    private fun clear(cell: Int): GameState =
        if (entries[cell] == EMPTY) {
            this
        } else {
            record(listOf(Change(cell, entries[cell], EMPTY)))
        }

    private fun record(edits: List<Change>): GameState {
        val real = edits.filter { it.from != it.to }
        if (real.isEmpty()) {
            return this
        }
        return copy(entries = apply(real), changes = changes + real, undone = emptyList())
    }

    private fun apply(edits: List<Change>): String {
        val chars = entries.toCharArray()
        edits.forEach { chars[it.cell] = it.to }
        return String(chars)
    }

    private fun keepDirection(): GameState {
        val cell = selected ?: return this
        return if (puzzle.wordAt(cell, isAcross) == null) {
            copy(isAcross = !isAcross)
        } else {
            this
        }
    }

    private fun inLine(
        from: Int,
        to: Int,
        isAcross: Boolean,
    ): Boolean =
        if (isAcross) {
            to >= 0 && to < puzzle.solution.length && to / puzzle.size == from / puzzle.size
        } else {
            to >= 0 && to < puzzle.solution.length
        }

    companion object {
        fun firstOpen(
            puzzle: Puzzle,
            entries: String,
        ): Word? = puzzle.words.firstOrNull { word -> word.cells.any { entries[it] == EMPTY } } ?: puzzle.words.firstOrNull()

        fun emptyEntries(puzzle: Puzzle) =
            puzzle.solution
                .map {
                    if (it == BLOCK) {
                        BLOCK
                    } else {
                        EMPTY
                    }
                }.joinToString("")

        fun replay(
            puzzle: Puzzle,
            changes: List<Change>,
        ): GameState =
            changes.fold(GameState(puzzle)) { state, change ->
                state.copy(entries = state.apply(listOf(change)), changes = state.changes + change)
            }

        fun restore(
            puzzle: Puzzle,
            entries: String,
            changes: List<Change>,
        ): GameState =
            if (changes.isEmpty() && entries.length == puzzle.solution.length) {
                GameState(puzzle, entries = entries)
            } else {
                replay(puzzle, changes).let { replayed ->
                    val fresh = GameState(puzzle, replayed.entries)
                    replayed.copy(selected = fresh.selected, isAcross = fresh.isAcross)
                }
            }
    }
}
