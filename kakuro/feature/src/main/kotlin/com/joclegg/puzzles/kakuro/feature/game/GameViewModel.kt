package com.joclegg.puzzles.kakuro.feature.game

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.core.data.GameSession
import com.joclegg.puzzles.kakuro.core.data.GameRepository
import com.joclegg.puzzles.kakuro.core.data.HintStyle
import com.joclegg.puzzles.kakuro.core.data.PuzzleBank
import com.joclegg.puzzles.kakuro.core.data.PuzzleRef
import com.joclegg.puzzles.kakuro.core.data.SavedGame
import com.joclegg.puzzles.kakuro.core.data.Settings
import com.joclegg.puzzles.kakuro.core.data.SettingsRepository
import com.joclegg.puzzles.kakuro.engine.Combinations
import com.joclegg.puzzles.kakuro.engine.GameState
import com.joclegg.puzzles.kakuro.engine.InputMode
import com.joclegg.puzzles.kakuro.engine.MAX_DIGIT
import com.joclegg.puzzles.kakuro.engine.MistakeCheck
import com.joclegg.puzzles.kakuro.engine.findHint
import com.joclegg.puzzles.kakuro.ui.BoardUi
import com.joclegg.puzzles.kakuro.ui.sizeLabel
import com.joclegg.puzzles.kakuro.ui.title
import com.joclegg.puzzles.kakuro.ui.toBoardUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GameUiState(
    val title: String,
    val levelLabel: String,
    val board: BoardUi,
    val elapsedMs: Long,
    val showTimer: Boolean,
    val notesMode: Boolean,
    val digitFirst: Boolean,
    val eraserActive: Boolean,
    val activeDigit: Int?,
    val doneDigits: Set<Int>,
    val mistakes: Int,
    val showMistakeCount: Boolean,
    val combinations: List<AnnotatedString>,
    val canUndo: Boolean,
    val canRedo: Boolean = false,
    val markActive: Boolean = false,
    val solved: Boolean,
    val newBest: Boolean,
    val resultSaved: Boolean,
    val showHints: Boolean = true,
    val showTutorial: Boolean = true,
    val allowReveal: Boolean = true,
    val revealed: Boolean = false,
    val hintMessage: String? = null,
)

class GameViewModel(
    private val gameId: Long,
    private val games: GameRepository,
    private val settings: SettingsRepository,
    private val bank: PuzzleBank,
    private val monotonicNowMs: () -> Long,
    private val newSeed: () -> Long,
) : ViewModel() {
    private var saved: SavedGame? = null
    private var game: GameState? = null
    private val session =
        GameSession(
            scope = viewModelScope,
            monotonicNowMs = monotonicNowMs,
            canRun = { game?.isSolved == false },
            onTick = ::publish,
            onSave = ::persistNow,
        )
    private var newBest = false
    private var resultSaved = false
    private var display = Settings()
    private var revealed = false
    private var hintMessage: String? = null

    private val _uiState = MutableStateFlow<GameUiState?>(null)
    val uiState: StateFlow<GameUiState?> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = games.get(gameId) ?: return@launch
            saved = loaded
            game = GameState.replay(loaded.ref.puzzle, loaded.changes).copy(mistakes = loaded.mistakes)
            session.restore(loaded.elapsedMs)
            settings.settings.collect {
                display = it
                game =
                    game?.let { current ->
                        val checked = current.copy(mistakeCheck = it.mistakeCheck, autoRemoveNotes = it.autoRemoveNotes)
                        if (checked.inputMode == it.inputMode) {
                            checked
                        } else {
                            checked.withInputMode(it.inputMode)
                        }
                    }
                publish()
            }
        }
    }

    fun onCellClick(cell: Int) = move { it.tapCell(cell) }

    fun onDigitClick(digit: Int) = move { it.tapDigit(digit) }

    fun onErase() = move { it.erase() }

    fun onUndo() = move { it.undo() }

    fun onRedo() = move { it.redo() }

    fun onMark() = move { it.mark() }

    fun onToggleNotes() = move { it.toggleNotes() }

    fun onHint() {
        val current = game ?: return
        if (current.isSolved) {
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
        if (current.isSolved || revealed) {
            return
        }
        session.stop()
        game = current.revealSolution()
        revealed = true
        val snapshot = snapshot() ?: return
        viewModelScope.launch { withContext(NonCancellable) { games.reveal(snapshot) } }
        publish()
    }

    fun onRestart() {
        val current = game ?: return
        if (current.isSolved) {
            return
        }
        game = current.restart()
        session.restart()
        persistNow()
        publish()
    }

    suspend fun newPuzzle(): Long {
        val size = saved?.ref?.size ?: return gameId
        val seed = newSeed()
        return games.start { PuzzleRef.random(bank, size, seed) }.id
    }

    fun onVisible() {
        session.onVisible()
    }

    fun onHidden() {
        session.onHidden()
        persistNow()
    }

    private fun move(change: (GameState) -> GameState) {
        if (revealed) {
            return
        }
        val before = game ?: return
        val after = change(before)
        if (after == before) {
            return
        }
        game = after
        hintMessage = null
        if (!before.isSolved && after.isSolved) {
            finish()
        } else if (after.changes != before.changes || after.mistakes != before.mistakes) {
            session.saveSoon()
        }
        publish()
    }

    private fun finish() {
        session.stop()
        val finished = snapshot() ?: return
        viewModelScope.launch {
            newBest = withContext(NonCancellable) { games.solve(finished) }
            resultSaved = true
            publish()
        }
    }

    private fun persistNow() {
        session.cancelPendingSave()
        val snapshot = snapshot() ?: return
        if (game?.isSolved == true) {
            return
        }
        viewModelScope.launch { withContext(NonCancellable) { games.save(snapshot) } }
    }

    private fun snapshot(): SavedGame? {
        val current = game ?: return null
        return saved?.copy(changes = current.changes, mistakes = current.mistakes, elapsedMs = session.elapsedMs())
    }

    private fun publish() {
        val current = game ?: return
        val ref = saved?.ref ?: return
        val board = current.toBoardUi()
        _uiState.value =
            GameUiState(
                showHints = display.showHints,
                showTutorial = display.showTutorial,
                allowReveal = display.allowReveal,
                revealed = revealed,
                hintMessage = hintMessage,
                title = ref.title(),
                levelLabel = sizeLabel(ref.size),
                board =
                    if (display.highlightRuns) {
                        board
                    } else {
                        board.copy(runCells = emptySet())
                    },
                elapsedMs = session.elapsedMs(),
                showTimer = display.showTimer,
                notesMode = current.notesMode,
                digitFirst = current.inputMode == InputMode.DIGIT_FIRST,
                eraserActive = current.eraserActive,
                activeDigit = current.activeDigit,
                doneDigits =
                    if (display.greyUsedDigits) {
                        (1..MAX_DIGIT).filter(current::isDigitDone).toSet()
                    } else {
                        emptySet()
                    },
                mistakes = current.mistakes,
                showMistakeCount = display.showMistakeCount && current.mistakeCheck != MistakeCheck.OFF,
                combinations =
                    if (display.showCombinations) {
                        current.selectedCombinations().map { (run, masks) ->
                            val direction =
                                if (run.isAcross) {
                                    "Across"
                                } else {
                                    "Down"
                                }
                            val placed =
                                run.cells
                                    .map { current.values[it] }
                                    .filter { it != 0 }
                                    .toSet()
                            buildAnnotatedString {
                                append("$direction ${run.sum} in ${run.cells.size}: ")
                                if (masks.isEmpty()) {
                                    append("none fit")
                                }
                                Combinations.ordered(masks, display.combinationOrder).forEachIndexed { index, digits ->
                                    if (index > 0) {
                                        append("  ")
                                    }
                                    digits.forEach { digit ->
                                        if (digit in placed) {
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$digit") }
                                        } else {
                                            append("$digit")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        emptyList()
                    },
                canUndo = current.changes.isNotEmpty() && !current.isSolved,
                canRedo = current.undone.isNotEmpty() && !current.isSolved,
                markActive = current.markActive,
                solved = current.isSolved,
                newBest = newBest,
                resultSaved = resultSaved,
            )
    }
}
