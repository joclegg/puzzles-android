package com.joclegg.puzzles.sudoku.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.ContinueCard
import com.joclegg.puzzles.core.ui.GameHero
import com.joclegg.puzzles.core.ui.IconTile
import com.joclegg.puzzles.core.ui.LevelMeter
import com.joclegg.puzzles.core.ui.MenuIcons
import com.joclegg.puzzles.core.ui.MenuSection
import com.joclegg.puzzles.core.ui.MenuTile
import com.joclegg.puzzles.core.ui.TileGrid
import com.joclegg.puzzles.core.ui.todayLabel
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.Medal
import com.joclegg.puzzles.sudoku.core.data.SavedGame
import com.joclegg.puzzles.sudoku.core.data.levels
import com.joclegg.puzzles.sudoku.core.ui.BackButton
import com.joclegg.puzzles.sudoku.core.ui.LocalBoardColors
import com.joclegg.puzzles.sudoku.core.ui.MedalBadge
import com.joclegg.puzzles.sudoku.core.ui.MedalColour
import com.joclegg.puzzles.sudoku.core.ui.SudokuGameIcon
import com.joclegg.puzzles.sudoku.core.ui.SudokuIcons
import com.joclegg.puzzles.sudoku.core.ui.dialogTestTags
import com.joclegg.puzzles.sudoku.core.ui.formatDuration
import com.joclegg.puzzles.sudoku.core.ui.gameTitle
import com.joclegg.puzzles.sudoku.core.ui.label
import com.joclegg.puzzles.sudoku.engine.Difficulty
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onOpenGame: (Long) -> Unit,
    onDailyCalendar: () -> Unit,
    onEnterPuzzle: (GameMode) -> Unit,
    onStats: () -> Unit,
    onHistory: () -> Unit,
    onSettings: (GameMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialMode: GameMode = GameMode.CLASSIC,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var starting by remember { mutableStateOf(false) }

    fun launchGame(start: suspend () -> Long) {
        if (starting) {
            return
        }
        starting = true
        scope.launch {
            try {
                onOpenGame(start())
            } finally {
                starting = false
            }
        }
    }
    HomeScreen(
        state = state,
        initialMode = initialMode,
        onResume = onOpenGame,
        onStart = { mode, level -> launchGame { viewModel.start(mode, level) } },
        onDaily = { mode -> launchGame { viewModel.startDaily(mode) } },
        onDailyCalendar = onDailyCalendar,
        onEnterPuzzle = onEnterPuzzle,
        onStats = onStats,
        onHistory = onHistory,
        onSettings = onSettings,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onResume: (Long) -> Unit,
    onStart: (GameMode, Difficulty) -> Unit,
    onDaily: (GameMode) -> Unit,
    onDailyCalendar: () -> Unit,
    onEnterPuzzle: (GameMode) -> Unit,
    onStats: () -> Unit,
    onHistory: () -> Unit,
    onSettings: (GameMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialMode: GameMode = GameMode.CLASSIC,
) {
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    var resumeChoice by remember { mutableStateOf<Pair<Difficulty, SavedGame>?>(null) }
    val accent = LocalBoardColors.current.accent
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Sudoku", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onStats, modifier = Modifier.testTag("stats")) {
                Icon(MenuIcons.Stats, contentDescription = "Best times")
            }
            IconButton(onClick = onHistory, modifier = Modifier.testTag("history")) {
                Icon(MenuIcons.History, contentDescription = "History")
            }
            IconButton(onClick = { onSettings(mode) }, modifier = Modifier.testTag("settings")) {
                Icon(SudokuIcons.Settings, contentDescription = "Settings")
            }
        }
        GameHero("Classic and killer, from 6x6 to evil", accent) { SudokuGameIcon() }
        state.inProgress.firstOrNull()?.let { latest ->
            ContinueCard(
                detail = "${continueTitle(latest)} · ${formatDuration(latest.elapsedMs)}",
                accent = accent,
                onClick = { onResume(latest.id) },
                modifier = Modifier.testTag("continue"),
            )
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            GameMode.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = mode == option,
                    onClick = { mode = option },
                    shape = SegmentedButtonDefaults.itemShape(index, GameMode.entries.size),
                    modifier = Modifier.testTag("mode_${option.name.lowercase()}"),
                ) {
                    Text(
                        if (option == GameMode.CLASSIC) {
                            "Classic"
                        } else {
                            "Killer"
                        },
                    )
                }
            }
        }
        MenuSection("Today", aside = todayLabel()) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MenuTile(
                    label = state.dailyLevels[mode]?.let { "Daily puzzle · ${it.label()}" } ?: "Daily puzzle",
                    accent = accent,
                    onClick = { onDaily(mode) },
                    modifier = Modifier.weight(1f).fillMaxHeight().testTag("daily"),
                    icon = MenuIcons.Today,
                    active = state.todaysMedal != Medal.NONE,
                    trailing =
                        if (state.todaysMedal == Medal.NONE) {
                            null
                        } else {
                            { TodaysMedal(state.todaysMedal) }
                        },
                )
                IconTile(
                    icon = MenuIcons.Calendar,
                    contentDescription = "Daily calendar",
                    accent = accent,
                    onClick = onDailyCalendar,
                    modifier = Modifier.fillMaxHeight().testTag("daily_calendar"),
                )
            }
        }
        MenuSection("New game") {
            TileGrid(count = mode.levels.size, columns = 2) { index, tileModifier ->
                val level = mode.levels[index]
                val saved = state.savedFor(mode, level)
                MenuTile(
                    label = level.label(),
                    accent = accent,
                    onClick = {
                        if (saved != null) {
                            resumeChoice = level to saved
                        } else {
                            onStart(mode, level)
                        }
                    },
                    modifier = tileModifier.testTag("level_${level.name.lowercase()}"),
                    detail = saved?.let { "In progress · ${formatDuration(it.elapsedMs)}" },
                    active = saved != null,
                    meter = { LevelMeter(rank = index, count = mode.levels.size, accent = accent) },
                )
            }
            MenuTile(
                label = "Enter puzzle",
                accent = accent,
                onClick = { onEnterPuzzle(mode) },
                modifier = Modifier.fillMaxWidth().testTag("enter_puzzle"),
                icon = MenuIcons.Pencil,
            )
        }
    }
    resumeChoice?.let { (level, saved) ->
        AlertDialog(
            onDismissRequest = { resumeChoice = null },
            modifier = Modifier.dialogTestTags().testTag("resume_dialog"),
            title = { Text("${if (mode == GameMode.KILLER) "Killer" else "Classic"} · ${level.label()}") },
            text = { Text("You have a game in progress (${formatDuration(saved.elapsedMs)}).") },
            confirmButton = {
                TextButton(
                    onClick = {
                        resumeChoice = null
                        onResume(saved.id)
                    },
                    modifier = Modifier.testTag("resume_game"),
                ) { Text("Resume") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        resumeChoice = null
                        onStart(mode, level)
                    },
                    modifier = Modifier.testTag("new_game"),
                ) { Text("New game") }
            },
        )
    }
}

@Composable
private fun TodaysMedal(
    medal: Medal,
    modifier: Modifier = Modifier,
) {
    if (medal == Medal.GOLD) {
        MedalBadge(MedalColour.GOLD, modifier.testTag("todays_medal_gold"), size = 24.dp)
    } else {
        MedalBadge(MedalColour.SILVER, modifier.testTag("todays_medal_silver"), size = 24.dp)
    }
}

private fun continueTitle(game: SavedGame): String =
    gameTitle(
        killer = game.ref.mode == GameMode.KILLER,
        custom = game.ref.custom,
        difficulty = game.ref.difficulty,
        answerKnown = game.ref.puzzle.solution != null,
    )
