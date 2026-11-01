package com.joclegg.puzzles.solitaire.feature.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.ContinueCard
import com.joclegg.puzzles.core.ui.MenuIcons
import com.joclegg.puzzles.core.ui.MenuSection
import com.joclegg.puzzles.core.ui.MenuTile
import com.joclegg.puzzles.core.ui.formatDuration
import com.joclegg.puzzles.core.ui.home.GameHomeScaffold
import com.joclegg.puzzles.core.ui.todayLabel
import com.joclegg.puzzles.solitaire.core.data.SavedGame
import com.joclegg.puzzles.solitaire.ui.LocalTableColors
import com.joclegg.puzzles.solitaire.ui.SolitaireGameIcon
import com.joclegg.puzzles.solitaire.ui.SolitaireIcons
import com.joclegg.puzzles.solitaire.ui.dialogTestTags
import com.joclegg.puzzles.solitaire.ui.title
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
    val accent = LocalTableColors.current.accent
    GameHomeScaffold(
        title = "Solitaire",
        calendarGame = "SOLITAIRE",
        tagline = "Klondike, draw 1 or 3, or reverse",
        accent = accent,
        settingsIcon = SolitaireIcons.Settings,
        onStats = onStats,
        onHistory = onHistory,
        onSettings = onSettings,
        onBack = onBack,
        gameIcon = { SolitaireGameIcon() },
        modifier = modifier,
    ) {
        state.inProgress.firstOrNull()?.let { latest ->
            ContinueCard(
                detail = "${latest.deal.title()} · ${formatDuration(latest.elapsedMs)} · ${latest.moves.size} moves",
                accent = accent,
                onClick = { onResume(latest.id) },
                modifier = Modifier.testTag("continue"),
            )
        }
        MenuSection("Today", aside = todayLabel()) {
            MenuTile(
                label =
                    if (dealing == DealKind.DAILY) {
                        "Dealing…"
                    } else {
                        "Daily deal"
                    },
                accent = accent,
                onClick = onDaily,
                modifier = Modifier.fillMaxWidth().testTag("daily"),
                detail =
                    if (state.dailyWon) {
                        "Won"
                    } else {
                        null
                    },
                icon = MenuIcons.Today,
                done = state.dailyWon,
                enabled = dealing == null,
            )
        }
        MenuSection("New game") {
            val saved = state.savedDeal
            MenuTile(
                label =
                    if (dealing == DealKind.NEW) {
                        "Dealing…"
                    } else {
                        "New game"
                    },
                accent = accent,
                onClick = {
                    if (saved != null) {
                        resumeChoice = saved
                    } else {
                        onStart()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("play"),
                detail = saved?.let { "In progress · ${formatDuration(it.elapsedMs)}" },
                icon = MenuIcons.Deal,
                active = saved != null,
                enabled = dealing == null,
            )
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

enum class DealKind {
    NEW,
    DAILY,
}
