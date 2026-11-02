package com.joclegg.puzzles.sightlines.feature.game

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.BackButton
import com.joclegg.puzzles.core.ui.HelpersHint
import com.joclegg.puzzles.core.ui.WinDialog
import com.joclegg.puzzles.core.ui.WinStat
import com.joclegg.puzzles.core.ui.formatDuration
import com.joclegg.puzzles.sightlines.engine.Dot
import com.joclegg.puzzles.sightlines.feature.R
import com.joclegg.puzzles.sightlines.ui.ColourPicker
import com.joclegg.puzzles.sightlines.ui.HowToPlayButton
import com.joclegg.puzzles.sightlines.ui.LocalSightlinesColors
import com.joclegg.puzzles.sightlines.ui.SightlinesBoard
import com.joclegg.puzzles.sightlines.ui.SightlinesIcons
import com.joclegg.puzzles.sightlines.ui.ToolButton
import com.joclegg.puzzles.sightlines.ui.TourStep
import com.joclegg.puzzles.sightlines.ui.dialogTestTags
import com.joclegg.puzzles.sightlines.ui.rememberTourState
import com.joclegg.puzzles.sightlines.ui.tourTarget
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
            onDotClick = viewModel::onDotClick,
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
    onDotClick: (Int) -> Unit,
    onColourClick: (Dot) -> Unit,
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
            ToolButton(SightlinesIcons.Undo, stringResource(R.string.undo_the_last_tap), onUndo, tag = "undo", enabled = state.canUndo)
            ToolButton(
                SightlinesIcons.Swap,
                if (state.pickColour) {
                    stringResource(R.string.pick_a_colour)
                } else {
                    stringResource(R.string.tap_to_cycle)
                },
                onInputModeToggle,
                tag = "input_mode",
            )
            if (state.showHints) {
                ToolButton(
                    SightlinesIcons.Hint,
                    stringResource(R.string.show_a_hint),
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
                    Icon(SightlinesIcons.More, contentDescription = stringResource(R.string.more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.dialogTestTags()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.restart_this_puzzle)) },
                        enabled = !state.solved,
                        onClick = {
                            menuOpen = false
                            onRestart()
                        },
                        modifier = Modifier.testTag("restart"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.new_puzzle)) },
                        onClick = {
                            menuOpen = false
                            onNewPuzzle()
                        },
                        modifier = Modifier.testTag("new_puzzle"),
                    )
                    if (state.allowReveal && !state.solved) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.show_solution)) },
                            onClick = {
                                menuOpen = false
                                revealing = true
                            },
                            modifier = Modifier.testTag("menu_solution"),
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings)) },
                        onClick = {
                            menuOpen = false
                            onSettings()
                        },
                        modifier = Modifier.testTag("menu_settings"),
                    )
                }
            }
        }
        HelpersHint(onSettings)
        Spacer(Modifier.height(24.dp))
        SightlinesBoard(state.board, onDotClick, Modifier.tourTarget(tour, "board").fillMaxWidth().padding(horizontal = 12.dp))
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
            title = { Text(stringResource(R.string.show_the_solution)) },
            text = { Text(stringResource(R.string.this_ends_the_puzzle_it_won_t_count)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        revealing = false
                        onRevealSolution()
                    },
                    modifier = Modifier.testTag("confirm_reveal"),
                ) { Text(stringResource(R.string.show_solution)) }
            },
            dismissButton = { TextButton(onClick = { revealing = false }) { Text(stringResource(R.string.cancel)) } },
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
        title = stringResource(R.string.solved),
        accent = LocalSightlinesColors.current.accent,
        dailyGroup = "SIGHTLINES",
        newLabel = state.levelLabel,
        newTag = "solved_new_puzzle",
        onNew = onNewPuzzle,
        newBest = state.newBest,
        stats =
            buildList {
                add(WinStat(stringResource(R.string.time), formatDuration(state.elapsedMs), "solved_time"))
                if (state.hints > 0) {
                    add(WinStat(stringResource(R.string.hints), "${state.hints}"))
                }
            },
        modifier = modifier.dialogTestTags().testTag("solved_dialog"),
    ) {
        TextButton(onClick = onHome, modifier = Modifier.testTag("solved_home")) { Text(stringResource(R.string.home)) }
    }
}

private val RULES =
    listOf(
        "Fill every dot blue or red. Tap a dot to cycle it: blue, red, empty.",
        "Blue dots can see other blue dots in their own row and column. Red dots block the view.",
        "A number says how many blue dots that dot can see.",
        "Every blue dot must see at least one other.",
    )

private val TUTORIAL =
    listOf(
        TourStep("board", "Tap a dot to cycle it blue, red, empty. A number says how many blue dots it can see; red dots block the view."),
        TourStep("hint", "Stuck? The bulb shows you the next step."),
        TourStep("menu", "Restart, show the solution and settings live in this menu."),
    )
