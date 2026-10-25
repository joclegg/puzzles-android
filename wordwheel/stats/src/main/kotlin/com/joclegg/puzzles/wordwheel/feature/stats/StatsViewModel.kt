package com.joclegg.puzzles.wordwheel.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.wordwheel.core.data.GameRepository
import com.joclegg.puzzles.wordwheel.core.data.LevelStats
import com.joclegg.puzzles.wordwheel.core.data.SavedGame
import com.joclegg.puzzles.wordwheel.engine.Level
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val stats: List<LevelStats> = emptyList(),
    val history: List<SavedGame> = emptyList(),
    val filter: Level? = null,
)

class StatsViewModel(
    private val games: GameRepository,
) : ViewModel() {
    private val filter = MutableStateFlow<Level?>(null)

    val uiState: StateFlow<StatsUiState> =
        combine(games.stats, games.history, filter) { stats, history, filter ->
            StatsUiState(stats, history.filter { filter == null || it.ref.level == filter }, filter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatsUiState())

    fun setFilter(value: Level?) {
        filter.value = value
    }

    suspend fun replay(game: SavedGame): Long = games.start { game.ref }.id

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
