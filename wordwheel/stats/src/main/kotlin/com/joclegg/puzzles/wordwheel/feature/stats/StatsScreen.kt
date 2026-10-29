package com.joclegg.puzzles.wordwheel.feature.stats

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
import com.joclegg.puzzles.wordwheel.core.data.LevelStats
import com.joclegg.puzzles.wordwheel.core.ui.BackButton
import com.joclegg.puzzles.wordwheel.core.ui.LocalWheelColors
import com.joclegg.puzzles.wordwheel.core.ui.formatDuration
import com.joclegg.puzzles.wordwheel.core.ui.label

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
    stats: List<LevelStats>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalWheelColors.current.accent
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Stats", style = MaterialTheme.typography.headlineMedium)
        }
        MenuPanel(accent) {
            StatsRow("Letters", "Solved", "Bonus", "Best", header = true)
            stats.forEach { level ->
                StatsRow(
                    level.level.label(),
                    "${level.solved}/${level.played}",
                    level.bonusWords.toString(),
                    level.bestMs?.let(::formatDuration) ?: "–",
                    tag = "level_${level.level.letters}",
                )
            }
        }
        Text(
            "Solved out of played. Bonus counts every extra word you found.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatsRow(
    size: String,
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
        Text(size, fontWeight = weight, modifier = Modifier.weight(1f))
        Text(solved, fontWeight = weight, modifier = Modifier.weight(1f).then(tag?.let { Modifier.testTag("${it}_solved") } ?: Modifier))
        Text(best, fontWeight = weight, modifier = Modifier.weight(1f).then(tag?.let { Modifier.testTag("${it}_best") } ?: Modifier))
        Text(average, fontWeight = weight, modifier = Modifier.weight(1f))
    }
}
