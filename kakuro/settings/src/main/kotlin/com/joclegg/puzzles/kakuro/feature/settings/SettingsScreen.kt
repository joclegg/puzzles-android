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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.kakuro.core.data.Settings
import com.joclegg.puzzles.kakuro.core.data.Toggle
import com.joclegg.puzzles.kakuro.core.ui.BackButton
import com.joclegg.puzzles.kakuro.engine.InputMode
import com.joclegg.puzzles.kakuro.engine.MistakeCheck

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(settings, viewModel::onToggle, viewModel::onInputModeChange, viewModel::onMistakeCheckChange, onBack, modifier)
}

@Composable
fun SettingsScreen(
    settings: Settings,
    onToggle: (Toggle, Boolean) -> Unit,
    onInputModeChange: (InputMode) -> Unit,
    onMistakeCheckChange: (MistakeCheck) -> Unit,
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
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
            }
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Entering numbers", style = MaterialTheme.typography.titleMedium)
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
                Hint("Cell first: tap a cell, then a number. Number first: pick a number, then tap cells to fill them.")
                Text("Mistakes", style = MaterialTheme.typography.titleMedium)
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
                    "Block clashes refuses a number already in the run. Show wrong turns any wrong number red. " +
                        "Both turn runs that can no longer add up red, and count mistakes.",
                )
                ToggleRow(settings, Toggle.SHOW_MISTAKE_COUNT, onToggle, "Show the mistake counter")
                ToggleRow(
                    settings,
                    Toggle.AUTO_REMOVE_NOTES,
                    onToggle,
                    "Remove pencil marks automatically",
                    detail = "Placing a number clears that number's pencil marks from its across and down runs.",
                )
                ToggleRow(
                    settings,
                    Toggle.HIGHLIGHT_RUNS,
                    onToggle,
                    "Highlight runs",
                    detail = "Shade the across and down runs of the selected cell.",
                )
                ToggleRow(settings, Toggle.SHOW_TIMER, onToggle, "Show the timer")
                ToggleRow(
                    settings,
                    Toggle.SHOW_COMBINATIONS,
                    onToggle,
                    "Sum helper",
                    detail = "List the digit combinations that still fit the selected cell's runs.",
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
