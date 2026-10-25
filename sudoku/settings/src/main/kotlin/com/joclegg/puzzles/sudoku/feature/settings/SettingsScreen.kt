package com.joclegg.puzzles.sudoku.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.sudoku.core.data.DigitCounts
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.ModeSettings
import com.joclegg.puzzles.sudoku.core.data.Settings
import com.joclegg.puzzles.sudoku.core.data.Toggle
import com.joclegg.puzzles.sudoku.core.ui.BackButton
import com.joclegg.puzzles.sudoku.core.ui.SudokuIcons
import com.joclegg.puzzles.sudoku.engine.HighlightStyle
import com.joclegg.puzzles.sudoku.engine.InputMode
import com.joclegg.puzzles.sudoku.engine.MistakeCheck

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    initialMode: GameMode,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        settings = settings,
        initialMode = initialMode,
        onToggle = viewModel::onToggle,
        onInputModeChange = viewModel::onInputModeChange,
        onMistakeLevelChange = viewModel::onMistakeLevelChange,
        onDigitCountsChange = viewModel::onDigitCountsChange,
        onSameNumberStyleChange = viewModel::onSameNumberStyleChange,
        onNotesStyleChange = viewModel::onNotesStyleChange,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    settings: Settings,
    initialMode: GameMode,
    onToggle: (GameMode, Toggle, Boolean) -> Unit,
    onInputModeChange: (InputMode) -> Unit,
    onMistakeLevelChange: (GameMode, MistakeCheck) -> Unit,
    onDigitCountsChange: (GameMode, DigitCounts) -> Unit,
    onSameNumberStyleChange: (GameMode, HighlightStyle) -> Unit,
    onNotesStyleChange: (GameMode, HighlightStyle) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    val current = settings.forMode(mode)
    val toggle = { toggle: Toggle, enabled: Boolean -> onToggle(mode, toggle, enabled) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
            }
        }
        SettingsCard(title = "General") {
            Section("Entering numbers")
            InputModeChoice(settings.inputMode, onInputModeChange)
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            PrimaryTabRow(selectedTabIndex = mode.ordinal) {
                GameMode.entries.forEach { option ->
                    Tab(
                        selected = mode == option,
                        onClick = { mode = option },
                        text = { Text(modeLabel(option)) },
                        modifier = Modifier.testTag("settings_tab_${option.name.lowercase()}"),
                    )
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Section("Mistakes")
                Choice(
                    options = MistakeCheck.entries,
                    selected = current.mistakeCheck,
                    label = { mistakeLabel(it) },
                    tag = { "setting_mistakes_${it.name.lowercase()}" },
                    onSelect = { onMistakeLevelChange(mode, it) },
                )
                Hint(
                    "Block clashes refuses a number already in the row, column, box or cage. " +
                        "Show wrong turns any wrong number red. Applies to new games.",
                )
                ToggleRow(current, Toggle.SHOW_MISTAKE_COUNT, toggle, "Show mistake counter")
                Section("Highlighting")
                ToggleRow(current, Toggle.HIGHLIGHT_HOUSES, toggle, "Row, column and box", detail = "Around the selected cell")
                if (mode == GameMode.KILLER) {
                    ToggleRow(current, Toggle.HIGHLIGHT_CAGE, toggle, "Cage", detail = "The selected cell's cage")
                }
                StyleChoice("Same number", current.sameNumberStyle, "same_number", onSelect = { onSameNumberStyleChange(mode, it) })
                StyleChoice("Matching pencil marks", current.notesStyle, "notes", onSelect = { onNotesStyleChange(mode, it) })
                Hint("Cell shades the whole cell; number colours just the digit.")
                ToggleRow(
                    current,
                    Toggle.HIGHLIGHT_LAST_PLACED,
                    toggle,
                    "Highlight the cell you just filled",
                    detail = "Keep it and its row, column and box highlighted",
                )
                Section("Look")
                ToggleRow(current, Toggle.GREY_GIVEN_CELLS, toggle, "Grey starting cells")
                ToggleRow(
                    current,
                    Toggle.BOLD_PLACED_DIGITS,
                    toggle,
                    "Bold numbers you place",
                    detail = "Heavier than the starting numbers",
                )
                Section("Playing")
                ToggleRow(current, Toggle.AUTO_REMOVE_NOTES, toggle, "Clear pencil marks automatically", detail = "When you place a number")
                Text("Number counts")
                Choice(
                    options = DigitCounts.entries,
                    selected = current.digitCounts,
                    label = {
                        when (it) {
                            DigitCounts.COUNT_UP -> "Count up"
                            DigitCounts.COUNT_DOWN -> "Count down"
                            DigitCounts.OFF -> "Off"
                        }
                    },
                    tag = { "setting_counts_${it.name.lowercase()}" },
                    onSelect = { onDigitCountsChange(mode, it) },
                )
                Hint("Count up shows how many of each number are placed; count down shows how many are left.")
                ToggleRow(current, Toggle.DISABLE_COMPLETED_DIGITS, toggle, "Disable finished numbers", detail = "Once all are placed")
                ToggleRow(current, Toggle.SHOW_TIMER, toggle, "Show timer", detail = "Best times are still recorded")
            }
        }
    }
}

@Composable
private fun InputModeChoice(
    inputMode: InputMode,
    onChange: (InputMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            InputMode.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = inputMode == option,
                    onClick = { onChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, InputMode.entries.size),
                    modifier = Modifier.testTag("setting_${option.name.lowercase()}"),
                    icon = {
                        Icon(
                            if (option == InputMode.CELL_FIRST) {
                                SudokuIcons.CellFirst
                            } else {
                                SudokuIcons.NumberFirst
                            },
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                        )
                    },
                ) {
                    Text(
                        if (option == InputMode.CELL_FIRST) {
                            "Cell first"
                        } else {
                            "Number first"
                        },
                    )
                }
            }
        }
        Hint(
            if (inputMode == InputMode.CELL_FIRST) {
                "Tap a cell, then pick its number. Try number first: it's faster once you're used to it. Applies to both modes."
            } else {
                "Pick a number, then tap every cell it goes in. Applies to both modes."
            },
        )
    }
}

@Composable
private fun <T> Choice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    tag: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                modifier = Modifier.testTag(tag(option)),
                icon = {},
            ) { Text(label(option), maxLines = 1) }
        }
    }
}

@Composable
private fun StyleChoice(
    title: String,
    selected: HighlightStyle,
    tagName: String,
    onSelect: (HighlightStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title)
        Choice(
            options = HighlightStyle.entries,
            selected = selected,
            label = {
                when (it) {
                    HighlightStyle.CELL -> "Cell"
                    HighlightStyle.NUMBER -> "Number"
                    HighlightStyle.OFF -> "Off"
                }
            },
            tag = { "setting_${tagName}_${it.name.lowercase()}" },
            onSelect = onSelect,
        )
    }
}

private fun modeLabel(mode: GameMode) =
    when (mode) {
        GameMode.CLASSIC -> "Classic"
        GameMode.KILLER -> "Killer"
    }

private fun mistakeLabel(level: MistakeCheck) =
    when (level) {
        MistakeCheck.OFF -> "Off"
        MistakeCheck.BLOCK_CLASHES -> "Block clashes"
        MistakeCheck.SHOW_WRONG -> "Show wrong"
    }

@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun Section(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 8.dp),
    )
}

@Composable
private fun Hint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
private fun ToggleRow(
    settings: ModeSettings,
    toggle: Toggle,
    onToggle: (Toggle, Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            if (detail != null) {
                Hint(detail)
            }
        }
        Switch(
            checked = toggle.read(settings),
            onCheckedChange = { onToggle(toggle, it) },
            modifier = Modifier.testTag("setting_${toggle.name.lowercase()}"),
        )
    }
}
