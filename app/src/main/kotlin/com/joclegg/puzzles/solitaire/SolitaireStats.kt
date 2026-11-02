package com.joclegg.puzzles.solitaire

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.joclegg.puzzles.R
import com.joclegg.puzzles.core.ui.MenuPanel
import com.joclegg.puzzles.core.ui.formatDuration
import com.joclegg.puzzles.core.ui.stats.HistoryEntry
import com.joclegg.puzzles.core.ui.stats.HistoryFilter
import com.joclegg.puzzles.core.ui.stats.StatsSource
import com.joclegg.puzzles.solitaire.core.data.DrawStats
import com.joclegg.puzzles.solitaire.core.data.GameRepository
import com.joclegg.puzzles.solitaire.core.data.GameStatus
import com.joclegg.puzzles.solitaire.core.data.SavedGame
import com.joclegg.puzzles.solitaire.engine.DrawMode
import com.joclegg.puzzles.solitaire.ui.LocalSolitaireColors
import com.joclegg.puzzles.solitaire.ui.label
import com.joclegg.puzzles.solitaire.ui.title
import com.joclegg.puzzles.stats.puzzleStatusLabel

const val SOLITAIRE_STATS_FOOTNOTE = "Abandoned deals count as played. Daily deals count under the draw mode they were played with."

val solitaireHistoryFilters = DrawMode.entries.map { HistoryFilter(it, it.label(), it.name.lowercase()) }

fun solitaireStatsSource(games: GameRepository) =
    StatsSource<List<DrawStats>, SavedGame, DrawMode>(
        stats = games.stats,
        initialStats = emptyList(),
        history = games.history,
        matches = { game, drawMode -> game.deal.drawMode == drawMode },
        entry = SavedGame::historyEntry,
        replay = { game -> games.start(game.deal).id },
    )

private fun SavedGame.historyEntry() =
    HistoryEntry(
        id = id,
        title = deal.title(),
        startedAt = startedAt,
        elapsedMs = elapsedMs,
        summary = "${puzzleStatusLabel(status.name)} · ${moves.size} moves",
        inProgress = status == GameStatus.IN_PROGRESS,
    )

@Composable
fun SolitaireStatsPanel(
    mode: DrawStats,
    modifier: Modifier = Modifier,
) {
    val tag = mode.drawMode.name.lowercase()
    MenuPanel(LocalSolitaireColors.current.accent, modifier) {
        Text(mode.drawMode.label(), style = MaterialTheme.typography.titleLarge)
        StatRow(stringResource(R.string.played), mode.played.toString(), "${tag}_played")
        StatRow(stringResource(R.string.won), mode.won.toString(), "${tag}_won")
        StatRow(stringResource(R.string.win_rate), mode.winRate(), "${tag}_win_rate")
        StatRow(stringResource(R.string.best_time), mode.bestMs?.let(::formatDuration) ?: "–", "${tag}_best")
        StatRow(stringResource(R.string.average_time), mode.averageMs?.let(::formatDuration) ?: "–", "${tag}_average")
        StatRow(stringResource(R.string.fewest_moves), mode.fewestMoves?.toString() ?: "–", "${tag}_fewest_moves")
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, modifier = Modifier.testTag(tag))
    }
}

private fun DrawStats.winRate(): String =
    if (played == 0) {
        "–"
    } else {
        "${won * PERCENT / played}%"
    }

private const val PERCENT = 100
