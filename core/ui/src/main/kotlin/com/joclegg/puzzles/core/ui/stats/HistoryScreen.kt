package com.joclegg.puzzles.core.ui.stats

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.BackButton
import com.joclegg.puzzles.core.ui.MenuEmptyState
import com.joclegg.puzzles.core.ui.MenuFilterChip
import com.joclegg.puzzles.core.ui.MenuPanel
import com.joclegg.puzzles.core.ui.R
import com.joclegg.puzzles.core.ui.formatDuration
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class HistoryFilter<F>(
    val value: F,
    val label: String,
    val tag: String,
)

@Composable
fun <F> HistoryRoute(
    viewModel: StatsViewModel<*, *, F>,
    accent: Color,
    filters: List<HistoryFilter<F>>,
    onOpenGame: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    HistoryScreen(
        history = state.history,
        filter = state.filter,
        filters = filters,
        accent = accent,
        onFilter = viewModel::setFilter,
        onEntryClick = { entry ->
            if (entry.inProgress) {
                onOpenGame(entry.id)
            } else {
                scope.launch { onOpenGame(viewModel.replay(entry)) }
            }
        },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun <F> HistoryScreen(
    history: List<HistoryEntry>,
    filter: F?,
    filters: List<HistoryFilter<F>>,
    accent: Color,
    onFilter: (F?) -> Unit,
    onEntryClick: (HistoryEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text(stringResource(R.string.history), style = MaterialTheme.typography.headlineMedium)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuFilterChip(stringResource(R.string.all), selected = filter == null, accent = accent, onClick = { onFilter(null) })
            filters.forEach { option ->
                MenuFilterChip(
                    label = option.label,
                    selected = filter == option.value,
                    accent = accent,
                    onClick = { onFilter(option.value) },
                    modifier = Modifier.testTag("filter_${option.tag}"),
                )
            }
        }
        if (history.isEmpty()) {
            MenuEmptyState(stringResource(R.string.no_games_yet), accent)
        } else {
            MenuPanel(accent, Modifier.weight(1f, fill = false)) {
                LazyColumn {
                    items(history, key = { it.id }) { entry ->
                        HistoryItem(entry, onClick = { onEntryClick(entry) })
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
    entry: HistoryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag("history_item"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.titleSmall)
            Text(
                Instant.ofEpochMilli(entry.startedAt).atZone(ZoneId.systemDefault()).format(dateFormat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatDuration(entry.elapsedMs))
            Text(
                entry.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
