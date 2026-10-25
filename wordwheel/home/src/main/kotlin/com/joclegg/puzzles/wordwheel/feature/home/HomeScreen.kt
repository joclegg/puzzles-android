package com.joclegg.puzzles.wordwheel.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.wordwheel.core.data.SavedGame
import com.joclegg.puzzles.wordwheel.core.ui.BackButton
import com.joclegg.puzzles.wordwheel.core.ui.WordWheelIcons
import com.joclegg.puzzles.wordwheel.core.ui.dialogTestTags
import com.joclegg.puzzles.wordwheel.core.ui.formatDuration
import com.joclegg.puzzles.wordwheel.core.ui.label
import com.joclegg.puzzles.wordwheel.core.ui.title
import com.joclegg.puzzles.wordwheel.engine.Level
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onOpenGame: (Long) -> Unit,
    onStats: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var starting by remember { mutableStateOf(false) }

    fun launchGame(start: suspend () -> Long) {
        if (starting) {
            return
        }
        starting = true
        scope.launch {
            try {
                onOpenGame(start())
            } finally {
                starting = false
            }
        }
    }
    HomeScreen(
        state = state,
        onResume = onOpenGame,
        onStart = { level -> launchGame { viewModel.start(level) } },
        onDaily = { level -> launchGame { viewModel.openDaily(level) } },
        onStats = onStats,
        onHistory = onHistory,
        onSettings = onSettings,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onResume: (Long) -> Unit,
    onStart: (Level) -> Unit,
    onDaily: (Level) -> Unit,
    onStats: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var resumeChoice by remember { mutableStateOf<SavedGame?>(null) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Word Wheel", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings, modifier = Modifier.testTag("settings")) {
                Icon(WordWheelIcons.Settings, contentDescription = "Settings")
            }
        }
        state.inProgress.firstOrNull()?.let { latest -> ContinueCard(latest, onContinue = { onResume(latest.id) }) }
        Level.entries.forEach { level ->
            val saved = state.savedFor(level)
            Button(
                onClick = {
                    if (saved != null) {
                        resumeChoice = saved
                    } else {
                        onStart(level)
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("level_${level.letters}"),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(level.label())
                    if (saved != null) {
                        Text("In progress · ${formatDuration(saved.elapsedMs)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Text("Today's puzzles", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Level.entries.forEach { level ->
                OutlinedButton(
                    onClick = { onDaily(level) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.weight(1f).testTag("daily_${level.letters}"),
                ) {
                    Text(
                        if (level in state.dailySolved) {
                            "${level.letters} ✓"
                        } else {
                            "${level.letters}"
                        },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onStats, modifier = Modifier.weight(1f).testTag("stats")) { Text("Stats") }
            OutlinedButton(onClick = onHistory, modifier = Modifier.weight(1f).testTag("history")) { Text("History") }
        }
    }
    resumeChoice?.let { saved ->
        AlertDialog(
            onDismissRequest = { resumeChoice = null },
            modifier = Modifier.dialogTestTags().testTag("resume_dialog"),
            title = { Text(saved.ref.title()) },
            text = { Text("You have a puzzle in progress (${formatDuration(saved.elapsedMs)}).") },
            confirmButton = {
                TextButton(
                    onClick = {
                        resumeChoice = null
                        onResume(saved.id)
                    },
                    modifier = Modifier.testTag("resume_game"),
                ) { Text("Resume") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        resumeChoice = null
                        onStart(saved.ref.level)
                    },
                    modifier = Modifier.testTag("new_game"),
                ) { Text("New puzzle") }
            },
        )
    }
}

@Composable
private fun ContinueCard(
    game: SavedGame,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onContinue, modifier = modifier.fillMaxWidth().testTag("continue")) {
        Column(Modifier.padding(16.dp)) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
            Text("${game.ref.title()} · ${formatDuration(game.elapsedMs)}")
        }
    }
}
