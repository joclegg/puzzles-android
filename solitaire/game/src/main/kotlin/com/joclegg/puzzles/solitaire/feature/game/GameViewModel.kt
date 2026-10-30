package com.joclegg.puzzles.solitaire.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.solitaire.core.data.DealRef
import com.joclegg.puzzles.solitaire.core.data.GameRepository
import com.joclegg.puzzles.solitaire.core.data.HintStyle
import com.joclegg.puzzles.solitaire.core.data.SavedGame
import com.joclegg.puzzles.solitaire.core.data.Settings
import com.joclegg.puzzles.solitaire.core.data.SettingsRepository
import com.joclegg.puzzles.solitaire.core.data.WinnableDeals
import com.joclegg.puzzles.solitaire.core.ui.TableUi
import com.joclegg.puzzles.solitaire.core.ui.title
import com.joclegg.puzzles.solitaire.core.ui.toTableUi
import com.joclegg.puzzles.solitaire.engine.Board
import com.joclegg.puzzles.solitaire.engine.Card
import com.joclegg.puzzles.solitaire.engine.GameState
import com.joclegg.puzzles.solitaire.engine.GameTimer
import com.joclegg.puzzles.solitaire.engine.Hint
import com.joclegg.puzzles.solitaire.engine.LostMoveSearch
import com.joclegg.puzzles.solitaire.engine.Pile
import com.joclegg.puzzles.solitaire.engine.Solver
import com.joclegg.puzzles.solitaire.engine.Verdict
import com.joclegg.puzzles.solitaire.engine.findHint
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
    val findingLostMove: Boolean = false,
    val finishing: Boolean = false,
    val lastWinnableMove: Int = 0,
    val canFinish: Boolean = false,
    val showHints: Boolean = true,
    val showTutorial: Boolean = true,
    val hintMessage: String? = null,
)

class GameViewModel(
    private val gameId: Long,
    private val games: GameRepository,
    private val settings: SettingsRepository,
    private val monotonicNowMs: () -> Long,
    private val newSeed: () -> Long,
    private val winnableDeals: WinnableDeals,
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
    private var findingLostMove = false
    private var finishing: Job? = null
    private var warningDismissed = false
    private var hintMessage: String? = null

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

    fun onFinish() {
        val current = game ?: return
        if (finishing != null) {
            return
        }
        val steps = current.autoFinishMoves()
        finishing =
            viewModelScope.launch {
                steps.forEach { step ->
                    move { it.copy(moves = it.moves + step, selection = null) }
                    delay(FINISH_STEP_MS)
                }
                finishing = null
                publish()
            }
        publish()
    }

    fun onHint() {
        val current = game ?: return
        when (val hint = current.findHint()) {
            null -> {
                hintMessage = "No moves left."
                publish()
            }

            Hint.Draw -> {
                if (display.hintStyle == HintStyle.FILL) {
                    move { it.tapStock() }
                } else {
                    hintMessage = "Nothing on the table can move, so draw from the stock."
                    publish()
                }
            }

            is Hint.Transfer -> {
                if (display.hintStyle == HintStyle.FILL) {
                    move { it.drop(hint.selection.pile, it.board.cardsIn(hint.selection.pile).size - hint.selection.count, hint.to) }
                } else {
                    game = current.copy(selection = hint.selection)
                    hintMessage = hintText(current.board, hint)
                    publish()
                }
            }
        }
    }

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
        val winnable = winnableDeals.takeIf { display.winnableOnly }
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
        hintMessage = null
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
        findingLostMove = false
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
                    Verdict.WINNABLE -> {
                        lastWinnableMoves = moves
                    }

                    Verdict.UNWINNABLE -> {
                        unwinnable = true
                        findingLostMove = true
                        publish()
                        lastWinnableMoves = findLostMove(current, LostMoveSearch(minOf(lastWinnableMoves ?: 0, moves), moves))
                        findingLostMove = false
                    }

                    Verdict.UNKNOWN -> {
                        Unit
                    }
                }
                publish()
            }
    }

    private suspend fun findLostMove(
        lost: GameState,
        start: LostMoveSearch,
    ): Int {
        var search = start
        var probe = search.probe
        while (probe != null) {
            val board = lost.copy(moves = lost.moves.take(probe)).board
            val started = monotonicNowMs()
            val verdict =
                withContext(solving) {
                    Solver.solve(board, lost.drawMode, lost.order) {
                        monotonicNowMs() - started > CHECK_LIMIT_MS
                    }
                }
            search = search.narrow(probe, verdict)
            probe = search.probe
        }
        return search.winnable
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
                findingLostMove = findingLostMove,
                finishing = finishing != null,
                lastWinnableMove = lastWinnableMoves ?: 0,
                canFinish = current.canAutoFinish && finishing == null,
                showHints = display.showHints,
                showTutorial = display.showTutorial,
                hintMessage = hintMessage,
            )
        }
    }

    private companion object {
        const val TICK_MS = 500L
        const val SAVE_AFTER_IDLE_MS = 400L
        const val CHECK_LIMIT_MS = 2_000L
        const val FINISH_STEP_MS = 110L
    }
}

internal fun hintText(
    board: Board,
    hint: Hint.Transfer,
): String {
    val name = board.cardsIn(hint.selection.pile).let { it[it.size - hint.selection.count] }.label()
    val to = hint.to
    if (to is Pile.Foundation) {
        return "Move the $name to its foundation."
    }
    val target = board.cardsIn(to).lastOrNull()
    val where =
        if (target == null) {
            "to the empty column ${(to as Pile.Tableau).index + 1}"
        } else {
            "onto the ${target.label()}"
        }
    val from = hint.selection.pile
    if (from is Pile.Tableau) {
        val column = board.tableau[from.index]
        return if (column.faceUpFrom > 0 && column.cards.size - hint.selection.count == column.faceUpFrom) {
            "Move the $name $where to turn over the card under it."
        } else {
            "Move the $name $where."
        }
    }
    return "Move the $name from the waste $where, so the card under it can be played."
}

private fun Card.label() = "$rankLabel${suit.symbol}"
