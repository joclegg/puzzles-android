package com.joclegg.puzzles.solitaire.feature.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.joclegg.puzzles.solitaire.core.ui.BackButton
import com.joclegg.puzzles.solitaire.core.ui.SolitaireIcons
import com.joclegg.puzzles.solitaire.core.ui.SolitaireTable
import com.joclegg.puzzles.solitaire.core.ui.ToolButton
import com.joclegg.puzzles.solitaire.core.ui.dialogTestTags
import com.joclegg.puzzles.solitaire.core.ui.formatDuration
import com.joclegg.puzzles.solitaire.engine.Pile
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
            onStockClick = viewModel::onStockClick,
            onCardClick = viewModel::onCardClick,
            onEmptyPileClick = viewModel::onEmptyPileClick,
            onUndo = viewModel::onUndo,
            onRestart = viewModel::onRestart,
            onNewDeal = { scope.launch { onOpenGame(viewModel.newDeal()) } },
            onHome = onHome,
            modifier = modifier,
        )
    }
}

@Composable
fun GameScreen(
    state: GameUiState,
    onStockClick: () -> Unit,
    onCardClick: (Pile, Int) -> Unit,
    onEmptyPileClick: (Pile) -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onNewDeal: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onHome)
            Column(Modifier.weight(1f)) {
                Text(state.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("game_title"))
                Row {
                    if (state.showTimer) {
                        Text(formatDuration(state.elapsedMs), modifier = Modifier.testTag("timer"))
                        Text("  ")
                    }
                    if (state.showMoveCount) {
                        Text("Moves ${state.moveCount}", modifier = Modifier.testTag("move_count"))
                    }
                }
            }
            ToolButton(SolitaireIcons.Undo, "Undo the last move", onUndo, tag = "undo", enabled = state.canUndo)
            Column {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("game_menu")) {
                    Icon(SolitaireIcons.More, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Restart this deal") },
                        enabled = !state.won,
                        onClick = {
                            menuOpen = false
                            onRestart()
                        },
                        modifier = Modifier.testTag("restart"),
                    )
                    DropdownMenuItem(
                        text = { Text("New deal") },
                        onClick = {
                            menuOpen = false
                            onNewDeal()
                        },
                        modifier = Modifier.testTag("new_deal"),
                    )
                }
            }
        }
        SolitaireTable(
            table = state.table,
            onStockClick = onStockClick,
            onCardClick = onCardClick,
            onEmptyPileClick = onEmptyPileClick,
            modifier = Modifier.weight(1f),
        )
    }
    if (state.won && state.resultSaved) {
        WonDialog(state, onNewDeal, onHome)
    }
}

@Composable
private fun WonDialog(
    state: GameUiState,
    onNewDeal: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = {},
        modifier = modifier.dialogTestTags().testTag("won_dialog"),
        title = { Text("You won!") },
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
                Row {
                    Text("Time ${formatDuration(state.elapsedMs)}", modifier = Modifier.testTag("won_time"))
                    Spacer(Modifier.width(16.dp))
                    Text("Moves ${state.moveCount}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onNewDeal, modifier = Modifier.testTag("won_new_deal")) { Text("New deal") }
        },
        dismissButton = {
            TextButton(onClick = onHome, modifier = Modifier.testTag("won_home")) { Text("Home") }
        },
    )
}
