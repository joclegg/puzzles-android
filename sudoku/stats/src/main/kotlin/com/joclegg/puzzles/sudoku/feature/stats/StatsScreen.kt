package com.joclegg.puzzles.sudoku.feature.stats

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.MenuPanel
import com.joclegg.puzzles.sudoku.core.data.BestTime
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.levels
import com.joclegg.puzzles.sudoku.core.ui.BackButton
import com.joclegg.puzzles.sudoku.core.ui.LocalBoardColors
import com.joclegg.puzzles.sudoku.core.ui.formatDuration
import com.joclegg.puzzles.sudoku.core.ui.label

@Composable
fun StatsRoute(
    viewModel: StatsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(state.bestTimes, onBack, modifier)
}

@Composable
fun StatsScreen(
    bestTimes: List<BestTime>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalBoardColors.current.accent
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Best times", style = MaterialTheme.typography.headlineMedium)
        }
        GameMode.entries.forEach { mode ->
            MenuPanel(accent) {
                Text(
                    if (mode == GameMode.CLASSIC) {
                        "Classic"
                    } else {
                        "Killer"
                    },
                    style = MaterialTheme.typography.titleLarge,
                )
                StatsRow("Level", "Solved", "Best", "Average", header = true)
                mode.levels.forEach { level ->
                    val stats = bestTimes.find { it.mode == mode && it.difficulty == level }
                    StatsRow(
                        level.label(),
                        (stats?.completed ?: 0).toString(),
                        stats?.let { formatDuration(it.bestMs) } ?: "–",
                        stats?.takeIf { it.completed > 0 }?.let { formatDuration(it.averageMs) } ?: "–",
                        tag = "best_${mode.name.lowercase()}_${level.name.lowercase()}",
                    )
                }
            }
        }
        Text(
            "Custom puzzles show in History but don't count towards best times.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatsRow(
    level: String,
    solved: String,
    best: String,
    average: String,
    modifier: Modifier = Modifier,
    header: Boolean = false,
    tag: String? = null,
) {
    val weight =
        if (header) {
            FontWeight.SemiBold
        } else {
            FontWeight.Normal
        }
    Row(modifier.fillMaxWidth()) {
        Text(level, fontWeight = weight, modifier = Modifier.weight(1.3f))
        Text(solved, fontWeight = weight, modifier = Modifier.weight(1f).then(tag?.let { Modifier.testTag("${it}_solved") } ?: Modifier))
        Text(best, fontWeight = weight, modifier = Modifier.weight(1f).then(tag?.let { Modifier.testTag("${it}_best") } ?: Modifier))
        Text(average, fontWeight = weight, modifier = Modifier.weight(1f))
    }
}
