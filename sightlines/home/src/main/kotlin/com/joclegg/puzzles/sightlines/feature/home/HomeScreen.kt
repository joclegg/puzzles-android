package com.joclegg.puzzles.sightlines.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.joclegg.puzzles.sightlines.core.data.SavedGame
import com.joclegg.puzzles.sightlines.core.ui.BackButton
import com.joclegg.puzzles.sightlines.core.ui.SightlinesIcons
import com.joclegg.puzzles.sightlines.core.ui.dialogTestTags
import com.joclegg.puzzles.sightlines.core.ui.formatDuration
import com.joclegg.puzzles.sightlines.core.ui.sizeLabel
import com.joclegg.puzzles.sightlines.core.ui.title
import com.joclegg.puzzles.sightlines.engine.SIZES
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
        onStart = { size -> launchGame { viewModel.start(size) } },
        onDaily = { launchGame { viewModel.openDaily() } },
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
    onStart: (Int) -> Unit,
    onDaily: () -> Unit,
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
            Text("Sightlines", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings, modifier = Modifier.testTag("settings")) {
                Icon(SightlinesIcons.Settings, contentDescription = "Settings")
            }
        }
        state.inProgress.firstOrNull()?.let { latest -> ContinueCard(latest, onContinue = { onResume(latest.id) }) }
        SIZES.forEach { size ->
            val saved = state.savedFor(size)
            Button(
                onClick = {
                    if (saved != null) {
                        resumeChoice = saved
                    } else {
                        onStart(size)
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("size_$size"),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(sizeLabel(size))
                    if (saved != null) {
                        Text("In progress · ${formatDuration(saved.elapsedMs)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        OutlinedButton(onClick = onDaily, modifier = Modifier.fillMaxWidth().testTag("daily")) {
            Text(
                if (state.dailySolved) {
                    "Daily puzzle · Solved ✓"
                } else {
                    "Daily puzzle"
                },
            )
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
                        onStart(saved.ref.size)
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
