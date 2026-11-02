package com.joclegg.puzzles.core.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class HistoryEntry(
    val id: Long,
    val title: String,
    val startedAt: Long,
    val elapsedMs: Long,
    val summary: String,
    val inProgress: Boolean,
)

data class StatsUiState<S, F>(
    val stats: S,
    val history: List<HistoryEntry> = emptyList(),
    val filter: F? = null,
)

class StatsSource<S, G, F>(
    val stats: Flow<S>,
    val initialStats: S,
    val history: Flow<List<G>> = flowOf(emptyList()),
    val matches: (G, F) -> Boolean = { _, _ -> true },
    val entry: (G) -> HistoryEntry = { error("No history") },
    val replay: suspend (G) -> Long = { error("No history") },
)

class StatsViewModel<S, G, F>(
    private val source: StatsSource<S, G, F>,
) : ViewModel() {
    private val filter = MutableStateFlow<F?>(null)
    private var shown: Map<Long, G> = emptyMap()

    val uiState: StateFlow<StatsUiState<S, F>> =
        combine(source.stats, source.history, filter) { stats, history, filter ->
            val games = history.filter { filter == null || source.matches(it, filter) }.associateBy { source.entry(it).id }
            shown = games
            StatsUiState(stats, games.values.map(source.entry), filter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatsUiState(source.initialStats))

    fun setFilter(value: F?) {
        filter.value = value
    }

    suspend fun replay(entry: HistoryEntry): Long = source.replay(shown.getValue(entry.id))

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
