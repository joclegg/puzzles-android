package com.joclegg.puzzles.kakuro.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.data.CombinationOrder
import com.joclegg.puzzles.core.ui.BackButton
import com.joclegg.puzzles.kakuro.core.data.HintStyle
import com.joclegg.puzzles.kakuro.core.data.Settings
import com.joclegg.puzzles.kakuro.core.data.Toggle
import com.joclegg.puzzles.kakuro.engine.InputMode
import com.joclegg.puzzles.kakuro.engine.MistakeCheck
import com.joclegg.puzzles.kakuro.feature.R

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        settings,
        viewModel::onToggle,
        viewModel::onHintStyleChange,
        viewModel::onInputModeChange,
        viewModel::onMistakeCheckChange,
        viewModel::onCombinationOrderChange,
        onBack,
        modifier,
    )
}

@Composable
fun SettingsScreen(
    settings: Settings,
    onToggle: (Toggle, Boolean) -> Unit,
    onHintStyleChange: (HintStyle) -> Unit,
    onInputModeChange: (InputMode) -> Unit,
    onMistakeCheckChange: (MistakeCheck) -> Unit,
    onCombinationOrderChange: (CombinationOrder) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
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
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.entering_numbers), style = MaterialTheme.typography.titleMedium)
                Choice(
                    options = InputMode.entries,
                    selected = settings.inputMode,
                    label = {
                        when (it) {
                            InputMode.CELL_FIRST -> "Cell first"
                            InputMode.DIGIT_FIRST -> "Number first"
                        }
                    },
                    tag = { "setting_${it.name.lowercase()}" },
                    onSelect = onInputModeChange,
                )
                Hint(stringResource(R.string.cell_first_tap_a_cell_then_a_number))
                Text(stringResource(R.string.mistakes), style = MaterialTheme.typography.titleMedium)
                Choice(
                    options = MistakeCheck.entries,
                    selected = settings.mistakeCheck,
                    label = {
                        when (it) {
                            MistakeCheck.OFF -> "Off"
                            MistakeCheck.BLOCK_CLASHES -> "Block clashes"
                            MistakeCheck.SHOW_WRONG -> "Show wrong"
                        }
                    },
                    tag = { "setting_mistakes_${it.name.lowercase()}" },
                    onSelect = onMistakeCheckChange,
                )
                Hint(
                    stringResource(R.string.block_clashes_refuses_a_number_already),
                )
                ToggleRow(settings, Toggle.SHOW_MISTAKE_COUNT, onToggle, stringResource(R.string.show_the_mistake_counter))
                ToggleRow(
                    settings,
                    Toggle.AUTO_REMOVE_NOTES,
                    onToggle,
                    stringResource(R.string.remove_pencil_marks_automatically),
                    detail = stringResource(R.string.placing_a_number_clears_that_number_s),
                )
                ToggleRow(
                    settings,
                    Toggle.HIGHLIGHT_RUNS,
                    onToggle,
                    stringResource(R.string.highlight_runs),
                    detail = stringResource(R.string.shade_the_across_and_down_runs_of_the),
                )
                ToggleRow(settings, Toggle.SHOW_TIMER, onToggle, stringResource(R.string.show_the_timer))
                ToggleRow(
                    settings,
                    Toggle.SHOW_COMBINATIONS,
                    onToggle,
                    stringResource(R.string.sum_helper),
                    detail = stringResource(R.string.list_the_digit_combinations_that_still),
                )
                if (settings.showCombinations) {
                    Text(stringResource(R.string.sum_helper_order))
                    Choice(
                        options = CombinationOrder.entries,
                        selected = settings.combinationOrder,
                        label = {
                            when (it) {
                                CombinationOrder.INCREASING -> "Increasing"
                                CombinationOrder.DECREASING -> "Decreasing"
                            }
                        },
                        tag = { "setting_combination_order_${it.name.lowercase()}" },
                        onSelect = onCombinationOrderChange,
                    )
                }
                ToggleRow(
                    settings,
                    Toggle.GREY_USED_DIGITS,
                    onToggle,
                    stringResource(R.string.grey_out_used_up_numbers),
                    detail = stringResource(R.string.dim_a_number_on_the_pad_once_every),
                )
                ToggleRow(
                    settings,
                    Toggle.SHOW_HINTS,
                    onToggle,
                    stringResource(R.string.hints),
                    detail = stringResource(R.string.show_the_hint_button),
                )
                ToggleRow(
                    settings,
                    Toggle.SHOW_TUTORIAL,
                    onToggle,
                    stringResource(R.string.how_to_play),
                    detail = stringResource(R.string.show_the_button_with_the_rules_and_a),
                )
                ToggleRow(
                    settings,
                    Toggle.ALLOW_REVEAL,
                    onToggle,
                    stringResource(R.string.show_solution),
                    detail = stringResource(R.string.offer_show_solution_in_the_game_menu),
                )
                if (settings.showHints) {
                    HintStyleRow(settings.hintStyle, onHintStyleChange)
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    settings: Settings,
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
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun Hint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
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
