package com.joclegg.puzzles.solitaire.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.solitaire.core.data.DealRef
import com.joclegg.puzzles.solitaire.core.data.GameRepository
import com.joclegg.puzzles.solitaire.core.data.GameStatus
import com.joclegg.puzzles.solitaire.core.data.SavedGame
import com.joclegg.puzzles.solitaire.core.data.SettingsRepository
import com.joclegg.puzzles.solitaire.core.data.WinnableDeals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
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
    private val winnable: WinnableDeals,
    private val background: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(games.inProgress, games.dailies) { inProgress, dailies ->
            val daily = DealRef.dailyId(today())
            HomeUiState(inProgress, dailyWon = dailies.any { it.puzzleId == daily && it.status == GameStatus.WON.name })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    suspend fun start(): Long {
        val prefs = settings.settings.first()
        val seed = newSeed()
        return games
            .start(
                withContext(background) {
                    DealRef.random(seed, prefs.drawMode, prefs.order, winnable.takeIf { prefs.winnableOnly })
                },
            ).id
    }

    suspend fun openDaily(): Long {
        val prefs = settings.settings.first()
        val date = today()
        val existing = uiState.value.inProgress.find { it.deal.id == DealRef.dailyId(date) }
        if (existing != null) {
            return existing.id
        }
        return games
            .start(
                withContext(background) {
                    DealRef.daily(date, prefs.drawMode, prefs.order, winnable.takeIf { prefs.winnableOnly })
                },
            ).id
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
