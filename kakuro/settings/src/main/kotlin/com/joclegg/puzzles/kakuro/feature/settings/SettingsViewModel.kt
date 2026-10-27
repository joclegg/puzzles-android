package com.joclegg.puzzles.kakuro.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.kakuro.core.data.HintStyle
import com.joclegg.puzzles.kakuro.core.data.Settings
import com.joclegg.puzzles.kakuro.core.data.SettingsRepository
import com.joclegg.puzzles.kakuro.core.data.Toggle
import com.joclegg.puzzles.kakuro.engine.InputMode
import com.joclegg.puzzles.kakuro.engine.MistakeCheck
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
        toggle: Toggle,
        enabled: Boolean,
    ) {
        viewModelScope.launch { settings.set(toggle, enabled) }
    }

    fun onInputModeChange(mode: InputMode) {
        viewModelScope.launch { settings.setInputMode(mode) }
    }

    fun onMistakeCheckChange(level: MistakeCheck) {
        viewModelScope.launch { settings.setMistakeCheck(level) }
    }

    fun onHintStyleChange(style: HintStyle) {
        viewModelScope.launch { settings.setHintStyle(style) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
