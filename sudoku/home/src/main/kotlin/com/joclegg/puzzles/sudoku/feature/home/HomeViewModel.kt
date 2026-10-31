package com.joclegg.puzzles.sudoku.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.sudoku.core.data.DailyIds
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.GameRepository
import com.joclegg.puzzles.sudoku.core.data.GameStatus
import com.joclegg.puzzles.sudoku.core.data.PuzzleBank
import com.joclegg.puzzles.sudoku.core.data.PuzzleRef
import com.joclegg.puzzles.sudoku.core.data.PuzzleSupply
import com.joclegg.puzzles.sudoku.core.data.SavedGame
import com.joclegg.puzzles.sudoku.core.data.SettingsRepository
import com.joclegg.puzzles.sudoku.core.data.openDaily
import com.joclegg.puzzles.sudoku.engine.Difficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class HomeUiState(
    val inProgress: List<SavedGame> = emptyList(),
    val dailyDone: Set<String> = emptySet(),
    val today: LocalDate = LocalDate.now(),
) {
    fun dailyDone(
        mode: GameMode,
        difficulty: Difficulty,
    ) = DailyIds.of(mode, difficulty, today) in dailyDone

    fun savedFor(
        mode: GameMode,
        difficulty: Difficulty,
    ): SavedGame? = inProgress.find { it.ref.slot == PuzzleRef.levelSlot(mode, difficulty) }
}

class HomeViewModel(
    private val games: GameRepository,
    private val settings: SettingsRepository,
    private val bank: PuzzleBank,
    private val supply: PuzzleSupply,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(games.inProgress, settings.settings, games.history) { inProgress, prefs, history ->
            HomeUiState(
                inProgress = inProgress,
                dailyDone = history.filter { it.status == GameStatus.COMPLETED }.map { it.ref.id }.toSet(),
                today = today(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    suspend fun start(
        mode: GameMode,
        difficulty: Difficulty,
    ): Long {
        val ref = supply.next(mode, difficulty)
        return games.start(ref, settings.settings.first().mistakeCheckFor(mode)).id
    }

    suspend fun startDaily(
        mode: GameMode,
        difficulty: Difficulty,
    ): Long = games.openDaily(bank, mode, difficulty, today(), settings.settings.first().mistakeCheckFor(mode))

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
