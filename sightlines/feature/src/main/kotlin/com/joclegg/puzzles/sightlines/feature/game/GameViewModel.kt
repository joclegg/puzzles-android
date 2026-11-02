package com.joclegg.puzzles.sightlines.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.core.data.GameSession
import com.joclegg.puzzles.sightlines.core.data.GameRepository
import com.joclegg.puzzles.sightlines.core.data.InputMode
import com.joclegg.puzzles.sightlines.core.data.PuzzleRef
import com.joclegg.puzzles.sightlines.core.data.SavedGame
import com.joclegg.puzzles.sightlines.core.data.Settings
import com.joclegg.puzzles.sightlines.core.data.SettingsRepository
import com.joclegg.puzzles.sightlines.engine.Dot
import com.joclegg.puzzles.sightlines.engine.GameState
import com.joclegg.puzzles.sightlines.ui.BoardUi
import com.joclegg.puzzles.sightlines.ui.message
import com.joclegg.puzzles.sightlines.ui.sizeLabel
import com.joclegg.puzzles.sightlines.ui.title
import com.joclegg.puzzles.sightlines.ui.toBoardUi
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
    val progressPercent: Int,
    val hintMessage: String?,
    val hints: Int,
    val showTimer: Boolean,
    val showProgress: Boolean,
    val pickColour: Boolean,
    val colour: Dot,
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
    private var colour = Dot.BLUE
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

    private val _uiState = MutableStateFlow<GameUiState?>(null)
    val uiState: StateFlow<GameUiState?> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = games.get(gameId) ?: return@launch
            saved = loaded
            game = GameState.replay(loaded.ref.puzzle, loaded.changes)
            hints = loaded.hints
            session.restore(loaded.elapsedMs)
            settings.settings.collect {
                display = it
                publish()
            }
        }
    }

    fun onDotClick(cell: Int) =
        move {
            if (display.inputMode == InputMode.PICK_COLOUR) {
                it.paint(cell, colour)
            } else {
                it.tap(cell)
            }
        }

    fun onColourClick(picked: Dot) {
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
            session.saveSoon()
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
        val size = saved?.ref?.size ?: return gameId
        val seed = newSeed()
        return games.start { PuzzleRef.random(seed, size) }.id
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
        } else if (after.changes != before.changes) {
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
        return saved?.copy(changes = current.changes, hints = hints, elapsedMs = session.elapsedMs())
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
                levelLabel = sizeLabel(ref.size),
                board = current.toBoardUi(display.markBrokenRules, display.showSeenCount),
                elapsedMs = session.elapsedMs(),
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
}
