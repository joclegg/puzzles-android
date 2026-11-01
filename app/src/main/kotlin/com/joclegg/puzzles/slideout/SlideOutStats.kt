package com.joclegg.puzzles.slideout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.joclegg.puzzles.core.ui.formatDuration
import com.joclegg.puzzles.core.ui.stats.HistoryEntry
import com.joclegg.puzzles.core.ui.stats.HistoryFilter
import com.joclegg.puzzles.core.ui.stats.StatsLine
import com.joclegg.puzzles.core.ui.stats.StatsSource
import com.joclegg.puzzles.core.ui.stats.StatsTable
import com.joclegg.puzzles.slideout.core.data.GameRepository
import com.joclegg.puzzles.slideout.core.data.GameStatus
import com.joclegg.puzzles.slideout.core.data.LevelStats
import com.joclegg.puzzles.slideout.core.data.SavedGame
import com.joclegg.puzzles.slideout.engine.Level
import com.joclegg.puzzles.slideout.ui.LocalSlideColors
import com.joclegg.puzzles.slideout.ui.label
import com.joclegg.puzzles.slideout.ui.title
import com.joclegg.puzzles.stats.puzzleStatusLabel

const val SLIDE_OUT_STATS_FOOTNOTE = "Solved out of played. At par means solved in the fewest possible moves."

val slideOutHistoryFilters = Level.entries.map { HistoryFilter(it, it.label().first().toString(), it.name.lowercase()) }

fun slideOutStatsSource(games: GameRepository) =
    StatsSource<List<LevelStats>, SavedGame, Level>(
        stats = games.stats,
        initialStats = emptyList(),
        history = games.history,
        matches = { game, level -> game.ref.level == level },
        entry = SavedGame::historyEntry,
        replay = { game -> games.start { game.ref }.id },
    )

private fun SavedGame.historyEntry() =
    HistoryEntry(
        id = id,
        title = ref.title(),
        startedAt = startedAt,
        elapsedMs = elapsedMs,
        summary = "${puzzleStatusLabel(status.name)} · ${moves.size} moves",
        inProgress = status == GameStatus.IN_PROGRESS,
    )

@Composable
fun SlideOutStatsTable(
    stats: List<LevelStats>,
    modifier: Modifier = Modifier,
) {
    StatsTable(
        header = StatsLine("Level", "Solved", "At par", "Best"),
        lines =
            stats.map { level ->
                StatsLine(
                    level.level.label(),
                    "${level.solved}/${level.played}",
                    level.atPar.toString(),
                    level.bestMs?.let(::formatDuration) ?: "–",
                    tag = "level_${level.level.name.lowercase()}",
                )
            },
        modifier = modifier,
        accent = LocalSlideColors.current.accent,
    )
}
