package com.joclegg.puzzles.fiveletters.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class Toggle(
    val read: (Settings) -> Boolean,
    val default: Boolean,
) {
    HARD_MODE(Settings::hardMode, default = false),
    HIGH_CONTRAST(Settings::highContrast, default = false),
    SHOW_HINTS(Settings::showHints, default = true),
    SHOW_TUTORIAL(Settings::showTutorial, default = true),
    ALLOW_REVEAL(Settings::allowReveal, default = true),
}

data class Settings(
    val hardMode: Boolean = false,
    val highContrast: Boolean = false,
    val showHints: Boolean = true,
    val showTutorial: Boolean = true,
    val allowReveal: Boolean = true,
)

class SettingsRepository(
    private val store: DataStore<Preferences>,
) {
    val settings: Flow<Settings> =
        store.data.map { prefs ->
            Settings(
                hardMode = prefs[Toggle.HARD_MODE.key] ?: Toggle.HARD_MODE.default,
                highContrast = prefs[Toggle.HIGH_CONTRAST.key] ?: Toggle.HIGH_CONTRAST.default,
                showHints = prefs[Toggle.SHOW_HINTS.key] ?: true,
                showTutorial = prefs[Toggle.SHOW_TUTORIAL.key] ?: true,
                allowReveal = prefs[Toggle.ALLOW_REVEAL.key] ?: true,
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
