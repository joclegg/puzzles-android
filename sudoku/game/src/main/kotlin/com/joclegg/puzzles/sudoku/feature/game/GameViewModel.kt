package com.joclegg.puzzles.sudoku.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.sudoku.core.data.DigitCounts
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.GameRepository
import com.joclegg.puzzles.sudoku.core.data.HintStyle
import com.joclegg.puzzles.sudoku.core.data.ModeSettings
import com.joclegg.puzzles.sudoku.core.data.SavedGame
import com.joclegg.puzzles.sudoku.core.data.Settings
import com.joclegg.puzzles.sudoku.core.data.SettingsRepository
import com.joclegg.puzzles.sudoku.core.ui.BoardUi
import com.joclegg.puzzles.sudoku.core.ui.toBoardUi
import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.GameState
import com.joclegg.puzzles.sudoku.engine.GameTimer
import com.joclegg.puzzles.sudoku.engine.InputMode
import com.joclegg.puzzles.sudoku.engine.findHint
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GameUiState(
    val mode: GameMode,
    val difficulty: Difficulty?,
    val custom: Boolean,
    val answerKnown: Boolean,
    val board: BoardUi,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val markActive: Boolean = false,
    val elapsedMs: Long,
    val mistakes: Int,
    val checking: Boolean,
    val showMistakeCount: Boolean = true,
    val showTimer: Boolean = true,
    val showDigitCounts: Boolean = true,
    val inputMode: InputMode,
    val notesMode: Boolean,
    val eraserActive: Boolean = false,
    val activeDigit: Int?,
    val doneDigits: Set<Int>,
    val digitCounts: List<Int>,
    val completed: Boolean,
    val newBest: Boolean,
    val resultSaved: Boolean = false,
    val revealed: Boolean = false,
    val showHints: Boolean = true,
    val showTutorial: Boolean = true,
    val allowReveal: Boolean = true,
    val hintMessage: String? = null,
)

class GameViewModel(
    private val gameId: Long,
    private val games: GameRepository,
    private val settings: SettingsRepository,
    private val monotonicNowMs: () -> Long,
) : ViewModel() {
    private var saved: SavedGame? = null
    private var game: GameState? = null
    private var timer = GameTimer()
    private var visible = false
    private var ticker: Job? = null
    private var pendingSave: Job? = null
    private var newBest = false
    private var resultSaved = false
    private var revealed = false
    private var display = ModeSettings()
    private var hintMessage: String? = null

    private val _uiState = MutableStateFlow<GameUiState?>(null)
    val uiState: StateFlow<GameUiState?> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = games.get(gameId) ?: return@launch
            val prefs = settings.settings.first()
            saved = loaded
            game =
                GameState(
                    puzzle = loaded.ref.puzzle,
                    values = loaded.values,
                    notes = loaded.notes,
                    inputMode = prefs.inputMode,
                    mistakeCheck = loaded.mistakeCheck,
                    mistakes = loaded.mistakes,
                    undoStack = loaded.moves,
                    options = prefs.forMode(loaded.ref.mode).playOptions,
                )
            timer = GameTimer(accumulatedMs = loaded.elapsedMs)
            if (visible) {
                resumeTimer()
            }
            settings.settings.collect(::applySettings)
        }
    }

    private fun applySettings(prefs: Settings) {
        val mode = saved?.ref?.mode ?: return
        display = prefs.forMode(mode)
        game =
            game?.let { current ->
                val withOptions = current.copy(options = display.playOptions)
                if (withOptions.inputMode == prefs.inputMode) {
                    withOptions
                } else {
                    withOptions.withInputMode(prefs.inputMode)
                }
            }
        publish()
    }

    fun onCellClick(cell: Int) = move { it.tapCell(cell) }

    fun onDigitClick(digit: Int) = move { it.tapDigit(digit) }

    fun onErase() = move { it.erase() }

    fun onUndo() = move { it.undo() }

    fun onRedo() = move { it.redo() }

    fun onMark() = move { it.mark() }

    fun onToggleNotes() = move { it.toggleNotesMode() }

    fun onRestart() {
        val current = game ?: return
        if (current.isComplete) {
            return
        }
        game = current.restart()
        timer = GameTimer()
        if (visible) {
            resumeTimer()
        }
        persistNow()
        publish()
    }

    fun onHint() {
        val current = game ?: return
        if (current.isComplete) {
            return
        }
        val hint = current.findHint() ?: return
        if (display.hintStyle == HintStyle.FILL) {
            move { it.placeHint(hint) }
            return
        }
        game = current.copy(selected = hint.cell)
        hintMessage = hint.message
        publish()
    }

    fun onRevealSolution() {
        val current = game ?: return
        if (current.isComplete || current.puzzle.solution == null) {
            return
        }
        timer = timer.pause(monotonicNowMs())
        ticker?.cancel()
        pendingSave?.cancel()
        game = current.revealSolution()
        revealed = true
        val snapshot = snapshot() ?: return
        viewModelScope.launch { withContext(NonCancellable) { games.reveal(snapshot) } }
        publish()
    }

    fun onVisible() {
        visible = true
        resumeTimer()
    }

    fun onHidden() {
        visible = false
        timer = timer.pause(monotonicNowMs())
        ticker?.cancel()
        persistNow()
    }

    private fun resumeTimer() {
        val current = game ?: return
        if (current.isComplete) {
            return
        }
        timer = timer.start(monotonicNowMs())
        ticker?.cancel()
        ticker =
            viewModelScope.launch {
                while (isActive) {
                    publish()
                    delay(TICK_MS)
                }
            }
    }

    private fun move(change: (GameState) -> GameState) {
        val before = game ?: return
        val after = change(before)
        if (after == before) {
            return
        }
        game = after
        hintMessage = null
        if (!before.isComplete &&
            after.isComplete
        ) {
            finish()
        } else if (after.values != before.values || after.notes != before.notes) {
            persistSoon()
        }
        publish()
    }

    private fun finish() {
        timer = timer.pause(monotonicNowMs())
        ticker?.cancel()
        val finished = snapshot() ?: return
        viewModelScope.launch {
            newBest = games.complete(finished)
            resultSaved = true
            publish()
        }
    }

    private fun persistSoon() {
        pendingSave?.cancel()
        pendingSave =
            viewModelScope.launch {
                delay(SAVE_AFTER_IDLE_MS)
                persistNow()
            }
    }

    private fun persistNow() {
        pendingSave?.cancel()
        val snapshot = snapshot() ?: return
        if (game?.isComplete == true) {
            return
        }
        viewModelScope.launch { withContext(NonCancellable) { games.save(snapshot) } }
    }

    private fun snapshot(): SavedGame? {
        val current = game ?: return null
        return saved?.copy(
            values = current.values,
            notes = current.notes,
            mistakes = current.mistakes,
            elapsedMs = timer.elapsedMs(monotonicNowMs()),
            moves = current.undoStack,
        )
    }

    private fun publish() {
        val current = game ?: return
        val ref = saved?.ref ?: return
        _uiState.update {
            GameUiState(
                mode = ref.mode,
                difficulty = ref.difficulty,
                custom = ref.custom,
                answerKnown = ref.puzzle.solution != null,
                board =
                    current.toBoardUi().let { board ->
                        board.copy(
                            boldPlacedDigits = display.boldPlacedDigits,
                            greyGivenCells = display.greyGivenCells,
                        )
                    },
                elapsedMs = timer.elapsedMs(monotonicNowMs()),
                mistakes = current.mistakes,
                checking = current.countsMistakes,
                showMistakeCount = display.showMistakeCount,
                showTimer = display.showTimer,
                showDigitCounts = display.digitCounts != DigitCounts.OFF,
                inputMode = current.inputMode,
                notesMode = current.notesMode,
                eraserActive = current.eraserActive,
                activeDigit = current.activeDigit,
                doneDigits =
                    if (display.disableCompletedDigits) {
                        current.puzzle.grid.digits
                            .filter(
                                current::isDigitDone,
                            ).toSet()
                    } else {
                        emptySet()
                    },
                digitCounts =
                    List(10) { digit ->
                        if (display.digitCounts == DigitCounts.COUNT_DOWN) {
                            (current.puzzle.grid.size - current.digitCount(digit)).coerceAtLeast(0)
                        } else {
                            current.digitCount(digit)
                        }
                    },
                completed = current.isComplete,
                canUndo = current.undoStack.isNotEmpty() && !current.isComplete,
                canRedo = current.redoStack.isNotEmpty() && !current.isComplete,
                markActive = current.markActive,
                showHints = display.showHints,
                showTutorial = display.showTutorial,
                allowReveal = display.allowReveal,
                hintMessage = hintMessage,
                newBest = newBest,
                resultSaved = resultSaved,
                revealed = revealed,
            )
        }
    }

    private companion object {
        const val TICK_MS = 500L
        const val SAVE_AFTER_IDLE_MS = 400L
    }
}
