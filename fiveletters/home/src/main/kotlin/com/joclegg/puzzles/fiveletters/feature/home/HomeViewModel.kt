package com.joclegg.puzzles.fiveletters.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.fiveletters.core.data.DailyWordSource
import com.joclegg.puzzles.fiveletters.core.data.GameRepository
import com.joclegg.puzzles.fiveletters.core.data.GameStatus
import com.joclegg.puzzles.fiveletters.core.data.SavedGame
import com.joclegg.puzzles.fiveletters.core.data.SettingsRepository
import com.joclegg.puzzles.fiveletters.core.data.WordRef
import com.joclegg.puzzles.fiveletters.engine.Words
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val inProgress: List<SavedGame> = emptyList(),
    val dailyNumber: Int = 0,
    val dailyResult: GameStatus? = null,
    val nytResult: GameStatus? = null,
) {
    val practice: SavedGame? get() = inProgress.find { !it.ref.isDaily && !it.ref.isNyt }
}

class HomeViewModel(
    private val games: GameRepository,
    private val settings: SettingsRepository,
    private val words: suspend () -> Words,
    private val newSeed: () -> Long,
    private val today: () -> LocalDate,
    private val nytWords: DailyWordSource,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(games.inProgress, games.history) { inProgress, history ->
            val dailyId = WordRef.dailyId(today())
            val nytId = WordRef.nytId(today())
            HomeUiState(
                inProgress = inProgress,
                dailyNumber = Words.dayNumber(today()),
                dailyResult = history.find { it.ref.id == dailyId && it.status != GameStatus.ABANDONED }?.status,
                nytResult = history.find { it.ref.id == nytId && it.status != GameStatus.ABANDONED }?.status,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    suspend fun openDaily(): Long {
        val date = today()
        val dailyId = WordRef.dailyId(date)
        val existing = games.history.first().find { it.ref.id == dailyId && it.status != GameStatus.ABANDONED }
        return existing?.id ?: games.start(WordRef.daily(words(), date), settings.settings.first().hardMode).id
    }

    suspend fun openNytDaily(): Long? {
        val date = today()
        val nytId = WordRef.nytId(date)
        val existing = games.history.first().find { it.ref.id == nytId && it.status != GameStatus.ABANDONED }
        if (existing != null) {
            return existing.id
        }
        val answer = nytWords.word(date) ?: return null
        return games.start(WordRef.nyt(date, answer), settings.settings.first().hardMode).id
    }

    suspend fun startPractice(): Long = games.start(WordRef.practice(words(), newSeed()), settings.settings.first().hardMode).id

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
