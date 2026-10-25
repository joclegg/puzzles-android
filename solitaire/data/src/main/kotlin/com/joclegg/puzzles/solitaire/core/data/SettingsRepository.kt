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

enum class Toggle(
    val read: (Settings) -> Boolean,
    val default: Boolean = true,
) {
    REVERSE(Settings::reverse, default = false),
    ONE_TAP_MOVE(Settings::oneTapMove),
    SHOW_TIMER(Settings::showTimer),
    SHOW_MOVE_COUNT(Settings::showMoveCount),
}

data class Settings(
    val drawMode: DrawMode = DrawMode.DRAW_ONE,
    val reverse: Boolean = false,
    val oneTapMove: Boolean = true,
    val showTimer: Boolean = true,
    val showMoveCount: Boolean = true,
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
                oneTapMove = prefs[Toggle.ONE_TAP_MOVE.key] ?: true,
                showTimer = prefs[Toggle.SHOW_TIMER.key] ?: true,
                showMoveCount = prefs[Toggle.SHOW_MOVE_COUNT.key] ?: true,
            )
        }

    suspend fun setDrawMode(drawMode: DrawMode) {
        store.edit { it[DRAW_MODE] = drawMode.name }
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
