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

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(settings, viewModel::onToggle, onBack, modifier)
}

@Composable
fun SettingsScreen(
    settings: Settings,
    onToggle: (Toggle, Boolean) -> Unit,
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
                ToggleRow(
                    settings,
                    Toggle.SHOW_MISTAKES,
                    onToggle,
                    "Show mistakes",
                    detail = "Wrong digits and runs that can't add up turn red.",
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
