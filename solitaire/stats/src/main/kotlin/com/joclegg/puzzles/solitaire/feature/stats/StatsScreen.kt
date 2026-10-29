package com.joclegg.puzzles.solitaire.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.MenuPanel
import com.joclegg.puzzles.solitaire.core.data.DrawStats
import com.joclegg.puzzles.solitaire.core.ui.BackButton
import com.joclegg.puzzles.solitaire.core.ui.LocalTableColors
import com.joclegg.puzzles.solitaire.core.ui.formatDuration
import com.joclegg.puzzles.solitaire.core.ui.label

@Composable
fun StatsRoute(
    viewModel: StatsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(state.stats, onBack, modifier)
}

@Composable
fun StatsScreen(
    stats: List<DrawStats>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalTableColors.current.accent
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Stats", style = MaterialTheme.typography.headlineMedium)
        }
        stats.forEach { mode ->
            val tag = mode.drawMode.name.lowercase()
            MenuPanel(accent) {
                Text(mode.drawMode.label(), style = MaterialTheme.typography.titleLarge)
                StatRow("Played", mode.played.toString(), "${tag}_played")
                StatRow("Won", mode.won.toString(), "${tag}_won")
                StatRow("Win rate", mode.winRate(), "${tag}_win_rate")
                StatRow("Best time", mode.bestMs?.let(::formatDuration) ?: "–", "${tag}_best")
                StatRow("Average time", mode.averageMs?.let(::formatDuration) ?: "–", "${tag}_average")
                StatRow("Fewest moves", mode.fewestMoves?.toString() ?: "–", "${tag}_fewest_moves")
            }
        }
        Text(
            "Abandoned deals count as played. Daily deals count under the draw mode they were played with.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun DrawStats.winRate(): String =
    if (played == 0) {
        "–"
    } else {
        "${won * PERCENT / played}%"
    }

private const val PERCENT = 100

@Composable
private fun StatRow(
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, modifier = Modifier.testTag(tag))
    }
}
