package com.joclegg.puzzles.sightlines.feature.game

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
import com.joclegg.puzzles.sightlines.core.ui.BackButton
import com.joclegg.puzzles.sightlines.core.ui.ColourPicker
import com.joclegg.puzzles.sightlines.core.ui.SightlinesBoard
import com.joclegg.puzzles.sightlines.core.ui.SightlinesIcons
import com.joclegg.puzzles.sightlines.core.ui.ToolButton
import com.joclegg.puzzles.sightlines.core.ui.dialogTestTags
import com.joclegg.puzzles.sightlines.core.ui.formatDuration
import com.joclegg.puzzles.sightlines.engine.Dot
import kotlinx.coroutines.launch

@Composable
fun GameRoute(
    viewModel: GameViewModel,
    onOpenGame: (Long) -> Unit,
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
            onHint = viewModel::onHint,
            onRestart = viewModel::onRestart,
            onNewPuzzle = { scope.launch { onOpenGame(viewModel.newPuzzle()) } },
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
    onHint: () -> Unit,
    onRestart: () -> Unit,
    onNewPuzzle: () -> Unit,
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
                    if (state.showProgress) {
                        Text("${state.progressPercent}%", modifier = Modifier.testTag("progress"))
                    }
                }
            }
            ToolButton(SightlinesIcons.Undo, "Undo the last tap", onUndo, tag = "undo", enabled = state.canUndo)
            ToolButton(SightlinesIcons.Hint, "Show a hint", onHint, tag = "hint", enabled = !state.solved)
            ToolButton(SightlinesIcons.Rules, "Show the rules", { rulesOpen = true }, tag = "rules")
            Column {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("game_menu")) {
                    Icon(SightlinesIcons.More, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
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
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        SightlinesBoard(state.board, onDotClick, Modifier.fillMaxWidth().padding(horizontal = 12.dp))
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
                Text("Fill every dot blue or red. Tap a dot to cycle it: blue, red, empty.")
                Text("Blue dots can see other blue dots in their own row and column. Red dots block the view.")
                Text("A number says how many blue dots that dot can see.")
                Text("Every blue dot must see at least one other.")
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
                if (state.hints > 0) {
                    Text("Hints ${state.hints}")
                }
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
