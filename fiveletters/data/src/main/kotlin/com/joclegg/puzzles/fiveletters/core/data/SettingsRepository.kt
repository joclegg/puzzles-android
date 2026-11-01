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
    SHOW_HINTS(Settings::showHints, default = false),
    SHOW_TUTORIAL(Settings::showTutorial, default = true),
    ALLOW_REVEAL(Settings::allowReveal, default = false),
}

data class Settings(
    val hardMode: Boolean = false,
    val highContrast: Boolean = false,
    val showHints: Boolean = false,
    val showTutorial: Boolean = true,
    val allowReveal: Boolean = false,
)

class SettingsRepository(
    private val store: DataStore<Preferences>,
) {
    val settings: Flow<Settings> =
        store.data.map { prefs ->
            Settings(
                hardMode = prefs[Toggle.HARD_MODE.key] ?: Toggle.HARD_MODE.default,
                highContrast = prefs[Toggle.HIGH_CONTRAST.key] ?: Toggle.HIGH_CONTRAST.default,
                showHints = prefs[Toggle.SHOW_HINTS.key] ?: false,
                showTutorial = prefs[Toggle.SHOW_TUTORIAL.key] ?: true,
                allowReveal = prefs[Toggle.ALLOW_REVEAL.key] ?: false,
            )
        }

    suspend fun keepLegacyDefaults() {
        store.edit { prefs ->
            listOf(Toggle.SHOW_HINTS, Toggle.ALLOW_REVEAL).forEach { toggle ->
                if (toggle.key !in prefs) {
                    prefs[toggle.key] = true
                }
            }
        }
    }

    suspend fun set(
        toggle: Toggle,
        enabled: Boolean,
    ) {
        store.edit { it[toggle.key] = enabled }
    }
}

private val Toggle.key get() = booleanPreferencesKey(name.lowercase())
