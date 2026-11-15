package com.joclegg.puzzles.crossword

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.joclegg.puzzles.R
import com.joclegg.puzzles.core.ui.formatDuration
import com.joclegg.puzzles.core.ui.stats.HistoryEntry
import com.joclegg.puzzles.core.ui.stats.HistoryFilter
import com.joclegg.puzzles.core.ui.stats.StatsLine
import com.joclegg.puzzles.core.ui.stats.StatsSource
import com.joclegg.puzzles.core.ui.stats.StatsTable
import com.joclegg.puzzles.crossword.core.data.GameRepository
import com.joclegg.puzzles.crossword.core.data.GameStatus
import com.joclegg.puzzles.crossword.core.data.SavedGame
import com.joclegg.puzzles.crossword.core.data.SizeStats
import com.joclegg.puzzles.crossword.engine.SIZES
import com.joclegg.puzzles.crossword.ui.LocalCrosswordColors
import com.joclegg.puzzles.crossword.ui.sizeLabel
import com.joclegg.puzzles.crossword.ui.title
import com.joclegg.puzzles.stats.hintsSummary
import com.joclegg.puzzles.stats.puzzleStatusLabel

const val CROSSWORD_STATS_FOOTNOTE = "Solved out of played. Abandoned puzzles count as played. Daily puzzles count under their size."

val crosswordHistoryFilters = SIZES.map { HistoryFilter(it, sizeLabel(it), "$it") }

fun crosswordStatsSource(games: GameRepository) =
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
fun CrosswordStatsTable(
    stats: List<SizeStats>,
    modifier: Modifier = Modifier,
) {
    StatsTable(
        header =
            StatsLine(
                stringResource(R.string.size),
                stringResource(R.string.solved),
                stringResource(R.string.best),
                stringResource(R.string.average),
            ),
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
        accent = LocalCrosswordColors.current.accent,
    )
}
