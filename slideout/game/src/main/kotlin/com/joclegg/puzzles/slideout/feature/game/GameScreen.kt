package com.joclegg.puzzles.slideout.feature.game

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.WinDialog
import com.joclegg.puzzles.core.ui.WinStat
import com.joclegg.puzzles.slideout.core.ui.BackButton
import com.joclegg.puzzles.slideout.core.ui.HowToPlayButton
import com.joclegg.puzzles.slideout.core.ui.LocalSlideColors
import com.joclegg.puzzles.slideout.core.ui.SlideBoard
import com.joclegg.puzzles.slideout.core.ui.SlideOutIcons
import com.joclegg.puzzles.slideout.core.ui.ToolButton
import com.joclegg.puzzles.slideout.core.ui.TourStep
import com.joclegg.puzzles.slideout.core.ui.dialogTestTags
import com.joclegg.puzzles.slideout.core.ui.formatDuration
import com.joclegg.puzzles.slideout.core.ui.rememberTourState
import com.joclegg.puzzles.slideout.core.ui.tourTarget
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
            onSlide = viewModel::onSlide,
            onUndo = viewModel::onUndo,
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
    onSlide: (Int, Int) -> Unit,
    onUndo: () -> Unit,
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
                    Text("Moves ${state.moveCount}", modifier = Modifier.testTag("move_count"))
                    if (state.showPar) {
                        Spacer(Modifier.width(12.dp))
                        Text("Par ${state.par}", modifier = Modifier.testTag("par"))
                    }
                }
            }
            ToolButton(SlideOutIcons.Undo, "Undo the last move", onUndo, tag = "undo", enabled = state.canUndo)
            if (state.showHints) {
                ToolButton(
                    SlideOutIcons.Hint,
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
                    Icon(SlideOutIcons.More, contentDescription = "More")
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
        SlideBoard(state.board, onSlide, Modifier.tourTarget(tour, "board").fillMaxWidth().padding(horizontal = 16.dp))
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
        accent = LocalSlideColors.current.accent,
        newBest = state.newBest,
        stats =
            buildList {
                add(
                    WinStat(
                        "Moves",
                        if (state.moveCount <= state.par) {
                            "${state.moveCount} moves, par!"
                        } else {
                            "${state.moveCount} moves, par is ${state.par}"
                        },
                        "solved_moves",
                    ),
                )
                add(WinStat("Time", formatDuration(state.elapsedMs), "solved_time"))
                if (state.hints > 0) {
                    add(WinStat("Hints", "${state.hints}"))
                }
            },
        modifier = modifier.dialogTestTags().testTag("solved_dialog"),
    ) {
        TextButton(onClick = onHome, modifier = Modifier.testTag("solved_home")) { Text("Home") }
        TextButton(onClick = onNewPuzzle, modifier = Modifier.testTag("solved_new_puzzle")) { Text("New puzzle") }
    }
}

private val RULES =
    listOf(
        "Slide the red block out through the gap on the right.",
        "Blocks only slide along their length: across ones left and right, upright ones up and down.",
        "Each slide of a block is one move, however far it goes. Par is the fewest moves possible.",
    )

private val TUTORIAL =
    listOf(
        TourStep("board", "Drag a block along its length to slide it. Free the red block through the gap on the right."),
        TourStep("hint", "Stuck? The bulb shows you the next step."),
        TourStep("menu", "Restart, show the solution and settings live in this menu."),
    )
