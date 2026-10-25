package com.joclegg.puzzles.fiveletters.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.fiveletters.core.data.GameRepository
import com.joclegg.puzzles.fiveletters.core.data.WordStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(
    games: GameRepository,
) : ViewModel() {
    val uiState: StateFlow<WordStats> = games.stats.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WordStats())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
