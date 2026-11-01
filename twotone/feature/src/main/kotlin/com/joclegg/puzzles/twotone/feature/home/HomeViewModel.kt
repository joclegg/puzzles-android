package com.joclegg.puzzles.twotone.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.twotone.core.data.GameRepository
import com.joclegg.puzzles.twotone.core.data.GameStatus
import com.joclegg.puzzles.twotone.core.data.PuzzleRef
import com.joclegg.puzzles.twotone.core.data.SavedGame
import com.joclegg.puzzles.twotone.engine.SIZES
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val inProgress: List<SavedGame> = emptyList(),
    val dailySolved: Set<Int> = emptySet(),
) {
    fun savedFor(size: Int) = inProgress.find { !it.ref.isDaily && it.ref.size == size }
}

class HomeViewModel(
    private val games: GameRepository,
    private val newSeed: () -> Long,
    private val today: () -> LocalDate,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(games.inProgress, games.history) { inProgress, history ->
            val date = today()
            val solved =
                SIZES.filter { size ->
                    history.any { it.ref.id == PuzzleRef.dailyId(date, size) && it.status == GameStatus.SOLVED }
                }
            HomeUiState(inProgress, dailySolved = solved.toSet())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    suspend fun start(size: Int): Long {
        val seed = newSeed()
        return games.start { PuzzleRef.random(seed, size) }.id
    }

    suspend fun openDaily(size: Int): Long {
        val date = today()
        val id = PuzzleRef.dailyId(date, size)
        return uiState.value.inProgress
            .find { it.ref.id == id }
            ?.id ?: games.start { PuzzleRef.daily(date, size) }.id
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
