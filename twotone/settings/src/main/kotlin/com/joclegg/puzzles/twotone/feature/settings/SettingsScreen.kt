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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.twotone.core.data.InputMode
import com.joclegg.puzzles.twotone.core.data.Settings
import com.joclegg.puzzles.twotone.core.data.Toggle
import com.joclegg.puzzles.twotone.core.ui.BackButton

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
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
            }
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Filling in", style = MaterialTheme.typography.titleMedium)
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
                                    InputMode.TAP_TO_CYCLE -> "Tap to cycle"
                                    InputMode.PICK_COLOUR -> "Pick a colour"
                                },
                                maxLines = 1,
                            )
                        }
                    }
                }
                Text(
                    "Tap to cycle steps a spot through the colours. Pick a colour shows colour buttons under the board: " +
                        "pick one, then tap spots to fill them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ToggleRow(
                    settings,
                    Toggle.MARK_BROKEN_RULES,
                    onToggle,
                    "Mark broken rules",
                    detail = "Outline tiles that make three in a row, overfill a line or copy another line.",
                )
                ToggleRow(settings, Toggle.SHOW_TIMER, onToggle, "Show the timer")
                ToggleRow(settings, Toggle.SHOW_PROGRESS, onToggle, "Show how much is filled in")
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
