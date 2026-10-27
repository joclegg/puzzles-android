package com.joclegg.puzzles.wordwheel.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.wordwheel.core.data.GameRepository
import com.joclegg.puzzles.wordwheel.core.data.PuzzleBank
import com.joclegg.puzzles.wordwheel.core.data.PuzzleRef
import com.joclegg.puzzles.wordwheel.core.data.SavedGame
import com.joclegg.puzzles.wordwheel.core.data.Settings
import com.joclegg.puzzles.wordwheel.core.data.SettingsRepository
import com.joclegg.puzzles.wordwheel.core.ui.GridUi
import com.joclegg.puzzles.wordwheel.core.ui.message
import com.joclegg.puzzles.wordwheel.core.ui.title
import com.joclegg.puzzles.wordwheel.engine.GameState
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class GameUiState(
    val title: String,
    val grid: GridUi,
    val wheel: String,
    val chosen: List<Int>,
    val message: String?,
    val bonusCount: Int,
    val wordsLeft: Int,
    val elapsedMs: Long,
    val showTimer: Boolean,
    val showBonusCount: Boolean,
    val solved: Boolean,
    val newBest: Boolean,
    val resultSaved: Boolean,
    val showHints: Boolean = true,
    val showTutorial: Boolean = true,
    val allowReveal: Boolean = true,
    val revealedAll: Boolean = false,
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
    private var chosen: List<Int> = emptyList()
    private var elapsedBase = 0L
    private var runningSince: Long? = null
    private var ticker: Job? = null
    private var newBest = false
    private var resultSaved = false
    private var display = Settings()
    private var revealedAll = false
    private val random = Random(newSeed())

    private val _uiState = MutableStateFlow<GameUiState?>(null)
    val uiState: StateFlow<GameUiState?> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = games.get(gameId) ?: return@launch
            saved = loaded
            game = GameState(loaded.ref.puzzle, loaded.found, loaded.bonus, loaded.revealed)
            elapsedBase = loaded.elapsedMs
            settings.settings.collect {
                display = it
                publish()
            }
        }
    }

    fun onChosenChange(indices: List<Int>) {
        chosen = indices
        publish()
    }

    fun onSubmit() {
        val current = game ?: return
        val wheel = current.wheel
        val word = chosen.map { wheel[it] }.joinToString("")
        chosen = emptyList()
        if (word.isEmpty()) {
            publish()
            return
        }
        update(current.submit(word, bank.words))
    }

    fun onClear() {
        chosen = emptyList()
        publish()
    }

    fun onShuffle() {
        val current = game ?: return
        chosen = emptyList()
        game = current.shuffle(current.wheel.indices.shuffled(random))
        publish()
    }

    fun onHint() = game?.let { update(it.reveal { hidden -> hidden.randomOrNull(random) }) }

    suspend fun newPuzzle(): Long {
        val level = saved?.ref?.level ?: return gameId
        val seed = newSeed()
        return games.start { PuzzleRef.random(bank, level, seed) }.id
    }

    fun onRevealSolution() {
        val current = game ?: return
        if (current.isSolved || revealedAll) {
            return
        }
        pause()
        game = current.revealAll()
        revealedAll = true
        chosen = emptyList()
        val snapshot = snapshot() ?: return
        viewModelScope.launch { withContext(NonCancellable) { games.reveal(snapshot) } }
        publish()
    }

    fun onVisible() {
        if (game?.isSolved == true || runningSince != null) {
            return
        }
        runningSince = monotonicNowMs()
        ticker?.cancel()
        ticker =
            viewModelScope.launch {
                while (isActive) {
                    publish()
                    delay(TICK_MS)
                }
            }
    }

    fun onHidden() {
        pause()
        persist()
    }

    private fun elapsed() = elapsedBase + (runningSince?.let { monotonicNowMs() - it } ?: 0)

    private fun pause() {
        elapsedBase = elapsed()
        runningSince = null
        ticker?.cancel()
    }

    private fun update(after: GameState) {
        if (revealedAll) {
            return
        }
        val before = game ?: return
        game = after
        if (!before.isSolved && after.isSolved) {
            pause()
            val finished = snapshot() ?: return
            viewModelScope.launch {
                newBest = withContext(NonCancellable) { games.solve(finished) }
                resultSaved = true
                publish()
            }
        } else if (after.found != before.found || after.bonus != before.bonus || after.revealed != before.revealed) {
            persist()
        }
        publish()
    }

    private fun persist() {
        val snapshot = snapshot() ?: return
        if (game?.isSolved == true) {
            return
        }
        viewModelScope.launch { withContext(NonCancellable) { games.save(snapshot) } }
    }

    private fun snapshot(): SavedGame? {
        val current = game ?: return null
        return saved?.copy(found = current.found, bonus = current.bonus, revealed = current.revealed, elapsedMs = elapsed())
    }

    private fun publish() {
        val current = game ?: return
        val ref = saved?.ref ?: return
        val foundCells =
            ref.puzzle.crossword.placements
                .filter { it.word in current.found }
                .flatMap { it.cells }
                .toSet()
        _uiState.value =
            GameUiState(
                showHints = display.showHints,
                showTutorial = display.showTutorial,
                allowReveal = display.allowReveal,
                revealedAll = revealedAll,
                title = ref.title(),
                grid = GridUi(ref.puzzle.crossword, foundCells, current.revealed - foundCells),
                wheel = current.wheel,
                chosen = chosen,
                message = current.last?.message(),
                bonusCount = current.bonus.size,
                wordsLeft =
                    ref.puzzle.crossword.words
                        .count { it !in current.found },
                elapsedMs = elapsed(),
                showTimer = display.showTimer,
                showBonusCount = display.showBonusCount,
                solved = current.isSolved,
                newBest = newBest,
                resultSaved = resultSaved,
            )
    }

    private companion object {
        const val TICK_MS = 500L
    }
}
