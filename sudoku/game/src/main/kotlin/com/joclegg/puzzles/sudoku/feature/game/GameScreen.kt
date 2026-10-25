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
import com.joclegg.puzzles.sudoku.core.ui.KeypadTool
import com.joclegg.puzzles.sudoku.core.ui.LocalBoardColors
import com.joclegg.puzzles.sudoku.core.ui.NumberPad
import com.joclegg.puzzles.sudoku.core.ui.SudokuBoard
import com.joclegg.puzzles.sudoku.core.ui.SudokuIcons
import com.joclegg.puzzles.sudoku.core.ui.dialogTestTags
import com.joclegg.puzzles.sudoku.core.ui.formatDuration
import com.joclegg.puzzles.sudoku.core.ui.gameTitle
import com.joclegg.puzzles.sudoku.core.ui.label
import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.InputMode
import kotlinx.coroutines.delay

@Composable
fun GameRoute(
    viewModel: GameViewModel,
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
            onErase = viewModel::onErase,
            onToggleNotes = viewModel::onToggleNotes,
            onRestart = viewModel::onRestart,
            onRevealSolution = viewModel::onRevealSolution,
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
    onErase: () -> Unit,
    onToggleNotes: () -> Unit,
    onRestart: () -> Unit,
    onRevealSolution: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GameHeader(
            state,
            onBack = onHome,
            onRestart = onRestart,
            onRevealSolution = onRevealSolution,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        SudokuBoard(board = state.board, onCellClick = onCellClick)
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
                    KeypadTool(SudokuIcons.Undo, "Undo the last move", onUndo, tag = "undo"),
                ),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun GameHeader(
    state: GameUiState,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onRevealSolution: () -> Unit,
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
        PuzzleMenu(state, onRestart, onRevealSolution)
    }
}

private enum class PuzzleAction { RESTART, REVEAL }

@Composable
private fun PuzzleMenu(
    state: GameUiState,
    onRestart: () -> Unit,
    onRevealSolution: () -> Unit,
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
