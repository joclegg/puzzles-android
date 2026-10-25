package com.joclegg.puzzles.sudoku.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.sudoku.core.data.DailyDay
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.GameRepository
import com.joclegg.puzzles.sudoku.core.data.PuzzleBank
import com.joclegg.puzzles.sudoku.core.data.SettingsRepository
import com.joclegg.puzzles.sudoku.core.data.dailyDays
import com.joclegg.puzzles.sudoku.core.data.openDaily
import com.joclegg.puzzles.sudoku.engine.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class DailyUiState(
    val month: YearMonth,
    val today: LocalDate,
    val days: Map<LocalDate, DailyDay> = emptyMap(),
) {
    val canGoForward get() = month < YearMonth.from(today)
}

class DailyViewModel(
    private val games: GameRepository,
    private val settings: SettingsRepository,
    private val bank: PuzzleBank,
    private val today: () -> LocalDate = LocalDate::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.from(today()))

    val uiState: StateFlow<DailyUiState> =
        combine(month, games.history) { shown, history -> DailyUiState(shown, today(), dailyDays(history, zone())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DailyUiState(month.value, today()))

    fun onPreviousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun onNextMonth() {
        if (month.value < YearMonth.from(today())) {
            month.value = month.value.plusMonths(1)
        }
    }

    fun levelFor(
        mode: GameMode,
        date: LocalDate,
    ): Difficulty = bank.dailyLevel(mode, date)

    suspend fun play(
        mode: GameMode,
        date: LocalDate,
    ): Long = games.openDaily(bank, mode, date, settings.settings.first().mistakeCheckFor(mode))

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
