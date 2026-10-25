package com.joclegg.puzzles.sudoku.feature.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.ui.BackButton
import com.joclegg.puzzles.sudoku.core.ui.KeypadTool
import com.joclegg.puzzles.sudoku.core.ui.LocalBoardColors
import com.joclegg.puzzles.sudoku.core.ui.NumberPad
import com.joclegg.puzzles.sudoku.core.ui.SudokuBoard
import com.joclegg.puzzles.sudoku.core.ui.SudokuIcons
import com.joclegg.puzzles.sudoku.core.ui.dialogTestTags
import com.joclegg.puzzles.sudoku.engine.CageProblem
import com.joclegg.puzzles.sudoku.engine.InputMode
import kotlinx.coroutines.launch

@Composable
fun EntryRoute(
    viewModel: EntryViewModel,
    onPlay: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    EntryScreen(
        state = state,
        onModeChange = viewModel::onModeChange,
        onCellClick = viewModel::onCellClick,
        onDigitClick = viewModel::onDigitClick,
        onErase = viewModel::onErase,
        onClear = viewModel::onClear,
        onToggleCageTool = viewModel::onToggleCageTool,
        onConfirmCage = viewModel::onConfirmCage,
        onFix = { playUnverified -> scope.launch { viewModel.fix(playUnverified)?.let(onPlay) } },
        onDismissProblem = viewModel::onDismissProblem,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun EntryScreen(
    state: EntryUiState,
    onModeChange: (GameMode) -> Unit,
    onCellClick: (Int) -> Unit,
    onDigitClick: (Int) -> Unit,
    onErase: () -> Unit,
    onClear: () -> Unit,
    onToggleCageTool: () -> Unit,
    onConfirmCage: () -> Unit,
    onFix: (playUnverified: Boolean) -> Unit,
    onDismissProblem: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val killer = state.mode == GameMode.KILLER
    Column(
        modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Enter puzzle", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                if (state.cageTool) {
                    "Cage sum: ${state.cageSumInput.ifEmpty { "–" }}"
                } else {
                    "${state.givenCount} numbers"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("entry_status"),
            )
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            GameMode.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = state.mode == option,
                    onClick = { onModeChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, GameMode.entries.size),
                    modifier = Modifier.testTag("entry_${option.name.lowercase()}"),
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
        SudokuBoard(board = state.board, onCellClick = onCellClick)
        NumberPad(
            onDigit = onDigitClick,
            activeDigit = state.activeDigit.takeIf { state.inputMode == InputMode.DIGIT_FIRST && !state.cageTool },
            counts =
                if (state.cageTool) {
                    emptyList()
                } else {
                    state.digitCounts
                },
            tools = entryTools(state, killer, onToggleCageTool, onConfirmCage, onErase, onClear),
        )
        when {
            state.problem == EntryProblem.CLASH -> ProblemText("Some numbers clash. Fix the red cells.")
            state.cageProblem == CageProblem.NOT_CONNECTED -> ProblemText("A cage's cells must touch each other.")
            state.cageProblem == CageProblem.IMPOSSIBLE_SUM -> ProblemText("No set of different numbers makes that sum.")
            state.cageProblem == CageProblem.EMPTY -> ProblemText("Pick the cage's cells and type its sum first.")
        }
        Button(
            onClick = { onFix(false) },
            enabled = state.canFix,
            modifier = Modifier.fillMaxWidth().testTag("fix_puzzle"),
        ) {
            Text(
                if (state.checking) {
                    "Checking…"
                } else {
                    "Fix puzzle"
                },
            )
        }
    }
    unverifiedMessage(state.problem)?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissProblem,
            modifier = Modifier.dialogTestTags().testTag("unverified_dialog"),
            title = { Text("Can't confirm one answer") },
            text = { Text("$message You can still play it. Wrong numbers can't be shown, but clashes can still be blocked.") },
            confirmButton = { TextButton(onClick = { onFix(true) }, modifier = Modifier.testTag("play_anyway")) { Text("Play anyway") } },
            dismissButton = { TextButton(onClick = onDismissProblem) { Text("Keep editing") } },
        )
    }
}

private fun unverifiedMessage(problem: EntryProblem?): String? =
    when (problem) {
        EntryProblem.NO_SOLUTION -> "This puzzle has no answer, so there may be a typo."
        EntryProblem.MULTIPLE_SOLUTIONS -> "This puzzle has more than one answer."
        EntryProblem.TOO_HARD_TO_CHECK -> "Checking this puzzle would take too long."
        EntryProblem.CLASH, null -> null
    }

private fun entryTools(
    state: EntryUiState,
    killer: Boolean,
    onToggleCageTool: () -> Unit,
    onConfirmCage: () -> Unit,
    onErase: () -> Unit,
    onClear: () -> Unit,
): List<KeypadTool> =
    buildList {
        if (killer) {
            add(
                KeypadTool(
                    icon = SudokuIcons.Cage,
                    description =
                        if (state.cageTool) {
                            "Cages on: tap neighbouring cells, type the sum, then tick"
                        } else {
                            "Draw cages"
                        },
                    onClick = onToggleCageTool,
                    tag = "cage_tool",
                    on = state.cageTool,
                ),
            )
        }
        if (state.cageTool) {
            add(KeypadTool(SudokuIcons.Check, "Save this cage", onConfirmCage, tag = "confirm_cage"))
            add(KeypadTool(SudokuIcons.Eraser, "Delete the selected cage", onErase, tag = "erase"))
        } else {
            add(KeypadTool(SudokuIcons.Eraser, "Erase the selected cell", onErase, tag = "erase"))
            add(KeypadTool(SudokuIcons.Clear, "Clear the whole grid", onClear, tag = "clear"))
        }
    }

@Composable
private fun ProblemText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, color = LocalBoardColors.current.wrong, modifier = modifier.fillMaxWidth().testTag("entry_problem"))
}
