package com.joclegg.puzzles.fiveletters.feature.game

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.fiveletters.core.ui.BackButton
import com.joclegg.puzzles.fiveletters.core.ui.FiveLettersIcons
import com.joclegg.puzzles.fiveletters.core.ui.Keyboard
import com.joclegg.puzzles.fiveletters.core.ui.ToolButton
import com.joclegg.puzzles.fiveletters.core.ui.WordBoard
import com.joclegg.puzzles.fiveletters.core.ui.dialogTestTags
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
    val context = LocalContext.current
    state?.let {
        GameScreen(
            state = it,
            onLetterClick = viewModel::onLetterClick,
            onEnterClick = viewModel::onEnterClick,
            onDeleteClick = viewModel::onDeleteClick,
            onShare = {
                val send =
                    Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, it.shareText)
                context.startActivity(Intent.createChooser(send, null))
            },
            onNewPractice = { scope.launch { onOpenGame(viewModel.newPractice()) } },
            onSettings = onSettings,
            onHome = onHome,
            modifier = modifier,
        )
    }
}

@Composable
fun GameScreen(
    state: GameUiState,
    onLetterClick: (Char) -> Unit,
    onEnterClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShare: () -> Unit,
    onNewPractice: () -> Unit,
    onSettings: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rulesOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var resultClosed by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onHome)
            Column(Modifier.weight(1f)) {
                Text(state.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("game_title"))
                if (state.hardMode) {
                    Text("Hard mode", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (state.won || state.lost) {
                ToolButton(FiveLettersIcons.Share, "Share your result", onShare, tag = "share")
            }
            ToolButton(FiveLettersIcons.Rules, "Show the rules", { rulesOpen = true }, tag = "rules")
            Column {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("game_menu")) {
                    Icon(FiveLettersIcons.More, contentDescription = "More")
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
        Spacer(Modifier.height(12.dp))
        WordBoard(state.board, Modifier.weight(1f, fill = false).padding(horizontal = 12.dp))
        Text(
            state.message.orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = 8.dp).testTag("message"),
        )
        Keyboard(
            letterMarks = state.board.letterMarks,
            onLetterClick = onLetterClick,
            onEnterClick = onEnterClick,
            onDeleteClick = onDeleteClick,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 12.dp),
        )
    }
    if (rulesOpen) {
        AlertDialog(
            onDismissRequest = { rulesOpen = false },
            modifier = Modifier.dialogTestTags().testTag("rules_dialog"),
            title = { Text("How to play") },
            text = {
                Column {
                    Text("Guess the five-letter word in six tries. After each guess the tiles change colour:")
                    Text("Green: right letter, right place.")
                    Text("Yellow: in the word, wrong place.")
                    Text("Grey: not in the word.")
                }
            },
            confirmButton = { TextButton(onClick = { rulesOpen = false }) { Text("Got it") } },
        )
    }
    if ((state.won || state.lost) && !resultClosed) {
        ResultDialog(state, onShare, onNewPractice, onClose = { resultClosed = true })
    }
}

@Composable
private fun ResultDialog(
    state: GameUiState,
    onShare: () -> Unit,
    onNewPractice: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onClose,
        modifier = modifier.dialogTestTags().testTag("result_dialog"),
        title = {
            Text(
                if (state.won) {
                    "Got it in ${state.guessCount}!"
                } else {
                    "The word was ${state.answer}"
                },
                modifier = Modifier.testTag("result_title"),
            )
        },
        text = {
            Text(
                if (state.isDaily) {
                    "A new word comes tomorrow. Share your grid without giving the word away."
                } else {
                    "Play another practice word, or share your grid."
                },
            )
        },
        confirmButton = { TextButton(onClick = onShare, modifier = Modifier.testTag("result_share")) { Text("Share") } },
        dismissButton = {
            if (state.isDaily) {
                TextButton(onClick = onClose, modifier = Modifier.testTag("result_close")) { Text("Close") }
            } else {
                TextButton(onClick = onNewPractice, modifier = Modifier.testTag("result_new_practice")) { Text("New word") }
            }
        },
    )
}
