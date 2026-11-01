package com.joclegg.puzzles.solitaire.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.joclegg.puzzles.solitaire.engine.BuildOrder
import com.joclegg.puzzles.solitaire.engine.DrawMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class HintStyle { POINT, FILL }

enum class Toggle(
    val read: (Settings) -> Boolean,
    val default: Boolean = true,
) {
    REVERSE(Settings::reverse, default = false),
    ONE_TAP_MOVE(Settings::oneTapMove),
    WINNABLE_ONLY(Settings::winnableOnly),
    WARN_UNWINNABLE(Settings::warnUnwinnable, default = false),
    SHOW_TIMER(Settings::showTimer),
    SHOW_MOVE_COUNT(Settings::showMoveCount),
    SHOW_HINTS(Settings::showHints, default = false),
    SHOW_TUTORIAL(Settings::showTutorial),
    ALLOW_REVEAL(Settings::allowReveal, default = false),
}

data class Settings(
    val drawMode: DrawMode = DrawMode.DRAW_ONE,
    val reverse: Boolean = false,
    val oneTapMove: Boolean = true,
    val winnableOnly: Boolean = true,
    val warnUnwinnable: Boolean = false,
    val showTimer: Boolean = true,
    val showMoveCount: Boolean = true,
    val showHints: Boolean = false,
    val showTutorial: Boolean = true,
    val allowReveal: Boolean = false,
    val hintStyle: HintStyle = HintStyle.POINT,
) {
    val order: BuildOrder
        get() =
            if (reverse) {
                BuildOrder.UP
            } else {
                BuildOrder.DOWN
            }
}

class SettingsRepository(
    private val store: DataStore<Preferences>,
) {
    val settings: Flow<Settings> =
        store.data.map { prefs ->
            Settings(
                drawMode = prefs[DRAW_MODE]?.let { saved -> DrawMode.entries.find { it.name == saved } } ?: DrawMode.DRAW_ONE,
                reverse = prefs[Toggle.REVERSE.key] ?: Toggle.REVERSE.default,
                oneTapMove = prefs[Toggle.ONE_TAP_MOVE.key] ?: Toggle.ONE_TAP_MOVE.default,
                winnableOnly = prefs[Toggle.WINNABLE_ONLY.key] ?: Toggle.WINNABLE_ONLY.default,
                warnUnwinnable = prefs[Toggle.WARN_UNWINNABLE.key] ?: Toggle.WARN_UNWINNABLE.default,
                showTimer = prefs[Toggle.SHOW_TIMER.key] ?: Toggle.SHOW_TIMER.default,
                showMoveCount = prefs[Toggle.SHOW_MOVE_COUNT.key] ?: Toggle.SHOW_MOVE_COUNT.default,
                showHints = prefs[Toggle.SHOW_HINTS.key] ?: Toggle.SHOW_HINTS.default,
                showTutorial = prefs[Toggle.SHOW_TUTORIAL.key] ?: Toggle.SHOW_TUTORIAL.default,
                allowReveal = prefs[Toggle.ALLOW_REVEAL.key] ?: Toggle.ALLOW_REVEAL.default,
                hintStyle = prefs[HINT_STYLE]?.let { saved -> HintStyle.entries.find { it.name == saved } } ?: HintStyle.POINT,
            )
        }

    suspend fun setDrawMode(drawMode: DrawMode) {
        store.edit { it[DRAW_MODE] = drawMode.name }
    }

    suspend fun setHintStyle(style: HintStyle) {
        store.edit { it[HINT_STYLE] = style.name }
    }

    suspend fun keepLegacyDefaults() {
        store.edit { prefs ->
            listOf(Toggle.WARN_UNWINNABLE, Toggle.SHOW_HINTS, Toggle.ALLOW_REVEAL).forEach { toggle ->
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

    private companion object {
        val DRAW_MODE = stringPreferencesKey("draw_mode")
    }
}

private val Toggle.key get() = booleanPreferencesKey(name.lowercase())

private val HINT_STYLE = stringPreferencesKey("hint_style")
