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
    val markBrokenRules: Boolean = true,
    val showTimer: Boolean = true,
    val showProgress: Boolean = true,
    val showSeenCount: Boolean = true,
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
                inputMode = prefs[INPUT_MODE]?.let { saved -> InputMode.entries.find { it.name == saved } } ?: InputMode.TAP_TO_CYCLE,
                markBrokenRules = prefs[Toggle.MARK_BROKEN_RULES.key] ?: true,
                showTimer = prefs[Toggle.SHOW_TIMER.key] ?: true,
                showProgress = prefs[Toggle.SHOW_PROGRESS.key] ?: true,
                showSeenCount = prefs[Toggle.SHOW_SEEN_COUNT.key] ?: true,
                showHints = prefs[Toggle.SHOW_HINTS.key] ?: true,
                showTutorial = prefs[Toggle.SHOW_TUTORIAL.key] ?: true,
                allowReveal = prefs[Toggle.ALLOW_REVEAL.key] ?: true,
            )
        }

    suspend fun setInputMode(mode: InputMode) {
        store.edit { it[INPUT_MODE] = mode.name }
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
