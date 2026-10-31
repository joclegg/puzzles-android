package com.joclegg.puzzles.solitaire.feature.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.joclegg.puzzles.solitaire.core.ui.BackButton
import com.joclegg.puzzles.solitaire.core.ui.HintButton
import com.joclegg.puzzles.solitaire.core.ui.HowToPlayButton
import com.joclegg.puzzles.solitaire.core.ui.LocalTableColors
import com.joclegg.puzzles.solitaire.core.ui.SolitaireIcons
import com.joclegg.puzzles.solitaire.core.ui.SolitaireTable
import com.joclegg.puzzles.solitaire.core.ui.ToolButton
import com.joclegg.puzzles.solitaire.core.ui.TourStep
import com.joclegg.puzzles.solitaire.core.ui.dialogTestTags
import com.joclegg.puzzles.solitaire.core.ui.formatDuration
import com.joclegg.puzzles.solitaire.core.ui.rememberTourState
import com.joclegg.puzzles.solitaire.core.ui.tourTarget
import com.joclegg.puzzles.solitaire.engine.Pile
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
            onStockClick = viewModel::onStockClick,
            onCardClick = viewModel::onCardClick,
            onEmptyPileClick = viewModel::onEmptyPileClick,
            onCardDrop = viewModel::onCardDrop,
            onUndoToWinnable = viewModel::onUndoToWinnable,
            onDismissUnwinnable = viewModel::onDismissUnwinnable,
            onGiveUp = {
                scope.launch {
                    viewModel.giveUp()
                    onHome()
                }
            },
            onUndo = viewModel::onUndo,
            onFinish = viewModel::onFinish,
            onHint = viewModel::onHint,
            onRestart = viewModel::onRestart,
            onNewDeal = { scope.launch { onOpenGame(viewModel.newDeal()) } },
            onSettings = onSettings,
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
    onCardDrop: (Pile, Int, Pile) -> Unit,
    onUndoToWinnable: () -> Unit,
    onDismissUnwinnable: () -> Unit,
    onGiveUp: () -> Unit,
    onUndo: () -> Unit,
    onFinish: () -> Unit,
    onHint: () -> Unit,
    onRestart: () -> Unit,
    onNewDeal: () -> Unit,
    onSettings: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val tour = rememberTourState()
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
            if (state.showHints) {
                HintButton(onHint, enabled = !state.won, modifier = Modifier.tourTarget(tour, "hint"))
            }
            if (state.showTutorial) {
                HowToPlayButton(RULES, TUTORIAL, tour)
            }
            Column {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("game_menu").tourTarget(tour, "menu")) {
                    Icon(SolitaireIcons.More, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.dialogTestTags()) {
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
        SolitaireTable(
            table = state.table,
            onStockClick = onStockClick,
            onCardClick = onCardClick,
            onEmptyPileClick = onEmptyPileClick,
            modifier = Modifier.tourTarget(tour, "table").weight(1f),
            onCardDrop = onCardDrop,
            celebrate = state.finishing,
        )
        state.hintMessage?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("hint_message"),
            )
        }
        if (state.canFinish) {
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("finish"),
            ) {
                Text("Finish")
            }
        }
    }
    if (state.unwinnable) {
        AlertDialog(
            onDismissRequest = onDismissUnwinnable,
            modifier = Modifier.dialogTestTags().testTag("unwinnable_dialog"),
            title = { Text("This deal can't be won now") },
            text = {
                Text(unwinnableText(state))
            },
            confirmButton = {
                TextButton(
                    onClick = onUndoToWinnable,
                    enabled = !state.findingLostMove,
                    modifier = Modifier.testTag("undo_to_winnable"),
                ) {
                    Text(undoToWinnableLabel(state))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onGiveUp, modifier = Modifier.testTag("unwinnable_leave")) { Text("Leave as lost") }
                    TextButton(onClick = onDismissUnwinnable, modifier = Modifier.testTag("keep_playing")) { Text("Keep playing") }
                    TextButton(onClick = onNewDeal, modifier = Modifier.testTag("unwinnable_new_deal")) { Text("New deal") }
                }
            },
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
    WinDialog(
        title = "You won!",
        accent = LocalTableColors.current.accent,
        dailyGroup = "SOLITAIRE",
        newLabel = "Deal",
        newTag = "won_new_deal",
        onNew = onNewDeal,
        newBest = state.newBest,
        stats =
            buildList {
                add(WinStat("Time", formatDuration(state.elapsedMs), "won_time"))
                add(WinStat("Moves", "${state.moveCount}"))
            },
        modifier = modifier.dialogTestTags().testTag("won_dialog"),
    ) {
        TextButton(onClick = onHome, modifier = Modifier.testTag("won_home")) { Text("Home") }
    }
}

private val RULES =
    listOf(
        "Move every card up to the four foundations, Ace to King.",
        "In the columns, build down in alternating colours. Any face-up run can move together.",
        "Only a King can fill an empty column.",
        "Tap a card to send it to the best place, or drag it.",
    )

private val TUTORIAL =
    listOf(
        TourStep(
            "table",
            "Tap the stock to turn cards over. Build the columns down in alternating colours, and the foundations up from Ace to King.",
        ),
        TourStep("hint", "Stuck? The bulb suggests a move."),
        TourStep("menu", "Restart, deal again and settings live in this menu."),
    )

private fun unwinnableText(state: GameUiState): String {
    val checked =
        "You've been through the whole stock with nothing new to play, " +
            "so the solver checked every line from here: none of them wins."
    return if (state.findingLostMove || state.lastWinnableMove == 0) {
        checked
    } else {
        "$checked\n\nIt could still be won up to move ${state.lastWinnableMove}. " +
            "Move ${state.lastWinnableMove + 1} is where it slipped away."
    }
}

private fun undoToWinnableLabel(state: GameUiState): String =
    when {
        state.findingLostMove -> "Finding where it went wrong…"
        state.lastWinnableMove == 0 -> "Start the deal again"
        else -> "Undo ${state.moveCount - state.lastWinnableMove} moves"
    }
