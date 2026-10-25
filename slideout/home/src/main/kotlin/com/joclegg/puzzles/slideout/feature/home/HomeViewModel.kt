package com.joclegg.puzzles.slideout.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.slideout.core.data.GameRepository
import com.joclegg.puzzles.slideout.core.data.GameStatus
import com.joclegg.puzzles.slideout.core.data.PuzzleBank
import com.joclegg.puzzles.slideout.core.data.PuzzleRef
import com.joclegg.puzzles.slideout.core.data.SavedGame
import com.joclegg.puzzles.slideout.engine.Level
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val inProgress: List<SavedGame> = emptyList(),
    val dailySolved: Set<Level> = emptySet(),
) {
    fun savedFor(level: Level) = inProgress.find { !it.ref.isDaily && it.ref.level == level }
}

class HomeViewModel(
    private val games: GameRepository,
    private val bank: PuzzleBank,
    private val newSeed: () -> Long,
    private val today: () -> LocalDate,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(games.inProgress, games.history) { inProgress, history ->
            val date = today()
            val solved =
                Level.entries.filter { level ->
                    history.any { it.ref.id == PuzzleRef.dailyId(date, level) && it.status == GameStatus.SOLVED }
                }
            HomeUiState(inProgress, dailySolved = solved.toSet())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    suspend fun start(level: Level): Long {
        val seed = newSeed()
        return games.start { PuzzleRef.random(bank, level, seed) }.id
    }

    suspend fun openDaily(level: Level): Long {
        val date = today()
        val id = PuzzleRef.dailyId(date, level)
        return uiState.value.inProgress
            .find { it.ref.id == id }
            ?.id ?: games.start { PuzzleRef.daily(bank, date, level) }.id
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
