package com.joclegg.puzzles.sudoku.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.Medal
import com.joclegg.puzzles.sudoku.core.data.SavedGame
import com.joclegg.puzzles.sudoku.core.data.levels
import com.joclegg.puzzles.sudoku.core.ui.BackButton
import com.joclegg.puzzles.sudoku.core.ui.MedalBadge
import com.joclegg.puzzles.sudoku.core.ui.MedalColour
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
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Sudoku", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { onSettings(mode) }, modifier = Modifier.testTag("settings")) {
                Icon(SudokuIcons.Settings, contentDescription = "Settings")
            }
        }
        state.inProgress.firstOrNull()?.let { latest -> ContinueCard(latest, onContinue = { onResume(latest.id) }) }
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
        mode.levels.forEach { level ->
            val saved = state.savedFor(mode, level)
            Button(
                onClick = {
                    if (saved != null) {
                        resumeChoice = level to saved
                    } else {
                        onStart(mode, level)
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("level_${level.name.lowercase()}"),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(level.label())
                    if (saved != null) {
                        Text("In progress · ${formatDuration(saved.elapsedMs)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onDaily(mode) }, modifier = Modifier.weight(1f).testTag("daily")) {
                Text(state.dailyLevels[mode]?.let { "Daily puzzle · ${it.label()}" } ?: "Daily puzzle")
                when (state.todaysMedal) {
                    Medal.GOLD -> MedalBadge(MedalColour.GOLD, Modifier.padding(start = 8.dp).testTag("todays_medal_gold"))
                    Medal.SILVER -> MedalBadge(MedalColour.SILVER, Modifier.padding(start = 8.dp).testTag("todays_medal_silver"))
                    Medal.NONE -> Unit
                }
            }
            IconButton(onClick = onDailyCalendar, modifier = Modifier.testTag("daily_calendar")) {
                Icon(SudokuIcons.Calendar, contentDescription = "Daily calendar")
            }
        }
        OutlinedButton(onClick = { onEnterPuzzle(mode) }, modifier = Modifier.fillMaxWidth().testTag("enter_puzzle")) {
            Text("Enter puzzle")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onStats, modifier = Modifier.weight(1f).testTag("stats")) { Text("Best times") }
            OutlinedButton(onClick = onHistory, modifier = Modifier.weight(1f).testTag("history")) { Text("History") }
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
private fun ContinueCard(
    game: SavedGame,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title =
        gameTitle(
            killer = game.ref.mode == GameMode.KILLER,
            custom = game.ref.custom,
            difficulty = game.ref.difficulty,
            answerKnown = game.ref.puzzle.solution != null,
        )
    Card(onClick = onContinue, modifier = modifier.fillMaxWidth().testTag("continue")) {
        Column(Modifier.padding(16.dp)) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
            Text("$title · ${formatDuration(game.elapsedMs)}")
        }
    }
}
