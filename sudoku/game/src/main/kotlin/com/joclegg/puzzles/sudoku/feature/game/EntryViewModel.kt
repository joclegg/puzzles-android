package com.joclegg.puzzles.sudoku.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.GameRepository
import com.joclegg.puzzles.sudoku.core.data.PuzzleRef
import com.joclegg.puzzles.sudoku.core.data.SettingsRepository
import com.joclegg.puzzles.sudoku.core.ui.BoardUi
import com.joclegg.puzzles.sudoku.core.ui.toBoardUi
import com.joclegg.puzzles.sudoku.engine.Cage
import com.joclegg.puzzles.sudoku.engine.CageBuilder
import com.joclegg.puzzles.sudoku.engine.CageProblem
import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.GameState
import com.joclegg.puzzles.sudoku.engine.Grader
import com.joclegg.puzzles.sudoku.engine.Grid
import com.joclegg.puzzles.sudoku.engine.Highlight
import com.joclegg.puzzles.sudoku.engine.InputMode
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import com.joclegg.puzzles.sudoku.engine.Puzzle
import com.joclegg.puzzles.sudoku.engine.PuzzleCodec
import com.joclegg.puzzles.sudoku.engine.Solver
import com.joclegg.puzzles.sudoku.engine.conflicts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EntryProblem { CLASH, NO_SOLUTION, MULTIPLE_SOLUTIONS, TOO_HARD_TO_CHECK }

data class EntryUiState(
    val mode: GameMode,
    val board: BoardUi,
    val inputMode: InputMode,
    val activeDigit: Int?,
    val givenCount: Int,
    val cageCount: Int = 0,
    val digitCounts: List<Int> = emptyList(),
    val cageTool: Boolean = false,
    val cageSumInput: String = "",
    val cageProblem: CageProblem? = null,
    val problem: EntryProblem? = null,
    val checking: Boolean = false,
) {
    val canFix get() = !checking && (givenCount > 0 || cageCount > 0)
}

class EntryViewModel(
    initialMode: GameMode,
    private val games: GameRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private var mode = initialMode
    private var entry = GameState(emptyPuzzle(), mistakeCheck = MistakeCheck.OFF)
    private var cages = CageBuilder()
    private var cageTool = false
    private var cageProblem: CageProblem? = null
    private var problem: EntryProblem? = null
    private var checking = false

    private val _uiState = MutableStateFlow(render())
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = settings.settings.first()
            entry = entry.copy(options = prefs.forMode(mode).playOptions).withInputMode(prefs.inputMode)
            settings.customDraft.first()?.let(::restoreDraft)
            publish()
        }
    }

    fun onModeChange(newMode: GameMode) {
        mode = newMode
        if (mode == GameMode.CLASSIC) {
            cageTool = false
        }
        problem = null
        saveDraft()
        publish()
    }

    fun onToggleCageTool() {
        cageTool = !cageTool
        cageProblem = null
        publish()
    }

    fun onCellClick(cell: Int) =
        if (cageTool) {
            editCages { it.tapCell(cell) }
        } else {
            edit { it.tapCell(cell) }
        }

    fun onDigitClick(digit: Int) =
        if (cageTool) {
            editCages { it.typeDigit(digit) }
        } else {
            edit { it.tapDigit(digit) }
        }

    fun onErase() =
        if (cageTool) {
            editCages { it.deleteSelection() }
        } else {
            edit { it.erase() }
        }

    fun onConfirmCage() {
        cageProblem = cages.problem()
        editCages { it.confirm() }
    }

    fun onClear() {
        cages = CageBuilder()
        edit { it.copy(values = Grid.STANDARD.emptyValues()) }
    }

    fun onDismissProblem() {
        problem = null
        publish()
    }

    suspend fun fix(playUnverified: Boolean = false): Long? {
        val givens = entry.values
        val cageList = activeCages()
        checking = true
        publish()
        val result = withContext(Dispatchers.Default) { check(givens, cageList) }
        checking = false
        if (result.problem == EntryProblem.CLASH || (result.problem != null && !playUnverified)) {
            problem = result.problem
            publish()
            return null
        }
        val ref = PuzzleRef("custom:${PuzzleCodec.encode(result.puzzle)}", mode, result.level, result.puzzle, custom = true)
        val game = games.start(ref, mistakeCheck = settings.settings.first().mistakeCheckFor(mode))
        settings.setCustomDraft(null)
        return game.id
    }

    private class CheckResult(
        val puzzle: Puzzle,
        val problem: EntryProblem? = null,
        val level: Difficulty? = null,
    )

    private fun check(
        givens: List<Int>,
        cageList: List<Cage>,
    ): CheckResult {
        val unverified = Puzzle(givens, solution = null, cages = cageList)
        val solver = Solver(givens, cageList)
        if (!solver.consistent) {
            return CheckResult(unverified, EntryProblem.CLASH)
        }
        return when (solver.countSolutionsWithin(limit = 2, maxSteps = MAX_SOLVER_STEPS)) {
            0 -> {
                CheckResult(unverified, EntryProblem.NO_SOLUTION)
            }

            1 -> {
                val solved = Puzzle(givens, Solver(givens, cageList).solve(), cageList)
                CheckResult(solved, level = Grader(givens, cageList).grade().level)
            }

            null -> {
                CheckResult(unverified, EntryProblem.TOO_HARD_TO_CHECK)
            }

            else -> {
                CheckResult(unverified, EntryProblem.MULTIPLE_SOLUTIONS)
            }
        }
    }

    private fun activeCages() =
        if (mode == GameMode.KILLER) {
            cages.cages
        } else {
            emptyList()
        }

    private fun edit(change: (GameState) -> GameState) {
        val next = change(entry)
        if (next == entry) {
            return
        }
        val valuesChanged = next.values != entry.values
        entry = next
        problem = null
        if (valuesChanged) {
            saveDraft()
        }
        publish()
    }

    private fun editCages(change: (CageBuilder) -> CageBuilder) {
        val next = change(cages)
        val cagesChanged = next.cages != cages.cages
        cages = next
        problem = null
        if (cagesChanged) {
            cageProblem = null
            saveDraft()
        }
        publish()
    }

    private fun saveDraft() {
        val draft = "${mode.name}$DRAFT_SEPARATOR${PuzzleCodec.encode(Puzzle(entry.values, null, cages.cages))}"
        viewModelScope.launch { settings.setCustomDraft(draft) }
    }

    private fun restoreDraft(draft: String) {
        val (savedMode, encoded) =
            if (DRAFT_SEPARATOR in draft) {
                draft.split(DRAFT_SEPARATOR, limit = 2)
            } else {
                listOf(GameMode.CLASSIC.name, draft)
            }
        val puzzle = runCatching { PuzzleCodec.decode(encoded) }.getOrNull() ?: return
        if (savedMode == GameMode.KILLER.name) {
            mode = GameMode.KILLER
        }
        entry = entry.copy(values = puzzle.givens)
        cages = CageBuilder(puzzle.cages)
    }

    private fun publish() {
        _uiState.value = render()
    }

    private fun render(): EntryUiState {
        val cageList = activeCages()
        val clashes = conflicts(entry.values, cageList)
        val board = entry.copy(puzzle = emptyPuzzle(cageList)).toBoardUi()
        val selection = cages.selection.toSet()
        return EntryUiState(
            mode = mode,
            board =
                board.copy(
                    cells =
                        board.cells.mapIndexed { cell, ui ->
                            ui.copy(
                                wrong = cell in clashes,
                                highlight =
                                    if (cageTool) {
                                        cageToolHighlight(cell in selection)
                                    } else {
                                        ui.highlight
                                    },
                            )
                        },
                ),
            inputMode = entry.inputMode,
            activeDigit = entry.activeDigit,
            givenCount = entry.values.count { it != 0 },
            cageCount = cageList.size,
            digitCounts = List(10) { entry.digitCount(it) },
            cageTool = cageTool,
            cageSumInput = cages.sumInput,
            cageProblem = cageProblem,
            problem = problem,
            checking = checking,
        )
    }

    private fun cageToolHighlight(selected: Boolean) =
        if (selected) {
            Highlight.SELECTED
        } else {
            Highlight.NONE
        }

    private companion object {
        const val MAX_SOLVER_STEPS = 3_000_000L
        const val DRAFT_SEPARATOR = "#"

        fun emptyPuzzle(cages: List<Cage> = emptyList()) = Puzzle(Grid.STANDARD.emptyValues(), solution = null, cages = cages)
    }
}
