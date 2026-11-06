package com.joclegg.puzzles.sudoku.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.joclegg.puzzles.core.data.CombinationOrder
import com.joclegg.puzzles.sudoku.engine.HighlightStyle
import com.joclegg.puzzles.sudoku.engine.InputMode
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import com.joclegg.puzzles.sudoku.engine.PlayOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class DigitCounts { COUNT_UP, COUNT_DOWN, OFF }

enum class HintStyle { POINT, FILL }

data class ModeSettings(
    val mistakeCheck: MistakeCheck = MistakeCheck.OFF,
    val showMistakeCount: Boolean = false,
    val highlightHouses: Boolean = false,
    val highlightCage: Boolean = false,
    val sameNumberStyle: HighlightStyle = HighlightStyle.OFF,
    val notesStyle: HighlightStyle = HighlightStyle.OFF,
    val autoRemoveNotes: Boolean = false,
    val digitCounts: DigitCounts = DigitCounts.OFF,
    val disableCompletedDigits: Boolean = false,
    val showTimer: Boolean = true,
    val boldPlacedDigits: Boolean = true,
    val highlightLastPlaced: Boolean = false,
    val greyGivenCells: Boolean = true,
    val showHints: Boolean = false,
    val showTutorial: Boolean = true,
    val allowReveal: Boolean = false,
    val hintStyle: HintStyle = HintStyle.POINT,
    val showCombinations: Boolean = false,
    val combinationOrder: CombinationOrder = CombinationOrder.INCREASING,
) {
    val playOptions get() = PlayOptions(highlightHouses, highlightCage, sameNumberStyle, notesStyle, autoRemoveNotes, highlightLastPlaced)
}

data class Settings(
    val inputMode: InputMode = InputMode.CELL_FIRST,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val classic: ModeSettings = ModeSettings(),
    val killer: ModeSettings = ModeSettings(greyGivenCells = Toggle.GREY_GIVEN_CELLS.defaultFor(GameMode.KILLER)),
) {
    fun forMode(mode: GameMode) =
        if (mode == GameMode.KILLER) {
            killer
        } else {
            classic
        }

    fun mistakeCheckFor(mode: GameMode) = forMode(mode).mistakeCheck
}

enum class Toggle(
    val read: (ModeSettings) -> Boolean,
    private val default: Boolean = false,
    private val killerDefault: Boolean = default,
    val killerOnly: Boolean = false,
) {
    SHOW_MISTAKE_COUNT(ModeSettings::showMistakeCount),
    HIGHLIGHT_HOUSES(ModeSettings::highlightHouses),
    HIGHLIGHT_CAGE(ModeSettings::highlightCage, killerOnly = true),
    AUTO_REMOVE_NOTES(ModeSettings::autoRemoveNotes),
    DISABLE_COMPLETED_DIGITS(ModeSettings::disableCompletedDigits),
    SHOW_TIMER(ModeSettings::showTimer, default = true),
    BOLD_PLACED_DIGITS(ModeSettings::boldPlacedDigits, default = true),
    HIGHLIGHT_LAST_PLACED(ModeSettings::highlightLastPlaced),
    GREY_GIVEN_CELLS(ModeSettings::greyGivenCells, default = true, killerDefault = false),
    SHOW_HINTS(ModeSettings::showHints),
    SHOW_TUTORIAL(ModeSettings::showTutorial, default = true),
    ALLOW_REVEAL(ModeSettings::allowReveal),
    SHOW_COMBINATIONS(ModeSettings::showCombinations, killerOnly = true),
    ;

    fun defaultFor(mode: GameMode) =
        if (mode == GameMode.KILLER) {
            killerDefault
        } else {
            default
        }
}

class SettingsRepository(
    private val store: DataStore<Preferences>,
) {
    val settings: Flow<Settings> =
        store.data.map { prefs ->
            Settings(
                inputMode = prefs[INPUT_MODE]?.let { runCatching { InputMode.valueOf(it) }.getOrNull() } ?: InputMode.CELL_FIRST,
                theme = prefs[THEME]?.let { saved -> ThemeMode.entries.find { it.name == saved } } ?: ThemeMode.SYSTEM,
                classic = prefs.modeSettings(GameMode.CLASSIC),
                killer = prefs.modeSettings(GameMode.KILLER),
            )
        }

    val customDraft: Flow<String?> = store.data.map { it[CUSTOM_DRAFT] }

    suspend fun setTheme(theme: ThemeMode) {
        store.edit { it[THEME] = theme.name }
    }

    suspend fun setInputMode(mode: InputMode) {
        store.edit { it[INPUT_MODE] = mode.name }
    }

    suspend fun setMistakeCheck(
        mode: GameMode,
        level: MistakeCheck,
    ) {
        store.edit { it[mistakeKey(mode)] = level.name }
    }

    suspend fun setSameNumberStyle(
        mode: GameMode,
        style: HighlightStyle,
    ) {
        store.edit { it[styleKey("same_number", mode)] = style.name }
    }

    suspend fun setNotesStyle(
        mode: GameMode,
        style: HighlightStyle,
    ) {
        store.edit { it[styleKey("notes", mode)] = style.name }
    }

    suspend fun setHintStyle(
        mode: GameMode,
        style: HintStyle,
    ) {
        store.edit { it[hintStyleKey(mode)] = style.name }
    }

    suspend fun setCombinationOrder(
        mode: GameMode,
        order: CombinationOrder,
    ) {
        store.edit { it[combinationOrderKey(mode)] = order.name }
    }

    suspend fun setDigitCounts(
        mode: GameMode,
        counts: DigitCounts,
    ) {
        store.edit { it[digitCountsKey(mode)] = counts.name }
    }

    suspend fun keepLegacyDefaults() {
        store.edit { prefs ->
            GameMode.entries.forEach { mode ->
                LEGACY_TOGGLES.forEach { toggle ->
                    if (toggle.key(mode) !in prefs) {
                        prefs[toggle.key(mode)] = true
                    }
                }
                if (mistakeKey(mode) !in prefs) {
                    prefs[mistakeKey(mode)] = MistakeCheck.SHOW_WRONG.name
                }
                listOf("same_number", "notes").forEach { name ->
                    if (styleKey(name, mode) !in prefs) {
                        prefs[styleKey(name, mode)] = HighlightStyle.CELL.name
                    }
                }
                if (digitCountsKey(mode) !in prefs) {
                    prefs[digitCountsKey(mode)] = DigitCounts.COUNT_UP.name
                }
            }
        }
    }

    suspend fun set(
        mode: GameMode,
        toggle: Toggle,
        enabled: Boolean,
    ) {
        store.edit { it[toggle.key(mode)] = enabled }
    }

    suspend fun setCustomDraft(draft: String?) {
        store.edit {
            if (draft == null) {
                it.remove(CUSTOM_DRAFT)
            } else {
                it[CUSTOM_DRAFT] = draft
            }
        }
    }

    private companion object {
        val LEGACY_TOGGLES =
            listOf(
                Toggle.SHOW_MISTAKE_COUNT,
                Toggle.HIGHLIGHT_HOUSES,
                Toggle.HIGHLIGHT_CAGE,
                Toggle.AUTO_REMOVE_NOTES,
                Toggle.DISABLE_COMPLETED_DIGITS,
                Toggle.HIGHLIGHT_LAST_PLACED,
                Toggle.SHOW_HINTS,
                Toggle.ALLOW_REVEAL,
            )
        val INPUT_MODE = stringPreferencesKey("input_mode")
        val THEME = stringPreferencesKey("theme")
        val CUSTOM_DRAFT = stringPreferencesKey("custom_draft")
    }
}

private fun Toggle.key(mode: GameMode) = booleanPreferencesKey("${name.lowercase()}_${mode.name.lowercase()}")

private fun styleKey(
    name: String,
    mode: GameMode,
) = stringPreferencesKey("highlight_${name}_${mode.name.lowercase()}")

private fun digitCountsKey(mode: GameMode) = stringPreferencesKey("digit_counts_${mode.name.lowercase()}")

private fun hintStyleKey(mode: GameMode) = stringPreferencesKey("hint_style_${mode.name.lowercase()}")

private fun combinationOrderKey(mode: GameMode) = stringPreferencesKey("combination_order_${mode.name.lowercase()}")

private fun mistakeKey(mode: GameMode) = stringPreferencesKey("mistakes_${mode.name.lowercase()}")

private fun Preferences.modeSettings(mode: GameMode): ModeSettings {
    fun on(toggle: Toggle) = this[toggle.key(mode)] ?: toggle.defaultFor(mode)

    fun style(name: String) =
        this[styleKey(name, mode)]?.let { saved -> HighlightStyle.entries.find { it.name == saved } } ?: HighlightStyle.OFF
    return ModeSettings(
        mistakeCheck = this[mistakeKey(mode)]?.let { saved -> MistakeCheck.entries.find { it.name == saved } } ?: MistakeCheck.OFF,
        showMistakeCount = on(Toggle.SHOW_MISTAKE_COUNT),
        highlightHouses = on(Toggle.HIGHLIGHT_HOUSES),
        highlightCage = on(Toggle.HIGHLIGHT_CAGE),
        sameNumberStyle = style("same_number"),
        notesStyle = style("notes"),
        autoRemoveNotes = on(Toggle.AUTO_REMOVE_NOTES),
        digitCounts = this[digitCountsKey(mode)]?.let { saved -> DigitCounts.entries.find { it.name == saved } } ?: DigitCounts.OFF,
        disableCompletedDigits = on(Toggle.DISABLE_COMPLETED_DIGITS),
        showTimer = on(Toggle.SHOW_TIMER),
        boldPlacedDigits = on(Toggle.BOLD_PLACED_DIGITS),
        highlightLastPlaced = on(Toggle.HIGHLIGHT_LAST_PLACED),
        greyGivenCells = on(Toggle.GREY_GIVEN_CELLS),
        showHints = on(Toggle.SHOW_HINTS),
        showTutorial = on(Toggle.SHOW_TUTORIAL),
        allowReveal = on(Toggle.ALLOW_REVEAL),
        hintStyle = this[hintStyleKey(mode)]?.let { saved -> HintStyle.entries.find { it.name == saved } } ?: HintStyle.POINT,
        showCombinations = on(Toggle.SHOW_COMBINATIONS),
        combinationOrder =
            this[combinationOrderKey(mode)]?.let { saved -> CombinationOrder.entries.find { it.name == saved } }
                ?: CombinationOrder.INCREASING,
    )
}
