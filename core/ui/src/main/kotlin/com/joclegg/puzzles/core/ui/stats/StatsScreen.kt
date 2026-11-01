package com.joclegg.puzzles.core.ui.stats

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.BackButton
import com.joclegg.puzzles.core.ui.MenuPanel

data class StatsLine(
    val label: String,
    val solved: String,
    val best: String,
    val last: String,
    val tag: String? = null,
)

@Composable
fun <S> StatsRoute(
    viewModel: StatsViewModel<S, *, *>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Stats",
    spacing: Dp = 8.dp,
    footnote: String? = null,
    content: @Composable ColumnScope.(S) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(title, onBack, modifier, spacing, footnote) { content(state.stats) }
}

@Composable
fun StatsScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    footnote: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
        content()
        if (footnote != null) {
            Text(
                footnote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StatsTable(
    header: StatsLine,
    lines: List<StatsLine>,
    accent: Color,
    modifier: Modifier = Modifier,
    heading: String? = null,
    labelWeight: Float = 1f,
) {
    MenuPanel(accent, modifier) {
        if (heading != null) {
            Text(heading, style = MaterialTheme.typography.titleLarge)
        }
        StatsTableRow(header, labelWeight, FontWeight.SemiBold)
        lines.forEach { StatsTableRow(it, labelWeight, FontWeight.Normal) }
    }
}

@Composable
private fun StatsTableRow(
    line: StatsLine,
    labelWeight: Float,
    weight: FontWeight,
    modifier: Modifier = Modifier,
) {
    val tag = line.tag
    Row(modifier.fillMaxWidth()) {
        Text(line.label, fontWeight = weight, modifier = Modifier.weight(labelWeight))
        Text(line.solved, fontWeight = weight, modifier = Modifier.weight(1f).then(tag?.let { Modifier.testTag("${it}_solved") } ?: Modifier))
        Text(line.best, fontWeight = weight, modifier = Modifier.weight(1f).then(tag?.let { Modifier.testTag("${it}_best") } ?: Modifier))
        Text(line.last, fontWeight = weight, modifier = Modifier.weight(1f))
    }
}
