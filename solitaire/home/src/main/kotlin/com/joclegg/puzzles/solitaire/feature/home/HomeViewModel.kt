package com.joclegg.puzzles.solitaire.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.solitaire.core.data.DealRef
import com.joclegg.puzzles.solitaire.core.data.GameRepository
import com.joclegg.puzzles.solitaire.core.data.GameStatus
import com.joclegg.puzzles.solitaire.core.data.SavedGame
import com.joclegg.puzzles.solitaire.core.data.SettingsRepository
import com.joclegg.puzzles.solitaire.engine.DrawMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val inProgress: List<SavedGame> = emptyList(),
    val dailyWon: Boolean = false,
) {
    val savedDeal: SavedGame? get() = inProgress.find { !it.deal.isDaily }
}

class HomeViewModel(
    private val games: GameRepository,
    private val settings: SettingsRepository,
    private val newSeed: () -> Long,
    private val today: () -> LocalDate,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(games.inProgress, games.history) { inProgress, history ->
            val daily = DealRef.daily(today(), DrawMode.DRAW_ONE).id
            HomeUiState(inProgress, dailyWon = history.any { it.deal.id == daily && it.status == GameStatus.WON })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    suspend fun start(): Long {
        val prefs = settings.settings.first()
        return games.start(DealRef.random(newSeed(), prefs.drawMode, prefs.order)).id
    }

    suspend fun openDaily(): Long {
        val prefs = settings.settings.first()
        val daily = DealRef.daily(today(), prefs.drawMode, prefs.order)
        return uiState.value.inProgress
            .find { it.deal.id == daily.id }
            ?.id ?: games.start(daily).id
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
