package com.joclegg.puzzles.sudoku.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.sudoku.core.data.DigitCounts
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.HintStyle
import com.joclegg.puzzles.sudoku.core.data.Settings
import com.joclegg.puzzles.sudoku.core.data.SettingsRepository
import com.joclegg.puzzles.sudoku.core.data.Toggle
import com.joclegg.puzzles.sudoku.engine.HighlightStyle
import com.joclegg.puzzles.sudoku.engine.InputMode
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<Settings> =
        settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), Settings())

    fun onToggle(
        mode: GameMode,
        toggle: Toggle,
        enabled: Boolean,
    ) {
        viewModelScope.launch { settings.set(mode, toggle, enabled) }
    }

    fun onMistakeLevelChange(
        mode: GameMode,
        level: MistakeCheck,
    ) {
        viewModelScope.launch { settings.setMistakeCheck(mode, level) }
    }

    fun onHintStyleChange(
        mode: GameMode,
        style: HintStyle,
    ) {
        viewModelScope.launch { settings.setHintStyle(mode, style) }
    }

    fun onDigitCountsChange(
        mode: GameMode,
        counts: DigitCounts,
    ) {
        viewModelScope.launch { settings.setDigitCounts(mode, counts) }
    }

    fun onSameNumberStyleChange(
        mode: GameMode,
        style: HighlightStyle,
    ) {
        viewModelScope.launch { settings.setSameNumberStyle(mode, style) }
    }

    fun onNotesStyleChange(
        mode: GameMode,
        style: HighlightStyle,
    ) {
        viewModelScope.launch { settings.setNotesStyle(mode, style) }
    }

    fun onInputModeChange(mode: InputMode) {
        viewModelScope.launch { settings.setInputMode(mode) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
