package com.joclegg.puzzles.slideout.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.slideout.core.data.GameRepository
import com.joclegg.puzzles.slideout.core.data.PuzzleBank
import com.joclegg.puzzles.slideout.core.data.PuzzleRef
import com.joclegg.puzzles.slideout.core.data.SavedGame
import com.joclegg.puzzles.slideout.core.data.Settings
import com.joclegg.puzzles.slideout.core.data.SettingsRepository
import com.joclegg.puzzles.slideout.core.ui.BoardUi
import com.joclegg.puzzles.slideout.core.ui.title
import com.joclegg.puzzles.slideout.engine.GameState
import com.joclegg.puzzles.slideout.engine.GameTimer
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
    val board: BoardUi,
    val elapsedMs: Long,
    val moveCount: Int,
    val par: Int,
    val hints: Int,
    val showTimer: Boolean,
    val showPar: Boolean,
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
    private val bank: PuzzleBank,
    private val monotonicNowMs: () -> Long,
    private val newSeed: () -> Long,
) : ViewModel() {
    private var saved: SavedGame? = null
    private var game: GameState? = null
    private var hints = 0
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
            game = GameState(loaded.ref.puzzle, loaded.moves)
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

    fun onSlide(
        block: Int,
        position: Int,
    ) = move { it.slide(block, position) }

    fun onUndo() = move { it.undo() }

    fun onHint() {
        val current = game ?: return
        if (current.isSolved) {
            return
        }
        game =
            if (current.hint != null) {
                current.copy(hint = null)
            } else {
                hints++
                persistSoon()
                current.withHint()
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
        val level = saved?.ref?.level ?: return gameId
        val seed = newSeed()
        return games.start { PuzzleRef.random(bank, level, seed) }.id
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
        } else if (after.moves != before.moves) {
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
        return saved?.copy(moves = current.moves, hints = hints, elapsedMs = timer.elapsedMs(monotonicNowMs()))
    }

    private fun publish() {
        val current = game ?: return
        val ref = saved?.ref ?: return
        _uiState.value =
            GameUiState(
                showHints = display.showHints,
                showTutorial = display.showTutorial,
                allowReveal = display.allowReveal,
                revealed = revealed,
                title = ref.title(),
                board = BoardUi(current.board, current.hint),
                elapsedMs = timer.elapsedMs(monotonicNowMs()),
                moveCount = current.moves.size,
                par = ref.puzzle.par,
                hints = hints,
                showTimer = display.showTimer,
                showPar = display.showPar,
                canUndo = current.moves.isNotEmpty() && !current.isSolved,
                solved = current.isSolved,
                newBest = newBest,
                resultSaved = resultSaved,
            )
    }

    private companion object {
        const val TICK_MS = 500L
        const val SAVE_AFTER_IDLE_MS = 400L
    }
}
