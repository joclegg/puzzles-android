package com.joclegg.puzzles.wordwheel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.joclegg.puzzles.core.ui.formatDuration
import com.joclegg.puzzles.core.ui.stats.HistoryEntry
import com.joclegg.puzzles.core.ui.stats.HistoryFilter
import com.joclegg.puzzles.core.ui.stats.StatsLine
import com.joclegg.puzzles.core.ui.stats.StatsSource
import com.joclegg.puzzles.core.ui.stats.StatsTable
import com.joclegg.puzzles.stats.puzzleStatusLabel
import com.joclegg.puzzles.wordwheel.core.data.GameRepository
import com.joclegg.puzzles.wordwheel.core.data.GameStatus
import com.joclegg.puzzles.wordwheel.core.data.LevelStats
import com.joclegg.puzzles.wordwheel.core.data.SavedGame
import com.joclegg.puzzles.wordwheel.core.ui.LocalWheelColors
import com.joclegg.puzzles.wordwheel.core.ui.label
import com.joclegg.puzzles.wordwheel.core.ui.title
import com.joclegg.puzzles.wordwheel.engine.Level

const val WORD_WHEEL_STATS_FOOTNOTE = "Solved out of played. Bonus counts every extra word you found."

val wordWheelHistoryFilters = Level.entries.map { HistoryFilter(it, "${it.letters}", "${it.letters}") }

fun wordWheelStatsSource(games: GameRepository) =
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
        summary = "${puzzleStatusLabel(status.name)} · ${found.size} words, ${bonus.size} bonus",
        inProgress = status == GameStatus.IN_PROGRESS,
    )

@Composable
fun WordWheelStatsTable(
    stats: List<LevelStats>,
    modifier: Modifier = Modifier,
) {
    StatsTable(
        header = StatsLine("Letters", "Solved", "Bonus", "Best"),
        lines =
            stats.map { level ->
                StatsLine(
                    level.level.label(),
                    "${level.solved}/${level.played}",
                    level.bonusWords.toString(),
                    level.bestMs?.let(::formatDuration) ?: "–",
                    tag = "level_${level.level.letters}",
                )
            },
        modifier = modifier,
        accent = LocalWheelColors.current.accent,
    )
}
