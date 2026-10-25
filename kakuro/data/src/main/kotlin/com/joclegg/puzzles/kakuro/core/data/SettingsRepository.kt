package com.joclegg.puzzles.kakuro.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class Toggle(
    val read: (Settings) -> Boolean,
    val default: Boolean = true,
) {
    SHOW_MISTAKES(Settings::showMistakes),
    HIGHLIGHT_RUNS(Settings::highlightRuns),
    SHOW_TIMER(Settings::showTimer),
    SHOW_COMBINATIONS(Settings::showCombinations, default = false),
}

data class Settings(
    val showMistakes: Boolean = true,
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
                showMistakes = on(Toggle.SHOW_MISTAKES),
                highlightRuns = on(Toggle.HIGHLIGHT_RUNS),
                showTimer = on(Toggle.SHOW_TIMER),
                showCombinations = on(Toggle.SHOW_COMBINATIONS),
            )
        }

    suspend fun set(
        toggle: Toggle,
        enabled: Boolean,
    ) {
        store.edit { it[toggle.key] = enabled }
    }
}

private val Toggle.key get() = booleanPreferencesKey(name.lowercase())
