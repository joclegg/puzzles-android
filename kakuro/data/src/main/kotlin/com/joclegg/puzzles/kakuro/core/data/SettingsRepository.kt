package com.joclegg.puzzles.kakuro.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.joclegg.puzzles.kakuro.engine.InputMode
import com.joclegg.puzzles.kakuro.engine.MistakeCheck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class Toggle(
    val read: (Settings) -> Boolean,
    val default: Boolean = true,
) {
    SHOW_MISTAKE_COUNT(Settings::showMistakeCount),
    HIGHLIGHT_RUNS(Settings::highlightRuns),
    SHOW_TIMER(Settings::showTimer),
    SHOW_COMBINATIONS(Settings::showCombinations, default = false),
}

data class Settings(
    val inputMode: InputMode = InputMode.CELL_FIRST,
    val mistakeCheck: MistakeCheck = MistakeCheck.SHOW_WRONG,
    val showMistakeCount: Boolean = true,
    val highlightRuns: Boolean = true,
    val showTimer: Boolean = true,
    val showCombinations: Boolean = false,
)

class SettingsRepository(
    private val store: DataStore<Preferences>,
) {
    val settings: Flow<Settings> =
        store.data.map { prefs ->
            fun on(toggle: Toggle) = prefs[toggle.key] ?: toggle.default
            Settings(
                inputMode = prefs[INPUT_MODE]?.let { saved -> InputMode.entries.find { it.name == saved } } ?: InputMode.CELL_FIRST,
                mistakeCheck =
                    prefs[MISTAKE_CHECK]?.let { saved -> MistakeCheck.entries.find { it.name == saved } } ?: MistakeCheck.SHOW_WRONG,
                showMistakeCount = on(Toggle.SHOW_MISTAKE_COUNT),
                highlightRuns = on(Toggle.HIGHLIGHT_RUNS),
                showTimer = on(Toggle.SHOW_TIMER),
                showCombinations = on(Toggle.SHOW_COMBINATIONS),
            )
        }

    suspend fun setInputMode(mode: InputMode) {
        store.edit { it[INPUT_MODE] = mode.name }
    }

    suspend fun setMistakeCheck(level: MistakeCheck) {
        store.edit { it[MISTAKE_CHECK] = level.name }
    }

    suspend fun set(
        toggle: Toggle,
        enabled: Boolean,
    ) {
        store.edit { it[toggle.key] = enabled }
    }
}

private val Toggle.key get() = booleanPreferencesKey(name.lowercase())

private val INPUT_MODE = stringPreferencesKey("input_mode")
private val MISTAKE_CHECK = stringPreferencesKey("mistake_check")
