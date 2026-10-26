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
import com.joclegg.puzzles.solitaire.engine.Card
import com.joclegg.puzzles.solitaire.engine.GameState
import com.joclegg.puzzles.solitaire.engine.GameTimer
import com.joclegg.puzzles.solitaire.engine.Pile
import com.joclegg.puzzles.solitaire.engine.Solver
import com.joclegg.puzzles.solitaire.engine.Verdict
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
    val unwinnable: Boolean = false,
    val canFinish: Boolean = false,
)

class GameViewModel(
    private val gameId: Long,
    private val games: GameRepository,
    private val settings: SettingsRepository,
    private val monotonicNowMs: () -> Long,
    private val newSeed: () -> Long,
    private val solving: CoroutineDispatcher = Dispatchers.Default,
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
    private var check: Job? = null
    private var lastWinnableMoves: Int? = null
    private var unwinnable = false
    private var warningDismissed = false

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
        if (check == null) {
            checkWinnable()
        }
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

    fun onFinish() = move { it.autoFinish() }

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
        val seed = newSeed()
        val winnable = display.winnableOnly
        return games.start(withContext(solving) { DealRef.random(seed, deal.drawMode, deal.order, winnable) }).id
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
            checkWinnable()
        }
        publish()
    }

    fun onUndoToWinnable() {
        val current = game ?: return
        warningDismissed = false
        move { current.copy(moves = current.moves.take(lastWinnableMoves ?: 0), selection = null) }
    }

    suspend fun giveUp() {
        check?.cancel()
        timer = timer.pause(monotonicNowMs())
        ticker?.cancel()
        pendingSave?.cancel()
        val lost = snapshot() ?: return
        withContext(NonCancellable) { games.lose(lost) }
    }

    fun onDismissUnwinnable() {
        warningDismissed = true
        publish()
    }

    private fun checkWinnable() {
        check?.cancel()
        unwinnable = false
        val current = game ?: return
        if (!display.warnUnwinnable || current.isWon || !current.cycledStock || current.board.cardCount != Card.deck.size) {
            return
        }
        val moves = current.moves.size
        check =
            viewModelScope.launch {
                val started = monotonicNowMs()
                val verdict =
                    withContext(solving) {
                        Solver.solve(current.board, current.drawMode, current.order) {
                            !isActive || monotonicNowMs() - started > CHECK_LIMIT_MS
                        }
                    }
                when (verdict) {
                    Verdict.WINNABLE -> lastWinnableMoves = moves
                    Verdict.UNWINNABLE -> unwinnable = true
                    Verdict.UNKNOWN -> Unit
                }
                publish()
            }
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
                unwinnable = unwinnable && !warningDismissed && !current.isWon,
                canFinish = current.canAutoFinish,
            )
        }
    }

    private companion object {
        const val TICK_MS = 500L
        const val SAVE_AFTER_IDLE_MS = 400L
        const val CHECK_LIMIT_MS = 2_000L
    }
}
