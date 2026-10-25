package com.joclegg.puzzles.solitaire.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.solitaire.core.data.DealRef
import com.joclegg.puzzles.solitaire.core.data.GameRepository
import com.joclegg.puzzles.solitaire.core.data.SavedGame
import com.joclegg.puzzles.solitaire.core.data.Settings
import com.joclegg.puzzles.solitaire.core.data.SettingsRepository
import com.joclegg.puzzles.solitaire.core.ui.TableUi
import com.joclegg.puzzles.solitaire.core.ui.title
import com.joclegg.puzzles.solitaire.core.ui.toTableUi
import com.joclegg.puzzles.solitaire.engine.GameState
import com.joclegg.puzzles.solitaire.engine.GameTimer
import com.joclegg.puzzles.solitaire.engine.Pile
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
    val table: TableUi,
    val elapsedMs: Long,
    val moveCount: Int,
    val showTimer: Boolean,
    val showMoveCount: Boolean,
    val canUndo: Boolean,
    val won: Boolean,
    val newBest: Boolean,
    val resultSaved: Boolean,
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
    private var timer = GameTimer()
    private var visible = false
    private var ticker: Job? = null
    private var pendingSave: Job? = null
    private var newBest = false
    private var resultSaved = false
    private var display = Settings()

    private val _uiState = MutableStateFlow<GameUiState?>(null)
    val uiState: StateFlow<GameUiState?> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = games.get(gameId) ?: return@launch
            saved = loaded
            game = GameState(loaded.deal.board, loaded.deal.drawMode, loaded.moves, order = loaded.deal.order)
            timer = GameTimer(accumulatedMs = loaded.elapsedMs)
            if (visible) {
                resumeTimer()
            }
            settings.settings.collect(::applySettings)
        }
    }

    private fun applySettings(prefs: Settings) {
        display = prefs
        game = game?.copy(oneTapMove = prefs.oneTapMove)
        publish()
    }

    fun onStockClick() = move { it.tapStock() }

    fun onCardClick(
        pile: Pile,
        index: Int,
    ) = move { it.tapCard(pile, index) }

    fun onEmptyPileClick(pile: Pile) = move { it.tapEmptyPile(pile) }

    fun onCardDrop(
        from: Pile,
        index: Int,
        to: Pile,
    ) = move { it.drop(from, index, to) }

    fun onUndo() = move { it.undo() }

    fun onRestart() {
        val current = game ?: return
        if (current.isWon) {
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

    suspend fun newDeal(): Long {
        val deal = saved?.deal ?: return gameId
        return games.start(DealRef.random(newSeed(), deal.drawMode, deal.order)).id
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
        if (current.isWon) {
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
        if (!before.isWon && after.isWon) {
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
            newBest = withContext(NonCancellable) { games.win(finished) }
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
        if (game?.isWon == true) {
            return
        }
        viewModelScope.launch { withContext(NonCancellable) { games.save(snapshot) } }
    }

    private fun snapshot(): SavedGame? {
        val current = game ?: return null
        return saved?.copy(moves = current.moves, elapsedMs = timer.elapsedMs(monotonicNowMs()))
    }

    private fun publish() {
        val current = game ?: return
        val deal = saved?.deal ?: return
        _uiState.update {
            GameUiState(
                title = deal.title(),
                table = current.toTableUi(),
                elapsedMs = timer.elapsedMs(monotonicNowMs()),
                moveCount = current.moveCount,
                showTimer = display.showTimer,
                showMoveCount = display.showMoveCount,
                canUndo = current.moves.isNotEmpty() && !current.isWon,
                won = current.isWon,
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
