package com.joclegg.puzzles.crossword.feature.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.BackButton
import com.joclegg.puzzles.crossword.core.data.Settings
import com.joclegg.puzzles.crossword.core.data.Toggle
import com.joclegg.puzzles.crossword.feature.R

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
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
            }
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ToggleRow(
                    settings,
                    Toggle.SKIP_FILLED_CELLS,
                    onToggle,
                    stringResource(R.string.skip_filled_cells),
                    detail = stringResource(R.string.skip_filled_cells_detail),
                )
                ToggleRow(settings, Toggle.SHOW_TIMER, onToggle, stringResource(R.string.show_the_timer))
                ToggleRow(
                    settings,
                    Toggle.SHOW_MISTAKES,
                    onToggle,
                    stringResource(R.string.show_mistakes),
                    detail = stringResource(R.string.colour_wrong_letters_red),
                )
                ToggleRow(
                    settings,
                    Toggle.ALLOW_REVEAL,
                    onToggle,
                    stringResource(R.string.allow_reveal),
                    detail = stringResource(R.string.offer_reveal_letter_word_and_puzzle),
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
