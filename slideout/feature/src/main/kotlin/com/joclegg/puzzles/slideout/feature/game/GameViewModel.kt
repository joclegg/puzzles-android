package com.joclegg.puzzles.slideout.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.core.data.GameSession
import com.joclegg.puzzles.slideout.core.data.GameRepository
import com.joclegg.puzzles.slideout.core.data.PuzzleBank
import com.joclegg.puzzles.slideout.core.data.PuzzleRef
import com.joclegg.puzzles.slideout.core.data.SavedGame
import com.joclegg.puzzles.slideout.core.data.Settings
import com.joclegg.puzzles.slideout.core.data.SettingsRepository
import com.joclegg.puzzles.slideout.engine.GameState
import com.joclegg.puzzles.slideout.ui.BoardUi
import com.joclegg.puzzles.slideout.ui.label
import com.joclegg.puzzles.slideout.ui.title
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
    val levelLabel: String,
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
    private val session =
        GameSession(
            scope = viewModelScope,
            monotonicNowMs = monotonicNowMs,
            canRun = { game?.isSolved == false },
            onTick = ::publishElapsed,
            onSave = ::persistNow,
        )
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
            session.restore(loaded.elapsedMs)
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
                session.saveSoon()
                current.withHint()
            }
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
        hints = 0
        persistNow()
        publish()
    }

    suspend fun newPuzzle(): Long {
        val level = saved?.ref?.level ?: return gameId
        val seed = newSeed()
        return games.start { PuzzleRef.random(bank, level, seed) }.id
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
        if (!before.isSolved && after.isSolved) {
            finish()
        } else if (after.moves != before.moves) {
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
        return saved?.copy(moves = current.moves, hints = hints, elapsedMs = session.elapsedMs())
    }

    private fun publishElapsed() {
        _uiState.update { it?.copy(elapsedMs = session.elapsedMs()) }
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
                levelLabel = ref.level.label(),
                board = BoardUi(current.board, current.hint),
                elapsedMs = session.elapsedMs(),
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
}
