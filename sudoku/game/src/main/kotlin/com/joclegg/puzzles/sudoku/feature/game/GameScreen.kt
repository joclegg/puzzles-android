package com.joclegg.puzzles.sudoku.feature.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.ui.BackButton
import com.joclegg.puzzles.sudoku.core.ui.HintButton
import com.joclegg.puzzles.sudoku.core.ui.HowToPlayButton
import com.joclegg.puzzles.sudoku.core.ui.KeypadTool
import com.joclegg.puzzles.sudoku.core.ui.LocalBoardColors
import com.joclegg.puzzles.sudoku.core.ui.NumberPad
import com.joclegg.puzzles.sudoku.core.ui.SudokuBoard
import com.joclegg.puzzles.sudoku.core.ui.SudokuIcons
import com.joclegg.puzzles.sudoku.core.ui.TourState
import com.joclegg.puzzles.sudoku.core.ui.TourStep
import com.joclegg.puzzles.sudoku.core.ui.dialogTestTags
import com.joclegg.puzzles.sudoku.core.ui.formatDuration
import com.joclegg.puzzles.sudoku.core.ui.gameTitle
import com.joclegg.puzzles.sudoku.core.ui.label
import com.joclegg.puzzles.sudoku.core.ui.rememberTourState
import com.joclegg.puzzles.sudoku.core.ui.tourTarget
import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.InputMode
import kotlinx.coroutines.delay

@Composable
fun GameRoute(
    viewModel: GameViewModel,
    onSettings: (GameMode) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnHome by rememberUpdatedState(onHome)
    LifecycleResumeEffect(viewModel) {
        viewModel.onVisible()
        onPauseOrDispose { viewModel.onHidden() }
    }
    val solved = state?.takeIf { it.resultSaved }
    LaunchedEffect(solved != null) {
        if (solved != null) {
            delay(SOLVED_SCREEN_MS)
            currentOnHome()
        }
    }
    state?.let {
        GameScreen(
            state = it,
            onCellClick = viewModel::onCellClick,
            onDigitClick = viewModel::onDigitClick,
            onUndo = viewModel::onUndo,
            onRedo = viewModel::onRedo,
            onErase = viewModel::onErase,
            onToggleNotes = viewModel::onToggleNotes,
            onMark = viewModel::onMark,
            onRestart = viewModel::onRestart,
            onRevealSolution = viewModel::onRevealSolution,
            onHint = viewModel::onHint,
            onSettings = { onSettings(it.mode) },
            onHome = onHome,
            modifier = modifier,
        )
    }
    solved?.let { SolvedScreen(it, onDismiss = onHome) }
}

@Composable
private fun SolvedScreen(
    state: GameUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
                .testTag("solved_screen"),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Solved!", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
            Text("Great job", style = MaterialTheme.typography.headlineSmall)
            Text(formatDuration(state.elapsedMs), style = MaterialTheme.typography.displaySmall, modifier = Modifier.testTag("final_time"))
            if (state.newBest) {
                Text(
                    "New best!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("new_best"),
                )
            }
        }
    }
}

private const val SOLVED_SCREEN_MS = 2_000L

@Composable
fun GameScreen(
    state: GameUiState,
    onCellClick: (Int) -> Unit,
    onDigitClick: (Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onErase: () -> Unit,
    onToggleNotes: () -> Unit,
    onMark: () -> Unit,
    onRestart: () -> Unit,
    onRevealSolution: () -> Unit,
    onHint: () -> Unit,
    onSettings: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tour = rememberTourState()
    Column(
        modifier.fillMaxSize().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GameHeader(
            state,
            tour = tour,
            onBack = onHome,
            onRestart = onRestart,
            onRevealSolution = onRevealSolution,
            onHint = onHint,
            onUndo = onUndo,
            onRedo = onRedo,
            onSettings = onSettings,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        SudokuBoard(board = state.board, onCellClick = onCellClick, modifier = Modifier.tourTarget(tour, "board"))
        Text(
            state.hintMessage.orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp).testTag("hint_message"),
        )
        NumberPad(
            onDigit = onDigitClick,
            activeDigit = state.activeDigit.takeIf { state.inputMode == InputMode.DIGIT_FIRST },
            doneDigits = state.doneDigits,
            counts =
                if (state.showDigitCounts) {
                    state.digitCounts
                } else {
                    emptyList()
                },
            digits = state.board.grid.digits,
            tools =
                listOf(
                    KeypadTool(
                        icon = SudokuIcons.Eraser,
                        description =
                            if (state.inputMode == InputMode.DIGIT_FIRST) {
                                "Eraser: tap cells to clear them, tap again to stop"
                            } else {
                                "Erase the selected cell"
                            },
                        onClick = onErase,
                        tag = "erase",
                        on = state.eraserActive,
                    ),
                    KeypadTool(
                        icon = SudokuIcons.Pencil,
                        description =
                            if (state.notesMode) {
                                "Notes on: numbers go in as pencil marks"
                            } else {
                                "Notes off: tap to add pencil marks"
                            },
                        onClick = onToggleNotes,
                        tag = "notes",
                        on = state.notesMode,
                    ),
                    KeypadTool(
                        icon = SudokuIcons.Mark,
                        description =
                            if (state.inputMode == InputMode.DIGIT_FIRST) {
                                "Mark: tap cells to box them in red, tap again to stop"
                            } else {
                                "Box the selected cell in red, to remember where a trial started"
                            },
                        onClick = onMark,
                        tag = "mark",
                        on = state.markActive,
                    ),
                ),
            modifier = Modifier.padding(horizontal = 8.dp).tourTarget(tour, "pad"),
        )
    }
}

@Composable
private fun GameHeader(
    state: GameUiState,
    tour: TourState,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onRevealSolution: () -> Unit,
    onHint: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title =
        gameTitle(
            killer = state.mode == GameMode.KILLER,
            custom = state.custom,
            difficulty = state.difficulty,
            answerKnown = state.answerKnown,
        )
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BackButton(onBack)
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).testTag("game_title"))
        if (state.checking && state.showMistakeCount) {
            Text(
                "Mistakes: ${state.mistakes}",
                color =
                    if (state.mistakes > 0) {
                        LocalBoardColors.current.wrong
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.padding(end = 16.dp).testTag("mistakes"),
            )
        }
        if (state.revealed) {
            Text("Solution shown", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("solution_shown"))
        } else if (state.showTimer) {
            Text(
                formatDuration(state.elapsedMs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("timer"),
            )
        }
        IconButton(onClick = onUndo, enabled = state.canUndo, modifier = Modifier.testTag("undo")) {
            Icon(SudokuIcons.Undo, contentDescription = "Undo the last move")
        }
        IconButton(onClick = onRedo, enabled = state.canRedo, modifier = Modifier.testTag("redo")) {
            Icon(SudokuIcons.Redo, contentDescription = "Redo the move you undid")
        }
        if (state.showHints) {
            HintButton(onHint, enabled = !state.completed && !state.revealed, modifier = Modifier.tourTarget(tour, "hint"))
        }
        if (state.showTutorial) {
            HowToPlayButton(RULES, TUTORIAL, tour)
        }
        PuzzleMenu(state, onRestart, onRevealSolution, onSettings, Modifier.tourTarget(tour, "menu"))
    }
}

private val RULES =
    listOf(
        "Fill the grid so every row, column and box has each number once.",
        "In killer sudoku the numbers in each dashed cage also add up to its total, with no number repeated in a cage.",
    )

private val TUTORIAL =
    listOf(
        TourStep("board", "Tap a cell to select it. Shaded cells are givens and can't change."),
        TourStep("pad", "Then tap a number to place it. The pencil puts numbers in as notes, the eraser clears a cell."),
        TourStep("hint", "Stuck? The bulb points at a cell you can fill next and says why."),
        TourStep("menu", "Restart, show the solution and settings live in this menu."),
    )

private enum class PuzzleAction { RESTART, REVEAL }

@Composable
private fun PuzzleMenu(
    state: GameUiState,
    onRestart: () -> Unit,
    onRevealSolution: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf<PuzzleAction?>(null) }
    val playing = !state.completed
    Box(modifier) {
        IconButton(onClick = { open = true }, modifier = Modifier.testTag("puzzle_menu")) {
            Icon(SudokuIcons.More, contentDescription = "Puzzle options")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.dialogTestTags()) {
            DropdownMenuItem(
                text = { Text("Restart puzzle") },
                enabled = playing,
                onClick = {
                    open = false
                    confirming = PuzzleAction.RESTART
                },
                modifier = Modifier.testTag("menu_restart"),
            )
            if (state.allowReveal) {
                DropdownMenuItem(
                    text = { Text("Show solution") },
                    enabled = playing && state.answerKnown,
                    onClick = {
                        open = false
                        confirming = PuzzleAction.REVEAL
                    },
                    modifier = Modifier.testTag("menu_solution"),
                )
            }
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = {
                    open = false
                    onSettings()
                },
                modifier = Modifier.testTag("menu_settings"),
            )
        }
    }
    confirming?.let { action ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            modifier = Modifier.dialogTestTags().testTag("puzzle_confirm"),
            title = {
                Text(
                    when (action) {
                        PuzzleAction.RESTART -> "Restart this puzzle?"
                        PuzzleAction.REVEAL -> "Show the solution?"
                    },
                )
            },
            text = {
                Text(
                    when (action) {
                        PuzzleAction.RESTART -> "Your numbers, pencil marks, mistakes and time will be cleared."
                        PuzzleAction.REVEAL -> "This ends the game. It won't count towards best times or medals."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = null
                        when (action) {
                            PuzzleAction.RESTART -> onRestart()
                            PuzzleAction.REVEAL -> onRevealSolution()
                        }
                    },
                    modifier = Modifier.testTag("confirm_action"),
                ) {
                    Text(
                        when (action) {
                            PuzzleAction.RESTART -> "Restart"
                            PuzzleAction.REVEAL -> "Show solution"
                        },
                    )
                }
            },
            dismissButton = { TextButton(onClick = { confirming = null }) { Text("Cancel") } },
        )
    }
}
