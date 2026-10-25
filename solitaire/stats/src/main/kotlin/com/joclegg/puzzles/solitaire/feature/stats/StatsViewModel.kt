package com.joclegg.puzzles.solitaire.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.solitaire.core.data.DrawStats
import com.joclegg.puzzles.solitaire.core.data.GameRepository
import com.joclegg.puzzles.solitaire.core.data.SavedGame
import com.joclegg.puzzles.solitaire.engine.DrawMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val stats: List<DrawStats> = emptyList(),
    val history: List<SavedGame> = emptyList(),
    val filter: DrawMode? = null,
)

class StatsViewModel(
    private val games: GameRepository,
) : ViewModel() {
    private val filter = MutableStateFlow<DrawMode?>(null)

    val uiState: StateFlow<StatsUiState> =
        combine(games.stats, games.history, filter) { stats, history, filter ->
            StatsUiState(stats, history.filter { filter == null || it.deal.drawMode == filter }, filter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatsUiState())

    fun setFilter(value: DrawMode?) {
        filter.value = value
    }

    suspend fun replay(game: SavedGame): Long = games.start(game.deal).id

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
