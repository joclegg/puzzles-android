package com.joclegg.puzzles.wordwheel.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class Toggle(
    val read: (Settings) -> Boolean,
) {
    SHOW_TIMER(Settings::showTimer),
    SHOW_BONUS_COUNT(Settings::showBonusCount),
    SHOW_HINTS(Settings::showHints),
    SHOW_TUTORIAL(Settings::showTutorial),
    ALLOW_REVEAL(Settings::allowReveal),
}

data class Settings(
    val showTimer: Boolean = true,
    val showBonusCount: Boolean = true,
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
                showTimer = prefs[Toggle.SHOW_TIMER.key] ?: true,
                showBonusCount = prefs[Toggle.SHOW_BONUS_COUNT.key] ?: true,
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
