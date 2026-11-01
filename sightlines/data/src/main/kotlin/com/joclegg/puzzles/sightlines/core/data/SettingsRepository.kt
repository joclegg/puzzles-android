package com.joclegg.puzzles.sightlines.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class InputMode { TAP_TO_CYCLE, PICK_COLOUR }

enum class Toggle(
    val read: (Settings) -> Boolean,
) {
    MARK_BROKEN_RULES(Settings::markBrokenRules),
    SHOW_TIMER(Settings::showTimer),
    SHOW_PROGRESS(Settings::showProgress),
    SHOW_SEEN_COUNT(Settings::showSeenCount),
    SHOW_HINTS(Settings::showHints),
    SHOW_TUTORIAL(Settings::showTutorial),
    ALLOW_REVEAL(Settings::allowReveal),
}

data class Settings(
    val inputMode: InputMode = InputMode.TAP_TO_CYCLE,
    val markBrokenRules: Boolean = false,
    val showTimer: Boolean = true,
    val showProgress: Boolean = false,
    val showSeenCount: Boolean = false,
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
                inputMode = prefs[INPUT_MODE]?.let { saved -> InputMode.entries.find { it.name == saved } } ?: InputMode.TAP_TO_CYCLE,
                markBrokenRules = prefs[Toggle.MARK_BROKEN_RULES.key] ?: false,
                showTimer = prefs[Toggle.SHOW_TIMER.key] ?: true,
                showProgress = prefs[Toggle.SHOW_PROGRESS.key] ?: false,
                showSeenCount = prefs[Toggle.SHOW_SEEN_COUNT.key] ?: false,
                showHints = prefs[Toggle.SHOW_HINTS.key] ?: false,
                showTutorial = prefs[Toggle.SHOW_TUTORIAL.key] ?: true,
                allowReveal = prefs[Toggle.ALLOW_REVEAL.key] ?: false,
            )
        }

    suspend fun setInputMode(mode: InputMode) {
        store.edit { it[INPUT_MODE] = mode.name }
    }

    suspend fun keepLegacyDefaults() {
        store.edit { prefs ->
            listOf(Toggle.MARK_BROKEN_RULES, Toggle.SHOW_PROGRESS, Toggle.SHOW_SEEN_COUNT, Toggle.SHOW_HINTS, Toggle.ALLOW_REVEAL).forEach { toggle ->
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

private val INPUT_MODE = stringPreferencesKey("input_mode")
