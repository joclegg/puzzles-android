package com.joclegg.puzzles.sudoku.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.sudoku.core.data.BestTime
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.GameRepository
import com.joclegg.puzzles.sudoku.core.data.SavedGame
import com.joclegg.puzzles.sudoku.core.data.SettingsRepository
import com.joclegg.puzzles.sudoku.engine.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter { CLASSIC, KILLER, CUSTOM }

data class StatsUiState(
    val bestTimes: List<BestTime> = emptyList(),
    val history: List<SavedGame> = emptyList(),
    val filter: HistoryFilter? = null,
)

class StatsViewModel(
    private val games: GameRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val filter = MutableStateFlow<HistoryFilter?>(null)

    val uiState: StateFlow<StatsUiState> =
        combine(games.bestTimes, games.history, filter) { best, history, filter ->
            StatsUiState(best, history.filter { it.matches(filter) }, filter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatsUiState())

    fun onSetPastBest(
        mode: GameMode,
        level: Difficulty,
        bestMs: Long,
    ) {
        viewModelScope.launch { games.setPastBest(mode, level, bestMs) }
    }

    fun setFilter(value: HistoryFilter?) {
        filter.value = value
    }

    suspend fun replay(game: SavedGame): Long = games.start(game.ref, settings.settings.first().mistakeCheckFor(game.ref.mode)).id

    private fun SavedGame.matches(filter: HistoryFilter?) =
        when (filter) {
            null -> true
            HistoryFilter.CUSTOM -> ref.custom
            HistoryFilter.CLASSIC -> !ref.custom && ref.mode == GameMode.CLASSIC
            HistoryFilter.KILLER -> !ref.custom && ref.mode == GameMode.KILLER
        }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
