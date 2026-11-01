package com.joclegg.puzzles.kakuro

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.joclegg.puzzles.core.ui.formatDuration
import com.joclegg.puzzles.core.ui.stats.HistoryEntry
import com.joclegg.puzzles.core.ui.stats.HistoryFilter
import com.joclegg.puzzles.core.ui.stats.StatsLine
import com.joclegg.puzzles.core.ui.stats.StatsSource
import com.joclegg.puzzles.core.ui.stats.StatsTable
import com.joclegg.puzzles.kakuro.core.data.GameRepository
import com.joclegg.puzzles.kakuro.core.data.GameStatus
import com.joclegg.puzzles.kakuro.core.data.SavedGame
import com.joclegg.puzzles.kakuro.core.data.SizeStats
import com.joclegg.puzzles.kakuro.core.ui.LocalKakuroColors
import com.joclegg.puzzles.kakuro.core.ui.sizeLabel
import com.joclegg.puzzles.kakuro.core.ui.title
import com.joclegg.puzzles.kakuro.engine.SIZES
import com.joclegg.puzzles.stats.hintsSummary
import com.joclegg.puzzles.stats.puzzleStatusLabel

const val KAKURO_STATS_FOOTNOTE = "Solved out of played. Abandoned puzzles count as played. Daily puzzles count under their size."

val kakuroHistoryFilters = SIZES.map { HistoryFilter(it, "$it", "$it") }

fun kakuroStatsSource(games: GameRepository) =
    StatsSource<List<SizeStats>, SavedGame, Int>(
        stats = games.stats,
        initialStats = emptyList(),
        history = games.history,
        matches = { game, size -> game.ref.size == size },
        entry = SavedGame::historyEntry,
        replay = { game -> games.start { game.ref }.id },
    )

private fun SavedGame.historyEntry() =
    HistoryEntry(
        id = id,
        title = ref.title(),
        startedAt = startedAt,
        elapsedMs = elapsedMs,
        summary = hintsSummary(puzzleStatusLabel(status.name), hints),
        inProgress = status == GameStatus.IN_PROGRESS,
    )

@Composable
fun KakuroStatsTable(
    stats: List<SizeStats>,
    modifier: Modifier = Modifier,
) {
    StatsTable(
        header = StatsLine("Size", "Solved", "Best", "Average"),
        lines =
            stats.map { size ->
                StatsLine(
                    sizeLabel(size.size),
                    "${size.solved}/${size.played}",
                    size.bestMs?.let(::formatDuration) ?: "–",
                    size.averageMs?.let(::formatDuration) ?: "–",
                    tag = "size_${size.size}",
                )
            },
        modifier = modifier,
        accent = LocalKakuroColors.current.accent,
    )
}
