package com.joclegg.puzzles.kakuro.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.kakuro.core.data.GameRepository
import com.joclegg.puzzles.kakuro.core.data.SavedGame
import com.joclegg.puzzles.kakuro.core.data.SizeStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val stats: List<SizeStats> = emptyList(),
    val history: List<SavedGame> = emptyList(),
    val filter: Int? = null,
)

class StatsViewModel(
    private val games: GameRepository,
) : ViewModel() {
    private val filter = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<StatsUiState> =
        combine(games.stats, games.history, filter) { stats, history, filter ->
            StatsUiState(stats, history.filter { filter == null || it.ref.size == filter }, filter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatsUiState())

    fun setFilter(value: Int?) {
        filter.value = value
    }

    suspend fun replay(game: SavedGame): Long = games.start { game.ref }.id

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
