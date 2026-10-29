package com.joclegg.puzzles.sudoku.feature.stats

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
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.GameStatus
import com.joclegg.puzzles.sudoku.core.data.SavedGame
import com.joclegg.puzzles.sudoku.core.ui.BackButton
import com.joclegg.puzzles.sudoku.core.ui.LocalBoardColors
import com.joclegg.puzzles.sudoku.core.ui.formatDuration
import com.joclegg.puzzles.sudoku.core.ui.gameTitle
import com.joclegg.puzzles.sudoku.core.ui.label
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
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
    onFilter: (HistoryFilter?) -> Unit,
    onGameClick: (SavedGame) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalBoardColors.current.accent
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("History", style = MaterialTheme.typography.headlineMedium)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuFilterChip("All", selected = state.filter == null, accent = accent, onClick = { onFilter(null) })
            HistoryFilter.entries.forEach { filter ->
                MenuFilterChip(
                    label = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = state.filter == filter,
                    accent = accent,
                    onClick = { onFilter(filter) },
                    modifier = Modifier.testTag("filter_${filter.name.lowercase()}"),
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
    val title =
        gameTitle(
            killer = game.ref.mode == GameMode.KILLER,
            custom = game.ref.custom,
            difficulty = game.ref.difficulty,
            answerKnown = game.ref.puzzle.solution != null,
        )
    val status =
        when (game.status) {
            GameStatus.COMPLETED -> "Solved"
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
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                Instant.ofEpochMilli(game.startedAt).atZone(ZoneId.systemDefault()).format(dateFormat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatDuration(game.elapsedMs))
            Text(
                if (game.mistakeCheck != MistakeCheck.OFF) {
                    "$status · ${game.mistakes} mistakes"
                } else {
                    status
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
