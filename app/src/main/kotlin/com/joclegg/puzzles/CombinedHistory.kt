package com.joclegg.puzzles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class HistoryEntry(
    val game: String,
    val finishedAt: Long,
    val result: String,
    val elapsedMs: Long?,
)

const val COMBINED_HISTORY = "history"

fun mergeHistory(lists: List<List<HistoryEntry>>): List<HistoryEntry> = lists.flatten().sortedByDescending { it.finishedAt }

private fun entry(
    game: String,
    finishedAt: Long?,
    status: String,
    elapsedMs: Long?,
): HistoryEntry? =
    finishedAt?.takeIf { status != "IN_PROGRESS" && status != "ABANDONED" }?.let {
        HistoryEntry(game, it, status.lowercase().replaceFirstChar(Char::uppercase), elapsedMs)
    }

fun combinedHistory(app: PuzzlesApplication): Flow<List<HistoryEntry>> =
    combine(
        listOf(
            app.sudoku.games.history.map { games -> games.mapNotNull { entry("Sudoku", it.finishedAt, it.status.name, it.elapsedMs) } },
            app.solitaire.games.history.map { games -> games.mapNotNull { entry("Solitaire", it.finishedAt, it.status.name, it.elapsedMs) } },
            app.twoTone.games.history.map { games -> games.mapNotNull { entry("Two Tone", it.finishedAt, it.status.name, it.elapsedMs) } },
            app.sightlines.games.history.map { games -> games.mapNotNull { entry("Sightlines", it.finishedAt, it.status.name, it.elapsedMs) } },
            app.fiveLetters.games.history.map { games ->
                games.mapNotNull { entry("Five Letters", it.finishedAt, it.status.name, null)?.withGuesses(it.guesses.size) }
            },
            app.kakuro.games.history.map { games -> games.mapNotNull { entry("Kakuro", it.finishedAt, it.status.name, it.elapsedMs) } },
            app.slideOut.games.history.map { games -> games.mapNotNull { entry("Slide Out", it.finishedAt, it.status.name, it.elapsedMs) } },
            app.wordWheel.games.history.map { games -> games.mapNotNull { entry("Word Wheel", it.finishedAt, it.status.name, it.elapsedMs) } },
        ),
    ) { lists -> mergeHistory(lists.toList()) }

private fun HistoryEntry.withGuesses(count: Int) = copy(result = "$result in $count")

private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
fun CombinedHistoryRoute(
    app: PuzzlesApplication,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val flow = remember(app) { combinedHistory(app) }
    val entries by flow.collectAsState(emptyList())
    CombinedHistoryScreen(entries, onBack, modifier)
}

@Composable
fun CombinedHistoryScreen(
    entries: List<HistoryEntry>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("back")) { Text("←") }
            Text("History", style = MaterialTheme.typography.headlineMedium)
        }
        if (entries.isEmpty()) {
            Text("Finished puzzles from every game show up here.", modifier = Modifier.testTag("history_empty"))
        }
        LazyColumn(Modifier.fillMaxWidth().testTag("history_list")) {
            items(entries) { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("history_row"), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.game, style = MaterialTheme.typography.titleMedium)
                        Text(
                            Instant.ofEpochMilli(item.finishedAt).atZone(ZoneId.systemDefault()).format(DATE),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(item.result)
                        item.elapsedMs?.let { Text(formatTime(it), style = MaterialTheme.typography.bodySmall) }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
