package com.joclegg.puzzles.kakuro.feature.game

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
import com.joclegg.puzzles.kakuro.feature.R
import com.joclegg.puzzles.kakuro.ui.HintButton
import com.joclegg.puzzles.kakuro.ui.HowToPlayButton
import com.joclegg.puzzles.kakuro.ui.KakuroBoard
import com.joclegg.puzzles.kakuro.ui.KakuroIcons
import com.joclegg.puzzles.kakuro.ui.KeypadTool
import com.joclegg.puzzles.kakuro.ui.LocalKakuroColors
import com.joclegg.puzzles.kakuro.ui.NumberPad
import com.joclegg.puzzles.kakuro.ui.ToolButton
import com.joclegg.puzzles.kakuro.ui.TourStep
import com.joclegg.puzzles.kakuro.ui.dialogTestTags
import com.joclegg.puzzles.kakuro.ui.rememberTourState
import com.joclegg.puzzles.kakuro.ui.tourTarget
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
            onCellClick = viewModel::onCellClick,
            onDigitClick = viewModel::onDigitClick,
            onErase = viewModel::onErase,
            onToggleNotes = viewModel::onToggleNotes,
            onMark = viewModel::onMark,
            onUndo = viewModel::onUndo,
            onRedo = viewModel::onRedo,
            onRestart = viewModel::onRestart,
            onRevealSolution = viewModel::onRevealSolution,
            onHint = viewModel::onHint,
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
    onCellClick: (Int) -> Unit,
    onDigitClick: (Int) -> Unit,
    onErase: () -> Unit,
    onToggleNotes: () -> Unit,
    onMark: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onRestart: () -> Unit,
    onRevealSolution: () -> Unit,
    onHint: () -> Unit,
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
                    if (state.showMistakeCount) {
                        Text("Mistakes ${state.mistakes}", modifier = Modifier.testTag("mistakes"))
                    }
                }
            }
            ToolButton(KakuroIcons.Undo, stringResource(R.string.undo_the_last_tap), onUndo, tag = "undo", enabled = state.canUndo)
            ToolButton(KakuroIcons.Redo, stringResource(R.string.redo_the_tap_you_undid), onRedo, tag = "redo", enabled = state.canRedo)
            if (state.showHints) {
                HintButton(onHint, enabled = !state.solved, modifier = Modifier.tourTarget(tour, "hint"))
            }
            if (state.showTutorial) {
                HowToPlayButton(RULES, TUTORIAL, tour)
            }
            Column {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("game_menu").tourTarget(tour, "menu")) {
                    Icon(KakuroIcons.More, contentDescription = stringResource(R.string.more))
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
        KakuroBoard(state.board, onCellClick, Modifier.tourTarget(tour, "board").fillMaxWidth().padding(horizontal = 8.dp))
        Text(
            state.hintMessage.orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp).testTag("hint_message"),
        )
        Spacer(Modifier.height(12.dp))
        state.combinations.forEach {
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp).testTag("combinations"))
        }
        Spacer(Modifier.weight(1f))
        NumberPad(
            onDigitClick,
            Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
            activeDigit = state.activeDigit,
            doneDigits = state.doneDigits,
            tools =
                listOf(
                    KeypadTool(
                        KakuroIcons.Eraser,
                        when {
                            !state.digitFirst -> stringResource(R.string.clear_the_selected_cell)
                            state.eraserActive -> stringResource(R.string.erase_is_on_tap_cells_to_clear_them)
                            else -> stringResource(R.string.turn_on_erase_then_tap_cells_to_clear)
                        },
                        onErase,
                        tag = "erase",
                        on = state.eraserActive,
                    ),
                    KeypadTool(
                        KakuroIcons.Pencil,
                        if (state.notesMode) {
                            stringResource(R.string.notes_are_on_digits_go_in_as_notes)
                        } else {
                            stringResource(R.string.notes_are_off_digits_go_in_as_answers)
                        },
                        onToggleNotes,
                        tag = "notes",
                        on = state.notesMode,
                    ),
                    KeypadTool(
                        KakuroIcons.Mark,
                        if (state.digitFirst) {
                            stringResource(R.string.mark_tap_cells_to_box_them_in_red_tap)
                        } else {
                            stringResource(R.string.box_the_selected_cell_in_red_to)
                        },
                        onMark,
                        tag = "mark",
                        on = state.markActive,
                    ),
                ),
        )
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
        accent = LocalKakuroColors.current.accent,
        dailyGroup = "KAKURO",
        newLabel = state.levelLabel,
        newTag = "solved_new_puzzle",
        onNew = onNewPuzzle,
        newBest = state.newBest,
        stats =
            buildList {
                add(WinStat(stringResource(R.string.time), formatDuration(state.elapsedMs), "solved_time"))
            },
        modifier = modifier.dialogTestTags().testTag("solved_dialog"),
    ) {
        TextButton(onClick = onHome, modifier = Modifier.testTag("solved_home")) { Text(stringResource(R.string.home)) }
    }
}

private val RULES =
    listOf(
        "Fill each white cell with a digit from 1 to 9.",
        "The number above a diagonal is the sum of the run to its right; below it, the run beneath.",
        "A digit can't repeat within a run.",
    )

private val TUTORIAL =
    listOf(
        TourStep("board", "Each white cell takes a digit from 1 to 9. A clue is the sum of the run to its right or below it."),
        TourStep("hint", "Stuck? The bulb shows you the next step."),
        TourStep("menu", "Restart, show the solution and settings live in this menu."),
    )
