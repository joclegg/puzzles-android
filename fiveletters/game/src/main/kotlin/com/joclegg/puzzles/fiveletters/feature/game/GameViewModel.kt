package com.joclegg.puzzles.fiveletters.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.fiveletters.core.data.GameRepository
import com.joclegg.puzzles.fiveletters.core.data.SavedGame
import com.joclegg.puzzles.fiveletters.core.data.WordRef
import com.joclegg.puzzles.fiveletters.core.ui.BoardUi
import com.joclegg.puzzles.fiveletters.core.ui.message
import com.joclegg.puzzles.fiveletters.core.ui.title
import com.joclegg.puzzles.fiveletters.core.ui.toBoardUi
import com.joclegg.puzzles.fiveletters.engine.GameState
import com.joclegg.puzzles.fiveletters.engine.Words
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GameUiState(
    val title: String,
    val board: BoardUi,
    val message: String?,
    val hardMode: Boolean,
    val won: Boolean,
    val lost: Boolean,
    val guessCount: Int,
    val answer: String,
    val shareText: String,
    val isDaily: Boolean,
)

class GameViewModel(
    private val gameId: Long,
    private val games: GameRepository,
    private val words: suspend () -> Words,
    private val newSeed: () -> Long,
) : ViewModel() {
    private var saved: SavedGame? = null
    private var game: GameState? = null
    private var wordList: Words? = null

    private val _uiState = MutableStateFlow<GameUiState?>(null)
    val uiState: StateFlow<GameUiState?> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = games.get(gameId) ?: return@launch
            wordList = words()
            saved = loaded
            game = GameState(loaded.ref.answer, loaded.guesses, hardMode = loaded.hardMode)
            publish()
        }
    }

    fun onLetterClick(letter: Char) = change { it.type(letter) }

    fun onDeleteClick() = change { it.delete() }

    fun onEnterClick() {
        val list = wordList ?: return
        val before = game ?: return
        val after = before.submit(list)
        game = after
        if (after.guesses != before.guesses) {
            val snapshot = saved?.copy(guesses = after.guesses) ?: return
            saved = snapshot
            viewModelScope.launch {
                withContext(NonCancellable) {
                    if (after.isOver) {
                        games.finish(snapshot, won = after.isWon)
                    } else {
                        games.save(snapshot)
                    }
                }
            }
        }
        publish()
    }

    suspend fun newPractice(): Long {
        val list = wordList ?: words()
        val hardMode = saved?.hardMode ?: false
        return games.start(WordRef.practice(list, newSeed()), hardMode).id
    }

    private fun change(edit: (GameState) -> GameState) {
        val before = game ?: return
        game = edit(before)
        publish()
    }

    private fun publish() {
        val current = game ?: return
        val ref = saved?.ref ?: return
        _uiState.value =
            GameUiState(
                title = ref.title(),
                board = current.toBoardUi(),
                message = current.rejection?.message(),
                hardMode = current.hardMode,
                won = current.isWon,
                lost = current.isLost,
                guessCount = current.guesses.size,
                answer = current.answer,
                shareText = current.shareText(ref.title()),
                isDaily = ref.isDaily,
            )
    }
}
