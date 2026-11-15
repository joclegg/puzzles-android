package com.joclegg.puzzles.crossword.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class HintStyle { POINT, FILL }

enum class Toggle(
    val read: (Settings) -> Boolean,
    val default: Boolean = false,
) {
    SHOW_TIMER(Settings::showTimer, default = true),
    SHOW_MISTAKES(Settings::showMistakes),
    ALLOW_REVEAL(Settings::allowReveal),
    SKIP_FILLED_CELLS(Settings::skipFilledCells, default = true),
    SHOW_HINTS(Settings::showHints),
}

data class Settings(
    val showTimer: Boolean = true,
    val showMistakes: Boolean = false,
    val allowReveal: Boolean = false,
    val skipFilledCells: Boolean = true,
    val showHints: Boolean = false,
    val hintStyle: HintStyle = HintStyle.POINT,
)

class SettingsRepository(
    private val store: DataStore<Preferences>,
) {
    val settings: Flow<Settings> =
        store.data.map { prefs ->
            fun on(toggle: Toggle) = prefs[toggle.key] ?: toggle.default
            Settings(
                showTimer = on(Toggle.SHOW_TIMER),
                showMistakes = on(Toggle.SHOW_MISTAKES),
                allowReveal = on(Toggle.ALLOW_REVEAL),
                skipFilledCells = on(Toggle.SKIP_FILLED_CELLS),
                showHints = on(Toggle.SHOW_HINTS),
                hintStyle = prefs[HINT_STYLE]?.let { saved -> HintStyle.entries.find { it.name == saved } } ?: HintStyle.POINT,
            )
        }

    suspend fun setHintStyle(style: HintStyle) {
        store.edit { it[HINT_STYLE] = style.name }
    }

    suspend fun keepLegacyDefaults() {
        store.edit { prefs ->
            LEGACY_TOGGLES.filter { it.key !in prefs }.forEach { prefs[it.key] = true }
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

private val HINT_STYLE = stringPreferencesKey("hint_style")

private val LEGACY_TOGGLES = listOf(Toggle.SHOW_MISTAKES, Toggle.ALLOW_REVEAL)
