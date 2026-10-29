package com.joclegg.puzzles.slideout.feature.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.MenuEmptyState
import com.joclegg.puzzles.core.ui.MenuFilterChip
import com.joclegg.puzzles.core.ui.MenuPanel
import com.joclegg.puzzles.slideout.core.data.GameStatus
import com.joclegg.puzzles.slideout.core.data.SavedGame
import com.joclegg.puzzles.slideout.core.ui.BackButton
import com.joclegg.puzzles.slideout.core.ui.LocalSlideColors
import com.joclegg.puzzles.slideout.core.ui.formatDuration
import com.joclegg.puzzles.slideout.core.ui.label
import com.joclegg.puzzles.slideout.core.ui.title
import com.joclegg.puzzles.slideout.engine.Level
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryRoute(
    viewModel: StatsViewModel,
    onOpenGame: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    HistoryScreen(
        state = state,
        onFilter = viewModel::setFilter,
        onGameClick = { game ->
            if (game.status == GameStatus.IN_PROGRESS) {
                onOpenGame(game.id)
            } else {
                scope.launch { onOpenGame(viewModel.replay(game)) }
            }
        },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun HistoryScreen(
    state: StatsUiState,
    onFilter: (Level?) -> Unit,
    onGameClick: (SavedGame) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalSlideColors.current.accent
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("History", style = MaterialTheme.typography.headlineMedium)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuFilterChip("All", selected = state.filter == null, accent = accent, onClick = { onFilter(null) })
            Level.entries.forEach { level ->
                MenuFilterChip(
                    label = level.label().first().toString(),
                    selected = state.filter == level,
                    accent = accent,
                    onClick = { onFilter(level) },
                    modifier = Modifier.testTag("filter_${level.name.lowercase()}"),
                )
            }
        }
        if (state.history.isEmpty()) {
            MenuEmptyState("No games yet.", accent)
        } else {
            MenuPanel(accent, Modifier.weight(1f, fill = false)) {
                LazyColumn {
                    items(state.history, key = { it.id }) { game ->
                        HistoryItem(game, onClick = { onGameClick(game) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

@Composable
private fun HistoryItem(
    game: SavedGame,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status =
        when (game.status) {
            GameStatus.SOLVED -> "Solved"
            GameStatus.IN_PROGRESS -> "In progress"
            GameStatus.ABANDONED -> "Abandoned"
            GameStatus.REVEALED -> "Solution shown"
        }
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag("history_item"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(game.ref.title(), style = MaterialTheme.typography.titleSmall)
            Text(
                Instant.ofEpochMilli(game.startedAt).atZone(ZoneId.systemDefault()).format(dateFormat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatDuration(game.elapsedMs))
            Text(
                "$status · ${game.moves.size} moves",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
