package com.joclegg.puzzles.twotone.feature.settings

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
import com.joclegg.puzzles.core.ui.BackButton
import com.joclegg.puzzles.twotone.core.data.InputMode
import com.joclegg.puzzles.twotone.core.data.Settings
import com.joclegg.puzzles.twotone.core.data.Toggle
import com.joclegg.puzzles.twotone.feature.R

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(settings, viewModel::onToggle, viewModel::onInputModeChange, onBack, modifier)
}

@Composable
fun SettingsScreen(
    settings: Settings,
    onToggle: (Toggle, Boolean) -> Unit,
    onInputModeChange: (InputMode) -> Unit,
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
                Text(stringResource(R.string.filling_in), style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    InputMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.inputMode == mode,
                            onClick = { onInputModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, InputMode.entries.size),
                            modifier = Modifier.testTag("setting_${mode.name.lowercase()}"),
                            icon = {},
                        ) {
                            Text(
                                when (mode) {
                                    InputMode.TAP_TO_CYCLE -> stringResource(R.string.tap_to_cycle)
                                    InputMode.PICK_COLOUR -> stringResource(R.string.pick_a_colour)
                                },
                                maxLines = 1,
                            )
                        }
                    }
                }
                Text(
                    stringResource(R.string.tap_to_cycle_steps_a_spot_through_the),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ToggleRow(
                    settings,
                    Toggle.MARK_BROKEN_RULES,
                    onToggle,
                    stringResource(R.string.mark_broken_rules),
                    detail = stringResource(R.string.outline_tiles_that_make_three_in_a_row),
                )
                ToggleRow(settings, Toggle.SHOW_TIMER, onToggle, stringResource(R.string.show_the_timer))
                ToggleRow(settings, Toggle.SHOW_PROGRESS, onToggle, stringResource(R.string.show_how_much_is_filled_in))
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
