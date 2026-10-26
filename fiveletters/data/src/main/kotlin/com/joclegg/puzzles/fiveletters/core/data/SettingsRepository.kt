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
}

data class Settings(
    val hardMode: Boolean = false,
    val highContrast: Boolean = false,
)

class SettingsRepository(
    private val store: DataStore<Preferences>,
) {
    val settings: Flow<Settings> =
        store.data.map { prefs ->
            Settings(
                hardMode = prefs[Toggle.HARD_MODE.key] ?: Toggle.HARD_MODE.default,
                highContrast = prefs[Toggle.HIGH_CONTRAST.key] ?: Toggle.HIGH_CONTRAST.default,
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
