package com.joclegg.puzzles.wordwheel.feature.game

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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.wordwheel.core.ui.BackButton
import com.joclegg.puzzles.wordwheel.core.ui.LetterWheel
import com.joclegg.puzzles.wordwheel.core.ui.ToolButton
import com.joclegg.puzzles.wordwheel.core.ui.WordGrid
import com.joclegg.puzzles.wordwheel.core.ui.WordWheelIcons
import com.joclegg.puzzles.wordwheel.core.ui.dialogTestTags
import com.joclegg.puzzles.wordwheel.core.ui.formatDuration
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
            onChosenChange = viewModel::onChosenChange,
            onSubmit = viewModel::onSubmit,
            onClear = viewModel::onClear,
            onShuffle = viewModel::onShuffle,
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
    onChosenChange: (List<Int>) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onShuffle: () -> Unit,
    onHint: () -> Unit,
    onNewPuzzle: () -> Unit,
    onSettings: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rulesOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
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
                    Text("${state.wordsLeft} to find", modifier = Modifier.testTag("words_left"))
                    if (state.showBonusCount) {
                        Spacer(Modifier.width(12.dp))
                        Text("Bonus ${state.bonusCount}", modifier = Modifier.testTag("bonus_count"))
                    }
                }
            }
            ToolButton(WordWheelIcons.Hint, "Show a letter", onHint, tag = "hint", enabled = !state.solved)
            ToolButton(WordWheelIcons.Rules, "Show the rules", { rulesOpen = true }, tag = "rules")
            Column {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("game_menu")) {
                    Icon(WordWheelIcons.More, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.dialogTestTags()) {
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
        WordGrid(state.grid, Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp))
        Text(
            state.chosen
                .map { state.wheel[it] }
                .joinToString("")
                .ifEmpty { state.message.orEmpty() },
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.height(40.dp).testTag("current_word"),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolButton(WordWheelIcons.Shuffle, "Shuffle the letters", onShuffle, tag = "shuffle")
            LetterWheel(state.wheel, state.chosen, onChosenChange, onSubmit, Modifier.width(260.dp).padding(8.dp))
            Column {
                ToolButton(WordWheelIcons.Check, "Enter the word", onSubmit, tag = "submit_word")
                ToolButton(WordWheelIcons.Clear, "Clear the word", onClear, tag = "clear_word")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
    if (rulesOpen) {
        AlertDialog(
            onDismissRequest = { rulesOpen = false },
            modifier = Modifier.dialogTestTags().testTag("rules_dialog"),
            title = { Text("How to play") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Swipe across the letters to make a word, or tap them and press the tick.")
                    Text("Find every word in the crossword. Other real words count as bonus words.")
                    Text("Each letter can be used once per word.")
                }
            },
            confirmButton = { TextButton(onClick = { rulesOpen = false }) { Text("Got it") } },
        )
    }
    if (state.solved && state.resultSaved) {
        AlertDialog(
            onDismissRequest = {},
            modifier = Modifier.dialogTestTags().testTag("solved_dialog"),
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
                    Text("Time ${formatDuration(state.elapsedMs)}")
                    Text("Bonus words ${state.bonusCount}")
                }
            },
            confirmButton = { TextButton(onClick = onNewPuzzle, modifier = Modifier.testTag("solved_new_puzzle")) { Text("Next puzzle") } },
            dismissButton = { TextButton(onClick = onHome, modifier = Modifier.testTag("solved_home")) { Text("Home") } },
        )
    }
}
