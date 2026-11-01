package com.joclegg.puzzles.sightlines.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.sightlines.core.data.InputMode
import com.joclegg.puzzles.sightlines.core.data.Settings
import com.joclegg.puzzles.sightlines.core.data.SettingsRepository
import com.joclegg.puzzles.sightlines.core.data.Toggle
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

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
