package com.joclegg.puzzles.twotone.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.twotone.core.data.GameRepository
import com.joclegg.puzzles.twotone.core.data.InputMode
import com.joclegg.puzzles.twotone.core.data.PuzzleRef
import com.joclegg.puzzles.twotone.core.data.SavedGame
import com.joclegg.puzzles.twotone.core.data.Settings
import com.joclegg.puzzles.twotone.core.data.SettingsRepository
import com.joclegg.puzzles.twotone.core.ui.BoardUi
import com.joclegg.puzzles.twotone.core.ui.message
import com.joclegg.puzzles.twotone.core.ui.title
import com.joclegg.puzzles.twotone.core.ui.toBoardUi
import com.joclegg.puzzles.twotone.engine.GameState
import com.joclegg.puzzles.twotone.engine.GameTimer
import com.joclegg.puzzles.twotone.engine.Tile
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GameUiState(
    val title: String,
    val board: BoardUi,
    val elapsedMs: Long,
    val progressPercent: Int,
    val hintMessage: String?,
    val hints: Int,
    val showTimer: Boolean,
    val showProgress: Boolean,
    val pickColour: Boolean,
    val colour: Tile,
    val canUndo: Boolean,
    val solved: Boolean,
    val newBest: Boolean,
    val resultSaved: Boolean,
    val showHints: Boolean = true,
    val showTutorial: Boolean = true,
    val allowReveal: Boolean = true,
    val revealed: Boolean = false,
)

class GameViewModel(
    private val gameId: Long,
    private val games: GameRepository,
    private val settings: SettingsRepository,
    private val monotonicNowMs: () -> Long,
    private val newSeed: () -> Long,
) : ViewModel() {
    private var saved: SavedGame? = null
    private var game: GameState? = null
    private var hints = 0
    private var colour = Tile.RED
    private var timer = GameTimer()
    private var visible = false
    private var ticker: Job? = null
    private var pendingSave: Job? = null
    private var newBest = false
    private var resultSaved = false
    private var display = Settings()
    private var revealed = false

    private val _uiState = MutableStateFlow<GameUiState?>(null)
    val uiState: StateFlow<GameUiState?> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = games.get(gameId) ?: return@launch
            saved = loaded
            game = GameState.replay(loaded.ref.puzzle, loaded.changes)
            hints = loaded.hints
            timer = GameTimer(accumulatedMs = loaded.elapsedMs)
            if (visible) {
                resumeTimer()
            }
            settings.settings.collect {
                display = it
                publish()
            }
        }
    }

    fun onTileClick(cell: Int) =
        move {
            if (display.inputMode == InputMode.PICK_COLOUR) {
                it.paint(cell, colour)
            } else {
                it.tap(cell)
            }
        }

    fun onColourClick(picked: Tile) {
        colour = picked
        publish()
    }

    fun onUndo() = move { it.undo() }

    fun onInputModeToggle() {
        val next =
            if (display.inputMode == InputMode.PICK_COLOUR) {
                InputMode.TAP_TO_CYCLE
            } else {
                InputMode.PICK_COLOUR
            }
        viewModelScope.launch { settings.setInputMode(next) }
    }

    fun onHint() {
        val current = game ?: return
        if (current.isSolved) {
            return
        }
        if (current.hint != null) {
            game = current.copy(hint = null)
        } else {
            game = current.withHint()
            hints++
            persistSoon()
        }
        publish()
    }

    fun onRevealSolution() {
        val current = game ?: return
        if (current.isSolved || revealed) {
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

    fun onRestart() {
        val current = game ?: return
        if (current.isSolved) {
            return
        }
        game = current.restart()
        timer = GameTimer()
        hints = 0
        if (visible) {
            resumeTimer()
        }
        persistNow()
        publish()
    }

    suspend fun newPuzzle(): Long {
        val size = saved?.ref?.size ?: return gameId
        val seed = newSeed()
        return games.start { PuzzleRef.random(seed, size) }.id
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
        if (current.isSolved) {
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
        if (revealed) {
            return
        }
        val before = game ?: return
        val after = change(before)
        if (after == before) {
            return
        }
        game = after
        if (!before.isSolved && after.isSolved) {
            finish()
        } else if (after.changes != before.changes) {
            persistSoon()
        }
        publish()
    }

    private fun finish() {
        timer = timer.pause(monotonicNowMs())
        ticker?.cancel()
        pendingSave?.cancel()
        val finished = snapshot() ?: return
        viewModelScope.launch {
            newBest = withContext(NonCancellable) { games.solve(finished) }
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
        if (game?.isSolved == true) {
            return
        }
        viewModelScope.launch { withContext(NonCancellable) { games.save(snapshot) } }
    }

    private fun snapshot(): SavedGame? {
        val current = game ?: return null
        return saved?.copy(changes = current.changes, hints = hints, elapsedMs = timer.elapsedMs(monotonicNowMs()))
    }

    private fun publish() {
        val current = game ?: return
        val ref = saved?.ref ?: return
        _uiState.update {
            GameUiState(
                showHints = display.showHints,
                showTutorial = display.showTutorial,
                allowReveal = display.allowReveal,
                revealed = revealed,
                title = ref.title(),
                board = current.toBoardUi(display.markBrokenRules),
                elapsedMs = timer.elapsedMs(monotonicNowMs()),
                progressPercent = current.progressPercent,
                hintMessage = current.hint?.message(),
                hints = hints,
                showTimer = display.showTimer,
                showProgress = display.showProgress,
                pickColour = display.inputMode == InputMode.PICK_COLOUR,
                colour = colour,
                canUndo = current.changes.isNotEmpty() && !current.isSolved,
                solved = current.isSolved,
                newBest = newBest,
                resultSaved = resultSaved,
            )
        }
    }

    private companion object {
        const val TICK_MS = 500L
        const val SAVE_AFTER_IDLE_MS = 400L
    }
}
