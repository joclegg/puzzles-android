package com.joclegg.puzzles.solitaire.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.joclegg.puzzles.solitaire.core.data.HintStyle
import com.joclegg.puzzles.solitaire.core.data.Settings
import com.joclegg.puzzles.solitaire.core.data.Toggle
import com.joclegg.puzzles.solitaire.engine.DrawMode
import com.joclegg.puzzles.solitaire.feature.R
import com.joclegg.puzzles.solitaire.ui.label

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        settings = settings,
        onToggle = viewModel::onToggle,
        onHintStyleChange = viewModel::onHintStyleChange,
        onDrawModeChange = viewModel::onDrawModeChange,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    settings: Settings,
    onToggle: (Toggle, Boolean) -> Unit,
    onHintStyleChange: (HintStyle) -> Unit,
    onDrawModeChange: (DrawMode) -> Unit,
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
        SettingsCard(title = stringResource(R.string.play)) {
            Choice(
                options = DrawMode.entries,
                selected = settings.drawMode,
                label = { it.label() },
                tag = { "setting_${it.name.lowercase()}" },
                onSelect = onDrawModeChange,
            )
            Hint(stringResource(R.string.draw_3_is_harder_applies_to_new_games))
            ToggleRow(
                settings,
                Toggle.REVERSE,
                onToggle,
                stringResource(R.string.reverse),
                detail = stringResource(R.string.foundations_start_with_kings_columns),
            )
            ToggleRow(
                settings,
                Toggle.ONE_TAP_MOVE,
                onToggle,
                stringResource(R.string.one_tap_to_move),
                detail = stringResource(R.string.tapping_a_card_sends_it_to_the_best),
            )
            ToggleRow(
                settings,
                Toggle.WINNABLE_ONLY,
                onToggle,
                stringResource(R.string.winnable_deals_only),
                detail = stringResource(R.string.every_new_deal_can_be_won_applies_to),
            )
            ToggleRow(
                settings,
                Toggle.WARN_UNWINNABLE,
                onToggle,
                stringResource(R.string.tell_me_when_a_deal_can_t_be_won),
                detail = stringResource(R.string.checks_after_a_full_pass_through_the),
            )
            ToggleRow(settings, Toggle.SHOW_TIMER, onToggle, stringResource(R.string.show_the_timer))
            ToggleRow(settings, Toggle.SHOW_MOVE_COUNT, onToggle, stringResource(R.string.show_the_move_counter))
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
            if (settings.showHints) {
                HintStyleRow(settings.hintStyle, onHintStyleChange)
            }
        }
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
private fun Hint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
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
