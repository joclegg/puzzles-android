package com.joclegg.puzzles.fiveletters.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.joclegg.puzzles.fiveletters.core.data.WordStats
import com.joclegg.puzzles.fiveletters.core.ui.BackButton
import com.joclegg.puzzles.fiveletters.core.ui.LocalLetterColors

@Composable
fun StatsRoute(
    viewModel: StatsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(stats, onBack, modifier)
}

@Composable
fun StatsScreen(
    stats: WordStats,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLetterColors.current
    val accent = LocalLetterColors.current.accent
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Stats", style = MaterialTheme.typography.headlineMedium)
        }
        MenuPanel(accent) {
            Row(Modifier.fillMaxWidth()) {
                Figure(stats.played.toString(), "Played", "played", Modifier.weight(1f))
                Figure(
                    if (stats.played == 0) {
                        "0"
                    } else {
                        (stats.won * PERCENT / stats.played).toString()
                    },
                    "Win %",
                    "win_percent",
                    Modifier.weight(1f),
                )
                Figure(stats.currentStreak.toString(), "Streak", "streak", Modifier.weight(1f))
                Figure(stats.maxStreak.toString(), "Best streak", "max_streak", Modifier.weight(1f))
            }
        }
        MenuPanel(accent, title = "Guesses") {
            val most = (stats.guessCounts.maxOrNull() ?: 0).coerceAtLeast(1)
            stats.guessCounts.forEachIndexed { index, count ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", modifier = Modifier.width(20.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(MIN_BAR + (1 - MIN_BAR) * count / most)
                            .background(colors.correct, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("guesses_${index + 1}"),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(count.toString(), color = colors.markedText)
                    }
                }
            }
        }
    }
}

@Composable
private fun Figure(
    value: String,
    label: String,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag(tag))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private const val PERCENT = 100
private const val MIN_BAR = 0.08f
