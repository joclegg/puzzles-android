package com.joclegg.puzzles.sudoku.feature.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.sudoku.core.data.BestTime
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.levels
import com.joclegg.puzzles.sudoku.core.ui.BackButton
import com.joclegg.puzzles.sudoku.core.ui.dialogTestTags
import com.joclegg.puzzles.sudoku.core.ui.formatDuration
import com.joclegg.puzzles.sudoku.core.ui.label
import com.joclegg.puzzles.sudoku.core.ui.parseDuration
import com.joclegg.puzzles.sudoku.engine.Difficulty

@Composable
fun StatsRoute(
    viewModel: StatsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(state.bestTimes, viewModel::onSetPastBest, onBack, modifier)
}

@Composable
fun StatsScreen(
    bestTimes: List<BestTime>,
    onSetPastBest: (GameMode, Difficulty, Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<Pair<GameMode, Difficulty>?>(null) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Best times", style = MaterialTheme.typography.headlineMedium)
        }
        GameMode.entries.forEach { mode ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        onClick = { editing = mode to level },
                    )
                }
            }
        }
        Text(
            "Custom puzzles show in History but don't count towards best times. " +
                "Tap a level to add a best time from another app; it counts until you beat it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    editing?.let { (mode, level) ->
        PastBestDialog(
            title = "${modeLabel(mode)} · ${level.label()}",
            onSave = { bestMs ->
                editing = null
                onSetPastBest(mode, level, bestMs)
            },
            onDismiss = { editing = null },
        )
    }
}

private fun modeLabel(mode: GameMode) =
    if (mode == GameMode.CLASSIC) {
        "Classic"
    } else {
        "Killer"
    }

@Composable
private fun PastBestDialog(
    title: String,
    onSave: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    val parsed = parseDuration(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.dialogTestTags().testTag("past_best_dialog"),
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your best time from another app, as minutes:seconds.")
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("1:02") },
                    singleLine = true,
                    isError = text.isNotEmpty() && parsed == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.testTag("past_best_input"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onSave) },
                enabled = parsed != null,
                modifier = Modifier.testTag("past_best_save"),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
    onClick: (() -> Unit)? = null,
) {
    val weight =
        if (header) {
            FontWeight.SemiBold
        } else {
            FontWeight.Normal
        }
    Row(modifier.fillMaxWidth().then(onClick?.let { Modifier.clickable(onClick = it).testTag(tag.orEmpty()) } ?: Modifier)) {
        Text(level, fontWeight = weight, modifier = Modifier.weight(1.3f))
        Text(solved, fontWeight = weight, modifier = Modifier.weight(1f).then(tag?.let { Modifier.testTag("${it}_solved") } ?: Modifier))
        Text(best, fontWeight = weight, modifier = Modifier.weight(1f).then(tag?.let { Modifier.testTag("${it}_best") } ?: Modifier))
        Text(average, fontWeight = weight, modifier = Modifier.weight(1f))
    }
}
