package com.joclegg.puzzles.kakuro.feature.game

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.kakuro.core.ui.BackButton
import com.joclegg.puzzles.kakuro.core.ui.KakuroBoard
import com.joclegg.puzzles.kakuro.core.ui.KakuroIcons
import com.joclegg.puzzles.kakuro.core.ui.KeypadTool
import com.joclegg.puzzles.kakuro.core.ui.NumberPad
import com.joclegg.puzzles.kakuro.core.ui.ToolButton
import com.joclegg.puzzles.kakuro.core.ui.dialogTestTags
import com.joclegg.puzzles.kakuro.core.ui.formatDuration
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
    onNewPuzzle: () -> Unit,
    onSettings: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var rulesOpen by remember { mutableStateOf(false) }
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
            ToolButton(KakuroIcons.Undo, "Undo the last tap", onUndo, tag = "undo", enabled = state.canUndo)
            ToolButton(KakuroIcons.Redo, "Redo the tap you undid", onRedo, tag = "redo", enabled = state.canRedo)
            ToolButton(KakuroIcons.Rules, "Show the rules", { rulesOpen = true }, tag = "rules")
            Column {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("game_menu")) {
                    Icon(KakuroIcons.More, contentDescription = "More")
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
        KakuroBoard(state.board, onCellClick, Modifier.fillMaxWidth().padding(horizontal = 8.dp))
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
                            !state.digitFirst -> "Clear the selected cell"
                            state.eraserActive -> "Erase is on: tap cells to clear them"
                            else -> "Turn on erase, then tap cells to clear them"
                        },
                        onErase,
                        tag = "erase",
                        on = state.eraserActive,
                    ),
                    KeypadTool(
                        KakuroIcons.Pencil,
                        if (state.notesMode) {
                            "Notes are on: digits go in as notes"
                        } else {
                            "Notes are off: digits go in as answers"
                        },
                        onToggleNotes,
                        tag = "notes",
                        on = state.notesMode,
                    ),
                    KeypadTool(
                        KakuroIcons.Mark,
                        if (state.digitFirst) {
                            "Mark: tap cells to box them in red, tap again to stop"
                        } else {
                            "Box the selected cell in red, to remember where a trial started"
                        },
                        onMark,
                        tag = "mark",
                        on = state.markActive,
                    ),
                ),
        )
    }
    if (rulesOpen) {
        RulesDialog(onDismiss = { rulesOpen = false })
    }
    if (state.solved && state.resultSaved) {
        SolvedDialog(state, onNewPuzzle, onHome)
    }
}

@Composable
private fun RulesDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.dialogTestTags().testTag("rules_dialog"),
        title = { Text("Rules") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fill each white cell with a digit from 1 to 9.")
                Text("The number above a diagonal is the sum of the run to its right; below it, the run beneath.")
                Text("A digit can't repeat within a run.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
    )
}

@Composable
private fun SolvedDialog(
    state: GameUiState,
    onNewPuzzle: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = {},
        modifier = modifier.dialogTestTags().testTag("solved_dialog"),
        title = { Text("Solved!") },
        text = {
            Column {
                if (state.newBest) {
                    Text(
                        "New best!",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("new_best"),
                    )
                }
                Text("Time ${formatDuration(state.elapsedMs)}", modifier = Modifier.testTag("solved_time"))
            }
        },
        confirmButton = {
            TextButton(onClick = onNewPuzzle, modifier = Modifier.testTag("solved_new_puzzle")) { Text("New puzzle") }
        },
        dismissButton = {
            TextButton(onClick = onHome, modifier = Modifier.testTag("solved_home")) { Text("Home") }
        },
    )
}
