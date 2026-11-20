package com.joclegg.puzzles.crossword.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.core.data.GameSession
import com.joclegg.puzzles.core.ui.dailyDateOf
import com.joclegg.puzzles.crossword.core.data.GameRepository
import com.joclegg.puzzles.crossword.core.data.HintStyle
import com.joclegg.puzzles.crossword.core.data.PuzzleBank
import com.joclegg.puzzles.crossword.core.data.PuzzleRef
import com.joclegg.puzzles.crossword.core.data.SavedGame
import com.joclegg.puzzles.crossword.core.data.Settings
import com.joclegg.puzzles.crossword.core.data.SettingsRepository
import com.joclegg.puzzles.crossword.engine.Clues
import com.joclegg.puzzles.crossword.engine.GameState
import com.joclegg.puzzles.crossword.engine.Word
import com.joclegg.puzzles.crossword.engine.findHint
import com.joclegg.puzzles.crossword.ui.BoardInput
import com.joclegg.puzzles.crossword.ui.BoardUi
import com.joclegg.puzzles.crossword.ui.ClueUi
import com.joclegg.puzzles.crossword.ui.clueUi
import com.joclegg.puzzles.crossword.ui.sizeLabel
import com.joclegg.puzzles.crossword.ui.title
import com.joclegg.puzzles.crossword.ui.toBoardUi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class GameUiState(
    val dailyDate: LocalDate? = null,
    val title: String,
    val levelLabel: String,
    val board: BoardUi,
    val currentClue: ClueUi?,
    val acrossClues: List<ClueUi>,
    val downClues: List<ClueUi>,
    val elapsedMs: Long,
    val showTimer: Boolean,
    val canUndo: Boolean,
    val canRedo: Boolean = false,
    val solved: Boolean,
    val newBest: Boolean,
    val resultSaved: Boolean,
    val allowReveal: Boolean = false,
    val revealed: Boolean = false,
    val showHints: Boolean = false,
    val hintMessage: String? = null,
)

class GameViewModel(
    private val gameId: Long,
    private val games: GameRepository,
    private val settings: SettingsRepository,
    private val bank: PuzzleBank,
    private val monotonicNowMs: () -> Long,
    private val newSeed: () -> Long,
    private val background: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private var saved: SavedGame? = null
    private var game: GameState? = null
    private var clues = Clues(emptyMap())
    private val session =
        GameSession(
            scope = viewModelScope,
            monotonicNowMs = monotonicNowMs,
            canRun = { game?.isSolved == false && !revealed },
            onTick = ::publishElapsed,
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
            clues = withContext(background) { runCatching { bank.clues }.getOrDefault(Clues(emptyMap())) }
            game =
                GameState
                    .restore(loaded.ref.puzzle, loaded.entries, loaded.changes)
                    .copy(mistakes = loaded.mistakes, reveals = loaded.hints)
            session.restore(loaded.elapsedMs)
            settings.settings.collect {
                display = it
                game = game?.copy(skipFilledCells = it.skipFilledCells, countMistakes = it.showMistakes)
                publish()
            }
        }
    }

    fun onCellClick(cell: Int) = move { it.tapCell(cell) }

    fun onClueClick(clue: ClueUi) =
        move { state ->
            val word = state.puzzle.words.find { it.number == clue.number && it.isAcross == clue.isAcross }
            word?.let(state::selectWord) ?: state
        }

    fun onInput(input: BoardInput) =
        move {
            when (input) {
                is BoardInput.Letter -> it.type(input.letter)
                BoardInput.Backspace -> it.backspace()
                BoardInput.ToggleDirection -> it.toggleDirection()
                BoardInput.NextClue -> it.nextClue()
                BoardInput.PreviousClue -> it.previousClue()
                is BoardInput.Move -> it.arrow(input.arrow)
            }
        }

    fun onNextClue() = move { it.nextClue() }

    fun onPreviousClue() = move { it.previousClue() }

    fun onUndo() = move { it.undo() }

    fun onRedo() = move { it.redo() }

    fun onHint() {
        val current = game ?: return
        if (current.isSolved || revealed) {
            return
        }
        val hint = current.findHint() ?: return
        val fill = display.hintStyle == HintStyle.FILL
        move {
            if (fill) {
                it.placeHint(hint)
            } else {
                it.pointAt(hint)
            }
        }
        if (game?.isSolved == false) {
            session.saveSoon()
        }
        if (!fill) {
            hintMessage = hint.message
            publish()
        }
    }

    fun onRevealLetter() = move { it.revealLetter() }

    fun onRevealWord() = move { it.revealWord() }

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
        hintMessage = null
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
        hintMessage = null
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
        return saved?.copy(
            entries = current.entries,
            changes = current.changes,
            mistakes = current.mistakes,
            hints = current.reveals,
            elapsedMs = session.elapsedMs(),
        )
    }

    private fun publishElapsed() {
        _uiState.update { it?.copy(elapsedMs = session.elapsedMs()) }
    }

    private fun publish() {
        val current = game ?: return
        val ref = saved?.ref ?: return
        val clueOf = { word: Word -> current.clueUi(word, clues::clueFor) }
        _uiState.value =
            GameUiState(
                dailyDate = dailyDateOf(ref.id),
                title = ref.title(),
                levelLabel = sizeLabel(ref.size),
                board = current.toBoardUi(display.showMistakes),
                currentClue = current.currentWord?.let(clueOf),
                acrossClues = current.puzzle.across.map(clueOf),
                downClues = current.puzzle.down.map(clueOf),
                elapsedMs = session.elapsedMs(),
                showTimer = display.showTimer,
                canUndo = current.changes.isNotEmpty() && !current.isSolved && !revealed,
                canRedo = current.undone.isNotEmpty() && !current.isSolved && !revealed,
                solved = current.isSolved,
                newBest = newBest,
                resultSaved = resultSaved,
                allowReveal = display.allowReveal,
                revealed = revealed,
                showHints = display.showHints,
                hintMessage = hintMessage,
            )
    }
}
