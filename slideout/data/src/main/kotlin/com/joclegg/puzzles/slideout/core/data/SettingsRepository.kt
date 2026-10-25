package com.joclegg.puzzles.slideout.core.data

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
    SHOW_PAR(Settings::showPar),
}

data class Settings(
    val showTimer: Boolean = true,
    val showPar: Boolean = true,
)

class SettingsRepository(
    private val store: DataStore<Preferences>,
) {
    val settings: Flow<Settings> =
        store.data.map { prefs ->
            Settings(
                showTimer = prefs[Toggle.SHOW_TIMER.key] ?: true,
                showPar = prefs[Toggle.SHOW_PAR.key] ?: true,
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
