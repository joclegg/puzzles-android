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
    val daily2Result: GameStatus? = null,
) {
    val practice: SavedGame? get() = inProgress.find { !it.ref.isDaily && !it.ref.isDaily2 }
}

class HomeViewModel(
    private val games: GameRepository,
    private val settings: SettingsRepository,
    private val words: suspend () -> Words,
    private val newSeed: () -> Long,
    private val today: () -> LocalDate,
    private val daily2Words: DailyWordSource,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(games.inProgress, games.dailies) { inProgress, dailies ->
            val dailyId = WordRef.dailyId(today())
            HomeUiState(
                inProgress = inProgress,
                dailyNumber = Words.dayNumber(today()),
                dailyResult =
                    dailies
                        .find { it.puzzleId == dailyId && it.status != GameStatus.ABANDONED.name }
                        ?.let { GameStatus.valueOf(it.status) },
                daily2Result =
                    dailies
                        .find { WordRef.daily2DateOf(it.puzzleId) == today() && it.status != GameStatus.ABANDONED.name }
                        ?.let { GameStatus.valueOf(it.status) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    suspend fun openDaily(): Long {
        val date = today()
        val dailyId = WordRef.dailyId(date)
        val existing = games.dailies.first().find { it.puzzleId == dailyId && it.status != GameStatus.ABANDONED.name }
        return existing?.id ?: games.start(WordRef.daily(words(), date), settings.settings.first().hardMode).id
    }

    suspend fun openDaily2(): Long? {
        val date = today()
        val existing = games.dailies.first().find { WordRef.daily2DateOf(it.puzzleId) == date && it.status != GameStatus.ABANDONED.name }
        if (existing != null) {
            return existing.id
        }
        val answer = daily2Words.word(date) ?: return null
        return games.start(WordRef.daily2(date, answer), settings.settings.first().hardMode).id
    }

    suspend fun startPractice(): Long = games.start(WordRef.practice(words(), newSeed()), settings.settings.first().hardMode).id

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
