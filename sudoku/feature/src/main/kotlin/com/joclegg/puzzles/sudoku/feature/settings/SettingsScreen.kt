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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.BackButton
import com.joclegg.puzzles.sudoku.core.data.DigitCounts
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.HintStyle
import com.joclegg.puzzles.sudoku.core.data.ModeSettings
import com.joclegg.puzzles.sudoku.core.data.Settings
import com.joclegg.puzzles.sudoku.core.data.Toggle
import com.joclegg.puzzles.sudoku.engine.HighlightStyle
import com.joclegg.puzzles.sudoku.engine.InputMode
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import com.joclegg.puzzles.sudoku.feature.R
import com.joclegg.puzzles.sudoku.ui.SudokuIcons

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
        onHintStyleChange = viewModel::onHintStyleChange,
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
    onHintStyleChange: (GameMode, HintStyle) -> Unit,
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
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
            }
        }
        SettingsCard(title = stringResource(R.string.general)) {
            Section(stringResource(R.string.entering_numbers))
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
                Section(stringResource(R.string.mistakes))
                Choice(
                    options = MistakeCheck.entries,
                    selected = current.mistakeCheck,
                    label = { mistakeLabel(it) },
                    tag = { "setting_mistakes_${it.name.lowercase()}" },
                    onSelect = { onMistakeLevelChange(mode, it) },
                )
                Hint(
                    stringResource(R.string.block_clashes_refuses_a_number_already),
                )
                ToggleRow(current, Toggle.SHOW_MISTAKE_COUNT, toggle, stringResource(R.string.show_mistake_counter))
                Section(stringResource(R.string.highlighting))
                ToggleRow(
                    current,
                    Toggle.HIGHLIGHT_HOUSES,
                    toggle,
                    stringResource(R.string.row_column_and_box),
                    detail = stringResource(R.string.around_the_selected_cell),
                )
                if (mode == GameMode.KILLER) {
                    ToggleRow(
                        current,
                        Toggle.HIGHLIGHT_CAGE,
                        toggle,
                        stringResource(R.string.cage),
                        detail = stringResource(R.string.the_selected_cell_s_cage),
                    )
                }
                StyleChoice(
                    stringResource(R.string.same_number),
                    current.sameNumberStyle,
                    "same_number",
                    onSelect = { onSameNumberStyleChange(mode, it) },
                )
                StyleChoice(
                    stringResource(R.string.matching_pencil_marks),
                    current.notesStyle,
                    "notes",
                    onSelect = { onNotesStyleChange(mode, it) },
                )
                Hint(stringResource(R.string.cell_shades_the_whole_cell_number))
                ToggleRow(
                    current,
                    Toggle.HIGHLIGHT_LAST_PLACED,
                    toggle,
                    stringResource(R.string.highlight_the_cell_you_just_filled),
                    detail = stringResource(R.string.keep_it_and_its_row_column_and_box),
                )
                Section(stringResource(R.string.look))
                ToggleRow(current, Toggle.GREY_GIVEN_CELLS, toggle, stringResource(R.string.grey_starting_cells))
                ToggleRow(
                    current,
                    Toggle.BOLD_PLACED_DIGITS,
                    toggle,
                    stringResource(R.string.bold_numbers_you_place),
                    detail = stringResource(R.string.heavier_than_the_starting_numbers),
                )
                Section(stringResource(R.string.playing))
                ToggleRow(
                    current,
                    Toggle.AUTO_REMOVE_NOTES,
                    toggle,
                    stringResource(R.string.clear_pencil_marks_automatically),
                    detail = stringResource(R.string.when_you_place_a_number),
                )
                Text(stringResource(R.string.number_counts))
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
                Hint(stringResource(R.string.count_up_shows_how_many_of_each_number))
                ToggleRow(
                    current,
                    Toggle.DISABLE_COMPLETED_DIGITS,
                    toggle,
                    stringResource(R.string.disable_finished_numbers),
                    detail = stringResource(R.string.once_all_are_placed),
                )
                ToggleRow(
                    current,
                    Toggle.SHOW_TIMER,
                    toggle,
                    stringResource(R.string.show_timer),
                    detail = stringResource(R.string.best_times_are_still_recorded),
                )
                Section(stringResource(R.string.help))
                ToggleRow(
                    current,
                    Toggle.SHOW_HINTS,
                    toggle,
                    stringResource(R.string.hints),
                    detail = stringResource(R.string.show_the_hint_button),
                )
                if (current.showHints) {
                    HintStyleRow(current.hintStyle, { onHintStyleChange(mode, it) })
                }
                ToggleRow(
                    current,
                    Toggle.SHOW_TUTORIAL,
                    toggle,
                    stringResource(R.string.how_to_play),
                    detail = stringResource(R.string.show_the_button_with_the_rules_and_a),
                )
                ToggleRow(
                    current,
                    Toggle.ALLOW_REVEAL,
                    toggle,
                    stringResource(R.string.show_solution),
                    detail = stringResource(R.string.offer_show_solution_in_the_game_menu),
                )
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
                            stringResource(R.string.cell_first)
                        } else {
                            stringResource(R.string.number_first)
                        },
                    )
                }
            }
        }
        Hint(
            if (inputMode == InputMode.CELL_FIRST) {
                stringResource(R.string.tap_a_cell_then_pick_its_number_try)
            } else {
                stringResource(R.string.pick_a_number_then_tap_every_cell_it)
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

@Composable
private fun HintStyleRow(
    style: HintStyle,
    onChange: (HintStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.hints_fill_the_answer_in))
            Text(
                stringResource(R.string.off_a_hint_points_at_the_next_step_and),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = style == HintStyle.FILL,
            onCheckedChange = {
                onChange(
                    if (it) {
                        HintStyle.FILL
                    } else {
                        HintStyle.POINT
                    },
                )
            },
            modifier = Modifier.testTag("setting_hint_fill"),
        )
    }
}
