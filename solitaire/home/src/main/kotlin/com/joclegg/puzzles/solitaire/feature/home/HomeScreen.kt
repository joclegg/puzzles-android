package com.joclegg.puzzles.solitaire.feature.home

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
import com.joclegg.puzzles.solitaire.core.data.SavedGame
import com.joclegg.puzzles.solitaire.core.ui.BackButton
import com.joclegg.puzzles.solitaire.core.ui.SolitaireIcons
import com.joclegg.puzzles.solitaire.core.ui.dialogTestTags
import com.joclegg.puzzles.solitaire.core.ui.formatDuration
import com.joclegg.puzzles.solitaire.core.ui.title
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
    var starting by remember { mutableStateOf<DealKind?>(null) }

    fun launchGame(
        kind: DealKind,
        start: suspend () -> Long,
    ) {
        if (starting != null) {
            return
        }
        starting = kind
        scope.launch {
            try {
                onOpenGame(start())
            } finally {
                starting = null
            }
        }
    }
    HomeScreen(
        state = state,
        dealing = starting,
        onResume = onOpenGame,
        onStart = { launchGame(DealKind.NEW) { viewModel.start() } },
        onDaily = { launchGame(DealKind.DAILY) { viewModel.openDaily() } },
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
    dealing: DealKind?,
    onResume: (Long) -> Unit,
    onStart: () -> Unit,
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
            Text("Solitaire", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings, modifier = Modifier.testTag("settings")) {
                Icon(SolitaireIcons.Settings, contentDescription = "Settings")
            }
        }
        state.inProgress.firstOrNull()?.let { latest -> ContinueCard(latest, onContinue = { onResume(latest.id) }) }
        val saved = state.savedDeal
        Button(
            onClick = {
                if (saved != null) {
                    resumeChoice = saved
                } else {
                    onStart()
                }
            },
            enabled = dealing == null,
            modifier = Modifier.fillMaxWidth().testTag("play"),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (dealing == DealKind.NEW) {
                        "Dealing…"
                    } else {
                        "New game"
                    },
                )
                if (saved != null) {
                    Text("In progress · ${formatDuration(saved.elapsedMs)}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        OutlinedButton(onClick = onDaily, enabled = dealing == null, modifier = Modifier.fillMaxWidth().testTag("daily")) {
            Text(
                if (dealing == DealKind.DAILY) {
                    "Dealing…"
                } else if (state.dailyWon) {
                    "Daily deal · Won ✓"
                } else {
                    "Daily deal"
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
            title = { Text(saved.deal.title()) },
            text = { Text("You have a game in progress (${formatDuration(saved.elapsedMs)}).") },
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
                        onStart()
                    },
                    modifier = Modifier.testTag("new_game"),
                ) { Text("New deal") }
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
            Text("${game.deal.title()} · ${formatDuration(game.elapsedMs)} · ${game.moves.size} moves")
        }
    }
}

enum class DealKind {
    NEW,
    DAILY,
}
