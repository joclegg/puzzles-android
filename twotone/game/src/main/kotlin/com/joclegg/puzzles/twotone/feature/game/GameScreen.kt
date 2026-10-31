package com.joclegg.puzzles.twotone.feature.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.WinDialog
import com.joclegg.puzzles.core.ui.WinStat
import com.joclegg.puzzles.twotone.core.ui.BackButton
import com.joclegg.puzzles.twotone.core.ui.ColourPicker
import com.joclegg.puzzles.twotone.core.ui.HowToPlayButton
import com.joclegg.puzzles.twotone.core.ui.LocalTileColors
import com.joclegg.puzzles.twotone.core.ui.ToolButton
import com.joclegg.puzzles.twotone.core.ui.TourStep
import com.joclegg.puzzles.twotone.core.ui.TwoToneBoard
import com.joclegg.puzzles.twotone.core.ui.TwoToneIcons
import com.joclegg.puzzles.twotone.core.ui.dialogTestTags
import com.joclegg.puzzles.twotone.core.ui.formatDuration
import com.joclegg.puzzles.twotone.core.ui.rememberTourState
import com.joclegg.puzzles.twotone.core.ui.tourTarget
import com.joclegg.puzzles.twotone.engine.Tile
import kotlinx.coroutines.launch

@Composable
fun GameRoute(
    viewModel: GameViewModel,
    onOpenGame: (Long) -> Unit,
    onSettings: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(viewModel) {
        viewModel.onVisible()
        onPauseOrDispose { viewModel.onHidden() }
    }
    state?.let {
        GameScreen(
            state = it,
            onTileClick = viewModel::onTileClick,
            onColourClick = viewModel::onColourClick,
            onUndo = viewModel::onUndo,
            onInputModeToggle = viewModel::onInputModeToggle,
            onHint = viewModel::onHint,
            onRestart = viewModel::onRestart,
            onRevealSolution = viewModel::onRevealSolution,
            onNewPuzzle = { scope.launch { onOpenGame(viewModel.newPuzzle()) } },
            onSettings = onSettings,
            onHome = onHome,
            modifier = modifier,
        )
    }
}

@Composable
fun GameScreen(
    state: GameUiState,
    onTileClick: (Int) -> Unit,
    onColourClick: (Tile) -> Unit,
    onUndo: () -> Unit,
    onInputModeToggle: () -> Unit,
    onHint: () -> Unit,
    onRestart: () -> Unit,
    onRevealSolution: () -> Unit,
    onNewPuzzle: () -> Unit,
    onSettings: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var revealing by remember { mutableStateOf(false) }
    val tour = rememberTourState()
    var showGivens by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onHome)
            Column(Modifier.weight(1f)) {
                Text(state.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("game_title"))
                Row {
                    if (state.showTimer) {
                        Text(formatDuration(state.elapsedMs), modifier = Modifier.testTag("timer"))
                        Spacer(Modifier.width(12.dp))
                    }
                    if (state.showProgress) {
                        Text("${state.progressPercent}%", modifier = Modifier.testTag("progress"))
                    }
                }
            }
            ToolButton(TwoToneIcons.Undo, "Undo the last tap", onUndo, tag = "undo", enabled = state.canUndo)
            ToolButton(
                TwoToneIcons.StartingTiles,
                if (showGivens) {
                    "Hide the starting tiles"
                } else {
                    "Show the starting tiles"
                },
                { showGivens = !showGivens },
                tag = "starting_tiles",
            )
            ToolButton(
                TwoToneIcons.Palette,
                if (state.pickColour) {
                    "Switch to tap to cycle"
                } else {
                    "Switch to pick a colour"
                },
                onInputModeToggle,
                tag = "input_mode",
            )
            if (state.showHints) {
                ToolButton(
                    TwoToneIcons.Hint,
                    "Show a hint",
                    onHint,
                    tag = "hint",
                    modifier = Modifier.tourTarget(tour, "hint"),
                    enabled = !state.solved,
                )
            }
            if (state.showTutorial) {
                HowToPlayButton(RULES, TUTORIAL, tour)
            }
            Column {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("game_menu").tourTarget(tour, "menu")) {
                    Icon(TwoToneIcons.More, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.dialogTestTags()) {
                    DropdownMenuItem(
                        text = { Text("Restart this puzzle") },
                        enabled = !state.solved,
                        onClick = {
                            menuOpen = false
                            onRestart()
                        },
                        modifier = Modifier.testTag("restart"),
                    )
                    DropdownMenuItem(
                        text = { Text("New puzzle") },
                        onClick = {
                            menuOpen = false
                            onNewPuzzle()
                        },
                        modifier = Modifier.testTag("new_puzzle"),
                    )
                    if (state.allowReveal && !state.solved) {
                        DropdownMenuItem(
                            text = { Text("Show solution") },
                            onClick = {
                                menuOpen = false
                                revealing = true
                            },
                            modifier = Modifier.testTag("menu_solution"),
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            menuOpen = false
                            onSettings()
                        },
                        modifier = Modifier.testTag("menu_settings"),
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        TwoToneBoard(
            state.board.copy(showGivens = showGivens),
            onTileClick,
            Modifier.tourTarget(tour, "board").fillMaxWidth().padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(16.dp))
        if (state.pickColour) {
            ColourPicker(state.colour, onColourClick)
            Spacer(Modifier.height(16.dp))
        }
        state.hintMessage?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 24.dp).testTag("hint_message"),
            )
        }
    }
    if (revealing) {
        AlertDialog(
            onDismissRequest = { revealing = false },
            modifier = Modifier.dialogTestTags(),
            title = { Text("Show the solution?") },
            text = { Text("This ends the puzzle. It won't count towards best times.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        revealing = false
                        onRevealSolution()
                    },
                    modifier = Modifier.testTag("confirm_reveal"),
                ) { Text("Show solution") }
            },
            dismissButton = { TextButton(onClick = { revealing = false }) { Text("Cancel") } },
        )
    }
    if (state.solved && state.resultSaved) {
        SolvedDialog(state, onNewPuzzle, onHome)
    }
}

@Composable
private fun SolvedDialog(
    state: GameUiState,
    onNewPuzzle: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WinDialog(
        title = "Solved!",
        accent = LocalTileColors.current.accent,
        dailyGroup = "TWO_TONE",
        newLabel = state.levelLabel,
        newTag = "solved_new_puzzle",
        onNew = onNewPuzzle,
        newBest = state.newBest,
        stats =
            buildList {
                add(WinStat("Time", formatDuration(state.elapsedMs), "solved_time"))
                if (state.hints > 0) {
                    add(WinStat("Hints", "${state.hints}"))
                }
            },
        modifier = modifier.dialogTestTags().testTag("solved_dialog"),
    ) {
        TextButton(onClick = onHome, modifier = Modifier.testTag("solved_home")) { Text("Home") }
    }
}

private val RULES =
    listOf(
        "Fill every tile red or blue. Tap a tile to cycle it: red, blue, empty.",
        "No three tiles of the same colour next to each other in a row or column.",
        "Every row and column has as many red tiles as blue.",
        "No two rows are the same, and no two columns are the same.",
    )

private val TUTORIAL =
    listOf(
        TourStep(
            "board",
            "Tap a tile to cycle it red, blue, empty. No three of a colour in a line, and each row and column is half red, half blue.",
        ),
        TourStep("hint", "Stuck? The bulb shows you the next step."),
        TourStep("menu", "Restart, show the solution and settings live in this menu."),
    )
