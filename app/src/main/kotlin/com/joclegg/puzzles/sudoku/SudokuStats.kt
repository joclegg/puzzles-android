package com.joclegg.puzzles.sudoku

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.joclegg.puzzles.core.ui.formatDuration
import com.joclegg.puzzles.core.ui.stats.HistoryEntry
import com.joclegg.puzzles.core.ui.stats.HistoryFilter
import com.joclegg.puzzles.core.ui.stats.StatsLine
import com.joclegg.puzzles.core.ui.stats.StatsSource
import com.joclegg.puzzles.core.ui.stats.StatsTable
import com.joclegg.puzzles.stats.puzzleStatusLabel
import com.joclegg.puzzles.sudoku.core.data.BestTime
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.GameRepository
import com.joclegg.puzzles.sudoku.core.data.GameStatus
import com.joclegg.puzzles.sudoku.core.data.SavedGame
import com.joclegg.puzzles.sudoku.core.data.SettingsRepository
import com.joclegg.puzzles.sudoku.core.data.levels
import com.joclegg.puzzles.sudoku.core.ui.LocalBoardColors
import com.joclegg.puzzles.sudoku.core.ui.gameTitle
import com.joclegg.puzzles.sudoku.core.ui.label
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import kotlinx.coroutines.flow.first

enum class SudokuHistoryFilter { CLASSIC, KILLER, CUSTOM }

const val SUDOKU_STATS_FOOTNOTE = "Custom puzzles show in History but don't count towards best times."

val sudokuHistoryFilters =
    SudokuHistoryFilter.entries.map { HistoryFilter(it, it.name.lowercase().replaceFirstChar { c -> c.uppercase() }, it.name.lowercase()) }

fun sudokuStatsSource(
    games: GameRepository,
    settings: SettingsRepository,
) = StatsSource<List<BestTime>, SavedGame, SudokuHistoryFilter>(
    stats = games.bestTimes,
    initialStats = emptyList(),
    history = games.history,
    matches = SavedGame::matches,
    entry = SavedGame::historyEntry,
    replay = { game -> games.start(game.ref, settings.settings.first().mistakeCheckFor(game.ref.mode)).id },
)

private fun SavedGame.matches(filter: SudokuHistoryFilter) =
    when (filter) {
        SudokuHistoryFilter.CUSTOM -> ref.custom
        SudokuHistoryFilter.CLASSIC -> !ref.custom && ref.mode == GameMode.CLASSIC
        SudokuHistoryFilter.KILLER -> !ref.custom && ref.mode == GameMode.KILLER
    }

private fun SavedGame.historyEntry(): HistoryEntry {
    val status = puzzleStatusLabel(status.name)
    return HistoryEntry(
        id = id,
        title =
            gameTitle(
                killer = ref.mode == GameMode.KILLER,
                custom = ref.custom,
                difficulty = ref.difficulty,
                answerKnown = ref.puzzle.solution != null,
            ),
        startedAt = startedAt,
        elapsedMs = elapsedMs,
        summary =
            if (mistakeCheck != MistakeCheck.OFF) {
                "$status · $mistakes mistakes"
            } else {
                status
            },
        inProgress = this.status == GameStatus.IN_PROGRESS,
    )
}

@Composable
fun SudokuBestTimes(
    bestTimes: List<BestTime>,
    mode: GameMode,
    modifier: Modifier = Modifier,
) {
    StatsTable(
        header = StatsLine("Level", "Solved", "Best", "Average"),
        lines =
            mode.levels.map { level ->
                val stats = bestTimes.find { it.mode == mode && it.difficulty == level }
                StatsLine(
                    level.label(),
                    (stats?.completed ?: 0).toString(),
                    stats?.let { formatDuration(it.bestMs) } ?: "–",
                    stats?.takeIf { it.completed > 0 }?.let { formatDuration(it.averageMs) } ?: "–",
                    tag = "best_${mode.name.lowercase()}_${level.name.lowercase()}",
                )
            },
        accent = LocalBoardColors.current.accent,
        modifier = modifier,
        heading =
            if (mode == GameMode.CLASSIC) {
                "Classic"
            } else {
                "Killer"
            },
        labelWeight = 1.3f,
    )
}
